package ace.event.coref;

import java.util.ArrayList;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.EntityMention.MentionType;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common.Gender;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class FeasTuneBase {

	public boolean _conflictCoordinate(EventMention em, EventMention ant, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
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

	public boolean _triggerMatch_(EventMention ant, EventMention em, CoNLLPart part) {
		return em.head.equalsIgnoreCase(ant.head);
	}

	public boolean _commonBV_(EventMention ant, EventMention em, CoNLLPart part) {
		if (ant.head.equals(em.head)) {
			return false;
		}
		// common character
		boolean common = false;
		// loop: for (int i = 0; i < ant.head.length(); i++) {
		// for (int j = 0; j < em.head.length(); j++) {
		// if (ant.head.charAt(i) == em.head.charAt(j)) {
		// common = true;
		// break loop;
		// }
		// }
		// }

		for (String b1 : ant.bvs.keySet()) {
			for (String b2 : em.bvs.keySet()) {
				if (b1.equals(b2)) {
					common = true;
				}
			}
		}

		// same meaning
		/*
		 * // String[] sem1 = Common.getSemantic(em.head); // String[] sem2 =
		 * Common.getSemantic(an.head); // if(sem1!=null && sem2!=null) { //
		 * for(String s1 : sem1) { // for(String s2 : sem2) { //
		 * if(s1.equals(s2) && s1.endsWith("=")) { // return true; // } // } //
		 * } // }
		 */

		if (common) {
			// if (!conflictBV(ant, em, part)) {
			return true;
			// } else {
			// EventMention gEM =
			// RuleCoref.goldEventMentionMap.get(em.toString());
			// EventMention gAn =
			// RuleCoref.goldEventMentionMap.get(an.toString());
			// if (gEM != null && gAn != null && gEM.goldChainID ==
			// gAn.goldChainID) {
			// RuleCoref.printPair(em, an);
			// }
			// }
		}
		return false;
	}

	public boolean conflictBV(EventMention em, EventMention an, CoNLLPart part) {
		if (em.head.equals(an.head)) {
			return false;
		}
		for (String bv1 : em.bvs.keySet()) {
			String pattern1 = em.bvs.get(bv1);
			int idx1 = em.head.indexOf(bv1);
			for (String bv2 : an.bvs.keySet()) {
				String pattern2 = an.bvs.get(bv2);
				int idx2 = an.head.indexOf(bv2);
				if (bv1.equals(bv2)) {
					if (idx1 != idx2 && em.head.length() != 1 && an.head.length() != 1) {
						return true;
					}
					if (pattern1.equals(pattern2) && (pattern1.equals("verb_BV") || pattern2.equals("BV_verb"))) {
						return true;
					}

					if (pattern1.equals(pattern2) && pattern1.equals("adj_BV")) {
						return true;
					}

					if ((pattern1.equals("adj_BV#BV") && pattern1.equals("BV"))
							|| (pattern1.equals("BV") && pattern1.equals("adj_BV#BV"))) {
						return true;
					}
					return false;
				}
			}
		}
		return true;
	}

	public boolean _conflictSubType_(EventMention ant, EventMention em, CoNLLPart part) {
		boolean conflict = !em.subType.equals(ant.subType);
		return conflict;
	}

	public boolean _conflictOverlap_(EventMention ant, EventMention em, CoNLLPart part) {
		return ant.headCharEnd >= em.headCharStart;
	}

	public boolean _conflictNumber_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		return em.number != ant.number;
	}

	public boolean _conflictPersonArgument_(EventMention ant, EventMention em, CoNLLPart part) {
		if (!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("per")
							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("per")) {
						continue;
					}

					if (arg1.get(0).mention.entity != arg2.get(0).mention.entity) {
						conflict = true;
					}
				}
			}
		}
		// arg0, arg1
		if (em.srlArgs.containsKey("A0") && ant.srlArgs.containsKey("A0")) {
			EntityMention m1 = em.srlArgs.get("A0").get(0);
			EntityMention m2 = ant.srlArgs.get("A0").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, part)) {
				conflict = true;
			}
		}
		if (em.srlArgs.containsKey("A1") && ant.srlArgs.containsKey("A1")) {
			EntityMention m1 = em.srlArgs.get("A1").get(0);
			EntityMention m2 = ant.srlArgs.get("A1").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, part)) {
				conflict = true;
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}

	public boolean _conflictDestination_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		if (em.argHash.containsKey("Destination") && ant.argHash.containsKey("Destination")) {
			boolean conflict = false;
			for (EventMentionArgument arg1 : em.argHash.get("Destination")) {
				EntityMention m1 = arg1.mention;
				if (m1.ner.equalsIgnoreCase("other")) {
					continue;
				}
				for (EventMentionArgument arg2 : ant.argHash.get("Destination")) {
					EntityMention m2 = arg2.mention;
					if (m2.ner.equalsIgnoreCase("other")) {
						continue;
					}
					if (m1.entity != m2.entity) {
						conflict = true;
					} else {
						return false;
					}
				}
			}
			return conflict;
		}
		return false;
	}

	public boolean _conflictValueArgument_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
					boolean extra1 = false;
					boolean extra2 = false;
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						if (!m1.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if (!m2.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m2.head.contains(m1.head)) {
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
						if (!m2.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a1 : arg1) {
							EntityMention m1 = a1.mention;
							if (!m1.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m1.head.contains(m2.head)) {
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
			return true;
		} else {
			return false;
		}
	}

	public boolean _conflictACERoleSemantic_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		boolean conflict = false;
		for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					EntityMention m1 = arg1.get(0).mention;
					EntityMention m2 = arg2.get(0).mention;
					if (!m1.semClass.equals(m2.semClass)) {
						conflict = true;
					}
				}
			}
		}
		return conflict;
	}

	public boolean _conflictSRLRoleSemantic_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		boolean conflict = false;
		for (String role1 : em.srlArgs.keySet()) {
			for (String role2 : ant.srlArgs.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EntityMention> arg1 = em.srlArgs.get(role1);
					ArrayList<EntityMention> arg2 = ant.srlArgs.get(role2);
					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					EntityMention m1 = arg1.get(0);
					EntityMention m2 = arg2.get(0);
					if (m1.entity != m2.entity && m1.mentionType == MentionType.Proper
							&& m2.mentionType == MentionType.Proper) {
						conflict = true;
					}
				}
			}
		}
		return conflict;
	}

	public String _triggerPair_(EventMention ant, EventMention em, CoNLLPart part) {
		String m1 = em.head;
		String m2 = ant.head;
		String pair = m1.compareTo(m2) > 0 ? m1 + "#" + m2 : m2 + "#" + m1;
		return pair;
	}

	public String _bvStr_(EventMention ant, EventMention em, CoNLLPart part) {
		String str1 = ant.head;
		String str2 = em.head;
		if (str1.equals(str2)) {
			return "";
		}
		for (String b1 : ant.bvs.keySet()) {
			for (String b2 : em.bvs.keySet()) {
				if (b1.equals(b2)) {
					return b1;
				}
			}
		}
		return "";
	}

	public String _bvConstructPair_(EventMention ant, EventMention em, CoNLLPart part) {
		String str1 = ant.head;
		String str2 = em.head;
		if (str1.equals(str2)) {
			return "";
		}
		for (String b1 : ant.bvs.keySet()) {
			for (String b2 : em.bvs.keySet()) {
				if (b1.equals(b2)) {
					return this.formPair(ant.bvs.get(b1), em.bvs.get(b2));
				}
			}
		}
		return "";
	}

	public boolean _conflictModify_(EventMention ant, EventMention em, CoNLLPart part) {
		if(!this._triggerMatch_(ant, em, part) && !this._commonBV_(ant, em, part)) {
			return false;
		}
		if (!em.modifyList.containsAll(ant.modifyList) && !ant.modifyList.containsAll(em.modifyList)) {
			return true;
		}
		return false;
	}

	public ArrayList<String> _conflictArguments(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
			for (EventMentionArgument arg2 : em.eventMentionArguments) {
				if (arg1.role.equals(arg2.role) && arg1.mention.entity != arg2.mention.entity) {
					args.add(arg1.role);
				}
			}
		}
		return args;
	}

	public ArrayList<String> _corefArguments(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
			for (EventMentionArgument arg2 : em.eventMentionArguments) {
				if (arg1.role.equals(arg2.role) && arg1.mention.entity == arg2.mention.entity) {
					args.add(arg1.role);
				}
			}
		}
		return args;
	}

