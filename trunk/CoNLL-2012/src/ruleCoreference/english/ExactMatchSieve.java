package ruleCoreference.english;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import util.Common;

/*
 * links two mentions only if they contain exactly the same extent, also the same modifier
 */
public class ExactMatchSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			if (!ruleCoref.ontoCommon.clusterExactStringMatch(antecedent.entity, em.entity, ruleCoref.part)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(4) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
				return;
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (Common.isPronoun(em.head)) {
			return false;
		}
		if (antecedent.extent.equalsIgnoreCase(em.extent)) {
			// System.out.println(em.extent + " ExactMatchSieve");
			boolean modiferCompatible = true;
			ArrayList<String> curModifiers = em.modifyList;
			ArrayList<String> canModifiers = antecedent.modifyList;
			HashSet<String> curModifiersHash = new HashSet<String>();
			curModifiersHash.addAll(curModifiers);
			HashSet<String> canModifiersHash = new HashSet<String>();
			canModifiersHash.addAll(canModifiers);
			for (String canModifier : canModifiers) {
				if (!curModifiersHash.contains(canModifier)) {
					modiferCompatible = false;
					break;
				}
			}
			for (String curModifier : curModifiers) {
				if (!canModifiersHash.contains(curModifier)) {
					modiferCompatible = false;
					break;
				}
			}
			if (modiferCompatible) {
				if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
					return true;
				}
			}
		}
		return false;
	}
}
