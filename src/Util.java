import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.*;
import java.util.*;

/**
 * Created by Marco on 24/05/2015.
 */
public class Util {

    static double[] getRelativeFrequencies(Instance testinst, int len) {
        FeatureSequence fs = (FeatureSequence) testinst.getData();
        int[] features = fs.getFeatures();
        double[] occ = new double[len];
        int count = 0;
        for(int i = 0; i < features.length;i++) {
            if (features[i] < occ.length) {
                occ[features[i]]++;
                count++;
                }
            }
        for(int i = 0; i < occ.length;i++)
            occ[i] /=  count;
        return occ;
        }
    
    static double[] getIDF(InstanceList instances, int len){
    	double[] occ = new double[len];
		int count = 0;
		for (int j = 0; j < instances.size(); j++){
    		FeatureSequence fs = (FeatureSequence) instances.get(j).getData();
    		int[] features = fs.getFeatures();
    		for(int i = 0; i<features.length;i++){
    			if (features[i] < len){
    				occ[features[i]]++;
    				count++;
    				}
    			}
    		}
    	for(int i = 0; i < occ.length; i++){
			occ[i]/=count;
			}
    	return occ;
    	}
    
    static double[] getRelativeCollection(InstanceList instances, int len){
    	double[] occ = new double[len];
		int count = 0;
		for (int j = 0; j < instances.size(); j++){
    		FeatureSequence fs = (FeatureSequence) instances.get(j).getData();
    		int[] features = fs.getFeatures();
    		for(int i = 0; i<features.length;i++){
    			if (features[i] < len){
    				occ[features[i]]++;
    				count++;
    				}
    			}
    		}
    	for(int i = 0; i < occ.length; i++){
			occ[i]/=count;
			}
    	return occ;
    	}

    static ArrayList<Integer> getFileNames (String path){
        ArrayList <Integer> results = new ArrayList<Integer>();
        System.out.println(path);
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                results.add(Integer.parseInt(file.getName().replace("user", "").replace(".bin", "")));
            }
        }
        return results;
    }
    static int getNumAuthors(String authoredPath) {
        File f = new File(authoredPath.replace("authored",""));
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(f.listFiles()));
        return files.size()/2;
    }

    static ArrayList<Integer> sample(int range, int size) {
        ArrayList<Integer> sample = new ArrayList<Integer>();
        Random randomGenerator = new Random(0);
        while (sample.size() < size){
            int randomInt = randomGenerator.nextInt(range);
            if (!sample.contains(randomInt) && randomInt != 0)
                sample.add(randomInt);
        }
        return sample;
    }

    public static double computeSimKLdemi(double[] v1,double[] v2) {
        double s = (double) 1/2 * computeKL(v1,v2) + (double) 1/2 * computeKL(v2,v1);
        return s;
    }

    public static double computeKL(double[] v1,double[] v2) {
        double value = 0.0;
        for (int i = 0; i < v1.length; i++) {
            value += v1[i]*Math.log(v1[i]/v2[i]);
            }
        return value;
    }
    public static ArrayList<String> readIds(String idsPath) throws IOException {
        ArrayList<String> ids = new ArrayList<String> ();
        BufferedReader br = new BufferedReader(new FileReader(idsPath));
        while (true) {
            String line = br.readLine();
            if ((line == null || line.isEmpty())){
                if (line == null)
                    break;
            }
            else{
                ids.add(line);
            }
        }
        br.close();
        return ids;
    }
    public static void writeStatistics(String statisticsPath, HashMap<Integer,HashMap<Integer,Double>> statistics) throws FileNotFoundException {
        HashMap<Integer,Double> overall = new HashMap<Integer,Double>();
        StringBuffer sb = new StringBuffer();
        String tb = new String("\t");
        String nl = System.getProperty("line.separator");
        for (Map.Entry<Integer,HashMap<Integer,Double>> entry : statistics.entrySet()) {
            Integer id = entry.getKey();
            for (Map.Entry<Integer, Double> entry2 : entry.getValue().entrySet()) {
                sb.append(id).append(tb).append(entry2.getKey()).append(tb).append(entry2.getValue()).append(nl);
                if (overall.containsKey(entry2.getKey()))
                    overall.put(entry2.getKey(),overall.get(entry2.getKey())+entry2.getValue());
                else
                    overall.put(entry2.getKey(),entry2.getValue());
            }
        }
        for (Map.Entry<Integer, Double> entry2 : overall.entrySet()) {
            sb.append("average").append(tb).append(entry2.getKey()).append(tb).append(entry2.getValue()/statistics.size()).append(nl);
        }
        PrintWriter writer = new PrintWriter(statisticsPath);
        writer.write(sb.toString());
        writer.close();
    }
}
