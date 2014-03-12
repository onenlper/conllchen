package ace.event.mln;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;
import util.Common.Numb;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.PlainText;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.rule.RuleCoref;

public class MLNUtil {

	public static HashMap<EntityMention, Integer> mentionIndexes = new HashMap<EntityMention, Integer>();

	public static int getIndex(EntityMention em) {
		if (mentionIndexes.containsKey(em)) {
			return mentionIndexes.get(em);
		} else {
			mentionIndexes.put(em, mentionIndexes.size());
			return mentionIndexes.size() - 1;
		}
	}

	public static void createNInstance(HashMap<String, ArrayList<String>> features, EntityMention em,
			EntityMention ant, CoNLLPart part, boolean coref) throws Exception {
		if (em.equals(ant)) {
			System.err.println("duplicate");
			return;
		}

		MLNEntityFeas feas = new MLNEntityFeas();
		Method methods[] = MLNEntityFeas.class.getMethods();
		int[] pair = new int[2];
		pair[0] = getIndex(ant);
		pair[1] = getIndex(em);

		if (pair[0] >= pair[1]) {
			System.err.println("GEE");
			System.exit(1);
		}

		for (Method method : methods) {
			if (method.getName().endsWith("_")) {
				Object obj = method.invoke(feas, ant, em, part);
				if (obj instanceof Boolean && ((Boolean) obj).booleanValue()) {
					ArrayList<String> pairs = features.get(method.getName());
					if (pairs == null) {
						pairs = new ArrayList<String>();
						features.put(method.getName(), pairs);
					}
					pairs.add(pair[0] + " " + pair[1]);
				} else if (obj instanceof String) {
					ArrayList<String> pairs = features.get(method.getName());
					if (pairs == null) {
						pairs = new ArrayList<String>();
						features.put(method.getName(), pairs);
					}
					pairs.add(pair[0] + " " + pair[1] + " \"" + obj + "\"");
				}
			}
		}
		if (coref) {
			ArrayList<String> pairs = features.get("nCoref");
			if (pairs == null) {
				pairs = new ArrayList<String>();
				features.put("nCoref", pairs);
			}
			pairs.add(pair[0] + " " + pair[1]);
		}

	}

	public static void createInstance(HashMap<String, ArrayList<String>> features, EntityMention em2,
			EntityMention ant2, CoNLLPart part, boolean coref) throws Exception {
		if (em2.equals(ant2)) {
			System.err.println("duplicate");
			return;
		}
		EventMention em = (EventMention) em2;
		EventMention ant = (EventMention) ant2;
		MLNEventFeas feas = new MLNEventFeas();
		Method methods[] = MLNEventFeas.class.getMethods();
		int[] pair = new int[2];
		pair[0] = getIndex(ant);
		pair[1] = getIndex(em);
		if (pair[0] >= pair[1]) {
			System.err.println("GEE");
			System.exit(1);
		}
		for (Method method : methods) {
			if (method.getName().endsWith("_")) {
				Object obj = method.invoke(feas, ant, em, part);
				if (obj instanceof Boolean && ((Boolean) obj).booleanValue()) {
					ArrayList<String> pairs = features.get(method.getName());
					if (pairs == null) {
						pairs = new ArrayList<String>();
						features.put(method.getName(), pairs);
					}
					pairs.add(pair[0] + " " + pair[1]);
				} else if (obj instanceof String) {
					ArrayList<String> pairs = features.get(method.getName());
					if (pairs == null) {
						pairs = new ArrayList<String>();
						features.put(method.getName(), pairs);
					}
					pairs.add(pair[0] + " " + pair[1] + " \"" + obj + "\"");
				}
			}
		}
		if (coref) {
			ArrayList<String> pairs = features.get("coref");
			if (pairs == null) {
				pairs = new ArrayList<String>();
				features.put("coref", pairs);
			}
			pairs.add(pair[0] + " " + pair[1]);
		}
	}

