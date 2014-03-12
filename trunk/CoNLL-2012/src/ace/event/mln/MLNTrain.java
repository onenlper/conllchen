package ace.event.mln;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;
import ace.ACECommon;
import ace.model.EventMention;
import ace.reader.ACEReader;

public class MLNTrain {

	public static void main(String args[]) throws Exception {
		EntityMention.ace = true;
		String folder = "1_2_3_4";
		String tokens[] = folder.split("_");
		ArrayList<String> fileList = ACECommon.getFileList(tokens);

		String outputFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/mln/";
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

		ArrayList<String> lines = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			// System.out.println(part.getDocument().getFilePath());
			MLNUtil.mentionIndexes.clear();

			ArrayList<String> subLines = new ArrayList<String>();

			subLines.add(">>");
			HashMap<String, ArrayList<String>> features = new HashMap<String, ArrayList<String>>();
			ArrayList<EntityMention> allMentions = MLNUtil.getEntityEventMentions(part, true);

			Collections.sort(allMentions);

			// Event Coreference:
			for (int i = 0; i < allMentions.size(); i++) {
				EntityMention em = allMentions.get(i);
				if (!(em instanceof EventMention)) {
					continue;
				}
				boolean anaphor = false;
				for (int j = i - 1; j >= 0; j--) {
					EntityMention ant = allMentions.get(j);
					if (ant instanceof EventMention && em.goldChainID == ant.goldChainID) {
						anaphor = true;
						MLNUtil.createInstance(features, em, ant, part, true);
						break;
					}
					// create positive instance
					// ...
				}
				if (!anaphor) {
					continue;
				}
				for (int j = i - 1; j >= 0; j--) {
					EntityMention ant = allMentions.get(j);

					if (!(ant instanceof EventMention)) {
						continue;
					}
					if (ant.goldChainID == em.goldChainID) {
						continue;
					}

					MLNUtil.createInstance(features, em, ant, part, false);

				}
			}

			// Entity Coreference:
//			for (int i = 0; i < allMentions.size(); i++) {
//				EntityMention em = allMentions.get(i);
//				if ((em instanceof EventMention)) {
//					continue;
//				}
//				boolean anaphor = false;
//				for (int j = i - 1; j >= 0; j--) {
//					EntityMention ant = allMentions.get(j);
//					if (!(ant instanceof EventMention) && em.goldChainID == ant.goldChainID) {
//						anaphor = true;
//						MLNUtil.createNInstance(features, em, ant, part, true);
//						break;
//					}
//					// create positive instance
//					// ...
//				}
//				if (!anaphor) {
//					continue;
//				}
//				for (int j = i - 1; j >= 0; j--) {
//					EntityMention ant = allMentions.get(j);
//					if (ant instanceof EventMention) {
//						continue;
//					}
//					if (ant.goldChainID == em.goldChainID) {
//						break;
//					}
//					MLNUtil.createNInstance(features, em, ant, part, false);
//				}
//			}

			for (String str : features.keySet()) {
				subLines.add(">" + str);
				for (String pair : features.get(str)) {
					subLines.add(pair);
				}
			}
			ArrayList<String> otherLines = MLNUtil.addOtherPredicates(part, allMentions);
			if (subLines.size() != 1) {
				subLines.addAll(otherLines);
				lines.addAll(subLines);
			}
		}
		Common.outputLines(lines, outputFolder + "train.atom");
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
