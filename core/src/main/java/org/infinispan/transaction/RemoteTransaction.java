/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.transaction;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.mvcc.InternalMVCCEntry;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.InvalidTransactionException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Defines the state of a remotely originated transaction.
 *
 * @author Mircea.Markus@jboss.com
 * @author Pedro Ruivo
 * @author Sebastiano Peluso
 * @since 4.0
 */
public class RemoteTransaction extends AbstractCacheTransaction implements Cloneable {

   private static final Log log = LogFactory.getLog(RemoteTransaction.class);

   private volatile boolean valid = true;

   /**
    * During state transfer only the locks are being migrated over, but no modifications.
    */
   private boolean missingModifications;

   private EnumSet<State> state;

   private enum State {
      /**
       * the prepare command was received and started the validation
       */
      PREPARING,
      /**
       * the prepare command was received and finished the validation
       */
      PREPARED,
      /**
       * the rollback command was received before the prepare command and the transaction must be aborted
       */
      ROLLBACK_ONLY,
      /**
       * the commit command was received before the prepare command and the transaction must be committed
       */
      COMMIT_ONLY
   }

   public RemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx, int viewId) {
      super(tx, viewId);
      this.modifications = modifications == null || modifications.length == 0 ? Collections.<WriteCommand>emptyList() : Arrays.asList(modifications);
      lookedUpEntries = new HashMap<Object, CacheEntry>(this.modifications.size());
      this.state = EnumSet.noneOf(State.class);
   }

   public RemoteTransaction(GlobalTransaction tx, int viewId) {
      super(tx, viewId);
      this.modifications = new LinkedList<WriteCommand>();
      lookedUpEntries = new HashMap<Object, CacheEntry>(2);
      this.state = EnumSet.noneOf(State.class);
   }

   public void invalidate() {
      valid = false;
   }

   @Override
   public void putLookedUpEntry(Object key, CacheEntry e) {
      if (!valid) {
         throw new InvalidTransactionException("This remote transaction " + getGlobalTransaction() + " is invalid");
      }
      if (log.isTraceEnabled()) {
         log.tracef("Adding key %s to tx %s", key, getGlobalTransaction());
      }
      lookedUpEntries.put(key, e);
   }

   @Override
   public void putLookedUpEntries(Map<Object, CacheEntry> entries) {
      if (!valid) {
         throw new InvalidTransactionException("This remote transaction " + getGlobalTransaction() + " is invalid");
      }
      if (log.isTraceEnabled()) {
         log.tracef("Adding keys %s to tx %s", entries.keySet(), getGlobalTransaction());
      }
      lookedUpEntries.putAll(entries);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RemoteTransaction)) return false;
      RemoteTransaction that = (RemoteTransaction) o;
      return tx.equals(that.tx);
   }

   @Override
   public int hashCode() {
      return tx.hashCode();
   }

   @Override
   @SuppressWarnings("unchecked")
   public Object clone() {
      try {
         RemoteTransaction dolly = (RemoteTransaction) super.clone();
         dolly.modifications = new ArrayList<WriteCommand>(modifications);
         dolly.lookedUpEntries = new HashMap<Object, CacheEntry>(lookedUpEntries);
         return dolly;
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException("Impossible!!");
      }
   }

   @Override
   public String toString() {
      return "RemoteTransaction{" +
            "modifications=" + modifications +
            ", lookedUpEntries=" + lookedUpEntries +
            ", lockedKeys= " + lockedKeys +
            ", backupKeyLocks " + backupKeyLocks +
            ", isMissingModifications " + missingModifications +
            ", tx=" + tx +
            '}';
   }

   public void setMissingModifications(boolean missingModifications) {
      this.missingModifications = missingModifications;
   }

   public boolean isMissingModifications() {
      return missingModifications;
   }

   /**
    * check if the transaction is marked for rollback (by the Rollback Command)
    * @return true if it is marked for rollback, false otherwise
    */
   public synchronized boolean isMarkedForRollback() {
      return state.contains(State.ROLLBACK_ONLY);
   }

   /**
    * check if the transaction is marked for commit (by the Commit Command)
    * @return true if it is marked for commit, false otherwise
    */
   public synchronized boolean isMarkedForCommit() {
      return state.contains(State.COMMIT_ONLY);
   }

   /**
    * mark the transaction as prepared (the validation was finished) and notify a possible pending commit or rollback
    * command
    */
   public synchronized void markPreparedAndNotify() {
      state.add(State.PREPARED);
      this.notifyAll();
   }

   /**
    * mark the transaction as preparing, blocking the commit and rollback commands until the
    * {@link #markPreparedAndNotify()} is invoked
    */
   public synchronized void markForPreparing() {
      state.add(State.PREPARING);
   }

   /**
    * Commit and rollback commands invokes this method and they are blocked here if the state is PREPARING
    *
    * @param commit true if it is a commit command, false otherwise
    * @return true if the command needs to be processed, false otherwise
    * @throws InterruptedException when it is interrupted while waiting
    */
   public final synchronized boolean waitPrepared(boolean commit) throws InterruptedException {
      boolean result;
      if (state.contains(State.PREPARED)) {
         result = true;
         log.tracef("Finished waiting: transaction already prepared");
      } else  if (state.contains(State.PREPARING)) {
         this.wait();
         result = true;
         log.tracef("Transaction was in PREPARING state but now it is prepared");
      } else {
         State status = commit ? State.COMMIT_ONLY : State.ROLLBACK_ONLY;
         log.tracef("Transaction hasn't received the prepare yet, setting status to: %s", status);
         state.add(status);
         result = false;
      }
      return result;
   }   

   @Override
   public void addLocalReadKey(Object key, InternalMVCCEntry ime) {
      //no-op
   }

   @Override
   public void removeLocalReadKey(Object key) {
      //no-op
   }

   @Override
   public void removeRemoteReadKey(Object key){
      //no-op
   }

   @Override
   public void addRemoteReadKey(Object key, InternalMVCCEntry ime) {
      //no-op
   }
}
