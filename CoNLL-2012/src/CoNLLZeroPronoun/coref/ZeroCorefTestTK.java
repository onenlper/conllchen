package CoNLLZeroPronoun.coref;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import util.ChCommon;
import util.Common;
import CoNLLZeroPronoun.detect.ZeroDetectTest;

public class ZeroCorefTestTK extends ZeroCoref {

	String folder;

	public ZeroCorefTestTK(String folder) {
		this.folder = folder;
		String f = this.folder;
		f = "all";
		fea = new ZeroCorefFea(false, "zeroCoref" + f);
	}

	public void test() {
		// Common.outputLines(this.getTestInstances(), "zeroCorefTrain." +
		// this.folder);
	}

	double good = 0;
	double bad = 0;

	double a = 0;
	double b = 0;

	public void getTestInstances() {
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_development");
		ChCommon.loadPredictNE("all", "development");

		// HashSet<String> gram2 = Common.readFile2Set("gram2.all");
		// HashSet<String> gram3 = Common.readFile2Set("gram3.all");

		ArrayList<ArrayList<EntityMention>> corefResults = new ArrayList<ArrayList<EntityMention>>();
		ArrayList<ArrayList<Entity>> goldEntities = new ArrayList<ArrayList<Entity>>();

		ZeroDetectTest zeroDetectTest = new ZeroDetectTest("all");

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace("auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> goldChains = part.getChains();

				goldEntities.add(goldChains);

				fea.part = part;
				HashMap<String, Integer> chainMap = formChainMap(goldChains);
				fea.chainMap = chainMap;

				ArrayList<EntityMention> corefResult = new ArrayList<EntityMention>();
				corefResults.add(corefResult);

				ParseTreeMention ptm = new ParseTreeMention();
				ArrayList<EntityMention> goldBoundaryNPMentions = ptm.getMentions(part);

				Collections.sort(goldBoundaryNPMentions);

				ArrayList<EntityMention> anaphorZeros = RuleZeroCoref.getAnaphorZeros(part.getChains());
				// anaphorZeros = zeroDetectTest.detectZeros(part, null);

				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				candidates.addAll(goldBoundaryNPMentions);

				if (!file.contains("/nw/") 
//						&& !file.contains("/mz/") && !file.contains("/wb/")
						) {
					// candidates.addAll(anaphorZeros);
				}
				Collections.sort(candidates);

				Collections.sort(anaphorZeros);
				findAntecedent(file, part, chainMap, corefResult, anaphorZeros, candidates);
			}
		}

		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);

		System.out.println(a / (a + b));

		evaluate(corefResults, goldEntities);
	}

	static int all = 0;

	private void findAntecedent(String file, CoNLLPart part, HashMap<String, Integer> chainMap,
			ArrayList<EntityMention> corefResult, ArrayList<EntityMention> anaphorZeros,
			ArrayList<EntityMention> candidates) {
		all += anaphorZeros.size();
		for (EntityMention zero : anaphorZeros) {
			if (zero.notInChainZero) {
				continue;
			}
			Entity zeroE = zero.entity;
			Collections.sort(zeroE.mentions);
			EntityMention antecedent = null;
			// System.out.println("====");
			double val = -1000;
			double threshold = -10000.5;
			for (int h = candidates.size() - 1; h >= 0; h--) {
				EntityMention cand = candidates.get(h);
				cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();
				zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
				if (cand.compareTo(zero) < 0 && zero.sentenceID - cand.sentenceID < 3) {
					fea.set(anaphorZeros, candidates, cand, zero, part);
					boolean coref = chainMap.containsKey(zero.toName()) && chainMap.containsKey(cand.toName())
							&& chainMap.get(zero.toName()).intValue() == chainMap.get(cand.toName()).intValue();
					// System.out.print(coref + " ");

					String tk = ZeroUtil.getTree(cand, zero, part);
					if (!tk.startsWith(" (")) {
						continue;
					}
					String treeKernel = "|BT|" + tk + "|ET|";
					// System.out.println(tk);
					treeKs.add("+1 " + treeKernel);
					lines.add(part.getPartName() + " " + cand.toName() + " " + zero.toName());

					// fdjkl

					ArrayList<String> instances = new ArrayList<String>();
					instances.add("+1 " + treeKernel);
					Common.outputLines(instances, "tk.test");

					double d = runSVMTk();
					System.out.println(d + "###");
					if (d > threshold && d > val) {
						antecedent = cand;
						val = d;
						// break;
					}
				}
			}
			if (antecedent != null) {
				if (antecedent.end != -1) {
					zero.antecedent = antecedent;
				} else {
					zero.antecedent = antecedent.antecedent;
				}
			} else {
				// removes.add(zero);
			}
			if (zero.antecedent != null && zero.antecedent.end != -1 && chainMap.containsKey(zero.toName())
					&& chainMap.containsKey(zero.antecedent.toName())
					&& chainMap.get(zero.toName()).intValue() == chainMap.get(zero.antecedent.toName()).intValue()) {
				good++;
				// System.out.println("+++");
				// printResult(zero, zero.antecedent, part);
				// System.out.println("Predicate: " +
				// this.getPredicate(zero.V));
				// System.out.println("Object NP: " +
				// this.getObjectNP(zero));
				// System.out.println("===");
				System.out.println("Right!!! " + good + "/" + bad);
			} else {
				bad++;
				// System.out.println("---");
				if (antecedent != null) {
					// System.out.println(antecedent.end == -1);
					if (antecedent.end == -1) {
						a++;
					}
					b++;
				}
				System.out.println("Error??? " + good + "/" + bad);
			}
			printResult(zero, zero.antecedent, part);
			// System.out.println("Predicate: " + fea.getPredicate(zero.V));
			// System.out.println("Object NP: " + fea.getObjectNP(zero));
			String conllPath = file;
			int aa = conllPath.indexOf(anno);
			int bb = conllPath.indexOf(".");
			String middle = conllPath.substring(aa + anno.length(), bb);
			String path = prefix + middle + suffix;
			System.out.println(path);
			System.out.println("=== " + file);

			if (antecedent != null) {
				CoNLLWord candWord = part.getWord(antecedent.start);
				CoNLLWord zeroWord = part.getWord(zero.start);

				String zeroSpeaker = part.getWord(zero.start).speaker;
				String candSpeaker = part.getWord(antecedent.start).speaker;
				// if (!zeroSpeaker.equals(candSpeaker)) {
				// if (antecedent.source.equals("我") &&
				// zeroWord.toSpeaker.contains(candSpeaker)) {
				// zero.head = "你";
				// zero.source = "你";
				// } else if (antecedent.source.equals("你") &&
				// candWord.toSpeaker.contains(zeroSpeaker)) {
				// zero.head = "我";
				// zero.source = "我";
				// }
				// } else {
				zero.source = antecedent.source;
				zero.head = antecedent.head;
				// }
				// String pred = fea.getPredicate(zero.V);
				// String np = fea.getObjectNP(zero);
			}
		}
		for (EntityMention zero : anaphorZeros) {
			if (zero.antecedent != null) {
				corefResult.add(zero);
			}
		}
	}

	private double runSVMTk() {
		String lineStr = "";
		String cmd = "./svmTk.sh";

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			lineStr = inBr.readLine();
			// System.out.println(lineStr);
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					Common.bangErrorPOS("");
				}
			}
			inBr.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ArrayList<String> lines = Common.getLines("tk.result");

		return Double.parseDouble(lines.get(0).split("\\s+")[0]);
	}

	public static void evaluate(ArrayList<ArrayList<EntityMention>> zeroses, ArrayList<ArrayList<Entity>> entitieses) {
		double gold = 0;
		double system = 0;
		double hit = 0;

		for (int i = 0; i < zeroses.size(); i++) {
			ArrayList<EntityMention> zeros = zeroses.get(i);
			ArrayList<Entity> entities = entitieses.get(i);
			ArrayList<EntityMention> goldInChainZeroses = RuleZeroCoref.getAnaphorZeros(entities);
			HashMap<String, Integer> chainMap = formChainMap(entities);
			gold += goldInChainZeroses.size();
			system += zeros.size();
			for (EntityMention zero : zeros) {
				EntityMention ant = zero.antecedent;
				Integer zID = chainMap.get(zero.toName());
				Integer aID = chainMap.get(ant.toName());
				if (zID != null && aID != null && zID.intValue() == aID.intValue()) {
					hit++;
				}
			}
		}

		double r = hit / gold;
		double p = hit / system;
		double f = 2 * r * p / (r + p);
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);
	}

	private static HashMap<String, Integer> formChainMap(ArrayList<Entity> entities) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < entities.size(); i++) {
			for (EntityMention m : entities.get(i).mentions) {
				map.put(m.toName(), i);
			}
		}
		return map;
	}

	static ArrayList<String> treeKs = new ArrayList<String>();
	static ArrayList<String> lines = new ArrayList<String>();

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		ZeroCorefTestTK test = new ZeroCorefTestTK(args[0]);
		test.getTestInstances();

		Common.outputLines(treeKs, "treeKernel.test.all");
		Common.outputLines(lines, "mzeros.all");

		System.out.println(all);
	}
}
