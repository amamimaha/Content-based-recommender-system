
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


class Preprocessing {

    HashMap<String, ArrayList<String>[]> authors;
    HashMap<String, String> abstc;
    HashMap<String, String> reftest;
    String datapath;
    String abstractpath;
    String testpath;
    String authoredPath;
    String authorIndexPath;
    String refsPath;
    int minWritten;
    int minRefs;

    public Preprocessing(String datapath, String abstractpath, String testpath, String authoredPath, String refsPath, int minWritten, int minRefs) throws IOException {
        this.datapath = datapath;
        this.abstractpath = abstractpath;
        this.testpath = testpath;
        this.authoredPath = authoredPath;
        this.refsPath = refsPath;
        this.authors = new HashMap<String, ArrayList<String>[]>();
        this.abstc = new HashMap<String, String>();
        this.reftest = new HashMap<String, String>();
        this.minWritten = minWritten;
        this.minRefs = minRefs;
    }

    public void preprocessing () {
        try {
            System.out.println("Read author data");
            readAuthorData();
            System.out.println("Filter abstracts");
            filterAbstracts();
            System.out.println("Read abstracts");
            readAbstracts();
            System.out.println("Export data");
            exportData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void readAuthorData () throws IOException {
        // process dblp.txt which contains lines as follows
        //#* --- paper title
        //#@ --- authors
        //#t ---- year
        //#c  --- publication venue #index 00---- index id of this paper
        //#% ---- the id of references of this paper (there are multiple lines, with each indicating a reference)
        //#! --- abstract
        BufferedReader br = new BufferedReader(new FileReader(this.datapath));
        String coauthors;
        String index;
        String abstrac;
        ArrayList<String> references;
        coauthors = "";
        index = "";
        abstrac = "";
        references = new ArrayList<String>();
        int count = 0;
        while (true) {
            String line = br.readLine();
            if ((line == null || line.isEmpty())) {
                // process only articles with abstracts
                if (!abstrac.isEmpty()) {
                    count++;
                    if (count%100 == 0)
                        System.out.println("Processed "+count);
                    abstc.put(index, "");
                    // split line to co-authors and stores them in a hashmap with key name of author
                    String author[] = coauthors.split(",");
                    for (int i = 0; i < author.length; i++) {
                        // if the author exists in the hashmap "authors" add the reference
                        if (!author[i].isEmpty()) {
                            ArrayList<String>[] authorData;
                            if (authors.containsKey(author[i]))
                                authorData = authors.get(author[i]);
                            else
                                authorData = new ArrayList[]{new ArrayList<String>(), new ArrayList<String>()};
                            authorData[0].add(index);
                            for (String r : references)
                                if (!authorData[1].contains(r)) authorData[1].add(r);
                            authors.put(author[i], authorData);
                        }
                    }

                    // initialize "references" hashmap
                    coauthors = "";
                    index = "";
                    abstrac = "";
                    references = new ArrayList<String>();
                }
                if (line == null)
                    break;
            } else {
                // save the index of article
                if (line.startsWith("#index"))
                    index = line.replace("#index", "");
                else if (line.startsWith("#@"))
                    coauthors = line.replace("#@", "");
                else if (line.startsWith("#%"))
                    references.add(line.replace("#%", ""));
                else if (line.startsWith("#!"))
                    abstrac = line.replace("#!", "");
            }
        }
        br.close();
        System.out.println("Read authors: "+authors.size());
        // remove authors with less than 10 articles from the hashmap authors and rates and put text of articles in hashmap "abstc"
        Iterator<Map.Entry<String, ArrayList<String>[]>> it = authors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>[]> entry = it.next();
            ArrayList<String>[] authorData = entry.getValue();
            Iterator<String> it2 = authorData[1].iterator();
            while (it2.hasNext()) {
                if (!abstc.containsKey(it2.next()))
                    it2.remove();
            }

            // remove authors with less that 10 articles and authors
            if (authorData[0].size() < minWritten || authorData[1].size() < minRefs) {
                it.remove();
            }
        }
        System.out.println("Filtered authors: "+authors.size());
    }
    public void filterAbstracts () throws IOException {
        // read dblp.txt to get abstracts of authors and put them in hashmap "abstc"
        abstc = new HashMap<String, String>();
        Iterator<Map.Entry<String, ArrayList<String>[]>> it = authors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>[]> entry = it.next();
            ArrayList<String>[] authorData = entry.getValue();
            for (String w : authorData[0])
                abstc.put(w,"");
            for (String r : authorData[1])
                abstc.put(r,"");
        }
    }
    

    public void readAbstracts () throws IOException {
        // read dblp.txt to get abstracts of authors and put them in hashmap "abstc"
        BufferedReader br = new BufferedReader(new FileReader(this.datapath));
        String index;
        String abstrac;
        String title;
        index = "";
        abstrac = "";
        title= "";
        while (true) {
            String line = br.readLine();
            if (line == null || line.isEmpty()) {
                if (abstc.containsKey(index))
                    abstc.put(index, title+" "+abstrac);
                if (line == null)
                    break;
                index = "";
                abstrac = "";
                title = "";
            } else {
                if (line.startsWith("#index"))
                    index = line.replace("#index", "");
                else if (line.startsWith("#!"))
                    abstrac = line.replace("#!", "");
                else if (line.startsWith("#*"))
                    title = line.replace("#*", "");
            }
        }
        System.out.println("Read abstracts: "+abstc.size());
    }
    
    public void exportData () throws IOException {

        // write abstracts in another file with structure: "index" + "text of abstract"
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(abstractpath, false));
        Iterator<Map.Entry<String,String>> it = abstc.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            count++;
            if (count%100 == 0)
                System.out.println("Exported abstracts "+count);