	private static void calAttribute(EventMention em, CoNLLPart part) {
		em.number = Numb.SINGULAR;
		em.posTag = part.getWord(em.headEnd).posTag;

		if (em.posTag.equals("NN")) {
			em.noun = true;
			RuleCoref.ontoCommon.calEventNounAttribute(em, part);
			// System.err.println(em.head + "#" + em.modifyList + "#" +
			// em.number + "#" + em.goldChainID);
		}

		for (EventMentionArgument arg : em.eventMentionArguments) {
			ArrayList<EventMentionArgument> args = em.argHash.get(arg.role);
			if (args == null) {
				args = new ArrayList<EventMentionArgument>();
				em.argHash.put(arg.role, args);
			}
			args.add(arg);
		}

	}

	static boolean goldMentions;

	public static ArrayList<EntityMention> getEntityEventMentions(CoNLLPart part, boolean gold) {
		goldMentions = gold;
		ArrayList<EntityMention> mentions = null;
		if (gold) {
			mentions = getGoldTestEventMentions(part);
		} else {
			mentions = getSystemTestEventMentions(part);
		}
		ChCommon chCommon = new ChCommon("chinese");
		int sequence = 0;
		Collections.sort(mentions);
		for (EntityMention mention : mentions) {
			int idx = getIndex(mention);
			if (mention instanceof EventMention) {
				continue;
			}
			mention.sequence = sequence++;
			ACECorefCommon.assingStartEnd(mention, part);
			chCommon.calACEAttribute(mention, part);
		}
		return mentions;
	}

	static HashMap<String, ArrayList<EntityMention>> allSemanticResult = ChCommon.loadSemanticResult();

	protected static ArrayList<String> addOtherPredicates(CoNLLPart part, ArrayList<EntityMention> allMentions) {
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<Entity> entities = part.getChains();
		// lines.add(">nCoref_");
		// for (Entity entity : entities) {
		// for (int i = 0; i < entity.mentions.size(); i++) {
		// int first = MLNUtil.getIndex(entity.mentions.get(i));
		// for (int j = i - 1; j >= 0; j--) {
		// int second = MLNUtil.getIndex(entity.mentions.get(j));
		// lines.add(first + " " + second);
		// lines.add(second + " " + first);
		// }
		// lines.add(first + " " + first);
		// }
		// }

		lines.add(">singlePerArg_");
		for (EntityMention mention : allMentions) {
			if (mention instanceof EventMention) {
				EventMention em = (EventMention) mention;
				for (EventMentionArgument arg : em.eventMentionArguments) {
					if (em.argHash.get(arg.role).size() == 1 && arg.mention.semClass.equalsIgnoreCase("per")) {
						int first = MLNUtil.getIndex(em);
						int second = MLNUtil.getIndex(arg.mention);
						lines.add(first + " " + second + " \"" + arg.role + "\"");
					}
				}
			}
		}

		// lines.add(">singleValueArg_");
		// for (EntityMention mention : allMentions) {
		// if (mention instanceof EventMention) {
		// EventMention em = (EventMention) mention;
		// for (EventMentionArgument arg : em.eventMentionArguments) {
		// if (em.argHash.get(arg.role).size() == 1 &&
		// arg.mention.semClass.equalsIgnoreCase("value")) {
		// int first = MLNUtil.getIndex(em);
		// int second = MLNUtil.getIndex(arg.mention);
		// lines.add(first + " " + second + " \"" + arg.role + "\"");
		// }
		// }
		// }
		// }

		lines.add(">hasArgument_");
		for (EntityMention mention : allMentions) {
			if (mention instanceof EventMention) {
				EventMention em = (EventMention) mention;
				for (EventMentionArgument arg : em.eventMentionArguments) {
					if (em.argHash.get(arg.role).size() == 1) {
						int first = MLNUtil.getIndex(em);
						int second = MLNUtil.getIndex(arg.mention);
						lines.add(first + " " + second + " \"" + arg.role + "\"");
					}
				}
			}
		}

		lines.add(">hasRole_");
		for (EntityMention mention : allMentions) {
			if (mention instanceof EventMention) {
				EventMention em = (EventMention) mention;
				HashSet<String> roles = new HashSet<String>();
				for (EventMentionArgument arg : em.eventMentionArguments) {
					roles.add(arg.role);
				}
				for (String role : roles) {
					int first = MLNUtil.getIndex(em);
					lines.add(first + " " + " \"" + role + "\"");
				}
			}
		}

		lines.add(">event_");
		for (EntityMention mention : allMentions) {
			if (mention instanceof EventMention) {
				int first = MLNUtil.getIndex(mention);
				lines.add(Integer.toString(first));
			}
		}

		lines.add(">semType_");
		for (EntityMention mention : allMentions) {
			if (!(mention instanceof EventMention)) {
				int first = MLNUtil.getIndex(mention);
				lines.add(Integer.toString(first) + " \"" + mention.semClass + "\"");
			}
		}

		lines.add(">semSubType_");
		for (EntityMention mention : allMentions) {
			if (!(mention instanceof EventMention)) {
				int first = MLNUtil.getIndex(mention);
				lines.add(Integer.toString(first) + " \"" + mention.subType + "\"");
			}
		}
		return lines;
	}

