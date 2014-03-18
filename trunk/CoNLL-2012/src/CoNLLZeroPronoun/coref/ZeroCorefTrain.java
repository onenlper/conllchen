package CoNLLZeroPronoun.coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ZeroCorefTrain extends ZeroCoref {

	String folder;

	public ZeroCorefTrain(String folder) {
		this.folder = folder;
		fea = new ZeroCorefFea(true, "zeroCoref" + this.folder);
	}

	public void train() {
		Common.outputLines(this.getTrainInstances(), "zeroCorefTrain." + this.folder);
	}

	HashMap<String, Integer> chainMap;

	public ArrayList<String> getTrainInstances() {
		ArrayList<String> lines = new ArrayList<String>();
		// lines.addAll(getTrainZeroInstances());
		lines.addAll(getTrainNoZeroInstances());
		fea.freeze();
		return lines;
	}

	public ArrayList<String> getTrainNoZeroInstances() {
		HashSet<String> gram2 = new HashSet<String>();
		HashSet<String> gram3 = new HashSet<String>();
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train/");
		ArrayList<String> instances = new ArrayList<String>();

		int qid = 1;

		for (String file : files) {
			// System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> goldChains = part.getChains();
				fea.part = part;
				chainMap = this.formChainMap(goldChains);
				fea.chainMap = chainMap;
				ParseTreeMention ptm = new ParseTreeMention();
				ArrayList<EntityMention> goldBoundaryNPMentions = ptm.getMentions(part);
				ArrayList<EntityMention> goldInChainZeroses = RuleZeroCoref.getAnaphorZeros(part.getChains());
				Collections.sort(goldInChainZeroses);

				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				candidates.addAll(goldBoundaryNPMentions);
				// candidates.addAll(goldInChainZeroses);

				Collections.sort(candidates);
				this.assignVNode(goldInChainZeroses, part);
				this.assignNPNode(goldBoundaryNPMentions, part);
				
				for (EntityMention zero : goldInChainZeroses) {
					if (zero.notInChainZero) {
						System.out.println("NOt happen");
						System.exit(1);
						continue;
					}
					Entity zeroE = zero.entity;
					Collections.sort(zeroE.mentions);
					EntityMention antecedent = null;
					for (EntityMention m : zeroE.mentions) {
						if (m.end == -1) {
							continue;
						}
						if (m.compareTo(zero) < 0) {
							antecedent = m;
						} else {
							break;
						}
					}
					if (antecedent == null) {
						Common.bangErrorPOS("Not Happen");
						continue;
					}
					if (zero.V == null) {
						Common.bangErrorPOS("Not Happen" + part.getWord(zero.start).word);
						continue;
					}
					String conllPath = document.getFilePath();
					int a = conllPath.indexOf(anno);
					int b = conllPath.indexOf(".");
					String middle = conllPath.substring(a + anno.length(), b);
					String path = prefix + middle + suffix;
					fea.path = path;
					fea.conllPath = conllPath;
					// System.out.println("================= " + path);
					// System.out.println("================= " + conllPath);
					// this.printZero(zero, part);
					// System.out.println("----");
					for (EntityMention cand : candidates) {
						cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();
						if (cand.compareTo(zero) < 0 && cand.compareTo(antecedent) >= 0) {
							fea.set(goldInChainZeroses, candidates, cand, zero, part);
							if (chainMap.containsKey(cand.toName()) && chainMap.containsKey(zero.toName())) {
								fea.label = chainMap.containsKey(cand.toName())
								&& chainMap.containsKey(zero.toName())
								&& chainMap.get(cand.toName()).intValue() == chainMap.get(zero.toName())
										.intValue();
								String feaStr = fea.getSVMFormatString();
								if (chainMap.containsKey(cand.toName())
										&& chainMap.containsKey(zero.toName())
										&& chainMap.get(cand.toName()).intValue() == chainMap.get(zero.toName())
												.intValue()
								// && cand.end!=-1
								) {
									instances.add("+1 " + feaStr);
									// instances.add("2 qid:" + qid + " " +
									// feaStr);
								} else if (chainMap.containsKey(cand.toName())
										&& chainMap.containsKey(zero.toName())
										&& chainMap.get(cand.toName()).intValue() != chainMap.get(zero.toName())
												.intValue()) {
									instances.add("-1 " + feaStr);
									// instances.add("1 qid:" + qid + " " +
									// feaStr);
								}
							}
						}
					}
					qid++;
					this.addEmptyCategoryNode(zero);

					String zeroSpeaker = part.getWord(zero.start).speaker;
					String candSpeaker = part.getWord(antecedent.start).speaker;

					if (!zeroSpeaker.equals(candSpeaker)) {
						if (antecedent.source.equals("我")) {
							zero.head = "你";
							zero.source = "你";
						} else if (antecedent.source.equals("你")) {
							zero.head = "我";
							zero.source = "我";
						}
					} else {
						zero.source = antecedent.source;
						zero.head = antecedent.head;
					}

					// printResult(zero, zero.antecedent, part);
					// System.out.println("Predicate: " +
					// this.getPredicate(zero.V));
					// System.out.println("Object NP: " +
					// this.getObjectNP(zero));
					// System.out.println("===");
					String pred = fea.getPredicate(zero.V);
					String np = fea.getObjectNP(zero);

					if (!np.isEmpty()) {
						gram3.add("* " + pred + " " + np);
					}
					gram2.add("* " + pred);
				}
			}
		}
		fea.freeze();
		return instances;
	}

	public ArrayList<String> getTrainZeroInstances() {
		HashSet<String> gram2 = new HashSet<String>();
		HashSet<String> gram3 = new HashSet<String>();
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train/");
		ArrayList<String> instances = new ArrayList<String>();

		int qid = 1;

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> goldChains = part.getChains();
				fea.part = part;
				chainMap = this.formChainMap(goldChains);
				fea.chainMap = chainMap;
				ParseTreeMention ptm = new ParseTreeMention();
				ArrayList<EntityMention> goldBoundaryNPMentions = ptm.getMentions(part);
				ArrayList<EntityMention> goldInChainZeroses = RuleZeroCoref.getAnaphorZeros(part.getChains());
				Collections.sort(goldInChainZeroses);

				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				candidates.addAll(goldBoundaryNPMentions);
				candidates.addAll(goldInChainZeroses);
				Collections.sort(candidates);
				this.assignVNode(goldInChainZeroses, part);
				this.assignNPNode(goldBoundaryNPMentions, part);
				for (EntityMention zero : goldInChainZeroses) {
					if (zero.notInChainZero) {
						continue;
					}
					Entity zeroE = zero.entity;
					Collections.sort(zeroE.mentions);
					EntityMention antecedent = null;
					for (EntityMention m : zeroE.mentions) {
						if (m.compareTo(zero) < 0) {
							antecedent = m;
						} else {
							break;
						}
					}
					if (antecedent == null) {
						Common.bangErrorPOS("Not Happen");
						continue;
					}
					if (zero.V == null) {
						Common.bangErrorPOS("Not Happen" + part.getWord(zero.start).word);
						continue;
					}
					for (EntityMention cand : candidates) {
						cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();
						if (cand.compareTo(zero) < 0 && cand.compareTo(antecedent) >= 0
						// && antecedent.end==-1
						) {
							fea.set(goldInChainZeroses, candidates, cand, zero, part);
							if (chainMap.containsKey(cand.toName()) && chainMap.containsKey(zero.toName())) {
								String feaStr = fea.getSVMFormatString();
								if (chainMap.containsKey(cand.toName())
										&& chainMap.containsKey(zero.toName())
										&& chainMap.get(cand.toName()).intValue() == chainMap.get(zero.toName())
												.intValue()) {
									instances.add("+1 " + feaStr);
									// instances.add("2 qid:" + qid + " " +
									// feaStr);
								} else if (chainMap.containsKey(cand.toName())
										&& chainMap.containsKey(zero.toName())
										&& chainMap.get(cand.toName()).intValue() != chainMap.get(zero.toName())
												.intValue()) {
									instances.add("-1 " + feaStr);
									// instances.add("1 qid:" + qid + " " +
									// feaStr);
								}
							}
						}
					}
					qid++;
					this.addEmptyCategoryNode(zero);

					String zeroSpeaker = part.getWord(zero.start).speaker;
					String candSpeaker = part.getWord(antecedent.start).speaker;

					if (!zeroSpeaker.equals(candSpeaker)) {
						if (antecedent.source.equals("我")) {
							zero.head = "你";
							zero.source = "你";
						} else if (antecedent.source.equals("你")) {
							zero.head = "我";
							zero.source = "我";
						}
					} else {
						zero.source = antecedent.source;
						zero.head = antecedent.head;
					}

					// printResult(zero, zero.antecedent, part);
					// System.out.println("Predicate: " +
					// this.getPredicate(zero.V));
					// System.out.println("Object NP: " +
					// this.getObjectNP(zero));
					// System.out.println("===");
					String pred = fea.getPredicate(zero.V);
					String np = fea.getObjectNP(zero);

					if (!np.isEmpty()) {
						gram3.add("* " + pred + " " + np);
					} else {
						gram2.add("* " + pred);
					}
				}
			}
		}
		Common.outputHashSet(gram2, "gram2." + folder);
		Common.outputHashSet(gram3, "gram3." + folder);
		return instances;
	}

	private ArrayList<String> stats() {
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train/");
		ArrayList<String> instances = new ArrayList<String>();

		double allM = 0;
		double azp = 0;

		double notInChainZero = 0;

		int chains = 0;
		int chainwithzero = 0;
		int chainallzero = 0;

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

			OntoCorefXMLReader.addGoldZeroPronouns(document, true);

			for (CoNLLPart part : document.getParts()) {

				ArrayList<Entity> goldChains = part.getChains();

				HashMap<String, Integer> chainMap = this.formChainMap(goldChains);

				chains += goldChains.size();

				for (Entity goldChain : goldChains) {
					boolean npis = false;
					boolean zerois = false;
					allM += goldChain.mentions.size();
					for (int i = 0; i < goldChain.mentions.size(); i++) {
						Collections.sort(goldChain.mentions);

						EntityMention m = goldChain.mentions.get(i);

						if (m.end == -1) {
							zerois = true;
						} else {
							npis = true;
						}

						EntityMention zero = goldChain.mentions.get(i);
						if (zero.end != -1) {
							continue;
						}
						azp++;

						if (m.notInChainZero) {
							notInChainZero++;

							System.out.println(goldChain.mentions.get(0).toName() + " # "
									+ goldChain.mentions.get(1).toName());

							Common.bangErrorPOS("pp");

						} else {
							if (goldChain.mentions.size() == 1) {
								Common.bangErrorPOS("NO");
							}
						}
					}

					if (zerois) {
						chainwithzero++;
					}
					if (!npis) {
						chainallzero++;
					}
				}
			}
		}
		System.out.println("allM:" + allM);
		System.out.println("azp:" + azp);
		System.out.println("azp/allM:" + azp / allM);
		System.out.println("newNPMentions:" + OntoCorefXMLReader.newNPMentions);
		System.out.println("zeroCorefNewNP:" + OntoCorefXMLReader.zeroCorefNewNP);
		System.out.println("notInChainZero: " + notInChainZero);

		System.out.println(allM - azp - OntoCorefXMLReader.newNPMentions);

		System.out.println("chains: " + chains);
		System.out.println("chainwithzero: " + chainwithzero);
		System.out.println("chainallzero: " + chainallzero);
		return instances;
	}

	private HashMap<String, Integer> formChainMap(ArrayList<Entity> entities) {
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
		ZeroCorefTrain train = new ZeroCorefTrain(args[0]);
		train.train();

		System.out.println(ZeroCorefFea.allMatch);
		System.out.println(ZeroCorefFea.rightMatch);
	}
}
