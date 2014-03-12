package ACL13.NBModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import ruleCoreference.chinese.ChDictionary;
import util.ChCommon;
import util.Common;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;

public abstract class IFeatures {

	ChCommon chCommon;

	public IFeatures() {
		chCommon = new ChCommon("chinese");
	}

	public boolean sameHeadFeatures(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (m1.headStart == m2.headStart && m1.headEnd == m2.headEnd) {
			return true;
		}
		return false;
	}

	public boolean I2IFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		String mString = m2.original.toLowerCase();
		String antString = m1.original.toLowerCase();
		ChDictionary dict = ChCommon.dict;
		String mSpeaker = part.getWord(m2.headStart).speaker;
		String antSpeaker = part.getWord(m1.headStart).speaker;

		if (dict.firstPersonPronouns.contains(mString) && m2.number == Numb.SINGULAR
				&& dict.firstPersonPronouns.contains(antString) && m1.number == Numb.SINGULAR
				&& mSpeaker.equals(antSpeaker) && !Common.isPronoun(antSpeaker)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean speaker2IFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		String mString = m2.original.toLowerCase();
		String antString = m1.original.toLowerCase();
		ChDictionary dict = ChCommon.dict;

		if (chCommon.isSpeaker(m1, m2, part)
				&& ((dict.firstPersonPronouns.contains(mString) && m2.number == Numb.SINGULAR) || (dict.firstPersonPronouns
						.contains(antString) && m1.number == Numb.SINGULAR))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean you2youFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		String mString = m2.original.toLowerCase();
		String antString = m1.original.toLowerCase();
		ChDictionary dict = ChCommon.dict;
		String mSpeaker = part.getWord(m2.headStart).speaker;
		String antSpeaker = part.getWord(m1.headStart).speaker;
		CoNLLWord antWord = part.getWord(m1.headStart);
		CoNLLWord mWord = part.getWord(m2.headStart);

		if (mSpeaker.equalsIgnoreCase(antSpeaker)
				&& (antWord.toSpeaker.containsAll(mWord.toSpeaker) || mWord.toSpeaker.containsAll(antWord.toSpeaker))
				&& !mSpeaker.equalsIgnoreCase("-") && dict.secondPersonPronouns.contains(mString)
				&& dict.secondPersonPronouns.contains(antString) && m1.number == Numb.SINGULAR
				&& m2.number == Numb.SINGULAR) {
			return true;
		}
		return false;
	}

	public boolean I2youFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		// previous I - you or previous you - I in two person conversation
		String mSpeaker = part.getWord(m2.headStart).speaker;
		String antSpeaker = part.getWord(m1.headStart).speaker;
		CoNLLWord antWord = part.getWord(m1.headStart);
		CoNLLWord mWord = part.getWord(m2.headStart);

		if ((m2.person == Person.I && m1.person == Person.YOU && antWord.toSpeaker.contains(mSpeaker))
				|| (m2.person == Person.YOU && m1.person == Person.I && mWord.toSpeaker.contains(antSpeaker))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean exactMatch(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (m1.source.equalsIgnoreCase(m2.source)) {
			boolean modiferCompatible = true;
			ArrayList<String> curModifiers = m2.modifyList;
			ArrayList<String> canModifiers = m1.modifyList;
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
			if (!modiferCompatible) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	public boolean roleFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (m1.roleSet.contains(m2) || m2.roleSet.contains(m1)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean aliasFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		// isAbb
		if (Common.isAbbreviation(m1.source, m2.source)) {
			return true;
		}

		// isSamePerson
		if ((m2.ner.equalsIgnoreCase("PERSON") && m1.start == m1.end && m1.source.startsWith(m2.head))
				|| (m1.ner.equalsIgnoreCase("PERSON") && m2.start == m2.end && m2.source.startsWith(m1.head))) {
			return true;
		}
		if ((m2.ner.equalsIgnoreCase("PERSON") && m1.start == m1.end && m1.source.endsWith(m2.head))
				|| (m1.ner.equalsIgnoreCase("PERSON") && m2.start == m2.end && m2.source.endsWith(m1.head))) {
			return true;
		}

		// isTitle
		if (m1.start == m1.end && m2.start + 1 == m2.end && m1.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(m2.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& m1.source.equalsIgnoreCase(part.getWord(m2.start).word)
				&& ChCommon.dict.titleWords.contains(part.getWord(m2.end).word)) {
			return true;
		}
		if (m1.start + 1 == m1.end && m2.start == m2.end && m2.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(m1.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& m2.source.equalsIgnoreCase(part.getWord(m1.start).word)
				&& ChCommon.dict.titleWords.contains(part.getWord(m1.end).word)) {
			return true;
		}

		// isSameComponents
		HashSet<String> antComp = getComponents(m1);
		HashSet<String> emComp = getComponents(m2);
		if (m1.source.length() == m2.source.length() && antComp != null && emComp != null) {
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
				return true;
			}
		}

		// isAbbNation
		if (m1.ner.equalsIgnoreCase("GPE") && m2.ner.equalsIgnoreCase("GPE")) {
			if (m1.start == m1.end && m2.start == m2.end) {
				String bigger = m1.source.length() > m2.source.length() ? m1.source : m2.source;
				String small = m1.source.length() <= m2.source.length() ? m1.source : m2.source;
				if (small.length() == 2 && small.charAt(1) == '国' && small.charAt(0) == bigger.charAt(0)) {
					return true;
				}
			}
		}

		// isAbbName
		if (m1.start + 1 == m1.end && m2.start + 1 == m2.end
				&& part.getWord(m2.end).word.equalsIgnoreCase(part.getWord(m1.end).word)) {
			if (part.getWord(m1.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(m2.start).word.equals(part.getWord(m1.start).word.substring(0, 1))) {
				return true;
			}
			if (part.getWord(m2.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(m1.start).word.equals(part.getWord(m2.start).word.substring(0, 1))) {
				return true;
			}
		}
		return false;
	}

	public boolean headMatch(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (m1.head.equalsIgnoreCase(m2.head)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean inWithIn(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if(m1.end<m2.start || m2.end<m1.start) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public String getCommonSemantic(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if(m1.head.equalsIgnoreCase(m2.head)) {
			return "";
		}
		String se1[] = Common.getSemantic(m1.head);
		if(se1==null) {
			se1 = Common.getSemantic(m1.head.substring(m1.head.length()-1));
		}
		String se2[] = Common.getSemantic(m2.head);
		if(se2==null) {
			se2 = Common.getSemantic(m2.head.substring(m2.head.length()-1));
		}
		if(se1==null || se2==null) {
			return "";
		}
		String longPrefix = "";
		for(String s1 : se1) {
			for(String s2: se2) {
				String prefix = this.getPrefix(s1, s2);
				if(longPrefix.length()<prefix.length()) {
					longPrefix = prefix;
				}
			}
		}
		return longPrefix;
	}
	
	private String getPrefix(String s1, String s2) {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<s1.length();i++) {
			if(s1.charAt(i)==s2.charAt(i)) {
				sb.append(s1.charAt(i));
			} else {
				break;
			}
		}
		return sb.toString();
	}

	public boolean conflictModifier(EntityMention m1, EntityMention m2, CoNLLPart part) {
		boolean thisHasExtra = false;
		Set<String> thisWordSet = new HashSet<String>();
		Set<String> antWordSet = new HashSet<String>();
		Set<String> locationModifier = new HashSet<String>(Arrays.asList("东", "南", "西", "北", "中", "东面", "南面", "西面",
				"北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧", "前"));
		for (int i = m2.start; i < m2.end; i++) {
			String w1 = part.getWord(i).orig.toLowerCase();
			String pos1 = part.getWord(i).posTag;
			if ((pos1.startsWith("PU") || w1.equalsIgnoreCase(m2.head))) {
				continue;
			}
			thisWordSet.add(w1);
		}
		for (int j = m1.start; j < m1.end; j++) {
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
		if (hasLocationModifier) {
			return true;
		}
		return false;
	}

	public boolean relaxHeadMatch(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (m2.head.charAt(0) == m1.head.charAt(m1.head.length() - 1) && m2.head.length() == 1) {
			// more constraint
			if (m2.sentenceID - m1.sentenceID <= 3
					&& !part.getWord(m1.headStart).rawNamedEntity.equalsIgnoreCase("PERSON") && !m2.head.equals("人")) {
				return true;
			}
		}
		// 八里乡 INANIMATE SINGULAR UNKNOWN GPE 52:1728,1728#八里 INANIMATE SINGULAR
		// UNKNOWN GPE
		if (m1.animacy == m2.animacy && m1.ner.equalsIgnoreCase(m2.ner) && !m2.ner.equalsIgnoreCase("OTHER")
				&& (m2.head.startsWith(m1.head) || m1.head.startsWith(m2.head))) {
			// Systm2.out.println(m1.extent + " # " + m2.extent + " relax");
			return true;
		}
		return false;
	}

	public boolean attributeAgree(Entity c1, Entity c2, CoNLLPart part) {
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
		for (EntityMention m : c1.mentions) {
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

		for (EntityMention m : c2.mentions) {
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

	public boolean personDisagree(Entity c1, Entity c2, CoNLLPart part) {
		for (EntityMention m1 : c1.mentions) {
			for (EntityMention m2 : c2.mentions) {
				if (mentionPersonDisagree(m1, m2, part)) {
					return true;
				}
			}
		}
		return false;
	}

	public String headPair(EntityMention m1, EntityMention m2, CoNLLPart part) {
		return Common.concat(m1.head, m2.head);
	}

	public String extentPair(EntityMention m1, EntityMention m2, CoNLLPart part) {
		return Common.concat(m1.extent, m2.extent);
	}

	public boolean sameModifier(EntityMention m1, EntityMention m2, CoNLLPart part) {
		StringBuilder sb1 = new StringBuilder();

		for (int i = m1.start; i < m1.end; i++) {
			CoNLLWord w = part.getWord(i);
			if (!w.posTag.equalsIgnoreCase("DEG")) {
				sb1.append(w.word);
			}
		}
		StringBuilder sb2 = new StringBuilder();
		for (int i = m2.start; i < m2.end; i++) {
			CoNLLWord w = part.getWord(i);
			if (!w.posTag.equalsIgnoreCase("DEG")) {
				sb2.append(w.word);
			}
		}

		if (!sb1.toString().trim().isEmpty() && sb1.toString().trim().equals(sb2.toString().trim())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean mentionPersonDisagree(EntityMention m1, EntityMention m2, CoNLLPart part) {
		String speak1 = part.getWord(m1.headStart).speaker;
		String speak2 = part.getWord(m2.headStart).speaker;
		boolean sameSpeaker = speak1.equals(speak2);

		// "我" X "他" and "你" X "他"
		if (m1.person == Person.HE || m1.person == Person.SHE) {
			if (m2.person == Person.I || m2.person == Person.YOU) {
				return true;
			}
		}
		if (m2.person == Person.HE || m2.person == Person.HE) {
			if (m1.person == Person.I || m1.person == Person.YOU) {
				return true;
			}
		}

		// 我们 X 他们
		if (m1.person == Person.THEY) {
			if (m2.person == Person.WE || m2.person == Person.YOUS) {
				return true;
			}
		}
		if (m2.person == Person.THEY) {
			if (m1.person == Person.WE || m1.person == Person.YOUS) {
				return true;
			}
		}

		// 这 && 那
		if ((m2.source.contains("这") && m1.source.contains("那"))
				|| (m2.source.contains("那") && m1.source.contains("这"))) {
			return true;
		}

		// 我的院子 and 我 can't coreference
		if (m1.end != m1.start && m2.isPronoun && part.getWord(m1.start).word.equals(m2.head)) {
			return true;
		}
		if (sameSpeaker && m2.person != m1.person) {
			if ((m2.person == Person.IT && m1.person == Person.THEY)
					|| (m2.person == Person.THEY && m1.person == Person.IT)
					|| (m2.person == Person.THEY && m1.person == Person.THEY))
				return false;
			else if (m2.person != Person.UNKNOWN && m1.person != Person.UNKNOWN)
				return true;
		}
		if (sameSpeaker) {
			if (!m1.isPronoun) {
				if (m2.person == Person.I || m2.person == Person.WE || m2.person == Person.YOU)
					return true;
			} else if (!m2.isPronoun) {
				if (m1.person == Person.I || m1.person == Person.WE || m1.person == Person.YOU)
					return true;
			}
		}
		if (m2.person == Person.YOU && m1.compareTo(m2) < 0) {
			if (!part.getWord(m2.headStart).speaker.equals("-")) {
				int currentUtterOrder = part.getWord(m2.headStart).utterOrder;
				int anteUtterOrder = part.getWord(m1.headStart).utterOrder;
				if (anteUtterOrder != -1 && anteUtterOrder == currentUtterOrder - 1 && m1.person == Person.I) {
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

	public boolean negativeRule(Entity c1, Entity c2, CoNLLPart part) {
		for (EntityMention m1 : c1.mentions) {
			for (EntityMention m2 : c2.mentions) {
				// in-with-in
				// number mismatch
				// 很多小朋友 ANIMATE PLURAL MALE OTHER 54:647,648#小朋友
				// 这 && 那
				// 这个 这些
				EntityMention longM = (m1.end - m1.start) > (m2.end - m2.start) ? m1 : m2;
				EntityMention shortM = (m1.end - m1.start) <= (m2.end - m2.start) ? m1 : m2;

				if (shortM.start == shortM.end
						&& longM.head.equalsIgnoreCase(shortM.head)
						&& (longM.start + 1 == longM.end || (longM.start + 2 == longM.end && part
								.getWord(longM.start + 1).word.equals("的")))) {
					if (chCommon.getChDictionary().parts.contains(part.getWord(longM.start).word)) {
						return false;
					}
				}
				// discourse constraint
				if (part.getDocument().ontoCommon.isSpeaker(m1, m2, part) && m1.person != Person.I
						&& m2.person != Person.I) {
					return false;
				}
				int dist = Math.abs(part.getWord(m1.headStart).utterOrder - part.getWord(m2.headStart).utterOrder);
				if (part.getDocument().getType() != DocType.Article && dist == 1
						&& !part.getDocument().ontoCommon.isSpeaker(m1, m2, part)) {
					if (m1.person == Person.I && m2.person == Person.I) {
						return false;
					}
					if (m1.person == Person.YOU && m2.person == Person.YOU) {
						return false;
					}
					if (m1.person == Person.YOUS && m2.person == Person.YOUS) {
						return false;
					}
					if (m1.person == Person.WE && m2.person == Person.WE) {
						return false;
					}
				}
				// CC construct
				MyTreeNode maxTreeNode = null;
				String shortEM = "";
				if (m1.source.length() > m2.source.length()) {
					maxTreeNode = m1.treeNode;
					shortEM = m2.source.replaceAll("\\s+", "");
				} else {
					maxTreeNode = m2.treeNode;
					shortEM = m1.source.replaceAll("\\s+", "");
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
					// cc
					if (longM.headStart == shortM.headStart && shortM.start > 0
							&& part.getWord(shortM.start - 1).posTag.equals("CC")) {
						return false;
					}
				}
				// Copular construct
				if (chCommon.isCopular2(m1, m2, part.getCoNLLSentences())) {
					return false;
				}
				if (m1.ner.equals(m2.ner)) {
					String head1 = m1.head;
					String head2 = m2.head;
					if ((m1.ner.equals("PERSON"))) {
						int similarity = 0;
						for (int i = 0; i < head1.length(); i++) {
							if (head2.indexOf(head1.charAt(i)) != -1) {
								similarity++;
							}
						}
						if (similarity == 0) {
							return false;
						}
					} else if (m1.ner.equals("LOC") || m1.ner.equals("GPE") || m1.ner.equals("ORG")) {
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
			}
		}
		return true;
	}

	public boolean wordInclude(Entity c1, Entity c2, CoNLLPart part) {
		List<String> removeW = Arrays.asList(new String[] { "这个", "这", "那个", "那", "自己", "的", "该", "公司", "这些", "那些",
				"'s" });
		ArrayList<String> removeWords = new ArrayList<String>();
		removeWords.addAll(removeW);
		HashSet<String> mentionClusterStrs = new HashSet<String>();
		for (EntityMention em : c2.mentions) {
			for (int i = em.start; i <= em.end; i++) {
				mentionClusterStrs.add(part.getWord(i).orig.toLowerCase());
				if (part.getWord(i).posTag.equalsIgnoreCase("DT") && i < em.end
						&& part.getWord(i + 1).posTag.equalsIgnoreCase("M")) {
					removeWords.add(part.getWord(i).word);
					removeWords.add(part.getWord(i + 1).word);
				}
			}
		}
		mentionClusterStrs.removeAll(removeWords);

		mentionClusterStrs.remove(c1);
		HashSet<String> candidateClusterStrs = new HashSet<String>();
		for (EntityMention e : c1.mentions) {
			for (int i = e.start; i <= e.end; i++) {
				candidateClusterStrs.add(part.getWord(i).orig.toLowerCase());
			}
			candidateClusterStrs.remove(e.head.toLowerCase());
		}
		if (candidateClusterStrs.containsAll(mentionClusterStrs))
			return true;
		else
			return false;
	}

	// public boolean extentContain(EntityMention m1, EntityMention m2,
	// CoNLLPart part) {
	//		
	//		
	// }

	public boolean headStart(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (!m1.head.equals(m2.head) && (m1.head.startsWith(m2.head) || m2.head.startsWith(m1.head))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean headEnd(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (!m1.head.equals(m2.head) && (m1.head.endsWith(m2.head) || m2.head.endsWith(m1.head))) {
			return true;
		} else {
			return false;
		}	
	}
	
	public boolean headContain(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (!m1.head.equals(m2.head) && (m1.head.contains(m2.head) || m2.head.contains(m1.head))) {
			return true;
		} else {
			return false;
		}	
	}
	
	public boolean headEqualFirstSentence(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if(m1.head.equalsIgnoreCase(m2.head) && (m1.sentenceID==1 || m1.sentenceID==0)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean headNoEqualExtentContain(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (!m1.head.equals(m2.head) && (m1.extent.contains(m2.extent) || m2.extent.contains(m1.extent))
				&& (!m1.ner.equalsIgnoreCase("other") || !m2.ner.equalsIgnoreCase("other"))) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean headNoEqualExtentStart(EntityMention m1, EntityMention m2, CoNLLPart part) {
		if (!m1.head.equals(m2.head) && (m1.extent.startsWith(m2.extent) || m2.extent.startsWith(m1.extent))
				&& (!m1.ner.equalsIgnoreCase("other") || !m2.ner.equalsIgnoreCase("other"))) {
			return true;
		} else {
			return false;
		}
	}
}
