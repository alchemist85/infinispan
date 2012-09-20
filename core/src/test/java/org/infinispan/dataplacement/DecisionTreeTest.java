package org.infinispan.dataplacement;

import org.infinispan.dataplacement.c50.keyfeature.Feature;
import org.infinispan.dataplacement.c50.keyfeature.FeatureValue;
import org.infinispan.dataplacement.c50.keyfeature.NameListFeature;
import org.infinispan.dataplacement.c50.keyfeature.NumericFeature;
import org.infinispan.dataplacement.c50.tree.DecisionTree;
import org.infinispan.dataplacement.c50.tree.DecisionTreeBuilder;
import org.infinispan.dataplacement.c50.tree.DecisionTreeParser;
import org.infinispan.dataplacement.c50.tree.ParseTreeNode;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decision Tree test
 *
 * @author Pedro Ruivo
 * @since 5.2
 */
@Test(groups = "functional", testName = "dataplacement.DecisionTreeTest")
public class DecisionTreeTest {

   public void testEx1() throws Exception {
      DecisionTreeParser parser = new DecisionTreeParser("ex1.tree");
      ParseTreeNode root = parser.parse();

      assertNode(root, 3, "2", new int[] {0,0,0,3,2,3,0,0,0,0}, "name", 5, null,
                 new String[][] {new String[] {"N/A"},
                                 new String[] {"peter"},
                                 new String[] {"per"},
                                 new String[] {"por"},
                                 new String[] {"par\\,","pir \\\"na"}});
      ParseTreeNode[] forks = root.getForks();

      assertNode(forks[0], 0, "1", null, null, 0, null, null);
      assertNode(forks[1], 0, "2", new int[] {0,0,0,0,2,0,0,0,0,0}, null, 0, null, null);
      assertNode(forks[2], 0, "3", new int[] {0,0,0,3,0,0,0,0,0,0}, null, 0, null, null);
      assertNode(forks[3], 0, "4", new int[] {0,0,0,0,0,3,0,0,0,0}, null, 0, null, null);
      assertNode(forks[4], 0, "5", null, null, 0, null, null);

   }

   public void testEx2() throws Exception {
      DecisionTreeParser parser = new DecisionTreeParser("ex2.tree");
      ParseTreeNode root = parser.parse();

      assertNode(root, 1, "2", new int[] {0,0,0,3,2,3,0,0,0,0}, "name", 4, null, null);
      ParseTreeNode[] forks = root.getForks();

      assertNode(forks[0], 0, "2", null, null, 0, null, null);
      assertNode(forks[1], 0, "3", new int[] {0,0,0,0,2,0,0,0,0,0}, null, 0, null, null);
      assertNode(forks[2], 0, "4", new int[] {0,0,0,3,0,0,0,0,0,0}, null, 0, null, null);
      assertNode(forks[3], 0, "5", new int[] {0,0,0,0,0,3,0,0,0,0}, null, 0, null, null);
   }

   public void testEx3() throws Exception {
      DecisionTreeParser parser = new DecisionTreeParser("ex3.tree");
      ParseTreeNode root = parser.parse();

      assertNode(root, 2, "5", new int[] {0,0,0,0,1,1,2,2,0,1}, "key_index", 3, "27", null);
      ParseTreeNode[] forks = root.getForks();

      assertNode(forks[0], 0, "5", null, null, 0, null, null);
      assertNode(forks[1], 0, "3", new int[] {0,0,0,0,1,0,0,0,0,1}, null, 0, null, null);
      assertNode(forks[2], 0, "2", new int[] {0,0,0,0,0,1,2,2,0,0}, null, 0, null, null);
   }

   public void testBig() throws Exception {
      DecisionTreeParser parser = new DecisionTreeParser("big");
      ParseTreeNode root = parser.parse();

      //too big to do all the cases. test only the root      
      assertNode(root, 2, "5", new int[] {0,0,39,34,45,5,46,31,25,13}, "thread_index", 3, "8", null);
   }

   public void testEx1Decision() throws Exception {
      ParseTreeNode root = new DecisionTreeParser("ex1").parse();
      Map<String, Feature> featureMap = new HashMap<String, Feature>();
      Feature feature = new NameListFeature("name", "peter", "per", "por", "par\\,", "pir \\\"na");
      featureMap.put(feature.getName(), feature);

      DecisionTreeBuilder builder = new DecisionTreeBuilder(featureMap);
      DecisionTree tree = builder.build(root);

      assert tree.query(Collections.<Feature, FeatureValue>emptyMap()) == 1;

      Map<Feature, FeatureValue> keyFeature = new HashMap<Feature, FeatureValue>();
      keyFeature.put(feature, feature.createFeatureValue("peter"));
      assert tree.query(keyFeature) == 2;

      keyFeature.put(feature, feature.createFeatureValue("per"));
      assert tree.query(keyFeature) == 3;

      keyFeature.put(feature, feature.createFeatureValue("por"));
      assert tree.query(keyFeature) == 4;

      keyFeature.put(feature, feature.createFeatureValue("par\\,"));
      assert tree.query(keyFeature) == 5;

      keyFeature.put(feature, feature.createFeatureValue("pir \\\"na"));
      assert tree.query(keyFeature) == 5;
   }

