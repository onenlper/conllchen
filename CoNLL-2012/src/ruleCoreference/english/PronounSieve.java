package ruleCoreference.english;

import java.util.ArrayList;

import model.EntityMention;
import model.CoNLL.CoNLLDocument.DocType;
import util.Common;
import util.Common.Person;

public class PronounSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (!em2.isPronoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		if (em.person == Person.YOU && ruleCoref.part.getDocument().getType() == DocType.Article
				&& ruleCoref.part.getWord(em.headStart).speaker.equals("-")) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(RuleCoref.bs.get(26) && Math.abs(antecedent.sentenceID-em.sentenceID) > 3
			        && em.person!=Person.I && em.person!=Person.YOU) {
				continue;
			}
			if (RuleCoref.bs.get(27) && !ruleCoref.ontoCommon.attributeAgree(antecedent, em)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(28) && ruleCoref.ontoCommon.getEnDictionary().demonymSet.contains(antecedent.original.toLowerCase())
					&& ruleCoref.ontoCommon.getEnDictionary().notOrganizationPRP.contains(em.head.toLowerCase())) {
				continue;
			}
			if (RuleCoref.bs.get(29) && clusterPersonDisagree(antecedent, em2, ruleCoref.part)) {
				continue;
			}
			if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
//				System.out.println(antecedent.original + " # " + em.original + " %" + em.head);
				return;
			}
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (!Common.isPronoun(em.head)) {
			return false;
		}
		return false;
	}
}
