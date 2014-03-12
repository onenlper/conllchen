package mentionDetection.arabic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.ArCommon;
import util.Common;

public class ArabicCRFMention extends MentionDetect {

	static HashMap<String, ArrayList<EntityMention>> EMs;

	static HashMap<String, CoNLLDocument> documentBuffer = new HashMap<String, CoNLLDocument>();

	public static void loadEMs(String filename) {
		ArrayList<String> lines = Common.getLines(filename);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isEmpty()) {
				continue;
			}
			String tokens[] = line.split("\\s+");
			int length = tokens.length;
			String label = tokens[length - 1];
			int wordID = Integer.valueOf(tokens[length - 3]);
			int partID = Integer.valueOf(tokens[length - 4]);
			String docID = tokens[length - 5];
			String filePath = tokens[length - 6];
			String key = docID + "_" + partID;
			if (label.equalsIgnoreCase("B")) {
				int k = i + 1;
				while (!lines.get(k).isEmpty() && lines.get(k).trim().endsWith("I")) {
					k++;
				}
				int start = wordID;
				int end = Integer.valueOf(lines.get(k - 1).split("\\s+")[length - 3]);
				EntityMention em = new EntityMention();
				em.start = start;
				em.end = end;
				CoNLLDocument document;
				if (documentBuffer.containsKey(filePath)) {
					document = documentBuffer.get(filePath);
				} else {
					document = new CoNLLDocument(filePath);
					documentBuffer.put(filePath, document);
				}
				if (EMs.containsKey(key)) {
					EMs.get(key).add(em);
				} else {
					ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
					ems.add(em);
					System.out.println("Loading..." + key);
					EMs.put(key, ems);
				}
			}
		}
	}

	ArCommon ontoCommon;

	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		// TODO Auto-generated method stub
		ontoCommon = new ArCommon("arabic");
		if (EMs == null) {
			EMs = new HashMap<String, ArrayList<EntityMention>>();
			loadEMs("/users/yzcchen/tool/CRF/CRF++-0.54/conll/arabicMD.developResult");
//			loadEMs("/users/yzcchen/tool/CRF/CRF++-0.54/conll/arabicMD.developResult.short");
		}
		String documentID = part.getDocument().getDocumentID();
		System.out.println("Getting..." + documentID + "_" + part.getPartID());
		ArrayList<EntityMention> mentions = EMs.get(documentID + "_" + part.getPartID());
		HashSet<EntityMention> hashMentions = new HashSet<EntityMention>();
		hashMentions.addAll(mentions);
		mentions.clear();
		mentions.addAll(hashMentions);
		for(EntityMention mention : mentions) {
			ontoCommon.calAttribute(mention, part);
		}
		return mentions;
	}

}
