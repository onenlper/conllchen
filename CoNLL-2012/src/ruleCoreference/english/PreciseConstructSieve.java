package ruleCoreference.english;

import java.util.ArrayList;

import model.EntityMention;
import util.Common;

/*
 * This sieve will link two mentions which are in appositive structure, predicate nominative, role appositive, 
 * relative isAbbreviation structure 
 */
public class PreciseConstructSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		for (EntityMention antecedent : orderedAntecedents) {
			if (RuleCoref.bs.get(6) && Common.isEnglishAbbreviation(antecedent.head, em.head)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
			if (RuleCoref.bs.get(7) && ruleCoref.ontoCommon.isEnglishAcronym(antecedent, em)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
			if (RuleCoref.bs.get(8) && ruleCoref.ontoCommon.isEnglishDemonym(antecedent, em)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
			if (RuleCoref.bs.get(9) && ruleCoref.ontoCommon.isRoleAppositive(antecedent, em, ruleCoref.part)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					ruleCoref.roleSets.add(em);
					return;
				}
			}
			
			if(RuleCoref.bs.get(10) && ruleCoref.ontoCommon.isRelativePronoun(antecedent, em, ruleCoref.part)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					ruleCoref.roleSets.add(em);
					return;
				}
			}
			if (!ruleCoref.folder.equals("nw")) {
				if (RuleCoref.bs.get(11) && ruleCoref.ontoCommon.isEnglishCopular(antecedent, em, ruleCoref.part.getCoNLLSentences())) {
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						ruleCoref.roleSets.add(em);
						return;
					}
				}
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (ruleCoref.ontoCommon.isCopular2(antecedent, em, ruleCoref.sentences)) {
			if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
				return true;
			}
		}
		if (ruleCoref.ontoCommon.isRoleAppositive(antecedent, em, ruleCoref.sentences)) {
			if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
				return true;
			}
		}

		if (Common.isAbbreviation(antecedent.head, em.head)) {
			if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
				return true;
			}
		}
		return false;
	}
}