	private static ArrayList<EntityMention> getSieveCorefMentions(CoNLLPart part) {
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/ACE_test/";
		PlainText sgm = ACECommon.getPlainText(Common.changeSurffix(part.getDocument().getFilePath(), "sgm"));
		ArrayList<String> lines = Common.getLines(baseFolder + part.getDocument().getDocumentID()
				+ ".entities.sieve.entity");
		ArrayList<EntityMention> allMentions = new ArrayList<EntityMention>();

		ArrayList<Entity> entities = new ArrayList<Entity>();

		for (String line : lines) {
			Entity entity = new Entity();
			String tokens[] = line.split("\\s+");
			for (String token : tokens) {
				String pos[] = token.split(",");
				EntityMention mention = new EntityMention();
				int charStart = Integer.valueOf(Integer.valueOf(pos[0]));
				int charEnd = Integer.valueOf(Integer.valueOf(pos[1]));
				mention.headCharStart = charStart;
				mention.headCharEnd = charEnd;
				mention.entity = entity;
				mention.head = sgm.content.substring(mention.headCharStart, mention.headCharEnd + 1);
				allMentions.add(mention);
			}
			entities.add(entity);
		}
		part.setChains(entities);
		// sign start, end
		for (EntityMention mention : allMentions) {
			ACECorefCommon.assingStartEnd(mention, part);
		}
		// assign semantic
		ArrayList<EntityMention> semanticM = allSemanticResult.get(part.getDocument().getFilePath());
		for (EntityMention em : allMentions) {
			for (EntityMention sm : semanticM) {
				if (em.headCharStart == sm.headStart && em.headCharEnd == sm.headEnd) {
					em.semClass = sm.semClass;
					em.subType = sm.subType;
					break;
				}
			}

			int start = em.start;
			int end = em.end;
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			em.source = sb.toString().trim().toLowerCase().replaceAll("\\s+", "");
			em.original = sb2.toString().trim().replaceAll("\\s+", "");
			em.head = em.head.replaceAll("\\s+", "");

			RuleCoref.ontoCommon.calACEAttribute(em, part);
			em.extent = em.head;
		}

		return allMentions;
	}

