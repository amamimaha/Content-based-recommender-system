import cc.mallet.pipe.*;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

class MalletHelper {
    private int optimizeInterval = 50;
    private int nIterations = 1000;
    private int nThread;
    private int id;
    private String authoredPath;
    private String refsPath;
    private String trainPath;
    private String testPath;
    private String stopwordsPath;
    private String resultPath;
    private int numTopics;
    private String ratePath;
    private HashMap<String, String> abst = new HashMap<String, String>();
    private InstanceList references;
    
    public MalletHelper(int nThread, int id, String authoredPath, String refsPath,
                        String trainPath, String testPath, HashMap<String, String> abst, String stopwordsPath, String resultPath, String ratePath, int numTopics) throws IOException {
        this.nThread = nThread;
        this.id = id;
        this.stopwordsPath = stopwordsPath;
        this.authoredPath = authoredPath;
        this.refsPath = refsPath;
        this.trainPath = trainPath;
        this.testPath = testPath;
        this.resultPath = resultPath;
        this.numTopics = numTopics;
        this.abst = abst;
        this.ratePath = ratePath;
        loadTrainData(authoredPath, trainPath, abst);
        this.references = getReferences(trainPath);
    }


    void loadData (String idsPath, String dataPath, HashMap <String,String> abst) throws IOException{
        // get indexes of abstracts (written articles and references)  from train/author i and put them in a hashmap abstc
        String line;
        BufferedWriter w = new BufferedWriter(new FileWriter(dataPath));
        BufferedReader br = new BufferedReader(new FileReader(idsPath));
        while ((line = br.readLine()) != null) {
        	if (abst.containsKey(line)){
        		w.write(abst.get(line));
        		w.newLine();
        		}
            }
            br.close();
            w.close();
            }
    
    int loadTrainData (String idsPath, String dataPath, HashMap <String,String> abst) throws IOException{
        String line;
        BufferedReader br = new BufferedReader(new FileReader(idsPath));
        int count = 0;
        while ((line = br.readLine()) != null) {
        	if (abst.containsKey(line)){
        		count++;
        	}      	
        }
        br.close();
        BufferedWriter w = new BufferedWriter(new FileWriter(dataPath));
        BufferedReader b = new BufferedReader(new FileReader(idsPath));
        while ((line = b.readLine()) != null) {
        	if (abst.containsKey(line)){
        		w.write(abst.get(line));
        		w.newLine();
        		}
            }
            b.close();
            w.close();
            return count;
            }
    
    

    public InstanceList getReferences(String filePath) throws IOException {
        Pipe pipe = readDataPipe();
        return getInstances(pipe, filePath);
        }
    

    public Pipe readDataPipe() {
        ArrayList pipeList = new ArrayList();
        pipeList.add(new Target2Label());
        return new SerialPipes(pipeList);
        }
    
    public InstanceList loadInstances (Pipe p){
 	   Iterator<Entry<String, String>> it = abst.entrySet().iterator();
       InstanceList collectionIsntances = new InstanceList (p);
       while (it.hasNext()) {
            String s = (String) it.next().getValue();
            Instance inst = new Instance(new StringReader(s),"",null,null);       
            collectionIsntances.addThruPipe(inst);
            }    
        return collectionIsntances;
        }
    
