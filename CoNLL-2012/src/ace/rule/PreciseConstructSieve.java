package ace.rule;

import java.util.ArrayList;

import ace.model.EventMention;

import model.EntityMention;
import util.Common;

/*
 * This sieve will link two mentions which are in appositive structure, predicate nominative, role appositive, 
 * relative isAbbreviation structure 
 */
public class PreciseConstructSieve extends Sieve {
	public static boolean role = false;

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if(em instanceof EventMention) {
			return;
		}
		for (EntityMention antecedent : orderedAntecedents) {
			if (this.isCopular(antecedent, em, ruleCoref.part)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
			if (this.isRoleAppositive(antecedent, em)) {
				if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
					return;
				}
			}
			if (this.isAbb(antecedent, em, ruleCoref)) {
				if (RuleCoref.bs.get(10)) {
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
					}
				}
			}
			// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
			// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
			// antecedent: 洛·迈利萨尼迪斯
			if (this.isSamePerson(antecedent, em, ruleCoref)) {
				if (RuleCoref.bs.get(11))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
					}
			}
			// 李登辉总统 李登辉
			if (this.isTitle(antecedent, em, ruleCoref)) {
				if (RuleCoref.bs.get(12))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
					}
			}
			// 海军陆战队和陆军 UNKNOWN PLURAL UNKNOWN OTHER 10:212,215 #陆军和海军陆战队
			// if (this.isSameComponents(antecedent, em, ruleCoref)) {
			// if (RuleCoref.bs.get(13))
			// if (ruleCoref.combine2Entities(antecedent, em,
			// ruleCoref.sentences)) {
			// System.out.println(antecedent.extent + "#" + em.extent +
			// " isSameComponents");
			// return;
			// }
			// }
			// 多国 INANIMATE PLURAL UNKNOWN NORP 13:635,635 #多明尼加
			if (this.isAbbNation(antecedent, em, ruleCoref)) {
				if (RuleCoref.bs.get(14))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
					}
			}
			// 陈总统 ANIMATE SINGULAR MALE OTHER 13:615,616 #陈水扁总统
			if (this.isAbbName(antecedent, em, ruleCoref)) {
				if (RuleCoref.bs.get(15))
					if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
						return;
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
		if (Common.isAbbreviation(antecedent.head, em.head)) {
			if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
				return true;
			}
		}
		return false;
	}

}
