package ruleCoreference.arabic;

import java.util.ArrayList;

import model.EntityMention;
import util.Common;

/*
 * cluster head match
 * word inclusion
 * not i with i
 */
public class StrictHeadMatchSieve2 extends Sieve {
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
				if (!this.wordInclusion(antecedent, em, ruleCoref)) {
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
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (!iWithi) {
					if(ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
