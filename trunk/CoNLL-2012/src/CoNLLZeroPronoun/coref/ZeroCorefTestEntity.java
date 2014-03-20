package CoNLLZeroPronoun.coref;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import jnisvmlight.SVMLightModel;
import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import util.ChCommon;
import util.Common;
import util.YYFeature;
import CoNLLZeroPronoun.detect.ZeroDetectTest;

public class ZeroCorefTestEntity extends ZeroCoref {

	String folder;
	SVMLightModel model;

	SVMLightModel modelMR;

	public ZeroCorefTestEntity(String folder) {
		this.folder = folder;
		String f = this.folder;
		f = "all";
		fea = new ZeroCorefFea(false, "zeroCoref" + f);
		try {
			this.model = SVMLightModel.readSVMLightModelFromURL(new java.io.File(
					"/users/yzcchen/tool/JNI_SVM-light-6.01/src/svmlight-6.01/zpmodel").toURL());

			this.modelMR = SVMLightModel.readSVMLightModelFromURL(new java.io.File(
					"/users/yzcchen/tool/JNI_SVM-light-6.01/src/svmlight-6.01/zpmodelMR").toURL());

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		int pID = 0;
		for (int i = 0; i < files.size(); i++) {
			String file = files.get(i);
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace("auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				pID++;
				// if(pID%5!=4) {
				// continue;
				// }
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
				// && !file.contains("/mz/") && !file.contains("/wb/")
				) {
					candidates.addAll(anaphorZeros);
				}
				Collections.sort(candidates);

				Collections.sort(anaphorZeros);
				this.assignVNode(anaphorZeros, part);
				this.assignNPNode(goldBoundaryNPMentions, part);

				findAntecedent(file, part, chainMap, corefResult, anaphorZeros, candidates);
			}
		}

		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);

		System.out.println(a / (a + b));

