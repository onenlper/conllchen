package ruleCoreference.english;

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
		for(EntityMention antecedent : orderedAntecedents) {
			String concat = Common.concat(antecedent.head.toLowerCase(), em.head.toLowerCase());
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
			String concat2 = Common.concat(antecedent.source.toLowerCase(), em.source.toLowerCase());
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
