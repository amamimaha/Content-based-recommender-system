import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Starter {
	static int numTopics = 10;
	static int numUsers = 1000;
	static int minWritten = 20;
	static int minRefs = 40;
	static String basePath = "C:/Users/Maha/eclipse-workspace/Recommendation/data/";
	static String dblpPath = basePath+"dblp.txt";
	static String abstractPath = basePath+"abstracts.txt";
	static String authoredPath = basePath+"userdata/authored";
	static String refsPath = basePath+"userdata/refs";
	static String ratePath = basePath+"test/rates";
	static String idPath = basePath+"test/refs";
	static String trainPath = basePath+"train/user";
	static String train1Path = basePath+"train/";
	static String test1Path = basePath+"test/test1.txt";
	static String test2Path = basePath+"test/test2.txt";
	static String stopwordsPath = basePath+"en.txt";
	static String resultPath = basePath+"result/author";
	static String statisticsPath = basePath+"precision.txt";
    static ArrayList ids = new ArrayList();
    static double sum = 0;
	public static void main(String[] args) throws Exception {
		Preprocessing p = new Preprocessing(dblpPath, abstractPath, test1Path, authoredPath, refsPath, minWritten, minRefs);
		if (false) {
			p.preprocessing();
			}
		ArrayList<Integer> sampledAuthors;
		if (true) {
			int numAuthors = Util.getNumAuthors(authoredPath);
			System.out.println(numAuthors+" authors found");
			if (true){
				sampledAuthors = Util.sample(numAuthors, numUsers);
				System.out.println(numUsers+" authors sampled");
				}
			else 
				sampledAuthors = Util.getFileNames(train1Path);
			if (true){
				HashSet<String> refs = new HashSet<String>();
				int count = 0;
				for (Integer i : sampledAuthors) {
					if ((++count)%10 == 0) System.out.println(count+" authors exported");
					refs.addAll(p.getRefs(refsPath + i, ratePath + i));			
					}
				System.out.println("num ratings: "+refs.size());
				p.exportRefs(refs, test2Path, abstractPath);
			}
			/*HashSet<String> a = p.exportID(test2Path);
			for (Integer i : sampledAuthors){
				p.exportTest(idPath + i, a, refsPath + i);
				}*/
			HashMap<String, String> abst = p.loadData(abstractPath);
			HashMap<Integer,HashMap<Integer,Double>> statistics = new HashMap<Integer,HashMap<Integer,Double>>();
			for (Integer i : sampledAuthors) {
				System.out.println(i);
				MalletHelper mh = new MalletHelper(1, i, authoredPath + i, refsPath + i, trainPath + i,
						test2Path, abst, stopwordsPath, resultPath + i, ratePath + i, numTopics);
			mh.trainAndExportAll();
			//mh.test();
			//statistics.putAll(mh.evaluate());
			}
			System.out.println(ids.size());
			Util.writeStatistics(statisticsPath, statistics);
			}
	}
}
