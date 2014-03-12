package ruleCoreference.chinese;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

import model.EntityMention;

public class StringPairSieve extends Sieve{

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (RuleCoref.headPair == null) {
			RuleCoref.headPair = Common.readFile2Map5(ruleCoref.language + "_" + ruleCoref.folder + "_head_all");
		}
		if (RuleCoref.sourcePair == null) {
			RuleCoref.sourcePair = Common.readFile2Map5(ruleCoref.language + "_" + ruleCoref.folder + "_source_all");
		}
		for(EntityMention antecedent : orderedAntecedents) {
			String concat = Common.concat(antecedent.head, em.head);
			if(RuleCoref.headPair.containsKey(concat)) {
				double value = RuleCoref.headPair.get(concat);
				if(value >= RuleCoref.t3) {
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						if (ruleCoref.goldMentions.contains(em) && ruleCoref.goldMentions.contains(antecedent) && 
								ruleCoref.goldMaps.get(em).contains(antecedent)) {
//							System.out.println("O LinkHead: " + antecedent + "@" + em);
						} else {
//							System.out.println("X LinkHead: " + antecedent + "@" + em);
						}
						return;
					}
				}
			}
			String concat2 = Common.concat(antecedent.source, em.source);
			if(RuleCoref.sourcePair.containsKey(concat2)) {
				double value = RuleCoref.sourcePair.get(concat2);
				if(value >= RuleCoref.t4) {
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						if (ruleCoref.goldMentions.contains(em) && ruleCoref.goldMentions.contains(antecedent) && 
								ruleCoref.goldMaps.get(em).contains(antecedent)) {
//							System.out.println("O LinkSource: " + antecedent + "@" + em);
						} else {
//							System.out.println("X LinkSource: " + antecedent + "@" + em);
						}
						return;
					}
				}
			}
		}
		
	}

}
