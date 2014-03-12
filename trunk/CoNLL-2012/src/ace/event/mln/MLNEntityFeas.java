package ace.event.mln;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import ace.ml.EntityFeatures;
import ace.rule.RuleCoref;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.EntityMention.MentionType;
import util.ChCommon;

public class MLNEntityFeas {

	public boolean nExactMatch_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.exactMatchRule(ant, em, part);
	}

	public boolean nStrictHeadRule1_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.strictHeadMatchRule1(ant, em, part);
	}

	public boolean nStrictHeadRule2_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.strictHeadMatchRule2(ant, em, part);
	}

	public boolean nStrictHeadRule3_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.strictHeadMatchRule3(ant, em, part);
	}

	public boolean nStrictHeadRule4_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.strictHeadMatchRule4(ant, em, part);
	}

	public boolean nRelaxRule_(EntityMention ant, EntityMention em, CoNLLPart part) {
		return EntityFeatures.relaxHeadMatchRule(ant, em, part);
	}

	// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
	// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
	// antecedent: 洛·迈利萨尼迪斯
	public boolean nNameAbv_(EntityMention ant, EntityMention em, CoNLLPart part) {
		boolean abv = false;
		if ((((em.ner.equalsIgnoreCase("PERSON") && ant.start == ant.end && ant.source.startsWith(em.head)) || (ant.ner
				.equalsIgnoreCase("PERSON")
				&& em.start == em.end && em.source.startsWith(ant.head))) || ((em.ner.equalsIgnoreCase("PERSON")
				&& ant.start == ant.end && ant.source.endsWith(em.head)) || (ant.ner.equalsIgnoreCase("PERSON")
				&& em.start == em.end && em.source.endsWith(ant.head))))) {
			abv = true;
		}
		if (ant.ner.equalsIgnoreCase("GPE") && em.ner.equalsIgnoreCase("GPE")) {
			if (ant.start == ant.end && em.start == em.end) {
				String bigger = ant.source.length() > em.source.length() ? ant.source : em.source;
				String small = ant.source.length() <= em.source.length() ? ant.source : em.source;
				if (small.length() == 2 && small.charAt(1) == '国' && small.charAt(0) == bigger.charAt(0)) {
					abv = true;
				}
			}
		}
		if ((((ant.start == ant.end && em.start + 1 == em.end && ant.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(em.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& ant.source.equalsIgnoreCase(part.getWord(em.start).word) && ChCommon.dict.titleWords.contains(part
				.getWord(em.end).word))) || (ant.start + 1 == ant.end && em.start == em.end
				&& em.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(ant.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& em.source.equalsIgnoreCase(part.getWord(ant.start).word) && ChCommon.dict.titleWords.contains(part
				.getWord(ant.end).word)))) {
			abv = true;
		}
		return abv;
	}

	public boolean nCorpular_(EntityMention ant, EntityMention em, CoNLLPart part) {
		// copular
		if (ant.sentenceID != em.sentenceID) {
			return false;
		}
		int sentenceIdx = ant.sentenceID;
		ArrayList<String> depends = part.getCoNLLSentences().get(sentenceIdx).depends;
		int position1[] = ChCommon.getPosition(ant, part.getCoNLLSentences());
		int position2[] = ChCommon.getPosition(em, part.getCoNLLSentences());
		int startWordIdx1 = position1[1];
		int startWordIdx2 = position2[1];
		int copularIdx = -1;
		for (String depend : depends) {
			String strs[] = depend.split(" ");
			String type = strs[0];
			int wordIdx1 = Integer.parseInt(strs[1]) - 1;
			int wordIdx2 = Integer.parseInt(strs[2]) - 1;
			if ((type.equals("attr")) && wordIdx2 == startWordIdx2) {
				// System.out.println(em.getContent());
				copularIdx = wordIdx1;
				break;
			}
		}
		if (copularIdx == -1) {
			return false;
		}
		for (String depend : depends) {
			String strs[] = depend.split(" ");
			int wordIdx1 = Integer.parseInt(strs[1]) - 1;
			int wordIdx2 = Integer.parseInt(strs[2]) - 1;
			if (wordIdx1 == copularIdx && wordIdx2 == startWordIdx1) {
				return true;
			}
		}
		return false;
	}

	public boolean nAppositive_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.headCharEnd + 1 == em.headCharStart && em.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean nFirstAttriComp_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (this.nAttriComp_(ant, em, part) && !ant.findComp) {
			ant.findComp = true;
			return true;
		} else {
			return false;
		}
	}

	public boolean nHeadMatch_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (em.head.equalsIgnoreCase(ant.head)) {
			return true;
			// link = true;
		} else {
			return false;
		}
	}

	public boolean nIsIWithI_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if ((ant.start <= em.start && ant.end > em.end)) {
			return true;
		}
		return false;
	}

	public String nSentenceDis_(EntityMention ant, EntityMention em, CoNLLPart part) {
		int dis = em.sentenceID - ant.sentenceID + 1;
		return Integer.toString((int) (Math.log(dis) / Math.log(2)));
	}

	public String nMentionDis_(EntityMention ant, EntityMention em, CoNLLPart part) {
		int dis = em.sequence - ant.sequence;
		if (dis < 0) {
			dis = 0;
		}
		return Integer.toString((int) (Math.log(dis) / Math.log(2)));
	}

	public boolean nAttriComp_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ChCommon.attributeAgreeMention(ant, em) && em.isPronoun) {
			return true;
		} else {
			return false;
		}
	}

	public boolean nConflictPerson_(EntityMention em, EntityMention ant, CoNLLPart part) {
//		if(em.number!=Numb.UNKNOWN && ant.number!=Numb.UNKNOWN&&em.number!=ant.number) {
//			return false;
//		}
//		
//		if(em.gender!=Gender.UNKNOWN && ant.gender!=Gender.UNKNOWN&&em.gender!=ant.gender) {
//			return false;
//		}
//		
//		if(em.person!=Person.UNKNOWN && ant.person!=Person.UNKNOWN&&em.person!=ant.person) {
//			return false;
//		}
		
		if(!(em.subType.equals(ant.subType))) {
			return true;
		}
		
		if(em.mentionType==MentionType.Proper && ant.mentionType==MentionType.Proper && !em.head.equals(ant.head)) {
			return true;
		}
		return false;
	}

	public boolean nRelaxHeadMatch_(EntityMention ant, EntityMention em, CoNLLPart part) {
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

	public boolean nWordInclusion_(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (!ant.head.equals(em.head)) {
			return false;
		}
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

	// public boolean haveIncompatibleModify(EntityMention ant, EntityMention
	// em, CoNLLPart part) {
	// if (ant.isPronoun || em.isPronoun) {
	// return false;
	// }
	// if ((ant.source.startsWith("那") && em.source.startsWith("这"))
	// || (ant.source.startsWith("这") && em.source.startsWith("那"))) {
	// return false;
	// }
	// if (!ant.head.equalsIgnoreCase(em.head)) {
	// return true;
	// }
	// boolean thisHasExtra = false;
	// Set<String> thisWordSet = new HashSet<String>();
	// Set<String> antWordSet = new HashSet<String>();
	// Set<String> locationModifier = new HashSet<String>(Arrays.asList("东",
	// "南", "西", "北", "中", "东面", "南面", "西面",
	// "北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧", "前"));
	// String mPRP = "";
	// String antPRP = "";
	// for (int i = em.start; i <= em.end; i++) {
	// String w1 = part.getWord(i).orig.toLowerCase();
	// String pos1 = part.getWord(i).posTag;
	// if ((pos1.startsWith("PU") || w1.equalsIgnoreCase(em.head))) {
	// continue;
	// }
	// thisWordSet.add(w1);
	// }
	// for (int j = ant.start; j <= ant.end; j++) {
	// String w2 = part.getWord(j).orig.toLowerCase();
	// String pos2 = part.getWord(j).posTag;
	// antWordSet.add(w2);
	// }
	// for (String w : thisWordSet) {
	// if (!antWordSet.contains(w)) {
	// thisHasExtra = true;
	// }
	// }
	// boolean hasLocationModifier = false;
	// for (String l : locationModifier) {
	// if (antWordSet.contains(l) && !thisWordSet.contains(l)) {
	// hasLocationModifier = true;
	// }
	// }
	// if (thisHasExtra || hasLocationModifier) {
	// return true;
	// } else {
	// return false;
	// }
	// }

}
