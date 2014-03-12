package ace.ml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.Common;
import ace.ACECommon;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class EventFeatures {

	public static boolean sameTrigger(EventMention ant, EventMention em, CoNLLPart part) {
		boolean ret = false;
		if (ant.head.equals(em.head)) {
			return true;
		}
		return ret;
	}

	public static boolean sameBV(EventMention ant, EventMention em, CoNLLPart part) {
		String antHead = ant.head;
		String emHead = em.head;
		for(int i=0;i<antHead.length();i++) {
			for(int j=0;j<emHead.length();j++) {
				if(antHead.charAt(i)==emHead.charAt(j)) {
					System.err.println(antHead.charAt(i));
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean corefArgument(EventMention ant, EventMention em, CoNLLPart part) {
		boolean corefArg = false;
		for(EventMentionArgument antArg : ant.eventMentionArguments) {
			for(EventMentionArgument emArg : em.eventMentionArguments) {
				if(antArg.role.equalsIgnoreCase(emArg.role) && !argumentCoref(antArg, emArg, part)) {
					return false;
				}
				if(antArg.role.equalsIgnoreCase(emArg.role) && argumentCoref(antArg, emArg, part)) {
					corefArg = true;
				}
			}
		}
		return corefArg;
	}
	
	public static boolean conflictArgument(EventMention ant, EventMention em, CoNLLPart part) {
		for(EventMentionArgument antArg : ant.eventMentionArguments) {
			for(EventMentionArgument emArg : em.eventMentionArguments) {
				if(antArg.role.equalsIgnoreCase(emArg.role) && !argumentCoref(antArg, emArg, part)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean sameMeaning(EventMention ant, EventMention em, CoNLLPart part) {
		if (sameTrigger(ant, em, part)) {
			return false;
		}
		String semantics1[] = Common.getSemanticDic().get(ant.head);
		String semantics2[] = Common.getSemanticDic().get(em.head);

		if (semantics1 == null || semantics2 == null) {
			return false;
		}

		for (String s1 : semantics1) {
			for (String s2 : semantics2) {
				if (s1.substring(0, 5).equalsIgnoreCase(s2.substring(0, 5))) {
					return true;
				}
			}
		}
		return false;
	}

	public static int tokenDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.headCharEnd - ant.headCharEnd + 1;
		int k = (int) (Math.log(dis) / Math.log(4));
		if (k > 3) {
			k = 3;
		}
		return k;
	}

	public static int sentDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.sentenceID - ant.sentenceID + 1;
		int k = (int) (Math.log(dis) / Math.log(2));
		if (k > 3) {
			k = 3;
		}
		return k;
	}

	public static int eventDis(EventMention ant, EventMention em, CoNLLPart part) {
		int dis = em.sequence - ant.sequence + 1;
		int k = (int) (Math.log(dis) / Math.log(2));
		if (k > 3) {
			k = 3;
		}
		return k;
	}

	public static boolean argumentCoref(EventMentionArgument antArg, EventMentionArgument emArg, CoNLLPart part) {
		if (ACEML.goldMentions) {
			return antArg.mention.entity == emArg.mention.entity;
		} else {
			// system argument coreference
			EntityFeatures fea = new EntityFeatures();
			Method methods[] = EntityFeatures.class.getMethods();
			for (Method method : methods) {
				Class<?> argTypes[] = method.getParameterTypes();
				if (argTypes.length == 3 && argTypes[0] == EntityMention.class && argTypes[1] == EntityMention.class
						&& argTypes[2] == CoNLLPart.class) {
					Object b;
					try {
						b = method.invoke(fea, antArg.mention, emArg.mention, part);
						StringBuilder sb = new StringBuilder();
						sb.append(method.getName()).append(":\n");
						if (b instanceof Boolean && ((Boolean) b).booleanValue()) {
//							System.err.println("COREF???" + method.getName() + antArg.mention.head + "#"
//									+ emArg.mention.head);
							return true;
						}
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						System.err.println(method.getName() + "###");
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
			return false;
		}
	}

	public static int overlapRoleAndEntity(EventMention ant, EventMention em, CoNLLPart part) {
		int i = 0;
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					i++;
				}
			}
		}
		return i;
	}

	public static int[] overlapRoles(EventMention ant, EventMention em, CoNLLPart part) {
		int[] roles = new int[ACECommon.roles.size()];
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					roles[ACECommon.roles.indexOf(antArg.role)] = 1;
				}
			}
		}
		return roles;
	}

	public static int corefNumber(EventMention ant, EventMention em, CoNLLPart part) {
		int i = 0;
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (!antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					i++;
				}
			}
		}
		return i;
	}

	public static int priorNumber(EventMention ant, EventMention em, CoNLLPart part) {
		HashSet<EventMentionArgument> match = new HashSet<EventMentionArgument>();
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
				} else {
					match.add(antArg);
				}
			}
		}
		return match.size();
	}

	public static int[] priorRoles(EventMention ant, EventMention em, CoNLLPart part) {
		int[] roles = new int[ACECommon.roles.size()];
		ArrayList<EventMentionArgument> match = new ArrayList<EventMentionArgument>();
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			boolean find = false;
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					find = true;
				}
			}
			if (!find) {
				match.add(antArg);
			}
		}
		for (EventMentionArgument arg : match) {
			roles[ACECommon.roles.indexOf(arg.role)] = 1;
		}
		return roles;
	}

	public static int actNumber(EventMention ant, EventMention em, CoNLLPart part) {
		HashSet<EventMentionArgument> match = new HashSet<EventMentionArgument>();
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			boolean find = false;
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					find = true;
				}
			}
			if (!find) {
				match.add(antArg);
			}
		}
		return match.size();
	}

	public static int[] actRoles(EventMention ant, EventMention em, CoNLLPart part) {
		int[] roles = new int[ACECommon.roles.size()];
		HashSet<EventMentionArgument> match = new HashSet<EventMentionArgument>();
		for (EventMentionArgument emArg : em.eventMentionArguments) {
			boolean find = false;
			for (EventMentionArgument antArg : ant.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && argumentCoref(antArg, emArg, part)) {
					find = true;
				}
			}
			if (!find) {
				match.add(emArg);
			}
		}
		for (EventMentionArgument arg : match) {
			roles[ACECommon.roles.indexOf(arg.role)] = 1;
		}
		return roles;
	}

	public static boolean placeConflict(EventMention ant, EventMention em, CoNLLPart part) {
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && antArg.role.equals("Place")
						&& !argumentCoref(antArg, emArg, part)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean timeConflict(EventMention ant, EventMention em, CoNLLPart part) {
		for (EventMentionArgument antArg : ant.eventMentionArguments) {
			for (EventMentionArgument emArg : em.eventMentionArguments) {
				if (antArg.role.equals(emArg.role) && antArg.role.equals("Time-Within")
						&& !antArg.mention.head.equals(emArg.mention.head)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean modalityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.modality.equals(em.modality);
	}

	public static boolean polarityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.polarity.equals(em.polarity);
	}

	public static boolean genericityConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.genericity.equals(em.genericity);
	}

	public static boolean tenseConflict(EventMention ant, EventMention em, CoNLLPart part) {
		return !ant.tense.equals(em.tense);
	}
	// public static boolean sameBV(EventMention ant, EventMention em, CoNLLPart
	// part) {
	// if(!ant.head.equals(em.head)) {
	//			
	// } else {
	// return false;
	// }
	// }
}
