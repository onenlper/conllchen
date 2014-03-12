package ruleCoreference.arabic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.GoldMention;
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
import util.ArCommon;
import util.Common;
import util.Common.Person;
import coref.OntoAltafToSemEvalOffical;

public class RuleCoref {

	CoNLLPart part;

	public ArrayList<EntityMention> mentions;

	ArrayList<Entity> goldChain;

	ArrayList<Entity> systemChain;

	ArrayList<CoNLLSentence> sentences;

	String language;

	String folder;

	ArCommon ontoCommon;

	HashSet<EntityMention> goldMentions = new HashSet<EntityMention>();

	public RuleCoref(CoNLLPart part) {
		ontoCommon = new ArCommon("arabic");
		appoPairs.clear();
		this.part = part;
		goldChain = new ArrayList<Entity>();
		MentionDetect md = new ParseTreeMention();
		MentionDetect md2 = new GoldMention();
		this.goldMentions.addAll(md2.getMentions(part));
		this.mentions = md.getMentions(part);
		this.goldChain = part.getChains();
		Collections.sort(mentions);
		this.systemChain = new ArrayList<Entity>();
		this.sentences = part.getCoNLLSentences();
		int entityIdx = 0;
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
//		if (em.isPronoun && em.entity.mentions.size() == 1) {
//			ArrayList<EntityMention> ems = sentenceMentions.get(sentenceId);
//			this.leftToRightSort(ems);
//			ems = sortSameSentenceForPronoun(ems, em, part.getCoNLLSentences().get(sentenceId));
//			for (int i = 0; i < ems.size(); i++) {
//				if (ems.get(i).compareTo(em) >= 0) {
//				} else {
//					antecedents.add(ems.get(i));
//				}
//			}
//			for (int i = sentenceId - 1; i >= 0; i--) {
//				ArrayList<EntityMention> tempEMs = sentenceMentions.get(i);
//				if (tempEMs != null) {
//					this.leftToRightSort(tempEMs);
//					antecedents.addAll(tempEMs);
//				}
//			}
//		} else {
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
//		}
		Collections.sort(antecedents);
		Collections.reverse(antecedents);
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
						//system.out.println("NEGATIVE");
						return false;
					}
				}
				String concat2 = Common.concat(em11.original, em22.original);
				if (RuleCoref.sourcePair.containsKey(concat2)) {
					double value = RuleCoref.sourcePair.get(concat2);
					if (value <= t2) {
						//system.out.println("NEGATIVE");
						return false;
					}
				}
				// in-with-in
				// number mismatch
				// 很多小朋友 ANIMATE PLURAL MALE OTHER 54:647,648#小朋友
				EntityMention longM = (em11.end-em11.start)>(em22.end-em22.start)?em11:em22;
				EntityMention shortM = (em11.end-em11.start)<=(em22.end-em22.start)?em11:em22;
				// discourse constraint
				if (!(this.currentSieve instanceof DiscourseProcessSieve)) {
					if (part.getDocument().ontoCommon.isSpeaker(em11, em22, part) && em11.person != Person.I
							&& em22.person != Person.I) {
						return false;
					}
					String mSpeaker = part.getWord(em11.headStart).speaker;
					String antSpeaker = part.getWord(em22.headStart).speaker;

					int dist = Math.abs(part.getWord(em11.headStart).utterOrder
							- part.getWord(em22.headStart).utterOrder);
					if (part.getDocument().getType() != DocType.Article && dist == 1
							&& !part.getDocument().ontoCommon.isSpeaker(em11, em22, part)) {
						if (em11.person == Person.I && em22.person == Person.I) {
							return false;
						}
						if (em11.person == Person.YOU && em22.person == Person.YOU) {
							return false;
						}
						if (em11.person == Person.YOUS && em22.person == Person.YOUS) {
							return false;
						}
						if (em11.person == Person.WE && em22.person == Person.WE) {
							return false;
						}
					}
				}
				//cc
				if(longM.headStart==shortM.headStart && shortM.start>0 && part.getWord(shortM.start-1).posTag.equals("CC")) {
					return false;
				}
				// corpus construct
				if (ontoCommon.isCopular2(antecedent, em2, sentences)) {
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
								// //system.out.println(head1 + " " + head2 + " "
								// + em11.ner);
								return false;
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
		if (node.getAncestors().size()==1) {
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

	public boolean combine2Entities(EntityMention antecedent, EntityMention em2, ArrayList<CoNLLSentence> sentences) {
		HashSet<EntityMention> predictMentions = new HashSet<EntityMention>();
		predictMentions.addAll(this.mentions);
		if (!checkCompatible(antecedent, em2, sentences)) {
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
			if (this.getMentionFromSet(predictMentions, antecedent) != null) {
				trueAntecedent = this.getMentionFromSet(predictMentions, trueAntecedent);
			}
		}
		if (this.goldMentions.contains(em2) && this.goldMentions.contains(antecedent)) {
			if (this.goldMaps.get(em2).contains(antecedent)) {
//				//system.out.println("==========RIGHT===========");
//				//system.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
//				//system.out.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
//				//system.out.println("gold ante:\t" + trueAntecedent + " " + (trueAntecedent==null?"":this.part.getWord(trueAntecedent.start).speaker));
//				//system.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
//				//system.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
			} else {
				System.out.println("==========P1===========");
				System.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
				System.out.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
				System.out.println("gold ante:\t" + trueAntecedent + " " + (trueAntecedent==null?"":this.part.getWord(trueAntecedent.start).speaker));
				System.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
				System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
			}
		} else {
			System.out.println("==========P2===========");
			System.out.println("mention:\t" + em2 + " " + this.part.getWord(em2.start).speaker);
			System.out.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
			System.out.println("gold ante:\t" + trueAntecedent + " " + (trueAntecedent==null?"":this.part.getWord(trueAntecedent.start).speaker));
			System.out.println("sieve:\t\t" + this.currentSieve.getClass().getName());
			System.out.println(part.getDocument().getFilePath() + " " + part.getPartID());
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
//							mention = this.getMentionFromSet(predictMentions, mention);
//							//system.out.println("==========Recall=========");
//							//system.out.println("mention:\t" + mention + " " + this.part.getWord(mention.start).speaker);
//							//system.out.println("antecedent:\t" + antecedent + " " + this.part.getWord(antecedent.start).speaker);
//							//system.out.println("system ante:\t" + mention.antecedent + " " + (mention.antecedent==null?"":this.part.getWord(mention.antecedent.start).speaker));
//							//system.out.println(part.label);
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
					// //system.out.println("#########################################");
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static HashMap<String, Double> headPair;

	static HashMap<String, Double> sourcePair;

	public static HashMap<EntityMention, EntityMention> appoPairs = new HashMap<EntityMention, EntityMention>();

	public static void loadSieves() {
		sieves = new ArrayList<Sieve>();

		Sieve exactMatchSieve = new ExactMatchSieve();
		sieves.add(exactMatchSieve);

		 Sieve stringPairSieve = new StringPairSieve();
		 sieves.add(stringPairSieve);
	}

	public Sieve currentSieve;

	public static double t1 = -1;
	public static double t2 = -1;
	public static double t3 = 2;
	public static double t4 = 2;
	public static double t5 = -1;

	public static void main(String args[]) throws Exception {
		String args2[];
		// load test folder open 
		if (args[0].equals("load")) {
			ArrayList<String> twoLine = Common.getLines("arabic_"+args[2]+ "_opt");
			args2 = new String[7];
			String tokens[] = twoLine.get(0).split("\\s+");
			args2[0] = args[1];// test or development
			args2[1] = args[2];// folder
			args2[2] = tokens[2];
			args2[3] = tokens[3];
			args2[4] = tokens[4];
			args2[5] = tokens[5];
			args2[6] = tokens[6];
			tokens = twoLine.get(1).split("\\s+");
		} else {
			if (args.length < 2) {
				//system.out.println("java ~ development folder");
				return;
			}
			args2 = args;
		}
		run(args2,  "");
	}

	public static HashMap<String, Double> mention_stats = new HashMap<String, Double>();
	public static void run(String[] args, String sur) throws IOException, Exception {
		String folder = args[1];
		if (args.length != 2) {
			t1 = Double.valueOf(args[2]);
			t2 = Double.valueOf(args[3]);
			t3 = Double.valueOf(args[4]);
			t4 = Double.valueOf(args[5]);
			t5 = Double.valueOf(args[6]);
			mention_stats = Common.readFile2Map5("arabic_" + folder + "_mention");
		}
		String outputFolder = "/users/yzcchen/chen3/conll12/arabic/" + folder + "_" + args[0] + sur + "/";
		ArrayList<String> files = Common.getLines("arabic_list_" + folder + "_" + args[0]);
		if (!(new File(outputFolder).exists())) {
			(new File(outputFolder)).mkdir();
		}
		loadSieves();
		//system.out.println(files.size());
		FileWriter fofFw2 = new FileWriter(outputFolder + File.separator + "all.txt2");
		FileWriter fofFw = new FileWriter(outputFolder + File.separator + "all.txt");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			//system.out.println(conllFn);
			int a = conllFn.lastIndexOf(File.separator);
			int b = conllFn.lastIndexOf(".");
			String stem = conllFn.substring(a + 1, b);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			for (int k = 0; k < document.getParts().size(); k++) {
 				//system.out.println(outputFolder + stem + "_" + k);
				fofFw2.write(conllFn + "_" + k + "\n");
				fofFw.write(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + "\n");
				CoNLLPart part = document.getParts().get(k);
				RuleCoref ruleCoref = new RuleCoref(part);
				ruleCoref.language = "arabic";
				ruleCoref.folder = folder;
				if (RuleCoref.headPair == null) {
					RuleCoref.headPair = Common.readFile2Map5(ruleCoref.language + "_" + folder + "_head_all");
				}
				if (RuleCoref.sourcePair == null) {
					RuleCoref.sourcePair = Common.readFile2Map5(ruleCoref.language + "_" + folder + "_source_all");
				}
				for (Sieve sieve : sieves) {
					ruleCoref.currentSieve = sieve;
					//system.out.println("before");
					sieve.act(ruleCoref);
					//system.out.println("after");
				}
				ArrayList<Entity> entities = ruleCoref.systemChain;
				ArrayList<Entity> goldEntities = ruleCoref.goldChain;
				outputEntities(entities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.system");
				outputEntities(goldEntities, outputFolder + document.getDocumentID().replace("/", "-") + "_" + k
						+ ".entities.gold");
				outputAppositive(outputFolder + document.getDocumentID().replace("/", "-") + "_" + k + ".appos");
				ruleCoref.printRecallError(entities, goldEntities);
			}
			//system.out.println(document.getType());
		}
		System.out.println(outputFolder);
		fofFw2.close();
		fofFw.close();
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/arabic/" + folder + "_" + args[0] + sur + "/";
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
