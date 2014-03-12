package ruleCoreference.arabic;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import util.Common;

/*
 * cluster head match
 * compatible modifier
 * not i with i
 */
public class StrictHeadMatchSieve3 extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (em.isPronoun) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(antecedent.isPronoun) {
				continue;
			}
			if (this.clusterHeadMatch(antecedent, em, ruleCoref)) {
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
		if (Common.isPronoun(em.head)) {
			return false;
		}
		if (antecedent.head.equals(em.head)) {
			boolean modiferCompatible = true;
			ArrayList<String> curModifiers = em.modifyList;
			ArrayList<String> canModifiers = antecedent.modifyList;
			HashSet<String> canModifiersHash = new HashSet<String>();
			canModifiersHash.addAll(canModifiers);
			for (String curModifier : curModifiers) {
				if (!canModifiersHash.contains(curModifier)) {
					modiferCompatible = false;
					break;
				}
			}
			if (modiferCompatible) {
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (!iWithi) {
					// System.out.println(antecedent.extent + " " + em.extent +
					// " VariantStrictHeadSieve5");
					if(ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
