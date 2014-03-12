package machineLearning.english;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import machineLearning.CorefFeature;
import machineLearning.Feature;
import machineLearning.ML;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.EntityMention.MentionType;
import util.EnCommon;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;

public class EnCorefFeature extends CorefFeature {

	EnCommon enCommon;

	public EnCorefFeature(ML ml) {
		super(ml);
		enCommon = new EnCommon("english");
	}

	public EnCorefFeature(ML ml, CoNLLPart part) {
		this(ml);
		this.part = part;
	}

	public List<Feature> getLoneFeature(boolean train, EntityMention mention) {
		List<Feature> features = new ArrayList<Feature>();
		// PRONOUN_1
		if (mention.mentionType == MentionType.Pronominal) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// Proper noun
		if (mention.mentionType == MentionType.Proper) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// Nominal
		if (mention.mentionType == MentionType.Nominal) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// subject_1
		if (mention.isSub) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// nested_1
		if (mention.isNNP) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 4 number_2
		features.add(new Feature(mention.number.ordinal(), 3));
		// 5 gender_2
		features.add(new Feature(mention.gender.ordinal(), 4));
		// 9 animacy_2
		features.add(new Feature(mention.animacy.ordinal(), 3));
		// 10 person
		features.add(new Feature(mention.person.ordinal(), 8));
		
		if (mention.generic) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		if (this.enCommon.skipMentionRule1(mention)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (this.enCommon.skipMentionRule2(mention)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (this.enCommon.skipMentionRule3(mention)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if(this.enCommon.skipMentionRule4(mention, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		if(allNEs.indexOf(mention.ner.toUpperCase())==-1) {
			System.out.println(mention.ner);
		}
		
		// NE feature
		features.add(new Feature(allNEs.indexOf(mention.ner.toUpperCase()), 19));
		
		return features;
	}

	static ArrayList<String> allNEs = new ArrayList<String>(Arrays.asList("OTHER", "CARDINAL", "DATE", "EVENT", "FAC",
			"GPE", "LAW", "LOC", "MONEY", "NORP", "ORDINAL", "ORG", "PERCENT", "PERSON", "PRODUCT", "QUANTITY", "TIME",
			"WORK_OF_ART", "LANGUAGE"));

	public List<Feature> getBilateralFea(boolean train, EntityMention[] pair) {
		List<Feature> features = new ArrayList<Feature>();
		EntityMention m = pair[0];
		EntityMention ant = pair[1];

		String antStem = ant.source.toLowerCase();
		String mStem = m.source.toLowerCase();

		String antOrig = ant.original.toLowerCase();
		String mOrig = m.original.toLowerCase();

		String antHead = ant.head.toLowerCase();
		String mHead = m.head.toLowerCase();
		
		String antHeadStem = part.getWord(ant.headStart).word;
		String mHeadStem = part.getWord(m.headStart).word;
		
		// string_match
		if (this.enCommon.mentionExactStringMatch(ant, m, part)) {
			features.add(new Feature(0, 5));
		} // left
		else if (this.enCommon.mentionStartWith(ant.getWordSet(), m.getWordSet())) {
			features.add(new Feature(1, 5));
		} // right
		else if (this.enCommon.mentionEndWith(ant.getWordSet(), m.getWordSet())) {
			features.add(new Feature(2, 5));
		} // contain
		else if (this.enCommon.mentionContainOther(ant.getWordSet(), m.getWordSet())) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}

		// 15 PN_STR_MATCH
		if (this.enCommon.mentionExactStringMatch(ant, m, part) && ant.isProperNoun && m.isProperNoun) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 16 PRO_STR_MATCH
		if (ant.isPronoun && m.isPronoun && this.enCommon.mentionExactStringMatch(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// Nominal Str match
		if (this.enCommon.mentionExactStringMatch(ant, m, part) && ant.mentionType == MentionType.Nominal
				&& m.mentionType == MentionType.Nominal) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 17 MODIFIER_MATCH
		if (mHead.equalsIgnoreCase(antHead) && !this.enCommon.mentionIncompatibleModify(ant, m, part)) {
			features.add(new Feature(0, 3));
		} else if (mHead.equalsIgnoreCase(antHead) && this.enCommon.mentionIncompatibleModify(ant, m, part)) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 18 PRO_TYPE_MATCH
		if (ant.isPronoun && m.isPronoun) {
			if (ant.person == m.person) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}

		// 19 NUMBER AGREEMENT
		if ((ant.number == Numb.SINGULAR && m.number == Numb.SINGULAR)
				|| (ant.number == Numb.PLURAL && m.number == Numb.PLURAL)) {
			features.add(new Feature(0, 3));
		} else if (ant.number == Numb.UNKNOWN || m.number == Numb.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 20 GENDER AGREEMENT
		if ((ant.gender == Gender.FEMALE && m.gender == Gender.FEMALE)
				|| (ant.gender == Gender.MALE && m.gender == Gender.MALE)) {
			features.add(new Feature(0, 3));
		} else if (ant.gender == Gender.UNKNOWN || m.gender == Gender.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// NE AgreeMent
		if (ant.ner.equalsIgnoreCase(m.ner) && !ant.ner.equalsIgnoreCase("OTHER")) {
			features.add(new Feature(0, 3));
		} else if (ant.ner.equalsIgnoreCase("OTHER") || m.ner.equalsIgnoreCase("OTHER")) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// Animacy
		if ((ant.animacy == Animacy.ANIMATE && m.animacy == Animacy.ANIMATE)
				|| (ant.animacy == Animacy.INANIMATE && m.animacy == Animacy.INANIMATE)) {
			features.add(new Feature(0, 3));
		} else if (ant.animacy == Animacy.UNKNOWN || m.animacy == Animacy.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 21 AGREEMENT
		if (this.enCommon.mentionAttributeAgree(ant, m)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// in-with-in
		if (this.enCommon.isIWithI(ant, m, part.getCoNLLSentences())) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// have same head start end
		if (ant.headStart == m.headStart && ant.headEnd == m.headEnd) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// head match
		if (ant.head.equalsIgnoreCase(m.head)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 29 COPULAR
		if (this.enCommon.isEnglishCopular(ant, m, sentences)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 32 sentence DISTANCE
		int[] antPos = ant.position;
		int[] mPos = m.position;

		double sentenceDiff = Math.abs(antPos[0] - mPos[0]);
		int senDis = (int) Math.ceil(Math.log(sentenceDiff + 1) / Math.log(2));
		if (senDis > 5) {
			senDis = 5;
		}
		features.add(new Feature(senDis, 6));

//		double wordDiff = Math.abs(ant.headStart - m.headStart);
//		int wordDis = (int) Math.ceil(Math.log(wordDiff + 1) / Math.log(2));
//		if (wordDis > 5) {
//			wordDis = 5;
//		}
//		features.add(new Feature(wordDis, 6));

		// i-i
		if (this.enCommon.discourseI2I(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// (speaker - I)
		if (this.enCommon.discourseSpeaker2I(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// you - you
		if (this.enCommon.discourseyou2you(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// previous I - you or previous you - I in two person conversation
		if (this.enCommon.discourseI2you(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// this to far
		if (this.enCommon.thisToFar(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// generic you
		if (ant.generic && ant.person == Person.YOU) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// I to notspeaker
		if (this.enCommon.disConstraintItoNotSpeaker(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// different speaker
		if (this.enCommon.disConstraintDiffSpeaker(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// cc construct
		if (this.enCommon.ccConstruct(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// Acronym
		if (this.enCommon.isMentionEnglishAcronym(ant, m)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// Demonym
		if (this.enCommon.isEnglishDemonym(ant, m)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// RoleAppositive
		if (this.enCommon.isRoleAppositive(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// person disagree
		if (ant.isPronoun && m.isPronoun) {
			if (this.enCommon.mentionPersonDisagree(ant, m, part)) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}
		
		// distance constraint
		if(this.enCommon.distanceConstraint(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// relax head
		if(this.enCommon.relaxHeadMatch(ant, m)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// word inclusion
		if(this.enCommon.metnionWordInclusion(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// proper word match
		if(this.enCommon.sameProperHeadLastWord(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// relax exact match
		if(this.enCommon.relaxExactMatch(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// NE relation feature
		String ner1 = ant.ner.toLowerCase();
		String ner2 = m.ner.toLowerCase();
		String relation = "";
		if (ner1.compareTo(ner2) < 0) {
			relation = ner1.toLowerCase() + "#" + ner2.toLowerCase();
		} else {
			relation = ner2.toLowerCase() + "#" + ner1.toLowerCase();
		}
		features.add(new Feature(this.getNERelationFeaIdx(relation), -1));

		// 37 between words
		int betweenIndex = 0;
		if (ant.position[0] == m.position[0]) {
			int start = ant.position[2] + 1;
			int end = m.position[1] - 1;
			StringBuilder sb = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append("#");
			}
			String between = sb.toString();
			if (end-start<3) {
				if (train) {
					if (stringFea2.containsKey(between)) {
						betweenIndex = stringFea2.get(between);
					} else {
						betweenIndex = stringFea2.size();
						stringFea2.put(between, betweenIndex);
					}
				} else {
					if (stringFea2.containsKey(between)) {
						betweenIndex = stringFea2.get(between);
					} else {
						betweenIndex = -1;
					}
				}
			} else {
				betweenIndex = -1;
			}
		} else {
			betweenIndex = -1;
		}
		features.add(new Feature(betweenIndex, -1));
		
		// 39 ngram_concate
		String concatHead = antHead.compareTo(mHead)>0?(antHead+"_"+mHead):(mHead+"_"+antHead);
//		String concatStemHead = antHeadStem.compareTo(mHeadStem)>0?(antHeadStem+"_"+mHeadStem):(mHeadStem+"_"+antHeadStem);
		
		String concatExtent = antOrig.compareTo(mOrig)>0?(antOrig+"_"+mOrig):(mOrig+"_"+antOrig);
//		String concatExtent = antStem.compareTo(mStem)>0?(antStem+"_"+mStem):(mStem+"_"+antStem);
		
		int concatFea = -1;
		if (train) {
			if (stringFea3.containsKey(concatHead)) {
				concatFea = stringFea3.get(concatHead);
			} else {
				concatFea = stringFea3.size();
				stringFea3.put(concatHead, concatFea);
			}
			if (ml.goldMaps.get(m) != null && ml.goldMaps.get(m).contains(ant)) {
				if (positiveHeadPair.containsKey(concatHead)) {
					Atom a = positiveHeadPair.get(concatHead);
					a.increase();
				} else {
					positiveHeadPair.put(concatHead, new Atom(concatHead, 1));
				}

				if (positiveSourcePair.containsKey(concatExtent)) {
					Atom a = positiveSourcePair.get(concatExtent);
					a.increase();
				} else {
					positiveSourcePair.put(concatExtent, new Atom(concatExtent, 1));
				}
			} else {
				if (negativeHeadPair.containsKey(concatHead)) {
					Atom a = negativeHeadPair.get(concatHead);
					a.increase();
				} else {
					negativeHeadPair.put(concatHead, new Atom(concatHead, 1));
				}
				if (negativeSourcePair.containsKey(concatExtent)) {
					Atom a = negativeSourcePair.get(concatExtent);
					a.increase();
				} else {
					negativeSourcePair.put(concatExtent, new Atom(concatExtent, 1));
				}  
			}
			allHeadPairs.add(concatHead);
			allSourcePairs.add(concatExtent);
		} else {
			if (stringFea3.containsKey(concatHead)) {
				concatFea = stringFea3.get(concatHead);
			} else {
				concatFea = -1;
			}
		}
		features.add(new Feature(concatFea, -1));
		return features;
	}


	public static HashSet<String> allHeadPairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveHeadPair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeHeadPair = new HashMap<String, Atom>();
	
	public static HashSet<String> allSourcePairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveSourcePair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeSourcePair = new HashMap<String, Atom>();
	
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
}
