package machineLearning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import ruleCoreference.chinese.ChDictionary;
import util.Common;
import util.Common.Numb;
import util.Common.Person;

public abstract class CorefFeature {
	public HashMap<String, Integer> stringFea1;
	public HashMap<String, Integer> stringFea2;
	public HashMap<String, Integer> stringFea3;
	
	public HashMap<EntityMention, EntityMention> appoPairs = new HashMap<EntityMention, EntityMention>();

	public void init(boolean train, String name) {
		if (train) {
			stringFea1 = new HashMap<String, Integer>();
			stringFea2 = new HashMap<String, Integer>();
			stringFea3 = new HashMap<String, Integer>();
		} else {
			stringFea1 = Common.readFile2Map(name + "_stringFea1");
			stringFea2 = Common.readFile2Map(name + "_stringFea2");
			stringFea3 = Common.readFile2Map(name + "_stringFea3");
		}
	}

	protected int getNERelationFeaIdx(String relation) {
		return stringFea1.get(relation.toLowerCase());
	}

	public ArrayList<CoNLLSentence> getSentences() {
		return sentences;
	}

	public void setSentences(ArrayList<CoNLLSentence> sentences) {
		this.sentences = sentences;
	}

	protected ArrayList<CoNLLSentence> sentences;

	protected ML ml;

	protected CoNLLPart part;

	public CorefFeature(ML ml) {
		this.ml = ml;
	}

	public void setPart(CoNLLPart part) {
		this.part = part;
	}

	public abstract List<Feature> getLoneFeature(boolean train, EntityMention mention);

	public abstract List<Feature> getBilateralFea(boolean train, EntityMention[] pair);

	boolean link = false;

