package ruleCoreference.english;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.OntoCommon;
import util.Common.Person;
import coref.OntoAltafToSemEvalOffical;

public class RuleCoref {

	public static ArrayList<Boolean> bs;

	public CoNLLPart part;

	public ArrayList<EntityMention> mentions;

	ArrayList<Entity> goldChain;

	ArrayList<Entity> systemChain;

	ArrayList<CoNLLSentence> sentences;

	String language;

	String folder;

	OntoCommon ontoCommon;

	HashSet<EntityMention> roleSets;

	public RuleCoref(CoNLLPart part) {
		roleSets = new HashSet<EntityMention>();
		// System.out.println(part.getDocument().getType());
		appoPairs.clear();
		this.part = part;
		goldChain = new ArrayList<Entity>();
		MentionDetect md = new ParseTreeMention();
		this.mentions = md.getMentions(part);
		this.goldChain = part.getChains();
		this.leftToRightSort(mentions);
		this.systemChain = new ArrayList<Entity>();
		this.sentences = part.getCoNLLSentences();
		int entityIdx = 0;
		this.ontoCommon = new OntoCommon("english");
		for (EntityMention em : mentions) {
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
		loadGoldMaps();
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
			if (ontoCommon.getEnDictionary().relativePronouns.contains(em.head.toLowerCase())) {
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

	public boolean checkCompatible(EntityMention antecedent, EntityMention em2, CoNLLPart part) {
		if (RuleCoref.bs.get(30) && antecedent.generic && antecedent.person == Person.YOU)
			return false;
		if (RuleCoref.bs.get(31) && em2.generic)
			return false;
		for (EntityMention em11 : antecedent.entity.mentions) {
			for (EntityMention em22 : em2.entity.mentions) {
				if(tuneSwitch) {
					if (RuleCoref.headPair == null) {
						RuleCoref.headPair = Common.readFile2Map5(language + "_" + folder + "_head_all");
					}
					if (RuleCoref.sourcePair == null) {
						RuleCoref.sourcePair = Common.readFile2Map5(language + "_" + folder + "_source_all");
					}
					String concat = Common.concat(em11.head.toLowerCase(), em22.head.toLowerCase());
					if (RuleCoref.headPair.containsKey(concat)) {
						double value = RuleCoref.headPair.get(concat);
						if (value <= t1) {
							return false;
						}
					}
					String concat2 = Common.concat(em11.source.toLowerCase(), em22.source.toLowerCase());
					if (RuleCoref.sourcePair.containsKey(concat2)) {
						double value = RuleCoref.sourcePair.get(concat2);
						if (value <= t2) {
							return false;
						}
					}
				}
				// us and US
				// if((em11.original.equals("us")&&em22.original.equals("US"))
				// || (em22.original.equals("us")&&em11.original.equals("US")))
				// {
				// return false;
				// }
				// discourse constraint
				if (!(this.currentSieve instanceof DiscourseProcessSieve)) {
					if (RuleCoref.bs.get(32) && part.getDocument().ontoCommon.isSpeaker(em11, em22, part)
							&& em11.person != Person.I && em22.person != Person.I) {
						return false;
					}
					int dist = Math.abs(part.getWord(em11.headStart).utterOrder
							- part.getWord(em22.headStart).utterOrder);
					if (RuleCoref.bs.get(33) && part.getDocument().getType() != DocType.Article && dist == 1
							&& !part.getDocument().ontoCommon.isSpeaker(em11, em22, part)) {
						if (em11.person == Person.I && em22.person == Person.I) {
							return false;
						}
						if (em11.person == Person.YOU && em22.person == Person.YOU) {
							return false;
						}
						if (em11.person == Person.WE && em22.person == Person.WE) {
							return false;
						}
					}
				}
				// in-with-in
				boolean iWithi = ontoCommon.isIWithI(em11, em22, sentences);
				if (iWithi && !ontoCommon.isCopular2(em11, em22, sentences)
						&& !ontoCommon.isRoleAppositive(em11, em22, sentences)) {
					return false;
				}
				// CC construct
				boolean cc1 = false;
				boolean cc2 = false;
				for (int k = em11.start; k <= em11.end; k++) {
					if (this.part.getWord(k).posTag.equalsIgnoreCase("cc")) {
						cc1 = true;
						break;
					}
				}
				for (int k = em22.start; k <= em22.end; k++) {
					if (this.part.getWord(k).posTag.equalsIgnoreCase("cc")) {
						cc2 = true;
						break;
					}
				}
				if (RuleCoref.bs.get(34) && (cc1 && !cc2) || (!cc1 && cc2) && em11.head.equalsIgnoreCase(em22.head)) {
					return false;
				}
				EntityMention longEM = em22;
				EntityMention shortEM = em11;
				if (em11.getWordSet().size() > em22.getWordSet().size()) {
					longEM = em11;
					shortEM = em22;
				}
				if (longEM.original.contains(" and ")) {
					for (String str : longEM.original.split(" and ")) {
						for (String str1 : str.split(",")) {
							if (RuleCoref.bs.get(35) && str1.trim().endsWith(shortEM.original)) {
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}

	/*
	 * mentions, all mentions in the same sentence mention, current mention
	 */
	// public ArrayList<EntityMention>
	// sortSameSentenceForPronoun(ArrayList<EntityMention> mentions,
	// EntityMention mention, CoNLLSentence sentence) {
	// ArrayList<EntityMention> mentionsCopy = new ArrayList<EntityMention>();
	// ArrayList<EntityMention> sortedMentions = new ArrayList<EntityMention>();
	// mentionsCopy.addAll(mentions);
	// for (int i = 0; i < mentionsCopy.size(); i++) {
	// EntityMention temp = mentionsCopy.get(i);
	// if (temp.compareTo(mention) >= 0) {
	// mentionsCopy.remove(i);
	// i--;
	// }
	// }
	// MyTreeNode root = sentence.getSyntaxTree().root;
	// ArrayList<MyTreeNode> allSprings = root.getDepthFirstOffsprings();
	// Collections.reverse(allSprings);
	// MyTreeNode sNode = getLowestSNode(mention.treeNode);
	// while (sNode != null) {
	// ArrayList<MyTreeNode> leaves = sNode.getLeaves();
	// int leftBound = sentence.getWord(leaves.get(0).leafIdx).index;
	// int rightBound = sentence.getWord(leaves.get(leaves.size() -
	// 1).leafIdx).index;
	// for (int j = 0; j < mentionsCopy.size(); j++) {
	// EntityMention tmp = mentionsCopy.get(j);
	// if (tmp.start >= leftBound && tmp.end <= rightBound) {
	// sortedMentions.add(tmp);
	// mentionsCopy.remove(j);
	// j--;
	// }
	// }
	// boolean find = false;
	// for (int i = 0; i < sNode.childIndex; i++) {
	// if (sNode.parent.children.get(i).value.startsWith("S")) {
	// sNode = sNode.parent.children.get(i);
	// find = true;
	// break;
	// }
	// }
	// if (!find) {
	// sNode = this.getLowestSNode(sNode);
	// }
	// }
	// return sortedMentions;
	// }
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
	 * find the lowest S tree node, otherwise, return root
	 */
	private MyTreeNode getLowestSNode(MyTreeNode node) {
		if (node.value.equalsIgnoreCase("TOP")) {
			return null;
		}
		ArrayList<MyTreeNode> ancestors = node.getAncestors();
		for (int i = ancestors.size() - 2; i >= 0; i--) {
			if (ancestors.get(i).value.startsWith("S")) {
				return ancestors.get(i);
			}
		}
		return ancestors.get(0);
	}

	public boolean combine2Entities(EntityMention antecedent, EntityMention em2, ArrayList<CoNLLSentence> sentenenses) {
		HashSet<EntityMention> predictMentions = new HashSet<EntityMention>();
		predictMentions.addAll(this.mentions);
		if (!checkCompatible(antecedent, em2, part)) {
			return false;
		}
		if ((antecedent.original.equalsIgnoreCase("this") || antecedent.original.equalsIgnoreCase("these"))
				&& Math.abs(antecedent.position[0] - em2.position[0]) > 3) {
			return false;
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
		if (this.goldMentions.contains(em2) && this.goldMentions.contains(antecedent)) {
			if (this.goldMaps.get(em2).contains(antecedent)) {
				// System.out.println("++++++" + antecedent + " ^" +
				// this.part.getWord(antecedent.start).speaker
				// + "^#" + em2 + " ^" +
				// this.part.getWord(antecedent.start).speaker + "^@" +
				// this.currentSieve.getClass().getCanonicalName());
			} else {
				// System.out.println("==========P1===========");
				// System.out.println("mention:\t" + em2 + " " +
				// this.part.getWord(em2.start).speaker);
				// System.out.println("antecedent:\t" + antecedent + " " +
				// this.part.getWord(antecedent.start).speaker);
				// System.out.println("gold ante:\t" + trueAntecedent + " " +
				// (trueAntecedent==null?"":this.part.getWord(trueAntecedent.start).speaker));
				// System.out.println("sieve:\t\t" +
				// this.currentSieve.getClass().getName());
				// System.out.println(part.getDocument().getFilePath() + " " +
				// part.getPartID());
			}
		} else {
			// System.out.println("==========P2===========");
			// System.out.println("mention:\t" + em2 + " " +
			// this.part.getWord(em2.start).speaker);
			// System.out.println("antecedent:\t" + antecedent + " " +
			// this.part.getWord(antecedent.start).speaker);
			// System.out.println("gold ante:\t" + trueAntecedent + " " +
			// (trueAntecedent==null?"":this.part.getWord(trueAntecedent.start).speaker));
			// System.out.println("sieve:\t\t" +
			// this.currentSieve.getClass().getName());
			// System.out.println(part.getDocument().getFilePath() + " " +
			// part.getPartID());
		}
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
							// System.out.println(part.label);
							break;
						}
					} else if (predictMaps.get(mention) != null && predictMaps.get(mention).contains(antecedent)) {
						break;
					}
				}
			}

		}
	}

	public EntityMention getMentionFromSet(HashSet<EntityMention> sets, EntityMention mention) {
		for (EntityMention m : sets) {
			if (m.equals(mention)) {
				return m;
			}
		}
		return null;
	}

	public static ArrayList<Sieve> sieves;

	public static ArrayList<ArrayList<Element>> nerses;

	public static HashMap<String, ArrayList<EntityMention>> allMentions;

	public void outputEntities(ArrayList<Entity> entities, String path, boolean gold) {
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
					if (!gold && this.roleSets.contains(em)) {
						continue;
					}
					sb.append(em.start).append(",").append(em.end).append(" ");
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

		Sieve discourseProcessSieve = new DiscourseProcessSieve();
		sieves.add(discourseProcessSieve);
		//
		Sieve exactMatchSieve = new ExactMatchSieve();
		sieves.add(exactMatchSieve);

		Sieve relaxStringExactMatch = new RelaxStringExactMatch();
		sieves.add(relaxStringExactMatch);
		// // //
		Sieve preciseConstructSieve = new PreciseConstructSieve();
		sieves.add(preciseConstructSieve);

		Sieve strictHeadMatchSieve = new StrictHeadMatch1Sieve();
		sieves.add(strictHeadMatchSieve);

		Sieve strictHeadMatch2Sieve = new StrictHeadMatch2Sieve();
		sieves.add(strictHeadMatch2Sieve);

		Sieve strictHeadMatch3Sieve = new StrictHeadMatch3Sieve();
		sieves.add(strictHeadMatch3Sieve);

		Sieve strictHeadMatch4Sieve = new StrictHeadMatch4Sieve();
		sieves.add(strictHeadMatch4Sieve);
		// 
		Sieve relaxedHeadMatchSieve = new RelaxedHeadMatchSieve(); //
		sieves.add(relaxedHeadMatchSieve);

		// //
		Sieve pronounSieve = new PronounSieve();
		sieves.add(pronounSieve);

		if(tuneSwitch) {
			Sieve stringPairSieve = new StringPairSieve();
			sieves.add(stringPairSieve);
		}
	}

	public Sieve currentSieve;

	public static double t1;
	public static double t2;
	public static double t3;
	public static double t4;

	public static void main(String args[]) throws Exception {
		ArrayList<Boolean> bs2 = new ArrayList<Boolean>();
		String args2[];
		if(args[0].equals("load")) {
			// load test folder 
			args2 = new String[7];
			tuneSwitch = true;
			ArrayList<String> twoLine = Common.getLines("english_"+args[2] + "_opt");
			String tokens[] = twoLine.get(0).split("\\s+");
			args2[0] = args[1];// test or development
			args2[1] = args[2];// folder
			args2[2] = tokens[2];
			args2[3] = tokens[3];
			args2[4] = tokens[4];
			args2[5] = tokens[5];
			args2[6] = tokens[6];
			tokens = twoLine.get(1).split("\\s+");
			for(String token : tokens) {
				bs2.add(new Boolean(token));
			}
		} else {
			args2 = args;
			for (int i = 0; i < 36; i++) {
				bs2.add(new Boolean(true));
			}
		}
		
		run(args2, bs2, "");
	}

	public static boolean tuneSwitch = false;

	public static void run(String[] args, ArrayList<Boolean> bs2, String sur) throws Exception {
		bs = bs2;
		
		if (args.length < 2) {
			System.out.println("java ~ [development|test] folder");
			return;
		}
		if (args.length > 2) {
			tuneSwitch = true;
		}
		String mode = args[0];
		String folder = args[1];
		if (tuneSwitch) {
			t1 = Double.valueOf(args[2]);
			t2 = Double.valueOf(args[3]);
			t3 = Double.valueOf(args[4]);
			t4 = Double.valueOf(args[5]);
			ParseTreeMention.t5 = Double.valueOf(args[6]);
			ParseTreeMention.stats = Common.readFile2Map5("english_" + folder + "_mention");
		}
		String outputFolder = "/users/yzcchen/chen3/conll12/english/" + folder + "_" + args[0] + sur + "/";
		ArrayList<String> files = Common.getLines("english_list_" + folder + "_" + args[0]);
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		// System.out.println(files.size());
		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			boolean skip = false;
			// skip = true;
			String conllFn = files.get(fileIdx);
			// if(!conllFn.contains("nw/wsj/24/wsj_2454") && skip) {
			// continue;
			// }
			int a = conllFn.lastIndexOf(File.separator);
			int b = conllFn.lastIndexOf(".");
			String stem = conllFn.substring(a + 1, b);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			System.out.println(document.getFilePath());
//			System.out.println(conllFn);
			for (int k = 0; k < document.getParts().size(); k++) {
				// if(k!=0 && skip) {
				// continue;
				// }
				// System.out.println(outputFolder + stem + "_" + k);
				fofFw2.write(conllFn + "_" + k + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "english";
				ruleCoref.folder = folder;
				for (Sieve sieve : sieves) {
					ruleCoref.currentSieve = sieve;
					sieve.act(ruleCoref);
				}
				ArrayList<Entity> entities = ruleCoref.systemChain;
				ArrayList<Entity> goldEntities = ruleCoref.goldChain;
				ruleCoref.outputEntities(entities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.system", false);
				ruleCoref.outputEntities(goldEntities, outputFolder + document.getDocumentID().replace("/", "-") + "_"
						+ k + ".entities.gold", true);
				outputAppositive(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + ".appos");
				ruleCoref.printRecallError(entities, goldEntities);
			}
		}
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();

		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/english/" + folder + "_" + mode + sur + "/";
		a2[1] = "system";
		OntoAltafToSemEvalOffical.runOutputKey(a2);
	}

	public static void outputAppositive(String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (EntityMention em : appoPairs.keySet()) {
				StringBuilder sb = new StringBuilder();
				EntityMention em2 = appoPairs.get(em);
				sb.append(em.start).append(",").append(em.end).append(" ").append(em2.start).append(",")
						.append(em2.end);
				fw.write(sb.toString() + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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
				goldMentions.add(em);
			}
		}
	}

	public HashSet<EntityMention> goldMentions = new HashSet<EntityMention>();

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
