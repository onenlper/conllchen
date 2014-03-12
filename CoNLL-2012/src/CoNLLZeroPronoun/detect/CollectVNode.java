package CoNLLZeroPronoun.detect;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.ParseTreeMention;
import mentionDetection.chinese.ChineseMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common;
import CoNLLZeroPronoun.coref.RuleZeroCoref;
import CoNLLZeroPronoun.coref.ZeroCoref;
import CoNLLZeroPronoun.coref.ZeroCorefFea;

public class CollectVNode extends ZeroCoref {

	public static void main(String args[]) {

		// CollectVNode n = new CollectVNode();
		// n.collect(args[0]);

		// process();
		// filter();
		// (new CollectVNode()).collect(args[0]);
		// collectNPSemantics();
//		process4();
		process5();
	}

	public static void collectNPSemantics() {
		ArrayList<String> lines = new ArrayList<String>();
		lines.addAll(Common.getLines("chinese_list_all_train"));
		lines.addAll(Common.getLines("chinese_list_all_development"));
		ChCommon chCommon = new ChCommon("chinese");
		ChineseMention.goldNE = true;
		for (String line : lines) {
			String l = line.replace("auto_conll", "gold_conll");
			System.out.println(l);
			CoNLLDocument document = new CoNLLDocument(l);
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);
			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> chains = part.getChains();
				for (Entity chain : chains) {
					boolean zeroE = false;
					for (EntityMention m : chain.mentions) {
						if (m.end == -1) {
							zeroE = true;
							break;
						}
					}
					if (zeroE) {
						for (EntityMention m : chain.mentions) {
							if (m.end == -1) {
								continue;
							}
							chCommon.assignHeadExtent(m, part);
							String head = m.head;
							m.ner = part.getWord(m.end).rawNamedEntity;
							if (m.ner.equals("PERSON")) {
								head = "他";
							}

							if ((m.ner.equals("TIME") || m.ner.equals("DATE"))) {
								head = "那时";
							}

							if (m.ner.equals("LAW")) {
								head = "法律";
							}

							if (m.ner.equals("ORG")) {
								head = "公司";
							}

							if (m.ner.equals("LOC")) {
								head = "那里";
							}

							if (m.ner.equals("PRODUCT")) {
								head = "产品";
							}

							if (m.ner.equals("WORK_OF_ART")) {
								head = "书";
							}

							if (m.ner.equals("NORP")) {
								head = "人们";
							}

							if (m.ner.equals("LANGUAGE")) {
								head = "语言";
							}

							if (m.ner.equals("EVENT")) {
								head = "事件";
							}

							if (m.ner.equals("FAC")) {
								head = "部门";
							}

							if (m.ner.equals("GPE")) {
								head = "城市";
							}

							if (Common.getSemantic(head) == null && m.ner.equals("OTHER")) {
								head = head.substring(head.length() - 1);
							}

							if (Common.getSemantic(head) != null) {
								String sem = Common.getSemantic(head)[0];
								if (!sem.startsWith("A") && !sem.startsWith("D") && !sem.startsWith("B")
										&& !sem.startsWith("Cb") && !sem.startsWith("0")) {
									System.out.println(sem + "#" + head + "#" + m.ner + "# " + m.source);
								}
								String l1 = sem.substring(0, 1);
								String l2 = sem.substring(0, 2);
								String l3 = sem.substring(0, 4);
								String l4 = sem.substring(0, 5);
								String l5 = sem.substring(0, 7);
								String l6 = head;
							}
						}
					}
				}
			}
		}

	}

	public static void filter() {
		HashMap<String, Integer> gram2 = Common.readFile2Map("gram2detail.all2");
		System.out.println("Read in2");

		HashMap<String, Integer> gram3 = Common.readFile2Map("gram3detail.all2");
		System.out.println("Read in3");

		HashMap<String, Integer> gram2New = new HashMap<String, Integer>();
		HashMap<String, Integer> gram3New = new HashMap<String, Integer>();

		HashMap<String, String> posMap = Common.readFile2Map2("POS.txt", '\t');

		HashSet<String> keepPOS = new HashSet<String>(Arrays.asList("f", "j", "m", "n", "q", "r", "s", "Ng", "Tg",
				"nr", "ns", "nt", "nz"));

		filter(gram2, gram2New, posMap, keepPOS);
		Common.outputHashMap(gram2New, "gram2detail.all.new2");

		filter(gram3, gram3New, posMap, keepPOS);
		Common.outputHashMap(gram3New, "gram3detail.all.new2");
	}

	private static void filter(HashMap<String, Integer> gram2, HashMap<String, Integer> gram2New,
			HashMap<String, String> posMap, HashSet<String> keepPOS) {
		for (String key : gram2.keySet()) {
			int k = key.indexOf('\t');
			String gram = key.substring(k + 1).split("\\s+")[1];
			boolean keep = true;
			String sems[] = Common.getSemantic(gram);
			if (sems != null) {
				for (String sem : sems) {
					if (sem.startsWith("E") || sem.startsWith("F") || sem.startsWith("G") || sem.startsWith("H")
							|| sem.startsWith("I") || sem.startsWith("J") || sem.startsWith("K") || sem.startsWith("L")) {
						keep = false;
					}
				}
			} else {
				keep = false;
			}

			if (!keep) {
				System.out.println(gram);
			} else {
				gram2New.put(key, gram2.get(key));
			}
		}
	}

	public static void process3() {
		ArrayList<String> gram3Lines = new ArrayList<String>();
		ArrayList<String> lines = Common.getLines("3-gram");
		for (String line : lines) {
			String tokens[] = line.split("\\s+");
			if (Common.getSemantic(tokens[1]) != null) {
				gram3Lines.add(line);
			}
		}
		lines.clear();
		Common.outputLines(gram3Lines, "3-gram.new");
		gram3Lines.clear();
		ArrayList<String> gram4Lines = new ArrayList<String>();
		lines = Common.getLines("4-gram");
		for (String line : lines) {
			String tokens[] = line.split("\\s+");
			if (Common.getSemantic(tokens[1]) != null) {
				gram4Lines.add(line);
			}
		}
		lines.clear();
		Common.outputLines(gram4Lines, "4-gram.new");
	}

	public static void process2() {
		String folder2 = "/users/yzcchen/chen2/5-gram/trigram";
		String folder3 = "/users/yzcchen/chen3/5-gram/4-gram";
		int k = (new File(folder2)).listFiles().length;
		ArrayList<String> gram3Lines = new ArrayList<String>();
		for (File file : (new File(folder2)).listFiles()) {
			System.out.println(file.getAbsolutePath() + " " + (k--));
			ArrayList<String> lines = Common.getLines(file.getAbsolutePath());
			for (String line : lines) {
				if (line.startsWith("<S> ")) {
					gram3Lines.add(line);
				}
			}
			lines.clear();
		}
		Common.outputLines(gram3Lines, "3-gram");
		gram3Lines.clear();
		k = (new File(folder3)).listFiles().length;
		ArrayList<String> gram4Lines = new ArrayList<String>();
		for (File file : (new File(folder3)).listFiles()) {
			ArrayList<String> lines = Common.getLines(file.getAbsolutePath());
			System.out.println(file.getAbsolutePath() + " " + (k--));
			for (String line : lines) {
				if (line.startsWith("<S> ")) {
					gram4Lines.add(line);
				}
			}
			lines.clear();
		}
		Common.outputLines(gram4Lines, "4-gram");
	}
	
	public static void process5() {
		ArrayList<String> gram2 = Common.getLines("gram2detail.allY");
		ArrayList<String> gram3 = Common.getLines("gram3detail.allY");

		ArrayList<String> gram2out = new ArrayList<String>();
		ArrayList<String> gram3out = new ArrayList<String>();

		for (String line : gram2) {
			int p = line.indexOf('\t');
			String tokens[] = line.substring(p+1).split("\\s+");
			String sems[] = Common.getSemantic(tokens[1]);
			boolean keep = true;
			if (sems != null) {
				for (String sem : sems) {
					if (sem.startsWith("E") || sem.startsWith("F") || sem.startsWith("G") || sem.startsWith("H")
							|| sem.startsWith("I") || sem.startsWith("J") || sem.startsWith("K") || sem.startsWith("L")
							|| sem.startsWith("Ca")) {
						keep = false;
					}
				}
			} else {
				keep = false;
			}
			if(keep) {
				gram2out.add(line);
			}
			
		}
		Common.outputLines(gram2out, "gram2detail.allY.new");

		for (String line : gram3) {
			int p = line.indexOf('\t');
			String tokens[] = line.substring(p+1).split("\\s+");
			String sems[] = Common.getSemantic(tokens[1]);
			boolean keep = true;
			if (sems != null) {
				for (String sem : sems) {
					if (sem.startsWith("E") || sem.startsWith("F") || sem.startsWith("G") || sem.startsWith("H")
							|| sem.startsWith("I") || sem.startsWith("J") || sem.startsWith("K") || sem.startsWith("L")
							|| sem.startsWith("Ca")) {
						keep = false;
					}
				}
			} else {
				keep = false;
			}
			if(keep) {
				gram3out.add(line);
			}
		}
		Common.outputLines(gram3out, "gram3detail.allY.new");
	}
	

	public static void process4() {
		HashSet<String> gram2 = Common.readFile2Set("gram2.all");
		HashSet<String> gram3 = Common.readFile2Set("gram3.all");

		HashMap<String, Integer> gram2out = new HashMap<String, Integer>();
		HashMap<String, Integer> gram3out = new HashMap<String, Integer>();

//		HashMap<String, Integer> map = Common.readFile2Map("3-gram.new", '\t');
//		System.out.println(map.size());
//		int k = 0;
//		for (String key : map.keySet()) {
//			if(k%10000000==0) {
//				System.out.println(k);
//			}
//			k++;
//			String tokens[] = key.split("\\s+");
//			if (!tokens[0].equals("<S>")) {
//				continue;
//			}
//			int value = map.get(key);
//
//			String pattern = "* " + tokens[2];
//			if (gram2.contains(pattern)) {
//				gram2out.put(pattern + "\t" + key, value);
//			}
//		}
//		Common.outputHashMap(gram2out, "gram2detail.allY");
//
//		int k = 0;
//		HashMap<String, Integer> map = Common.readFile2Map("4-gram.new", '\t');
//		System.out.println(map.size());
//		for (String key : map.keySet()) {
//			if(k%10000000==0) {
//				System.out.println(k);
//			}
//			k++;
//			String tokens[] = key.split("\\s+");
//			if (!tokens[0].equals("<S>")) {
//				continue;
//			}
//			int value = map.get(key);
//			String pattern = "* " + tokens[2] + " " + tokens[3];
//			if (gram3.contains(pattern)) {
//				gram3out.put(pattern + "\t" + key, value);
//			}
//		}
//		Common.outputHashMap(gram3out, "gram3detail.allY");
	}

	public static void process() {
		HashSet<String> gram2 = Common.readFile2Set("gram2.all");
		HashSet<String> gram3 = Common.readFile2Set("gram3.all");

		HashMap<String, Integer> gram2out = new HashMap<String, Integer>();
		HashMap<String, Integer> gram3out = new HashMap<String, Integer>();

		String folder2 = "/users/yzcchen/chen2/5-gram/trigram";
		String folder3 = "/users/yzcchen/chen3/5-gram/4-gram";
		int k = (new File(folder2)).listFiles().length;
		for (File file : (new File(folder2)).listFiles()) {
			System.out.println(file.getAbsolutePath() + " " + (k--));
			HashMap<String, Integer> map = Common.readFile2Map(file.getAbsolutePath(), '\t');
			for (String key : map.keySet()) {
				String tokens[] = key.split("\\s+");
				if (!tokens[0].equals("<S>")) {
					continue;
				}
				int value = map.get(key);

				String pattern = "* " + tokens[2];
				if (gram2.contains(pattern)) {
					gram2out.put(pattern + "\t" + key, value);
				}
			}
			System.out.println(gram2out.size());
		}
		Common.outputHashMap(gram2out, "gram2detail.all2");

		k = (new File(folder3)).listFiles().length;
		for (File file : (new File(folder3)).listFiles()) {
			System.out.println(file.getAbsolutePath() + " " + (k--));
			HashMap<String, Integer> map = Common.readFile2Map(file.getAbsolutePath(), '\t');
			for (String key : map.keySet()) {
				String tokens[] = key.split("\\s+");
				if (!tokens[0].equals("<S>")) {
					continue;
				}
				int value = map.get(key);
				String pattern = "* " + tokens[2] + " " + tokens[3];
				if (gram3.contains(pattern)) {
					gram3out.put(pattern + "\t" + key, value);
				}
			}
			System.out.println(gram3out.size());
		}
		Common.outputHashMap(gram3out, "gram3detail.all2");
	}

	public void collect(String folder) {
		fea = new ZeroCorefFea(true, folder);

		ChCommon.loadPredictNE("all", "development");
		ArrayList<String> lines = new ArrayList<String>();
		lines.addAll(Common.getLines("chinese_list_" + folder + "_train"));
		lines.addAll(Common.getLines("chinese_list_all_development"));

		for (String line : lines) {

			CoNLLDocument document = new CoNLLDocument(line);

			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> goldChains = part.getChains();

				ParseTreeMention ptm = new ParseTreeMention();
				ArrayList<EntityMention> goldBoundaryNPMentions = ptm.getMentions(part);
				ArrayList<EntityMention> goldInChainZeroses = RuleZeroCoref.getAnaphorZeros(part.getChains());
				Collections.sort(goldInChainZeroses);

				this.assignVNode(goldInChainZeroses, part);

				for (EntityMention zero : goldInChainZeroses) {
					MyTreeNode V = zero.V;

					this.printZero(zero, part);

					System.out.println("Predicate: " + fea.getPredicate(zero.V));
					System.out.println("Object NP: " + fea.getObjectNP(zero));

					System.out.println("===");
				}

			}

		}
	}
}
