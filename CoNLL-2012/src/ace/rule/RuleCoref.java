package ace.rule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common;
import util.Common.Person;
import ace.ACECommon;
import ace.CRFMention;
import ace.GoldACEMention;
import ace.event.coref.MaxEntEventFeas;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.reader.ACEReader;

/*
 * head==extend
 * modifier==depends
 * word inclusion==maxnp or qp node
 */
public class RuleCoref {

	public RuleCoref() {

	}

	CoNLLPart part;

	public ArrayList<EntityMention> mentions;

	ArrayList<Entity> systemChain;

	ArrayList<CoNLLSentence> sentences;

	String language;

	String folder;

	public static ChCommon ontoCommon = new ChCommon("chinese");

	HashSet<EntityMention> goldMentions = new HashSet<EntityMention>();

	ArrayList<String> eventLines = new ArrayList<String>();
	ArrayList<String> entityLines = new ArrayList<String>();

	public static HashMap<String, EventMention> goldEventMentionMap = new HashMap<String, EventMention>();

	public RuleCoref(CoNLLPart part) {
		appoPairs.clear();
		this.part = part;
		MentionDetect md = new CRFMention();
		MentionDetect md2 = new GoldACEMention();
		this.goldMentions.addAll(md2.getMentions(part));
		goldEventMentionMap.clear();
		ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(part.getDocument().getFilePath());
		for (int k = 0; k < eventChains.size(); k++) {
			for (EventMention eventMention : eventChains.get(k).getEventMentions()) {
				eventMention.goldChainID = k;
				goldEventMentionMap.put(eventMention.toString(), eventMention);
			}
		}

		// this.mentions = md.getMentions(part);

		this.mentions = RuleCorefUtil.getEntityEventMentions(part, false);
		for (EntityMention mention : mentions) {
			if (mention instanceof EventMention) {
				this.eventLines.add(mention.headCharStart + "," + mention.headCharEnd);
			} else {
				this.entityLines.add(mention.headCharStart + "," + mention.headCharEnd);
			}
		}
		// eventMentions

		Collections.sort(mentions);
		this.systemChain = new ArrayList<Entity>();
		this.sentences = part.getCoNLLSentences();
		int entityIdx = 0;
		for (EntityMention em : mentions) {
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
			Entity entity = new Entity();
			entity.entityIdx = entityIdx;
			em.entityIndex = entityIdx;
//			em.entity = entity;
			ontoCommon.calACEAttribute(em, part);
			em.extent = em.head;
			int sentenceId = em.sentenceID;
			if (sentenceMentions.containsKey(sentenceId)) {
				sentenceMentions.get(sentenceId).add(em);
			} else {
				ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
				ems.add(em);
				sentenceMentions.put(sentenceId, ems);
			}
			entity.addMention(em);
			this.systemChain.add(entity);
			entityIdx++;
		}
	}

	// mentions in one sentence
	HashMap<Integer, ArrayList<EntityMention>> sentenceMentions = new HashMap<Integer, ArrayList<EntityMention>>();

	public void rightToLeftSort(ArrayList<EntityMention> ems) {
		for (int i = 0; i < ems.size(); i++) {
			for (int j = ems.size() - 1; j >= i + 1; j--) {
				EntityMention em1 = ems.get(j);
				EntityMention em2 = ems.get(j - 1);
				if (em1.start > em2.end) {
					this.swap(j, j - 1, ems);
				}
				if (em2.start >= em1.start && em2.end <= em1.end) {
					this.swap(j, j - 1, ems);
				}
			}
		}
	}

	public void swap(int i, int j, ArrayList<EntityMention> ems) {
		EntityMention temp = ems.get(i);
		ems.set(i, ems.get(j));
		ems.set(j, temp);
	}

	public void leftToRightSort(ArrayList<EntityMention> ems) {
		for (int i = 0; i < ems.size(); i++) {
			for (int j = ems.size() - 1; j >= i + 1; j--) {
				EntityMention em1 = ems.get(j);
				EntityMention em2 = ems.get(j - 1);
				if (em1.end < em2.start) {
					this.swap(j, j - 1, ems);
				}

				if (em2.start >= em1.start && em2.end <= em1.end) {
					this.swap(j, j - 1, ems);
				}
			}
		}
	}

