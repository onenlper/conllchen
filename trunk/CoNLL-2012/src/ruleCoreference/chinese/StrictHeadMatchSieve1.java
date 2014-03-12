package ruleCoreference.chinese;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import util.Common;

/*
 * cluster head match
 * word inclusion
 * compatible modifier
 * not i with i
 */
public class StrictHeadMatchSieve1 extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (em.isPronoun) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if (this.clusterHeadMatch(antecedent, em, ruleCoref)) {
				if (!this.wordInclusion(antecedent, em, ruleCoref)) {
					if(RuleCoref.bs.get(16))
						continue;
				}
				if (haveIncompatibleModify(antecedent, em, ruleCoref.part)) {
					if(RuleCoref.bs.get(17))
						continue;
				}
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (iWithi) {
					if(RuleCoref.bs.get(18))
						continue;
				}
				if(RuleCoref.bs.get(19))
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
			String curExtent = em.extent;
			String canExtent = antecedent.extent;
			int idx = -1;
			boolean inclusion = true;
			for (int i = 0; i < curExtent.length(); i++) {
				idx = canExtent.indexOf(curExtent.charAt(i), idx + 1);
				if (idx == -1) {
					inclusion = false;
					break;
				}
			}
			if (inclusion) {
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
						// System.out.println(antecedent.extent + " " +
						// em.extent + " StrictHeadMatch");
						if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
