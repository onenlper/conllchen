package machineLearning.chinese;

import java.util.ArrayList;
import java.util.List;

import machineLearning.CorefFeature;
import machineLearning.Feature;
import machineLearning.ML;
import model.EntityMention;

public class ChCorefRuleFeature extends CorefFeature {

	public ChCorefRuleFeature(ML ml) {
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
//		System.out.println(mention.ner);
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
		return this.sievesFeature(train, pair);
	}
	
}
