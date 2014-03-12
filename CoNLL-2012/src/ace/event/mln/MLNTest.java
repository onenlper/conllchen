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

public class MLNTest {

	public static void main(String args[]) throws Exception {
		EntityMention.ace = true;
		String folder = "0";
		String tokens[] = folder.split("_");
		ArrayList<String> fileList = ACECommon.getFileList(tokens);

		String outputFolder = "/users/yzcchen/chen3/conll12/chinese/ACE_test/";
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

		for (int k=0;k<parts.size();k++) {
			CoNLLPart part = parts.get(k);
			
			ArrayList<String> eventLines = new ArrayList<String>();
			ArrayList<String> entityLines = new ArrayList<String>();
			MLNUtil.mentionIndexes.clear();
			ArrayList<String> lines = new ArrayList<String>();
			lines.add(">>");
			HashMap<String, ArrayList<String>> features = new HashMap<String, ArrayList<String>>();
			ArrayList<EntityMention> allMentions = MLNUtil.getEntityEventMentions(part, false);

			Collections.sort(allMentions);
			// EventMentions:
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
					MLNUtil.createInstance(features, em, ant, part, false);
				}
			}
			
			// Entity Mentions:
//			for (int i = 0; i < allMentions.size(); i++) {
//				EntityMention em = allMentions.get(i);
//				if ((em instanceof EventMention)) {
//					continue;
//				}
//				for (int j = i - 1; j >= 0; j--) {
//					EntityMention ant = allMentions.get(j);
//					if ((ant instanceof EventMention)) {
//						continue;
//					}
//					MLNUtil.createNInstance(features, em, ant, part, false);
//				}
//			}
			
			
			
			
			for (String str : features.keySet()) {
				lines.add(">" + str);
				for (String pair : features.get(str)) {
					lines.add(pair);
				}
			}
			ArrayList<String> otherLines = MLNUtil.addOtherPredicates(part, allMentions);
			if(lines.size()==1) {
				lines.clear();
			} else {
				lines.addAll(otherLines);
			}
			
			for(EntityMention key : MLNUtil.mentionIndexes.keySet()) {
				if(key instanceof EventMention) {
					eventLines.add(key.getHeadCharStart() + "," + key.getHeadCharEnd());
				} else {
					if(!key.semClass.equalsIgnoreCase("time") && !key.semClass.equalsIgnoreCase("value")) {
						entityLines.add(key.getHeadCharStart() + "," + key.getHeadCharEnd());
					}
				}
			}
			
			Common.outputLines(lines, outputFolder + k + ".atom");
			Common.outputHashMap(MLNUtil.mentionIndexes, outputFolder + k + ".mention");
			Common.outputLines(eventLines, outputFolder + k + ".eventLines");
			Common.outputLines(entityLines, outputFolder + k + ".entityLines");
		}
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
		
		for(String file : all) {
			testPML.add("load corpus from \"" + file + ".atom\";");
			testPML.add("save corpus to ram;");
			testPML.add("test to \"" + file + ".mln\";");
			
			testPML.add("");
		}
		Common.outputLines(testPML, "/users/yzcchen/workspace/CoNLL-2012/src/ace/mln/test.pml");
	}
}
