package ace.event.mln;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.Common;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.rule.RuleCoref;

public class MLNEventFeas {

	public static HashMap<String, int[]> commonBV = Common.readFile2Map3("ACE_CommonBV");

	public static HashMap<String, int[]> commonPair = Common.readFile2Map3("ACE_CommonPair");

	public boolean triggerMatch_(EventMention em, EventMention ant, CoNLLPart part) {
		return false;
	}

	public boolean commonBV_(EventMention em, EventMention an, CoNLLPart part) {
		// boolean com
		String str = em.head.compareTo(an.head) > 0 ? (em.head + "#" + an.head) : (an.head + "#" + em.head);
		int pair[] = commonPair.get(str);
		if (em.head.equals(an.head)) {
			return true;
		}
		if (!em.head.equals(an.head)) {
			String h1 = em.head;
			String h2 = an.head;
			String bv = "";
			loop: for (int i = 0; i < h1.length(); i++) {
				for (int j = 0; j < h2.length(); j++) {
					if (h1.charAt(i) == h2.charAt(j)) {
						bv = Character.toString(h1.charAt(i));
						break loop;
					}
				}
			}
			if (!bv.isEmpty()) {
				boolean pFail = false;
				boolean bFail = false;
				if (pair != null) {
					double p = ((double) pair[0]) / ((double) (pair[0] + pair[1]));
					if (p == 0.0) {
						pFail = true;
					}
				}
				int b[] = commonBV.get(bv);
				if (b != null) {
					double p = ((double) b[0]) / ((double) (b[0] + b[1]));
					if (p < 0.00) {
						bFail = true;
					}
				}
				if (bFail) {
					if (an.goldChainID == em.goldChainID) {
					} else {
					}
					return false;
				}
				return true;
			}
		}
		return false;
	}

	public boolean conflictOrgArgument_(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
//		loop: for (String role1 : em.argHash.keySet()) {
//			for (String role2 : ant.argHash.keySet()) {
//				if (role1.equalsIgnoreCase(role2)) {
//					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
//					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
//
//					if (arg1.size() != 1 || arg2.size() != 1) {
//						continue;
//					}
//					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("org")
//							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("org")) {
//						continue;
//					}
//					boolean extra1 = false;
//					boolean extra2 = false;
//					for (EventMentionArgument a1 : arg1) {
//						EntityMention m1 = a1.mention;
//						boolean extra = true;
//						for (EventMentionArgument a2 : arg2) {
//							EntityMention m2 = a2.mention;
//							if (m2.entity == m1.entity) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra1 = true;
//							break;
//						}
//					}
//
//					for (EventMentionArgument a2 : arg2) {
//						EntityMention m2 = a2.mention;
//						boolean extra = true;
//						for (EventMentionArgument a1 : arg1) {
//							EntityMention m1 = a1.mention;
//							if (m2.entity == m1.entity) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra2 = true;
//							break;
//						}
//					}
//					if (extra1 && extra2) {
//						conflict = true;
//						break loop;
//					}
//				}
//			}
//		}
//		if (conflict) {
//			return true;
//		}
		return false;
	}

	public boolean exactArgs_(EventMention em, EventMention ant, CoNLLPart part) {
		// if(this.commonBV_(em, ant, part) || this.triggerMatch_(em, ant,
		// part)) {
		// return false;
		// }
		// if (em.subType.equals(ant.subType)) {
		// if (em.eventMentionArguments.size() ==
		// ant.eventMentionArguments.size()
		// && ant.eventMentionArguments.size() > 2) {
		// for (EventMentionArgument a1 : em.eventMentionArguments) {
		// boolean coref = false;
		// for (EventMentionArgument a2 : em.eventMentionArguments) {
		// if (a1.mention.entity == a2.mention.entity &&
		// a1.role.equals(a2.role)) {
		// coref = true;
		// break;
		// }
		// }
		// if (!coref) {
		// return false;
		// }
		// }
		// return true;
		// }
		// }
		return false;
	}

	public boolean conflictPolarity_(EventMention em, EventMention ant, CoNLLPart part) {
		return !em.polarity.equals(ant.polarity);
	}
	
	public boolean conflictModality_(EventMention em, EventMention ant, CoNLLPart part) {
		return !em.modality.equals(ant.modality);
	}
	
	public boolean conflictTense_(EventMention em, EventMention ant, CoNLLPart part) {
		return !em.tense.equals(ant.tense);
	}
	
	public boolean conflictGenericity_(EventMention em, EventMention ant, CoNLLPart part) {
		return !em.genericity.equals(ant.genericity);
	}
	
	
	public boolean conflictModify_(EventMention em, EventMention ant, CoNLLPart part) {
		if (!em.modifyList.containsAll(ant.modifyList) && !ant.modifyList.containsAll(em.modifyList)) {
			return true;
		}
		return false;
	}

