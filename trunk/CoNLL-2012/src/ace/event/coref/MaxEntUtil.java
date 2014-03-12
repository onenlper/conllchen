package ace.event.coref;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;
import util.Common.Numb;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.CRFMention;
import ace.PlainText;
import ace.ml.Feature;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.rule.RuleCoref;

public class MaxEntUtil {

	static boolean attriC = true;
	static boolean corefC = true;
	static boolean typeC = false;
	static boolean argC = true;
	
	public static HashMap<EntityMention, Integer> mentionIndexes = new HashMap<EntityMention, Integer>();

	public static boolean train = false;

	public static int getIndex(EntityMention em) {
		if (mentionIndexes.containsKey(em)) {
			return mentionIndexes.get(em);
		} else {
			mentionIndexes.put(em, mentionIndexes.size());
			return mentionIndexes.size() - 1;
		}
	}

	public static HashMap<String, Integer> stringFeas;

	public static int getStringFea(String str) {
		int k = 0;
		if (stringFeas.containsKey(str)) {
			k = stringFeas.get(str);
		} else if (train) {
			k = stringFeas.size() + 1;
			stringFeas.put(str, k);
		} else {
			return -1;
		}
		return k;
	}

	@SuppressWarnings("unchecked")
	public static String createInstance(EntityMention em2, EntityMention ant2, CoNLLPart part, int coref)
			throws Exception {
		ArrayList<Feature> features = new ArrayList<Feature>();
		HashSet<Integer> ngramFea = new HashSet<Integer>();
		EventMention em = (EventMention) em2;
		EventMention ant = (EventMention) ant2;
		FeasTune feas = new FeasTune();
		Method methods[] = FeasTune.class.getMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("_attriC") && !attriC) {
				continue;
			}
			if (method.getName().startsWith("_corefC") && !corefC) {
				continue;
			}
			if (method.getName().startsWith("_typeC") && !typeC) {
				continue;
			}
			if (method.getName().startsWith("_argC") && !argC) {
				continue;
			}
			// if ((feas.commonBV_(em, ant, part) || feas.triggerMatch_(ant, em,
			// part))
			// && method.getName().startsWith("conflict")) {
			// Object obj = method.invoke(feas, ant, em, part);
			// if (obj instanceof Boolean) {
			// if (((Boolean) obj).booleanValue()) {
			// int[] stat = MaxEntEventFeas.errors.get(method.getName());
			// if (stat == null) {
			// stat = new int[2];
			// MaxEntEventFeas.errors.put(method.getName(), stat);
			// }
			// EventMention gEM =
			// MaxEntTest.goldEventMentionMap.get(em.toString());
			// EventMention gAn =
			// MaxEntTest.goldEventMentionMap.get(ant.toString());
			// if (gEM != null && gAn != null && gEM.goldChainID ==
			// gAn.goldChainID) {
			// stat[0]++;
			// } else {
			// stat[1]++;
			// }
			// }
			// }
			// }

