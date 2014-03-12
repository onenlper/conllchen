package ace.event.coref;

import java.util.ArrayList;

import model.CoNLL.CoNLLPart;
import util.Common;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class JiBaseLineFea {
	// Ji's Base feature set
	public String _pairTypeSubType(EventMention ant, EventMention em, CoNLLPart part) {
		return em.type + "_" + em.subType;
	}

	public boolean _conflictSubType_(EventMention ant, EventMention em, CoNLLPart part) {
		boolean conflict = !em.subType.equals(ant.subType);
		return conflict;
	}
	
	public boolean _isNominal(EventMention ant, EventMention em, CoNLLPart part) {
		return em.noun;
	}

	public String _nomNumber(EventMention ant, EventMention em, CoNLLPart part) {
		return em.number.toString();
	}
	
	public boolean _conflictNumber_(EventMention ant, EventMention em, CoNLLPart part) {
		return em.number != ant.number;
	}

	public boolean _triggerMatch_(EventMention ant, EventMention em, CoNLLPart part) {
		return em.head.equalsIgnoreCase(ant.head);
	}

	public String _triggerPair_(EventMention ant, EventMention em, CoNLLPart part) {
		String m1 = em.head;
		String m2 = ant.head;
		String pair = m1.compareTo(m2) > 0 ? m1 + "#" + m2 : m2 + "#" + m1;
		return pair;
	}

	public String _posPair_(EventMention ant, EventMention em, CoNLLPart part) {
		String m1 = part.getWord(em.headEnd).posTag;
		String m2 = part.getWord(ant.headEnd).posTag;
		String pair = m1.compareTo(m2) > 0 ? m1 + "#" + m2 : m2 + "#" + m1;
		return pair;
	}

	public String _semanticDis(EventMention ant, EventMention em, CoNLLPart part) {
		String semantics1[] = Common.getSemanticDic().get(ant.head);
		String semantics2[] = Common.getSemanticDic().get(em.head);
		int maxSame = 0;
		if (semantics1 != null && semantics2 != null) {

			for (String s1 : semantics1) {
				for (String s2 : semantics2) {
					int k = 0;
					while (k < s1.length() && s1.charAt(k) == s2.charAt(k)) {
						k++;
					}
					if (k > maxSame) {
						maxSame = k;
					}
				}
			}
		}
		return Integer.toString(maxSame);
	}

	// Ji's distance feature
	public String _tokenDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.headEnd - ant.headEnd + 2;
		int k = (int) (Math.log(dis) / Math.log(4));
		if (k > 3) {
			k = 3;
		}
		return Integer.toString(k);
	}

	public String _sentDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.sentenceID - ant.sentenceID + 1;
		int k = (int) (Math.log(dis) / Math.log(2));
		if (k > 3) {
			k = 3;
		}
		return Integer.toString(k);
	}

	public String _eventDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.sequence - ant.sequence + 1;
		int k = (int) (Math.log(dis) / Math.log(2));
		if (k > 3) {
			k = 3;
		}
		int[] ret = new int[9];
		ret[0] = k;
		ret[1] = 4;
		return Integer.toString(k);
	}

	// Ji's argument
	public String _overlapNum(EventMention ant, EventMention em, CoNLLPart part) {
		int k = 0;
		for (EventMentionArgument a1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument a2 : em.getEventMentionArguments()) {
				if (a1.mention.entity == a2.mention.entity && a1.role.equals(a2.role)) {
					k++;
				}
			}
		}
		return Integer.toString(k);
	}

	//TODO
	public ArrayList<String> _overlapRoles(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> roles = new ArrayList<String>();
		for (EventMentionArgument a1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument a2 : em.getEventMentionArguments()) {
				if (a1.mention.entity == a2.mention.entity && a1.role.equals(a2.role)) {
					roles.add(a1.role);
				}
			}
		}
		return roles;
	}
	
	public String _roleMatchIDConflict(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> roles = new ArrayList<String>();
		for (EventMentionArgument a1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument a2 : em.getEventMentionArguments()) {
				if (a1.mention.entity != a2.mention.entity && a1.role.equals(a2.role)) {
					roles.add(a1.role);
				}
			}
		}
		return Integer.toString(roles.size());
	}
	
	public String _idMatchRoleConflict(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> roles = new ArrayList<String>();
		for (EventMentionArgument a1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument a2 : em.getEventMentionArguments()) {
				if (a1.mention.entity == a2.mention.entity && !a1.role.equals(a2.role)) {
					roles.add(a1.role);
				}
			}
		}
		return Integer.toString(roles.size());
	}

	public String _priorNum(EventMention ant, EventMention em, CoNLLPart part) {
		int k = 0;
		for (String role1 : ant.argHash.keySet()) {
			if (!em.argHash.containsKey(role1)) {
				k++;
			}
		}
		return Integer.toString(k);
	}

	public ArrayList<String> _priorRoles(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> roles = new ArrayList<String>();
		for (String role1 : ant.argHash.keySet()) {
			if (!em.argHash.containsKey(role1)) {
				roles.add(role1);
			}
		}
		return roles;
	}

	public String _actNum(EventMention ant, EventMention em, CoNLLPart part) {
		int k = 0;
		for (String role2 : em.argHash.keySet()) {
			if (!ant.argHash.containsKey(role2)) {
				k++;
			}
		}
		return Integer.toString(k);
	}

	public ArrayList<String> _actRoles(EventMention ant, EventMention em, CoNLLPart part) {
		ArrayList<String> roles = new ArrayList<String>();
		for (String role2 : em.argHash.keySet()) {
			if (!ant.argHash.containsKey(role2)) {
				roles.add(role2);
			}
		}
		return roles;
	}

	public String _corefNum(EventMention ant, EventMention em, CoNLLPart part) {
		int k = 0;
		for (EventMentionArgument a1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument a2 : em.getEventMentionArguments()) {
				if (a1.mention.entity == a2.mention.entity && !a1.role.equals(a2.role)) {
					k++;
				}
			}
		}
		return Integer.toString(k);
	}

	public boolean _timeConflict(EventMention ant, EventMention em, CoNLLPart part) {
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && antArg.role.equals("Time-Within")) {
					if (!antArg.mention.head.contains(emArg.mention.head)
							&& !emArg.mention.head.contains(antArg.mention.head)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean _placeConflict(EventMention ant, EventMention em, CoNLLPart part) {
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && antArg.role.equals("Place")) {
					if (antArg.mention.entity != emArg.mention.entity) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public String _modality(EventMention ant, EventMention em, CoNLLPart part) {
		return em.modality;
	}
	
	public String _polarity(EventMention ant, EventMention em, CoNLLPart part) {
		return em.polarity;
	}
	
	public String _genericity(EventMention ant, EventMention em, CoNLLPart part) {
		return em.genericity;
	}	
	
	public String _tense(EventMention ant, EventMention em, CoNLLPart part) {
		return em.tense;
	}
	
	public boolean _modalityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return ant.modality.equals(em.modality);
	}

	public boolean _polarityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return ant.polarity.equals(em.polarity);
	}

	public boolean _genericityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return ant.genericity.equals(em.genericity);
	}

	public boolean _tenseConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return ant.tense.equals(em.tense);
	}
	
	
}
