package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.GoldMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import ruleCoreference.english.EnDictionary;

public class AnalyzeSystem {

	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("java ~ goldChain systemChain");
			System.exit(1);
		}
		CoNLLDocument goldDoc = new CoNLLDocument(args[0]);
		CoNLLDocument systemDoc = new CoNLLDocument(args[1]);

		HashMap<String, CoNLLPart> goldPartMap = new HashMap<String, CoNLLPart>();
		HashMap<String, CoNLLPart> systemPartMap = new HashMap<String, CoNLLPart>();

		for (CoNLLPart part : goldDoc.getParts()) {
			goldPartMap.put(part.label, part);
		}

		for (CoNLLPart part : systemDoc.getParts()) {
			systemPartMap.put(part.label, part);
		}
		int g = 0;
		for (String key : goldPartMap.keySet()) {
			g++;
			CoNLLPart goldPart = goldPartMap.get(key);
			CoNLLPart systemPart = systemPartMap.get(key);

			ArrayList<Entity> goldEntities = goldPart.getChains();
			int a = key.indexOf('(');
			int b = key.indexOf(')');
			String fn = "/users/yzcchen/workspace/CoNLL-2012/src/analyze/" + key.substring(a + 1, b).replace('/', '_')
					+ "_" + goldPart.getPartID();
			outputEntities(goldEntities, fn + ".gold", systemPart);
			// boostSingletonMentions(goldPart, goldEntities);

			ArrayList<Entity> systemEntities = systemPart.getChains();
			outputEntities(systemEntities, fn + ".system", systemPart);

			HashMap<String, Integer> goldMap = toCluster(goldEntities);
			HashMap<String, Integer> systemMap = toCluster(systemEntities);

			HashMap<String, String> sourceMap = new HashMap<String, String>();
			for (Entity entity : systemEntities) {
				for (EntityMention m : entity.mentions) {
					sourceMap.put(m.toName(), m.source);
				}
			}

			System.out.println(g + " ======= " + key.replace('/', '_'));
			// recall error:
			recallError(systemPart, goldEntities, systemMap);
			System.out.println(g + " ======= " + key.replace('/', '_'));
			// precision error:
			// precisionError(systemEntities, goldMap);
		}

		System.out.println("Percent: " + involvePronoun / overall);

		System.out.println(involvePronoun + " / " + overall);
	}

	static double overall;
	static double involvePronoun;

	static EnDictionary dict = new EnDictionary();

	private static void recallError(CoNLLPart systemPart, ArrayList<Entity> goldEntities,
			HashMap<String, Integer> systemMap) {
		for (Entity goldEntity : goldEntities) {
			Collections.sort(goldEntity.mentions);
			for (int i = 0; i < goldEntity.mentions.size() - 1; i++) {
				EntityMention m1 = goldEntity.mentions.get(i);
				EntityMention m2 = goldEntity.mentions.get(i + 1);
				Integer c1 = systemMap.get(m1.toName());
				Integer c2 = systemMap.get(m2.toName());
				if (c1 != null && c2 != null) {
					if (c1.intValue() != c2.intValue()) {
						String s1 = getSource(systemPart, m1);
						String s2 = getSource(systemPart, m2);
						if (dict.allPronouns.contains(s1.toLowerCase()) || dict.allPronouns.contains(s2.toLowerCase())) {
							involvePronoun++;
						} else {
							System.out.println(s1 + " # " + s2);
						}
						overall++;
					} else {
					}
				}
			}
		}
	}

	private static void precisionError(ArrayList<Entity> systemEntities, HashMap<String, Integer> goldMap) {
		for (Entity systemEntity : systemEntities) {
			for (int i = 0; i < systemEntity.mentions.size() - 1; i++) {
				EntityMention m1 = systemEntity.mentions.get(i);
				EntityMention m2 = systemEntity.mentions.get(i + 1);
				Integer c1 = goldMap.get(m1.toName());
				Integer c2 = goldMap.get(m2.toName());
				if (c1 != null && c2 != null) {
					if (c1.intValue() != c2.intValue()) {
						String s1 = m1.source;
						String s2 = m2.source;
						if (dict.allPronouns.contains(s1.toLowerCase()) || dict.allPronouns.contains(s2.toLowerCase())) {
							involvePronoun++;
						} else {
							System.out.println(s1 + " # " + s2);
						}
						overall++;
					} else {
					}
				}
			}
		}
	}

	private static void outputEntities(ArrayList<Entity> chains, String filename, CoNLLPart part) {
		ArrayList<String> lines = new ArrayList<String>();
		for (int i = 0; i < chains.size(); i++) {
			Entity chain = chains.get(i);
			if (chain.mentions.size() == 1) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(i).append(": ");
			for (EntityMention m : chain.mentions) {
				sb.append(getSource(part, m)).append(", ");
			}
			lines.add(sb.toString());
		}
		Common.outputLines(lines, filename);
	}

	public static String getSource(CoNLLPart part, EntityMention m) {
		StringBuilder sb = new StringBuilder();
		for (int i = m.start; i <= m.end; i++) {
			sb.append(part.getWord(i).orig).append(" ");
		}
		return sb.toString().trim();
	}

	public static void boostSingletonMentions(CoNLLPart part, ArrayList<Entity> chains) {
		HashSet<String> mentions = new HashSet<String>();
		for (Entity chain : chains) {
			for (EntityMention m : chain.mentions) {
				mentions.add(m.toName());
			}
		}
		GoldMention gm = new GoldMention();
		ArrayList<EntityMention> goldMentions = gm.getMentions(part);
		for (EntityMention m : goldMentions) {
			if (!mentions.contains(m.toName())) {
				Entity e = new Entity();
				e.addMention(m);
				chains.add(e);
			}
		}
	}

	public static HashMap<String, Integer> toCluster(ArrayList<Entity> entities) {
		HashMap<String, Integer> chainMap = new HashMap<String, Integer>();
		for (int i = 0; i < entities.size(); i++) {
			for (EntityMention m : entities.get(i).mentions) {
				chainMap.put(m.toName(), i);
			}
		}
		return chainMap;
	}
}
