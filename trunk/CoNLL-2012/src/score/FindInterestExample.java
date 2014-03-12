package score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;

public class FindInterestExample {
	public static void main(String args[]) {
		CoNLLDocument document = new CoNLLDocument("all_eng_test_v4_gold_conll");
		
		for(CoNLLPart part : document.getParts()) {
			
			ArrayList<Entity> chains = part.getChains();
			for (Entity chain : chains) {
				for (EntityMention mention : chain.mentions) {
					assignHeadExtent(mention, part);
					mention.type = getType(mention, part);
				}
			}
			
			for(Entity chain : chains) {
				Collections.sort(chain.mentions);
				HashSet<String> set = new HashSet<String>();
				for(EntityMention m : chain.mentions) {
					set.add(m.type);
				}
				if(set.size()==3) {
					String enStr = getEntity(chain, part);
//					if(enStr.contains("Iraq(NR)(1) # the nation(NN)(3) # Iraq(NR)(4) # its(PN)(4) # it(PN)(4) # their country(NN)(8) #")) {
					System.out.println("---- : " + part.label + " # ");
					System.out.println(enStr);
					
					System.out.println("\n");
//					}
				}
			}
			
			
		}
	}
	
	private static String getEntity(Entity chain, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		for(EntityMention m : chain.mentions) {
			CoNLLWord w = part.getWord(m.headStart);
			m.sentenceID = w.sentence.getSentenceIdx();
			sb.append(m.original).append("(").append(m.type).append(")").append("(" + m.sentenceID + ")").append(" # ");
		}
		return sb.toString();
	}
	
	
	
	
	
	
	
	
	
	
	// "LAW, GPE, NORP, LANGUAGE, FAC, PRODUCT, LOC, PERSON, WORK_OF_ART,
	// ,EVENT, ORG,

	// ORDINAL, MONEY, PERCENT,DATE
	// QUANTITY, TIME, *, CARDINAL"

	static HashSet<String> NRNames = new HashSet<String>(Arrays.asList("LAW", "GPE", "NORP", "LANGUAGE", "FAC",
			"PRODUCT", "LOC", "PERSON", "WORK_OF_ART", "EVENT", "ORG"));
	
	
	private static String getType(EntityMention m, CoNLLPart part) {
		String type = "";

		CoNLLWord headWord = part.getWord(m.headStart);
		if (headWord.getPosTag().startsWith("PRP") || headWord.getPosTag().equals("DT")) {
			type = "PN";
		} else if (headWord.getPosTag().startsWith("NNP") || (NRNames.contains(headWord.rawNamedEntity))) {
			type = "NR";
		} else {
			type = "NN";
		}
		return type;
	}

	/** get position of sentenceIdx, wordStartIdx and wordEndIdx */
	public static int[] getPosition(EntityMention em, ArrayList<CoNLLSentence> sentences) {
		int sentenceID = 0;
		CoNLLSentence sentence = null;
		for (int i = 0; i < sentences.size(); i++) {
			sentence = sentences.get(i);
			if (em.start >= sentence.getStartWordIdx() && em.end <= sentence.getEndWordIdx()) {
				sentenceID = i;
				break;
			}
		}
		int position[] = new int[3];
		position[0] = sentenceID;
		position[1] = em.start - sentence.getStartWordIdx();
		position[2] = em.end - sentence.getStartWordIdx();
		return position;
	}

	public static void assignHeadExtent(EntityMention em, CoNLLPart part) {
		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		int[] position = getPosition(em, sentences);
		MyTreeNode node = getNPTreeNode(em, sentences);
		// find English mention's head
		// mention ends with 's
		MyTreeNode headLeaf = node.getHeadLeaf();
		int headStart = sentences.get(position[0]).getWord(headLeaf.leafIdx).index;
		if (headStart < em.start || headStart > em.end) {
			headStart = em.end;
		}
		String head = part.getWord(headStart).orig;
		em.headStart = headStart;
		em.headEnd = headStart;
		em.head = head;
	}

	public static MyTreeNode getNPTreeNode(EntityMention np, ArrayList<CoNLLSentence> sentences) {
		int position[] = getPosition(np, sentences);
		ArrayList<MyTreeNode> leaves = sentences.get(position[0]).getSyntaxTree().leaves;
		MyTreeNode leftNP = leaves.get(position[1]);
		MyTreeNode rightNP = leaves.get(position[2]);
		ArrayList<MyTreeNode> leftAncestors = leftNP.getAncestors();
		ArrayList<MyTreeNode> rightAncestors = rightNP.getAncestors();
		MyTreeNode commonNode = null;
		for (int i = 0; i < leftAncestors.size() && i < rightAncestors.size(); i++) {
			if (leftAncestors.get(i) == rightAncestors.get(i)) {
				commonNode = leftAncestors.get(i);
			} else {
				break;
			}
		}
		return commonNode;
	}
	
}