	private static ArrayList<Entity> clusterTime(ArrayList<EntityMention> mentions) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		for (EntityMention mention : mentions) {
			String head = mention.head;
			boolean coref = false;
			loop: for (Entity entity : entities) {
				for (EntityMention temp : entity.mentions) {
					if (temp.head.contains(head) || head.contains(temp.head)) {
						coref = true;
						entity.mentions.add(mention);
						break loop;
					}
				}
			}
			if (!coref) {
				Entity entity = new Entity();
				entity.addMention(mention);
				entities.add(entity);
			}
		}
		return entities;
	}

	private static ArrayList<Entity> clusterValue(ArrayList<EntityMention> mentions) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		for (EntityMention mention : mentions) {
			String head = mention.head;
			boolean coref = false;
			loop: for (Entity entity : entities) {
				for (EntityMention temp : entity.mentions) {
					if (temp.head.contains(head) || head.contains(temp.head)) {
						coref = true;
						entity.mentions.add(mention);
						break loop;
					}
				}
			}
			if (!coref) {
				Entity entity = new Entity();
				entity.addMention(mention);
				entities.add(entity);
			}
		}
		return entities;
	}

	private static ArrayList<EntityMention> getSystemTestEventMentions(CoNLLPart part) {
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = getSieveCorefMentions(part);

		// normal mentions
		argumentCandidate.addAll(mentions);
		// time mentions
		ArrayList<EntityMention> timeMentions = ACECommon.getTimeExpression(part);
		ArrayList<Entity> timeEntities = clusterTime(timeMentions);
		part.getChains().addAll(timeEntities);
		argumentCandidate.addAll(timeMentions);
		mentions.addAll(timeMentions);
		// value mentions
		ArrayList<EntityMention> valueMentions = ACECommon.getValueExpression(part);
		ArrayList<Entity> valueEntities = clusterValue(valueMentions);
		part.getChains().addAll(valueEntities);
		argumentCandidate.addAll(valueMentions);
		mentions.addAll(valueMentions);

		for (EntityMention mention : argumentCandidate) {
			ACECorefCommon.assingStartEnd(mention, part);
		}

		// event mentions
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>(ACECommon.getSystemEventMentions().get(
				part.getDocument().getFilePath()).values());
		if (allEvents != null) {
			for (EventMention eventMention : allEvents) {
				// assign head
				ACECorefCommon.assingStartEnd(eventMention, part);
				for (EventMentionArgument arg : eventMention.eventMentionArguments) {
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}
				mentions.add(eventMention);
				calAttribute(eventMention, part);
			}
			Collections.sort(allEvents);
			for (int i = 0; i < allEvents.size(); i++) {
				EventMention eventMention = allEvents.get(i);
				eventMention.sequence = i;
			}
		}
		return mentions;
	}

	private static ArrayList<EntityMention> getGoldTestEventMentions(CoNLLPart part) {
		// entity mentions
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		// gold mentions
		ArrayList<Entity> goldEntities = part.getChains();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		for (int k = 0; k < goldEntities.size(); k++) {
			Entity entity = goldEntities.get(k);
			for (EntityMention mention : entity.mentions) {
				mention.goldChainID = k;
				mention.entity = entity;
			}
			mentions.addAll(entity.mentions);
		}

		argumentCandidate.addAll(mentions);
		// time mentions
		ArrayList<EntityMention> timeMentions = ACECommon.getTimeMentions(part.getDocument().getFilePath());
		ArrayList<Entity> timeEntities = clusterTime(timeMentions);
		part.getChains().addAll(timeEntities);
		argumentCandidate.addAll(timeMentions);
		mentions.addAll(timeMentions);

		// value mentions
		ArrayList<EntityMention> valueMentions = ACECommon.getValueMentions(part.getDocument().getFilePath());
		ArrayList<Entity> valueEntities = clusterValue(valueMentions);
		part.getChains().addAll(valueEntities);
		argumentCandidate.addAll(valueMentions);
		mentions.addAll(valueMentions);

		// event mentions
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>();
		ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
		for (int k = 0; k < eventChains.size(); k++) {
			EventChain chain = eventChains.get(k);
			for (EventMention eventMention : chain.getEventMentions()) {
				// assign head
				ACECorefCommon.assingStartEnd(eventMention, part);
				eventMention.goldChainID = k;
				for (EventMentionArgument arg : eventMention.eventMentionArguments) {
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}
				calAttribute(eventMention, part);
				mentions.add(eventMention);
				allEvents.add(eventMention);
			}
		}
		Collections.sort(allEvents);
		for (int i = 0; i < allEvents.size(); i++) {
			EventMention eventMention = allEvents.get(i);
			eventMention.sequence = i;
		}
		return mentions;
	}

	private static EntityMention findMention(EventMentionArgument arg, ArrayList<EntityMention> mentions) {
		for (EntityMention mention : mentions) {
			if (goldMentions) {
				if (mention.extentCharStart == arg.getStart() && mention.extendCharEnd == arg.getEnd()) {
					return mention;
				}
			} else {
				if (mention.headCharStart == arg.getStart() && mention.headCharEnd == arg.getEnd()) {
					return mention;
				}
			}

		}
		System.err.println(arg.getExtent());
		System.out.println(arg.getStart() + "#" + arg.getEnd());
		System.err.println("DID NOT FIND????");
		System.exit(1);
		return null;
	}

}