	// public boolean violate(EntityMention pair[]) {
	// EntityMention em11 = pair[1];
	// EntityMention em22 = pair[0];
	// EntityMention longM = (em11.end - em11.start) > (em22.end - em22.start) ?
	// em11 : em22;
	// EntityMention shortM = (em11.end - em11.start) <= (em22.end - em22.start)
	// ? em11 : em22;
	//
	// if (shortM.start == shortM.end
	// && longM.head.equalsIgnoreCase(shortM.head)
	// && (longM.start + 1 == longM.end || (longM.start + 2 == longM.end && part
	// .getWord(longM.start + 1).word.equals("的")))) {
	// if
	// (ml.chCommon.getChDictionary().parts.contains(this.part.getWord(longM.start).word))
	// {
	// if (RuleCoref.bs.get(49))
	// return false;
	// }
	// }
	// // discourse constraint
	// if (!(this.currentSieve instanceof DiscourseProcessSieve)) {
	// if (part.getDocument().ontoCommon.isSpeaker(em11, em22, part) &&
	// em11.person != Person.I
	// && em22.person != Person.I) {
	// if (RuleCoref.bs.get(50))
	// return false;
	// }
	// String mSpeaker = part.getWord(em11.headStart).speaker;
	// String antSpeaker = part.getWord(em22.headStart).speaker;
	//
	// int dist = Math.abs(part.getWord(em11.headStart).utterOrder
	// - part.getWord(em22.headStart).utterOrder);
	// if (part.getDocument().getType() != DocType.Article && dist == 1
	// && !part.getDocument().ontoCommon.isSpeaker(em11, em22, part)) {
	// if (em11.person == Person.I && em22.person == Person.I) {
	// if (RuleCoref.bs.get(51))
	// return false;
	// }
	// if (em11.person == Person.YOU && em22.person == Person.YOU) {
	// if (RuleCoref.bs.get(52))
	// return false;
	// }
	// if (em11.person == Person.YOUS && em22.person == Person.YOUS) {
	// if (RuleCoref.bs.get(53))
	// return false;
	// }
	// if (em11.person == Person.WE && em22.person == Person.WE) {
	// if (RuleCoref.bs.get(54))
	// return false;
	// }
	// }
	// }
	//
	// boolean iWithi = ontoCommon.isIWithI(em11, em22, sentences);
	// if (iWithi && !ontoCommon.isCopular2(em11, em22, sentences)
	// && !(this.currentSieve instanceof SameHeadSieve)) {
	// if (RuleCoref.bs.get(55))
	// return false;
	// }
	// // CC construct
	// MyTreeNode maxTreeNode = null;
	// String shortEM = "";
	// if (antecedent.source.length() > em2.source.length()) {
	// maxTreeNode = antecedent.treeNode;
	// shortEM = em2.source.replaceAll("\\s+", "");
	// } else {
	// maxTreeNode = em2.treeNode;
	// shortEM = antecedent.source.replaceAll("\\s+", "");
	// }
	// ArrayList<MyTreeNode> offsprings = maxTreeNode.getBroadFirstOffsprings();
	// for (MyTreeNode node : offsprings) {
	// if (node.value.equalsIgnoreCase("cc") ||
	// node.value.equalsIgnoreCase("pu")) {
	// for (MyTreeNode child2 : node.parent.children) {
	// if (child2.toString().replaceAll("\\s+", "").endsWith(shortEM)) {
	// if (RuleCoref.bs.get(56)) {
	// return false;
	// }
	// }
	// }
	// }
	// }
	// // cc
	// if (longM.headStart == shortM.headStart && shortM.start > 0
	// && part.getWord(shortM.start - 1).posTag.equals("CC")) {
	// if (RuleCoref.bs.get(57)) {
	// return false;
	// }
	// }
	// // corpus construct
	// if (ml.chCommon.isCopular2(em11, em22, sentences)) {
	// if (RuleCoref.bs.get(58))
	// return false;
	// }
	// if (em11.ner.equals(em22.ner)) {
	// String head1 = em11.head;
	// String head2 = em22.head;
	// if ((em11.ner.equals("PERSON"))) {
	// int similarity = 0;
	// for (int i = 0; i < head1.length(); i++) {
	// if (head2.indexOf(head1.charAt(i)) != -1) {
	// similarity++;
	// }
	// }
	// if (similarity == 0) {
	// if (RuleCoref.bs.get(59))
	// return false;
	// }
	// } else if (em11.ner.equals("LOC") || em11.ner.equals("GPE") ||
	// em11.ner.equals("ORG")) {
	// if (!Common.isAbbreviation(head1, head2)) {
	// int similarity = 0;
	// for (int i = 0; i < head1.length(); i++) {
	// if (head2.indexOf(head1.charAt(i)) != -1) {
	// similarity++;
	// }
	// }
	// if (similarity == 0) {
	// if (RuleCoref.bs.get(60))
	// return false;
	// }
	// }
	// }
	// }
	// }