	public boolean conflictNumber_(EventMention em, EventMention ant, CoNLLPart part) {
		return em.number != ant.number;
		// return false;
	}

	public boolean conflictSubType_(EventMention em, EventMention ant, CoNLLPart part) {
		return !em.subType.equals(ant.subType);
		// return false;
	}

	public boolean conflictValueArgument_(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
//		loop: for (String role1 : em.argHash.keySet()) {
//			for (String role2 : ant.argHash.keySet()) {
//				if (role1.equalsIgnoreCase(role2)) {
//					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
//					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
//					boolean extra1 = false;
//					boolean extra2 = false;
//					for (EventMentionArgument a1 : arg1) {
//						EntityMention m1 = a1.mention;
//						if (!m1.semClass.equalsIgnoreCase("value")) {
//							continue;
//						}
//						boolean extra = true;
//						for (EventMentionArgument a2 : arg2) {
//							EntityMention m2 = a2.mention;
//							if (!m2.semClass.equalsIgnoreCase("value")) {
//								continue;
//							}
//							if (m2.head.contains(m1.head)) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra1 = true;
//							break;
//						}
//					}
//
//					for (EventMentionArgument a2 : arg2) {
//						EntityMention m2 = a2.mention;
//						if (!m2.semClass.equalsIgnoreCase("value")) {
//							continue;
//						}
//						boolean extra = true;
//						for (EventMentionArgument a1 : arg1) {
//							EntityMention m1 = a1.mention;
//							if (!m1.semClass.equalsIgnoreCase("value")) {
//								continue;
//							}
//							if (m1.head.contains(m2.head)) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra2 = true;
//							break;
//						}
//					}
//					if (extra1 && extra2) {
//						conflict = true;
//						break loop;
//					}
//				}
//			}
//		}
//		if (conflict) {
//			if (em.goldChainID != ant.goldChainID) {
//				// System.err.println("GEEEEEE");
//				// ruleCoref.printPair(em, ant);
//			}
//			return true;
//		} else {
		return false;
	}

	public boolean conflictPersonArgument_(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
//		loop: for (String role1 : em.argHash.keySet()) {
//			for (String role2 : ant.argHash.keySet()) {
//				if (role1.equalsIgnoreCase(role2)) {
//					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
//					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
//
//					if (arg1.size() != 1 || arg2.size() != 1) {
//						continue;
//					}
//
//					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("per")
//							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("per")) {
//						continue;
//					}
//
//					boolean extra1 = false;
//					boolean extra2 = false;
//					for (EventMentionArgument a1 : arg1) {
//						EntityMention m1 = a1.mention;
//						boolean extra = true;
//						for (EventMentionArgument a2 : arg2) {
//							EntityMention m2 = a2.mention;
//							if (m2.entity == m1.entity) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra1 = true;
//							break;
//						}
//					}
//
//					for (EventMentionArgument a2 : arg2) {
//						EntityMention m2 = a2.mention;
//						boolean extra = true;
//						for (EventMentionArgument a1 : arg1) {
//							EntityMention m1 = a1.mention;
//							if (m2.entity == m1.entity) {
//								extra = false;
//								break;
//							}
//						}
//						if (extra) {
//							extra2 = true;
//							break;
//						}
//					}
//					if (extra1 && extra2) {
//						conflict = true;
//						break loop;
//					}
//				}
//			}
//		}
//		if (conflict) {
//			if (em.goldChainID == ant.goldChainID) {
//				// System.err.println("GEEEEEE");
//				// ruleCoref.printPair(em, ant);
//			}
//			return true;
//		}
		return false;
	}

	public boolean conflictTimeArgument(EventMention em, EventMention ant, CoNLLPart part) {
		boolean conflict = false;
		for (String role1 : em.argHash.keySet()) {
			if (role1.toLowerCase().startsWith("time") && ant.argHash.containsKey(role1)) {
				ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
				ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role1);
				if (!arg1.get(0).mention.head.contains(arg2.get(0).mention.head)
						&& !arg2.get(0).mention.head.contains(arg1.get(0).mention.head)) {
					conflict = true;
				}

			}
		}
		if (conflict) {
			if (em.goldChainID == ant.goldChainID) {
				RuleCoref.printPair(em, ant);
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean conflictPositionArgument(EventMention em, EventMention ant, CoNLLPart part) {
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

	public boolean conflictOtherArgument(EventMention em, EventMention ant, CoNLLPart part) {
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
							if ((m2.entity != m1.entity && !part.getWord(m1.headEnd).word.equals(part
									.getWord(m2.headEnd)))
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
				RuleCoref.printPair(em, ant);
			}
			return true;
		} else {
			return false;
		}
	}

}