            Map.Entry<String,String> entry = it.next();
            String key = entry.getKey();
            String abs = entry.getValue();
            bufferedWriter.write(key + ";" + abs);
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
        // write authors and rates
        int i = 1;
        Iterator<Map.Entry<String, ArrayList<String>[]>> it2 = authors.entrySet().iterator();
        // write indexes and rates of author in a separate file and store his written abstracs in another file
        while (it2.hasNext()) {
            if (i%100 == 0)
                System.out.println("Exported authors "+i+" out of "+authors.size());
            Map.Entry<String,ArrayList<String>[]> entry = it2.next();
            String key = entry.getKey();
            // get abstracts of the author from hashmap authors
            ArrayList<String>[] authorData = authors.get(key);
            // get rates of the author from hashmap rates
            ArrayList<String> written = authorData[0];
            ArrayList<String> refs = authorData[1];
            // write files with index of articles and rates of the author
            BufferedWriter authored = new BufferedWriter(new FileWriter(authoredPath + i, false));
            BufferedWriter references = new BufferedWriter(new FileWriter(refsPath + i, false));
            // write text abstracts of the author
            // write indexes of articles and index references in train/author i and text abstracts in train/abs i
            for (int j = 0; j < written.size(); j++) {
                authored.write(written.get(j));
                authored.newLine();
            }
            // write indexes of articles and index references in train/author i and text abstracts in train/abs i
            for (int j = 0; j < refs.size(); j++) {
                references.write(refs.get(j));
                references.newLine();
                if (written.contains(refs.get(j)) == false)
                    // save the reference of all the authors in a hashmap
                    reftest.put(refs.get(j), abstc.get(refs.get(j)));
            }
            references.close();
            authored.close();
            i++;
        }
        // write all the reference of the authors 
        BufferedWriter t = new BufferedWriter(new FileWriter(testpath, false));
        Iterator<Entry<String, String>> it3 = reftest.entrySet().iterator();
        // write indexes and rates of author in a separate file and store his written abstracs in another file
        while (it3.hasNext()) {
            Map.Entry<String,String> entry = it3.next();
            String key = entry.getKey();
            String testData = reftest.get(key);
            t.write(key + ";" + testData);
            t.newLine();
        }
    }
    

    // generates test set (references of the authors)
    HashSet<String> getRefs (String idsPath, String ratePath) {
        // get indexes of abstracts (written articles and references)  from train/author i and put them in a hashmap abstc
        HashSet<String> abstc = new HashSet<String>();
        try {
            String line;
            int i = 0;
            BufferedWriter w = new BufferedWriter(new FileWriter(ratePath));
            BufferedReader br = new BufferedReader(new FileReader(idsPath));
            while ((line = br.readLine()) != null && i < 15) {
            	w.write(line);
            	w.newLine();
                abstc.add(line);
                i++;
            }
            w.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return abstc;
    }
    
    void exportTest(String idPath, HashSet<String> refs, String refsPath) throws IOException{
    	BufferedWriter w = new BufferedWriter(new FileWriter(idPath));
    	ArrayList<String> rates = Util.readIds(refsPath);
    	for (int i = 0; i < rates.size(); i++){
    		if(refs.contains(rates.get(i))){
    			w.write(rates.get(i));
    			w.newLine();
    			}
    		}
    	w.close();
    		
    }
    
    HashSet<String> exportID (String dataPath) throws IOException{
    	HashSet<String> ids = new HashSet<String> ();
        BufferedReader br = new BufferedReader(new FileReader(dataPath));
        while (true) {
            String line = br.readLine();
            if ((line == null || line.isEmpty())){
                if (line == null)
                    break;
            }
            else{
            	String t[] = line.split("\t");
                ids.add(t[0]);
            }
        }
        br.close();
        return ids;
    }
    
    
    HashSet<String> exportRefs (HashSet<String> abstc, String dataPath, String abstractPath){
    	HashSet<String> rates = new HashSet<String> ();
    	try {
        	this.testpath = dataPath;
            BufferedWriter w = new BufferedWriter(new FileWriter(dataPath));
            BufferedReader br = new BufferedReader(new FileReader(abstractPath));
            String line;
            while ((line = br.readLine()) != null) {
                String t[] = line.split(";");
                if (abstc.contains(t[0])){
                    w.write(t[0] + "\t" + t[1]);
                    w.newLine();
                    rates.add(t[0]);
                }
            }
            br.close();
            w.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return rates;
    }
    
    
    void exportRefs (HashSet<String> abstc, String dataPath, String abstractPath, String addedPath){
        try {
        	this.testpath = dataPath;
        	String line;
        	BufferedReader e = new BufferedReader(new FileReader(addedPath));
        	while ((line = e.readLine()) != null) 
        		abstc.add(line);
        	e.close();
            BufferedWriter w = new BufferedWriter(new FileWriter(dataPath));
            BufferedReader br = new BufferedReader(new FileReader(abstractPath));
            
            while ((line = br.readLine()) != null) {
                String t[] = line.split(";");
                if (abstc.contains(t[0])){
                    w.write(t[0] + "\t" + t[1]);
                    w.newLine();
                }
            }
            br.close();
            w.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    HashMap<String, String> loadData (String abstractPath) throws IOException{
    	HashMap<String,String> abs = new  HashMap<String, String> ();
    	String line;
        BufferedReader br = new BufferedReader(new FileReader(abstractPath));
        while ((line = br.readLine()) != null){
            String t[] = line.split(";");
            abs.put(t[0], t[1]);
            }
        br.close();
        return abs;
		}
    }

