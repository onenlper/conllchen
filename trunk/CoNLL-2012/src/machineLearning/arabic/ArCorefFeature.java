package machineLearning.arabic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import machineLearning.CorefFeature;
import machineLearning.Feature;
import machineLearning.ML;
import model.EntityMention;
import model.EntityMention.MentionType;
import util.ArCommon;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;

public class ArCorefFeature extends CorefFeature {

	ArCommon arCommon;
	
	public ArCorefFeature(ML ml) {
		super(ml);
		arCommon = new ArCommon("arabic");
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
		
		if(mention.lemma.equalsIgnoreCase("clitics")) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (mention.generic) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		return features;
	}
	
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
		if (ant.source.equals(m.source)) {
			features.add(new Feature(0, 5));
		} // left
		else if (ant.source.startsWith(m.source)) {
			features.add(new Feature(1, 5));
		} // right
		else if (ant.source.endsWith(m.source)) {
			features.add(new Feature(2, 5));
		} // contain
		else if (ant.source.contains(m.source)) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}
		
		// string_match
		if (ant.lemma.equals(m.lemma)) {
			features.add(new Feature(0, 5));
		} // left
		else if (ant.lemma.startsWith(m.lemma)) {
			features.add(new Feature(1, 5));
		} // right
		else if (ant.lemma.endsWith(m.lemma)) {
			features.add(new Feature(2, 5));
		} // contain
		else if (ant.lemma.contains(m.lemma)) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}
		
		// string_match
		if (ant.original.equals(m.original)) {
			features.add(new Feature(0, 5));
		} // left
		else if (ant.original.startsWith(m.original)) {
			features.add(new Feature(1, 5));
		} // right
		else if (ant.original.endsWith(m.original)) {
			features.add(new Feature(2, 5));
		} // contain
		else if (ant.original.contains(m.original)) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}
		
		// string_match
		if (ant.buckWalter.equals(m.buckWalter)) {
			features.add(new Feature(0, 5));
		} // left
		else if (ant.buckWalter.startsWith(m.buckWalter)) {
			features.add(new Feature(1, 5));
		} // right
		else if (ant.buckWalter.endsWith(m.buckWalter)) {
			features.add(new Feature(2, 5));
		} // contain
		else if (ant.buckWalter.contains(m.buckWalter)) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}
		
		// string_match
		if (ant.buckUnWalter.equals(m.buckUnWalter)) {
			features.add(new Feature(0, 5));
		} // left
		else if (ant.buckUnWalter.startsWith(m.buckUnWalter)) {
			features.add(new Feature(1, 5));
		} // right
		else if (ant.buckUnWalter.endsWith(m.buckUnWalter)) {
			features.add(new Feature(2, 5));
		} // contain
		else if (ant.buckUnWalter.contains(m.buckUnWalter)) {
			features.add(new Feature(3, 5));
		} else {
			features.add(new Feature(4, 5));
		}
		
		// 15 PN_STR_MATCH
		if (this.arCommon.mentionExactStringMatch(ant, m, part) && ant.isProperNoun && m.isProperNoun) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 16 PRO_STR_MATCH
		if (ant.isPronoun && m.isPronoun && this.arCommon.mentionExactStringMatch(ant, m, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// Nominal Str match
		if (this.arCommon.mentionExactStringMatch(ant, m, part) && ant.mentionType == MentionType.Nominal
				&& m.mentionType == MentionType.Nominal) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
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


		// in-with-in
		if (this.arCommon.isIWithI(ant, m, part.getCoNLLSentences())) {
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

		// 32 sentence DISTANCE
		int[] antPos = ant.position;
		int[] mPos = m.position;

		double sentenceDiff = Math.abs(antPos[0] - mPos[0]);
		int senDis = (int) Math.ceil(Math.log(sentenceDiff + 1) / Math.log(2));
		if (senDis > 5) {
			senDis = 5;
		}
		features.add(new Feature(senDis, 6));

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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

//	public List<Feature> getLoneFeature(boolean train, EntityMention mention) {
//		List<Feature> features = new ArrayList<Feature>();
//		int feat[] = new int[8];
//		// 1 PRONOUN_1
//		if (mention.isPronoun) {
//			feat[0] = 0;
//		} else {
//			feat[0] = 1;
//		}
//		// 2 subject_1
//		if (mention.isSub) {
//			feat[1] = 0;
//		} else {
//			feat[1] = 1;
//		}
//		// 3 nested_1
//		if (mention.isNNP) {
//			feat[2] = 0;
//		} else {
//			feat[2] = 1;
//		}
//
//		// 4 number_2
//		feat[3] = mention.number.ordinal();
//
//		// 5 gender_2
//		feat[4] = mention.gender.ordinal();
//
//		// 9 animacy_2
//		feat[5] = mention.animacy.ordinal();
//		feat[5] = mention.animacy.ordinal();
//		feat[5] = mention.animacy.ordinal();
//
//		// word sense
//		int[] position = ml.ontoCommon.getPosition(mention, sentences);
//		CoNLLWord word = sentences.get(position[0]).getWord(position[2]);
//		String wordSense = word.getWordSense();
//		if (wordSense.equalsIgnoreCase("-")) {
//			feat[6] = 0;
//		} else {
//			feat[6] = Integer.valueOf(wordSense);
//		}
//		// NE feature
//		if (mention.ner.equals("CARDINAL")) {
//			feat[7] = 0;
//		} else if (mention.ner.equals("DATE")) {
//			feat[7] = 1;
//		} else if (mention.ner.equals("EVENT")) {
//			feat[7] = 2;
//		} else if (mention.ner.equals("FAC")) {
//			feat[7] = 3;
//		} else if (mention.ner.equals("GPE")) {
//			feat[7] = 4;
//		} else if (mention.ner.equals("LAW")) {
//			feat[7] = 5;
//		} else if (mention.ner.equals("LOC")) {
//			feat[7] = 6;
//		} else if (mention.ner.equals("MONEY")) {
//			feat[7] = 7;
//		} else if (mention.ner.equals("NORP")) {
//			feat[7] = 8;
//		} else if (mention.ner.equals("ORDINAL")) {
//			feat[7] = 9;
//		} else if (mention.ner.equals("ORG")) {
//			feat[7] = 10;
//		} else if (mention.ner.equals("PERCENT")) {
//			feat[7] = 11;
//		} else if (mention.ner.equals("PERSON")) {
//			feat[7] = 12;
//		} else if (mention.ner.equals("PRODUCT")) {
//			feat[7] = 13;
//		} else if (mention.ner.equals("QUANTITY")) {
//			feat[7] = 14;
//		} else if (mention.ner.equals("TIME")) {
//			feat[7] = 15;
//		} else if (mention.ner.equals("WORK_OF_ART")) {
//			feat[7] = 16;
//		} else if (mention.ner.equals("LANGUAGE")) {
//			feat[7] = 17;
//		} else {
//			feat[7] = 18;
//		}
//		return features;
//	}
//
//	public List<Feature> getBilateralFea(boolean train, EntityMention[] pair) {
//		ArrayList<Feature> features = new ArrayList<Feature>();
//		int feat[] = new int[30];
//		EntityMention current = pair[0];
//		EntityMention candidate = pair[1];
//
//		String concat = Common.concat(current.head, candidate.head);
//		concat = concat.replace("\n", "").replace("\r", "");
//		String concat2 = Common.concat(candidate.original, current.original);
//		if (train) {
//			if (cancat_ngram.containsKey(concat)) {
//				feat[29] = cancat_ngram.get(concat);
//			} else {
//				feat[29] = cancat_ngram.size();
//				cancat_ngram.put(concat, feat[29]);
//			}
//			if (ml.goldMaps.get(current) != null && ml.goldMaps.get(current).contains(candidate)) {
//				if (positiveHeadPair.containsKey(concat)) {
//					Atom a = positiveHeadPair.get(concat);
//					a.increase();
//				} else {
//					positiveHeadPair.put(concat, new Atom(concat, 1));
//				}
//
//				if (positiveSourcePair.containsKey(concat2)) {
//					Atom a = positiveSourcePair.get(concat2);
//					a.increase();
//				} else {
//					positiveSourcePair.put(concat2, new Atom(concat2, 1));
//				}
//			} else {
//				if (negativeHeadPair.containsKey(concat)) {
//					Atom a = negativeHeadPair.get(concat);
//					a.increase();
//				} else {
//					negativeHeadPair.put(concat, new Atom(concat, 1));
//				}
//				if (negativeSourcePair.containsKey(concat2)) {
//					Atom a = negativeSourcePair.get(concat2);
//					a.increase();
//				} else {
//					negativeSourcePair.put(concat2, new Atom(concat2, 1));
//				}
//			}
//			allHeadPairs.add(concat);
//			allSourcePairs.add(concat2);
//		} else {
//			if (cancat_ngram.containsKey(concat)) {
//				feat[29] = cancat_ngram.get(concat);
//			} else {
//				feat[29] = -1;
//			}
//		}
////		return feat;
//		return features;
//	}

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
	public static HashSet<String> allHeadPairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveHeadPair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeHeadPair = new HashMap<String, Atom>();

	public static HashSet<String> allSourcePairs = new HashSet<String>();
	public static HashMap<String, Atom> positiveSourcePair = new HashMap<String, Atom>();
	public static HashMap<String, Atom> negativeSourcePair = new HashMap<String, Atom>();

}
