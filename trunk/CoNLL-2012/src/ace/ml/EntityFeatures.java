package ace.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import ruleCoreference.chinese.ChDictionary;
import util.ChCommon;
import util.Common;
import util.Common.Numb;
import util.Common.Person;

public class EntityFeatures {
	static ChCommon chCommon = new ChCommon("chinese");
	public static boolean sameHead(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.headStart == em.headStart && ant.headEnd == em.headEnd) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean discourseRule1(EntityMention ant, EntityMention em, CoNLLPart part) {
		String mString = em.original.toLowerCase();
		String antString = ant.original.toLowerCase();
		ChDictionary dict = chCommon.getChDictionary();
		String mSpeaker = part.getWord(em.headStart).speaker;
		String antSpeaker = part.getWord(ant.headStart).speaker;
		if (dict.firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR
				&& chCommon.getChDictionary().firstPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& mSpeaker.equals(antSpeaker) && !Common.isPronoun(antSpeaker)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean discourseRule2(EntityMention ant, EntityMention em, CoNLLPart part) {
		String mString = em.original.toLowerCase();
		String antString = ant.original.toLowerCase();
		ChDictionary dict = chCommon.getChDictionary();
		if(chCommon.isSpeaker(ant, em, part)
				&& ((dict.firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR) || (dict.firstPersonPronouns
						.contains(antString) && ant.number == Numb.SINGULAR))) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean discourseRule3(EntityMention ant, EntityMention em, CoNLLPart part) {
		String mString = em.original.toLowerCase();
		String antString = ant.original.toLowerCase();
		ChDictionary dict = chCommon.getChDictionary();
		String mSpeaker = part.getWord(em.headStart).speaker;
		String antSpeaker = part.getWord(ant.headStart).speaker;
		CoNLLWord antWord = part.getWord(ant.headStart);
		CoNLLWord mWord = part.getWord(em.headStart);
		if(mSpeaker.equalsIgnoreCase(antSpeaker)
				&& (antWord.toSpeaker.containsAll(mWord.toSpeaker) || mWord.toSpeaker.containsAll(antWord.toSpeaker))
				&& !mSpeaker.equalsIgnoreCase("-") && dict.secondPersonPronouns.contains(mString)
				&& dict.secondPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& em.number == Numb.SINGULAR) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean discourseRule4(EntityMention ant, EntityMention em, CoNLLPart part) {
		String mSpeaker = part.getWord(em.headStart).speaker;
		String antSpeaker = part.getWord(ant.headStart).speaker;
		CoNLLWord antWord = part.getWord(ant.headStart);
		CoNLLWord mWord = part.getWord(em.headStart);
		if((em.person == Person.I && ant.person == Person.YOU && antWord.toSpeaker.contains(mSpeaker))
				|| (em.person == Person.YOU && ant.person == Person.I && mWord.toSpeaker.contains(antSpeaker))) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean exactMatchRule(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
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
		if(modiferCompatible && ant.head.equalsIgnoreCase(em.head)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean preciseRule1(EntityMention ant, EntityMention em, CoNLLPart part) {
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
	
	// role appositive
	public static boolean preciseRule2(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.headCharEnd + 1 == em.headCharStart && em.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}
	
	// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
	// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
	// antecedent: 洛·迈利萨尼迪斯
	public static boolean preciseRule3(EntityMention ant, EntityMention em, CoNLLPart part) {
		if ((((em.ner.equalsIgnoreCase("PERSON") && ant.start == ant.end && ant.source.startsWith(em.head)) || (ant.ner
						.equalsIgnoreCase("PERSON")
						&& em.start == em.end && em.source.startsWith(ant.head))) || ((em.ner
						.equalsIgnoreCase("PERSON")
						&& ant.start == ant.end && ant.source.endsWith(em.head)) || (ant.ner.equalsIgnoreCase("PERSON")
						&& em.start == em.end && em.source.endsWith(ant.head))))) {
			return true;
		} else {
			return false;
		}
	}
	
	// 李登辉总统 李登辉
	public static boolean preciseRule4(EntityMention ant, EntityMention em, CoNLLPart part) {
		if ((((ant.start == ant.end && em.start + 1 == em.end && ant.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(em.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& ant.source.equalsIgnoreCase(part.getWord(em.start).word) && chCommon.getChDictionary().titleWords
						.contains(part.getWord(em.end).word))) || (ant.start + 1 == ant.end && em.start == em.end
						&& em.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(ant.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& em.source.equalsIgnoreCase(part.getWord(ant.start).word) && chCommon.getChDictionary().titleWords
						.contains(part.getWord(ant.end).word)))) {
			return true;
		} else {
			return false;
		}
	}
	
	// 多国 INANIMATE PLURAL UNKNOWN NORP 13:635,635 #多明尼加
	public static boolean preciseRule5(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.ner.equalsIgnoreCase("GPE") && em.ner.equalsIgnoreCase("GPE")) {
			if (ant.start == ant.end && em.start == em.end) {
				String bigger = ant.source.length() > em.source.length() ? ant.source : em.source;
				String small = ant.source.length() <= em.source.length() ? ant.source : em.source;
				if (small.length() == 2 && small.charAt(1) == '国' && small.charAt(0) == bigger.charAt(0)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean preciseRule6(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.start + 1 == ant.end && em.start + 1 == em.end
				&& part.getWord(em.end).word.equalsIgnoreCase(part.getWord(ant.end).word)) {
			if (part.getWord(ant.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(em.start).word.equals(part.getWord(ant.start).word.substring(0, 1))) {
				return true;
			}
			if (part.getWord(em.start).rawNamedEntity.equals("PERSON")
					&& part.getWord(ant.start).word.equals(part.getWord(em.start).word.substring(0, 1))) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean strictHeadMatchRule1(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (em.head.equalsIgnoreCase(ant.head) && !haveIncompatibleModify(ant, em, part)
				&& wordInclusion(ant, em, part) && !isIWithI(ant, em, part)) {
			return true;
			//link = true;
		} else {
			return false;
		}
	}
	
	public static boolean strictHeadMatchRule2(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (em.head.equalsIgnoreCase(ant.head) && wordInclusion(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			return true;
			//link = true;
		} else {
			return false;
		}
	}
	
	public static boolean strictHeadMatchRule3(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (em.head.equalsIgnoreCase(ant.head) && !haveIncompatibleModify(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			return true;
			//link = true;
		} else {
			return false;
		}
	}
	
	public static boolean strictHeadMatchRule4(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (em.head.equalsIgnoreCase(ant.head) && sameProperHeadLastWord(ant, em, part)
				&& !chHaveDifferentLocation(ant, em, part) && !numberInLaterMention(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			return true;
			//link = true;
		} else {
			return false;
		}
	}
	
	public static boolean relaxHeadMatchRule(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.isPronoun || em.isPronoun) {
			return false;
		}
		if (relaxHeadMatch(ant, em, part) && !haveIncompatibleModify(ant, em, part)
				&& !isIWithI(ant, em, part)) {
			return true;
			//link = true;
		} else {
			return false;
		}
	}
	
	public static boolean pronounRule(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (!Common.isPronoun(em.head)) {
			return false;
		}

		if ((em.person == Person.YOU || em.person == Person.YOUS) && part.getDocument().getType() == DocType.Article
				&& (part.getWord(em.headStart).speaker.equals("-") || part.getWord(em.headStart).speaker.equals("*"))) {
			return false;
		}
		if (Math.abs(ant.sentenceID - em.sentenceID) > 3 && em.person != Person.I && em.person != Person.YOU) {
			return false;
		}
		if (!chCommon.attributeAgreeMention(ant, em)) {
			return false;
		}
		if (mentionPersonDisagree(ant, em, part)) {
			return false;
		}
		if (isIWithI(ant, em, part)) {
			return false;
		}
		return true;
	}
	
	private static boolean mentionPersonDisagree(EntityMention ant, EntityMention m, CoNLLPart part) {
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
	
	
	
	private static boolean relaxHeadMatch(EntityMention ant, EntityMention em, CoNLLPart part) {
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
	
	private static boolean numberInLaterMention(EntityMention ant, EntityMention em, CoNLLPart part) {
		Set<String> antecedentWords = new HashSet<String>();
		Set<String> numbers = new HashSet<String>();
		numbers.addAll(chCommon.getChDictionary().singleWords);
		numbers.addAll(chCommon.getChDictionary().pluralWords);
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
	private static boolean chHaveDifferentLocation(EntityMention ant, EntityMention em, CoNLLPart part) {
		// state and country cannot be coref
		if ((chCommon.getChDictionary().statesAbbreviation.containsKey(ant.original) || chCommon
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
				if (chCommon.getChDictionary().statesAbbreviation.containsKey(loc))
					loc = chCommon.getChDictionary().statesAbbreviation.get(loc);
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
				if (chCommon.getChDictionary().statesAbbreviation.containsKey(loc))
					loc = chCommon.getChDictionary().statesAbbreviation.get(loc);
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
	
	
	private static boolean sameProperHeadLastWord(EntityMention a, EntityMention m, CoNLLPart part) {
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
	
	
	private static boolean haveIncompatibleModify(EntityMention ant, EntityMention em, CoNLLPart part) {
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
	
	private static boolean wordInclusion(EntityMention ant, EntityMention em, CoNLLPart part) {
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
	
	

	private static boolean isIWithI(EntityMention ant, EntityMention em, CoNLLPart part) {
		if ((ant.start <= em.start && ant.end > em.end)) {
			return true;
		}
		return false;
	}

}
