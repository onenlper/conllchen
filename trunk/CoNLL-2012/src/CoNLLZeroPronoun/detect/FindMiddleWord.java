package CoNLLZeroPronoun.detect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class FindMiddleWord {
	
	public static void main(String args[]) {
		HashMap<String, HashMap<String, Integer>> gram4MapWildDetail = new HashMap<String, HashMap<String, Integer>>();
		ArrayList<String> processlines = Common.getLines("4-grams-with-wildcard-count-detail");
		
		HashSet<String> middleWord = new HashSet<String>();
		
		HashMap<String, String> posMap = Common.readFile2Map2("POS.txt", '\t');
		
		HashSet<String> posSet = new HashSet<String>();
		
		HashSet<String> keepPOS = new HashSet<String>(Arrays.asList("f", "j", "m", "n", "q", "r", "s", "Ng", "Tg", "nr", "ns", "nt", "nz"));
		
		loop: for (String line : processlines) {

			int a = line.indexOf("\t");
			int b = line.lastIndexOf(" ");
			String key = line.substring(0, a);
			String subKey = line.substring(a + 1, b);
			
			String ts[] = key.split("\\s+");
			String ts2[] = subKey.split("\\s+");
			for(int i=0;i<ts2.length;i++) {
				if(ts[i].equals("*")) {
					middleWord.add(ts2[i]);
//					System.out.println(ts2[i]);
					
					String middle = ts2[i];
					if(posMap.containsKey(middle)) {
						String poses[] = posMap.get(middle).split("-");
						boolean keep = true;
						for(String pos : poses) {
							if(!keepPOS.contains(pos)) {
								keep = false;
							}
						}
						if(!keep) {
							continue loop;
						}
					}
					
					String sems[] = Common.getSemantic(middle);
					if(sems!=null) {
						for(String sem : sems) {
							if(sem.startsWith("K")) {
								continue loop;
							}
						}
					}
					
				}
			}
			
			int value = Integer.valueOf(line.substring(b+1));
			
			HashMap<String, Integer> map = gram4MapWildDetail.get(key);
			if(map ==null) {
				map = new HashMap<String, Integer>();
				gram4MapWildDetail.put(key, map);
			}
			map.put(subKey, value);
		}
		Common.outputHashSet(middleWord, "middleWord");
		
		HashMap<String, Integer> newWildMap = new HashMap<String, Integer>();
		ArrayList<String> outputLines = new ArrayList<String>();
		for(String key : gram4MapWildDetail.keySet()) {
			int val = 0;
			for(String subkey : gram4MapWildDetail.get(key).keySet()) {
				val += gram4MapWildDetail.get(key).get(subkey);
				
				outputLines.add(key + "\t" + subkey + " " + gram4MapWildDetail.get(key).get(subkey));
			}
			newWildMap.put(key, val);
		}
		
		Common.outputLines(outputLines, "4-grams-with-wildcard-count-detail.new");
		Common.outputHashMap(newWildMap, "4-grams-with-wildcard-count.new");
		
		System.out.println(posSet);
	}
}
