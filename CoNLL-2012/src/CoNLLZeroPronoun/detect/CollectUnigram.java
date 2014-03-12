package CoNLLZeroPronoun.detect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

import util.Common;

public class CollectUnigram {
	public static void main(String args[]) throws Exception{
		File folder = new File("/users/yzcchen/chen3/5-gram/5-gram");
		HashSet<String> unigram = Common.readFile2Set("unigram");
		unigram.add("<S>");
		unigram.add("</S>");
		int g = folder.listFiles().length;
		for(File f : folder.listFiles()) {
			System.out.println(f.getName() + "#" + g--);
			ArrayList<String> outputs = new ArrayList<String>();
			
			String outFn = "/users/yzcchen/chen3/filter-5-gram/5-gram/" + f.getName();
			
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			loop: while((line=br.readLine())!=null) {
				int k = line.indexOf("\t");
				String ts[] = line.substring(0, k).split("\\s+");
				for(String t : ts) {
					if(!unigram.contains(t)) {
						continue loop;
					}
				}
				outputs.add(line);
			}
			Common.outputLines(outputs, outFn);
			br.close();
		}
	
	}

	private static void unigram() {
		HashSet<String> unigrams = new HashSet<String>();
		
		File folder = new File("/users/yzcchen/workspace/CoNLL-2012/src/conll-source");
		for(File f : folder.listFiles()) {
			if(f.getAbsolutePath().endsWith(".token")) {
				ArrayList<String> lines = Common.getLines(f.getAbsolutePath());
				for(String line : lines) {
					String tokens[] = line.split("\\s+");
					for(String token : tokens) {
						unigrams.add(token);
					}
				}
			}
		}
		Common.outputHashSet(unigrams, "unigram");
	}
}
