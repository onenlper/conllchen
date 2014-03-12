package ace.rule;

import java.util.ArrayList;
import java.util.Collections;

import mentionDetect.MentionDetect;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common.Numb;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.CRFMention;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class RuleCorefUtil {

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

	static boolean goldMentions = true;

	public static ArrayList<EntityMention> getEntityEventMentions(CoNLLPart part, boolean gold) {
		goldMentions = gold;
		if (gold) {
			return getGoldTestEventMentions(part);
		} else {
			return getSystemTestEventMentions(part);
		}
	}

	private static ArrayList<EntityMention> getSystemTestEventMentions(CoNLLPart part) {
		// system mentions
		// entity mentions
		MentionDetect md = new CRFMention();
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = md.getMentions(part);

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
			allEvents = new ArrayList<EventMention>(ACECommon.getSystemEventMentions().get(
					part.getDocument().getFilePath()).values());
		}
		
		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);

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
			}
			mentions.addAll(entity.mentions);
		}

		argumentCandidate.addAll(mentions);
		// time mentions
		argumentCandidate.addAll(ACECommon.getTimeMentions(part.getDocument().getFilePath()));
		// value mentions
		argumentCandidate.addAll(ACECommon.getValueMentions(part.getDocument().getFilePath()));

		for (EntityMention mention : argumentCandidate) {
			ACECorefCommon.assingStartEnd(mention, part);
		}

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
				ACECommon.identBVs(eventMention, part);
			}
		}
		Collections.sort(allEvents);
		// assign semantic roles;
		ACECommon.assignSemanticRole(allEvents, argumentCandidate, part.semanticRoles);
		for (int i = 0; i < allEvents.size(); i++) {
			EventMention eventMention = allEvents.get(i);
			eventMention.sequence = i;
			ChCommon.calEventFeature(eventMention, part, argumentCandidate);
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
