package ace.event.coref;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class PoorClassifier {
	public static void main(String args[]) {
		if(args.length<1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Common.part = args[0];
		
		HashMap<String, Integer> stringFeas = Common.readFile2Map("eventStrFea" + Common.part);
		
		HashMap<Integer, String> stringFeas2 = new HashMap<Integer, String>();
		for(String key : stringFeas.keySet()) {
			stringFeas2.put(stringFeas.get(key), key);
		}
		
		String outputFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" + args[0] + "/";
		ArrayList<String> train = Common.getLines(outputFolder + "train.feature2");
		
		ArrayList<String> allText = Common.getLines(outputFolder + "all.txt");
		
		for(String file : allText) {
			ArrayList<String> test = Common.getLines(file + ".feature");
			ArrayList<String> predicts = new ArrayList<String>();
			
			ArrayList<String> mpextent = Common.getLines(file + ".mpextent");
			
			
			HashMap<String, Integer> map = loadGoldEntity(file);
			
			for(int k=0;k<test.size();k++) {
				String line = test.get(k);
				String tokens[] = mpextent.get(k).split(",");
				String m1 = tokens[0] + "," + tokens[1];
				String m2 = tokens[2] + "," + tokens[3];
					
				if(train.contains(line)) {
					StringBuilder sb = new StringBuilder();
					sb.append(map.containsKey(m1)).append(" ").append(map.containsKey(m2));
					
					tokens = line.split("\\s+");
					for(String token : tokens) {
						if(token.contains(":")) {
							int idx = Integer.parseInt(token.split(":")[0]);
							sb.append(stringFeas2.get(idx)).append(" ");
						}
					}
					System.err.println(sb.toString()+"\n");
					predicts.add("1");
				} else {
					predicts.add("-1");
				}
			}
			Common.outputLines(predicts, file + ".mppred");
		}
	}

	private static HashMap<String, Integer> loadGoldEntity(String file) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		ArrayList<String> entities = Common.getLines(file + ".entities.golden.event");
		for(int i=0;i<entities.size();i++) {
			String entity = entities.get(i);
			String tokens[] = entity.split("\\s+");
			for(String token : tokens) {
				map.put(token, i);
			}
		}
		return map;
	}
	
	
	
}
