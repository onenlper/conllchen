package ruleCoreference.english;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import util.Common;

/*
 * cluster head match
 * compatible modifier
 * not i with i
 */
public class StrictHeadMatch3Sieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			if (!this.clusterHeadMatch(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(17) && haveIncompatibleModify(antecedent, em, ruleCoref.part)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(18) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
				return;
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
					if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
