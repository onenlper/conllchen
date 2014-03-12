package ruleCoreference.chinese;

import java.util.ArrayList;

import model.EntityMention;

public class SameHeadSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (em.isPronoun) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(antecedent.headStart==em.headStart && antecedent.headEnd==em.headEnd) {
//			if(antecedent.end==em.end) {
				if(RuleCoref.bs.get(0))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		// TODO Auto-generated method stub
		return false;
	}
}
