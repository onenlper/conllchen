package ruleCoreference.english;

import java.util.ArrayList;

import model.EntityMention;

// Other string match sieve
public class OtherStringMatch extends Sieve {
	
//	public static boolean sw = false;
	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		if(em2.original.equals("Pali Rural Township")) {
			System.out.println();
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if (!this.neShort(antecedent, em2, ruleCoref.part)) {
				continue;
			}
			if (!ruleCoref.ontoCommon.attributeAgree(antecedent, em2)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em2, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
//				 System.out.println(antecedent.original + "("+antecedent.head +")"
//						 + " # " + em.original + " (" +em.head +")");
				return;
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		return false;
	}
}
