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
import ace.model.EventChain;
import ace.model.EventMention;
import ace.reader.ACEReader;

public class MaxEntTest {
	
	public static HashMap<String, EventMention> goldEventMentionMap = new HashMap<String, EventMention>();

	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Common.part = args[0];
		Common.train = false;
		MaxEntUtil.stringFeas = Common.readFile2Map("eventStrFea" + Common.part);
		EntityMention.ace = true;
		String folder = args[0];
		String tokens[] = folder.split("_");
		ArrayList<String> fileList = ACECommon.getFileList(tokens);

		String outputFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" + args[0] + "/";
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdir();
		}
		boolean train = false;
		ArrayList<CoNLLPart> parts = new ArrayList<CoNLLPart>();
		for (int i = 0; i < fileList.size(); i++) {
			String filename = fileList.get(i);
			CoNLLDocument document = ACEReader.read(filename, train);
			document.setDocumentID(Integer.toString(i));
			parts.addAll(document.getParts());
		}
		createAllFile(parts, outputFolder);
		MaxEntEventFeas fea = new MaxEntEventFeas();
		int size = parts.size();
		int contain = 0;
		int miss = 0;
		for (int k = 0; k < parts.size(); k++) {
			CoNLLPart part = parts.get(k);
			
			goldEventMentionMap.clear();
			ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
			for (int j = 0; j < eventChains.size(); j++) {
				for (EventMention eventMention : eventChains.get(j).getEventMentions()) {
					eventMention.goldChainID = j;
					goldEventMentionMap.put(eventMention.toString(), eventMention);
				}
			}
			
			System.err.println("Size: " + size--);
			ArrayList<String> eventLines = new ArrayList<String>();
			ArrayList<String> entityLines = new ArrayList<String>();

			ArrayList<String> positiveLinks = new ArrayList<String>();
			ArrayList<String> negativeLinks = new ArrayList<String>();

			MaxEntUtil.mentionIndexes.clear();
			ArrayList<String> lines = new ArrayList<String>();

			ArrayList<String> mentionPair = new ArrayList<String>();

			ArrayList<EntityMention> allMentions = MaxEntUtil.getEntityEventMentions(part, false);

			Collections.sort(allMentions);
			// EventMentions:
			// warning!!! how if only one mention in one document
			for (int i = 0; i < allMentions.size(); i++) {
				EntityMention em = allMentions.get(i);
				for (int j = i - 1; j >= 0; j--) {
					EntityMention ant = allMentions.get(j);

					lines.add(MaxEntUtil.createInstance(em, ant, part, 1));
					String pair = ant.getHeadCharStart() + "," + ant.getHeadCharEnd() + "," + em.getHeadCharStart()
							+ "," + em.getHeadCharEnd();

//					if (em.head.equals(ant.head)) {
//						String strPair = fea.formPair(em.head, ant.head);
//						if (MaxEntUtil.stringFeas.containsKey("triggerPair__" + strPair)) {
//							contain++;
//						} else {
//							miss++;
//						}
//					}
					if ((fea._triggerMatch_((EventMention) ant, (EventMention) em, part) || fea._commonBV_(
							(EventMention) em, (EventMention) ant, part))
							&& fea.tuneHighPrecissionNegativeConstraint((EventMention) ant, (EventMention) em, part)) {
						negativeLinks.add(pair);
					}
					mentionPair.add(pair);
				}
			}

			for (EntityMention key : MaxEntUtil.mentionIndexes.keySet()) {
				if (key instanceof EventMention) {
					eventLines.add(key.getHeadCharStart() + "," + key.getHeadCharEnd());
				} else {
					if (!key.semClass.equalsIgnoreCase("time") && !key.semClass.equalsIgnoreCase("value")) {
						entityLines.add(key.getHeadCharStart() + "," + key.getHeadCharEnd());
					}
				}
			}
			Common.outputLines(lines, outputFolder + k + ".feature");
			Common.outputLines(mentionPair, outputFolder + k + ".mpextent");
			Common.outputLines(eventLines, outputFolder + k + ".eventLines");
			Common.outputLines(entityLines, outputFolder + k + ".entityLines");

			Common.outputLines(positiveLinks, outputFolder + k + ".positiveLinks");
			Common.outputLines(negativeLinks, outputFolder + k + ".negativeLinks");
		}
//		System.err.println(contain + "##" + miss);
//		for (String key : MaxEntEventFeas.errors.keySet()) {
//			int stat[] = MaxEntEventFeas.errors.get(key);
//			System.err.println(key + ": " + stat[0] + "#" + stat[1]);
//		}
	}

	private static void createAllFile(ArrayList<CoNLLPart> parts, String outputFolder) {
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

		ArrayList<String> testPML = new ArrayList<String>();
		testPML.add("include \"coref-type.pml\";");
		testPML.add("include \"coref.pml\";");
		testPML.add("load weights from dump \"coref.weights\";");

		for (String file : all) {
			testPML.add("load corpus from \"" + file + ".atom\";");
			testPML.add("save corpus to ram;");
			testPML.add("test to \"" + file + ".mln\";");

			testPML.add("");
		}
		Common.outputLines(testPML, "/users/yzcchen/workspace/CoNLL-2012/src/ace/mln/test.pml");
	}
}
