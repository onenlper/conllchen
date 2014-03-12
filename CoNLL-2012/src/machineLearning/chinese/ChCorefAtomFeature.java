package machineLearning.chinese;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ruleCoreference.chinese.ChDictionary;
import util.Common;
import util.Common.Numb;
import util.Common.Person;

import machineLearning.CorefFeature;
import machineLearning.Feature;
import machineLearning.ML;
import model.EntityMention;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;

public class ChCorefAtomFeature extends CorefFeature {

	public ChCorefAtomFeature(ML ml) {
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
		EntityMention ant = pair[0];
		EntityMention em = pair[1];
		ArrayList<Feature> features = new ArrayList<Feature>();
		// same head sieve
		this.sameHeadSieve(features, pair[1], pair[0]);
		// Discourse sieve
		this.discourseSieve(features, pair[1], pair[0]);
		// ExactMatchSieve
		this.exactMatchSieve(features, pair[1], pair[0]);
		// PreciseConstructSieve
		this.preciseConstructSieve(features, pair[1], pair[0]);
		// StrictHeadMatchSieve1
		this.strictHeadMatchSieve1(features, pair[1], pair[0]);
		// StrictHeadMatchSieve4
		this.strictHeadMatchSieve4(features, pair[1], pair[0]);

		this.relaxHeadMatch(features, pair[1], pair[0]);
		// pronoun sieve
		this.pronounSieve(features, pair[1], pair[0]);

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

	private void pronounSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if ((ant.source.contains("双方") || ant.source.contains("两")) && em.head.equalsIgnoreCase("双方")) {
			features.add(new Feature(0, 2));
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
		if(a) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if ((em.person == Person.YOU||em.person==Person.YOUS) && part.getDocument().getType() == DocType.Article
				&& (part.getWord(em.headStart).speaker.equals("-")||part.getWord(em.headStart).speaker.equals("*"))) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (Math.abs(ant.sentenceID - em.sentenceID) > 3 && em.person != Person.I && em.person != Person.YOU) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (!this.ml.chCommon.attributeAgreeMention(ant, em)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if(!this.mentionPersonDisagree(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void relaxHeadMatch(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (this.relaxHeadMatch(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void strictHeadMatchSieve4(ArrayList<Feature> features, EntityMention ant,
			EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			features.add(new Feature(1, 2));
			features.add(new Feature(1, 2));
			return;
		}
		if (this.sameProperHeadLastWord(ant, em)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (this.chHaveDifferentLocation(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if(this.numberInLaterMention(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
			
	}

	private void strictHeadMatchSieve1(ArrayList<Feature> features, EntityMention ant,
			EntityMention em) {
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			features.add(new Feature(1, 2));
			features.add(new Feature(1, 2));
			features.add(new Feature(1, 2));
			return;
		}
		
		if (em.head.equalsIgnoreCase(ant.head)) { 
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
			
		if (!this.haveIncompatibleModify(ant, em)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		if (this.wordInclusion(ant, em, part)) { 
			features.add(new Feature(0, 2));
		}
		else {
			features.add(new Feature(1, 2));
		}
		
		if (!isIWithI(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void preciseConstructSieve(ArrayList<Feature> features, EntityMention ant,
			EntityMention em) {
		if (em.roleSet.contains(ant)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		if (Common.isAbbreviation(ant.source, em.source)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
		// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
		// antecedent: 洛·迈利萨尼迪斯
		if ((((em.ner.equalsIgnoreCase("PERSON") && ant.start == ant.end && ant.source
						.startsWith(em.head)) || (ant.ner.equalsIgnoreCase("PERSON") && em.start == em.end && em.source
						.startsWith(ant.head))) || ((em.ner.equalsIgnoreCase("PERSON")
						&& ant.start == ant.end && ant.source.endsWith(em.head)) || (ant.ner
						.equalsIgnoreCase("PERSON")
						&& em.start == em.end && em.source.endsWith(ant.head))))) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 李登辉总统 李登辉
		if ((((ant.start == ant.end && em.start + 1 == em.end
						&& ant.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(em.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& ant.source.equalsIgnoreCase(part.getWord(em.start).word) && ml.chCommon
						.getChDictionary().titleWords.contains(part.getWord(em.end).word))) || (ant.start + 1 == ant.end
						&& em.start == em.end
						&& em.ner.equalsIgnoreCase("PERSON")
						&& part.getWord(ant.start).rawNamedEntity.equalsIgnoreCase("PERSON")
						&& em.source.equalsIgnoreCase(part.getWord(ant.start).word) && ml.chCommon
						.getChDictionary().titleWords.contains(part.getWord(ant.end).word)))) {
			features.add(new Feature(0, 2));
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
		if (sat) {
			features.add(new Feature(0, 2));
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
		if (c) {
			features.add(new Feature(0, 2));
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
		if (b) {
			features.add(new Feature(0, 2));
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

	private void exactMatchSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
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
		if (modiferCompatible && ant.source.equalsIgnoreCase(em.source)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void discourseSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		String mString = em.original.toLowerCase();
		String antString = ant.original.toLowerCase();
		ChDictionary dict = ml.chCommon.getChDictionary();
		String mSpeaker = part.getWord(em.headStart).speaker;
		String antSpeaker = part.getWord(ant.headStart).speaker;
		CoNLLWord antWord = part.getWord(ant.headStart);
		CoNLLWord mWord = part.getWord(em.headStart);
		// (I - I) in the same speaker's quotation.
		if (ml.chCommon.getChDictionary().firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR
				&& ml.chCommon.getChDictionary().firstPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& mSpeaker.equals(antSpeaker) && !Common.isPronoun(antSpeaker)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// (speaker - I)
		if (ml.chCommon.isSpeaker(ant, em, part)
				&& ((dict.firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR) || (dict.firstPersonPronouns
						.contains(antString) && ant.number == Numb.SINGULAR))) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// You - You
		if (mSpeaker.equalsIgnoreCase(antSpeaker)
				&& (antWord.toSpeaker.containsAll(mWord.toSpeaker) || mWord.toSpeaker.containsAll(antWord.toSpeaker))
				&& !mSpeaker.equalsIgnoreCase("-") && dict.secondPersonPronouns.contains(mString)
				&& dict.secondPersonPronouns.contains(antString) && ant.number == Numb.SINGULAR
				&& em.number == Numb.SINGULAR) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// previous I - you or previous you - I in two person conversation
		if ((em.person == Person.I && ant.person == Person.YOU && antWord.toSpeaker.contains(mSpeaker))
				|| (em.person == Person.YOU && ant.person == Person.I && mWord.toSpeaker.contains(antSpeaker))) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void sameHeadSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (ant.headStart == em.headStart && ant.headEnd == em.headEnd) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
	}
}
