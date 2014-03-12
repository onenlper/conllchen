package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import model.EntityMention.MentionType;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import ruleCoreference.chinese.ChDictionary;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;
import ace.ACECommon;
import ace.model.EventMention;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class ChCommon {

	private String language;

	public static ChDictionary dict = new ChDictionary();

	public static HashMap<String, ArrayList<Element>> predictNEs;

//	public static HashMap<String, HashSet<String>> loadAnaphorResult(String folder) {
//		HashMap<String, HashSet<String>> rets = new HashMap<String, HashSet<String>>();
//		ArrayList<String> mids = Common.getLines("mids." + folder);
//		ArrayList<String> results = Common.getLines("/users/yzcchen/tool/svmlight/result." + folder);
//		
//		for(int i=0;i<mids.size();i++) {
//			double result = Double.parseDouble(results.get(i));
//			int k = mids.get(i).lastIndexOf(" ");
//			String key = mids.get(i).substring(0, k);
//			HashSet<String> set = rets.get(key);
//			if(set==null) {
//				set = new HashSet<String>();
//				rets.put(key, set);
//			}
//			if(result>-0.0) {
//				set.add(mids.get(i).substring(k+1));
//			}
//		}
//		return rets;
//	}
	
	public static void loadPredictNE(String folder, String mode) {
		if (predictNEs == null) {
			predictNEs = new HashMap<String, ArrayList<Element>>();
			String fn = "chinese_" + folder + ".neresult";
			if (mode.equals("test")) {
				fn = "chinese_" + folder + ".neresult.test";
			}
			ArrayList<String> lines = Common.getLines(fn);
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
				String key = docID + "_" + partID;
				if (label.startsWith("B")) {
					String content = label.substring(2);
					int k = i + 1;
					while (!lines.get(k).isEmpty() && lines.get(k).trim().endsWith("I")) {
						k++;
					}
					int start = wordID;
					int end = Integer.valueOf(lines.get(k - 1).split("\\s+")[length - 3]);
					Element element = new Element();
					element.start = start;
					element.end = end;
					element.content = content;
					if (predictNEs.containsKey(key)) {
						predictNEs.get(key).add(element);
					} else {
						ArrayList<Element> ems = new ArrayList<Element>();
						ems.add(element);
						predictNEs.put(key, ems);
					}
				}
			}
		}
	}

	public boolean numberInLaterMention(EntityMention ant, EntityMention mention, CoNLLPart part) {
		Set<String> antecedentWords = new HashSet<String>();
		Set<String> numbers = new HashSet<String>();
		numbers.addAll(this.getChDictionary().singleWords);
		numbers.addAll(this.getChDictionary().pluralWords);
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
	public boolean chHaveDifferentLocation(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		// state and country cannot be coref
		if ((this.getChDictionary().statesAbbreviation.containsKey(antecedent.original) || this.getChDictionary().statesAbbreviation
				.containsValue(mention.original))
				&& (antecedent.head.equalsIgnoreCase("国")))
			return true;
		Set<String> locationM = new HashSet<String>();
		Set<String> locationA = new HashSet<String>();
		String mString = mention.original.toLowerCase();
		String aString = antecedent.original.toLowerCase();
		Set<String> locationModifier = new HashSet<String>(Arrays.asList("东", "南", "西", "北", "中", "东面", "南面", "西面",
				"北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧"));

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

	public ChCommon(String language) {
		this.language = language;
	}

	public ChDictionary getChDictionary() {
		if (dict == null) {
			dict = new ChDictionary();
		}
		return dict;
	}

	public void setMentionType(EntityMention mention, CoNLLPart part) {
		if (part.getWord(mention.headStart).posTag.startsWith("PN") || (Common.isPronoun(mention.head))) {
			mention.mentionType = MentionType.Pronominal;
			mention.isPronoun = true;
		} else if ((!mention.ner.equalsIgnoreCase("OTHER") && !mention.ner.equalsIgnoreCase("CARDINAL"))
				|| part.getWord(mention.headStart).posTag.startsWith("NR")) {
			mention.mentionType = MentionType.Proper;
			mention.isProperNoun = true;
		} else {
			mention.mentionType = MentionType.Nominal;
		}
		String head = mention.head;
		if (Common.getSemantic(head) == null) {
			mention.mentionType = MentionType.Proper;
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
		if (m.isPronoun || ant.isPronoun || this.getChDictionary().allPronouns.contains(mSpan)
				|| this.getChDictionary().allPronouns.contains(antSpan)) {
			return false;
		}
		if (mSpan.equals(antSpan) || mSpan.equals(antSpan + " 's") || antSpan.equals(mSpan + " 's")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSpeaker(EntityMention ant, EntityMention m, CoNLLPart part) {
		if (part.getDocument().getType() == DocType.Conversation) {
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
		} else {
			EntityMention antSpeakerM = part.getWord(ant.headStart).speakerM;
			if (antSpeakerM != null) {
				this.assignHeadExtent(antSpeakerM, part);
				if (antSpeakerM.equals(m) || (!Common.isPronoun(antSpeakerM.head) && antSpeakerM.head.equals(m.head))) {
					return true;
				}
			}
			EntityMention mSpeakerM = part.getWord(m.headStart).speakerM;
			if (mSpeakerM != null) {
				this.assignHeadExtent(mSpeakerM, part);
				if (mSpeakerM != null && (mSpeakerM.equals(ant) || (mSpeakerM.head.equals(ant.head)))) {
					return true;
				}
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
		if (this.language.equalsIgnoreCase("chinese")) {
			int headPos = em.end;
			while (part.getWord(headPos).posTag.equals("PU")) {
				headPos--;
			}
			if (part.getWord(em.end).word.equals("自己") && em.end != em.start) {
				headPos--;
			}
			em.headStart = headPos;
			em.headEnd = headPos;
			em.head = part.getWord(em.headStart).orig;
		} else if (this.language.equalsIgnoreCase("english")) {
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
		em.source = sb.toString();
		em.NP = treeNode;
		
		
		boolean subject = false;
		if (em.NP.parent == null) {
			
		} else {
			for (int i = em.NP.childIndex + 1; i < em.NP.parent.children
					.size(); i++) {
				MyTreeNode sibling = em.NP.parent.children.get(i);
				if (sibling.value.equals("VP")) {
					subject = true;
					em.V = sibling;
					break;
				}
			}
			if (subject) {
				em.isSub = true;
			} 
		}
		
		return em;
	}

	public ArrayList<EntityMention> getAllNounPhrase(ArrayList<CoNLLSentence> sentences) {
		ArrayList<EntityMention> nounPhrases = new ArrayList<EntityMention>();
		for (CoNLLSentence sentence : sentences) {
			MyTree tree = sentence.getSyntaxTree();
			MyTreeNode root = tree.root;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				if ((this.language.equalsIgnoreCase("chinese") && (tn.value.toUpperCase().startsWith("NP")))) {
					EntityMention element = formPhrase(tn, sentence);
					if (element != null) {
						if (element.start == -1) {
//							System.out.println();
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
		if ((antecedent.start <= em.start && antecedent.end > em.end)) {
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

	public static void calEventFeature(EventMention eventMention, CoNLLPart part,
			ArrayList<EntityMention> argumentCandidate) {
		if (ACECommon.isZeroPronoun(eventMention, part, argumentCandidate)) {
			boolean pasive = false;
			if (eventMention.head.startsWith("被")
					|| (eventMention.headCharStart > 0 && part.rawText.charAt(eventMention.headCharStart - 1) == '被')) {
				pasive = true;
			}
			if (pasive) {
				if (!eventMention.srlArgs.containsKey("A1")) {
					ArrayList<EntityMention> srlRoles = new ArrayList<EntityMention>();
					srlRoles.add(eventMention.zeroSubjects.get(0));
					eventMention.srlArgs.put("A1", srlRoles);
				}
			} else {
				if (!eventMention.srlArgs.containsKey("A0")) {
					ArrayList<EntityMention> srlRoles = new ArrayList<EntityMention>();
					srlRoles.add(eventMention.zeroSubjects.get(0));
					eventMention.srlArgs.put("A0", srlRoles);
				}
			}
		}
	}

	public void calEventNounAttribute(EntityMention em, CoNLLPart part) {
		em.head = em.head.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.extent = em.extent.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.source = em.source.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.original = em.original.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");

		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		int position[] = getPosition(em, sentences);
		em.position = position;
		int sentenceIdx = position[0];
		int firstWordIdx = position[1];
		int lastWordIdx = position[2];
		em.sentenceID = sentenceIdx;
		em.startLeaf = firstWordIdx;
		em.endLeaf = lastWordIdx;
		// assignHeadExtent(em, part);
		MyTreeNode maxTree = getMaxNPTreeNode(em, sentences);
		MyTreeNode minTree = getMinNPTreeNode(em, sentences);
		MyTreeNode tn = getNewNPTreeNode(em, sentences);
		em.maxTreeNode = maxTree;
		em.minTreeNode = minTree;
		em.treeNode = tn;
		// em.extent = maxTree.toString();
		MyTree tree = sentences.get(position[0]).getSyntaxTree();

		ArrayList<String> depends = part.getCoNLLSentences().get(sentenceIdx).depends;

		HashSet<String> nonModifyPOS = new HashSet<String>(Arrays
				.asList("DEG", "P", "CC", "DT", "M", "LC", "DEC", "VV"));

		for (String depend : depends) {
			String strs[] = depend.split(" ");
			String type = strs[0];
			int wordIdx1 = Integer.parseInt(strs[1]) - 1;
			int wordIdx2 = Integer.parseInt(strs[2]) - 1;
			if ((type.equals("nsubj") || type.equals("xsubj")) && wordIdx2 == lastWordIdx) {
				// System.out.println(em.getContent());
				em.isSub = true;
			}
			if ((type.endsWith("mod") || type.equals("nn")) && wordIdx1 == lastWordIdx) {
				String word1 = part.getCoNLLSentences().get(sentenceIdx).words.get(wordIdx1).getWord();

				CoNLLWord w2 = part.getCoNLLSentences().get(sentenceIdx).words.get(wordIdx2);
				String word2 = w2.getWord();
				if (nonModifyPOS.contains(w2.posTag)) {
					continue;
				}
				if (w2.posTag.equals("CD") && !isNumber(word2)) {
					continue;
				}

				if (!em.head.contains(word2) && !word2.contains(em.head)) {
					em.modifyList.add(word2);
				}
			}
		}

		for (MyTreeNode leaf : em.maxTreeNode.getLeaves()) {
			if (!leaf.value.contains(em.head) && !em.modifyList.contains(leaf.value)
					&& !nonModifyPOS.contains(leaf.parent.value) && !em.head.contains(leaf.value)) {
				if (leaf.parent.value.equals("CD") && !isNumber(leaf.value)) {
					continue;
				}
				em.modifyList.add(leaf.value);
			}
		}

		for (String str : em.modifyList) {
			if (dict.pluralModify.contains(str)) {
				em.number = Numb.PLURAL;
			}
		}

	}

	public boolean isNumber(String str) {
		try {
			Integer i = Integer.valueOf(str);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	public void calACEAttribute(EntityMention em, CoNLLPart part) {
		if (em instanceof EventMention) {
			return;
		}
		em.head = em.head.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.extent = em.extent.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.source = em.source.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
		em.original = em.original.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");

		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		int position[] = getPosition(em, sentences);
		em.position = position;
		int sentenceIdx = position[0];
		int firstWordIdx = position[1];
		int lastWordIdx = position[2];
		em.sentenceID = sentenceIdx;
		em.startLeaf = firstWordIdx;
		em.endLeaf = lastWordIdx;
		// assignHeadExtent(em, part);
		MyTreeNode maxTree = getMaxNPTreeNode(em, sentences);
		MyTreeNode minTree = getMinNPTreeNode(em, sentences);
		MyTreeNode tn = getNewNPTreeNode(em, sentences);
		em.maxTreeNode = maxTree;
		em.minTreeNode = minTree;
		em.treeNode = tn;
		// em.extent = maxTree.toString();
		MyTree tree = sentences.get(position[0]).getSyntaxTree();

		ArrayList<String> depends = part.getCoNLLSentences().get(sentenceIdx).depends;
		for (String depend : depends) {
			String strs[] = depend.split(" ");
			String type = strs[0];
			int wordIdx1 = Integer.parseInt(strs[1]) - 1;
			int wordIdx2 = Integer.parseInt(strs[2]) - 1;
			if ((type.equals("nsubj") || type.equals("xsubj")) && wordIdx2 == lastWordIdx) {
				// System.out.println(em.getContent());
				em.isSub = true;
			}
			if ((type.endsWith("mod") || type.equals("nn")) && wordIdx1 == lastWordIdx) {
				String word1 = part.getCoNLLSentences().get(sentenceIdx).words.get(wordIdx1).getWord();
				String word2 = part.getCoNLLSentences().get(sentenceIdx).words.get(wordIdx2).getWord();
				if (!em.head.contains(word2)) {
					em.modifyList.add(word2);
				}
			}
		}
		// assign ne
		for (Element element : part.getNameEntities()) {
			if (em.headCharEnd == element.end) {
				em.ner = element.content;
			}
		}
		setMentionType(em, part);
		setGender(em, part);
		setNumber(em, part);
		setAnimacy(em, part);
		setPerson(em, part);
		if (em.animacy == Animacy.INANIMATE) {
			em.gender = Gender.UNKNOWN;
		}
		String sems[] = Common.getSemantic(em.head);
		if (ruleCoreference.chinese.RuleCoref.open && sems != null) {
			String sem = sems[0];
			if (em.animacy == Animacy.UNKNOWN) {
				if (sem.startsWith("A")) {
					em.animacy = Animacy.ANIMATE;
				} else {
					em.animacy = Animacy.INANIMATE;
				}
			}
		}

		if (em.animacy == Animacy.UNKNOWN) {
			// Animacy
			if (em.semClass.equalsIgnoreCase("PER")) {
				em.animacy = Animacy.ANIMATE;
			} else if (!em.semClass.equalsIgnoreCase("PER")) {
				em.animacy = Animacy.INANIMATE;
			}
		}

		if (em.number == Numb.UNKNOWN) {
			// Number
			if (em.subType.equalsIgnoreCase("p-group")) {
				em.number = Numb.PLURAL;
			} else if (em.subType.equalsIgnoreCase("p-individual")) {
				em.number = Numb.SINGULAR;
			}
		}
	}

	public static ArrayList<String> subTypes = new ArrayList<String>(Arrays.asList("f-airport", "f-building-grounds",
			"f-path", "f-plant", "f-subarea-facility", "g-continent", "g-county-or-district", "g-gpe-cluster",
			"g-nation", "g-population-center", "g-special", "g-state-or-province", "l-address", "l-boundary",
			"l-celestial", "l-land-region-natural", "l-region-general", "l-region-international", "l-water-body",
			"o-commercial", "o-educational", "o-entertainment", "o-government", "o-media", "o-medical-science",
			"o-non-governmental", "o-religious", "o-sports", "p-group", "p-indeterminate", "p-individual", "v-air",
			"v-land", "v-subarea-vehicle", "v-underspecified", "v-water", "w-biological", "w-blunt", "w-chemical",
			"w-exploding", "w-nuclear", "w-projectile", "w-sharp", "w-shooting", "w-underspecified", "o-other"));

	public static ArrayList<String> semClasses = new ArrayList<String>(Arrays.asList("wea", "veh", "per", "fac", "gpe",
			"loc", "org"));

	public static HashMap<String, ArrayList<EntityMention>> loadSemanticResult() {
		HashMap<String, ArrayList<EntityMention>> allSVMResult = new HashMap<String, ArrayList<EntityMention>>();
		String folder = "/users/yzcchen/ACL12/model/ACE2005/semantic3/";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test" + Common.part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder + "multiType.result2" + Common.part);
		ArrayList<String> subTypeResult = Common.getLines(folder + "multiSubType.result2" + Common.part);

		for (int i = 0; i < mentionStrs.size(); i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headCharStart = headStart;
			em.headCharEnd = headEnd;

			int typeIndex = Integer.valueOf(typeResult.get(i).split("\\s+")[0]);
			int subTypeIndex = Integer.valueOf(subTypeResult.get(i).split("\\s+")[0]);

			em.semClass = semClasses.get(typeIndex - 1);
			em.subType = subTypes.get(subTypeIndex - 1);

			if (allSVMResult.containsKey(fileKey)) {
				allSVMResult.get(fileKey).add(em);
			} else {
				ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
				ems.add(em);
				allSVMResult.put(fileKey, ems);
			}
		}
		return allSVMResult;
	}

	public void calChAttribute(EntityMention em, CoNLLPart part) {
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
		assignHeadExtent(em, part);
		MyTreeNode maxTree = getMaxNPTreeNode(em, sentences);
		MyTreeNode minTree = getMinNPTreeNode(em, sentences);
		MyTreeNode tn = getNewNPTreeNode(em, sentences);
		em.maxTreeNode = maxTree;
		em.minTreeNode = minTree;
		em.treeNode = tn;
		em.extent = maxTree.toString();
		em.source = em.source.replace(" ", "");
		MyTree tree = sentences.get(position[0]).getSyntaxTree();

		int begin = maxTree.getLeaves().get(0).leafIdx;
		int end = minTree.getLeaves().get(0).leafIdx;

		for (int i = begin; i < end; i++) {
			em.modifyList.add(tree.leaves.get(i).value);
		}

		for (Element nerEle : nerElements) {
			if (em.headStart >= nerEle.start && em.headStart <= nerEle.end) {
				em.ner = nerEle.getContent();
			}
		}
		setMentionType(em, part);
		setGender(em, part);
		setNumber(em, part);
		setAnimacy(em, part);
		setPerson(em, part);
		if (em.animacy == Animacy.INANIMATE) {
			em.gender = Gender.UNKNOWN;
		}
		String sems[] = Common.getSemantic(em.head);
		if (ruleCoreference.chinese.RuleCoref.open && sems != null) {
			String sem = sems[0];
			if (em.animacy == Animacy.UNKNOWN) {
				if (sem.startsWith("A")) {
					em.animacy = Animacy.ANIMATE;
				} else {
					em.animacy = Animacy.INANIMATE;
				}
			}
		}
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
			mention.isNT = true;
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
		if (part.getWord(mention.headEnd).posTag.equalsIgnoreCase("NT")
				|| this.getChDictionary().temporals.contains(mention.head)) {
			mention.animacy = Animacy.INANIMATE;
			mention.isNT = true;
		} else {
			mention.isNT = false;
		}
		if (mention.head.endsWith("人") || mention.head.endsWith("者")) {
			mention.animacy = Animacy.ANIMATE;
		}
		if (mention.animacy == Animacy.UNKNOWN && (mention.source.contains("这") || mention.source.contains("那"))) {
			mention.animacy = Animacy.INANIMATE;
		}
		if (mention.animacy == Animacy.UNKNOWN
				&& this.getChDictionary().animatePronouns.contains(part.getWord(mention.start).word)) {
			mention.animacy = Animacy.INANIMATE;
		}
		if (mention.animacy == Animacy.UNKNOWN) {
			int yes = 0;
			int no = 0;
			Integer in;
			if ((in = this.getChDictionary().animateHead.get(mention.head)) != null) {
				yes = in.intValue();
			}
			if ((in = this.getChDictionary().inanimateHead.get(mention.head)) != null) {
				no = in.intValue();
			}
			if (yes > no) {
				mention.animacy = Animacy.ANIMATE;
			} else if (yes < no) {
				mention.animacy = Animacy.INANIMATE;
			}
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
			if (dict.singularPronouns.contains(mention.original.toLowerCase())) {
				mention.person = Person.YOU;
			} else {
				mention.person = Person.YOUS;
			}
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
		mention.gender = Gender.UNKNOWN;
		if (mention.isPronoun) {
			if (this.getChDictionary().malePronouns.contains(mention.original.toLowerCase())) {
				mention.gender = Gender.MALE;
			} else if (this.getChDictionary().femalePronouns.contains(mention.original.toLowerCase())) {
				mention.gender = Gender.FEMALE;
			}
		} else {
			mention.gender = Gender.UNKNOWN;
		}
		if (mention.gender == Gender.UNKNOWN) {
			int yes = 0;
			int no = 0;
			Integer in;
			if ((in = this.getChDictionary().maleHead.get(mention.head)) != null) {
				yes = in.intValue();
			}
			if ((in = this.getChDictionary().femaleHead.get(mention.head)) != null) {
				no = in.intValue();
			}
			if (yes > no) {
				mention.gender = Gender.MALE;
			} else if (yes < no) {
				mention.gender = Gender.FEMALE;
			}
		}
	}

	public void setNumber(EntityMention mention, CoNLLPart part) {
		String headString = part.getWord(mention.headStart).orig.toLowerCase();
		mention.number = Numb.UNKNOWN;
		if (mention.isPronoun) {
			// 我们自己
			if (dict.reflexivePronouns.contains(headString) && mention.start < mention.headStart) {
				headString = part.getWord(mention.headStart - 1).orig;
			}
			if (dict.pluralPronouns.contains(headString)) {
				mention.number = Numb.PLURAL;
			} else if (dict.singularPronouns.contains(headString)) {
				mention.number = Numb.SINGULAR;
			} else {
				mention.number = Numb.UNKNOWN;
			}
		} else if (!mention.ner.equalsIgnoreCase("OTHER")) {
			mention.number = Numb.SINGULAR;
		} else {
			if (mention.head.endsWith("们")) {
				mention.number = Numb.PLURAL;
			}
			for (int i = mention.start; i <= mention.end; i++) {
				String pos = part.getWord(i).posTag;
				if (pos.equalsIgnoreCase("CD")) {
					String word = part.getWord(i).word;
					if (word.equals("一") || word.equals("1")) {
						mention.number = Numb.SINGULAR;
					} else {
						mention.number = Numb.PLURAL;
					}
					break;
				}
			}
		}
		for (int i = 0; i < mention.source.length(); i++) {
			if (this.getChDictionary().pluralWords.contains(mention.source.substring(i, i + 1))) {
				mention.number = Numb.PLURAL;
				break;
			}
		}
		if ((mention.source.startsWith("这") || mention.original.startsWith("那")) && mention.number == Numb.UNKNOWN) {
			mention.number = Numb.SINGULAR;
		}
		if (mention.number == Numb.UNKNOWN) {
			int yes = 0;
			int no = 0;
			Integer in;
			if ((in = this.getChDictionary().singleHead.get(mention.head)) != null) {
				yes = in.intValue();
			}
			if ((in = this.getChDictionary().pluraHead.get(mention.head)) != null) {
				no = in.intValue();
			}
			if (yes > no) {
				mention.number = Numb.SINGULAR;
			} else if (yes < no) {
				mention.number = Numb.PLURAL;
			}
		}
		if (mention.ner.equalsIgnoreCase("GPE") || mention.ner.equalsIgnoreCase("PERSON")
				|| mention.ner.equalsIgnoreCase("LOC")) {
			mention.number = Numb.SINGULAR;
		}
		for (int i = mention.start; i <= mention.end; i++) {
			if (part.getWord(i).posTag.equalsIgnoreCase("CC")) {
				mention.number = Numb.PLURAL;
			}
		}
	}

	// calculate some attribute of one entity mention: is_nested_NP is_Subject
	// ner_type
	public void calAttribute(EntityMention em, CoNLLPart part) {
		calChAttribute(em, part);
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
		if (antecedent.isPronoun || this.getChDictionary().allPronouns.contains(antecedent.original.toLowerCase()))
			return false;
		if (antecedent.start < mention.start && mention.end == antecedent.end && mention.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean attributeAgree(EntityMention antecedent, EntityMention mention) {

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
			if (m.ner.equalsIgnoreCase("PERSON") && m.gender == Gender.UNKNOWN) {
				genders1.add(Gender.MALE);
				genders1.add(Gender.FEMALE);
			}
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
			if (m.ner.equalsIgnoreCase("PERSON") && m.gender == Gender.UNKNOWN) {
				genders2.add(Gender.MALE);
				genders2.add(Gender.FEMALE);
			}
		}
		for (Gender gender : genders2) {
			if (!genders1.contains(gender)) {
				hasExtraAnt = true;
			}
		}
		for (Gender gender : genders1) {
			if (!genders2.contains(gender)) {
				hasExtraThis = true;
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		hasExtraAnt = false;
		hasExtraThis = false;

		for (Numb numb : numbs2) {
			if (!numbs1.contains(numb)) {
				hasExtraAnt = true;
			}
		}
		for (Numb numb : numbs1) {
			if (!numbs2.contains(numb)) {
				hasExtraThis = true;
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		// Numb
		hasExtraAnt = false;
		hasExtraThis = false;

		for (Animacy animacy : animacys2) {
			if (!animacys1.contains(animacy)) {
				hasExtraAnt = true;
			}
		}
		for (Animacy animacy : animacys1) {
			if (!animacys2.contains(animacy)) {
				hasExtraThis = true;
			}
		}
		if (hasExtraAnt && hasExtraThis)
			return false;

		if (!NEs1.contains("OTHER")) {
			for (String str : NEs2) {
				if (!NEs1.contains(str) && !str.equalsIgnoreCase("OTHER")) {
					hasExtraAnt = true;
				}
			}
		}
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

	public static boolean attributeAgreeMention(EntityMention antecedent, EntityMention mention) {

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
		genders1.add(antecedent.gender);
		numbs1.add(antecedent.number);
		animacys1.add(antecedent.animacy);
		NEs1.add(antecedent.ner);
		if (antecedent.ner.equalsIgnoreCase("PERSON") && antecedent.gender == Gender.UNKNOWN) {
			genders1.add(Gender.MALE);
			genders1.add(Gender.FEMALE);
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

		genders2.add(mention.gender);
		numbs2.add(mention.number);
		animacys2.add(mention.animacy);
		NEs2.add(mention.ner);
		if (mention.ner.equalsIgnoreCase("PERSON") && mention.gender == Gender.UNKNOWN) {
			genders2.add(Gender.MALE);
			genders2.add(Gender.FEMALE);
		}
		for (Gender gender : genders2) {
			if (!genders1.contains(gender)) {
				hasExtraAnt = true;
			}
		}
		for (Gender gender : genders1) {
			if (!genders2.contains(gender)) {
				hasExtraThis = true;
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		hasExtraAnt = false;
		hasExtraThis = false;

		for (Numb numb : numbs2) {
			if (!numbs1.contains(numb)) {
				hasExtraAnt = true;
			}
		}
		for (Numb numb : numbs1) {
			if (!numbs2.contains(numb)) {
				hasExtraThis = true;
			}
		}

		if (hasExtraAnt && hasExtraThis)
			return false;

		// Numb
		hasExtraAnt = false;
		hasExtraThis = false;

		for (Animacy animacy : animacys2) {
			if (!animacys1.contains(animacy)) {
				hasExtraAnt = true;
			}
		}
		for (Animacy animacy : animacys1) {
			if (!animacys2.contains(animacy)) {
				hasExtraThis = true;
			}
		}
		if (hasExtraAnt && hasExtraThis)
			return false;

		if (!NEs1.contains("OTHER")) {
			for (String str : NEs2) {
				if (!NEs1.contains(str) && !str.equalsIgnoreCase("OTHER")) {
					hasExtraAnt = true;
				}
			}
		}
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

	public boolean attributeAgreeUNKNOWN(EntityMention antecedent, EntityMention mention) {
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
		if (antecedent.end + 1 == mention.start && antecedent.treeNode.parent == mention.treeNode.parent
				&& mention.ner.equals("PERSON") && !antecedent.isNT) {
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

		if (this.getChDictionary().statesAbbreviation.containsKey(em.original)
				&& this.getChDictionary().statesAbbreviation.get(em.original).equals(antecedent.original)
				|| this.getChDictionary().statesAbbreviation.containsKey(antecedent.original)
				&& this.getChDictionary().statesAbbreviation.get(antecedent.original).equals(em.original))
			return true;

		if (this.getChDictionary().demonyms.get(thisString) != null) {
			if (this.getChDictionary().demonyms.get(thisString).contains(antString))
				return true;
		} else if (this.getChDictionary().demonyms.get(antString) != null) {
			if (this.getChDictionary().demonyms.get(antString).contains(thisString))
				return true;
		}
		return false;
	}
}
