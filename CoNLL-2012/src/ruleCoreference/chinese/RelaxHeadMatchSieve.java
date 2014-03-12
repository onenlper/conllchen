package ruleCoreference.chinese;

import java.util.ArrayList;

import model.EntityMention;

public class RelaxHeadMatchSieve extends Sieve{
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (em.isPronoun) {
			return;
		}
		if(em.source.equals("八里")) {
			System.out.print("");
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if (this.relaxClusterHeadMatch(antecedent, em, ruleCoref)) {
				if (!this.wordInclusion(antecedent, em, ruleCoref)) {
					if(RuleCoref.bs.get(36))
						continue;
				}
				if (haveIncompatibleModify(antecedent, em, ruleCoref.part)) {
					if(RuleCoref.bs.get(37))
						continue;
				}
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (iWithi) {
					if(RuleCoref.bs.get(38))
						continue;
				}
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					if(RuleCoref.bs.get(39))
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
