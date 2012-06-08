/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
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
package org.infinispan.context.impl;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.mvcc.InternalMVCCEntry;
import org.infinispan.mvcc.VersionVC;
import org.infinispan.mvcc.VersionVCFactory;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.AbstractCacheTransaction;
import org.infinispan.transaction.LocalTransaction;
import org.infinispan.transaction.xa.GlobalTransaction;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Invocation context to be used for locally originated transactions.
 *
 * @author Mircea.Markus@jboss.com
 * @author Galder Zamarreño
 * @author Pedro Ruivo
 * @author Sebastiano Peluso
 * @since 4.0
 */
public class LocalTxInvocationContext extends AbstractTxInvocationContext {

   public static final Map<Object,CacheEntry> EMPTY_ENTRY_MAP = new HashMap<Object, CacheEntry>(0);

   private LocalTransaction localTransaction;

   @Override
   public boolean isTransactionValid() {
      Transaction t = getTransaction();
      int status = -1;
      if (t != null) {
         try {
            status = t.getStatus();
         } catch (SystemException e) {
            // no op
         }
      }
      return status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARING;
   }

   @Override
   public boolean isOriginLocal() {
      return true;
   }

   @Override
   public Object getLockOwner() {
      return localTransaction.getGlobalTransaction();
   }

   @Override
   public GlobalTransaction getGlobalTransaction() {
      return localTransaction.getGlobalTransaction();
   }

   @Override
   public List<WriteCommand> getModifications() {
      return localTransaction == null ? null : localTransaction.getModifications();
   }

   public void setLocalTransaction(LocalTransaction localTransaction) {
      this.localTransaction = localTransaction;
   }

   @Override
   public CacheEntry lookupEntry(Object key) {
      return localTransaction != null ? localTransaction.lookupEntry(key) : null;
   }

   @Override
   public Map<Object, CacheEntry> getLookedUpEntries() {
      return localTransaction != null ? localTransaction.getLookedUpEntries() : EMPTY_ENTRY_MAP;
   }

   @Override
   public void putLookedUpEntry(Object key, CacheEntry e) {
      localTransaction.putLookedUpEntry(key, e);
   }

   @Override
   public void putLookedUpEntries(Map<Object, CacheEntry> lookedUpEntries) {
      localTransaction.putLookedUpEntries(lookedUpEntries);
   }

   @Override
   public void removeLookedUpEntry(Object key) {
      localTransaction.removeLookedUpEntry(key);
   }

   @Override
   public void clearLookedUpEntries() {
      localTransaction.clearLookedUpEntries();
   }

   @Override
   public boolean hasLockedKey(Object key) {
      return localTransaction != null && localTransaction.ownsLock(key);
   }

   public void remoteLocksAcquired(Collection<Address> nodes) {
      localTransaction.locksAcquired(nodes);
   }

   public Collection<Address> getRemoteLocksAcquired() {
      return localTransaction.getRemoteLocksAcquired();
   }

   @Override
   public AbstractCacheTransaction getCacheTransaction() {
      return localTransaction;
   }

   @Override
   public Set<Object> getLockedKeys() {
      return localTransaction == null ? Collections.emptySet() : localTransaction.getLockedKeys();
   }

   @Override
   public void addLockedKey(Object key) {
      localTransaction.registerLockedKey(key);
   }

   @Override
   public Transaction getTransaction() {
      Transaction tx = super.getTransaction();
      return tx == null ? localTransaction.getTransaction() : tx;
   }
   
   public Object[] getLocalReadSet(){
      return this.localTransaction.getLocalReadSet();
   }

   public Object[] getRemoteReadSet(){
      return this.localTransaction.getRemoteReadSet();
   }

   @Override
   public VersionVC calculateVersionToRead(VersionVCFactory versionVCFactory) {
      return localTransaction.calculateVectorClockToRead(versionVCFactory);
   }

   @Override
   public VersionVC getPrepareVersion() {
      return localTransaction.getPrepareVectorClock();
   }

   @Override
   public void updateVectorClock(VersionVC other) {
      localTransaction.updateVectorClock(other);
   }

   @Override
   public long getVectorClockValueIn(VersionVCFactory versionVCFactory, int idx) {
      return localTransaction.getValueFrom(versionVCFactory, idx);
   }

   @Override
   public void setVectorClockValueIn(VersionVCFactory versionVCFactory, int pos, long value){
      localTransaction.setVectorClockValueIn(versionVCFactory, pos, value);
   }

   public VersionVC getMinVersion() {
      return localTransaction.getMinVersion();
   }

   @Override
   public void addLocalReadKey(Object key, InternalMVCCEntry ime) {
      localTransaction.addLocalReadKey(key, ime);
   }

   @Override
   public void removeLocalReadKey(Object key) {
      localTransaction.removeLocalReadKey(key);
   }

   @Override
   public void removeRemoteReadKey(Object key) {
      localTransaction.removeRemoteReadKey(key);
   }

   @Override
   public void addRemoteReadKey(Object key, InternalMVCCEntry ime) {
      localTransaction.addRemoteReadKey(key, ime);
   }

   @Override
   public void markReadFrom(int idx) {
      localTransaction.setAlreadyRead(idx);
   }

   @Override
   public BitSet getReadFrom(){
      return (localTransaction == null)?null:localTransaction.getAlreadyRead();
   }

   @Override
   public void setAlreadyReadOnNode(boolean alreadyRead){
      this.localTransaction.setAlreadyReadOnNode(alreadyRead);
   }

   @Override
   public boolean getAlreadyReadOnNode(){
      return this.localTransaction.getAlreadyReadOnNode();
   }

   @Override
   public InternalMVCCEntry getLocalReadKey(Object Key) {
      return localTransaction.getLocalReadKey(Key);
   }

   @Override
   public InternalMVCCEntry getRemoteReadKey(Object Key) {
      return localTransaction.getRemoteReadKey(Key);
   }

   @Override
   public void setCommitVersion(VersionVC version) {
      localTransaction.setCommitVersion(version);
   }

   @Override
   public void setLastReadKey(CacheEntry entry){
      localTransaction.setLastReadKey(entry);
   }

   @Override
   public CacheEntry getLastReadKey(){
      return localTransaction.getLastReadKey();
   }

   @Override
   public void clearLastReadKey(){
      localTransaction.clearLastReadKey();
   }

   @Override
   public void setLastRemoteReadKey(InternalMVCCEntry entry) {
      localTransaction.setLastRemoteReadKey(entry);    // TODO: Customise this generated block
   }

   @Override
   public InternalMVCCEntry getLastRemoteReadKey() {
      return localTransaction.getLastRemoteReadKey();    // TODO: Customise this generated block
   }
}
