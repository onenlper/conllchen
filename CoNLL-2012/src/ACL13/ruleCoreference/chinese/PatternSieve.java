package ACL13.ruleCoreference.chinese;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.Common;
import ACL13.ruleCoreference.chinese.RuleCoref.Stat;

/*
 * links two mentions only if they contain exactly the same extent, also the same modifier
 */
public class PatternSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if (Common.isPronoun(em.head)) {
			return;
		}
		for(EntityMention m2 : em.entity.mentions) {
			for(EntityMention m1 : ruleCoref.getOrderedAntecedent(m2)) {
				if (satisfy(m1, m2, ruleCoref)) {
					return;
				}
			}
		}
	}

	public boolean satisfy(EntityMention m1, EntityMention m2, RuleCoref ruleCoref) {
		ArrayList<String> patterns = RuleCoref.getPatternFromPairs(m1, m2, ruleCoref.part);
		for (String pattern : patterns) {
			if (RuleCoref.stats.containsKey(pattern)) {
				Stat stat = RuleCoref.stats.get(pattern);
				if (stat.accuracy >= 0.65) {
					if (ruleCoref.combine2Entities(m1, m2, ruleCoref.sentences)) {
						System.err.println(pattern + ":" + RuleCoref.stats.get(pattern).accuracy + "#"
								+ RuleCoref.stats.get(pattern).coref);
						return true;
					}
				}
			}
		}
		return false;
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
