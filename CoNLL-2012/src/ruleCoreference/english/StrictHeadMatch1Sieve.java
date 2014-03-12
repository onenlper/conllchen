package ruleCoreference.english;

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
public class StrictHeadMatch1Sieve extends Sieve {
	
//	public static boolean sw = false;
	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		if(em2.original.equals("my tooth")) {
			System.out.println();
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			if (!this.clusterHeadMatch(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(12) && !this.wordInclusion(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(13) && haveIncompatibleModify(antecedent, em, ruleCoref.part)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(14) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
//				 System.out.println(antecedent.original + "("+antecedent.head +")"
//						 + " # " + em.original + " (" +em.head +")");
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