   public void testEx2Decision() throws Exception {
      ParseTreeNode root = new DecisionTreeParser("ex2").parse();
      Map<String, Feature> featureMap = new HashMap<String, Feature>();
      Feature feature = new NameListFeature("name", "name1", "name2", "name3");
      featureMap.put(feature.getName(), feature);

      DecisionTreeBuilder builder = new DecisionTreeBuilder(featureMap);
      DecisionTree tree = builder.build(root);

      assert tree.query(Collections.<Feature, FeatureValue>emptyMap()) == 2;

      Map<Feature, FeatureValue> keyFeature = new HashMap<Feature, FeatureValue>();
      keyFeature.put(feature, feature.createFeatureValue("name1"));
      assert tree.query(keyFeature) == 3;

      keyFeature.put(feature, feature.createFeatureValue("name2"));
      assert tree.query(keyFeature) == 4;

      keyFeature.put(feature, feature.createFeatureValue("name3"));
      assert tree.query(keyFeature) == 5;
   }

   public void testEx3Decision() throws Exception {
      ParseTreeNode root = new DecisionTreeParser("ex3").parse();
      Map<String, Feature> featureMap = new HashMap<String, Feature>();
      Feature feature = new NumericFeature("key_index");
      featureMap.put(feature.getName(), feature);

      DecisionTreeBuilder builder = new DecisionTreeBuilder(featureMap);
      DecisionTree tree = builder.build(root);

      assert tree.query(Collections.<Feature, FeatureValue>emptyMap()) == 5;

      Map<Feature, FeatureValue> keyFeature = new HashMap<Feature, FeatureValue>();
      keyFeature.put(feature, feature.createFeatureValue(27));
      assert tree.query(keyFeature) == 3;

      keyFeature.put(feature, feature.createFeatureValue(28));
      assert tree.query(keyFeature) == 2;

   }

   public void testBigDecision() throws Exception {
      DecisionTreeParser parser = new DecisionTreeParser("big");
      ParseTreeNode root = parser.parse();

      Map<String, Feature> featureMap = new HashMap<String, Feature>();
      Feature keyIndex = new NumericFeature("key_index");
      Feature threadIndex = new NumericFeature("thread_index");

      featureMap.put(keyIndex.getName(), keyIndex);
      featureMap.put(threadIndex.getName(), threadIndex);

      DecisionTreeBuilder builder = new DecisionTreeBuilder(featureMap);
      DecisionTree tree = builder.build(root);

      Map<Feature, FeatureValue> keyFeature = new HashMap<Feature, FeatureValue>();

      assert tree.query(keyFeature) == 5;

      keyFeature.put(keyIndex, keyIndex.createFeatureValue(2));

      assert tree.query(keyFeature) == 5;

      keyFeature.clear();
      keyFeature.put(threadIndex, threadIndex.createFeatureValue(7));

      assert tree.query(keyFeature) == 3;

      keyFeature.put(threadIndex, threadIndex.createFeatureValue(5));
      keyFeature.put(keyIndex, keyIndex.createFeatureValue(1841));

      assert tree.query(keyFeature) == 5;

      keyFeature.put(threadIndex, threadIndex.createFeatureValue(6));

      assert tree.query(keyFeature) == 2;

      keyFeature.put(threadIndex, threadIndex.createFeatureValue(9));
      keyFeature.put(keyIndex, keyIndex.createFeatureValue(44));

      assert tree.query(keyFeature) == 9;

      keyFeature.put(keyIndex, keyIndex.createFeatureValue(45));

      assert tree.query(keyFeature) == 5;

      //the deepest in the tree
      keyFeature.put(threadIndex, threadIndex.createFeatureValue(1));
      keyFeature.put(keyIndex, keyIndex.createFeatureValue(1117));

      assert tree.query(keyFeature) == 0;

      keyFeature.put(keyIndex, keyIndex.createFeatureValue(1118));

      assert tree.query(keyFeature) == 5;

      keyFeature.put(keyIndex, keyIndex.createFeatureValue(2334));

      assert tree.query(keyFeature) == 2;

      keyFeature.put(keyIndex, keyIndex.createFeatureValue(2335));

      assert tree.query(keyFeature) == 0;
   }

   private void assertNode(ParseTreeNode node, int type, String clazz, int[] freq, String att, int numberOfForks,
                           String cut, String[][] elts) {
      assert node != null;
      assert node.getType() == type;
      assert node.getClazz().equals(clazz);
      if (freq != null) {
         assert node.getFrequency() != null;
         assert freq.length == node.getFrequency().length;
         for (int i = 0; i < freq.length; ++i) {
            assert freq[i] == node.getFrequency()[i];
         }
      } else {
         assert node.getFrequency() == null;
      }
      if (att != null) {
         assert node.getAttribute() != null;
         assert att.equals(node.getAttribute());
      } else {
         assert node.getAttribute() == null;
      }
      assert node.getNumberOfForks() == numberOfForks;
      if (cut != null) {
         assert node.getCut() != null;
         assert cut.equals(node.getCut());
      } else {
         assert node.getCut() == null;
      }
      if (elts != null) {
         assert elts.length == node.getElts().length;
         for (int i = 0; i < elts.length; ++i) {
            List<String> eltsValues = node.getElts()[i].getValues();
            assert eltsValues.size() == elts[i].length;
            for (int j = 0; j < elts[i].length; ++j) {
               assert elts[i][j].equals(eltsValues.get(j));
            }
         }
      } else {
         assert node.getElts() == null;
      }
   }

}
