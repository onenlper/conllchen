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
import model.CoNLL.OntoCorefXMLReader;
import util.Common;
import util.YYFeature;

public class ZeroCorefTrainEntity extends ZeroCoref {

	String folder;

	public ZeroCorefTrainEntity(String folder) {
		this.folder = folder;
		fea = new ZeroCorefFea(true, "zeroCoref" + this.folder);
	}

	public void train() {
		Common.outputLines(this.getTrainInstances(), "zeroCorefTrain." + this.folder);
		Common.outputLines(mentionRanking, "zeroCorefTrainMR." + this.folder);
	}

	HashMap<String, Integer> chainMap;

	public ArrayList<String> getTrainInstances() {
		ArrayList<String> lines = new ArrayList<String>();
		// lines.addAll(getTrainZeroInstances());
		lines.addAll(getTrainNoZeroInstances());
		fea.freeze();
		return lines;
	}

	ArrayList<String> mentionRanking = new ArrayList<String>();

	public ArrayList<String> getTrainNoZeroInstances() {
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

				HashMap<String, Integer> goldMentionToClusterIDMap = new HashMap<String, Integer>();
				for (String key : chainMap.keySet()) {
					goldMentionToClusterIDMap.put(key, chainMap.get(key));
				}
				int eid = goldChains.size();
				for (EntityMention m : candidates) {
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
				for (EntityMention zero : goldInChainZeroses) {
					zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
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
					StringBuilder sb = new StringBuilder();
					sb.append(zero.toName() + "\n");
					sb.append("=======\n");
					HashSet<Integer> processedCluster = new HashSet<Integer>();
					for (EntityMention cand : candidates) {
						cand.sentenceID = part.getWord(cand.start).sentence.getSentenceIdx();
						if (cand.compareTo(zero) < 0 && zero.sentenceID - cand.sentenceID <= 2
								&& !processedCluster.contains(goldMentionToClusterIDMap.get(cand.toName()))) {

							boolean coref = isCoref(zero, cand);
							String feaStr;
							fea.set(goldInChainZeroses, candidates, cand, zero, part);
							feaStr = fea.getSVMFormatString();
							if (coref) {
								this.mentionRanking.add("+1 qid:" + qid + " " + feaStr);
							} else {
								this.mentionRanking.add("-1 qid:" + qid + " " + feaStr);
							}

							ArrayList<EntityMention> wholecluster = clusterMap.get(cand.toName());
							ArrayList<EntityMention> cluster = new ArrayList<EntityMention>();
							for (EntityMention cant : wholecluster) {
								 if(cant.compareTo(zero)<0) {
								cluster.add(cant);
								 }
							}

							HashMap<Integer, Integer> feaMap = new HashMap<Integer, Integer>();
							for (EntityMention cant : cluster) {
								sb.append(cant.toName()).append(" ");
								cant.sentenceID = part.getWord(cant.start).sentence.getSentenceIdx();
								fea.set(goldInChainZeroses, candidates, cant, zero, part);
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
							sb.append("\n");
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

							if (coref) {
								instances.add("+1 qid:" + qid + " " + entitySb.toString());
							} else {
								instances.add("-1 qid:" + qid + " " + entitySb.toString());
							}
							processedCluster.add(goldMentionToClusterIDMap.get(cand.toName()));
						}
					}
					qid++;
					// System.out.println(sb.toString());
				}
			}

		}
		fea.freeze();
		return instances;
	}

	private boolean isCoref(EntityMention zero, EntityMention cand) {
		boolean coref = chainMap.containsKey(cand.toName()) && chainMap.containsKey(zero.toName())
				&& chainMap.get(cand.toName()).intValue() == chainMap.get(zero.toName()).intValue();
		return coref;
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
		ZeroCorefTrainEntity train = new ZeroCorefTrainEntity(args[0]);
		train.train();

		System.out.println(ZeroCorefFea.allMatch);
		System.out.println(ZeroCorefFea.rightMatch);
	}
}