			if (method.getName().startsWith("_")) {
				Object obj = method.invoke(feas, ant, em, part);
				if (obj instanceof Boolean) {
					String str = obj.toString();
					int idx = getStringFea(method.getName() + "_" + str);
					if (idx != -1) {
						ngramFea.add(idx);
					}
				} else if (obj instanceof String) {
					String str = (String) obj;
					if (!str.isEmpty()) {
						int idx = getStringFea(method.getName() + "_" + str);
						if (idx != -1) {
							ngramFea.add(idx);
						}
					}
				} else if (obj instanceof int[]) {
					int[] f = (int[]) obj;
					features.add(new Feature(f[0], f[1]));
					String str = Integer.toString(f[0]);
					if (!str.isEmpty()) {
						int idx = getStringFea(method.getName() + "_" + str);
						if (idx != -1) {
							ngramFea.add(idx);
						}
					}
				} else if (obj instanceof ArrayList<?>) {
					ArrayList<String> strs = (ArrayList<String>) obj;
					for (String str : strs) {
						if (!str.isEmpty()) {
							int idx = getStringFea(method.getName() + "_" + str);
							if (idx != -1) {
								ngramFea.add(idx);
							}
						}
					}
				}
			}
		}
		int offset = 1;
		StringBuilder sb = new StringBuilder();
		sb.append(coref).append(" ");
		for (Feature fea : features) {
			sb.append(fea.index + offset).append(":1 ");
			offset += fea.limit;
		}
		ArrayList<Integer> ngramList = new ArrayList<Integer>();
		ngramList.addAll(ngramFea);
		Collections.sort(ngramList);
		for (Integer fea : ngramList) {
			sb.append(fea).append(":1 ");
		}
		return sb.toString();
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

	static boolean useExtentMatch = true;

	public static ArrayList<EntityMention> getEntityEventMentions(CoNLLPart part, boolean gold) {
		ArrayList<EntityMention> eventMentions = new ArrayList<EntityMention>();
		useExtentMatch = ACECommon.goldEntityMention || Common.train || (!Common.train && ACECommon.goldEntityCorefTest);
		ArrayList<EntityMention> mentions = null;
		if (gold) {
			mentions = getGoldTestEventMentions2(part);
		} else {
			mentions = getSystemTestEventMentions2(part);
		}
		ChCommon chCommon = new ChCommon("chinese");
		int sequence = 0;
		Collections.sort(mentions);
		for (EntityMention mention : mentions) {
			if (mention instanceof EventMention) {
				int idx = getIndex(mention);
				eventMentions.add(mention);
				continue;
			}
			mention.sequence = sequence++;
			ACECorefCommon.assingStartEnd(mention, part);
			chCommon.calACEAttribute(mention, part);
		}
		return eventMentions;
	}

	static HashMap<String, ArrayList<EntityMention>> allSemanticResult = ChCommon.loadSemanticResult();

	private static void assignSystemSemantic(EntityMention mention, String fileID) {
		ArrayList<EntityMention> systems = allSemanticResult.get(fileID);
		boolean find = false;
		for (EntityMention system : systems) {
			if (system.headCharStart == mention.headCharStart && system.headCharEnd == mention.headCharEnd) {
				mention.subType = system.subType;
				mention.semClass = system.semClass;
				find = true;
				break;
			}
		}
		if (!find) {
			System.err.println("GEE");
			Common.bangErrorPOS("");
			System.exit(1);
		}
	}

	private static void assignGoldSemantic(EntityMention mention, CoNLLPart part) {
		ArrayList<Entity> goldEntities = part.getChains();
		boolean find = false;
		for (Entity entity : goldEntities) {
			for (EntityMention gold : entity.mentions) {
				if (gold.headCharStart == mention.headCharStart && gold.headCharEnd == mention.headCharEnd) {
					mention.subType = gold.subType;
					mention.semClass = gold.semClass;
					find = true;
					break;
				}
			}
		}
		if (!find) {
			System.err.println("GEE");
//			Common.bangErrorPOS("");
//			System.exit(1);
		}
	}

	private static ArrayList<EntityMention> getSieveCorefMentions(CoNLLPart part) {
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/goldEntityMentions/ACE_test_" + Common.part + "/";
		if (!ACECommon.goldEntityMention) {
			baseFolder = "/users/yzcchen/chen3/conll12/chinese/systemEntityMentions/ACE_test_" + Common.part + "/";
		}
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

				String extendStr = part.headExtendMap.get(charStart + "," + charEnd);
//				mention.extentCharStart = Integer.parseInt(extendStr.split(",")[0]);
//				mention.extendCharEnd = Integer.parseInt(extendStr.split(",")[1]);
				if (!ACECommon.goldSemantic) {
					assignSystemSemantic(mention, part.getDocument().getFilePath());
				} else {
					assignGoldSemantic(mention, part);
				}

				allMentions.add(mention);
			}
			entities.add(entity);
		}
		part.setChains(entities);
		// sign start, end
		for (EntityMention mention : allMentions) {
			ACECorefCommon.assingStartEnd(mention, part);
		}

		for (EntityMention em : allMentions) {
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
		System.out.println(entities.size() + " ###");
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
				entity.type = "time";
				entity.subType = "time";
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
				entity.type = "value";
				entity.subType = "value";
				entity.addMention(mention);
				entities.add(entity);
			}
		}
		return entities;
	}

	private static ArrayList<EntityMention> getSystemTestEventMentions(CoNLLPart part) {
		// system mentions
		// entity mentions
		MentionDetect md = new CRFMention();
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = getSieveCorefMentions(part);

		// normal mentions
		argumentCandidate.addAll(mentions);
		// time mentions
		argumentCandidate.addAll(ACECommon.getTimeExpression(part));
		// value mentions
		argumentCandidate.addAll(ACECommon.getValueExpression(part));

		for (EntityMention mention : argumentCandidate) {
			ACECorefCommon.assingStartEnd(mention, part);
		}

		// event mentions
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>();
		if (ACECommon.getSystemEventMentions().containsKey(part.getDocument().getFilePath())) {
			allEvents.addAll(ACECommon.getSystemEventMentions().get(part.getDocument().getFilePath()).values());
		}

		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);

		if (allEvents != null) {
			for (EventMention eventMention : allEvents) {
				// assign head
				ACECommon.assignSystemAttribute(part.getDocument().getFilePath(), eventMention);

				ACECorefCommon.assingStartEnd(eventMention, part);
				for (EventMentionArgument arg : eventMention.eventMentionArguments) {
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}
				mentions.add(eventMention);
				calAttribute(eventMention, part);
				ACECommon.identBVs(eventMention, part);
			}
			Collections.sort(allEvents);
			for (int i = 0; i < allEvents.size(); i++) {
				EventMention eventMention = allEvents.get(i);
				eventMention.sequence = i;
				ChCommon.calEventFeature(eventMention, part, argumentCandidate);
			}
		}
		return mentions;
	}

	private static ArrayList<EntityMention> getSystemTestEventMentions2(CoNLLPart part) {
		// system mentions
		// entity mentions
		MentionDetect md = new CRFMention();

		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> timeMentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> valueMentions = new ArrayList<EntityMention>();
		
		HashSet<EntityMention> systemMentions = new HashSet<EntityMention>();
		HashSet<EntityMention> goldeMentions = new HashSet<EntityMention>();
//		if (ACECommon.goldEntityCorefTest) {
			ArrayList<Entity> goldEntities = part.getChains();
			for (int k = 0; k < goldEntities.size(); k++) {
				Entity entity = goldEntities.get(k);
				for (EntityMention mention : entity.mentions) {
					mention.goldChainID = k;
					mention.entity = entity;
				}
				goldeMentions.addAll(entity.mentions);
			}
//		} else {
			systemMentions.addAll(getSieveCorefMentions(part));
//		}
		
		for(EntityMention m : goldeMentions) {
			if(systemMentions.contains(m)) {
				argumentCandidate.add(m);
			}
		}
		if (ACECommon.goldEntityMention) {
			// time mentions
			timeMentions = ACECommon.getTimeMentions(part.getDocument().getFilePath());
			// value mentions
			valueMentions = ACECommon.getValueMentions(part.getDocument().getFilePath());
			ArrayList<Entity> timeEntities = clusterTime(timeMentions);
			part.getChains().addAll(timeEntities);
			argumentCandidate.addAll(timeMentions);

			ArrayList<Entity> valueEntities = clusterValue(valueMentions);
			part.getChains().addAll(valueEntities);
			argumentCandidate.addAll(valueMentions);
		} else {
			// normal mentions
			argumentCandidate.addAll(mentions);
			// time mentions
			argumentCandidate.addAll(ACECommon.getTimeExpression(part));
			// value mentions
			argumentCandidate.addAll(ACECommon.getValueExpression(part));
		}

		for (EntityMention mention : argumentCandidate) {
			ACECorefCommon.assingStartEnd(mention, part);
		}

		// event mentions

		ArrayList<EventMention> allEvents = new ArrayList<EventMention>();

		HashSet<EventMention> goldEvents = new HashSet<EventMention>();
		
		HashSet<EventMention> systemEvents = new HashSet<EventMention>();
		
//		if (ACECommon.goldEventMention) {
			ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
			for (int k = 0; k < eventChains.size(); k++) {
				EventChain chain = eventChains.get(k);
				for (EventMention eventMention : chain.getEventMentions()) {
					eventMention.goldChainID = k;
//					allEvents.add(eventMention);
					goldEvents.add(eventMention);
				}
			}
//		} else {
			if (ACECommon.getSystemEventMentions().containsKey(part.getDocument().getFilePath())) {
//				allEvents.addAll(ACECommon.getSystemEventMentions().get(part.getDocument().getFilePath()).values());
				systemEvents.addAll(ACECommon.getSystemEventMentions().get(part.getDocument().getFilePath()).values());
			}
//		}

		if(!ACECommon.goldEventMention) {
			for(EventMention gold : goldEvents) {
				if(systemEvents.contains(gold)) {
					// add correct
					allEvents.add(gold);
				}
			}
			for(EventMention system : systemEvents) {
				if(!goldEvents.contains(system)) {
					// add wrong
					allEvents.add(system);
				}
			}
		} else {
			allEvents.addAll(goldEvents);
		}
		
		
		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);

		if (allEvents != null) {
			for (EventMention eventMention : allEvents) {
				// assign head
				if (!ACECommon.goldAttributeTest || (!ACECommon.goldEventMention && !goldEvents.contains(eventMention)) ) {
					ACECommon.assignSystemAttribute(part.getDocument().getFilePath(), eventMention);
				}
				if (!ACECommon.goldEventArgument || (!ACECommon.goldEventMention && !goldEvents.contains(eventMention)) ) {
					ACECommon.assginSystemArguments(part.getDocument().getFilePath(), eventMention);
				}
				ACECorefCommon.assingStartEnd(eventMention, part);
				for (int i=0;i<eventMention.eventMentionArguments.size();i++) {
					EventMentionArgument arg = eventMention.eventMentionArguments.get(i);
					EntityMention m = findMention(arg, argumentCandidate);
					if(m==null) {
						eventMention.eventMentionArguments.remove(i);
						i--;
						continue;
					}
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}
				mentions.add(eventMention);
				calAttribute(eventMention, part);
				ACECommon.identBVs(eventMention, part);
			}
			Collections.sort(allEvents);
			for (int i = 0; i < allEvents.size(); i++) {
				EventMention eventMention = allEvents.get(i);
				eventMention.sequence = i;
				ChCommon.calEventFeature(eventMention, part, argumentCandidate);
			}
		}
		return mentions;
	}

	private static ArrayList<EntityMention> getGoldTestEventMentions2(CoNLLPart part) {
		// entity mentions
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		// gold mentions
		// regular mentions
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> timeMentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> valueMentions = new ArrayList<EntityMention>();

		// ArrayList<Entity> goldEntities = part.getChains();
		// for (int k = 0; k < goldEntities.size(); k++) {
		// Entity entity = goldEntities.get(k);
		// for (EntityMention mention : entity.mentions) {
		// mention.goldChainID = k;
		// mention.entity = entity;
		// }
		// mentions.addAll(entity.mentions);
		// }

		if (ACECommon.goldEntityCorefTrain) {
			ArrayList<Entity> goldEntities = part.getChains();
			for (int k = 0; k < goldEntities.size(); k++) {
				Entity entity = goldEntities.get(k);
				for (EntityMention mention : entity.mentions) {
					mention.goldChainID = k;
					mention.entity = entity;
				}
				mentions.addAll(entity.mentions);
			}
		} else {
			mentions = getSieveCorefMentions(part);
		}

		argumentCandidate.addAll(mentions);

		// time mentions
		timeMentions = ACECommon.getTimeMentions(part.getDocument().getFilePath());
		// value mentions
		valueMentions = ACECommon.getValueMentions(part.getDocument().getFilePath());
		ArrayList<Entity> timeEntities = clusterTime(timeMentions);
		part.getChains().addAll(timeEntities);
		argumentCandidate.addAll(timeMentions);
		mentions.addAll(timeMentions);

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
				eventMention.goldChainID = k;
				allEvents.add(eventMention);
			}
		}

		for (EventMention eventMention : allEvents) {
			// use system attribute(polarity...)
			if (!ACECommon.goldAttributeTrain) {
				ACECommon.assignSystemAttribute(part.getDocument().getFilePath(), eventMention);
			}
			// assign head
			ACECorefCommon.assingStartEnd(eventMention, part);
			for (EventMentionArgument arg : eventMention.eventMentionArguments) {
				arg.mention = findMention(arg, argumentCandidate);
				arg.mention.argument = arg;
			}
			calAttribute(eventMention, part);
			mentions.add(eventMention);
			ACECommon.identBVs(eventMention, part);
		}

		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);
		Collections.sort(allEvents);
		for (int i = 0; i < allEvents.size(); i++) {
			EventMention eventMention = allEvents.get(i);
			eventMention.sequence = i;
			ChCommon.calEventFeature(eventMention, part, argumentCandidate);
		}
		return mentions;
	}

	private static ArrayList<EntityMention> getGoldTestEventMentions(CoNLLPart part) {
		// entity mentions
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		// gold mentions
		// regular mentions
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> timeMentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> valueMentions = new ArrayList<EntityMention>();

		// if (Common.train) {
		ArrayList<Entity> goldEntities = part.getChains();
		for (int k = 0; k < goldEntities.size(); k++) {
			Entity entity = goldEntities.get(k);
			for (EntityMention mention : entity.mentions) {
				mention.goldChainID = k;
				mention.entity = entity;
			}
			mentions.addAll(entity.mentions);
		}
		// } else {
		// mentions = getSieveCorefMentions(part);
		// }
		argumentCandidate.addAll(mentions);

		// time mentions
		timeMentions = ACECommon.getTimeMentions(part.getDocument().getFilePath());
		// value mentions
		valueMentions = ACECommon.getValueMentions(part.getDocument().getFilePath());
		ArrayList<Entity> timeEntities = clusterTime(timeMentions);
		part.getChains().addAll(timeEntities);
		argumentCandidate.addAll(timeMentions);
		mentions.addAll(timeMentions);

		ArrayList<Entity> valueEntities = clusterValue(valueMentions);
		part.getChains().addAll(valueEntities);
		argumentCandidate.addAll(valueMentions);
		mentions.addAll(valueMentions);

		// event mentions
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>();

		// if (Common.train) {
		ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
		for (int k = 0; k < eventChains.size(); k++) {
			EventChain chain = eventChains.get(k);
			for (EventMention eventMention : chain.getEventMentions()) {
				eventMention.goldChainID = k;
				allEvents.add(eventMention);
			}
		}
		// } else {
		// // TODO
		// if
		// (ACECommon.getSystemEventMentions().containsKey(part.getDocument().getFilePath()))
		// {
		// allEvents = new
		// ArrayList<EventMention>(ACECommon.getSystemEventMentions().get(
		// part.getDocument().getFilePath()).values());
		// }
		// }

		for (EventMention eventMention : allEvents) {
			// use system attribute(polarity...)
			ACECommon.assignSystemAttribute(part.getDocument().getFilePath(), eventMention);

			if (!Common.train) {
				ACECommon.assginSystemArguments(part.getDocument().getFilePath(), eventMention);
			}

			// assign head
			ACECorefCommon.assingStartEnd(eventMention, part);
			for (EventMentionArgument arg : eventMention.eventMentionArguments) {
				arg.mention = findMention(arg, argumentCandidate);
				arg.mention.argument = arg;
			}
			calAttribute(eventMention, part);
			mentions.add(eventMention);
			ACECommon.identBVs(eventMention, part);
		}

		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);
		Collections.sort(allEvents);
		for (int i = 0; i < allEvents.size(); i++) {
			EventMention eventMention = allEvents.get(i);
			eventMention.sequence = i;
			ChCommon.calEventFeature(eventMention, part, argumentCandidate);
		}
		return mentions;
	}

	private static EntityMention findMention(EventMentionArgument arg, ArrayList<EntityMention> mentions) {
		for (EntityMention mention : mentions) {
			if (useExtentMatch) {
				if (mention.extentCharStart == arg.getStart() && mention.extendCharEnd == arg.getEnd()) {
					return mention;
				}
			} else {
				if (mention.headCharStart == arg.getStart() && mention.headCharEnd == arg.getEnd()) {
					return mention;
				}
			}
		}
//		System.err.println(arg.getExtent());
//		System.out.println(arg.getStart() + "#" + arg.getEnd() + arg.role);
//		System.err.println("DID NOT FIND????");
//		Exception e = new Exception();
//		e.printStackTrace();
//		System.exit(1);
		return null;
	}
}
