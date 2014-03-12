package CoNLLZeroPronoun.detect;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class Fill4GramCount {
	public static void main(String args[]) {
		if (args[0].equals("wild")) {
			fill4gramWild();
		} else {
			fill3gram();
		}
	}

	private static void fill4gramWild() {
		HashSet<String> set = Common.readFile2Set("4-grams-with-wildcard");
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		HashMap<String, Integer> detailMap = new HashMap<String, Integer>();

		String folder = "/users/yzcchen/chen3/5-gram/4-gram/";
		int k = (new File(folder)).listFiles().length;
		for (File f : (new File(folder)).listFiles()) {
			System.out.println(f.getName() + "#" + k--);
			HashMap<String, Integer> gram4 = Common.readFile2Map(f.getAbsolutePath(), '\t');
			for (String key : gram4.keySet()) {

				String tokens[] = key.split("\\s+");
				int value = gram4.get(key);
				
				if(tokens.length!=4) {
					Common.bangErrorPOS("Not 4 gram??");
				}
				
				String p1 = "*" + " " + tokens[1] + " " + tokens[2] + " " + tokens[3];
				String p2 = tokens[0] + " " + "*" + " " + tokens[2] + " " + tokens[3];
				String p3 = tokens[0] + " " + tokens[1] + " " + "*" + " " + tokens[3];

				if (set.contains(p1)) {
					Integer val = map.get(p1);
					if (val == null) {
						map.put(p1, value);
					} else {
						map.put(p1, val.intValue() + value);
					}
					detailMap.put(p1 + "\t" + key, value);
				}

				if (set.contains(p2)) {
					Integer val = map.get(p2);
					if (val == null) {
						map.put(p2, value);
					} else {
						map.put(p2, val.intValue() + value);
					}
					detailMap.put(p2 + "\t" + key, value);
				}

				if (set.contains(p3)) {
					Integer val = map.get(p3);
					if (val == null) {
						map.put(p3, value);
					} else {
						map.put(p3, val.intValue() + value);
					}
					detailMap.put(p3 + "\t" + key, value);
				}

			}
		}
		Common.outputHashMap(map, "4-grams-with-wildcard-count");
		Common.outputHashMap(detailMap, "4-grams-with-wildcard-count-detail");
	}

	private static void fill3gram() {
		HashSet<String> set = Common.readFile2Set("3-grams");
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (String key : set) {
			map.put(key, 0);
		}
		String folder = "/users/yzcchen/chen2/5-gram/trigram";
		int k = (new File(folder)).listFiles().length;
		for (File f : (new File(folder)).listFiles()) {
			System.out.println(f.getName() + "#" + k--);
			HashMap<String, Integer> gram4 = Common.readFile2Map(f.getAbsolutePath(), '\t');
			for (String key : gram4.keySet()) {
				if (map.containsKey(key)) {
					map.put(key, gram4.get(key));
				}
			}
		}
		Common.outputHashMap(map, "3-grams-count");
	}
}