	public ArrayList<EntityMention> getOrderedAntecedent(EntityMention em) {
		ArrayList<EntityMention> antecedents = new ArrayList<EntityMention>();
		int sentenceId = em.sentenceID;
		if (em.isPronoun && em.entity.mentions.size() == 1) {
			ArrayList<EntityMention> ems = sentenceMentions.get(sentenceId);
			this.leftToRightSort(ems);
			ems = sortSameSentenceForPronoun(ems, em, part.getCoNLLSentences().get(sentenceId));
			if (ontoCommon.getChDictionary().relativePronouns.contains(em.head.toLowerCase())) {
				Collections.reverse(ems);
			}
			for (int i = 0; i < ems.size(); i++) {
				if (ems.get(i).compareTo(em) >= 0) {
				} else {
					antecedents.add(ems.get(i));
				}
			}
			for (int i = sentenceId - 1; i >= 0; i--) {
				ArrayList<EntityMention> tempEMs = sentenceMentions.get(i);
				if (tempEMs != null) {
					this.leftToRightSort(tempEMs);
					antecedents.addAll(tempEMs);
				}
			}
		} else {
			ArrayList<EntityMention> ems = sentenceMentions.get(sentenceId);
			Collections.sort(ems);
			for (int i = 0; i < ems.size(); i++) {
				if (ems.get(i).compareTo(em) >= 0) {
				} else {
					antecedents.add(ems.get(i));
				}
			}
			for (int i = sentenceId - 1; i >= 0; i--) {
				ArrayList<EntityMention> tempEMs = sentenceMentions.get(i);
				if (tempEMs != null) {
					this.leftToRightSort(tempEMs);
					antecedents.addAll(tempEMs);
				}
			}
		}
		boolean entityCoref = true;
		if (em instanceof EventMention) {
			entityCoref = false;
		}
		for (int i = 0; i < antecedents.size(); i++) {
			EntityMention ant = antecedents.get(i);
			if (entityCoref) {
				if (ant instanceof EventMention) {
					antecedents.remove(i);
					i--;
				}
			} else {
				if (!(ant instanceof EventMention)) {
					antecedents.remove(i);
					i--;
				}
			}
		}
		return antecedents;
	}

