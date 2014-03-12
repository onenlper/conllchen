package crfMentionDetect;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import mentionDetect.GoldMention;
import model.Element;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.OntoCommon;

public class CRFMentionDetectTrain {
	public static void main(String args[]) throws Exception{
		if (args.length < 1) {
			System.out.println("java ~ english folder");
			return;
		}
		String language = args[0];
		OntoCommon ontoCommon = new OntoCommon(language);
		String folder = args[1];
		ArrayList<String> files = Common.getLines(language + "_list_" + folder + "_train");
		GoldMention goldMention = new GoldMention();
		FileWriter fw = new FileWriter("/users/yzcchen/conll12/" + language + "/mentionTrain." + folder);
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			if(!conllFn.contains(File.separator + folder + File.separator)) {
				continue;
			}
			System.out.println(conllFn);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			for (int m=0;m<document.getParts().size();m++) {
				CoNLLPart part = document.getParts().get(m);
				ArrayList<EntityMention> goldEMs = goldMention.getMentions(part);
				ArrayList<EntityMention> shortEMs = new ArrayList<EntityMention>();
				for (EntityMention em : goldEMs) {
					int position[] = ontoCommon.getPosition(em, part.getCoNLLSentences());
					MyTreeNode minTreeNode = ontoCommon.getMinNPTreeNode(em, part.getCoNLLSentences());
					CoNLLSentence sentence = part.getCoNLLSentences().get(position[0]);
					ArrayList<MyTreeNode> leaves = minTreeNode.getLeaves();
					CoNLLWord startWord = sentence.getWord(leaves.get(0).leafIdx);
					CoNLLWord lastWord = sentence.getWord(leaves.get(leaves.size()-1).leafIdx);
					EntityMention shortEM = new EntityMention();
					shortEM.start = startWord.getIndex();
					shortEM.end = lastWord.getIndex();
					shortEMs.add(shortEM);
				}
				ArrayList<Element> NEElements = part.getNameEntities();
				ArrayList<MentionInstance> mis = new ArrayList<MentionInstance>();
				for(int i=0;i<part.getWordCount();i++) {
					MentionInstance mi = new MentionInstance(part.getWord(i).word, i);
					mi.setDocumentID(document.getDocumentID());
					mi.setPartID(part.getPartID());
					mi.setFilePath(document.getFilePath());
					mis.add(mi);
				}
				for(EntityMention shortEM : shortEMs) {
					int start = shortEM.start;
					int end = shortEM.end;
					mis.get(start).label = "B";
					for(int i=start+1;i<=end;i++) {
						mis.get(i).label = "I";
					}
				}
				MentionDetectUtil.assignNEFea(mis, NEElements);
				MentionDetectUtil.assignNounPhrasePOSFea(mis, part);
				MentionDetectUtil.assignPronounFea(mis);
				MentionDetectUtil.assignInLocationFea(mis);
				MentionDetectUtil.outputInstances(mis, fw, part.getCoNLLSentences());
			}
		}
		fw.close();
	}
}