//	public ArrayList<String> _conflictArgumentsSemantic(EventMention ant, EventMention em, CoNLLPart part) {
//		ArrayList<String> args = new ArrayList<String>();
//		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
//			for (EventMentionArgument arg2 : em.eventMentionArguments) {
//				if (arg1.role.equals(arg2.role) && !arg1.mention.semClass.equals(arg2.mention.semClass)) {
//					args.add(arg1.role);
//				}
//			}
//		}
//		return args;
//	}
//	
//	public ArrayList<String> _conflictArgumentsSubType(EventMention ant, EventMention em, CoNLLPart part) {
//		ArrayList<String> args = new ArrayList<String>();
//		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
//			for (EventMentionArgument arg2 : em.eventMentionArguments) {
//				if (arg1.role.equals(arg2.role) && !arg1.mention.subType.equals(arg2.mention.subType)) {
//					args.add(arg1.role);
//				}
//			}
//		}
//		return args;
//	}
	
	public String _corefArgumentsNo(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
			for (EventMentionArgument arg2 : em.eventMentionArguments) {
				if (arg1.role.equals(arg2.role) && arg1.mention.entity == arg2.mention.entity) {
					args.add(arg1.role);
				}
			}
		}
		return Integer.toString(args.size());
	}

	public String _conflictArgumentsNo(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (EventMentionArgument arg1 : ant.eventMentionArguments) {
			for (EventMentionArgument arg2 : em.eventMentionArguments) {
				if (arg1.role.equals(arg2.role) && arg1.mention.entity != arg2.mention.entity) {
					args.add(arg1.role);
				}
			}
		}
		return Integer.toString(args.size());
	}

	public ArrayList<String> _additionArguments1(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (String role1 : ant.argHash.keySet()) {
			if (!em.argHash.containsKey(role1)) {
				args.add(role1);
			}
		}
		return args;
	}

	public ArrayList<String> _additionArguments2(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> args = new ArrayList<String>();
		for (String role1 : em.argHash.keySet()) {
			if (!ant.argHash.containsKey(role1)) {
				args.add(role1);
			}
		}
		return args;
	}

	public String _posPair(EventMention ant, EventMention em, CoNLLPart part) {
		String m1 = em.posTag;
		String m2 = ant.posTag;
		String pair = m1.compareTo(m2) > 0 ? m1 + "#" + m2 : m2 + "#" + m1;
		return pair;
	}

	private EntityMention getRepresent(EntityMention em) {
		return em;
	}

	private boolean personCompatible(EntityMention em, EntityMention ant, CoNLLPart part) {
		if (em.gender == Gender.MALE && ant.gender == Gender.FEMALE) {
			return false;
		}
		if (em.gender == Gender.FEMALE && ant.gender == Gender.MALE) {
			return false;
		}
		EntityMention m = getRepresent(em);
		EntityMention an = getRepresent(ant);
		if (m.mentionType == MentionType.Proper && an.mentionType == MentionType.Proper && !m.head.equals(an.head)) {
			return false;
		}
		String value1 = "";
		for (int k = ant.headEnd; k >= 0 && k >= ant.headEnd - 2; k--) {
			if (part.getWord(k).posTag.equals("CD")) {
				value1 = part.getWord(k).word;
			}
		}
		String value2 = "";
		for (int k = em.headEnd; k >= 0 && k >= em.headEnd - 2; k--) {
			if (part.getWord(k).posTag.equals("CD")) {
				value2 = part.getWord(k).word;
			}
		}
		HashMap<String, Integer> cluster = new HashMap<String, Integer>();
		cluster.put("3", 3);
		cluster.put("三", 3);
		if (!value1.isEmpty()
				&& !value2.isEmpty()
				&& !value1.equals(value2)
				&& !(cluster.containsKey(value1) && cluster.containsKey(value2) && cluster.get(value1) == cluster
						.get(value2)) && em.ner.equals("CARDINAL") && ant.ner.equals("CARDINAL")) {
			return false;
		}
		return true;
	}

	public String formPair(String s1, String s2) {
		return s1.compareTo(s2) > 0 ? s1 + "#" + s2 : s2 + "#" + s1;
	}

	public boolean _modalityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.modality.equals(em.modality);
	}

	public boolean _polarityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.polarity.equals(em.polarity);
	}

	public boolean _genericityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.genericity.equals(em.genericity);
	}

	public boolean _tenseConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.tense.equals(em.tense);
	}

	// public String _tokenDis(EventMention ant, EventMention em, CoNLLPart
	// part) {
	// int dis = em.headEnd - ant.headEnd + 2;
	// int k = (int) (Math.log(dis) / Math.log(4));
	// if (k > 3) {
	// k = 3;
	// }
	// return Integer.toString(k);
	// }
	//
	// public String _sentDis(EventMention ant, EventMention em, CoNLLPart part)
	// {
	// int dis = em.sentenceID - ant.sentenceID + 1;
	// int k = (int) (Math.log(dis) / Math.log(2));
	// if (k > 3) {
	// k = 3;
	// }
	// return Integer.toString(k);
	// }
	//
	// public String _eventDis(EventMention ant, EventMention em, CoNLLPart
	// part) {
	// int dis = em.sequence - ant.sequence + 1;
	// int k = (int) (Math.log(dis) / Math.log(2));
	// if (k > 3) {
	// k = 3;
	// }
	// int[] ret = new int[9];
	// ret[0] = k;
	// ret[1] = 4;
	// return Integer.toString(k);
	// }
}
