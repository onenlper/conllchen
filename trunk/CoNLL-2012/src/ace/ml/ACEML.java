package ace.ml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mentionDetect.MentionDetect;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.reader.ACEReader;

public abstract class ACEML {

	String outputFolder;

	ArrayList<String> fileList;

	ArrayList<CoNLLPart> parts;

	protected MentionDetect md;

	CorefFeature corefFeature;

	boolean train;

	String language;

	String folder;

	public ChCommon chCommon;

	public void config(MentionDetect md, CorefFeature corefFeature) {
		this.md = md;
		this.corefFeature = corefFeature;
	}

	public ACEML(String args[]) {
		String tokens[] = args[0].split("_");
		this.fileList = ACECommon.getFileList(tokens);
		this.chCommon = new ChCommon("chinese");
		if (args[1].equalsIgnoreCase("train")) {
			this.train = true;
		} else {
			this.train = false;
		}
		this.outputFolder = "/users/yzcchen/chen3/conll12/chinese/ACE_" + args[1] + "/";
		if (!new File(this.outputFolder).exists()) {
			new File(this.outputFolder).mkdir();
		}

		this.parts = new ArrayList<CoNLLPart>();
		for (int i = 0; i < fileList.size(); i++) {
			String filename = fileList.get(i);
			CoNLLDocument document = ACEReader.read(filename, train);
			document.setDocumentID(Integer.toString(i));
			parts.addAll(document.getParts());
		}
	}

