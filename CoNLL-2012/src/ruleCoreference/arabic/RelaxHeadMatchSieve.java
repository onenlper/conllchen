package ruleCoreference.arabic;

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
					continue;
				}
				if (haveIncompatibleModify(antecedent, em, ruleCoref.part)) {
					continue;
				}
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (iWithi) {
					continue;
				}
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