	public boolean compatible(EntityMention antecedent, EntityMention em, ArrayList<CoNLLSentence> sentences) {
		if (antecedent.ner.equals(em.ner)) {
			String head1 = antecedent.head;
			String head2 = em.head;
			if ((antecedent.ner.equals("PERSON"))) {
				int similarity = 0;
				for (int i = 0; i < head1.length(); i++) {
					if (head2.indexOf(head1.charAt(i)) != -1) {
						similarity++;
					}
				}
				if (similarity == 0) {
					return false;
				}
			} else if (antecedent.ner.equals("LOC") || antecedent.ner.equals("GPE") || antecedent.ner.equals("ORG")) {
				if (!Common.isAbbreviation(head1, head2)) {
					int similarity = 0;
					for (int i = 0; i < head1.length(); i++) {
						if (head2.indexOf(head1.charAt(i)) != -1) {
							similarity++;
						}
					}
					if (similarity == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean checkCompatible(EntityMention antecedent, EntityMention em2, ArrayList<CoNLLSentence> sentences) {
		for (EntityMention em11 : antecedent.entity.mentions) {
			for (EntityMention em22 : em2.entity.mentions) {
				if (tuneSwitch) {
					if (RuleCoref.headPair == null) {
						RuleCoref.headPair = Common.readFile2Map5(language + "_" + folder + "_head_all");
					}
					if (RuleCoref.sourcePair == null) {
						RuleCoref.sourcePair = Common.readFile2Map5(language + "_" + folder + "_source_all");
					}
					String concat = Common.concat(em11.head, em22.head);
					if (RuleCoref.headPair.containsKey(concat)) {
						double value = RuleCoref.headPair.get(concat);
						if (value <= t1) {
							return false;
						}
					}
					String concat2 = Common.concat(em11.source, em22.source);
					if (RuleCoref.sourcePair.containsKey(concat2)) {
						double value = RuleCoref.sourcePair.get(concat2);
						if (value <= t2) {
							return false;
						}
					}
				}
				// in-with-in
				// number mismatch
				// 很多小朋友 ANIMATE PLURAL MALE OTHER 54:647,648#小朋友
				// 这 && 那
				// 这个 这些
				EntityMention longM = (em11.end - em11.start) > (em22.end - em22.start) ? em11 : em22;
				EntityMention shortM = (em11.end - em11.start) <= (em22.end - em22.start) ? em11 : em22;

				if (shortM.start == shortM.end
						&& longM.head.equalsIgnoreCase(shortM.head)
						&& (longM.start + 1 == longM.end || (longM.start + 2 == longM.end && part
								.getWord(longM.start + 1).word.equals("的")))) {
					if (this.ontoCommon.getChDictionary().parts.contains(this.part.getWord(longM.start).word)) {
						if (RuleCoref.bs.get(49)) {
							return false;
						}
					}
				}
				// discourse constraint
				if (!(this.currentSieve instanceof DiscourseProcessSieve)) {
					if (part.getDocument().ontoCommon.isSpeaker(em11, em22, part) && em11.person != Person.I
							&& em22.person != Person.I) {
						if (RuleCoref.bs.get(50)) {
							return false;
						}
					}
					String mSpeaker = part.getWord(em11.headStart).speaker;
					String antSpeaker = part.getWord(em22.headStart).speaker;

					int dist = Math.abs(part.getWord(em11.headStart).utterOrder
							- part.getWord(em22.headStart).utterOrder);
					if (part.getDocument().getType() != DocType.Article && dist == 1
							&& !part.getDocument().ontoCommon.isSpeaker(em11, em22, part)) {
						if (em11.person == Person.I && em22.person == Person.I) {
							if (RuleCoref.bs.get(51))
								return false;
						}
						if (em11.person == Person.YOU && em22.person == Person.YOU) {
							if (RuleCoref.bs.get(52))
								return false;
						}
						if (em11.person == Person.YOUS && em22.person == Person.YOUS) {
							if (RuleCoref.bs.get(53))
								return false;
						}
						if (em11.person == Person.WE && em22.person == Person.WE) {
							if (RuleCoref.bs.get(54))
								return false;
						}
					}
				}

				boolean iWithi = ontoCommon.isIWithI(em11, em22, sentences);
				if (iWithi && !ontoCommon.isCopular2(em11, em22, sentences)
						&& !(this.currentSieve instanceof SameHeadSieve)) {
					if (RuleCoref.bs.get(55)) {
						// System.out.println(antecedent.head + "#" + em2.head +
						// " 55");
						// return false;
					}
				}
				// cc
				if (longM.headStart == shortM.headStart && shortM.start > 0
						&& part.getWord(shortM.start - 1).posTag.equals("CC")) {
					if (RuleCoref.bs.get(57)) {
						return false;
					}
				}
				if (em11.ner.equals(em22.ner)) {
					String head1 = em11.head;
					String head2 = em22.head;
					if ((em11.ner.equals("PERSON"))) {
						int similarity = 0;
						for (int i = 0; i < head1.length(); i++) {
							if (head2.indexOf(head1.charAt(i)) != -1) {
								similarity++;
							}
						}
						if (similarity == 0) {
							if (RuleCoref.bs.get(59))
								return false;
						}
					} else if (em11.ner.equals("LOC") || em11.ner.equals("GPE") || em11.ner.equals("ORG")) {
						if (!Common.isAbbreviation(head1, head2)) {
							int similarity = 0;
							for (int i = 0; i < head1.length(); i++) {
								if (head2.indexOf(head1.charAt(i)) != -1) {
									similarity++;
								}
							}
							if (similarity == 0) {
								if (RuleCoref.bs.get(60)) {
									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	// sort candidate mentions in same sentence for pronouns
	public ArrayList<EntityMention> sortSameSentenceForPronoun(ArrayList<EntityMention> mentions,
			EntityMention mention, CoNLLSentence sentence) {
		ArrayList<EntityMention> mentionsCopy = new ArrayList<EntityMention>();
		ArrayList<EntityMention> sortedMentions = new ArrayList<EntityMention>();
		mentionsCopy.addAll(mentions);
		for (int i = 0; i < mentionsCopy.size(); i++) {
			EntityMention temp = mentionsCopy.get(i);
			if (temp.compareTo(mention) >= 0) {
				mentionsCopy.remove(i);
				i--;
			}
		}
		MyTreeNode root = sentence.getSyntaxTree().root;
		ArrayList<MyTreeNode> allSprings = root.getDepthFirstOffsprings();
		Collections.reverse(allSprings);
		MyTreeNode sNode = getLowestSNode(mention.treeNode);
		while (sNode != null) {
			ArrayList<MyTreeNode> leaves = sNode.getLeaves();
			int leftBound = sentence.getWord(leaves.get(0).leafIdx).index;
			int rightBound = sentence.getWord(leaves.get(leaves.size() - 1).leafIdx).index;
			for (int j = 0; j < mentionsCopy.size(); j++) {
				EntityMention tmp = mentionsCopy.get(j);
				if (tmp.start >= leftBound && tmp.end <= rightBound) {
					sortedMentions.add(tmp);
					mentionsCopy.remove(j);
					j--;
				}
			}
			sNode = this.getLowestSNode(sNode);
		}
		return sortedMentions;
	}

	/*
	 * find the lowest S tree node, or older brother IP, otherwise, return root
	 */
	private MyTreeNode getLowestSNode(MyTreeNode node) {
		if (node.value.equalsIgnoreCase("TOP") || node.value.equalsIgnoreCase("ROOT")) {
			return null;
		}
		for (int i = node.childIndex - 1; i >= 0; i--) {
			if (node.parent.children.get(i).value.startsWith("IP")) {
				return node.parent.children.get(i);
			}
		}
		ArrayList<MyTreeNode> ancestors = node.getAncestors();
		for (int i = ancestors.size() - 2; i >= 0; i--) {
			if (ancestors.get(i).value.startsWith("IP")) {
				return ancestors.get(i);
			}
		}
		return ancestors.get(0);
	}

	public static void printPair(EntityMention em2, EntityMention an2) {
		EventMention em = (EventMention) em2;
		EventMention an = (EventMention) an2;
		int anID = goldEventMentionMap.containsKey(an.toString()) ? goldEventMentionMap.get(an.toString()).goldChainID
				: -1;
		System.err.println(an.getExtent());
		System.err.println(an.head + ":" + anID + ":" + an.subType + "#" + an.bvs + "#" + em.posTag);
		// System.err.println(an.getLdcScope().replace("\\s+", "").replace("\n",
		// ""));
		for (EventMentionArgument arg : an.eventMentionArguments) {
//			System.err.println(arg.role + ":" + arg.mention.head + "#" + arg.mention.goldChainID + "#"
//					+ arg.mention.semClass + "#" + arg.mention.getSubType() + "#" + arg.mention.ner + "#"
//					+ arg.mention.mentionType + "#" + arg.mention.entityIndex + "$" + arg.mention.number + "#"
//					+ arg.mention.gender);
			System.err.println(arg.role + ":" + arg.getExtent());
		}
		for (String role : an.srlArgs.keySet()) {
			ArrayList<EntityMention> mentions = an.srlArgs.get(role);
			StringBuilder sb = new StringBuilder();
			for (EntityMention mention : mentions) {
				sb.append(mention.head).append(" ");
			}
			System.err.println(role + ":" + sb.toString());
		}

		System.err.println("---------");
		int emID = goldEventMentionMap.containsKey(em.toString()) ? goldEventMentionMap.get(em.toString()).goldChainID
				: -1;
		System.err.println(em.getExtent());
		System.err.println(em.head + ":" + emID + ":" + em.subType + "#" + em.bvs + "#" + em.posTag);
		// System.err.println(em.getLdcScope().replace("\\s+", "").replace("\n",
		// ""));
		for (EventMentionArgument arg : em.eventMentionArguments) {
//			System.err.println(arg.role + ":" + arg.mention.head + "#" + arg.mention.goldChainID + "#"
//					+ arg.mention.semClass + "#" + arg.mention.getSubType() + "#" + arg.mention.ner + "#"
//					+ arg.mention.mentionType + "#" + arg.mention.entityIndex + "$" + arg.mention.number + "#"
//					+ arg.mention.gender);
			System.err.println(arg.role + ":" + arg.getExtent());
		}
		for (String role : em.srlArgs.keySet()) {
			ArrayList<EntityMention> mentions = em.srlArgs.get(role);
			StringBuilder sb = new StringBuilder();
			for (EntityMention mention : mentions) {
				sb.append(mention.head).append(" ");
			}
			System.err.println(role + ":" + sb.toString());
		}
		System.err.println("=====================================");
	}

	public boolean goldCoref(EventMention ant, EventMention em) {
		EventMention gEM = goldEventMentionMap.get(em.toString());
		EventMention gAn = goldEventMentionMap.get(ant.toString());
		if (gEM != null && gAn != null && gEM.goldChainID == gAn.goldChainID) {
			return true;
		} else {
			return false;
		}
	}

	public boolean combine2Entities(EntityMention ant, EntityMention em2, ArrayList<CoNLLSentence> sentences) {
		if (ant instanceof EventMention && !(em2 instanceof EventMention)) {
			return false;
		}

		if (em2 instanceof EventMention && !(ant instanceof EventMention)) {
			return false;
		}

		if (ant instanceof EventMention && em2 instanceof EventMention) {
			EventMention an = (EventMention) ant;
			EventMention em = (EventMention) em2;
			EventMention gEM = goldEventMentionMap.get(em2.toString());
			EventMention gAn = goldEventMentionMap.get(ant.toString());

//			int[] stat = MaxEntEventFeas.errors.get(ant.getSubType());
//			if (stat == null) {
//				stat = new int[2];
//				MaxEntEventFeas.errors.put(ant.getSubType(), stat);
//			}
//			if (gEM != null && gAn != null && gEM.goldChainID == gAn.goldChainID) {
//				stat[0]++;
//			} else {
//				stat[1]++;
//			}
//			if (gEM != null && gAn != null && gEM.goldChainID == gAn.goldChainID) {
//				if (an.argHash.containsKey("Time-Within") && em.argHash.containsKey("Time-Within")) {
//					System.err.println("NICE: " + this.part.getDocument().getFilePath());
//					printPair(em2, ant);
//				}
//			} else {
//				// TODO
//				if(an.argHash.containsKey("Time-Within") && em.argHash.containsKey("Time-Within")) {
//					System.err.println("BAD: " + this.part.getDocument().getFilePath());
//					printPair(em2, ant);
//				}
//			}
		}

		if (ant.entityIndex == em2.entityIndex) {
			return false;
		}
		HashSet<EntityMention> predictMentions = new HashSet<EntityMention>();
		predictMentions.addAll(this.mentions);
		if (!checkCompatible(ant, em2, sentences)) {
			return false;
		}
		Entity entity1 = ant.entity;
		Entity entity2 = em2.entity;
		int idx2 = this.findEntity(entity2);
		for (int i = 0; i < entity2.mentions.size(); i++) {
			EntityMention em = entity2.mentions.get(i);
			em.entityIndex = entity1.entityIdx;
			entity1.mentions.add(em);
			em.entity = entity1;
		}
		this.systemChain.remove(idx2);
		// if (this.currentSieve instanceof PronounSieve) {
		EntityMention trueAntecedent = null;
		HashSet<EntityMention> trueAnts = null;
		if ((trueAnts = this.goldMaps.get(em2)) != null) {
			ArrayList<EntityMention> trueAntsList = new ArrayList<EntityMention>();
			trueAntsList.addAll(trueAnts);
			Collections.sort(trueAntsList);
			for (EntityMention mention : trueAntsList) {
				if (mention.compareTo(em2) < 0) {
					trueAntecedent = mention;
				} else {
					break;
				}
			}
		}
		if (trueAntecedent != null) {
			if (this.getMentionFromSet(predictMentions, ant) != null) {
				trueAntecedent = this.getMentionFromSet(predictMentions, trueAntecedent);
			}
		}
		em2.antecedent = ant;
		return true;
	}

	public int findEntity(Entity entity) {
		for (int i = 0; i < this.systemChain.size(); i++) {
			Entity en = this.systemChain.get(i);
			if (en.entityIdx == entity.entityIdx) {
				return i;
			}
		}
		return -1;
	}

	public EntityMention getMentionFromSet(HashSet<EntityMention> sets, EntityMention mention) {
		for (EntityMention m : sets) {
			if (m.equals(mention)) {
				return m;
			}
		}
		return null;
	}

	public void printRecallError(ArrayList<Entity> entities, ArrayList<Entity> goldEntities) {
		HashMap<EntityMention, HashSet<EntityMention>> predictMaps = new HashMap<EntityMention, HashSet<EntityMention>>();
		this.loadGoldMaps(predictMaps, entities);
		HashSet<EntityMention> predictMentions = new HashSet<EntityMention>();
		predictMentions.addAll(this.mentions);
		for (Entity entity : goldEntities) {
			ArrayList<EntityMention> mentions = entity.mentions;
			Collections.sort(mentions);
			for (int i = 0; i < mentions.size(); i++) {
				EntityMention mention = mentions.get(i);
				if (!predictMentions.contains(mention)) {
					continue;
				}
				for (int j = i - 1; j >= 0; j--) {
					EntityMention antecedent = mentions.get(j);
					if (!predictMentions.contains(antecedent)) {
						continue;
					}
					if (predictMaps.get(mention) == null || !predictMaps.get(mention).contains(antecedent)) {
						if (this.goldMaps.get(mention) == null || !goldMaps.get(mention).contains(mention.antecedent)) {
							antecedent = this.getMentionFromSet(predictMentions, antecedent);
							mention = this.getMentionFromSet(predictMentions, mention);
							// System.out.println("==========Recall=========");
							// System.out.println("mention:\t" + mention + " " +
							// this.part.getWord(mention.start).speaker);
							// System.out.println("antecedent:\t" + antecedent +
							// " " +
							// this.part.getWord(antecedent.start).speaker);
							// System.out.println("system ante:\t" +
							// mention.antecedent + " " +
							// (mention.antecedent==null?"":this.part.getWord(mention.antecedent.start).speaker));
							// System.out.println(part.getDocument().getFilePath()
							// + " " + part.getPartID());
							break;
						}
					} else if (predictMaps.get(mention) != null && predictMaps.get(mention).contains(antecedent)) {
						break;
					}
				}
			}

		}
	}

	public static ArrayList<Sieve> sieves;

	public static ArrayList<ArrayList<Element>> nerses;

	public static HashMap<String, ArrayList<EntityMention>> allMentions;

	public void outputEntities(ArrayList<Entity> entities, String path, boolean eventMention) {
		FileWriter fw;
		try {
			fw = new FileWriter(path);
			for (Entity entity : entities) {
				ArrayList<EntityMention> ems = entity.mentions;
				StringBuilder sb = new StringBuilder();
				for (EntityMention em : ems) {
					if (eventMention) {
						if (em instanceof EventMention) {
							sb.append(em.headCharStart).append(",").append(em.headCharEnd).append(" ");
						}
					}
					if (!eventMention) {
						if (!(em instanceof EventMention)) {
							sb.append(em.headCharStart).append(",").append(em.headCharEnd).append(" ");
						}
					}
				}
				if (!sb.toString().trim().isEmpty()) {
					fw.write(sb.toString().trim() + "\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void outputEntities(ArrayList<Entity> entities, String path) {
		FileWriter fw;
		try {
			fw = new FileWriter(path);
			for (Entity entity : entities) {
				ArrayList<EntityMention> ems = entity.mentions;
				StringBuilder sb = new StringBuilder();
				for (EntityMention em : ems) {
					// map error head
					boolean matchHead = false;
					for (EntityMention gold : this.goldMentions) {
						if (gold.headCharStart == em.headCharStart && gold.headCharEnd == em.headCharEnd) {
							matchHead = true;
							break;
						}
					}
					if (!matchHead) {
						for (EntityMention gold : this.goldMentions) {
							if (gold.extentCharStart == em.headCharStart && gold.extendCharEnd == em.headCharEnd) {
								em.headCharStart = gold.headCharStart;
								em.headCharEnd = gold.headCharEnd;
								break;
							}
						}
					}
					sb.append(em.headCharStart).append(",").append(em.headCharEnd).append(" ");
				}
				fw.write(sb.toString().trim() + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static HashMap<String, Double> headPair;

	static HashMap<String, Double> sourcePair;

	public static HashMap<EntityMention, EntityMention> appoPairs = new HashMap<EntityMention, EntityMention>();

	public static void loadSieves() {
		sieves = new ArrayList<Sieve>();

		Sieve sameHeadSieve = new SameHeadSieve();
		sieves.add(sameHeadSieve);

		Sieve discourseProcessSieve = new DiscourseProcessSieve();
		sieves.add(discourseProcessSieve);

		// Sieve timeSieve = new TimeSieve();
		// sieves.add(timeSieve);

		Sieve exactMatchSieve = new ExactMatchSieve();
		sieves.add(exactMatchSieve);

		Sieve preciseConstructSieve = new PreciseConstructSieve();
		sieves.add(preciseConstructSieve);

		Sieve strictHeadMatchSieve1 = new StrictHeadMatchSieve1();
		sieves.add(strictHeadMatchSieve1);

		Sieve strictHeadMatchSieve2 = new StrictHeadMatchSieve2();
		sieves.add(strictHeadMatchSieve2);

		Sieve strictHeadMatchSieve3 = new StrictHeadMatchSieve3();
		sieves.add(strictHeadMatchSieve3);

		Sieve strictHeadMatchSieve4 = new StrictHeadMatchSieve4();
		sieves.add(strictHeadMatchSieve4);

		Sieve relaxHeadMatchSieve = new RelaxHeadMatchSieve();
		sieves.add(relaxHeadMatchSieve);

		Sieve pronounSieve = new PronounSieve();
		sieves.add(pronounSieve);

		Sieve eventSieve = new EventSieve();
		sieves.add(eventSieve);
		//
		// if (tuneSwitch) {
		// Sieve stringPairSieve = new StringPairSieve();
		// sieves.add(stringPairSieve);
		// }

		// Collections.reverse(sieves);
	}

	public Sieve currentSieve;

	public static double t1 = -1;
	public static double t2 = -1;
	public static double t3 = 2;
	public static double t4 = 2;
	public static double t5 = -1;

	public static ArrayList<Boolean> bs;

	public static boolean tuneSwitch = true;

	public static boolean open = true;

	public static String track;

	public static void main(String args[]) throws Exception {
		Common.part = args[args.length - 1];
		ArrayList<Boolean> bs2 = new ArrayList<Boolean>();
		String args2[];
		// load test folder open
		if (args[0].equals("load")) {
			tuneSwitch = true;
			ArrayList<String> twoLine = Common.getLines("chinese_" + args[2] + "_" + args[3] + "_opt");
			args2 = new String[8];
			String tokens[] = twoLine.get(0).split("\\s+");
			args2[0] = args[1];// test or development
			args2[1] = args[2];// folder
			args2[7] = args[3];
			args2[2] = tokens[2];
			args2[3] = tokens[3];
			args2[4] = tokens[4];
			args2[5] = tokens[5];
			args2[6] = tokens[6];
			tokens = twoLine.get(1).split("\\s+");
			for (String token : tokens) {
				bs2.add(new Boolean(token));
			}
		} else {
			if (args.length < 3) {
				System.out.println("java ~ development folder");
				return;
			}
			if (args.length > 3) {
				tuneSwitch = true;
			} else {
				tuneSwitch = false;
			}
			for (int i = 0; i < 61; i++) {
				bs2.add(new Boolean(true));
			}
			args2 = args;
		}
		run(args2, bs2, "");
	}

	public static HashMap<String, Double> mention_stats = new HashMap<String, Double>();

	public static void run(String[] args, ArrayList<Boolean> bs2, String sur) throws IOException, Exception {
		bs = bs2;
		String folder = "nw";
		if (tuneSwitch) {
			t1 = Double.valueOf(args[2]);
			t2 = Double.valueOf(args[3]);
			t3 = Double.valueOf(args[4]);
			t4 = Double.valueOf(args[5]);
			t5 = Double.valueOf(args[6]);
			mention_stats = Common.readFile2Map5("chinese_" + folder + "_mention");
		}
		if (args.length == 8) {
			open = Boolean.valueOf(args[7]);
		}
		if (open) {
			sur += "_open";
		} else {
			sur += "_close";
		}
		String outputFolder = "/users/yzcchen/chen3/conll12/chinese/ACE_test_" + Common.part + "/";
		ArrayList<String> files = Common.getLines("ACE_" + Common.part);
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		ChCommon.loadPredictNE(folder, args[0]);
		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			// System.out.println(conllFn);
			CoNLLDocument document = ACEReader.read(conllFn, false);
			document.setDocumentID(Integer.toString(fileIdx));
			for (int k = 0; k < document.getParts().size(); k++) {
				fofFw2.write(conllFn + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "chinese";
				ruleCoref.folder = folder;
				for (Sieve sieve : sieves) {
					// System.out.println("Sieve : " +
					// sieve.getClass().getName());
					ruleCoref.currentSieve = sieve;
					sieve.act(ruleCoref);
				}
				ArrayList<Entity> entities = ruleCoref.systemChain;
				ruleCoref.outputEntities(entities, outputFolder + document.getDocumentID() + ".entities.sieve.both");
				ruleCoref.outputEntities(entities, outputFolder + document.getDocumentID() + ".entities.sieve.entity",
						false);
				ruleCoref.outputEntities(entities, outputFolder + document.getDocumentID() + ".entities.sieve.event",
						true);
				Common.outputLines(ruleCoref.eventLines, outputFolder + document.getDocumentID() + ".eventLines");
				Common.outputLines(ruleCoref.entityLines, outputFolder + document.getDocumentID() + ".entityLines");
			}
		}
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + args[0] + sur + "/";
		a2[1] = "sieve";

		// double right = 0;
		// double wrong = 0;
//		for (String key : MaxEntEventFeas.errors.keySet()) {
//			int stat[] = MaxEntEventFeas.errors.get(key);
//			System.err.println(key + ": " + stat[0] + "#" + stat[1]);
//		}
		// double precission = right / (right + wrong);
		// System.err.println("Pre: " + precission);
		// System.err.println("######");
		// for (String key : MaxEntEventFeas.examples.keySet()) {
		// ArrayList<String> example = MaxEntEventFeas.examples.get(key);
		// System.err.println(key + ":");
		// for (String ex : example) {
		// System.err.println(ex);
		// }
		// System.err.println("----");
		// }
		// System.err.println("========");
		
	}

	static int error1 = 0;
	static int error2 = 0;
	static int error3 = 0;

	public void loadGoldMaps(HashMap<EntityMention, HashSet<EntityMention>> maps, ArrayList<Entity> chain) {
		for (Entity entity : chain) {
			for (EntityMention em : entity.mentions) {
				HashSet<EntityMention> ems = new HashSet<EntityMention>();
				for (EntityMention em2 : entity.mentions) {
					if (!em.equals(em2)) {
						ems.add(em2);
					}
				}
				maps.put(em, ems);
			}
		}
	}

	public HashMap<EntityMention, HashSet<EntityMention>> goldMaps = new HashMap<EntityMention, HashSet<EntityMention>>();

}