	public ArrayList<Feature> sievesFeature(boolean train, EntityMention[] pair) {
		link = false;
		ArrayList<Feature> features = new ArrayList<Feature>();
		// same head sieve
		this.addSameHeadSieve(features, pair[1], pair[0]);
		// Discourse sieve
		this.addDiscourseSieve(features, pair[1], pair[0]);
		// ExactMatchSieve
		this.addExactMatchSieve(features, pair[1], pair[0]);
		// PreciseConstructSieve
		this.addPreciseConstructSieve(features, pair[1], pair[0]);
		// StrictHeadMatchSieve1
		this.addStrictHeadMatchSieve1(features, pair[1], pair[0]);
		// StrictHeadMatchSieve2
		this.addStrictHeadMatchSieve2(features, pair[1], pair[0]);
		// StrictHeadMatchSieve3
		this.addStrictHeadMatchSieve3(features, pair[1], pair[0]);
		// StrictHeadMatchSieve4
		this.addStrictHeadMatchSieve4(features, pair[1], pair[0]);

		this.addRelaxHeadMatchSieve(features, pair[1], pair[0]);
		// pronoun sieve
		this.addPronounSieve(features, pair[1], pair[0]);

		int fea1 = -1;
		if (this.stringFea1.containsKey(pair[0].head)) {
			fea1 = this.stringFea1.get(pair[0].head);
		} else {
			if (train) {
				fea1 = this.stringFea1.size();
				this.stringFea1.put(pair[0].head, fea1);
			}
		}
		features.add(new Feature(fea1, -1));

		int fea2 = -1;
		if (this.stringFea2.containsKey(pair[0].head + "#" + pair[1].head)) {
			fea2 = this.stringFea2.get(pair[0].head + "#" + pair[1].head);
		} else {
			if (train) {
				fea2 = this.stringFea2.size();
				this.stringFea2.put(pair[0].head + "#" + pair[1].head, fea2);
			}
		}
		features.add(new Feature(fea1, -1));

		int fea3 = -1;
		if (this.stringFea3.containsKey(pair[0].extent + "#" + pair[1].extent)) {
			fea3 = this.stringFea3.get(pair[0].extent + "#" + pair[1].extent);
		} else {
			if (train) {
				fea3 = this.stringFea3.size();
				this.stringFea3.put(pair[0].extent + "#" + pair[1].extent, fea3);
			}
		}
		features.add(new Feature(fea1, -1));

		return features;
	}

	private void addRelaxHeadMatchSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && this.relaxHeadMatch(ant, em, part) && !this.haveIncompatibleModify(ant, em)
				&& !isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	public boolean relaxHeadMatch(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (em.head.charAt(0) == ant.head.charAt(ant.head.length() - 1) && em.head.length() == 1) {
			// more constraint
			if (em.sentenceID - ant.sentenceID <= 3
					&& !part.getWord(ant.headStart).rawNamedEntity.equalsIgnoreCase("PERSON") && !em.head.equals("人")) {
				return true;
			}
		}
		// 八里乡 INANIMATE SINGULAR UNKNOWN GPE 52:1728,1728#八里 INANIMATE SINGULAR
		// UNKNOWN GPE
		if (ant.animacy == em.animacy && ant.ner.equalsIgnoreCase(em.ner) && !em.ner.equalsIgnoreCase("OTHER")
				&& (em.head.startsWith(ant.head) || ant.head.startsWith(em.head))) {
			return true;
		}
		return false;
	}

