import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by Marco on 24/05/2015.
 */
public class Models {

	public static double getSimilarityScenario1(Instance testinst, double[][] topicWordWeights, double[][] documentTopicWeights) {
		 double value = 0.0;
		 // get document features
		 double[] occ = Util.getRelativeFrequencies(testinst, topicWordWeights[0].length);
		 // calculate weights of each feature
		 ArrayList<Double> weight = new ArrayList<Double>();
		 for (int t = 0; t < topicWordWeights.length; t++) {
			double p_wt = 0;
	        for (int i = 0; i < topicWordWeights[0].length; i++) {
	            if (occ[i] > 0){
					p_wt = Math.log(topicWordWeights[t][i]) * occ[i] + p_wt;
					}
	            }
	        weight.add(p_wt);
			}
       double max = Collections.max(weight);
       System.out.println(max);
       value  = -max;
       if (value == 0.0)
       	value = Double.POSITIVE_INFINITY;
       return value;
	    }

    public static double getSimilarityScenario2(Instance testinst, double[][] topicWordWeights, double[][] documentTopicWeights) {
        double value = 0.0;
        // get document features
        double[] occ = Util.getRelativeFrequencies(testinst, topicWordWeights[0].length);
        // calculate weights of each feature
        for (int i = 0; i < occ.length; i++) {
            if (occ[i] > 0) {
                double p_wt = 0;
                for (int t = 0; t < topicWordWeights.length; t++) {
                    // sum p(k|d) / number of test documents
                    double p_kt = 0;
                    for (int j = 0; j < documentTopicWeights.length; j++)
                        p_kt += documentTopicWeights[j][t];
                    p_kt = p_kt / documentTopicWeights[t].length;
                    // p(w|k) * p(k|t)
                    p_wt = topicWordWeights[t][i] * p_kt + p_wt;
                    }
                // get the maximum of weights of the feature
                // sum (max * occurrence of feature)
                if (p_wt > 0)
                    value += Math.log(p_wt) * occ[i];
                }
            }
        value = - value;
        if (value == 0.0)
            value = Double.POSITIVE_INFINITY;
        return value;
        }

    public static double getSimilarityScenario3(Instance inst, double[][] topicWordWeights, double[] occ) {
        double weight = 0.000001;
        FeatureSequence fs = (FeatureSequence) inst.getData();
        int[] features = fs.getFeatures();
        // calculating the language model of the test instance
        double[] languageModel = new double[topicWordWeights[0].length];
        double sum = 0;
        for (int i = 0; i < features.length; i++) {
            if (features[i] < topicWordWeights[0].length) {
                languageModel[features[i]]++;
                sum++;
				}
			}
        for (int i = 0; i < languageModel.length; i++){
            languageModel[i] = (languageModel[i] + weight * occ[i]) / (sum + weight);           
            }    
        // compute KL between languageModel and each topic distribution
        ArrayList<Double> weights = new ArrayList<Double>();
        double value = 0.0;
        for (int t = 0; t < topicWordWeights.length; t++) {  	
            value = Util.computeSimKLdemi(topicWordWeights[t], languageModel);
            weights.add(value);
            }
        double min = Collections.min(weights);
        return min;
        }
    
    public static double getSimilarityScenario4(Instance inst, ParallelTopicModel ldaModel, double[][] topicWordWeights, double[][] documentTopicWeights) {
        double value = 0.0;      
        TopicInferencer inferencer = ldaModel.getInferencer();
        ArrayList<Double> weight = new ArrayList<Double>();
        double[] testProbabilities = inferencer.getSampledDistribution(inst, 1000, 1, 5);
        for (int j = 0; j < documentTopicWeights.length; j++){
        	value = Util.computeKL( documentTopicWeights[j], testProbabilities);
            weight.add(value);
            }
        return Collections.min(weight);
        } 
    }
