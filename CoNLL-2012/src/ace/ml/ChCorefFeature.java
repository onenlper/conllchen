package ace.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;

public class ChCorefFeature extends CorefFeature {

	public ChCorefFeature(ACEML ml) {
		super(ml);
	}

	public List<Feature> getLoneFeature(boolean train, EntityMention mention) {
		List<Feature> features = new ArrayList<Feature>();
		// 1 PRONOUN_1
		if (mention.isPronoun) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 2 subject_1
		if (mention.isSub) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 3 nested_1
		if (mention.isNNP) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 4 number_2
		features.add(new Feature(mention.number.ordinal(), 3));
		// System.out.println(mention.gender+"#"+mention.number+"#"+mention.animacy);
		// 5 gender_2
		features.add(new Feature(mention.gender.ordinal(), 4));
		// 9 animacy_2
		features.add(new Feature(mention.animacy.ordinal(), 3));

		// NE feature
		// System.out.println(mention.ner);
		if (mention.ner.equals("CARDINAL")) {
			features.add(new Feature(0, 19));
		} else if (mention.ner.equals("DATE")) {
			features.add(new Feature(1, 19));
		} else if (mention.ner.equals("EVENT")) {
			features.add(new Feature(2, 19));
		} else if (mention.ner.equals("FAC")) {
			features.add(new Feature(3, 19));
		} else if (mention.ner.equals("GPE")) {
			features.add(new Feature(4, 19));
		} else if (mention.ner.equals("LAW")) {
			features.add(new Feature(5, 19));
		} else if (mention.ner.equals("LOC")) {
			features.add(new Feature(6, 19));
		} else if (mention.ner.equals("MONEY")) {
			features.add(new Feature(7, 19));
		} else if (mention.ner.equals("NORP")) {
			features.add(new Feature(8, 19));
		} else if (mention.ner.equals("ORDINAL")) {
			features.add(new Feature(9, 19));
		} else if (mention.ner.equals("ORG")) {
			features.add(new Feature(10, 19));
		} else if (mention.ner.equals("PERCENT")) {
			features.add(new Feature(11, 19));
		} else if (mention.ner.equals("PERSON")) {
			features.add(new Feature(12, 19));
		} else if (mention.ner.equals("PRODUCT")) {
			features.add(new Feature(13, 19));
		} else if (mention.ner.equals("QUANTITY")) {
			features.add(new Feature(14, 19));
		} else if (mention.ner.equals("TIME")) {
			features.add(new Feature(15, 19));
		} else if (mention.ner.equals("WORK_OF_ART")) {
			features.add(new Feature(16, 19));
		} else if (mention.ner.equals("LANGUAGE")) {
			features.add(new Feature(17, 19));
		} else {
			features.add(new Feature(18, 19));
		}
		return features;
	}