	public boolean validate(EntityMention ant, EntityMention em) {
		EntityMention longM = (ant.end - ant.start) > (em.end - em.start) ? ant : em;
		EntityMention shortM = (ant.end - ant.start) <= (em.end - em.start) ? ant : em;

		if (shortM.start == shortM.end
				&& longM.head.equalsIgnoreCase(shortM.head)
				&& (longM.start + 1 == longM.end || (longM.start + 2 == longM.end && part.getWord(longM.start + 1).word
						.equals("的")))) {
			if (ml.chCommon.getChDictionary().parts.contains(this.part.getWord(longM.start).word)) {
				return false;
			}
		}
		// discourse constraint
		if (part.getDocument().ontoCommon.isSpeaker(ant, em, part) && ant.person != Person.I
				&& em.person != Person.I) {
			return false;
		}

		int dist = Math.abs(part.getWord(ant.headStart).utterOrder - part.getWord(em.headStart).utterOrder);
		if (part.getDocument().getType() != DocType.Article && dist == 1
				&& !part.getDocument().ontoCommon.isSpeaker(ant, em, part)) {
			if (ant.person == Person.I && em.person == Person.I) {
				return false;
			}
			if (ant.person == Person.YOU && em.person == Person.YOU) {
				return false;
			}
			if (ant.person == Person.YOUS && em.person == Person.YOUS) {
				return false;
			}
			if (ant.person == Person.WE && em.person == Person.WE) {
				return false;
			}
		}

		boolean iWithi = ml.chCommon.isIWithI(ant, em, sentences);
		if (iWithi && !ml.chCommon.isCopular2(ant, em, sentences)) {
			return false;
		}
		// CC construct
		MyTreeNode maxTreeNode = null;
		String shortEM = "";
		if (ant.source.length() > em.source.length()) {
			maxTreeNode = ant.treeNode;
			shortEM = em.source.replaceAll("\\s+", "");
		} else {
			maxTreeNode = em.treeNode;
			shortEM = ant.source.replaceAll("\\s+", "");
		}
		ArrayList<MyTreeNode> offsprings = maxTreeNode.getBroadFirstOffsprings();
		for (MyTreeNode node : offsprings) {
			if (node.value.equalsIgnoreCase("cc") || node.value.equalsIgnoreCase("pu")) {
				for (MyTreeNode child2 : node.parent.children) {
					if (child2.toString().replaceAll("\\s+", "").endsWith(shortEM)) {
						return false;
					}
				}
			}
		}
		// cc
		if (longM.headStart == shortM.headStart && shortM.start > 0
				&& part.getWord(shortM.start - 1).posTag.equals("CC")) {
			return false;
		}
		// corpus construct
		if (ml.chCommon.isCopular2(ant, em, sentences)) {
			return false;
		}
		if (ant.ner.equals(em.ner)) {
			String head1 = ant.head;
			String head2 = em.head;
			if ((ant.ner.equals("PERSON"))) {
				int similarity = 0;
				for (int i = 0; i < head1.length(); i++) {
					if (head2.indexOf(head1.charAt(i)) != -1) {
						similarity++;
					}
				}
				if (similarity == 0)
					return false;
			} else if (ant.ner.equals("LOC") || ant.ner.equals("GPE") || ant.ner.equals("ORG")) {
				if (!Common.isAbbreviation(head1, head2)) {
					int similarity = 0;
					for (int i = 0; i < head1.length(); i++) {
						if (head2.indexOf(head1.charAt(i)) != -1) {
							similarity++;
						}
					}
					if (similarity == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void addPronounSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!link && (ant.source.contains("双方") || ant.source.contains("两")) && em.head.equalsIgnoreCase("双方")) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		boolean a = false;
		if (em.head.equalsIgnoreCase("双方")) {
			if (ant.source.contains("双方") || ant.source.contains("两")) {
				a = true;
			}
			MyTreeNode node = ant.treeNode;
			int CC = 0;
			for (MyTreeNode child : node.children) {
				if (child.value.equalsIgnoreCase("CC")) {
					CC++;
				}
			}
			if (CC == 1) {
				a = true;
			}
		}
		if (!link && a) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		if (!Common.isPronoun(em.head)) {
			features.add(new Feature(1, 2));
			return;
		}

		boolean b = true;
		if ((em.person == Person.YOU || em.person == Person.YOUS) && part.getDocument().getType() == DocType.Article
				&& (part.getWord(em.headStart).speaker.equals("-") || part.getWord(em.headStart).speaker.equals("*"))) {
			b = false;
		}
		if (Math.abs(ant.sentenceID - em.sentenceID) > 3 && em.person != Person.I && em.person != Person.YOU) {
			b = false;
		}
		if (!this.ml.chCommon.attributeAgreeMention(ant, em)) {
			b = false;
		}
		if (this.mentionPersonDisagree(ant, em, part)) {
			b = false;
		}
		if (this.isIWithI(ant, em, part)) {
			b = false;
		}
		if (!link && b) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	public boolean mentionPersonDisagree(EntityMention ant, EntityMention m, CoNLLPart part) {
		String speak1 = part.getWord(ant.headStart).speaker;
		String speak2 = part.getWord(m.headStart).speaker;
		boolean sameSpeaker = speak1.equals(speak2);

		// "我" X "他" and "你" X "他"
		if (ant.person == Person.HE || ant.person == Person.SHE) {
			if (m.person == Person.I || m.person == Person.YOU) {
				return true;
			}
		}
		if (m.person == Person.HE || m.person == Person.HE) {
			if (ant.person == Person.I || ant.person == Person.YOU) {
				return true;
			}
		}

		// 我们 X 他们
		if (ant.person == Person.THEY) {
			if (m.person == Person.WE || m.person == Person.YOUS) {
				return true;
			}
		}
		if (m.person == Person.THEY) {
			if (ant.person == Person.WE || ant.person == Person.YOUS) {
				return true;
			}
		}

		// 这 && 那
		if ((m.source.contains("这") && ant.source.contains("那"))
				|| (m.source.contains("那") && ant.source.contains("这"))) {
			return true;
		}

		// 我的院子 and 我 can't coreference
		if (ant.end != ant.start && m.isPronoun && part.getWord(ant.start).word.equals(m.head)) {
			return true;
		}
		if (sameSpeaker && m.person != ant.person) {
			if ((m.person == Person.IT && ant.person == Person.THEY)
					|| (m.person == Person.THEY && ant.person == Person.IT)
					|| (m.person == Person.THEY && ant.person == Person.THEY))
				return false;
			else if (m.person != Person.UNKNOWN && ant.person != Person.UNKNOWN)
				return true;
		}
		if (sameSpeaker) {
			if (!ant.isPronoun) {
				if (m.person == Person.I || m.person == Person.WE || m.person == Person.YOU)
					return true;
			} else if (!m.isPronoun) {
				if (ant.person == Person.I || ant.person == Person.WE || ant.person == Person.YOU)
					return true;
			}
		}
		if (m.person == Person.YOU && ant.compareTo(m) < 0) {
			if (!part.getWord(m.headStart).speaker.equals("-")) {
				int currentUtterOrder = part.getWord(m.headStart).utterOrder;
				int anteUtterOrder = part.getWord(ant.headStart).utterOrder;
				if (anteUtterOrder != -1 && anteUtterOrder == currentUtterOrder - 1 && ant.person == Person.I) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	private void addStrictHeadMatchSieve4(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && em.head.equalsIgnoreCase(ant.head) && this.sameProperHeadLastWord(ant, em)
				&& !this.chHaveDifferentLocation(ant, em, part) && !this.numberInLaterMention(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve3(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && em.head.equalsIgnoreCase(ant.head) && !this.haveIncompatibleModify(ant, em)
				&& !isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve2(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && em.head.equalsIgnoreCase(ant.head) && this.wordInclusion(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve1(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && em.head.equalsIgnoreCase(ant.head) && !this.haveIncompatibleModify(ant, em)
				&& this.wordInclusion(ant, em, part) && !isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	public boolean isIWithI(EntityMention ant, EntityMention em, CoNLLPart part) {
		if ((ant.start <= em.start && ant.end > em.end)) {
			return true;
		}
		return false;
	}

	public boolean wordInclusion(EntityMention ant, EntityMention em, CoNLLPart part) {
		List<String> removeW = Arrays.asList(new String[] { "这个", "这", "那个", "那", "自己", "的", "该", "公司", "这些", "那些",
				"'s" });
		ArrayList<String> removeWords = new ArrayList<String>();
		removeWords.addAll(removeW);
		HashSet<String> mentionClusterStrs = new HashSet<String>();
		for (int i = em.start; i <= em.end; i++) {
			mentionClusterStrs.add(part.getWord(i).orig.toLowerCase());
			if (part.getWord(i).posTag.equalsIgnoreCase("DT") && i < em.end
					&& part.getWord(i + 1).posTag.equalsIgnoreCase("M")) {
				removeWords.add(part.getWord(i).word);
				removeWords.add(part.getWord(i + 1).word);
			}
		}
		mentionClusterStrs.removeAll(removeWords);

		mentionClusterStrs.remove(em.head.toLowerCase());
		HashSet<String> candidateClusterStrs = new HashSet<String>();
		for (int i = ant.start; i <= ant.end; i++) {
			candidateClusterStrs.add(part.getWord(i).orig.toLowerCase());
		}
		candidateClusterStrs.remove(ant.head.toLowerCase());
		if (candidateClusterStrs.containsAll(mentionClusterStrs))
			return true;
		else
			return false;
	}

	public boolean numberInLaterMention(EntityMention ant, EntityMention em, CoNLLPart part) {
		Set<String> antecedentWords = new HashSet<String>();
		Set<String> numbers = new HashSet<String>();
		numbers.addAll(ml.chCommon.getChDictionary().singleWords);
		numbers.addAll(ml.chCommon.getChDictionary().pluralWords);
		for (int i = ant.start; i <= ant.end; i++) {
			antecedentWords.add(part.getWord(i).orig.toLowerCase());
		}
		for (int i = em.start; i < em.end; i++) {
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
	public boolean chHaveDifferentLocation(EntityMention ant, EntityMention em, CoNLLPart part) {
		// state and country cannot be coref
		if ((ml.chCommon.getChDictionary().statesAbbreviation.containsKey(ant.original) || ml.chCommon
				.getChDictionary().statesAbbreviation.containsValue(em.original))
				&& (ant.head.equalsIgnoreCase("国")))
			return true;
		Set<String> locationM = new HashSet<String>();
		Set<String> locationA = new HashSet<String>();
		String mString = em.original.toLowerCase();
		String aString = ant.original.toLowerCase();
		Set<String> locationModifier = new HashSet<String>(Arrays.asList("东", "南", "西", "北", "中", "东面", "南面", "西面",
				"北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧"));

		for (int i = em.start; i <= em.end; i++) {
			String word = part.getWord(i).word;
			if (locationModifier.contains(word)) {
				return true;
			}
			if (part.getWord(i).rawNamedEntity.equals("LOC")) {
				String loc = part.getWord(i).word;
				if (ml.chCommon.getChDictionary().statesAbbreviation.containsKey(loc))
					loc = ml.chCommon.getChDictionary().statesAbbreviation.get(loc);
				locationM.add(loc);
			}
		}
		for (int i = ant.start; i <= ant.end; i++) {
			String word = part.getWord(i).word;
			if (locationModifier.contains(word)) {
				return true;
			}
			if (part.getWord(i).rawNamedEntity.equals("LOC")) {
				String loc = part.getWord(i).word;
				if (ml.chCommon.getChDictionary().statesAbbreviation.containsKey(loc))
					loc = ml.chCommon.getChDictionary().statesAbbreviation.get(loc);
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

	public boolean sameProperHeadLastWord(EntityMention a, EntityMention m) {
		String ner1 = part.getWord(a.headEnd).getRawNamedEntity();
		String ner2 = part.getWord(m.headEnd).getRawNamedEntity();
		if (a.head.equalsIgnoreCase(m.head) && part.getWord(a.headEnd).posTag.equals("NR")
				&& part.getWord(m.headEnd).posTag.equals("NR")) {
			return true;
		}
		if (a.head.equalsIgnoreCase(m.head) && ner1.equalsIgnoreCase(ner2)
				&& (ner1.equalsIgnoreCase("PERSON") || ner1.equalsIgnoreCase("GPE") || ner1.equalsIgnoreCase("LOC"))) {
			return true;
		}
		return false;
	}

	public boolean haveIncompatibleModify(EntityMention ant, EntityMention em) {
		if ((ant.source.startsWith("那") && em.source.startsWith("这"))
				|| (ant.source.startsWith("这") && em.source.startsWith("那"))) {
			return false;
		}
		if (!ant.head.equalsIgnoreCase(em.head)) {
			return true;
		}
		boolean thisHasExtra = false;
		Set<String> thisWordSet = new HashSet<String>();
		Set<String> antWordSet = new HashSet<String>();
		Set<String> locationModifier = new HashSet<String>(Arrays.asList("东", "南", "西", "北", "中", "东面", "南面", "西面",
				"北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧", "前"));
		String mPRP = "";
		String antPRP = "";
		for (int i = em.start; i <= em.end; i++) {
			String w1 = part.getWord(i).orig.toLowerCase();
			String pos1 = part.getWord(i).posTag;
			if ((pos1.startsWith("PU") || w1.equalsIgnoreCase(em.head))) {
				continue;
			}
			thisWordSet.add(w1);
		}
		for (int j = ant.start; j <= ant.end; j++) {
			String w2 = part.getWord(j).orig.toLowerCase();
			String pos2 = part.getWord(j).posTag;
			antWordSet.add(w2);
		}
		for (String w : thisWordSet) {
			if (!antWordSet.contains(w)) {
				thisHasExtra = true;
			}
		}
		boolean hasLocationModifier = false;
		for (String l : locationModifier) {
			if (antWordSet.contains(l) && !thisWordSet.contains(l)) {
				hasLocationModifier = true;
			}
		}
		if (thisHasExtra || hasLocationModifier) {
			return true;
		} else {
			return false;
		}
	}

	private void addPreciseConstructSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!link && em.roleSet.contains(ant)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		if (!link && Common.isAbbreviation(ant.source, em.source)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
		// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
		// antecedent: 洛·迈利萨尼迪斯
		if (!link
				&& (((em.ner.equalsIgnoreCase("PERSON") && ant.start == ant.end && ant.source.startsWith(em.head)) || (ant.ner
						.equalsIgnoreCase("PERSON")
						&& em.start == em.end && em.source.startsWith(ant.head))) || ((em.ner
						.equalsIgnoreCase("PERSON")
						&& ant.start == ant.end && ant.source.endsWith(em.head)) || (ant.ner.equalsIgnoreCase("PERSON")
						&& em.start == em.end && em.source.endsWith(ant.head))))) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// 李登辉总统 李登辉
		if (!link
				&& (((ant.start == ant.end && em.start + 1 == em.end && ant.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(em.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& ant.source.equalsIgnoreCase(part.getWord(em.start).word) && ml.chCommon.getChDictionary().titleWords
						.contains(part.getWord(em.end).word))) || (ant.start + 1 == ant.end && em.start == em.end
						&& em.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(ant.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& em.source.equalsIgnoreCase(part.getWord(ant.start).word) && ml.chCommon.getChDictionary().titleWords
						.contains(part.getWord(ant.end).word)))) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// 海军陆战队和陆军 UNKNOWN PLURAL UNKNOWN OTHER 10:212,215 #陆军和海军陆战队
		boolean sat = false;
		HashSet<String> antComp = getComponents(ant);
		HashSet<String> emComp = getComponents(em);
		if (antComp != null && emComp != null && em.source.length() == ant.source.length()) {
			boolean extra1 = false;
			boolean extra2 = false;
			for (String str : antComp) {
				if (!emComp.contains(str)) {
					extra1 = true;
					break;
				}
			}
			for (String str : emComp) {
				if (!antComp.contains(str)) {
					extra2 = true;
					break;
				}
			}
			if (!extra1 && !extra2) {
				sat = true;
			}
		}
		if (!link && sat) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// 多国 INANIMATE PLURAL UNKNOWN NORP 13:635,635 #多明尼加
		boolean c = false;
		if (ant.ner.equalsIgnoreCase("GPE") && em.ner.equalsIgnoreCase("GPE")) {
			if (ant.start == ant.end && em.start == em.end) {
				String bigger = ant.source.length() > em.source.length() ? ant.source : em.source;
				String small = ant.source.length() <= em.source.length() ? ant.source : em.source;
				if (small.length() == 2 && small.charAt(1) == '国' && small.charAt(0) == bigger.charAt(0)) {
					c = true;
				}
			}
		}
		if (!link && c) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		boolean b = false;
		if (ant.start + 1 == ant.end && em.start + 1 == em.end
				&& part.getWord(em.end).word.equalsIgnoreCase(part.getWord(ant.end).word)) {
			if (part.getWord(ant.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(em.start).word.equals(part.getWord(ant.start).word.substring(0, 1))) {
				b = true;
			}
			if (part.getWord(em.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(ant.start).word.equals(part.getWord(em.start).word.substring(0, 1))) {
				b = true;
			}
		}
		if (!link && b) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private HashSet<String> getComponents(EntityMention mention) {
		HashSet<String> components = new HashSet<String>();
		MyTreeNode node = mention.treeNode;
		boolean connect = false;
		for (MyTreeNode child : node.children) {
			if (child.value.equalsIgnoreCase("CC") || child.value.equalsIgnoreCase("PU")) {
				connect = true;
				continue;
			}
			components.add(child.toString().replace(" ", ""));
		}
		if (connect) {
			return components;
		} else {
			return null;
		}
	}

	private void addExactMatchSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		boolean modiferCompatible = true;
		ArrayList<String> curModifiers = em.modifyList;
		ArrayList<String> canModifiers = ant.modifyList;
		HashSet<String> curModifiersHash = new HashSet<String>();
		curModifiersHash.addAll(curModifiers);
		HashSet<String> canModifiersHash = new HashSet<String>();
		canModifiersHash.addAll(canModifiers);
		for (String canModifier : canModifiers) {
			if (!curModifiersHash.contains(canModifier)) {
				modiferCompatible = false;
				break;
			}
		}
		for (String curModifier : curModifiers) {
			if (!canModifiersHash.contains(curModifier)) {
				modiferCompatible = false;
				break;
			}
		}
		if (!link && modiferCompatible && ant.source.equalsIgnoreCase(em.source)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addSameHeadSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!link && ant.headStart == em.headStart && ant.headEnd == em.headEnd) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addDiscourseSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		String mString = em.original.toLowerCase();
		String antString = ant.original.toLowerCase();
		ChDictionary dict = ml.chCommon.getChDictionary();
		String mSpeaker = part.getWord(em.headStart).speaker;
		String antSpeaker = part.getWord(ant.headStart).speaker;
		int mUtterOrder = part.getWord(em.headStart).utterOrder;
		int antUtterOrder = part.getWord(ant.headStart).utterOrder;
		CoNLLWord antWord = part.getWord(ant.headStart);
		CoNLLWord mWord = part.getWord(em.headStart);
		// (I - I) in the same speaker's quotation.
		if (!link && ml.chCommon.getChDictionary().firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR
				&& ml.chCommon.getChDictionary().firstPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& mSpeaker.equals(antSpeaker) && !Common.isPronoun(antSpeaker)) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// (speaker - I)
		if (!link
				&& ml.chCommon.isSpeaker(ant, em, part)
				&& ((dict.firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR) || (dict.firstPersonPronouns
						.contains(antString) && ant.number == Numb.SINGULAR))) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// You - You
		if (!link && mSpeaker.equalsIgnoreCase(antSpeaker)
				&& (antWord.toSpeaker.containsAll(mWord.toSpeaker) || mWord.toSpeaker.containsAll(antWord.toSpeaker))
				&& !mSpeaker.equalsIgnoreCase("-") && dict.secondPersonPronouns.contains(mString)
				&& dict.secondPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& em.number == Numb.SINGULAR) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// previous I - you or previous you - I in two person conversation
		if (!link && (em.person == Person.I && ant.person == Person.YOU && antWord.toSpeaker.contains(mSpeaker))
				|| (em.person == Person.YOU && ant.person == Person.I && mWord.toSpeaker.contains(antSpeaker))) {
			features.add(new Feature(0, 2));
			link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}
}