		evaluate(corefResults, goldEntities);
	}

	private void findAntecedent(String file, CoNLLPart part, HashMap<String, Integer> chainMap,
			ArrayList<EntityMention> corefResult, ArrayList<EntityMention> anaphorZeros,
			ArrayList<EntityMention> candidates) {

		ArrayList<Entity> NPCorefChains = part.getChains();
		HashMap<String, Integer> goldMentionToClusterIDMap = new HashMap<String, Integer>();
		for (int i = 0; i < NPCorefChains.size(); i++) {
			Entity e = NPCorefChains.get(i);
			for (EntityMention m : e.mentions) {
				goldMentionToClusterIDMap.put(m.toName(), i);
			}
		}
		int eid = NPCorefChains.size();
		for (int i = 0; i < candidates.size(); i++) {
			EntityMention m = candidates.get(i);
			if (!goldMentionToClusterIDMap.containsKey(m.toName())) {
				goldMentionToClusterIDMap.put(m.toName(), eid++);
			}
		}

		HashMap<String, ArrayList<EntityMention>> clusterMap = new HashMap<String, ArrayList<EntityMention>>();
		for (int i = 0; i < candidates.size(); i++) {
			EntityMention m = candidates.get(i);
			if (m.end == -1) {
				continue;
			}
			ArrayList<EntityMention> ms = new ArrayList<EntityMention>();
			clusterMap.put(m.toName(), ms);
			if (goldMentionToClusterIDMap.containsKey(m.toName())) {
				int clusterID = goldMentionToClusterIDMap.get(m.toName());
				for (int j = 0; j < candidates.size(); j++) {
					EntityMention m2 = candidates.get(j);
					if (m2.end == -1) {
						continue;
					}
					if (goldMentionToClusterIDMap.containsKey(m2.toName())
							&& goldMentionToClusterIDMap.get(m2.toName()).intValue() == clusterID) {
						ms.add(m2);
					}
				}
			} else {
				ms.add(m);
			}
		}

		for (int h = candidates.size() - 1; h >= 0; h--) {
			EntityMention cand = candidates.get(h);
			cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();
			this.subjectNP(cand, part);
		}

		for (EntityMention zero : anaphorZeros) {
			Entity zeroE = zero.entity;
			Collections.sort(zeroE.mentions);
			EntityMention antecedent = null;
			// System.out.println("====");

			HashMap<Integer, Integer> clusterIdMap = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
			int clusterID = 0;

			ArrayList<String> svmRankCRs = new ArrayList<String>();
			ArrayList<String> svmRankMRs = new ArrayList<String>();

			zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
			ArrayList<EntityMention> cands = new ArrayList<EntityMention>();
			for (int h = candidates.size() - 1; h >= 0; h--) {
				EntityMention cand = candidates.get(h);
				if (cand.start < zero.start && zero.sentenceID - cand.sentenceID <= 2) {
					cands.add(cand);
				}
			}
			System.out.println("-----");
			HashSet<Integer> processedCluster = new HashSet<Integer>();
			int antCount = 0;
			for (int h = 0; h < cands.size(); h++) {
				EntityMention cand = cands.get(h);
				if (cand.extent.isEmpty()) {
					continue;
				}
				this.assignNPNode(cand, part);
				this.subjectNP(cand, part);
				cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();

				fea.set(anaphorZeros, candidates, cand, zero, part);
				String feaStr = fea.getSVMFormatString();
				svmRankMRs.add("-1 " + feaStr);
				idMap.put(antCount, h);
				antCount++;
				if (cand.compareTo(zero) < 0 && cand.end < zero.start
						&& !processedCluster.contains(goldMentionToClusterIDMap.get(cand.toName()))) {
					ArrayList<EntityMention> wholecluster = clusterMap.get(cand.toName());
					ArrayList<EntityMention> cluster = new ArrayList<EntityMention>();
					for (EntityMention cant : wholecluster) {
//						 if(cant.compareTo(zero)<0) {
						cluster.add(cant);
//						 }
					}

					HashMap<Integer, Integer> feaMap = new HashMap<Integer, Integer>();
					for (EntityMention cant : cluster) {
						this.subjectNP(cant, part);
						cant.sentenceID = part.getWord(cant.start).sentence.getSentenceIdx();
						fea.set(anaphorZeros, candidates, cant, zero, part);
						feaStr = fea.getSVMFormatString();
						String tks[] = feaStr.split("\\s+");
						for (String tk : tks) {
							int comma = tk.indexOf(":");
							int feaIdx = Integer.parseInt(tk.substring(0, comma));
							if (feaIdx > YYFeature.strFeaFrom || cant.equals(cand)) {
								if (feaMap.containsKey(feaIdx)) {
									feaMap.put(feaIdx, feaMap.get(feaIdx).intValue() + 1);
								} else {
									feaMap.put(feaIdx, 1);
								}
								if (feaIdx < 0) {
									Common.bangErrorPOS(feaStr);
								}
							}
						}
					}
					ArrayList<Integer> feaIdxes = new ArrayList<Integer>(feaMap.keySet());
					Collections.sort(feaIdxes);
					StringBuilder entitySb = new StringBuilder();
					for (int feaIdx : feaIdxes) {
						int amount = feaMap.get(feaIdx);
						int newFea = feaIdx;
						// * 3;
						// if (amount == cluster.size()) {
						// newFea += 0;
						// } else if (amount >= cluster.size() / 2) {
						// newFea += 1;
						// } else if (amount > 0) {
						// newFea += 2;
						// }
						entitySb.append(newFea).append(":1 ");
					}

					// fea.set(anaphorZeros, candidates, cant, zero, part);
					// String feaStr = fea.getSVMFormatString();

					feaStr = "-1 " +
					// "qid:1 " +
							"" + entitySb.toString();
					svmRankCRs.add(feaStr);

					clusterIdMap.put(clusterID, h);
					clusterID++;
					processedCluster.add(goldMentionToClusterIDMap.get(cand.toName()));
				}
			}
			Common.outputLines(svmRankCRs, "svmRankCR.test");
			double probAnt[] = new double[cands.size()];

//			double probAntSVMMR[] = new double[svmRankMRs.size()];
//			for(int i=0;i<svmRankMRs.size();i++) {
//				probAntSVMMR[i] = this.modelMR.classify(Common.SVMStringToFeature(svmRankMRs.get(i)));
//			}
//			probAnt = probAntSVMMR;
			 
			double probAntSVMCR[] = new double[svmRankCRs.size()];
			for (int i = 0; i < svmRankCRs.size(); i++) {
				String line = svmRankCRs.get(i);
				probAntSVMCR[i] = this.model.classify(Common.SVMStringToFeature(line));
			}
			probAnt = probAntSVMCR;

			if (probAnt.length != 0) {
				int rankID = -1;
				// TODO
				double rankMax = -1000000;
				for (int i = 0; i < probAnt.length; i++) {
					double prob = probAnt[i];
					if (prob > rankMax) {
						rankMax = prob;
						rankID = i;
					}
				}
				antecedent = cands.get(clusterIdMap.get(rankID));
//				antecedent = cands.get(idMap.get(rankID));
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
				System.out.println("===");
				System.out.println("Right!!! " + good + "/" + bad);

				String key = part.getDocument().getDocumentID() + ":" + part.getPartID() + ":" + zero.start + "-"
						+ zero.antecedent.start + "," + zero.antecedent.end + ":GOOD";
				corrects.add(key);
			} else {

				// String key = part.getDocument().getDocumentID() + ":" +
				// part.getPartID() + ":" + zero.start + "-"
				// + zero.antecedent.start + "," + zero.antecedent.end + ":BAD";
				// corrects.add(key);

				bad++;
				// System.out.println("---");
				if (antecedent != null) {
					// System.out.println(antecedent.end == -1);
					if (antecedent.end == -1) {
						a++;
					}
					b++;
				}
				System.out.println("===");
				System.out.println("Error??? " + good + "/" + bad);
			}
			if (zero.antecedent != null) {
				printResult(zero, zero.antecedent, part);
				// System.out.println("Predicate: " + fea.getPredicate(zero.V));
				// System.out.println("Object NP: " + fea.getObjectNP(zero));
				String conllPath = file;
				int aa = conllPath.indexOf(anno);
				int bb = conllPath.indexOf(".");
				String middle = conllPath.substring(aa + anno.length(), bb);
				String path = prefix + middle + suffix;
				System.out.println(path);
				// System.out.println("=== " + file);
				this.addEmptyCategoryNode(zero);
				int zeroClusterID = goldMentionToClusterIDMap.get(zero.antecedent.toName());
				goldMentionToClusterIDMap.put(zero.antecedent.toName(), zeroClusterID);

				ArrayList<EntityMention> zeroCorefs = clusterMap.get(zero.antecedent.toName());
				zeroCorefs.add(zero);
				Collections.sort(zeroCorefs);
				for (EntityMention zeroCoref : zeroCorefs) {
					clusterMap.put(zeroCoref.toName(), zeroCorefs);
				}
			}

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

				String pred = fea.getPredicate(zero.V);
				String np = fea.getObjectNP(zero);
			}

			String pred = fea.getPredicate(zero.V);
			String np = fea.getObjectNP(zero);

			// if (!np.isEmpty()) {
			// gram3.add("* " + pred + " " + np);
			// }
			// gram2.add("* " + pred);
		}
		for (EntityMention zero : anaphorZeros) {
			if (zero.antecedent != null) {
				corefResult.add(zero);
			}
		}
	}

	static ArrayList<String> corrects = new ArrayList<String>();

	private static double[] runSVMRank() {
		String lineStr = "";
		String cmd = "./svmlightRankCR.sh";

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			lineStr = inBr.readLine();
			// System.out.println(lineStr);
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR SVMRANK");
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
		ArrayList<String> lines = Common.getLines("svmRankCR.result");
		double[] ret = new double[lines.size()];
		for (int i = 0; i < lines.size(); i++) {
			ret[i] = Double.parseDouble(lines.get(i));
		}
		return ret;
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

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		ZeroCorefTestEntity test = new ZeroCorefTestEntity(args[0]);
		test.getTestInstances();

		Common.outputLines(corrects, "correct.super");
	}
}
