package score;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import coref.OntoAltafToSemEval.CRFElement;

public class ConvertToNewFormat {
	static String goldKey = "";
	static String systemKey = "";
	static String mode = "";

	public static void main(String args[]) throws Exception {

		if (args.length == 2) {
			if (!args[0].equals("-g")) {
				System.err.println("java -g goldKey");
				System.exit(1);
			}
			mode = "gold";
			goldKey = args[1];
		}

		if (args.length == 3) {
			if (!args[0].equals("-s")) {
				System.err.println("java -s goldKey systemKey");
				System.exit(1);
			}
			mode = "system";
			goldKey = args[1];
			systemKey = args[2];
		}
		
		if(mode.equals("gold")) {
			transitGoldKey();
		} else {
			transitSystemKey();
		}
		
	}
	
	public static void transitSystemKey() throws Exception {
		FileWriter systemTypeKeyFw = new FileWriter(systemKey + ".type");
		
		// change NE and POS field
		CoNLLDocument goldDocument = new CoNLLDocument(goldKey);
		CoNLLDocument systemDocument = new CoNLLDocument(systemKey);
		
		HashMap<String, CoNLLPart> goldParts = new HashMap<String, CoNLLPart>();
		for(CoNLLPart part : goldDocument.getParts()) {
			goldParts.put(part.label, part);
		}
		int k = 0;
		for(CoNLLPart part : systemDocument.getParts()) {
			
			System.out.println(part.getPartName() + " # " + (k++));
			ArrayList<Entity> chains = part.getChains();
			for (Entity chain : chains) {
				for (EntityMention mention : chain.mentions) {
					assignHeadExtent(mention, part);

					mention.type = getType(mention, goldParts.get(part.label));
				}
			}
			writerKey(systemTypeKeyFw, chains, part);
		}
		systemTypeKeyFw.close();
	}
	
	
	public static void transitGoldKey() throws Exception{
		FileWriter goldTypeKeyFw = new FileWriter(goldKey + ".type");
		CoNLLDocument document = new CoNLLDocument(goldKey);
		int k = 0;
		for (CoNLLPart part : document.getParts()) {
			System.out.println(part.getPartName() + " # " + (k++));
			ArrayList<Entity> chains = part.getChains();
			for (Entity chain : chains) {
				for (EntityMention mention : chain.mentions) {
					assignHeadExtent(mention, part);

					mention.type = getType(mention, part);
				}
			}
			writerKey(goldTypeKeyFw, chains, part);
		}
		goldTypeKeyFw.close();
	}

	public static void writerKey(FileWriter systemKeyFw, ArrayList<Entity> systemChain, CoNLLPart part)
			throws IOException {
		HashSet<Integer> sentenceEnds = new HashSet<Integer>();
		for (CoNLLSentence sentence : part.getCoNLLSentences()) {
			sentenceEnds.add(sentence.getEndWordIdx());
		}
		systemKeyFw.write(part.label + "\n");
		ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
		int maxWord = part.getWordCount();
		for (int i = 0; i < maxWord; i++) {
			elements.add(new CRFElement());
		}
		for (int i = 0; i < systemChain.size(); i++) {
			Entity en = systemChain.get(i);
			for (EntityMention em : en.mentions) {
				int start = em.start;
				int end = em.end;

				StringBuilder sb = new StringBuilder();

				String label = Integer.toString(em.entityIndex);

				if (start == end) {
					sb.append("(").append(label + "@" + em.getType()).append(")");
					elements.get(start).append(sb.toString());
				} else {
					elements.get(start).append("(" + label + "@" + em.getType());
					elements.get(end).append(label + ")");
				}
			}
		}
		for (int i = 0; i < elements.size(); i++) {
			CRFElement element = elements.get(i);
			String sourceLine = stripLastColumn(part.getWord(i).sourceLine);
			if (element.predict.isEmpty()) {
				systemKeyFw.write(sourceLine + "-\n");
			} else {
				systemKeyFw.write(sourceLine + element.predict + "\n");
			}
			if (sentenceEnds.contains(i)) {
				systemKeyFw.write("\n");
			}
		}
		systemKeyFw.write("#end document\n");
	}

	private static String stripLastColumn(String s) {
		s = s.trim();
		int idx = s.lastIndexOf(' ');
		if(idx==-1) {
			idx = s.lastIndexOf('\t');
		}
		return s.substring(0, idx + 1);
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
