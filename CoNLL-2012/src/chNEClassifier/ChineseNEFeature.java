package chNEClassifier;

import java.util.ArrayList;

import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import crfMentionDetect.MentionInstance;

public class ChineseNEFeature {
	public static void assignNounPhrasePOSFea(ArrayList<MentionInstance> mis, CoNLLPart part) {
		for (int i = 0; i < mis.size(); i++) {
			MentionInstance mi = mis.get(i);
			CoNLLWord word = part.getWord(i);
			mi.setPosFea(word.posTag);
			String token = mi.getWord();
			CoNLLSentence sentence = word.getSentence();
			int leafIdx = i - sentence.getStartWordIdx();
			MyTreeNode leaf = sentence.syntaxTree.leaves.get(leafIdx);
			ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
			boolean isInNP = false;
			for (MyTreeNode treeNode : ancestors) {
				if (treeNode.value.toLowerCase().startsWith("np")) {
					isInNP = true;
					break;
				}
			}
			if (isInNP) {
				mi.setIsInNP(1);
			} else {
				mi.setIsInNP(0);
			}
		}
	}
}
