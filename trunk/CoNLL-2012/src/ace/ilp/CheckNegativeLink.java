package ace.ilp;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class CheckNegativeLink {
	public static void main(String args[]) {
		int ge = 0;
		int y = 0;
		int c = 0;
		for (int k = 0; k < 5; k++) {
			String folder1 = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" + Integer.toString(k);

			ArrayList<String> allLines = Common.getLines(folder1 + "/all.txt");

			for (String line : allLines) {
				ArrayList<String> negtives = Common.getLines(line + ".negativeLinks");
				String line2 = line.replace("/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_",
						"/users/yzcchen/chen3/conll12/chinese/ACE_test_");
				ArrayList<String> entities = Common.getLines(line + ".entities.mp.event");

				ArrayList<String> goldEntities = Common.getLines(line2 + ".entities.golden.event");
				HashMap<String, Integer> goldMap = new HashMap<String, Integer>();
				for (int j = 0; j < goldEntities.size(); j++) {
					String goldEntity = goldEntities.get(j);
					String mentions[] = goldEntity.split("\\s+");
					for (int i = 0; i < mentions.length; i++) {
						goldMap.put(mentions[i], j);
					}
				}

				for (String negtive : negtives) {
					String tokens[] = negtive.split(",");
					String m1 = tokens[0] + "," + tokens[1];
					String m2 = tokens[2] + "," + tokens[3];
					if (goldMap.containsKey(m1) && goldMap.containsKey(m2) && goldMap.get(m1) == goldMap.get(m2)) {
						y++;
					} else {
						c++;
					}
				}

				for (String entity : entities) {
					String mentions[] = entity.split("\\s+");

					for (int i = 0; i < mentions.length; i++) {
						for (int j = i + 1; j < mentions.length; j++) {
							String pair = mentions[i] + "," + mentions[j];

							if (negtives.contains(pair)) {
								System.err.println("GEEEEEEEE");
								ge++;
								System.err.println(mentions.length);
							}

						}
					}
				}
			}
		}
		System.err.println(ge);
		System.err.println(y+"/"+c);
	}
}
