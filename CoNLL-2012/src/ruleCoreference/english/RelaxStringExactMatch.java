package ruleCoreference.english;

import java.util.ArrayList;

import model.EntityMention;
import util.Common;


/*
 * proper head equal
 */
public class RelaxStringExactMatch extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun|| !em2.isProperNoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			String removeAfter1 = ruleCoref.ontoCommon.wordBeforeHead(antecedent, ruleCoref.part);
			String removeAfter2 = ruleCoref.ontoCommon.wordBeforeHead(em, ruleCoref.part);
			if(!removeAfter1.isEmpty() && !removeAfter2.isEmpty() && (removeAfter1.equalsIgnoreCase(removeAfter2) 
					|| removeAfter1.equalsIgnoreCase(removeAfter2 + "'s")
					|| removeAfter2.equalsIgnoreCase(removeAfter1 + "'s"))) {
				if (RuleCoref.bs.get(5) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
					return;
				}
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (Common.isPronoun(em.head) || !em.isProperNoun) {
			return false;
		}
		if (antecedent.head.equals(em.head) && antecedent.isProperNoun) {
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (!iWithi) {
				if(ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
					return true;
				}
			}
		}
		return false;
	}
}