    public InstanceList getInstances(Pipe pipe, String filePath) throws IOException {
        InstanceList instances = new InstanceList (pipe);
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = br.readLine()) != null) {
            Instance i = new Instance(new StringReader(line),"",null,null);   
            instances.addThruPipe(i);
            }
        br.close();
        return instances;
        }
    
   
   public void trainAndExportAll() throws Exception {   
	   Pipe pipe = preprocessDataPipe(stopwordsPath);
	   InstanceList trainingInstances = getTopicInstances(references, pipe);
	   // save instances
	   trainingInstances.save(new File(resultPath + "-mallet-data.bin"));
	   ParallelTopicModel ldaModel = trainTopicModel(trainingInstances,numTopics,nIterations, optimizeInterval);
	   ldaModel.write(new File(resultPath + "-mallet-model.bin"));
	   }
    

    public Pipe preprocessDataPipe(String stopwords) {
        ArrayList pipeList = new ArrayList();
        pipeList.add(new Input2CharSequence("UTF-8"));
        Pattern tokenPattern = Pattern.compile("\\p{L}+");
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        pipeList.add(new TokenSequenceLowercase());
        if (stopwords != null)
            pipeList.add(new TokenSequenceRemoveStopwords(new File(stopwords), "UTF-8", false, false, false));
        int [] gramSizes = {1,2};
        pipeList.add(new TokenSequenceNGrams(gramSizes));
        pipeList.add(new TokenSequence2FeatureSequence());
        return new SerialPipes(pipeList);
        }

    public InstanceList getTopicInstances(InstanceList instances,Pipe pipe) {
        InstanceList newInstances = new InstanceList (pipe);
        for (int i = 0; i<instances.size(); i++) {
            Instance instance = (Instance)instances.get(i).clone();
            instance.unLock();
            newInstances.addThruPipe(instance);
            }
        return newInstances;
        }

    // Topic Model model train
    public ParallelTopicModel trainTopicModel(InstanceList instances,int nTopics,int nIterations, int optimizeInterval) throws IOException {
        ParallelTopicModel model = new ParallelTopicModel(nTopics, 50, 0.01);
        model.addInstances(instances);
        model.optimizeInterval = optimizeInterval;
        model.setNumThreads(nThread);
        model.setNumIterations(nIterations);
        model.setTopicDisplay(0, 0);
        model.estimate();
        return model;
    }

    public void test() throws Exception { 	
        InstanceList previousInstanceList = InstanceList.load (new File(resultPath+"-mallet-data.bin"));
        Pipe instancePipe = previousInstanceList.getPipe();
        ParallelTopicModel ldaModel = ParallelTopicModel.read(new File(resultPath+"-mallet-model.bin"));
        double[][] topicWordWeights = getTopicWordWeights(ldaModel);
        double[][] documentTopicWeights = getDocumentTopicWeights(ldaModel, previousInstanceList);
        double[] occ = Util.getRelativeCollection(previousInstanceList, topicWordWeights[0].length);
        InstanceList instances = new InstanceList (instancePipe);
        BufferedReader br = new BufferedReader(new FileReader(testPath));
        String line;
        HashMap<Instance, String> abstc = new HashMap<Instance, String> ();
        while ((line = br.readLine()) != null) {
        	String t[] = line.split("\t");
            Instance inst = new Instance(new StringReader(t[1]),"",null,null);
            abstc.put(inst,t[0]);
            instances.addThruPipe(inst);
            }
        br.close();
        HashMap<String, Double> rates = new HashMap<String, Double> ();
        for (int model = 4; model <= 4; model++) {
            for (Instance inst : instances) {
                double s = 0;
                switch (model) {
                   case 1:
                        s = Models.getSimilarityScenario1(inst, topicWordWeights, documentTopicWeights);
                        break;
                   case 2:
                        s = Models.getSimilarityScenario2(inst, topicWordWeights, documentTopicWeights);
                        break;
                   case 3:
                        s = Models.getSimilarityScenario3(inst, topicWordWeights, occ);
                        break;
                   case 4:
                        s = Models.getSimilarityScenario4(inst, ldaModel, topicWordWeights, documentTopicWeights);
                        break;
                }
                rates.put(abstc.get(inst), s);
                
            }

            ValueComparator comparateur = new ValueComparator(rates);
            TreeMap<String, Double> mapTriee = new TreeMap<String, Double>(comparateur);
            mapTriee.putAll(rates);
            double max = mapTriee.values().stream().max(Double::compare).get();
            double min = mapTriee.values().stream().min(Double::compare).get();
            PrintWriter writer = new PrintWriter(resultPath + "-mallet-result-model-"+model+".txt");
            Iterator<Map.Entry<String, Double>> it = mapTriee.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Double> entry = it.next();
                String key = entry.getKey();
                Double s = entry.getValue();
                //s = ((s - min) / (max - min));
                writer.println(key + "\t" + s);
            }
            writer.close();
        }
    }
    
    
    

    public HashMap<Integer,HashMap<Integer,Double>> evaluate(){
        HashMap<Integer,Double> statistics = new HashMap<Integer,Double>();
        try{
           double p = 0.0;
           for (int model = 4; model <= 4; model++) {
                ArrayList<String> rates = Util.readIds(ratePath);
                ArrayList<String> abs = Util.readIds(authoredPath);
                // write authors and rates
                int i = 0;
                double freq = 0;
                BufferedReader brf = new BufferedReader(new FileReader(resultPath + "-mallet-result-model-"+model+".txt"));
                while (true) {
                    String line = brf.readLine();
                    if ((line == null || line.isEmpty())) {
                        if (line == null)
                            break;
                    } else {
                        String t[] = line.split("\t");
                        if (!abs.contains(t[0])) {
                            if (i < 100 && rates.contains(t[0]))
                                freq++;
                            i++;
                        }       
                    }
                }
                statistics.put(model, freq/ rates.size());
                p = (double) rates.size() / i;
                brf.close();
            }
        }        catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<Integer,HashMap<Integer,Double>> ret = new HashMap<Integer,HashMap<Integer,Double>>();
        ret.put(id,statistics);
        return ret;
    }

    

    public HashMap<Integer,HashMap<Integer,Double>> evaluate_coverage(){
        HashMap<Integer,Double> statistics = new HashMap<Integer,Double>();
        try{
           double p = 0.0;

           for (int model = 1; model <= 1; model++) {
                ArrayList<String> rates = Util.readIds(ratePath);
                ArrayList<String> abs = Util.readIds(authoredPath);
                // write authors and rates
                int i = 0;
                BufferedReader brf = new BufferedReader(new FileReader(resultPath + "-mallet-result-model-"+model+".txt"));
                while (true) {
                    String line = brf.readLine();
                    if ((line == null || line.isEmpty())) {
                        if (line == null)
                            break;
                    } else {
                        String t[] = line.split("\t");
                        
                        if (!abs.contains(t[0])) {
                            if (i < 100){
                               if (Starter.ids.contains(t[0]) == false) {
                            	   Starter.ids.add(t[0]);
                               }
                            }
                            i++;
                        }       
                    }
                }
                brf.close();
            }
        }        catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<Integer,HashMap<Integer,Double>> ret = new HashMap<Integer,HashMap<Integer,Double>>();
        ret.put(id,statistics);
        return ret;
    }
    public HashMap<Integer,HashMap<Integer,Double>> evaluate_NDCG(){
        HashMap<Integer,Double> statistics = new HashMap<Integer,Double>();
        try{
           double p = 0.0;
           for (int model = 1; model <= 1; model++) {
                ArrayList<String> rates = Util.readIds(ratePath);
                ArrayList<String> abs = Util.readIds(authoredPath);
                // write authors and rates
                int i = 1;
                double fdis = 0;
                double idcg = 1;
                System.out.println(resultPath);
                BufferedReader brf = new BufferedReader(new FileReader(resultPath + "-mallet-result-model-"+model+".txt"));
                while (true) {
                    String line = brf.readLine();
                    if ((line == null || line.isEmpty())) {
                        if (line == null)
                            break;
                    } else {
                        String t[] = line.split("\t");
                        double relevance = Double.parseDouble(t[1]);
                        if (!abs.contains(t[0])) {
                            if (i <= 100 && rates.contains(t[0]))
                            	if (i <= 1)
                            		fdis = relevance + fdis;
                            	else{
                            		fdis = fdis + relevance / Math.log(i);
                            		idcg = idcg + 1 / Math.log(i);
                            		}
                           
                            i++;
                        }       
                    }
                }

                statistics.put(model, fdis/idcg);
                p = (double) rates.size() / i;
                brf.close();
            }
        }        catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<Integer,HashMap<Integer,Double>> ret = new HashMap<Integer,HashMap<Integer,Double>>();
        ret.put(id,statistics);
        return ret;
    }

    
    class ValueComparator implements Comparator<String> {
        Map<String, Double> base;
        public ValueComparator(Map<String, Double> base) {
            this.base = base;
        }

        public int compare(String a, String b) {
            if (base.get(a)<= base.get(b)) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
    
    
    public double[][] getDocumentTopicWeights(ParallelTopicModel ldaModel, InstanceList previousInstanceList){
        double[][] documentTopicWeights = new double[previousInstanceList.size()][ldaModel.numTopics];
        for (int d = 0; d < previousInstanceList.size(); d++){
            double [] t = ldaModel.getTopicProbabilities(d);
            for (int topic = 0; topic < ldaModel.numTopics; topic++)
                documentTopicWeights[d][topic] = t[topic];
            }
        return documentTopicWeights;
        }
    
    public double[][] getTopicWordWeights(ParallelTopicModel ldaModel) throws IOException{
        // Probably not the most efficient way to do this...
        double[][] topicWordWeights = new double[ldaModel.numTopics][ldaModel.numTypes];
        double weight = 0.01;
        for (int topic = 0; topic < ldaModel.numTopics; topic++)
            for (int type = 0; type < ldaModel.numTypes; type++)
                topicWordWeights[topic][type] = weight;
        for (int type = 0; type < ldaModel.numTypes; type++) {
            int[] topicCounts = ldaModel.typeTopicCounts[type];
            int index = 0;
            while (index < topicCounts.length &&
                    topicCounts[index] > 0) {
                int currentTopic = topicCounts[index] & ldaModel.topicMask;
                topicWordWeights[currentTopic][type]  += topicCounts[index] >> ldaModel.topicBits;
                index++;
            }
        }

        double[] sumTopic = new double[ldaModel.numTopics];
        for (int topic = 0; topic < ldaModel.numTopics; topic++)
            for (int type = 0; type < ldaModel.numTypes; type++)
                sumTopic[topic] += topicWordWeights[topic][type];
        for (int topic = 0; topic < ldaModel.numTopics; topic++)
            for (int type = 0; type < ldaModel.numTypes; type++)
                topicWordWeights[topic][type] /= sumTopic[topic];
        return topicWordWeights;
    }
    
}