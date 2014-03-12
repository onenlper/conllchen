package mentionDetect;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.OntoCommon;

public class CRFMention extends MentionDetect{

	HashMap<String, ArrayList<EntityMention>> EMs;
	
	HashMap<String, CoNLLDocument> documentBuffer = new HashMap<String, CoNLLDocument>();
	
	public void loadEMs(String filename) {
		EMs = new HashMap<String, ArrayList<EntityMention>>();
		ArrayList<String> lines = Common.getLines(filename);
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i);
			if(line.isEmpty()) {
				continue;
			}
			String tokens[] = line.split("\\s+");
			int length = tokens.length;
			String label = tokens[length-1];
			int wordID = Integer.valueOf(tokens[length-3]);
			int partID = Integer.valueOf(tokens[length-4]);
			String docID = tokens[length-5];
			String filePath = tokens[length-6];
			String key = docID + "_" + partID;
			if(label.equalsIgnoreCase("B")) {
				int k=i+1;
				while(!lines.get(k).isEmpty() && lines.get(k).trim().endsWith("I")) {
					k++;
				}
				int start = wordID;
				int end = Integer.valueOf(lines.get(k-1).split("\\s+")[length-3]);
				EntityMention em = new EntityMention();
				em.start = start;
				em.end = end;
				CoNLLDocument document;
				if(documentBuffer.containsKey(filePath)) {
					document = documentBuffer.get(filePath);
				} else {
					document = new CoNLLDocument(filePath);
					documentBuffer.put(filePath, document);
				}
				ArrayList<CoNLLSentence> sentences = document.getParts().get(partID).getCoNLLSentences();
				MyTreeNode tn = ontoCommon.getMaxNPTreeNode(em, sentences);
				ArrayList<MyTreeNode> leaves = tn.getLeaves();
				int position[] = ontoCommon.getPosition(em, sentences);
				CoNLLSentence sentence = sentences.get(position[0]);
				CoNLLWord startWord = sentence.getWord(leaves.get(0).leafIdx);
				CoNLLWord endWord = sentence.getWord(leaves.get(leaves.size()-1).leafIdx);
				EntityMention longEM = new EntityMention();
				longEM.start = startWord.index;
				longEM.end = endWord.index;
				if(EMs.containsKey(key)) {
					EMs.get(key).add(longEM);
				} else {
					ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
					ems.add(longEM);
					EMs.put(key, ems);
				}
			}
		}
	}
	
	OntoCommon ontoCommon;
	
	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		// TODO Auto-generated method stub
		String basePath = "/users/yzcchen/conll12/";
		String filePath = part.getDocument().getFilePath();
		if(filePath.contains("chinese")) {
			basePath += "chinese/";
			ontoCommon = new OntoCommon("chinese");
		} else if (filePath.contains("english")) {
			basePath += "english/";
			ontoCommon = new OntoCommon("english");
		} else if (filePath.contains("arabic")) {
			basePath += "arabic/";
			ontoCommon = new OntoCommon("arabic");
		}
		String documentID = part.getDocument().getDocumentID();
		int a = documentID.indexOf(File.separator);
		String folder = documentID.substring(0, a);
		
		String mentionFile = basePath + folder + ".result";
		if(EMs==null) {
			this.loadEMs(mentionFile);
		}
		return EMs.get(documentID+"_"+part.getPartID());
	}

}
