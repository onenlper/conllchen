package ruleCoreference.arabic;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;

public class PronounSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		allPronoun++;
		if(em2.source.equals("他")) {
//			System.out.println();
		}
		if(em2.head.equalsIgnoreCase("双方")) {
			for (EntityMention antecedent : orderedAntecedents) {
				if(antecedent.source.contains("双方") || antecedent.source.contains("两")) {
					if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
						return;
					}
				}
				MyTreeNode node = antecedent.treeNode;
				int CC = 0;
				for(MyTreeNode child : node.children) {
					if(child.value.equalsIgnoreCase("CC")) {
						CC++;
					}
				}
				if(CC==1) {
					if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
						return;
					}
				}
			}
		}
		if (!Common.isPronoun(em2.head)) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		if ((em2.person == Person.YOU||em2.person==Person.YOUS) && ruleCoref.part.getDocument().getType() == DocType.Article
				&& (ruleCoref.part.getWord(em.headStart).speaker.equals("-")||ruleCoref.part.getWord(em.headStart).speaker.equals("*"))) {
			return;
		}
//		for (EntityMention antecedent : orderedAntecedents) {
//			if(em2.isPronoun && antecedent.isPronoun && em.sentenceID-antecedent.sentenceID>2) {
//				continue;
//			}
//			if(em2.isPronoun && antecedent.isPronoun && em2.source.equals(antecedent.source)) {
//				if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
//					return;
//				}
//			}
//		}
		for (EntityMention antecedent : orderedAntecedents) {
			if(antecedent.isNT) {
				continue;
			}
			if (Math.abs(antecedent.sentenceID - em.sentenceID) > 3 && em.person != Person.I && em.person != Person.YOU) {
				continue;
			}
			if (!ruleCoref.ontoCommon.attributeAgree(antecedent, em)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (clusterPersonDisagree(antecedent, em2, ruleCoref.part)) {
				continue;
			}
			if (ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
				return;
			}
		}
		nonResolve++;
	}

	public static int allPronoun = 0;
	
	public static int nonResolve = 0;
	
	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (!Common.isPronoun(em.head)) {
			return false;
		}
		if (antecedent.isPronoun && antecedent.PRONOUN_TYPE != em.PRONOUN_TYPE) {
			return false;
		}
		HashSet<Integer> animacy = new HashSet<Integer>();
		animacy.add(0);
		HashSet<Integer> number = new HashSet<Integer>();
		number.add(2);
		HashSet<String> type = new HashSet<String>();
		HashSet<String> subType = new HashSet<String>();
		HashSet<Integer> gender = new HashSet<Integer>();
		gender.add(2);
		boolean conflict = false;
		EntityMention candidate = antecedent;
		if ((candidate.PRONOUN_TYPE != Common.PRONOUN_ME && candidate.PRONOUN_TYPE != Common.PRONOUN_ME_S
				&& candidate.PRONOUN_TYPE != Common.PRONOUN_YOU && candidate.PRONOUN_TYPE != Common.PRONOUN_YOU_S)
				&& (em.PRONOUN_TYPE == Common.PRONOUN_ME || em.PRONOUN_TYPE == Common.PRONOUN_ME_S
						|| em.PRONOUN_TYPE == Common.PRONOUN_YOU || em.PRONOUN_TYPE == Common.PRONOUN_YOU_S)) {
			conflict = true;
		}
		if (candidate.animacy == Animacy.UNKNOWN) {
			animacy.add(0);
			animacy.add(-1);
			animacy.add(1);
		}
		animacy.add(candidate.animacy.ordinal());
		if (candidate.number == Numb.UNKNOWN) {
			number.add(0);
			number.add(1);
			number.add(2);
		}
		number.add(candidate.number.ordinal());
		if (candidate.gender == Gender.UNKNOWN) {
			gender.add(0);
			gender.add(1);
			gender.add(2);
		}
		boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
		if (!iWithi) {
			if (animacy.contains(em.animacy.ordinal()) && number.contains(em.number.ordinal())
					&& gender.contains(em.gender.ordinal())) {
				// System.out.println(antecedent.extent + " " + em.extent +
				// " pronoun");
				if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
					return true;
				}
			}
		}
		return false;
	}
}
