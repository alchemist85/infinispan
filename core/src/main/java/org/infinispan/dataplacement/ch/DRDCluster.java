package org.infinispan.dataplacement.ch;

import org.infinispan.remoting.transport.Address;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * @author Pedro Ruivo
 * @since 5.2
 */
public class DRDCluster {
   private final int id;
   private final float weight;
   private final Address[] members;

   public DRDCluster(int id, float weight, Address[] members) {
      this.id = id;
      this.weight = weight;
      this.members = members;
   }

   public int getId() {
      return id;
   }

   public float getWeight() {
      return weight;
   }

   public Address[] getMembers() {
      return members;
   }

   public final void writeTo(ObjectOutput output) throws IOException {
      output.writeInt(id);
      output.writeFloat(weight);
      output.writeInt(members.length);
      for (Address address : members) {
         output.writeObject(address);
      }
   }

   public static DRDCluster readFrom(ObjectInput input) throws IOException, ClassNotFoundException {
      final int id = input.readInt();
      final float weight = input.readFloat();
      final Address[] members = new Address[input.readInt()];
      for (int i = 0; i < members.length; ++i) {
         members[i] = (Address) input.readObject();
      }
      return new DRDCluster(id, weight, members);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      DRDCluster that = (DRDCluster) o;

      return id == that.id && Float.compare(that.weight, weight) == 0 && Arrays.equals(members, that.members);

   }

   @Override
   public int hashCode() {
      int result = id;
      result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
      result = 31 * result + Arrays.hashCode(members);
      return result;
   }
}
