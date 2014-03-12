package ruleCoreference.english;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import util.Common;

/*
 *     flags.USE_iwithini = true;
 flags.USE_INCLUSION_HEADMATCH = true;
 flags.USE_PROPERHEAD_AT_LAST = true;
 flags.USE_DIFFERENT_LOCATION = true;
 flags.USE_NUMBER_IN_MENTION = true;
 */
public class StrictHeadMatch4Sieve extends Sieve {
	public void sieve(RuleCoref ruleCoref, EntityMention em2, ArrayList<EntityMention> orderedAntecedents) {
		if (em2.isPronoun) {
			return;
		}
		EntityMention em = em2.entity.getMostRepresent();
		for (EntityMention antecedent : orderedAntecedents) {
			if (!this.clusterHeadMatch(antecedent, em, ruleCoref)) {
				continue;
			}
			if (RuleCoref.bs.get(19) && !this.sameProperHeadLastWordCluster(antecedent, em, ruleCoref)) {
				continue;
			}
			boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
			if (iWithi) {
				continue;
			}
			if (RuleCoref.bs.get(20) && ruleCoref.ontoCommon.englishHaveDifferentLocation(antecedent, em, ruleCoref.part)) {
				continue;
			}
			if (RuleCoref.bs.get(21) && ruleCoref.ontoCommon.numberInLaterMention(antecedent, em, ruleCoref.part)) {
				continue;
			}
			if (RuleCoref.bs.get(22) && ruleCoref.combine2Entities(antecedent, em2, ruleCoref.sentences)) {
				return;
			}
		}
	}

	public static int right = 0;
	public static int wrong = 0;

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
			for (int i = 0; i < canExtent.length(); i++) {
				idx = curExtent.indexOf(canExtent.charAt(i), idx + 1);
				if (idx == -1) {
					inclusion = false;
					break;
				}
			}
			if (inclusion) {
				boolean modiferCompatible = true;
				ArrayList<String> curModifiers = em.modifyList;
				ArrayList<String> canModifiers = antecedent.modifyList;
				HashSet<String> curModifiersHash = new HashSet<String>();
				curModifiersHash.addAll(curModifiers);
				for (String canModifier : canModifiers) {
					if (!curModifiersHash.contains(canModifier)) {
						modiferCompatible = false;
						break;
					}
				}
				if (modiferCompatible) {
					boolean iWithi = ruleCoref.ontoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
					// if(em.extent.contains("的")) {
					// continue;
					// }
					CoNLLSentence sentence = ruleCoref.sentences.get(em.sentenceID);
					boolean CD = false;
					ArrayList<MyTreeNode> leaves = em.treeNode.getLeaves();
					for (int m = leaves.get(0).leafIdx; m <= leaves.get(leaves.size() - 1).leafIdx; m++) {
						if (sentence.getWord(m).posTag.equalsIgnoreCase("CD")) {
							CD = true;
						}
					}

					if (!iWithi && !CD) {
						// if(corefSys.goldMaps.get(em)!=null &&
						// corefSys.goldMaps.get(em).contains(antecedent)) {
						// System.out.println(antecedent.extent + " " +
						// antecedent.sentenceId + " " + em.extent + " " +
						// em.sentenceId + " VariantStrictHeadSieve6");
						// System.out.println("RIGHT");
						// right ++;
						// } else {
						// System.out.println(antecedent.extent + " " +
						// antecedent.sentenceId + " " + em.extent + " " +
						// em.sentenceId + " VariantStrictHeadSieve6");
						// System.out.println("WRONG");
						// wrong ++;
						// }
						if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
