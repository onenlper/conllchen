package ace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;
import ace.event.coref.MaxEntUtil;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.reader.ACEReader;
import ace.rule.RuleCoref;

public class Investigate {

	public static void main(String args[]) throws Exception {
		EntityMention.ace = true;
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Common.part = args[0];
		String tokens[] = new String[4];
		int k = 0;
		for (int i = 0; i < 5; i++) {
			if (i != Integer.valueOf(args[0])) {
				tokens[k++] = Integer.toString(i);
			}
		}
		ArrayList<String> fileList = ACECommon.getFileList(tokens);

		String outputFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" + args[0] + "/";
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdir();
		}
		boolean train = true;
		ArrayList<CoNLLPart> parts = new ArrayList<CoNLLPart>();
		for (int i = 0; i < fileList.size(); i++) {
			String filename = fileList.get(i);
			CoNLLDocument document = ACEReader.read(filename, train);
			document.setDocumentID(Integer.toString(i));
			parts.addAll(document.getParts());
		}

		int size = parts.size();

		HashMap<String, int[]> stats = new HashMap<String, int[]>();
		int total = 0;
		for (CoNLLPart part : parts) {
			System.err.println("Size: " + size--);
			ArrayList<EntityMention> allMentions = MaxEntUtil.getEntityEventMentions(part, true);

			Collections.sort(allMentions);
			// Event Coreference:
			for (int i = 0; i < allMentions.size(); i++) {
				EntityMention em = allMentions.get(i);
				if (!(em instanceof EventMention)) {
					continue;
				}
				for (int j = i - 1; j >= 0; j--) {
					EntityMention ant = allMentions.get(j);
					if (!(ant instanceof EventMention)) {
						continue;
					}
					if (ant.goldChainID == em.goldChainID) {
						total++;
						// check rules here
						EventMention m1 = (EventMention) ant;
						EventMention m2 = (EventMention) em;

						for (String r1 : m1.argHash.keySet()) {
							for (String r2 : m2.argHash.keySet()) {

								if (r1.equals(r2)) {
									String key = r1;
									int stat[] = stats.get(key);
									if (stat == null) {
										stat = new int[2];
										stats.put(key, stat);
									}
									boolean coref = false;

									loop: for (EventMentionArgument a1 : m1.argHash.get(r1)) {
										for (EventMentionArgument a2 : m2.argHash.get(r2)) {
											if (a1.mention.entity == a2.mention.entity) {
												coref = true;
												break loop;
											}
										}
									}
									if (coref) {
										stat[0]++;
									} else {
										System.err.println("key: " + key + "\n" + part.getDocument().getFilePath());
										RuleCoref.printPair(m1, m2);
										stat[1]++;
									}
								}
							}
						}
					}
				}
			}
		}
		for (String stat : stats.keySet()) {
			int s[] = stats.get(stat);
			System.err.println(stat + ":" + s[0] + "#" + s[1]);
		}
		System.err.println("Total: " + total);
	}

	protected static void createAllFile(ArrayList<CoNLLPart> parts, String outputFolder) {
		ArrayList<String> all = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all.add(outputFolder + part.getDocument().getDocumentID());
		}
		Common.outputLines(all, outputFolder + "all.txt");
		ArrayList<String> all2 = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all2.add(part.getDocument().getFilePath());
		}
		Common.outputLines(all2, outputFolder + "all.txt2");
	}
}
