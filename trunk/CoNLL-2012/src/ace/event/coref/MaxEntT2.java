package ace.event.coref;

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

public class MaxEntT2 {

	public static void main(String args[]) throws Exception {
		EntityMention.ace = true;
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Common.part = args[0];
		MaxEntUtil.stringFeas = new HashMap<String, Integer>();
		MaxEntUtil.train = true;
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

		ArrayList<String> lines = new ArrayList<String>();
		int size = parts.size();
		for (CoNLLPart part : parts) {
			System.err.println("Size: " + size--);
			MaxEntUtil.mentionIndexes.clear();
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
						lines.add(MaxEntUtil.createInstance(em, ant, part, 1));
					}
					
				}
			}
		}
		Common.outputLines(lines, outputFolder + "train.feature2");
		System.err.println(outputFolder + "train.feature2");
		Common.outputHashMap(MaxEntUtil.stringFeas, "eventStrFea" + Common.part);
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
