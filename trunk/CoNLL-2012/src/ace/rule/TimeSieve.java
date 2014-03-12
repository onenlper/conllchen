package ace.rule;

import java.util.ArrayList;

import model.EntityMention;

public class TimeSieve extends Sieve {
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if(!ruleCoref.ontoCommon.getChDictionary().timePronoun.contains(em.source)) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
   			if (antecedent.isNT) {
   			 if(RuleCoref.bs.get(5))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		return false;
	}
}
