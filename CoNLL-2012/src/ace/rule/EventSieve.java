package ace.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common;
import ace.event.coref.MaxEntEventFeas;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class EventSieve extends Sieve {

	public static HashMap<String, int[]> commonBV = Common.readFile2Map3("ACE_CommonBV");

	public static HashMap<String, int[]> commonPair = Common.readFile2Map3("ACE_CommonPair");

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em1, ArrayList<EntityMention> orderedAntecedents) {
		if (!(em1 instanceof EventMention)) {
			return;
		}
		if (em1.isPronoun) {
			return;
		}
		MaxEntEventFeas fea = new MaxEntEventFeas();
		CoNLLPart part = ruleCoref.part;
		EventMention em = (EventMention) em1;
		for (EntityMention ant1 : orderedAntecedents) {
			if (!(ant1 instanceof EventMention)) {
				continue;
			}
			EventMention ant = (EventMention) ant1;

			// if (fea._triggerMatch_(ant, em, part)) {

			if (fea.ruleFea__(ant, em, part)) {
				// if (this.conflictModify(em, ant, ruleCoref.part)) {
				// if (ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// }
				// continue;
				// }

				// corefence but different role
				// if(this.differentRole(em, ant, ruleCoref.part)) {
				// if (ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// } else {
				// System.err.println("Nice!!!!");
				// }
				// continue;
				// }

				// if (this.conflictTimeArgument(em, ant, ruleCoref.part)) {
				// if (ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// } else {
				// System.err.println("Nice!!!!");
				// }
				// continue;
				// }

				// if (this.coordinate(em, ant, ruleCoref.part)) {
				// if (ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// } else {
				// System.err.println("GEEE");
				// }
				// continue;
				// }

				// if (this.a0Conflict(em, ant, ruleCoref.part)) {
				// if (!ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// }
				// continue;
				// }

				// if (this.a1Conflict(em, ant, ruleCoref.part)) {
				// if (ruleCoref.goldCoref(ant, em)) {
				// System.err.println(em.subType + "#" + ant.subType);
				// System.err.println(ant.modifyList + "#" + em.modifyList);
				// RuleCoref.printPair(em, ant);
				// }
				// continue;
				// }

				// only system
				// if (this.conflictOrgArgument(em, ant, ruleCoref)) {
				// continue;
				// }

				// if (this.conflictOtherArgument(em, ant, ruleCoref)) {
				// continue;
				// }

				// if (this.conflictValueArgument(em, ant, ruleCoref)) {
				// continue;
				// }
				//

				//
				// if (this.conflictGenericity(em, ant, ruleCoref)) {
				// continue;
				// }
				// if (this.conflictModality(em, ant, ruleCoref)) {
				// continue;
				// }
				// if (this.conflictPolarity(em, ant, ruleCoref)) {
				// continue;
				// }
				// if (this.conflictTense(em, ant, ruleCoref)) {
				// continue;
				// }
				//

				//				
				// only gold
				// if (this.conflictPersonArgument(em, ant, ruleCoref)) {
				// continue;
				// }

				if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					return;
				}
			}
		}
	}

	private boolean differentRole(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
		for (EventMentionArgument arg1 : em.getEventMentionArguments()) {
			for (EventMentionArgument arg2 : ant.getEventMentionArguments()) {
				if (arg1.mention.entity == arg2.mention.entity && !arg1.mention.equals(arg2.mention)) {
					if (arg1.role.equals(arg2.role) || (arg1.role.equals("Position") && arg2.role.equals("Person"))
							|| (arg1.role.equals("Person") && arg2.role.equals("Position"))) {
						return false;
					} else {
						conflict = true;
					}
				}
			}
		}
		return conflict;
	}

	private boolean a0Conflict(EventMention em, EventMention ant, CoNLLPart part) {
		if (em.srlArgs.containsKey("A0") && ant.srlArgs.containsKey("A0")) {
			EntityMention a0a = em.srlArgs.get("A0").get(0);
			EntityMention a0b = ant.srlArgs.get("A0").get(0);
			String w1 = part.getWord(a0a.headEnd).word;
			String w2 = part.getWord(a0b.headEnd).word;
			if (a0a.entity != a0b.entity) {
				return true;
			}
		}
		return false;
	}

	private boolean a1Conflict(EventMention em, EventMention ant, CoNLLPart part) {
		if (em.srlArgs.containsKey("A1") && ant.srlArgs.containsKey("A1")) {
			EntityMention a1a = em.srlArgs.get("A1").get(0);
			EntityMention a1b = ant.srlArgs.get("A1").get(0);
			String w1 = part.getWord(a1a.headEnd).word;
			String w2 = part.getWord(a1b.headEnd).word;
			if (a1a.entity != a1b.entity && !w1.equals(w2)) {
				return true;
			}
		}
		return false;
	}

	private boolean conflictOtherArgument(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		boolean conflict = false;
		HashSet<String> notOther = new HashSet<String>(Arrays.asList("org", "per", "time", "value"));
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					if (notOther.contains(arg1.get(0).mention.semClass)
							|| (notOther.contains(arg2.get(0).mention.semClass))) {
						continue;
					}
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if ((m2.entity != m1.entity && !ruleCoref.part.getWord(m1.headEnd).word
									.equals(ruleCoref.part.getWord(m2.headEnd)))
									&& m1.subType.equals(m2.subType)) {
								conflict = true;
							}
						}
					}
				}
			}
		}
		if (conflict) {
			if (em.goldChainID != ant.goldChainID) {
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictOrgArgument(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("org")
							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("org")) {
						continue;
					}
					boolean extra1 = false;
					boolean extra2 = false;
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						boolean extra = true;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if (m2.goldChainID == m1.goldChainID) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra1 = true;
							break;
						}
					}

					for (EventMentionArgument a2 : arg2) {
						EntityMention m2 = a2.mention;
						boolean extra = true;
						for (EventMentionArgument a1 : arg1) {
							EntityMention m1 = a1.mention;
							if (m2.goldChainID == m1.goldChainID) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra2 = true;
							break;
						}
					}
					if (extra1 && extra2) {
						conflict = true;
						break loop;
					}
				}
			}
		}
		if (conflict) {
			if (em.goldChainID == ant.goldChainID) {
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictModify(EventMention em, EventMention ant, CoNLLPart part) {
		boolean extra1 = false;
		for (String m1 : em.modifyList) {
			boolean bad = true;
			for (String m2 : ant.modifyList) {
				if (m2.contains(m1) || m1.contains(m2)) {
					bad = false;
					break;
				}
			}
			if (bad) {
				extra1 = true;
				break;
			}
		}
		boolean extra2 = false;
		for (String m2 : ant.modifyList) {
			boolean bad = true;
			for (String m1 : em.modifyList) {
				if (m2.contains(m1) || m1.contains(m2)) {
					bad = false;
					break;
				}
			}
			if (bad) {
				extra2 = true;
				break;
			}
		}
		if (extra1 && extra2) {
			return true;
		}
		return false;
	}

	private boolean coordinate(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
		if (em.sentenceID == ant.sentenceID && em.head.equals(ant.head) && em.posTag.equals(ant.posTag)) {
			int position1[] = ChCommon.getPosition(em, part.getCoNLLSentences());
			int position2[] = ChCommon.getPosition(ant, part.getCoNLLSentences());
			CoNLLSentence sentence = part.getCoNLLSentences().get(em.sentenceID);
			if (em.posTag.equals("VV") && ant.posTag.equals("VV")) {

				MyTreeNode leaf1 = sentence.syntaxTree.leaves.get(position1[2]);
				MyTreeNode leaf2 = sentence.syntaxTree.leaves.get(position2[2]);

				if (leaf1.parent.getRightSisters().size() == leaf2.parent.getRightSisters().size()
						&& leaf1.parent.getRightSisters().get(0).value
								.equals(leaf2.parent.getRightSisters().get(0).value)) {
					conflict = true;
				}
			} else if (em.noun) {
				ArrayList<MyTreeNode> ancestor1 = sentence.syntaxTree.leaves.get(position1[2]).getAncestors();
				MyTreeNode ip1 = null;
				for (int i = ancestor1.size() - 1; i >= 0; i--) {
					if (ancestor1.get(i).value.equalsIgnoreCase("IP")) {
						ip1 = ancestor1.get(i);
						break;
					}
				}
				ArrayList<MyTreeNode> ancestor2 = sentence.syntaxTree.leaves.get(position2[2]).getAncestors();
				MyTreeNode ip2 = null;
				for (int i = ancestor2.size() - 1; i >= 0; i--) {
					if (ancestor2.get(i).value.equalsIgnoreCase("IP")) {
						ip2 = ancestor2.get(i);
						break;
					}
				}
				boolean cc = false;
				for (int k = position2[2] + 1; k < position1[2]; k++) {
					if (sentence.words.get(k).posTag.equals("CC")) {
						cc = true;
					}
					if (sentence.words.get(k).posTag.equals("PU")) {
						cc = false;
						break;
					}
				}
				if (cc && ip1 == ip2) {
					conflict = true;
				}
			}
		}
		if (conflict) {
			return true;
		}
		return false;
	}

	private boolean conflictPolarity(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		if (!em.polarity.equals(ant.polarity)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictModality(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		if (!em.modality.equals(ant.modality)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictTense(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		if (!em.tense.equals(ant.tense)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictGenericity(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		if (!em.genericity.equals(ant.genericity)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean conflictPositionArgument(EventMention em, EventMention ant, RuleCoref ruleCoref) {
		boolean conflict = false;
		if (em.argHash.containsKey("Position") && ant.argHash.containsKey("Position")) {
			ArrayList<EventMentionArgument> arg1 = em.argHash.get("Position");
			ArrayList<EventMentionArgument> arg2 = ant.argHash.get("Position");
			if (arg1.size() != arg2.size()) {
				conflict = true;
			}
			for (EventMentionArgument a1 : arg1) {
				boolean extra = true;
				for (EventMentionArgument a2 : arg2) {
					if (a2.mention.head.equals(a1.mention.head)) {
						extra = false;
						break;
					}
				}
				if (extra) {
					conflict = true;
				}
			}

		}

		if (conflict) {
			if (em.goldChainID != ant.goldChainID) {
				// System.err.println("GEEEEEE");
				// ruleCoref.printPair(em, ant);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		// TODO Auto-generated method stub
		return false;
	}
}
