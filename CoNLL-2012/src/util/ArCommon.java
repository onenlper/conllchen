package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.EntityMention.MentionType;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import ruleCoreference.arabic.ArDictionary;
import ruleCoreference.english.EnDictionary;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class ArCommon {

	private String language;

	private static ArDictionary dict;

	public boolean numberInLaterMention(EntityMention ant, EntityMention mention, CoNLLPart part) {
		Set<String> antecedentWords = new HashSet<String>();
		Set<String> numbers = new HashSet<String>(Arrays.asList(new String[] { "one", "two", "three", "four", "five",
				"six", "seven", "eight", "nine", "ten", "hundred", "thousand", "million", "billion" }));
		for (int i = ant.start; i <= ant.end; i++) {
			antecedentWords.add(part.getWord(i).orig.toLowerCase());
		}
		for (int i = mention.start; i < mention.end; i++) {
			String word = part.getWord(i).orig.toLowerCase();
			try {
				Double.parseDouble(word);
				if (!antecedentWords.contains(word))
					return true;
			} catch (NumberFormatException e) {
				if (numbers.contains(word.toLowerCase()) && !antecedentWords.contains(word))
					return true;
				continue;
			}
		}
		return false;
	}

	/** Check whether two mentions have different locations */
	public boolean englishHaveDifferentLocation(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		// state and country cannot be coref
		if ((this.getArDictionary().statesAbbreviation.containsKey(antecedent.original) || this.getArDictionary().statesAbbreviation
				.containsValue(mention.original))
				&& (antecedent.head.equalsIgnoreCase("country") || mention.head.equalsIgnoreCase("nation")))
			return true;

		Set<String> locationM = new HashSet<String>();
		Set<String> locationA = new HashSet<String>();
		String mString = mention.original.toLowerCase();
		String aString = antecedent.original.toLowerCase();
		Set<String> locationModifier = new HashSet<String>(Arrays.asList("east", "west", "north", "south", "eastern",
				"western", "northern", "southern", "northwestern", "southwestern", "northeastern", "southeastern",
				"upper", "lower"));

		for (int i = mention.start; i <= mention.end; i++) {
			String word = part.getWord(i).word;
			if (locationModifier.contains(word)) {
				return true;
			}
			if (part.getWord(i).rawNamedEntity.equals("LOC")) {
				String loc = part.getWord(i).word;
				if (dict.statesAbbreviation.containsKey(loc))
					loc = dict.statesAbbreviation.get(loc);
				locationM.add(loc);
			}
		}
		for (int i = antecedent.start; i <= antecedent.end; i++) {
			String word = part.getWord(i).word;
			if (locationModifier.contains(word)) {
				return true;
			}
			if (part.getWord(i).rawNamedEntity.equals("LOC")) {
				String loc = part.getWord(i).word;
				if (dict.statesAbbreviation.containsKey(loc))
					loc = dict.statesAbbreviation.get(loc);
				locationA.add(loc);
			}
		}
		boolean mHasExtra = false;
		boolean aHasExtra = false;
		for (String s : locationM) {
			if (!aString.contains(s.toLowerCase()))
				mHasExtra = true;
		}
		for (String s : locationA) {
			if (!mString.contains(s.toLowerCase()))
				aHasExtra = true;
		}
		if (mHasExtra && aHasExtra) {
			return true;
		}
		return false;
	}

	public boolean isRelativePronoun(EntityMention ant, EntityMention mention, CoNLLPart part) {
		if (ant.position[0] != mention.position[0]) {
			return false;
		}
		Tree tree = part.getCoNLLSentences().get(ant.position[0]).stdTree;
		String relativePronounPattern = "NP < (NP=m1 $.. (SBAR < (WHNP < WP|WDT=m2)))";
		TregexPattern tgrepPattern = TregexPattern.compile(relativePronounPattern);
		TregexMatcher m = tgrepPattern.matcher(tree);
		while (m.find()) {
			Tree np1 = m.getNode("m1");
			Tree np2 = m.getNode("m2");
			int start1 = (int) ((CoreLabel) np1.label()).get(BeginIndexAnnotation.class);
			int end1 = (int) ((CoreLabel) np1.label()).get(EndIndexAnnotation.class) - 1;
			int start2 = (int) ((CoreLabel) np2.label()).get(BeginIndexAnnotation.class);
			int end2 = (int) ((CoreLabel) np2.label()).get(EndIndexAnnotation.class) - 1;
			if (start1 == ant.start && end1 == ant.end && start2 == mention.start && end2 == mention.end) {
				return true;
			}
		}
		return false;
	}

	public boolean isEnglishAppositive2(EntityMention ant, EntityMention mention, CoNLLPart part) {
		if (ant.position[0] != mention.position[0]) {
			return false;
		}
		Tree tree = part.getCoNLLSentences().get(ant.position[0]).stdTree;
		String pattens[] = new String[4];
		pattens[0] = "NP=m1 < (NP=m2 $.. (/,/ $.. NP=m3))";
		pattens[1] = "NP=m1 < (NP=m2 $.. (/,/ $.. (SBAR < (WHNP < WP|WDT=m3))))";
		pattens[2] = "/^NP(?:-TMP|-ADV)?$/=m1 < (NP=m2 $- /^,$/ $-- NP=m3 !$ CC|CONJP)";
		pattens[3] = "/^NP(?:-TMP|-ADV)?$/=m2 < (PRN=m3 < (NP < /^NNS?|CD$/ $-- /^-LRB-$/ $+ /^-RRB-$/))";
		for (String pattern : pattens) {
			try {
				TregexPattern tgrepPattern = TregexPattern.compile(pattern);
				TregexMatcher m = tgrepPattern.matcher(tree);
				while (m.find()) {
					Tree np2 = m.getNode("m2");
					Tree np3 = m.getNode("m3");
					int start1 = (int) ((CoreLabel) np2.label()).get(BeginIndexAnnotation.class);
					int end1 = (int) ((CoreLabel) np2.label()).get(EndIndexAnnotation.class) - 1;
					int start2 = (int) ((CoreLabel) np3.label()).get(BeginIndexAnnotation.class);
					int end2 = (int) ((CoreLabel) np3.label()).get(EndIndexAnnotation.class) - 1;
					if (start1 == ant.start && end1 == ant.end && start2 == mention.start && end2 == mention.end) {
						// System.out.println(ant.original + " # " +
						// mention.original);
						return true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return false;
	}

	public ArCommon(String language) {
		this.language = language;
	}

	public ArDictionary getArDictionary() {
		if (dict == null) {
			dict = new ArDictionary();
		}
		return dict;
	}

	public void setMentionType(EntityMention mention, CoNLLPart part) {
		if (this.getArDictionary().allPronouns.contains(mention.head) || part.getWord(mention.headStart).posTag.equals("PRP")) {
			mention.mentionType = MentionType.Pronominal;
			mention.isPronoun = true;
		} else if (!mention.ner.equalsIgnoreCase("OTHER") || part.getWord(mention.headStart).posTag.startsWith("NNP")) {
			mention.mentionType = MentionType.Proper;
		} else {
			mention.mentionType = MentionType.Nominal;
		}
	}

	public boolean clusterExactStringMatch(Entity antecedent, Entity current, CoNLLPart part) {
		for (EntityMention ant : antecedent.mentions) {
			for (EntityMention cur : current.mentions) {
				if (mentionExactStringMatch(ant, cur, part)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean mentionExactStringMatch(EntityMention ant, EntityMention m, CoNLLPart part) {
		String mSpan = m.original.toLowerCase();
		String antSpan = ant.original.toLowerCase();
		if (m.isPronoun || ant.isPronoun || this.getArDictionary().allPronouns.contains(mSpan)
				|| this.getArDictionary().allPronouns.contains(antSpan)) {
			return false;
		}
		if (mSpan.equals(antSpan) || mSpan.equals(antSpan + " 's") || antSpan.equals(mSpan + " 's")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSpeaker(EntityMention ant, EntityMention m, CoNLLPart part) {
		String antSpeaker = part.getWord(ant.headStart).speaker;
		String mSpeaker = part.getWord(m.headStart).speaker;
		for (String s : antSpeaker.split("_")) {
			if (m.head.equalsIgnoreCase(s)) {
				for (int i = m.start; i <= m.end; i++) {
					if (part.getWord(i).posTag.equalsIgnoreCase("CC")) {
						return false;
					}
				}
				return true;
			}
		}
		for (String s : mSpeaker.split("_")) {
			if (ant.head.equalsIgnoreCase(s)) {
				for (int i = ant.start; i <= ant.end; i++) {
					if (part.getWord(i).posTag.equalsIgnoreCase("CC")) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	// UTD, which is .....
	public String wordBeforeHead(EntityMention mention, CoNLLPart part) {
		int commaIndex = -1;
		int wIndex = -1;
		for (int i = mention.start; i <= mention.end; i++) {
			if (part.getWord(i).word.equals(",") && commaIndex == -1 && i > mention.headStart) {
				commaIndex = i;
			}
			if (part.getWord(i).posTag.startsWith("W") && wIndex == -1 && i > mention.headStart) {
				wIndex = i;
			}
		}
		StringBuilder sb = new StringBuilder();
		if (commaIndex != -1 && mention.headStart < commaIndex) {
			for (int i = mention.start; i < commaIndex; i++) {
				sb.append(part.getWord(i).word).append(" ");
			}
		}
		if (commaIndex == -1 && wIndex != -1 && mention.headStart < wIndex) {
			for (int i = mention.start; i < wIndex; i++) {
				sb.append(part.getWord(i).word).append(" ");
			}
		}
		return sb.toString();
	}

	public void assignHeadExtent(EntityMention em, CoNLLPart part) {
		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		int[] position = this.getPosition(em, sentences);
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

	public ArrayList<String> getFileList(String posts[]) {
		ArrayList<String> fileLists = new ArrayList<String>();
		for (String post : posts) {
			ArrayList<String> list = Common.getLines("Onto_" + post);
			for (String str : list) {
				fileLists.add(str);
			}
		}
		return fileLists;
	}

	public String ontoDataPath = "";

	// get all the ontoNotes file, given a postfix
	public ArrayList<String> getOntoFiles(String postfix) {
		ArrayList<String> filenames = new ArrayList<String>();
		String folders[] = { "nw" };
		for (String folder : folders) {
			File subFolders[] = (new File(ontoDataPath + folder)).listFiles();
			for (File subFolder : subFolders) {
				if (subFolder.isDirectory()) {
					File subFolders2[] = subFolder.listFiles();
					for (File subFolder2 : subFolders2) {
						File files[] = subFolder2.listFiles();
						for (File file : files) {
							if (file.getName().endsWith(postfix)) {
								filenames.add(file.getAbsolutePath());
							}
						}
					}
				}
			}
		}
		return filenames;
	}

	public ArrayList<EntityMention> getLongEntityMention(ArrayList<EntityMention> allMentions) {
		Collections.sort(allMentions);
		for (int i = 0; i < allMentions.size() - 1; i++) {
			EntityMention current = allMentions.get(i);
			EntityMention next = allMentions.get(i + 1);
			if (next.headStart > current.headEnd) {
				continue;
			} else {
				int currentLen = current.headEnd - current.headStart;
				int nextLen = next.headEnd - next.headStart;
				if (currentLen < nextLen) {
					allMentions.remove(i);
				} else {
					allMentions.remove(i + 1);
				}
				i--;
			}
		}
		return allMentions;
	}

	public ArrayList<EntityMention> getShortEntityMention(ArrayList<EntityMention> allMentions) {
		Collections.sort(allMentions);
		for (int i = 0; i < allMentions.size() - 1; i++) {
			EntityMention current = allMentions.get(i);
			EntityMention next = allMentions.get(i + 1);

			if (next.headStart > current.headEnd) {
				continue;
			} else {
				int currentLen = current.headEnd - current.headStart;
				int nextLen = next.headEnd - next.headStart;
				if (currentLen > nextLen) {
					allMentions.remove(i);
				} else {
					allMentions.remove(i + 1);
				}
				i--;
			}
		}
		return allMentions;
	}

	public EntityMention formPhrase(MyTreeNode treeNode, CoNLLSentence sentence) {
		ArrayList<MyTreeNode> leaves = treeNode.getLeaves();
		int startIdx = leaves.get(0).leafIdx;
		int endIdx = leaves.get(leaves.size() - 1).leafIdx;
		int start = sentence.getWord(startIdx).index;
		int end = sentence.getWord(endIdx).index;
		StringBuilder sb = new StringBuilder();
		for (int i = startIdx; i <= endIdx; i++) {
			sb.append(sentence.getWord(i).word).append(" ");
		}
		EntityMention em = new EntityMention();
		em.start = start;
		em.end = end;
		em.source = sb.toString().trim();
		return em;
	}

	// public ArrayList<Element>
	// getAllMaximalNounPhrase(ArrayList<CoNLLSentence> prs) {
	// ArrayList<Element> nounPhrases = new ArrayList<Element>();
	// for (CoNLLSentence pr : prs) {
	// Tree tree = pr.tree;
	// TreeNode root = tree.root;
	// ArrayList<TreeNode> frontie = new ArrayList<TreeNode>();
	// frontie.add(root);
	// while (frontie.size() > 0) {
	// TreeNode tn = frontie.remove(0);
	// if (tn.value.toUpperCase().startsWith("NP")) {
	// Element element = formPhrase(tn, pr);
	// if (element != null) {
	// if (element.start == -1) {
	// System.out.println();
	// }
	// nounPhrases.add(element);
	// }
	// } else {
	// ArrayList<TreeNode> tns = tn.children;
	// frontie.addAll(tns);
	// }
	// }
	//
	// }
	// return nounPhrases;
	// }

	public ArrayList<EntityMention> getAllNounPhrase(ArrayList<CoNLLSentence> sentences) {
		ArrayList<EntityMention> nounPhrases = new ArrayList<EntityMention>();
		for (CoNLLSentence sentence : sentences) {
			MyTree tree = sentence.getSyntaxTree();
			MyTreeNode root = tree.root;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				if (tn.value.toUpperCase().startsWith("NP")) {
					EntityMention element = formPhrase(tn, sentence);
					if (element != null) {
						if (element.start == -1) {
							System.out.println();
						}
						nounPhrases.add(element);
					}
				}
				ArrayList<MyTreeNode> tns = tn.children;
				frontie.addAll(tns);
			}

		}
		return nounPhrases;
	}

	// create file of file
	public void createFoF(String path, ArrayList<String> files, double curve) {
		try {
			int p = path.lastIndexOf(File.separator);
			String prefix = path.substring(0, p);
			System.out.println(path);
			FileWriter fw = new FileWriter(path);
			ArrayList<String> allLines = new ArrayList<String>();
			for (String file : files) {
				ArrayList<String> lines = Common.getLines(file);
				for (String str : lines) {
					int pos1 = str.lastIndexOf(File.separator);
					allLines.add(prefix + File.separator + str.substring(pos1 + 1) + "_0");
				}
			}
			int curveSize = (int) (curve * allLines.size());
			System.out.println(curveSize);
			for (int i = 0; i < curveSize; i++) {
				String line = allLines.get(i);
				fw.write(line + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// one is a child NP in the other's NP constituent
	public boolean isIWithI(EntityMention antecedent, EntityMention em, ArrayList<CoNLLSentence> CoNLLSentences) {
		if ((antecedent.start <= em.start && antecedent.end >= em.end)) {
			return true;
		}
		return false;
		// boolean iWithi = false;
		// MyTreeNode curTreeNode = this.getNewNPTreeNode(em, CoNLLSentences);
		// MyTreeNode canTreeNode = this.getNewNPTreeNode(antecedent,
		// CoNLLSentences);
		// ArrayList<MyTreeNode> curAncestors = curTreeNode.getAncestors();
		// ArrayList<MyTreeNode> canAncestors = canTreeNode.getAncestors();
		//
		// if (curTreeNode == canTreeNode) {
		// return false;
		// }
		// for (MyTreeNode tn : curAncestors) {
		// if (tn == canTreeNode) {
		// iWithi = true;
		// }
		// }
		// for (MyTreeNode tn : canAncestors) {
		// if (tn == curTreeNode) {
		// iWithi = true;
		// }
		// }
		// return iWithi;
	}

	public MyTreeNode getNPTreeNode(EntityMention np, ArrayList<CoNLLSentence> sentences) {
		int position[] = this.getPosition(np, sentences);
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

	// the node can not be pos node
	public MyTreeNode getNewNPTreeNode(EntityMention np, ArrayList<CoNLLSentence> sentences) {
		int position[] = this.getPosition(np, sentences);
		ArrayList<MyTreeNode> leaves = sentences.get(position[0]).getSyntaxTree().leaves;
		MyTreeNode leftNP = leaves.get(position[1]);
		MyTreeNode rightNP = leaves.get(position[2]);
		ArrayList<MyTreeNode> leftAncestors = leftNP.getAncestors();
		ArrayList<MyTreeNode> rightAncestors = rightNP.getAncestors();
		MyTreeNode commonNode = null;
		for (int i = 0; i < leftAncestors.size() && i < rightAncestors.size(); i++) {
			if (leftAncestors.get(i) == rightAncestors.get(i) && !leftAncestors.get(i).isPOSNode()) {
				commonNode = leftAncestors.get(i);
			} else {
				break;
			}
		}
		return commonNode;
	}

	public void calAttribute(EntityMention em, CoNLLPart part) {
		setMentionType(em, part);
		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		ArrayList<Element> nerElements = part.getNameEntities();
		int position[] = getPosition(em, sentences);
		em.position = position;
		int sentenceIdx = position[0];
		int firstWordIdx = position[1];
		int lastWordIdx = position[2];
		em.sentenceID = sentenceIdx;
		em.startLeaf = firstWordIdx;
		em.endLeaf = lastWordIdx;
		CoNLLSentence sentence = sentences.get(position[0]);
		assignHeadExtent(em, part);
		MyTreeNode maxTree = getMaxNPTreeNode(em, sentences);
		MyTreeNode minTree = getMinNPTreeNode(em, sentences);
		MyTreeNode tn = getNewNPTreeNode(em, sentences);
		em.maxTreeNode = maxTree;
		em.minTreeNode = minTree;
		em.treeNode = tn;

		StringBuilder sb = new StringBuilder();
		
		ArrayList<MyTreeNode> leaves = maxTree.getLeaves();
		int maxStart = sentence.getWord(leaves.get(0).leafIdx).index;
		int maxEnd = sentence.getWord(leaves.get(leaves.size() - 1).leafIdx).index;
		// get Pronoun type
		em.PRONOUN_TYPE = Common.getPronounType(em.getHead());
		em.head = em.head.replace("\n", "").replaceAll("\\s+", "");
		em.extent = em.extent.replace("\n", "").replaceAll("\\s+", "");
		for (int i = em.start; i <= em.end; i++) {
			if (i != em.headStart) {
				CoNLLWord word = part.getWord(i);
				String posTag = word.getPosTag();
				if (posTag.equals("NN") || posTag.equals("NR") || posTag.equals("OD") || posTag.equals("JJ")
						|| posTag.equals("NT") || posTag.equals("CD") || posTag.equalsIgnoreCase("RB")) {
					if (!this.getArDictionary().stopWords.contains(word.getWord())) {
						em.modifyList.add(word.getOrig().toLowerCase());
					}
				}
			}
		}
		em.isNNP = false;
		if (tn != null) {
			MyTreeNode temp = tn.parent;
			while (temp != sentence.getSyntaxTree().root && temp != null) {
				if (temp.value.toLowerCase().startsWith("np")) {
					em.isNNP = true;
				}
				temp = temp.parent;
			}
		}

		for (Element nerEle : nerElements) {
			if (em.headStart >= nerEle.start && em.headStart <= nerEle.end) {
				em.ner = nerEle.getContent();
			}
		}

		// determine whether it is a proper noun
		if (em.ner.equalsIgnoreCase("person") || em.ner.equalsIgnoreCase("gpe") || em.ner.equalsIgnoreCase("loc")
				|| em.ner.equalsIgnoreCase("org") || em.ner.equalsIgnoreCase("fac") || em.ner.equalsIgnoreCase("law")) {
			em.isProperNoun = true;
		} else {
			em.isProperNoun = false;
		}
		setGender(em, part);
		setNumber(em, part);
		setAnimacy(em, part);
		setPerson(em, part);
	}

	private void setAnimacy(EntityMention mention, CoNLLPart part) {
		String headString = mention.head.toLowerCase();
		String nerString = mention.ner;
		if (mention.isPronoun) {
			if (dict.animatePronouns.contains(headString)) {
				mention.animacy = Animacy.ANIMATE;
			} else if (dict.inanimatePronouns.contains(headString)) {
				mention.animacy = Animacy.INANIMATE;
			} else {
				mention.animacy = Animacy.UNKNOWN;
			}
		} else if (nerString.equals("PERSON")) {
			mention.animacy = Animacy.ANIMATE;
		} else if (nerString.equals("CARDINAL")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("DATE")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("EVENT")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("FAC")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("GPE")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("LAW")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.equals("LOC")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("MONEY")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("NORP")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("ORDINAL")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("ORG")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("PERCENT")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("PRODUCT")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("QUANTITY")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("TIME")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("WORK_OF_ART")) {
			mention.animacy = Animacy.INANIMATE;
		} else if (nerString.startsWith("LANGUAGE")) {
			mention.animacy = Animacy.INANIMATE;
		} else {
			mention.animacy = Animacy.UNKNOWN;
		}
	}

	private void setPerson(EntityMention mention, CoNLLPart part) {
		// only do for pronoun
		if (!mention.isPronoun) {
			mention.person = Person.UNKNOWN;
			return;
		}
		if (dict.firstPersonPronouns.contains(mention.original.toLowerCase())) {
			if (mention.number == Numb.SINGULAR) {
				mention.person = Person.I;
			} else if (mention.number == Numb.PLURAL) {
				mention.person = Person.WE;
			} else {
				mention.person = Person.UNKNOWN;
			}
		} else if (dict.secondPersonPronouns.contains(mention.original.toLowerCase())) {
			mention.person = Person.YOU;
		} else if (dict.thirdPersonPronouns.contains(mention.original.toLowerCase())) {
			if (mention.gender == Gender.MALE && mention.number == Numb.SINGULAR) {
				mention.person = Person.HE;
			} else if (mention.gender == Gender.FEMALE && mention.number == Numb.SINGULAR) {
				mention.person = Person.SHE;
			} else if ((mention.gender == Gender.NEUTRAL || mention.animacy == Animacy.INANIMATE)
					&& mention.number == Numb.SINGULAR) {
				mention.person = Person.IT;
			} else if (mention.number == Numb.PLURAL)
				mention.person = Person.THEY;
			else {
				mention.person = Person.UNKNOWN;
			}
		} else {
			mention.person = Person.UNKNOWN;
		}
	}

	private void setGender(EntityMention mention, CoNLLPart part) {
		String headString = mention.head.toLowerCase();
		mention.gender = Gender.UNKNOWN;
		if (mention.isPronoun) {
			if (this.getArDictionary().malePronouns.contains(mention.original.toLowerCase())) {
				mention.gender = Gender.MALE;
			} else if (this.getArDictionary().femalePronouns.contains(mention.original.toLowerCase())) {
				mention.gender = Gender.FEMALE;
			}
		} else {
			int genderNumberCount[] = this.getNumberCount(mention, part);
			if (genderNumberCount != null && mention.number != Numb.PLURAL) {
				double male = genderNumberCount[0];
				double female = genderNumberCount[1];
				double neutral = genderNumberCount[2];

				if (male * 0.5 > female + neutral && male > 2)
					mention.gender = Gender.MALE;
				else if (female * 0.5 > male + neutral && female > 2)
					mention.gender = Gender.FEMALE;
				else if (neutral * 0.5 > male + female && neutral > 2)
					mention.gender = Gender.NEUTRAL;
			}
		}
	}

	private int[] getNumberCount(EntityMention mention, CoNLLPart part) {
		if (mention.original.equalsIgnoreCase("Sylvia Yang , deputy chairperson of the TBAD Women 's Division")) {
			int a = 1;
		}
		int headIndex = mention.headStart;
		if (part.getWord(headIndex).rawNamedEntity.startsWith("PER")) {
			ArrayList<String> words = new ArrayList<String>();
			for (int i = mention.start; i <= headIndex; i++) {
				if (part.getWord(i).rawNamedEntity.startsWith("PER")) {
					words.add(part.getWord(i).word);
				}
			}
			for (int i = words.size(); i > 0; i--) {
				if (this.getArDictionary().bigGenderNumber.containsKey(words.subList(0, i)))
					return this.getArDictionary().bigGenderNumber.get(words.subList(0, i));
			}
		}
		return null;
	}

	public void setNumber(EntityMention mention, CoNLLPart part) {
		String headString = part.getWord(mention.headStart).orig.toLowerCase();
		if (mention.isPronoun) {
			if (dict.pluralPronouns.contains(headString)) {
				mention.number = Numb.PLURAL;
			} else if (dict.singularPronouns.contains(headString)) {
				mention.number = Numb.SINGULAR;
			} else {
				mention.number = Numb.UNKNOWN;
			}
		} else if (!mention.ner.equalsIgnoreCase("OTHER")) {
			if (!mention.ner.startsWith("ORG")) {
				mention.number = Numb.SINGULAR;
			} else {
				mention.number = Numb.UNKNOWN;
			}
		} else {
			String posTag = part.getWord(mention.headStart).posTag;
			if (posTag.startsWith("N") && posTag.endsWith("S")) {
				mention.number = Numb.PLURAL;
			} else if (posTag.startsWith("N")) {
				mention.number = Numb.SINGULAR;
			} else {
				mention.number = Numb.UNKNOWN;
			}
		}
		for (int i = mention.start; i <= mention.end; i++) {
			if (part.getWord(i).posTag.equalsIgnoreCase("CC")) {
				mention.number = Numb.PLURAL;
			}
		}
	}

	public String[] vcArray = { "am", "are", "is", "was", "were", "'m", "'re", "'s", "be" };
	public HashSet<String> vcSet;
	public String[] vcArray2 = { "be", "been", "being" };
	public HashSet<String> vcSet2;

	public boolean isEnglishCopular(EntityMention candidate, EntityMention current,
			ArrayList<CoNLLSentence> coNLLSentences) {
		if (vcSet == null) {
			vcSet = new HashSet<String>();
			vcSet.addAll(Arrays.asList(vcArray));
		}
		if (vcSet2 == null) {
			vcSet2 = new HashSet<String>();
			vcSet2.addAll(Arrays.asList(vcArray2));
		}
		int posCan[] = getPosition(candidate, coNLLSentences);
		int posCur[] = getPosition(current, coNLLSentences);
		if (posCan[0] == posCur[0]) {
			MyTreeNode tn1 = candidate.treeNode;
			MyTreeNode tn2 = current.treeNode;
			if (tn2.parent.parent == tn1.parent) {
				MyTreeNode VP = tn2.parent;
				if (VP.value.equalsIgnoreCase("VP")) {
					MyTreeNode VBZ = VP.children.get(0);
					if (VBZ.value.startsWith("VB")) {
						String vcword = VBZ.children.get(0).value;
						if (vcSet.contains(vcword.toLowerCase())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean isCopular2(EntityMention candidate, EntityMention current, ArrayList<CoNLLSentence> coNLLSentences) {
		int posCan[] = getPosition(candidate, coNLLSentences);
		int posCur[] = getPosition(current, coNLLSentences);
		// 29 heuristic COPULAR
		if (posCan[0] == posCur[0]) {
			CoNLLSentence sentence = coNLLSentences.get(posCan[0]);
			int k = posCan[2] + 1;
			for (; k < posCur[1]; k++) {
				String pos = sentence.getWord(k).getPosTag();
				if (!pos.equals("PU") && !pos.equals("-NONE-")) {
					break;
				}
			}
			if (k < sentence.getWordsCount() && sentence.getWord(k).getPosTag().equals("VC")) {
				MyTreeNode VC = sentence.getSyntaxTree().leaves.get(k);
				MyTreeNode parent = VC.parent.parent;
				int childIdx;
				for (childIdx = 0; childIdx < parent.children.size(); childIdx++) {
					if (parent.children.get(childIdx) == VC.parent) {
						break;
					}
				}
				if (childIdx + 1 < parent.children.size()) {
					MyTreeNode next = parent.children.get(childIdx + 1);
					if (next.value.startsWith("NP")) {
						MyTreeNode lastChild = next;
						while (lastChild.children.size() != 0) {
							lastChild = lastChild.children.get(lastChild.children.size() - 1);
						}
						if (lastChild.leafIdx == posCur[2]) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// public boolean isEnglishCopular(EntityMention candidate, EntityMention
	// current, CoNLLPart part) {
	// if (candidate.treeNode.parent == current.treeNode.parent) {
	//			
	// }
	// }

	public boolean isEnglishAppositive(EntityMention candidate, EntityMention mention, CoNLLPart part) {
		if (candidate.end == mention.end) {
			return false;
		}
		if (mention.end + 1 < part.getWordCount() && part.getWord(mention.end + 1).posTag.equalsIgnoreCase("CC")) {
			return false;
		}

		int childIndex1 = candidate.treeNode.childIndex;
		int childIndex2 = mention.treeNode.childIndex;
		if (candidate.original.equalsIgnoreCase("Mt. Ali")
				&& mention.original
						.equalsIgnoreCase("the only single - species forest of hinoki cypress surviving in the world today")) {
			// System.out.println(candidate.treeNode.children.get(0).value);
			// System.out.println(candidate.treeNode.parent ==
			// mention.treeNode.parent &&
			// mention.treeNode.parent.value.startsWith("NP"));
			// System.out.println(childIndex1);
			// System.out.println(childIndex2);
			// System.out.println(candidate.ner);
		}
		if (candidate.treeNode.parent == mention.treeNode.parent && mention.treeNode.parent.value.startsWith("NP")) {
			MyTreeNode parent = candidate.treeNode.parent;
			boolean haveCC = false;
			for (MyTreeNode tn : parent.children) {
				if (tn.value.equals("CC")) {
					haveCC = true;
				}
			}
			if (haveCC) {
				return false;
			}
			if (parent.children.size() > childIndex1 + 1
					&& parent.children.get(childIndex1 + 1).value.equals(",")
					&& childIndex1 + 2 == childIndex2
					&& (candidate.ner.equalsIgnoreCase("PERSON") || candidate.ner.equalsIgnoreCase("LOC")
							|| candidate.ner.equalsIgnoreCase("GPE") || candidate.ner.equalsIgnoreCase("ORG"))
					&& !mention.ner.equalsIgnoreCase("PERSON")) {
				// System.out.println(candidate.source + " " + candidate.start +
				// " " + candidate.end + " #1 "
				// + mention.source + " " + mention.start + " " + mention.end);
				return true;
			}
			if (parent.children.size() == 2 && (mention.ner.equalsIgnoreCase("PERSON"))) {
				// System.out.println(candidate.source + " " + candidate.start +
				// " " + candidate.end + " #2 "
				// + mention.source + " " + mention.start + " " + mention.end);
				return true;
			}
			if (parent.children.size() == 4 && childIndex1 == 0
					&& parent.children.get(childIndex1 + 1).value.equalsIgnoreCase("-LRB-") && childIndex2 == 2
					&& parent.children.get(childIndex2 + 1).value.equalsIgnoreCase("-RRB-")) {
				// System.out.println(candidate.source + " " + candidate.start +
				// " " + candidate.end + " #3 "
				// + mention.source + " " + mention.start + " " + mention.end);
				return true;
			}

		}
		return false;
	}

	public boolean isRoleAppositive(EntityMention can, EntityMention cur, ArrayList<CoNLLSentence> sentences) {
		if (can.end + 1 == cur.start && cur.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else if (cur.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}

	/** get position of sentenceIdx, wordStartIdx and wordEndIdx */
	public int[] getPosition(EntityMention em, ArrayList<CoNLLSentence> sentences) {
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

	public boolean isMaximalNP(EntityMention can, EntityMention cur, ArrayList<CoNLLSentence> coNLLSentences) {
		if (can.isNNP && cur.isNNP) {
			// if(can.start==145 && can.end==146 && can.extent.equals("中国")) {
			// System.out.println();
			// }
			int[] position1 = getPosition(can, coNLLSentences);
			int[] position2 = getPosition(cur, coNLLSentences);
			if (position1[0] != position2[0]) {
				return false;
			}
			int canNPIdx = position1[1];
			int curNPIdx = position2[1];
			int sentenceIdx = position1[0];
			MyTree tree = coNLLSentences.get(sentenceIdx).getSyntaxTree();
			MyTreeNode leaf1 = null;
			try {
				leaf1 = tree.leaves.get(canNPIdx);
			} catch (Exception e) {
				System.out.println(can.toString());
				// System.exit(1);
			}
			StringBuilder sb = new StringBuilder();
			for (String modify : can.modifyList) {
				sb.append(modify);
			}
			// System.out.println(can + "#" + can.source+"#"+can.extent +
			// "#"+sb.toString());
			MyTreeNode leaf2 = tree.leaves.get(curNPIdx);
			MyTreeNode maxNP1 = leaf1;
			MyTreeNode parent1 = leaf1.parent;
			while (parent1 != tree.root) {
				if (parent1.value.equals("NP")) {
					maxNP1 = parent1;
				}
				parent1 = parent1.parent;
			}

			MyTreeNode maxNP2 = leaf2;
			MyTreeNode parent2 = leaf2.parent;
			while (parent2 != tree.root) {
				if (parent2.value.equals("NP")) {
					maxNP2 = parent2;
				}
				parent2 = parent2.parent;
			}
			if (maxNP1 == maxNP2) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void main(String args[]) {
		// Common.init();
		// ArrayList<String> files = Common.getLines("ACE_0");
		// String crfFile = "D:\\ACL12\\model\\ACE\\semantic\\FAC\\result";
		// ArrayList<ArrayList<Element>> elementses =
		// CorefACECommon.getSemanticsFromCRFFile(files, crfFile);
		// for(ArrayList<Element> elements : elementses) {
		// for(Element el:elements) {
		// System.out.println(el.start + " " + el.end + " " + el.content + " " +
		// el.confidence);
		// }
		// }
	}

	public MyTreeNode getMaxNPTreeNode(EntityMention np, ArrayList<CoNLLSentence> sentences) {
		int sentenceIdx = 0;
		int lastWordIdx = 0;
		int position[] = getPosition(np, sentences);
		sentenceIdx = position[0];
		lastWordIdx = position[2];
		ArrayList<MyTreeNode> leaves = sentences.get(sentenceIdx).getSyntaxTree().leaves;
		MyTreeNode rightNp = leaves.get(lastWordIdx);
		ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
		MyTreeNode NP = null;
		for (int i = rightAncestors.size() - 1; i >= 0; i--) {
			MyTreeNode tmp = rightAncestors.get(i);
			ArrayList<MyTreeNode> tmpLeaves = tmp.getLeaves();
			if ((tmp.value.toLowerCase().startsWith("np") || tmp.value.toLowerCase().startsWith("qp"))
					&& tmpLeaves.get(tmpLeaves.size() - 1).leafIdx == lastWordIdx) {
				NP = tmp;
			}
		}
		if (NP == null) {
			NP = rightNp.parent;
		}
		return NP;
	}

	public MyTreeNode getMinNPTreeNode(EntityMention np, ArrayList<CoNLLSentence> sentences) {
		int position[] = getPosition(np, sentences);
		int sentenceIdx = position[0];
		int endLeaf = position[2];
		ArrayList<MyTreeNode> leaves = sentences.get(sentenceIdx).getSyntaxTree().leaves;
		MyTreeNode rightNp = leaves.get(endLeaf);
		// System.out.println(npWordEndIdx +np.getContent());
		ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
		MyTreeNode NP = null;
		for (int i = 0; i < rightAncestors.size(); i++) {
			MyTreeNode tmp = rightAncestors.get(i);
			ArrayList<MyTreeNode> tmpLeaves = tmp.getLeaves();
			if ((tmp.value.toLowerCase().startsWith("np") || tmp.value.toLowerCase().startsWith("qp"))
					&& tmpLeaves.get(tmpLeaves.size() - 1).leafIdx == np.endLeaf) {
				NP = tmp;
			}
		}
		if (NP == null) {
			NP = rightNp.parent;
		}
		return NP;
	}

	public String getSemanticSymbol(EntityMention em, String head) {
		if (head.charAt(0) == '副') {
			head = head.substring(1);
		}
		if (!em.head.endsWith(head)) {
			head = em.head;
			// System.out.println(head + " " + em.head + "######");
		} else {

		}
		String semantics[] = Common.getSemanticDic().get(head);
		String semantic = "";
		if (semantics != null) {
			semantic = semantics[0];
		} else {
			boolean findNer = false;
			if (em.ner.equalsIgnoreCase("PERSON")) {
				semantic = "A0000001";
			} else if (em.ner.equalsIgnoreCase("LOC")) {
				semantic = "Be000000";
			} else if (em.ner.equalsIgnoreCase("GPE")) {
				semantic = "Di020000";
			} else if (em.ner.equalsIgnoreCase("ORG")) {
				semantic = "Dm000000";
			} else {
				// System.out.println(ele.content + " " + em.head);
			}
			findNer = true;
			if (!findNer) {
				if (head.endsWith("们") || head.endsWith("人") || head.endsWith("者") || head.endsWith("哥")
						|| head.endsWith("员") || head.endsWith("弟") || head.endsWith("爸")) {
					semantic = "A0000001";
				}
			}
		}
		return semantic;
	}

	public boolean isEnglishRoleAppositive(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		if (antecedent.isPronoun || this.getArDictionary().allPronouns.contains(antecedent.original.toLowerCase()))
			return false;
		if (antecedent.start < mention.start && mention.end == antecedent.end && mention.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean attributeAgree(EntityMention antecedent, EntityMention mention) {

		boolean hasExtraAnt = false;
		boolean hasExtraThis = false;

		HashSet<Gender> genders1 = new HashSet<Gender>();
		HashSet<Numb> numbs1 = new HashSet<Numb>();
		HashSet<Animacy> animacys1 = new HashSet<Animacy>();
		HashSet<Gender> genders2 = new HashSet<Gender>();
		HashSet<Numb> numbs2 = new HashSet<Numb>();
		HashSet<Animacy> animacys2 = new HashSet<Animacy>();
		HashSet<String> NEs1 = new HashSet<String>();
		HashSet<String> NEs2 = new HashSet<String>();
		for (EntityMention m : antecedent.entity.mentions) {
			genders1.add(m.gender);
			numbs1.add(m.number);
			animacys1.add(m.animacy);
			NEs1.add(m.ner);
		}
		if (genders1.size() > 1 && genders1.contains(Gender.UNKNOWN)) {
			genders1.remove(Gender.UNKNOWN);
		}
		if (numbs1.size() > 1 && numbs1.contains(Numb.UNKNOWN)) {
			numbs1.remove(Numb.UNKNOWN);
		}
		if (animacys1.size() > 1 && animacys1.contains(Animacy.UNKNOWN)) {
			animacys1.remove(Animacy.UNKNOWN);
		}
		if (NEs1.size() > 1 && NEs1.contains("OTHER")) {
			NEs1.remove("OTHER");
		}

		if (genders2.size() > 1 && genders2.contains(Gender.UNKNOWN)) {
			genders2.remove(Gender.UNKNOWN);
		}
		if (numbs2.size() > 1 && numbs2.contains(Numb.UNKNOWN)) {
			numbs2.remove(Numb.UNKNOWN);
		}
		if (animacys2.size() > 1 && animacys2.contains(Animacy.UNKNOWN)) {
			animacys2.remove(Animacy.UNKNOWN);
		}
		if (NEs2.size() > 1 && NEs2.contains("OTHER")) {
			NEs2.remove("OTHER");
		}

		for (EntityMention m : mention.entity.mentions) {
			genders2.add(m.gender);
			numbs2.add(m.number);
			animacys2.add(m.animacy);
			NEs2.add(m.ner);
		}
		// System.out.println("OK1: " + antecedent.gender + " " +
		// mention.gender);
		if (!genders1.contains(Gender.UNKNOWN)) {
			for (Gender gender : genders2) {
				if (!genders1.contains(gender) && gender != Gender.UNKNOWN) {
					hasExtraAnt = true;
				}
			}
		}
		// System.out.println("OK2");
		if (!genders2.contains(Gender.UNKNOWN)) {
			for (Gender gender : genders1) {
				if (!genders2.contains(gender) && gender != Gender.UNKNOWN) {
					hasExtraThis = true;
				}
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		// Numb
		hasExtraAnt = false;
		hasExtraThis = false;

		// System.out.println("OK3");
		if (!numbs1.contains(Numb.UNKNOWN)) {
			for (Numb numb : numbs2) {
				if (!numbs1.contains(numb) && numb != Numb.UNKNOWN) {
					hasExtraAnt = true;
				}
			}
		}
		// System.out.println("OK4");
		if (!numbs2.contains(Numb.UNKNOWN)) {
			for (Numb numb : numbs1) {
				if (!numbs2.contains(numb) && numb != Numb.UNKNOWN) {
					hasExtraThis = true;
				}
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		// Numb
		hasExtraAnt = false;
		hasExtraThis = false;

		// System.out.println("OK5");
		if (!animacys1.contains(Animacy.UNKNOWN)) {
			for (Animacy animacy : animacys2) {
				if (!animacys1.contains(animacy) && animacy != Animacy.UNKNOWN) {
					hasExtraAnt = true;
				}
			}
		}

		// System.out.println("OK6");
		if (!animacys2.contains(Animacy.UNKNOWN)) {
			for (Animacy animacy : animacys1) {
				if (!animacys2.contains(animacy) && animacy != Animacy.UNKNOWN) {
					hasExtraThis = true;
				}
			}
		}
		if (hasExtraAnt && hasExtraThis)
			return false;

		// System.out.println("OK7");
		if (!NEs1.contains("OTHER")) {
			for (String str : NEs2) {
				if (!NEs1.contains(str) && !str.equalsIgnoreCase("OTHER")) {
					hasExtraAnt = true;
				}
			}
		}
		// System.out.println("OK8");
		if (!NEs2.contains("OTHER")) {
			for (String str : NEs1) {
				if (!NEs2.contains(str) && !str.equalsIgnoreCase("OTHER")) {
					hasExtraThis = true;
				}
			}
		}
		if (hasExtraAnt && hasExtraThis)
			return false;
		else
			return true;
	}

	public boolean isEnglishAcronym(EntityMention antecedent, EntityMention em) {
		for (EntityMention em11 : antecedent.entity.mentions) {
			if (em11.isPronoun) {
				continue;
			}
			for (EntityMention em22 : em.entity.mentions) {
				if (acronym(em11.original, em22.original) || acronym(em22.original, em11.original)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean acronym(String s1, String s2) {
		String acronym = "";
		// make s1 shorter (acronym)
		if (s1.length() > s2.length()) {
			String temp = s1;
			s1 = s2;
			s2 = temp;
		}
		for (int i = 0; i < s2.length(); i++) {
			if (s2.charAt(i) >= 'A' && s2.charAt(i) <= 'Z') {
				acronym += s2.charAt(i);
			}
		}
		if (acronym.equals(s1) && !s2.contains(s1))
			return true;
		return false;
	}

	public boolean isDemonym(EntityMention em1, EntityMention em2) {
		String thisString = em1.original.toLowerCase();
		String antString = em2.original.toLowerCase();
		if (thisString.startsWith("the ")) {
			thisString = thisString.substring(4);
		}
		if (antString.startsWith("the ")) {
			antString = antString.substring(4);
		}
		if (dict.statesAbbreviation.containsKey(em1.original)
				&& dict.statesAbbreviation.get(em1.original).equals(em2.original)
				|| dict.statesAbbreviation.containsKey(em2.original)
				&& dict.statesAbbreviation.get(em1.original).equals(em1.original))
			return true;
		if (dict.demonyms.get(thisString) != null) {
			if (dict.demonyms.get(thisString).contains(antString))
				return true;
		} else if (dict.demonyms.get(antString) != null) {
			if (dict.demonyms.get(antString).contains(thisString))
				return true;
		}
		return false;
	}

	public boolean isRoleAppositive(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		if (!this.attributeAgree(antecedent, mention)) {
			return false;
		}
		if (this.isEnglishRoleAppositive(antecedent, mention, part)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isRelativePronoun(EntityMention antecedent, EntityMention em) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isEnglishDemonym(EntityMention antecedent, EntityMention em) {
		String thisString = antecedent.original.toLowerCase();
		String antString = em.original.toLowerCase();
		if (thisString.startsWith("the ") || thisString.startsWith("The ")) {
			thisString = thisString.substring(4);
		}
		if (antString.startsWith("the ") || antString.startsWith("The "))
			antString = antString.substring(4);

		if (this.getArDictionary().statesAbbreviation.containsKey(em.original)
				&& this.getArDictionary().statesAbbreviation.get(em.original).equals(antecedent.original)
				|| this.getArDictionary().statesAbbreviation.containsKey(antecedent.original)
				&& this.getArDictionary().statesAbbreviation.get(antecedent.original).equals(em.original))
			return true;

		if (this.getArDictionary().demonyms.get(thisString) != null) {
			if (this.getArDictionary().demonyms.get(thisString).contains(antString))
				return true;
		} else if (this.getArDictionary().demonyms.get(antString) != null) {
			if (this.getArDictionary().demonyms.get(antString).contains(thisString))
				return true;
		}
		return false;
	}
}
