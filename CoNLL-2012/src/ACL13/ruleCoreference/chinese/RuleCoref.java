package ACL13.ruleCoreference.chinese;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import jnisvmlight.LabeledFeatureVector;
import jnisvmlight.SVMLightModel;
import mentionDetect.GoldMention;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common;
import util.Common.Person;
import ACL13.NBModel.ClusterFeatures;
import coref.OntoAltafToSemEvalOffical;

public class RuleCoref {
	CoNLLPart part;

	public ArrayList<EntityMention> mentions;

	ArrayList<Entity> goldChain;

	ArrayList<Entity> systemChain;

	ArrayList<CoNLLSentence> sentences;

	String language;

	String folder;

	ChCommon ontoCommon;

	HashMap<String, EntityMention> goldMentions = new HashMap<String, EntityMention>();

	public RuleCoref(CoNLLPart part) {
		ontoCommon = new ChCommon("chinese");
		appoPairs.clear();
		this.part = part;
		goldChain = new ArrayList<Entity>();
		MentionDetect md = new ParseTreeMention();
		MentionDetect md2 = new GoldMention();

		ArrayList<EntityMention> gms = md2.getMentions(goldPart);
		for (EntityMention m : gms) {
			this.goldMentions.put(m.toName(), m);
		}
		// System.out.println(goldMentions.size());
		if (mode.equalsIgnoreCase("train")) {
			this.mentions = md2.getMentions(part);
		} else {
			// TODO
			this.mentions = md.getMentions(part);
		}
		this.goldChain = goldPart.getChains();
		Collections.sort(mentions);
		this.systemChain = new ArrayList<Entity>();
		this.sentences = part.getCoNLLSentences();
		int entityIdx = 0;
		for (EntityMention em : mentions) {
			int start = em.start;
			int end = em.end;
			EntityMention mention = new EntityMention(start, end);
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			mention.source = sb.toString().trim().toLowerCase();
			mention.original = sb2.toString().trim();
			Entity entity = new Entity();
			entity.entityIdx = entityIdx;
			em.entityIndex = entityIdx;
			em.entity = entity;
			ontoCommon.calAttribute(em, part);
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
		loadGoldMaps(goldMaps, this.goldChain);
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
		if (mode.equalsIgnoreCase("train")
		// || mode.equalsIgnoreCase("test")
		) {
			if (this.goldMentions.get(antecedent.toName()).entityIndex != this.goldMentions.get(em2.toName()).entityIndex) {
				return false;
			} else {
				return true;
			}
		} else {
			for (EntityMention em11 : antecedent.entity.mentions) {
				for (EntityMention em22 : em2.entity.mentions) {
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
							if (RuleCoref.bs.get(49))
								return false;
						}
					}
					// discourse constraint
					if (!(this.currentSieve instanceof DiscourseProcessSieve)) {
						if (part.getDocument().ontoCommon.isSpeaker(em11, em22, part) && em11.person != Person.I
								&& em22.person != Person.I) {
							if (RuleCoref.bs.get(50))
								return false;
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
						if (RuleCoref.bs.get(55))
							return false;
					}
					// CC construct
					MyTreeNode maxTreeNode = null;
					String shortEM = "";
					if (antecedent.source.length() > em2.source.length()) {
						maxTreeNode = antecedent.treeNode;
						shortEM = em2.source.replaceAll("\\s+", "");
					} else {
						maxTreeNode = em2.treeNode;
						shortEM = antecedent.source.replaceAll("\\s+", "");
					}
					ArrayList<MyTreeNode> offsprings = maxTreeNode.getBroadFirstOffsprings();
					for (MyTreeNode node : offsprings) {
						if (node.value.equalsIgnoreCase("cc") || node.value.equalsIgnoreCase("pu")) {
							for (MyTreeNode child2 : node.parent.children) {
								if (child2.toString().replaceAll("\\s+", "").endsWith(shortEM)) {
									if (RuleCoref.bs.get(56)) {
										return false;
									}
								}
							}
						}
					}
					// cc
					if (longM.headStart == shortM.headStart && shortM.start > 0
							&& part.getWord(shortM.start - 1).posTag.equals("CC")) {
						if (RuleCoref.bs.get(57)) {
							return false;
						}
					}
					// Copular construct
					if (ontoCommon.isCopular2(antecedent, em2, sentences)) {
						if (RuleCoref.bs.get(58))
							return false;
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
									if (RuleCoref.bs.get(60))
										return false;
								}
							}
						}
					}
				}
			}
			return true;
		}
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
		if (node.value.equalsIgnoreCase("TOP")) {
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

	public boolean combine2Entities(EntityMention antecedent, EntityMention em2, ArrayList<CoNLLSentence> sentences) {
		if (antecedent.entityIndex == em2.entityIndex) {
			return false;
		}
		HashSet<EntityMention> predictMentions = new HashSet<EntityMention>();
		predictMentions.addAll(this.mentions);
		if (!checkCompatible(antecedent, em2, sentences)) {
			return false;
		}
		// TODO
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
			if (this.getMentionFromSet(predictMentions, antecedent) != null) {
				trueAntecedent = this.getMentionFromSet(predictMentions, trueAntecedent);
			}
		}
		if (em2.roleSet.size() == 0 && antecedent.roleSet.size() == 0) {
			if (this.goldMentions.containsKey(em2.toName()) && this.goldMentions.containsKey(antecedent.toName())) {
				if (this.goldMaps.get(em2).contains(antecedent) && this.currentSieve instanceof PatternSieve) {
					System.out.println("==========Right==========");
					System.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
					System.out
							.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
					System.out.println("gold ante:\t" + trueAntecedent + " "
							+ (trueAntecedent == null ? "" : this.part.getWord(trueAntecedent.start).speaker));
					System.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
					System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
					// return true;
				} else {
					// if (this.currentSieve instanceof StrictHeadMatchSieve1
					// || this.currentSieve instanceof StrictHeadMatchSieve2
					// || this.currentSieve instanceof StrictHeadMatchSieve3
					// || this.currentSieve instanceof StrictHeadMatchSieve4
					// || this.currentSieve instanceof RelaxHeadMatchSieve) {
					if (this.currentSieve instanceof PatternSieve) {
						System.out.println("==========P1===========");
						System.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
						System.out.println("antecedent:\t" + antecedent + " "
								+ this.part.getWord(antecedent.start).speaker);
						System.out.println("gold ante:\t" + trueAntecedent + " "
								+ (trueAntecedent == null ? "" : this.part.getWord(trueAntecedent.start).speaker));
						System.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
						System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
					}
				}
			} else {
				// if (this.currentSieve instanceof StrictHeadMatchSieve1
				// || this.currentSieve instanceof StrictHeadMatchSieve2
				// || this.currentSieve instanceof StrictHeadMatchSieve3
				// || this.currentSieve instanceof StrictHeadMatchSieve4
				// || this.currentSieve instanceof RelaxHeadMatchSieve) {
				if (this.currentSieve instanceof PatternSieve) {
					System.out.println("==========P2===========");
					System.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
					System.out
							.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
					System.out.println("gold ante:\t" + trueAntecedent + " "
							+ (trueAntecedent == null ? "" : this.part.getWord(trueAntecedent.start).speaker));
					System.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
					System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
					// return false;
				}
			}
		}

		Entity entity1 = antecedent.entity;
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

		// System.out.println(antecedent.start+"#"+antecedent.end+"#"+antecedent.source
		// + " " + em2.start +"#"+em2.end+"#"+em2.source);
		em2.antecedent = antecedent;
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
							// if (!mention.isPronoun && !antecedent.isPronoun)
							// {
							System.out.println("==========Recall=========");
							System.out.println("mention:\t" + mention + " " + this.part.getWord(mention.start).speaker);
							System.out.println("antecedent:\t" + antecedent + " "
									+ this.part.getWord(antecedent.start).speaker);
							System.out.println("system ante:\t"
									+ mention.antecedent
									+ " "
									+ (mention.antecedent == null ? ""
											: this.part.getWord(mention.antecedent.start).speaker));
							System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
							ArrayList<String> patterns = RuleCoref.getPatternFromPairs(antecedent, mention, part);
							for (String pattern : patterns) {
								if (stats.containsKey(pattern)) {
									System.out.println(pattern + ":" + stats.get(pattern).accuracy + ":"
											+ stats.get(pattern).coref);
								}
							}
							// boolean succ = combine2Entities(antecedent,
							// mention, sentences);
							// System.err.println(succ);
							// }
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

	public static void outputEntities(ArrayList<Entity> entities, String path) {
		FileWriter fw;
		try {
			fw = new FileWriter(path);
			for (Entity entity : entities) {
				ArrayList<EntityMention> ems = entity.mentions;
				StringBuilder sb = new StringBuilder();
				if (ems.size() == 2 && ems.get(0).end == ems.get(1).end) {
					// System.out.println("#########################################");
					// continue;
				}
				for (EntityMention em : ems) {
					if (em.roleSet.size() != 0) {
						continue;
					}
					sb.append(em.start).append(",").append(em.end).append(" ");
				}
				fw.write(sb.toString().trim() + "\n");
			}
			fw.close();
		} catch (IOException e) {
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
		//
		Sieve timeSieve = new TimeSieve();
		sieves.add(timeSieve);

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

		Sieve patternSieve = new PatternSieve();
		sieves.add(patternSieve);

		Sieve relaxHeadMatchSieve = new RelaxHeadMatchSieve();
		sieves.add(relaxHeadMatchSieve);

		Sieve pronounSieve = new PronounSieve();
		sieves.add(pronounSieve);

		// if (tuneSwitch) {
		// Sieve stringPairSieve = new StringPairSieve();
		// sieves.add(stringPairSieve);
		// }

		// ArrayList<Sieve> tempSieves = new ArrayList<Sieve>();
		//		
		// System.out.println(sieves.size());
		// while(sieves.size()>0) {
		// Random random = new Random();
		// int k = random.nextInt(sieves.size());
		// tempSieves.add(sieves.get(k));
		// sieves.remove(k);
		// }
		// sieves = tempSieves;
		// System.out.println(sieves.size()+"#");
		// Collections.reverse(sieves);
	}

	public Sieve currentSieve;

	public static double t1 = -1;
	public static double t2 = -1;
	public static double t3 = 2;
	public static double t4 = 2;
	public static double t5 = -1;

	public static ArrayList<Boolean> bs;

	public static String mode;

	public static void main(String args[]) throws Exception {
		if (args.length < 2) {
			System.out.println("java ~ development folder");
			return;
		}
		mode = args[0];
		bs = new ArrayList<Boolean>();
		for (int i = 0; i < 61; i++) {
			bs.add(new Boolean(true));
		}

		if (mode.equalsIgnoreCase("train")) {
			cf = new ClusterFeatures(mode.equalsIgnoreCase("train"), args[1]);
			runTrain(args[1]);
			cf.outputIdx();
		} else if (mode.equalsIgnoreCase("test")) {
			cf = new ClusterFeatures(mode.equalsIgnoreCase("train"), args[1]);
			initSVMClassifier(args[1]);
			runTest(args[1]);
		} else if (mode.equalsIgnoreCase("collect")) {
			collectPattern(args[1]);
		} else if (mode.equalsIgnoreCase("calAccuracy")) {
			calAccuracy(args[1]);
		} else if (mode.equalsIgnoreCase("testSieve")) {
			mode = "test";
			testSieve(args[1]);
		}

		else {
			System.err.println("WRONG ARGUMENT");
		}
	}

	public static HashMap<String, Stat> loadPatternStat(String folder) {
		HashMap<String, Stat> stats = new HashMap<String, Stat>();
		ArrayList<String> lines = Common.getLines("patternsAccuracy." + folder);
		for (String line : lines) {
			int a = line.lastIndexOf(" ");
			int b = line.lastIndexOf(" ", a - 1);
			int c = line.lastIndexOf(" ", b - 1);
			String pattern = line.substring(0, c);
			double coref = Double.parseDouble(line.substring(c + 1, b));
			double notCoref = Double.parseDouble(line.substring(b + 1, a));
			double accuracy = Double.parseDouble(line.substring(a + 1));
			Stat stat = new Stat();
			stat.pattern = pattern;
			stat.coref = coref;
			stat.nonCoref = notCoref;
			stat.accuracy = accuracy;
			stats.put(pattern, stat);
		}
		return stats;
	}

	static HashMap<String, Stat> stats;

	public static void testSieve(String folder) throws Exception {
		stats = loadPatternStat(folder);
		String sur = "_open";
		String outputFolder = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_" + mode + "/");
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		ChCommon.loadPredictNE(folder, mode);

		CoNLLDocument goldDocument;

		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");

//		HashMap<String, HashSet<String>> anaphors = ChCommon.loadAnaphorResult(folder);
		
		int qid = 1;

		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);

//			if (!conllFn
//					.equalsIgnoreCase("/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/test/data/chinese/annotations/nw/xinhua/00/chtb_0059.v5_auto_conll")) {
//				continue;
//			}

			System.out.println(conllFn);

			// TODO
			CoNLLDocument document = new CoNLLDocument(conllFn
			// .replace("test", "test_gold")
			);
			goldDocument = new CoNLLDocument(conllFn.replace("test", "test_gold"));
			for (int k = 0; k < document.getParts().size(); k++) {
				
//				HashSet<String> anaphorSet = anaphors.get(conllFn + " " + k);
				goldPart = goldDocument.getParts().get(k);
				fofFw2.write(conllFn + "_" + k + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "chinese";
				ruleCoref.folder = folder;
				for (Sieve sieve : sieves) {
					ruleCoref.currentSieve = sieve;
					sieve.act(ruleCoref);
				}
				ArrayList<Entity> entities = ruleCoref.systemChain;
				System.err.println(entities.size());
				ArrayList<Entity> goldEntities = ruleCoref.goldChain;
				Collections.sort(entities);

				System.err.println(entities.size());

				ruleCoref.printRecallError(entities, goldEntities);
				outputEntities(entities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.system");
				outputEntities(goldEntities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.gold");
			}
			// System.out.println(document.getType());
		}
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		a2[1] = "system";
		OntoAltafToSemEvalOffical.runOutputKey(a2);
	}

	// public static void collect

	public static void collectPattern(String folder) {
		String mode = "train";

		ArrayList<String> files = Common.getLines("chinese_list_all_" + mode + "/");

		CoNLLDocument goldDocument;
		int g = files.size();
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn + ":" + (g--));

			// TODO
			CoNLLDocument document = new CoNLLDocument(conllFn.replace("test", "test_gold"));
			goldDocument = new CoNLLDocument(conllFn.replace("test", "test_gold"));

			for (int k = 0; k < document.getParts().size(); k++) {
				goldPart = goldDocument.getParts().get(k);
				CoNLLPart part = document.getParts().get(k);

				RuleCoref ruleCoref = new RuleCoref(part);

				ArrayList<Entity> entities = ruleCoref.goldChain;

				for (Entity entity : entities) {

					for (int i = 0; i < entity.mentions.size(); i++) {
						EntityMention m2 = entity.mentions.get(i);
						ruleCoref.ontoCommon.calAttribute(m2, goldPart);
						HashSet<String> ps2 = getPattersForMention(m2, goldPart);

						for (int j = 0; j < i; j++) {
							EntityMention m1 = entity.mentions.get(j);

							// if (m2.head.equalsIgnoreCase(m1.head)) {
							// continue;
							// }
							// HashSet<String> ps1 = getPattersForMention(m1,
							// goldPart);
							// for (String p1 : ps1) {
							// for (String p2 : ps2) {
							// addPattern(Common.concat(p1, p2));
							// }
							// }
							// System.out.println(m1.source + "#" + m2.source +
							// "$" + ps1.size() + "*" + ps2.size());

							ArrayList<String> ps = getPatternFromPairs(m1, m2, goldPart);
							for (String p : ps) {
								addPattern(p);
							}
						}
					}
				}
			}
		}
		Common.outputHashMap(patterns, "patterns.all");
	}

	static void calAccuracy(String folder) {
		String mode = "train";
		patterns = Common.readFile2Map("patterns.all");
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_" + mode + "/");

		HashMap<String, Stat> patternStats = new HashMap<String, Stat>();
		for (String key : patterns.keySet()) {
			Stat stat = new Stat();
			stat.pattern = key;
			patternStats.put(key, stat);
		}

		CoNLLDocument goldDocument;
		int g = files.size();
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn + "#" + (g--));

			// TODO
			CoNLLDocument document = new CoNLLDocument(conllFn);

			goldDocument = new CoNLLDocument(conllFn);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				goldPart = goldDocument.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ArrayList<EntityMention> mentions = ruleCoref.mentions;

				for (int i = 0; i < mentions.size(); i++) {
					EntityMention m2 = mentions.get(i);
					ruleCoref.ontoCommon.calAttribute(m2, goldPart);
					// System.out.print(m2.source);
					HashSet<String> ps2 = getPattersForMention(m2, goldPart);
					// System.out.println(ps2.size());
					for (int j = 0; j < i; j++) {
						EntityMention m1 = mentions.get(j);
						// System.out.print(m1.source);
						HashSet<String> ps1 = getPattersForMention(m1, goldPart);
						// System.out.println(ps1.size());
						ArrayList<String> ps = getPatternFromPairs(m1, m2, goldPart);
						for (String pattern : ps) {

							if (patternStats.containsKey(pattern)) {
								if (ruleCoref.goldMentions.containsKey(m1.toName())
										&& ruleCoref.goldMentions.containsKey(m2.toName())
										&& ruleCoref.goldMentions.get(m1.toName()).entityIndex == ruleCoref.goldMentions
												.get(m2.toName()).entityIndex) {
									patternStats.get(pattern).coref++;
								} else {
									patternStats.get(pattern).nonCoref++;
								}
							}

						}
						// for (String p1 : ps1) {
						// for (String p2 : ps2) {
						// String pattern = Common.concat(p1, p2);
						// if (patternStats.containsKey(pattern)) {
						// if (ruleCoref.goldMentions.containsKey(m1.toName())
						// && ruleCoref.goldMentions.containsKey(m2.toName())
						// &&
						// ruleCoref.goldMentions.get(m1.toName()).entityIndex
						// == ruleCoref.goldMentions
						// .get(m2.toName()).entityIndex) {
						// patternStats.get(pattern).coref++;
						// } else {
						// patternStats.get(pattern).nonCoref++;
						// }
						// }
						// }
						// }
					}
				}
			}
		}
		System.err.println("CALCULATE ACCURACY");
		for (String pn : patternStats.keySet()) {
			patternStats.get(pn).calAccuracy();
		}
		ArrayList<Stat> stats = new ArrayList<Stat>();
		stats.addAll(patternStats.values());
		System.err.println("SORT ACCURACY");
		Collections.sort(stats);
		System.err.println("OUTPUT ACCURACY");
		ArrayList<String> lines = new ArrayList<String>();
		for (Stat stat : stats) {
			StringBuilder sb = new StringBuilder();
			sb.append(stat.pattern).append(" ").append(stat.coref).append(" ").append(stat.nonCoref).append(" ")
					.append(stat.accuracy);
			lines.add(sb.toString());
		}
		Common.outputLines(lines, "patternsAccuracy." + folder);
	}

	static class Stat implements Comparable<Stat> {
		double coref = 0;
		double nonCoref = 0;
		String pattern;
		double accuracy = 0;

		public int compareTo(Stat stat) {
			int sign = (int) Math.signum(stat.accuracy - this.accuracy);
			if (sign == 0) {
				sign = (int) Math.signum(stat.coref - this.coref);
			}
			return sign;
		}

		public void calAccuracy() {
			this.accuracy = this.coref / (this.nonCoref + this.coref);
		}
	}

	static HashMap<String, Integer> patterns = new HashMap<String, Integer>();

	public static void addPattern(String pattern) {
		if (patterns.containsKey(pattern)) {

		} else {
			patterns.put(pattern, patterns.size());
		}
	}

	public static ArrayList<String> getPatternFromPairs(EntityMention m1, EntityMention m2, CoNLLPart part) {
		ArrayList<String> patterns = new ArrayList<String>();

		EntityMention sm = m1.source.length() < m2.source.length() ? m1 : m2;
		EntityMention lm = m1.source.length() >= m2.source.length() ? m1 : m2;

		String s = sm.source.replaceAll("\\s+", "").replace("的", "");
		String l = lm.source.replaceAll("\\s+", "").replace("的", "");
		
		// add source
		patterns.add(s + "#" + l);
		// add head
//		patterns.add(sm.head + "#" + lm.head);
		// replace Named Entity
		int fakeIdx = 0;
		for(int i=sm.start;i<=sm.end;i++) {
			if(!part.getWord(i).rawNamedEntity.equalsIgnoreCase("*")) {
				s = s.replace(part.getWord(i).word, part.getWord(i).rawNamedEntity + (fakeIdx));
				l = l.replace(part.getWord(i).word, part.getWord(i).rawNamedEntity + (fakeIdx++));
			}
		}
		
		for(int i=lm.start;i<=lm.end;i++) {
			if(!part.getWord(i).rawNamedEntity.equalsIgnoreCase("*") && l.contains(part.getWord(i).word)) {
				l = l.replace(part.getWord(i).word, part.getWord(i).rawNamedEntity + (fakeIdx++));
			}
		}
		patterns.add(s + "#" + l);
		
		// replace matching word
		fakeIdx = 0;
		int shortS, shortE, longS, longE;
		// System.out.println(s + "#" + l);
		for (int i = 0; i < s.length();) {

			int j = i + 1;
			String text = s.substring(i, j);

			if (l.indexOf(text) != -1) {
				while (l.indexOf(text) != -1 && j < s.length()) {
					j++;
					text = s.substring(i, j);
					// System.out.println();
				}
				String match;
				if (j == s.length() && l.indexOf(text) != -1) {
					match = s.substring(i, j);
					shortS = i;
					shortE = j - 1;
					longS = l.indexOf(match);
					longE = longS + shortE - shortS;
				} else {
					match = s.substring(i, j - 1);
					shortS = i;
					shortE = j - 2;
					longS = l.indexOf(match);
					longE = longS + shortE - shortS;
				}

				if (!match.isEmpty()) {
					String proxy = getFake(fakeIdx++);
					s = s.substring(0, shortS) + proxy + s.substring(shortE + 1);
					l = l.substring(0, longS) + proxy + l.substring(longE + 1);
					i = j;
					i = shortS + 1;
				}
			} else {
				i++;
			}
		}
		patterns.add(s + "#" + l);
		
		// System.out.println(s + "#" + l);
//		 System.out.println("=========");
		return patterns;
	}

	public static String getFake(int i) {
		char c = (char) ('a' + i);
		return Character.toString(c);
	}

	public static HashSet<String> getPattersForMention(EntityMention m, CoNLLPart part) {
		HashSet<String> totalPS = new HashSet<String>();
		int k = m.end;
		// 南韩总统金大忠
		while (k >= m.start && m.end - k <= 4) {
			HashSet<String> semWord = getSemantics(part.getWord(k));
			if (totalPS.isEmpty()) {
				totalPS.addAll(semWord);
			} else {
				HashSet<String> tempPS = new HashSet<String>();
				for (String sem : semWord) {
					for (String ps : totalPS) {
						tempPS.add(sem + ps);
					}
				}
				if (k == m.start || m.end - k == 4) {
					totalPS.clear();
				}
				totalPS.addAll(tempPS);
			}
			k--;
		}
		HashSet<String> ret = new HashSet<String>();
		for (String str : totalPS) {
			ret.add(str.replaceAll("\\*+", "\\*"));
		}

		ret.remove("*");
		return ret;
	}

	public static HashSet<String> getSemantics(CoNLLWord word) {
		HashSet<String> ps = new HashSet<String>();
		ps.add(word.word);
		ps.add(word.getRawNamedEntity());
		String semantics[] = Common.getSemantic(word.word);
		if (semantics != null) {
			for (String sem : semantics) {
				// ps.add(sem.substring(0, 1));
				// ps.add(sem.substring(1, 2));
				// ps.add(sem.substring(2, 4));
				// ps.add(sem.substring(4, 5));
				// ps.add(sem.substring(5, 7));
				break;
			}
		}
		// ps.add("");
		return ps;
	}

	// public static ArrayList<String> getOverlapPattern(EntityMention m1,
	// EntityMention m2) {
	//
	// }

	static ClusterFeatures cf;
	public static CoNLLPart goldPart;

	public static void runTest(String folder) throws Exception {
		String sur = "_open";
		String outputFolder = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_" + mode + "/");
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		ChCommon.loadPredictNE(folder, mode);

		CoNLLDocument goldDocument;

		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");

		int qid = 1;

		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);

			// TODO
			CoNLLDocument document = new CoNLLDocument(conllFn.replace("test", "test_gold"));

			goldDocument = new CoNLLDocument(conllFn.replace("test", "test_gold"));

			for (int k = 0; k < document.getParts().size(); k++) {
				goldPart = goldDocument.getParts().get(k);
				fofFw2.write(conllFn + "_" + k + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "chinese";
				ruleCoref.folder = folder;
				for (Sieve sieve : sieves) {
					ruleCoref.currentSieve = sieve;
					sieve.act(ruleCoref);
				}
				ArrayList<Entity> entities = ruleCoref.systemChain;
				System.err.println(entities.size());
				ArrayList<Entity> goldEntities = ruleCoref.goldChain;
				Collections.sort(entities);
				for (int i = 0; i < entities.size(); i++) {
					Collections.sort(entities.get(i).mentions);
					Entity c2 = entities.get(i);
					if (allPronoun(c2)) {
						continue;
					}
					ArrayList<String> allLines = new ArrayList<String>();
					// TODO
					ArrayList<Integer> antecedentClusters = new ArrayList<Integer>();

					boolean coref = false;

					for (int j = 0; j < i; j++) {
						Entity c1 = entities.get(j);
						if (allPronoun(c1)) {
							continue;
						}

						StringBuilder sb = new StringBuilder();

						String feature = cf.getClusterFeatures(c1, c2, part);
						sb.append("1 qid:").append(qid).append(" ").append(feature);
						allLines.add(sb.toString());
						antecedentClusters.add(j);

						int c1EntityIdx = ruleCoref.goldMentions.get(c1.mentions.get(0).toName()).entityIndex;
						int c2EntityIdx = ruleCoref.goldMentions.get(c2.mentions.get(0).toName()).entityIndex;
						if (c1EntityIdx == c2EntityIdx) {
							coref = true;
						}
					}
					// add own feature
					StringBuilder sb = new StringBuilder();
					String ownFeature = cf.getOwnFeature(c2, part);

					if (coref) {
						// ownFeature = " 1:1 " + ownFeature;
					}

					sb.append("1 qid:").append(qid).append(" ").append(ownFeature);
					allLines.add(sb.toString());
					antecedentClusters.add(i);
					qid++;

					// reranking
					double maxRank = -1;
					int anteCluster = -1;
					for (int m = 0; m < allLines.size(); m++) {
						String line = allLines.get(m);
						// svm classify
						double rank = getRank(line);
						// double rank = 0;

						StringBuilder ssb = new StringBuilder();
						ssb.append(rank + "#" + line);

						if (ruleCoref.goldMentions.get(entities.get(i).mentions.get(0).toName()).entityIndex == ruleCoref.goldMentions
								.get(entities.get(antecedentClusters.get(m)).mentions.get(0).toName()).entityIndex
								&& i != antecedentClusters.get(m)) {
							ssb.append("  " + entities.get(antecedentClusters.get(m)).print() + " # "
									+ entities.get(i).print());
						}

						System.out.println(ssb.toString());
						if (rank >= maxRank) {
							maxRank = rank;
							anteCluster = antecedentClusters.get(m);
						}
					}
					if (anteCluster != i) {
						System.err.println("MERGE");
						entities.get(anteCluster).mentions.addAll(entities.get(i).mentions);
						entities.remove(i);
						i--;
					} else {
						// System.err.println("NOT MERGE!!!!!!!");
					}
				}
				System.err.println(entities.size());
				// ruleCoref.printRecallError(entities, goldEntities);
				outputEntities(entities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.system");
				outputEntities(goldEntities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.gold");
			}
			// System.out.println(document.getType());
		}
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		a2[1] = "system";
		OntoAltafToSemEvalOffical.runOutputKey(a2);
	}

	static SVMLightModel model;

	public static void initSVMClassifier(String folder) throws Exception {
		model = SVMLightModel.readSVMLightModelFromURL(new java.io.File(
				"/users/yzcchen/tool/JNI_SVM-light-6.01/src/svmlight-6.01/model." + folder).toURL());

	}

	public static double getRank(String featureLine) {
		double label = 0;
		String tokens[] = featureLine.split("\\s+");
		int size = tokens.length - 2;
		int dims[] = new int[size];
		double vals[] = new double[size];
		int g = 0;
		for (int i = 2; i < tokens.length; i++) {
			String token = tokens[i];
			int p = token.indexOf(":");
			int dim = Integer.parseInt(token.substring(0, p));
			int val = Integer.parseInt(token.substring(p + 1));
			dims[g] = dim;
			vals[g] = val;
			g++;
		}
		LabeledFeatureVector feaVector = new LabeledFeatureVector(label, dims, vals);
		return model.classify(feaVector);
	}

	public static void runTrain(String folder) throws IOException, Exception {
		String sur = "_open";
		String outputFolder = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_" + mode + "/");
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		ChCommon.loadPredictNE(folder, mode);

		CoNLLDocument goldDocument;

		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");

		int qid = 1;
		ArrayList<String> allLines = new ArrayList<String>();

		double good = 0;
		double bad = 0;

		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);

			CoNLLDocument document = new CoNLLDocument(conllFn);

			goldDocument = new CoNLLDocument(conllFn.replace("test", "test_gold"));

			for (int k = 0; k < document.getParts().size(); k++) {
				goldPart = goldDocument.getParts().get(k);
				fofFw2.write(conllFn + "_" + k + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "chinese";
				ruleCoref.folder = folder;
				for (Sieve sieve : sieves) {
					ruleCoref.currentSieve = sieve;
					sieve.act(ruleCoref);
				}

				ArrayList<Entity> entities = ruleCoref.systemChain;
				System.err.println(entities.size());

				ArrayList<Entity> goldEntities = ruleCoref.goldChain;

				Collections.sort(entities);

				boolean merge = true;
				if (merge) {
					for (int i = 0; i < entities.size(); i++) {
						Entity c2 = entities.get(i);
						if (allPronoun(c2) || i == 0) {
							continue;
						}
						// TODO
						boolean coref = false;
						for (int j = 0; j < i; j++) {
							Entity c1 = entities.get(j);
							if (allPronoun(c1)) {
								continue;
							}
							int c1EntityIdx = ruleCoref.goldMentions.get(c1.mentions.get(0).toName()).entityIndex;
							int c2EntityIdx = ruleCoref.goldMentions.get(c2.mentions.get(0).toName()).entityIndex;

							StringBuilder sb = new StringBuilder();

							if (c1EntityIdx == c2EntityIdx) {
								// System.err.println("Merge:");

								c1.mentions.addAll(c2.mentions);
								entities.remove(i);
								i--;
								sb.append("2 ");
								coref = true;
							} else {
								// System.err.println("NOT Merge:");
								sb.append("1 ");
							}
							StringBuilder s = new StringBuilder();
							for (EntityMention m : c1.mentions) {
								s.append(m.source).append(" ");
							}
							s.append(" ### ");
							for (EntityMention m : c2.mentions) {
								s.append(m.source).append(" ");
							}
							// System.out.println(s.toString());

							String feature = cf.getClusterFeatures(c1, c2, part);
							sb.append("qid:").append(qid).append(" ").append(feature);
							allLines.add(sb.toString());
							// System.out.println(sb.toString());
						}
						// add own feature
						String ownFeature = cf.getOwnFeature(c2, part);
						StringBuilder sb = new StringBuilder();
						if (coref) {
							sb.append("1 ");
							// ownFeature = " 1:1 " + ownFeature;
							good++;
						} else {
							sb.append("2 ");
							bad++;
						}

						sb.append("qid:").append(qid).append(" ").append(ownFeature);
						// System.out.println(sb.toString());
						allLines.add(sb.toString());
						qid++;
					}
				}
				System.err.println(entities.size());
				// ruleCoref.printRecallError(entities, goldEntities);
				outputEntities(entities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.system");
				outputEntities(goldEntities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.gold");
			}
			// System.out.println(document.getType());
		}
		System.out.println(allLines.size());
		Common.outputLines(allLines, mode + "Rk." + folder);
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/chinese/" + folder + "_" + mode + sur + "/";
		a2[1] = "system";
		OntoAltafToSemEvalOffical.runOutputKey(a2);

		System.out.println(good / (bad + good));
	}

	public static boolean allPronoun(Entity c) {
		for (EntityMention m : c.mentions) {
			if (!m.isPronoun) {
				return false;
			}
		}
		return true;
	}

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

	public HashMap<EntityMention, HashSet<EntityMention>> getPairWise(ArrayList<Entity> entities) {
		HashMap<EntityMention, HashSet<EntityMention>> maps = new HashMap<EntityMention, HashSet<EntityMention>>();
		for (Entity entity : entities) {
			ArrayList<EntityMention> ems = entity.mentions;
			Collections.sort(ems);
			for (int i = 0; i < ems.size(); i++) {
				EntityMention current = ems.get(i);
				HashSet<EntityMention> candidates = new HashSet<EntityMention>();
				for (int j = 0; j < ems.size(); j++) {
					if (j != i) {
						candidates.add(ems.get(j));
					}
				}
				maps.put(current, candidates);
			}
		}
		return maps;
	}
}
