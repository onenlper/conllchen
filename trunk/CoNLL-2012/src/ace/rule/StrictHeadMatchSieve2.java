package ace.rule;

import java.util.ArrayList;

import ace.model.EventMention;

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
		if(em instanceof EventMention) {
			return;
		}
		if (em.isPronoun) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(antecedent.isPronoun) {
				if(RuleCoref.bs.get(20))
					continue;
			}
			if (this.clusterHeadMatch(antecedent, em, ruleCoref)) {
				if (!this.wordInclusion(antecedent, em, ruleCoref)) {
					if(RuleCoref.bs.get(21))
						continue;
				}
				boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
				if (iWithi) {
					if(RuleCoref.bs.get(22))
						continue;
				}
				if(RuleCoref.bs.get(23))
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
