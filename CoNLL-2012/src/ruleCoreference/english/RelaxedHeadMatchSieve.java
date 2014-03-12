package ruleCoreference.english;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import util.Common;

/*
 flags.USE_iwithini = true;
 flags.USE_RELAXED_HEADMATCH = true;
 flags.USE_WORDS_INCLUSION = true;
 flags.USE_ATTRIBUTES_AGREE = true;
 */
public class RelaxedHeadMatchSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			if (!this.relaxHeadMatch(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(23) && !this.wordInclusion(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(24) && !ruleCoref.ontoCommon.attributeAgree(antecedent, em)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(25) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
				return;
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (Common.isPronoun(em.head)) {
			return false;
		}
		if (antecedent.extent.contains(em.head)) {
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
					if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