	public List<Feature> getBilateralFea(boolean train, EntityMention[] pair) {
		ArrayList<Feature> features = new ArrayList<Feature>();
		EntityMention current = pair[0];
		EntityMention candidate = pair[1];

		String canStr = candidate.source;
		String curStr = current.source;

		String canHead = candidate.head;
		String curHead = current.head;

		// string_match
		if (curStr.equals(canStr)) {
			features.add(new Feature(0, 7));
		} // left
		else if (canStr.startsWith(curStr) || curStr.startsWith(canStr)) {
			features.add(new Feature(1, 7));
		} // right
		else if (canStr.endsWith(curStr) || curStr.endsWith(canStr)) {
			features.add(new Feature(2, 7));
		} // contain
		else if (canStr.contains(curStr) || curStr.contains(canStr)) {
			features.add(new Feature(3, 7));
		} // part contain
		else {
			int length1 = canStr.length();
			int length2 = curStr.length();
			boolean alias = true;
			boolean part = false;
			if (length1 <= length2) {
				for (int i = 0; i < length1; i++) {
					int pos = curStr.indexOf(canStr.charAt(i));
					if (pos == -1) {
						alias = false;
						break;
					}
					part = true;
				}
			} else {
				for (int i = 0; i < length2; i++) {
					int pos = canStr.indexOf(curStr.charAt(i));
					if (pos == -1) {
						alias = false;
						break;
					}
					part = true;
				}
			}
			if (alias) {
				features.add(new Feature(4, 7));
			} else if (part) {
				features.add(new Feature(5, 7));
			} else {
				features.add(new Feature(6, 7));
			}
		}

		// 14 edit distance
		int dis = Common.getEditDistance(canStr, curStr);
		if (dis > 7) {
			dis = 7;
		}
		features.add(new Feature(dis, 8));

		// 15 PN_STR_MATCH
		if (curStr.equals(canStr) && current.isProperNoun && candidate.isProperNoun) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 16 NONPRO_STR_MATCH
		if (!current.isProperNoun && !candidate.isProperNoun && curStr.equals(canStr)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 17 MODIFIER_MATCH
		if (candidate.head.equals(current.head)) {
			if (candidate.modifyList.size() != 0 && current.modifyList.size() != 0) {
				boolean modiferCompatible = true;
				ArrayList<String> curModifiers = current.modifyList;
				ArrayList<String> canModifiers = candidate.modifyList;
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
				if (modiferCompatible) {
					features.add(new Feature(0, 4));
				} else {
					features.add(new Feature(1, 4));
				}
			} else {
				features.add(new Feature(2, 4));
			}
		} else {
			features.add(new Feature(3, 4));
		}

		// 18 PRO_TYPE_MATCH
		if (candidate.isPronoun && current.isPronoun) {
			if (candidate.PRONOUN_TYPE == current.PRONOUN_TYPE) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}

		// 19 NUMBER AGREEMENT
		if ((candidate.number == Numb.SINGULAR && current.number == Numb.SINGULAR)
				|| (candidate.number == Numb.PLURAL && current.number == Numb.PLURAL)) {
			features.add(new Feature(0, 3));
		} else if (candidate.number == Numb.UNKNOWN || current.number == Numb.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 20 GENDER AGREEMENT
		if ((candidate.gender == Gender.FEMALE && current.gender == Gender.FEMALE)
				|| (candidate.gender == Gender.MALE && current.gender == Gender.MALE)) {
			features.add(new Feature(0, 3));
		} else if (candidate.gender == Gender.UNKNOWN || current.gender == Gender.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 21 AGREEMENT
		if (candidate.number != Numb.UNKNOWN && current.number != Numb.UNKNOWN && candidate.gender != Gender.UNKNOWN
				&& current.gender != Gender.UNKNOWN) {
			if (candidate.number == current.number && candidate.gender == current.gender) {
				features.add(new Feature(0, 3));
			} else if (candidate.number != current.number && candidate.gender != current.gender) {
				features.add(new Feature(1, 3));
			} else {
				features.add(new Feature(2, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}

		// 22 ANIMACY
		if ((candidate.animacy == Animacy.ANIMATE && current.animacy == Animacy.ANIMATE)
				|| (candidate.animacy == Animacy.INANIMATE && current.animacy == Animacy.INANIMATE)) {
			features.add(new Feature(0, 3));
		} else if (current.animacy == Animacy.UNKNOWN || candidate.animacy == Animacy.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 23 BOTH_PRONOUNS
		if (Common.isPronoun(curStr) && Common.isPronoun(canStr)) {
			features.add(new Feature(0, 3));
		} else if (!Common.isPronoun(curStr) && !Common.isPronoun(canStr)) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		// 24 BOTH_PROPER_NOUNS
		if (candidate.isProperNoun && current.isProperNoun) {
			features.add(new Feature(0, 3));
		} else if (!candidate.isProperNoun && !current.isProperNoun) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 25 MAXIMALNP
		if (!ml.chCommon.isMaximalNP(candidate, current, sentences)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 26 SPAN
		if (current.getS() > candidate.getS() && current.getE() < candidate.getE()) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// // 27 Character distance
		int[] position1 = ml.chCommon.getPosition(candidate, sentences);
		int[] position2 = ml.chCommon.getPosition(current, sentences);
		// double charDiff = Math.abs(current.getS() - candidate.getS());
		// int charDis = (int) Math.ceil(Math.log(charDiff + 1) / Math.log(2));
		// if (charDis > 10) {
		// charDis = 10;
		// }
		// features.add(new Feature(charDis, 11));

		// 28 length ratio
		double ratio = 0;
		if (canStr.length() >= curStr.length()) {
			ratio = (double) curStr.length() / (double) canStr.length();
		} else {
			ratio = (double) canStr.length() / (double) curStr.length();
		}
		if (ratio >= 0.8 && ratio <= 1) {
			features.add(new Feature(0, 5));
		} else if (ratio >= 0.6 && ratio < 0.8) {
			features.add(new Feature(1, 5));
		} else if (ratio >= 0.4 && ratio < 0.6) {
			features.add(new Feature(2, 5));
		} else if (ratio >= 0.2 && ratio < 0.4) {
			features.add(new Feature(3, 5));
		} else if (ratio >= 0 && ratio < 0.2) {
			features.add(new Feature(4, 5));
		}

		// 29 COPULAR
		if (isCopular(candidate, current, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 31 Semantic distance
		int maxSame = 0;
		String canLastWord = sentences.get(position1[0]).getWord(position1[2]).word;
		String curLastWord = sentences.get(position2[0]).getWord(position2[2]).word;
		String sem1 = ml.chCommon.getSemanticSymbol(current, curLastWord);
		// System.out.println(current.head + " " + curLastWord + " " +
		// sem1);
		String sem2 = ml.chCommon.getSemanticSymbol(candidate, canLastWord);
		if (sem1.isEmpty() || sem2.isEmpty()) {
			features.add(new Feature(9, 10));
		} else {
			int same = 0;
			for (; same < sem1.length(); same++) {
				if (sem1.charAt(same) != sem2.charAt(same)) {
					break;
				}
			}
			maxSame = same;
			features.add(new Feature(maxSame, 10));
		}

		// 32 sentence DISTANCE
		int sentence1 = position1[0];
		int sentence2 = position2[0];
		double sentenceDiff = Math.abs(sentence1 - sentence2);
		int senDis = (int) Math.ceil(Math.log(sentenceDiff + 1) / Math.log(2));
		if (senDis > 5) {
			senDis = 5;
		}
		features.add(new Feature(senDis, 6));

		// 33 NE equal
		if (current.ner.equalsIgnoreCase("OTHER") || candidate.ner.equalsIgnoreCase("OTHER")) {
			features.add(new Feature(2, 3));
		} else if (current.ner.equalsIgnoreCase(candidate.ner)) {
			features.add(new Feature(0, 3));
		} else {
			features.add(new Feature(1, 3));
		}

		// 34 other features
		String whole = curStr + canStr;
		boolean upcase = true;
		boolean lowcase = true;
		boolean digit = true;
		for (int i = 0; i < whole.length(); i++) {
			char c = whole.charAt(i);
			if (!(c >= '0' && c <= '9')) {
				digit = false;
			}
			if (!(c >= 'a' && c <= 'z')) {
				lowcase = false;
			}
			if (!(c >= 'A' && c <= 'Z')) {
				upcase = false;
			}
		}
		if (upcase) {
			features.add(new Feature(0, 4));
		} else if (lowcase) {
			features.add(new Feature(1, 4));
		} else if (digit) {
			features.add(new Feature(2, 4));
		} else {
			features.add(new Feature(3, 4));
		}

		// 35 word similarity
		// int sameChar = 0;
		// for (int i = 0; i < canStr.length(); i++) {
		// char c = canStr.charAt(i);
		// if (curStr.indexOf(c) >= 0) {
		// sameChar++;
		// }
		// }
		// double simChar = ((double) 2 * sameChar) / ((double) canStr.length()
		// + curStr.length());
		// if (simChar >= 0.8 && simChar <= 1) {
		// features.add(new Feature(0, 5));
		// } else if (simChar >= 0.6 && simChar < 0.8) {
		// features.add(new Feature(1, 5));
		// } else if (simChar >= 0.4 && simChar < 0.6) {
		// features.add(new Feature(2, 5));
		// } else if (simChar >= 0.2 && simChar < 0.4) {
		// features.add(new Feature(3, 5));
		// } else if (simChar >= 0 && simChar < 0.2) {
		// features.add(new Feature(4, 5));
		// }

		// 36 head_match
		if (canHead.equalsIgnoreCase(curHead)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// modifier
		if (candidate.headStart == current.headStart || candidate.headEnd > current.headEnd) {
			features.add(new Feature(0, 2));
			// System.out.println("modifier: " + candidate + " " +
			// current);
		} else {
			features.add(new Feature(1, 2));
		}
		// head
		if (candidate.headEnd == current.headEnd) {
			features.add(new Feature(0, 2));
			// System.out.println("head: " + candidate + " " +
			// current);
		} else {
			features.add(new Feature(1, 2));
		}

		// RoleAppositive
		if (isRoleAppositive(candidate, current, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		if (Common.isAbbreviation(canHead, curHead)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// same speaker
		String mSpeaker = part.getWord(pair[0].headStart).speaker;
		String antSpeaker = part.getWord(pair[1].headStart).speaker;
		if (mSpeaker.equals(antSpeaker)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		features.addAll(this.sievesFeature(train, pair));

		// NE relation feature
		String ner1 = current.ner.toLowerCase();
		String ner2 = candidate.ner.toLowerCase();
		String relation = "";
		if (ner1.compareTo(ner2) < 0) {
			relation = ner1.toLowerCase() + "#" + ner2.toLowerCase();
		} else {
			relation = ner2.toLowerCase() + "#" + ner1.toLowerCase();
		}
		features.add(new Feature(this.getNERelationFeaIdx(relation), -1));

		// 37 between words
		int betweenIdx = 0;
		if (position1[0] == position2[0]) {
			int start = position1[2] + 1;
			int end = position2[1] - 1;
			StringBuilder sb = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(sentences.get(position1[0]).getWord(i).word);
			}
			String between = sb.toString();
			if (between.length() < 3) {
				if (train) {
//					if (stringFea2.containsKey(between)) {
//						betweenIdx = stringFea2.get(between);
//					} else {
//						betweenIdx = stringFea2.size();
//						stringFea2.put(between, betweenIdx);
//					}
//				} else {
//					if (stringFea2.containsKey(between)) {
//						betweenIdx = stringFea2.get(between);
//					} else {
//						betweenIdx = -1;
//					}
				}
			} else {
				betweenIdx = -1;
			}
		} else {
			betweenIdx = -1;
		}
		features.add(new Feature(betweenIdx, -1));

		// 39 ngram_concate
		int concatIdx = 0;
		canStr = canHead;
		curStr = curHead;
		String concat = Common.concat(canStr, curStr);
		concat = concat.replace("\n", "").replace("\r", "");
		String concat2 = Common.concat(candidate.source, current.source);
		if (train) {
//			if (stringFea3.containsKey(concat)) {
//				concatIdx = stringFea3.get(concat);
//			} else {
//				concatIdx = stringFea3.size();
//				stringFea3.put(concat, concatIdx);
//			}
			if (ml.goldMaps.get(current) != null && ml.goldMaps.get(current).contains(candidate)) {
				if (positiveHeadPair.containsKey(concat)) {
					Atom a = positiveHeadPair.get(concat);
					a.increase();
				} else {
					positiveHeadPair.put(concat, new Atom(concat, 1));
				}

				if (positiveSourcePair.containsKey(concat2)) {
					Atom a = positiveSourcePair.get(concat2);
					a.increase();
				} else {
					positiveSourcePair.put(concat2, new Atom(concat2, 1));
				}
			} else {
				if (negativeHeadPair.containsKey(concat)) {
					Atom a = negativeHeadPair.get(concat);
					a.increase();
				} else {
					negativeHeadPair.put(concat, new Atom(concat, 1));
				}
				if (negativeSourcePair.containsKey(concat2)) {
					Atom a = negativeSourcePair.get(concat2);
					a.increase();
				} else {
					negativeSourcePair.put(concat2, new Atom(concat2, 1));
				}
			}
			allHeadPairs.add(concat);
			allSourcePairs.add(concat2);
		} else {
//			if (stringFea3.containsKey(concat)) {
//				concatIdx = stringFea3.get(concat);
//			} else {
//				concatIdx = -1;
//			}
		}
		features.add(new Feature(concatIdx, -1));
		return features;
	}

	// role appositive
	public static boolean isRoleAppositive(EntityMention ant, EntityMention em, CoNLLPart part) {
		if (ant.headCharEnd + 1 == em.headCharStart && em.ner.equalsIgnoreCase("PERSON")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static class Atom implements Comparable {
		String key;
		double count;

		public Atom(String key, double count) {
			this.key = key;
			this.count = count;
		}

		public void increase() {
			this.count += 1;
		}

		@Override
		public int compareTo(Object arg0) {
			if (this.count > ((Atom) arg0).count) {
				return 1;
			} else if (this.count == ((Atom) arg0).count) {
				return 0;
			} else {
				return -1;
			}
		}
	}
	
	public static boolean isCopular(EntityMention ant, EntityMention em, CoNLLPart part) {
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

	public static HashSet<String> allHeadPairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveHeadPair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeHeadPair = new HashMap<String, Atom>();

	public static HashSet<String> allSourcePairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveSourcePair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeSourcePair = new HashMap<String, Atom>();

}
