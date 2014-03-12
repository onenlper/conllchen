package ace.rule;

import java.util.ArrayList;

import model.EntityMention;
import util.Common;

/*
 * one mention is another's alias
 * require NER type equal
 */
public class AliasMatchSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if(em.isNT) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(antecedent.isNT) {
				continue;
			}
			if (em.ner.equals(antecedent.ner) && !em.ner.equalsIgnoreCase("OTHER")
					&& (!em.ner.equals("CARDINAL") || em.extent.equals(antecedent.extent))) {
				if (Common.contain(antecedent.head, em.head) || Common.contain(em.head, antecedent.head)) {
					if (clusterPersonDisagree(antecedent, em, ruleCoref.part)) {
						continue;
					}
					if (!ruleCoref.ontoCommon.attributeAgree(antecedent, em)) {
						continue;
					}
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
					}
				}
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (em.ner.equals(antecedent.ner) && !em.ner.equals("OTHER")
				&& (!em.ner.equals("CARDINAL") || em.extent.equals(antecedent.extent))) {
			if (Common.contain(antecedent.head, em.head) || Common.contain(em.head, antecedent.head)) {
				if(ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
					return true;
				}
			}
		}
		return false;
	}
}