	protected void createAllFile() {
		ArrayList<String> all = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all.add(this.outputFolder + part.getDocument().getDocumentID().replace("/", "-"));
		}
		Common.outputLines(all, this.outputFolder + "all.txt");
		ArrayList<String> all2 = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all2.add(part.getDocument().getFilePath());
		}
		Common.outputLines(all2, this.outputFolder + "all.txt2");
	}

	public void creatAltafFormat() {
		String name = "ACE";
		this.corefFeature.init(train, name);
		this.createAllFile();
		for (CoNLLPart part : this.parts) {
			this.corefFeature.setPart(part);
			this.corefFeature.setSentences(part.getCoNLLSentences());
			System.out.println(part.getPartName());
			ArrayList<ArrayList<EntityMention>> chains = this.getMentions(part);
			ArrayList<EntityMention> mentions = this.createNPSpan(part, chains);
			this.assignFeature(mentions, part);
			ArrayList<EntityMention[]> pairs = this.createMentionPair(mentions);
			this.createFeature(part, pairs);
		}
		if (train) {
			Common.outputHashMap(this.corefFeature.stringFea1, name + "_stringFea1");
			// Common.outputHashMap(this.corefFeature.stringFeaCount, name +
			// "_stringFeaCount");
			Common.outputHashMap(this.corefFeature.commonBV, "ACE_CommonBV");
			Common.outputHashMap(this.corefFeature.commonPair, "ACE_CommonPair");
		}
		System.out.println(poses);
		System.err.println(ChCorefRuleFeature.argNo);
		System.err.println("Entity: " + entityMentionNo);
		System.err.println("Event: " + eventMentionNo);
	}

	static int eventMentionNo = 0;
	static int entityMentionNo = 0;

	protected void assignFeature(ArrayList<EntityMention> mentions, CoNLLPart part) {
		Collections.sort(mentions);
		for (EntityMention mention : mentions) {
			this.chCommon.calACEAttribute(mention, part);
		}
	}

	protected void createFeature(CoNLLPart part, ArrayList<EntityMention[]> pairs) {
		ArrayList<String> lines = new ArrayList<String>();
		boolean firstLone = false;
		boolean firstBi = false;
		lines.add("3 3 3 4 5 4 5 3 3 5 3");
		lines.add("2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 4 4");
		for (EntityMention[] pair : pairs) {
			EntityMention current = pair[0];
			EntityMention candidate = pair[1];
			StringBuilder sb = new StringBuilder();
			List<Feature> feas;
			if (current.equals(candidate)) {
				feas = this.corefFeature.getLoneFeature(train, current);
				if (firstLone) {
					StringBuilder s = new StringBuilder();
					for (Feature fea : feas) {
						if (fea.limit != -1) {
							s.append(fea.limit).append(" ");
						}
					}
					lines.add(0, s.toString());
					firstLone = false;
				}
			} else {
				feas = this.corefFeature.getBilateralFea(train, pair);
				if (firstBi) {
					StringBuilder s = new StringBuilder();
					for (Feature fea : feas) {
						if (fea.limit != -1) {
							s.append(fea.limit).append(" ");
						}
					}
					if (firstLone) {
						lines.add(0, s.toString());
					} else {
						lines.add(1, s.toString());
					}
					firstBi = false;
				}
			}
			sb.append(current.headCharStart).append(",").append(current.headCharEnd).append(",").append(
					candidate.headCharStart).append(",").append(candidate.headCharEnd).append(" ");
			for (Feature fea : feas) {
				sb.append(fea.index).append(",");
			}
			lines.add(sb.toString());
		}
		Common.outputLines(lines, this.outputFolder + part.getPartName() + ".feat");
		System.out.println(this.outputFolder + part.getPartName() + ".feat");
	}

	protected ArrayList<EntityMention[]> createMentionPair(ArrayList<EntityMention> mentions) {
		Collections.sort(mentions);
		ArrayList<EntityMention[]> pairs = new ArrayList<EntityMention[]>();

		for (int i = 0; i < mentions.size(); i++) {
			EntityMention currentMention = mentions.get(i);
			for (int j = i; j >= 0; j--) {
				EntityMention candidateMention = mentions.get(j);
				EntityMention[] mentionPair = new EntityMention[2];
				mentionPair[0] = currentMention;
				mentionPair[1] = candidateMention;
				pairs.add(mentionPair);
			}
		}

		return pairs;
	}

	protected ArrayList<EntityMention> createNPSpan(CoNLLPart part, ArrayList<ArrayList<EntityMention>> chains) {
		ArrayList<String> eventLines = new ArrayList<String>();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<String> lines = new ArrayList<String>();
		for (ArrayList<EntityMention> chain : chains) {
			StringBuilder sb = new StringBuilder();
			for (EntityMention mention : chain) {
				sb.append(mention.headCharStart).append(",").append(mention.headCharEnd).append(" ");

				if (mention instanceof EventMention) {
					eventLines.add(mention.headCharStart + "," + mention.headCharEnd);
				}

			}
			lines.add(sb.toString().trim());
			mentions.addAll(chain);
		}
		Collections.sort(mentions);
		Common.outputLines(lines, this.outputFolder + part.getPartName() + ".npspan");
		Common.outputLines(eventLines, this.outputFolder + part.getPartName() + ".eventLines");
		return mentions;
	}

	ArrayList<Entity> goldChain;

	public void loadGoldMaps() {
		for (Entity entity : this.goldChain) {
			for (EntityMention em : entity.mentions) {
				HashSet<EntityMention> ems = new HashSet<EntityMention>();
				for (EntityMention em2 : entity.mentions) {
					if (!em.equals(em2)) {
						ems.add(em2);
					}
				}
				goldMaps.put(em, ems);
			}
		}
	}

	static boolean goldMentions = false;

	public HashMap<EntityMention, HashSet<EntityMention>> goldMaps = new HashMap<EntityMention, HashSet<EntityMention>>();

	private EntityMention findMention(EventMentionArgument arg, ArrayList<EntityMention> mentions) {
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

	public ArrayList<ArrayList<EntityMention>> getMentions(CoNLLPart part) {
		this.goldChain = part.getChains();
		loadGoldMaps();
		if (train) {
			// entity mentions
			ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

			ArrayList<ArrayList<EntityMention>> chains = new ArrayList<ArrayList<EntityMention>>();
			HashSet<EntityMention> entityMentions = new HashSet<EntityMention>();
			ArrayList<Entity> entities = part.getChains();
			for (int k = 0; k < entities.size(); k++) {
				Entity entity = entities.get(k);
				ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
				chain.addAll(entity.mentions);
				entityMentions.addAll(chain);
				chains.add(chain);
				for (EntityMention mention : entity.mentions) {
					mention.goldChainID = k;
				}
				argumentCandidate.addAll(entity.mentions);
			}

			// time mentions
			argumentCandidate.addAll(ACECommon.getTimeMentions(part.getDocument().getFilePath()));
			// value mentions
			argumentCandidate.addAll(ACECommon.getValueMentions(part.getDocument().getFilePath()));

			// event mentions
			ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
			ArrayList<EventMention> allEvents = new ArrayList<EventMention>();
			System.err.println(part.getDocument().getFilePath());
			for (int k = 0; k < eventChains.size(); k++) {
				EventChain chain = eventChains.get(k);
				ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
				for (EventMention eventMention : chain.getEventMentions()) {
					eventMention.goldChainID = k;
					// assign head
					ACECorefCommon.assingStartEnd(eventMention, part);
					for (EventMentionArgument arg : eventMention.eventMentionArguments) {
						arg.mention = findMention(arg, argumentCandidate);
						arg.mention.argument = arg;
					}
					allEvents.add(eventMention);
					mentions.add(eventMention);
				}
				chains.add(mentions);
			}
			Collections.sort(allEvents);
			for (int i = 0; i < allEvents.size(); i++) {
				EventMention eventMention = allEvents.get(i);
				eventMention.sequence = i;
				calEventFeature(eventMention, part);
			}
			return chains;
		} else {
			if (goldMentions) {
				return getGoldTestEventMentions(part);
			} else {
				return getSystemTestEventMentions(part);
			}
		}
	}

	private ArrayList<ArrayList<EntityMention>> getSystemTestEventMentions(CoNLLPart part) {
		// system mentions
		// entity mentions
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		ArrayList<ArrayList<EntityMention>> chains = new ArrayList<ArrayList<EntityMention>>();

		ArrayList<EntityMention> mentions = this.md.getMentions(part);

		for (EntityMention mention : mentions) {
			entityMentionNo++;
			ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
			chain.add(mention);
			chains.add(chain);
		}
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
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>(ACECommon.getSystemEventMentions().get(
				part.getDocument().getFilePath()).values());
		if (allEvents != null) {
			for (EventMention eventMention : allEvents) {
				// assign head
				ACEML.eventMentionNo++;
				ACECorefCommon.assingStartEnd(eventMention, part);
				for (EventMentionArgument arg : eventMention.eventMentionArguments) {
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}
				ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
				chain.add(eventMention);
				chains.add(chain);
			}
			Collections.sort(allEvents);
			for (int i = 0; i < allEvents.size(); i++) {
				EventMention eventMention = allEvents.get(i);
				eventMention.sequence = i;
				calEventFeature(eventMention, part);
			}
		}
		return chains;
	}

	private ArrayList<ArrayList<EntityMention>> getGoldTestEventMentions(CoNLLPart part) {
		// entity mentions
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();

		ArrayList<ArrayList<EntityMention>> chains = new ArrayList<ArrayList<EntityMention>>();

		// ArrayList<EntityMention> mentions = this.md.getMentions(part);
		// gold mentions
		ArrayList<Entity> goldEntities = part.getChains();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		for (int k = 0; k < goldEntities.size(); k++) {
			Entity entity = goldEntities.get(k);
			mentions.addAll(entity.mentions);
			for (EntityMention mention : entity.mentions) {
				mention.goldChainID = k;
			}
		}

		for (EntityMention mention : mentions) {
			ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
			chain.add(mention);
			chains.add(chain);
			argumentCandidate.add(mention);
		}

		// time mentions
		argumentCandidate.addAll(ACECommon.getTimeMentions(part.getDocument().getFilePath()));
		// value mentions
		argumentCandidate.addAll(ACECommon.getValueMentions(part.getDocument().getFilePath()));

		// event mentions
		ArrayList<EventMention> allEvents = new ArrayList<EventMention>();
		ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
		for (int k = 0; k < eventChains.size(); k++) {
			EventChain chain = eventChains.get(k);
			ArrayList<EntityMention> vMentions = new ArrayList<EntityMention>();
			for (EventMention eventMention : chain.getEventMentions()) {
				eventMention.goldChainID = k;
				// assign head
				ACECorefCommon.assingStartEnd(eventMention, part);

				for (EventMentionArgument arg : eventMention.eventMentionArguments) {
					arg.mention = findMention(arg, argumentCandidate);
					arg.mention.argument = arg;
				}

				vMentions.add(eventMention);
				allEvents.add(eventMention);
			}
			chains.add(vMentions);
		}
		Collections.sort(allEvents);
		for (int i = 0; i < allEvents.size(); i++) {
			EventMention eventMention = allEvents.get(i);
			eventMention.sequence = i;
			calEventFeature(eventMention, part);
		}
		return chains;
	}

	static HashSet<String> poses = new HashSet<String>();

	public void calEventFeature(EventMention mention, CoNLLPart part) {
		mention.posTag = part.getWord(mention.headEnd).posTag;
		poses.add(mention.posTag);
	}

	public static void main(String args[]) {

	}
}
