package ruleCoreference.chinese;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Common.Person;

public abstract class Sieve {

	public abstract void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents);

	public abstract boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref);

	public void act(RuleCoref ruleCoref) {
		ArrayList<EntityMention> selectedMentions = this.mentionSelection(ruleCoref.mentions);
		for (int i = 0; i < selectedMentions.size(); i++) {
			EntityMention em = selectedMentions.get(i);
			ArrayList<EntityMention> orderedAntecedents = ruleCoref.getOrderedAntecedent(em);
			if(ruleCoref.ontoCommon.getChDictionary().notResolve.contains(em.source)) {
				continue;
			}
			sieve(ruleCoref, em, orderedAntecedents);
		}
	}

	public boolean clusterPersonDisagree(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		for (EntityMention ante : antecedent.entity.mentions) {
			for (EntityMention m : mention.entity.mentions) {
				if (mentionPersonDisagree(ante, m, part)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean sameProperHeadLastWordCluster(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		for (EntityMention ante : antecedent.entity.mentions) {
			for (EntityMention cur : em.entity.mentions) {
				if (sameProperHeadLastWord(ante, cur, ruleCoref)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean sameProperHeadLastWord(EntityMention a, EntityMention m, RuleCoref ruleCoref) {
		String ner1 = ruleCoref.part.getWord(a.headEnd).getRawNamedEntity();
		String ner2 = ruleCoref.part.getWord(m.headEnd).getRawNamedEntity();
		if (a.head.equalsIgnoreCase(m.head) && ruleCoref.part.getWord(a.headEnd).posTag.equals("NR")
				&& ruleCoref.part.getWord(m.headEnd).posTag.equals("NR")) {
			return true;
		}
		if(a.head.equalsIgnoreCase(m.head) && ner1.equalsIgnoreCase(ner2) && 
				(ner1.equalsIgnoreCase("PERSON") || ner1.equalsIgnoreCase("GPE") || ner1.equalsIgnoreCase("LOC"))) {
			return true;
		}
		return false;
	}

	public boolean mentionPersonDisagree(EntityMention ant, EntityMention m, CoNLLPart part) {
		String speak1 = part.getWord(ant.headStart).speaker;
		String speak2 = part.getWord(m.headStart).speaker;
		boolean sameSpeaker = speak1.equals(speak2);

		// "我" X "他" and "你" X "他"
		if (ant.person == Person.HE || ant.person == Person.SHE) {
			if (m.person == Person.I || m.person == Person.YOU) {
				return true;
			}
		}
		if (m.person == Person.HE || m.person == Person.HE) {
			if (ant.person == Person.I || ant.person == Person.YOU) {
				return true;
			}
		}

		// 我们 X 他们
		if (ant.person == Person.THEY) {
			if (m.person == Person.WE || m.person == Person.YOUS) {
				return true;
			}
		}
		if (m.person == Person.THEY) {
			if (ant.person == Person.WE || ant.person == Person.YOUS) {
				return true;
			}
		}

		// 这 && 那
		if ((m.source.contains("这") && ant.source.contains("那"))
				|| (m.source.contains("那") && ant.source.contains("这"))) {
			return true;
		}

		// 我的院子 and 我 can't coreference
		if (ant.end != ant.start && m.isPronoun && part.getWord(ant.start).word.equals(m.head)) {
			return true;
		}
		if (sameSpeaker && m.person != ant.person) {
			if ((m.person == Person.IT && ant.person == Person.THEY)
					|| (m.person == Person.THEY && ant.person == Person.IT)
					|| (m.person == Person.THEY && ant.person == Person.THEY))
				return false;
			else if (m.person != Person.UNKNOWN && ant.person != Person.UNKNOWN)
				return true;
		}
		if (sameSpeaker) {
			if (!ant.isPronoun) {
				if (m.person == Person.I || m.person == Person.WE || m.person == Person.YOU)
					return true;
			} else if (!m.isPronoun) {
				if (ant.person == Person.I || ant.person == Person.WE || ant.person == Person.YOU)
					return true;
			}
		}
		if (m.person == Person.YOU && ant.compareTo(m) < 0) {
			if (!part.getWord(m.headStart).speaker.equals("-")) {
				int currentUtterOrder = part.getWord(m.headStart).utterOrder;
				int anteUtterOrder = part.getWord(ant.headStart).utterOrder;
				if (anteUtterOrder != -1 && anteUtterOrder == currentUtterOrder - 1 && ant.person == Person.I) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
	public boolean isSamePerson(EntityMention ant, EntityMention mention, RuleCoref ruleCoref) {
		for (EntityMention antecedent : ant.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if ((em.ner.equalsIgnoreCase("PERSON") && antecedent.start == antecedent.end && antecedent.source
						.startsWith(em.head))
						|| (antecedent.ner.equalsIgnoreCase("PERSON") && em.start == em.end && em.source
								.startsWith(antecedent.head))) {
//					System.out.println(antecedent.extent + " # " + em.extent + " isSamePerson");
					return true;
				}
				if ((em.ner.equalsIgnoreCase("PERSON") && antecedent.start == antecedent.end && antecedent.source
						.endsWith(em.head))
						|| (antecedent.ner.equalsIgnoreCase("PERSON") && em.start == em.end && em.source
								.endsWith(antecedent.head))) {
//					System.out.println(antecedent.extent + " # " + em.extent + " isSamePerson");
					return true;
				}
			}
		}
		return false;
	}

	// 李登辉总统 李登辉
	public boolean isTitle(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		CoNLLPart part = ruleCoref.part;
		if (antecedent.start == antecedent.end && em.start + 1 == em.end && antecedent.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(em.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& antecedent.source.equalsIgnoreCase(part.getWord(em.start).word)
				&& ruleCoref.ontoCommon.getChDictionary().titleWords.contains(part.getWord(em.end).word)) {
//			System.out.println(antecedent.extent + " # " + em.extent + " isTitle");
			return true;
		}
		if (antecedent.start + 1 == antecedent.end && em.start == em.end && em.ner.equalsIgnoreCase("PERSON")
				&& part.getWord(antecedent.start).rawNamedEntity.equalsIgnoreCase("PERSON")
				&& em.source.equalsIgnoreCase(part.getWord(antecedent.start).word)
				&& ruleCoref.ontoCommon.getChDictionary().titleWords.contains(part.getWord(antecedent.end).word)) {
//			System.out.println(antecedent.extent + " # " + em.extent + " isTitle");
			return true;
		}
		return false;
	}

	public boolean isAbb(EntityMention ant, EntityMention mention, RuleCoref ruleCoref) {
		for (EntityMention antecedent : ant.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if (Common.isAbbreviation(antecedent.source, em.source)) {
					return true;
				}
			}
		}
		return false;
	}

	// 多国 INANIMATE PLURAL UNKNOWN NORP 13:635,635 #多明尼加
	public boolean isAbbNation(EntityMention ant, EntityMention mention, RuleCoref ruleCoref) {
		for (EntityMention antecedent : ant.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if (antecedent.ner.equalsIgnoreCase("GPE") && em.ner.equalsIgnoreCase("GPE")) {
					if (antecedent.start == antecedent.end && em.start == em.end) {
						String bigger = antecedent.source.length() > em.source.length() ? antecedent.source : em.source;
						String small = antecedent.source.length() <= em.source.length() ? antecedent.source : em.source;
						if (small.length() == 2 && small.charAt(1) == '国' && small.charAt(0) == bigger.charAt(0)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// 陈总统 ANIMATE SINGULAR MALE OTHER 13:615,616 #陈水扁总统
	public boolean isAbbName(EntityMention ant, EntityMention mention, RuleCoref ruleCoref) {
		CoNLLPart part = ruleCoref.part;
		for (EntityMention antecedent : ant.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if (antecedent.start + 1 == antecedent.end && em.start + 1 == em.end
						&& part.getWord(em.end).word.equalsIgnoreCase(part.getWord(antecedent.end).word)) {
					if (part.getWord(antecedent.start).rawNamedEntity.equals("PERSON")
							&& part.getWord(em.start).word.equals(part.getWord(antecedent.start).word.substring(0, 1))) {
						return true;
					}
					if (part.getWord(em.start).rawNamedEntity.equals("PERSON")
							&& part.getWord(antecedent.start).word.equals(part.getWord(em.start).word.substring(0, 1))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean relaxClusterHeadMatch(EntityMention antecedent, EntityMention mention, RuleCoref ruleCoref) {
		for (EntityMention candidate : antecedent.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if (em.head.charAt(0) == candidate.head.charAt(candidate.head.length() - 1) && em.head.length() == 1) {
					// more constraint
					if (em.sentenceID - antecedent.sentenceID <= 3
							&& !ruleCoref.part.getWord(antecedent.headStart).rawNamedEntity.equalsIgnoreCase("PERSON")
							&& !em.head.equals("人")) {
						return true;
					}
				}
				// 八里乡 INANIMATE SINGULAR UNKNOWN GPE 52:1728,1728#八里 INANIMATE SINGULAR UNKNOWN GPE
				if(candidate.animacy==em.animacy && 
						candidate.ner.equalsIgnoreCase(em.ner) && !em.ner.equalsIgnoreCase("OTHER") 
						&& (em.head.startsWith(candidate.head) || candidate.head.startsWith(em.head))) {
//					System.out.println(candidate.extent + " # " + em.extent + " relax");
					return true;
				}
			}
		}
		return false;
	}

	// 海军陆战队和陆军 UNKNOWN PLURAL UNKNOWN OTHER 10:212,215 #陆军和海军陆战队
	public boolean isSameComponents(EntityMention ant, EntityMention mention, RuleCoref ruleCoref) {
		for (EntityMention antecedent : ant.entity.mentions) {
			HashSet<String> antComp = getComponents(antecedent);
			if (antComp == null) {
				continue;
			}
			for (EntityMention em : mention.entity.mentions) {
				HashSet<String> emComp = getComponents(em);
				if (emComp == null) {
					continue;
				}
				if (em.source.length() == antecedent.source.length()) {
					boolean extra1 = false;
					boolean extra2 = false;
					for (String str : antComp) {
						if (!emComp.contains(str)) {
							extra1 = true;
							break;
						}
					}
					for (String str : emComp) {
						if (!antComp.contains(str)) {
							extra2 = true;
							break;
						}
					}
					if (!extra1 && !extra2) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private HashSet<String> getComponents(EntityMention mention) {
		HashSet<String> components = new HashSet<String>();
		MyTreeNode node = mention.treeNode;
		boolean connect = false;
		for (MyTreeNode child : node.children) {
			if (child.value.equalsIgnoreCase("CC") || child.value.equalsIgnoreCase("PU")) {
				connect = true;
				continue;
			}
			components.add(child.toString().replace(" ", ""));
		}
		if (connect) {
			return components;
		} else {
			return null;
		}
	}

	// only resolve mentions that are currently first in textual order of
	// mentions
	public ArrayList<EntityMention> mentionSelection(ArrayList<EntityMention> allMentions) {
		ArrayList<EntityMention> selectedMentions = new ArrayList<EntityMention>();
		Collections.sort(allMentions);
		HashSet<Integer> visitedClusterIds = new HashSet<Integer>();
		for (EntityMention em : allMentions) {
			if (!visitedClusterIds.contains(em.entityIndex)) {
				selectedMentions.add(em);
				visitedClusterIds.add(em.entityIndex);
			}
		}
		return selectedMentions;
	}

	public boolean clusterHeadMatch(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		boolean match = false;
		for (EntityMention candidate : antecedent.entity.mentions) {
			if (em.head.equalsIgnoreCase(candidate.head)) {
				match = true;
				break;
			}
		}
		return match;
	}

	public boolean wordInclusion(EntityMention antecedent, EntityMention mention, RuleCoref ruleCoref) {
		List<String> removeW = Arrays.asList(new String[] { "这个", "这", "那个", "那", "自己", "的", "该", "公司", "这些", "那些",
				"'s" });
		ArrayList<String> removeWords = new ArrayList<String>();
		removeWords.addAll(removeW);
		HashSet<String> mentionClusterStrs = new HashSet<String>();
		for (EntityMention em : mention.entity.mentions) {
			for (int i = em.start; i <= em.end; i++) {
				mentionClusterStrs.add(ruleCoref.part.getWord(i).orig.toLowerCase());
				if (ruleCoref.part.getWord(i).posTag.equalsIgnoreCase("DT") && i < em.end
						&& ruleCoref.part.getWord(i + 1).posTag.equalsIgnoreCase("M")) {
					removeWords.add(ruleCoref.part.getWord(i).word);
					removeWords.add(ruleCoref.part.getWord(i + 1).word);
				}
			}
		}
		mentionClusterStrs.removeAll(removeWords);

		mentionClusterStrs.remove(mention.head.toLowerCase());
		HashSet<String> candidateClusterStrs = new HashSet<String>();
		for (EntityMention e : antecedent.entity.mentions) {
			for (int i = e.start; i <= e.end; i++) {
				candidateClusterStrs.add(ruleCoref.part.getWord(i).orig.toLowerCase());
			}
			candidateClusterStrs.remove(e.head.toLowerCase());
		}
		if (candidateClusterStrs.containsAll(mentionClusterStrs))
			return true;
		else
			return false;
	}

	public boolean haveIncompatibleModify(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		for (EntityMention ant : antecedent.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if((ant.source.startsWith("那") && em.source.startsWith("这"))
						|| (ant.source.startsWith("这") && em.source.startsWith("那"))) {
					return false;
				}
				if (!ant.head.equalsIgnoreCase(em.head)) {
					continue;
				}
				boolean thisHasExtra = false;
				Set<String> thisWordSet = new HashSet<String>();
				Set<String> antWordSet = new HashSet<String>();
				Set<String> locationModifier = new HashSet<String>(Arrays.asList("东", "南", "西", "北", "中", "东面", "南面",
						"西面", "北面", "中部", "东北", "西部", "南部", "下", "上", "新", "旧", "前"));
				String mPRP = "";
				String antPRP = "";
				for (int i = em.start; i <= em.end; i++) {
					String w1 = part.getWord(i).orig.toLowerCase();
					String pos1 = part.getWord(i).posTag;
					if ((pos1.startsWith("PU") || w1.equalsIgnoreCase(em.head))) {
						continue;
					}
//					if ((pos1.startsWith("DEG") && i>em.start)) {
//						mPRP = part.getWord(i-1).word;
//						continue;
//					}
//					if(em.start!=em.end && i==em.start && pos1.equals("PN")) {
//						mPRP = part.getWord(i-1).word;
//						continue;
//					}
					thisWordSet.add(w1);
				}
				for (int j = ant.start; j <= ant.end; j++) {
					String w2 = part.getWord(j).orig.toLowerCase();
					String pos2 = part.getWord(j).posTag;
//					if (pos2.startsWith("DEG") && j>ant.start) {
//						mPRP = part.getWord(j-1).word;
//						continue;
//					}
//					if(ant.start!=ant.end && j==ant.start && pos2.equals("PN")) {
//						antPRP = part.getWord(j).word;
//						continue;
//					}
					antWordSet.add(w2);
				}
				for (String w : thisWordSet) {
					if (!antWordSet.contains(w)) {
						thisHasExtra = true;
					}
				}
				boolean hasLocationModifier = false;
				for (String l : locationModifier) {
					if (antWordSet.contains(l) && !thisWordSet.contains(l)) {
						hasLocationModifier = true;
					}
				}
				if (thisHasExtra || hasLocationModifier) {
					return true;
				}
			}
		}
		return false;
	}
}
