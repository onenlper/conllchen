package CoNLLZeroPronoun.detect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Common.Feature;

public class ZeroDetect {

	CoNLLPart part;

	ZeroDetectFea detectFea;

	HashSet<EntityMention> goldZeros;
	
	ArrayList<String> tokenized;
	
	public ZeroDetect(boolean train, String folder) {
		detectFea = new ZeroDetectFea(train, "zeroDetect." + folder);
	}

	public void setPart(CoNLLPart part) {
		this.part = part;
	}
	
	public void setGoldZeros(HashSet<EntityMention> goldZeros) {
		this.goldZeros = goldZeros;
	}
	
	public void setTokenized(ArrayList<String> tokenized) {
		this.tokenized = tokenized;
	}

	public ArrayList<EntityMention> getHeuristicZeros() {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();

		for (CoNLLSentence s : this.part.getCoNLLSentences()) {
			HashSet<Integer> candidates = new HashSet<Integer>();

			MyTree tree = s.syntaxTree;
			MyTreeNode root = tree.root;
			this.visitTreeNode(root, candidates, s);

			for (Integer can : candidates) {
				EntityMention m = new EntityMention();
				m.start = can;
				m.end = -1;
				m.sentenceID = s.getSentenceIdx();
				
				mentions.add(m);
			}
		}

		Collections.sort(mentions);
		return mentions;
	}

//	private void visitTreeNode(MyTreeNode node, HashSet<Integer> zeros, CoNLLSentence s) {
//		if (node.value.equalsIgnoreCase("VP")) {
//			HashSet<String> filted = new HashSet<String>(Arrays.asList("ADVP"));
//
//			boolean CC = false;
//			// if in CC construct
//			for (MyTreeNode temp : node.parent.children) {
//				if (temp.value.equalsIgnoreCase("CC") && temp.getFirstXAncestor("VP") != null) {
//					CC = true;
//				}
//			}
//
//			boolean advp = false;
//			ArrayList<MyTreeNode> leftSisters = node.getLeftSisters();
//			for (int k=leftSisters.size()-1;k>=0;k--) {
//				MyTreeNode leftSister = leftSisters.get(k);
//				if(!filted.contains(leftSister.value)) {
//					break;
//				}
//				if (filted.contains(leftSister.value)) {
//					if (leftSister.parent.value.equals("VP")) {
//						advp = true;
//						break;
//					}
//				}
//			}
//
//			if(leftSisters.size()>0) {
//				for(MyTreeNode leftSister : leftSisters) {
//					if(leftSister.value.startsWith("NP")) {
////						advp = true;
//						EntityMention m = new EntityMention();
//						m.start = s.getWord(node.getLeaves().get(0).leafIdx).index;
//						m.end = -1;
////						System.out.println(this.goldZeros);
////						if (this.goldZeros.contains(m)) {
////							ArrayList<String> wildPS = PatternCollect.add3gramPatternWild(m, part, tokenized);
////							for (String ps : wildPS) {
////								System.out.println(ps);
////							}
////						}
//					}
//				}
//			}
//			
//			EntityMention m = new EntityMention();
//			m.start = s.getWord(node.getLeaves().get(0).leafIdx).index;
//			m.end = -1;
//			
//			
////			ArrayList<String> ps = PatternCollect.add3gramPattern(m, part, tokenized);
////			ArrayList<String> wildPS = PatternCollect.add3gramPatternWild(m, part, tokenized);
////			ArrayList<int[]> pairs = new ArrayList<int[]>();
////			for (int k = 0; k < ps.size(); k++) {
////				String p = ps.get(k);
////				String wildP = wildPS.get(k);
////				int pair[] = new int[2];
////				pair[0] = detectFea.get(detectFea.gram3Map, p);
////				pair[1] = detectFea.get(detectFea.gram4MapWild, wildP);
////				pairs.add(pair);
////				int ratio = 9;
////				if (pair[1] != 0) {
////					ratio = (int) Math.ceil(Math.log(pair[0] / pair[1]));
////				}
////			}
////			
////			boolean qualify = true;
////			for (int[] pair : pairs) {
////				if (!(pair[0] > 0 && pair[1] == 0)) {
////					qualify = false;
////					break;
////				}
////			}
////			if (qualify) {
//////				advp = true;
////			}
//			
//			
//			
//			
//			
//			if (!CC && !advp) {
//				int leafIdx = node.getLeaves().get(0).leafIdx;
//				zeros.add(s.getWord(leafIdx).index);
//			}
//		}
//		for (MyTreeNode child : node.children) {
//			this.visitTreeNode(child, zeros, s);
//		}
//	}

	private static void visitTreeNode(MyTreeNode node, HashSet<Integer> zeros,
			CoNLLSentence s) {
		if (node.value.equalsIgnoreCase("VP")) {
			boolean CC = false;
			// if in CC construct
			for (MyTreeNode temp : node.parent.children) {
				if (temp.value.equalsIgnoreCase("CC")) {
					CC = true;
				}
			}

			boolean advp = false;
			ArrayList<MyTreeNode> leftSisters = node.getLeftSisters();
			for (int k = leftSisters.size() - 1; k >= 0; k--) {
				MyTreeNode leftSister = leftSisters.get(k);
				if (leftSister.value.equals("ADVP")) {
					if (leftSister.parent.value.equals("VP")) {
						advp = true;
						break;
					}
				}
			}

			int leafIdx = node.getLeaves().get(0).leafIdx;
			if (!CC && !advp)
				zeros.add(s.getWord(leafIdx).index);
		}
		for (MyTreeNode child : node.children) {
			visitTreeNode(child, zeros, s);
		}
	}
	
	public String getZeroDetectFea(EntityMention zero) {
		detectFea.set(zero, part, tokenized);
		detectFea.zeroDetect = this;
		return this.detectFea.getSVMFormatString();
	}

	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("java ~ folder [test|train]");
			System.exit(1);
		}
		String folder = args[0];
		String mode = args[1];

		if (mode.equalsIgnoreCase("train")) {
			ZeroDetectTrain train = new ZeroDetectTrain(folder);
			// //TODO
			train.train();
		} else if (mode.equalsIgnoreCase("test")) {
			ZeroDetectTest test = new ZeroDetectTest(folder);
			test.test();
		}
	}

	private static void run(String[] args) {
		if (args.length != 2) {
			System.out.println("java ~ folder [train|development]");
			System.exit(1);
		}
		String folder = args[0];
		folder = "all";
		String mode = args[1];
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_" + mode + "/");

		double overall = 0;
		double zeroall = 0;
		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

			OntoCorefXMLReader.addGoldZeroPronouns(document, true);

			for (int i = 0; i < document.getParts().size(); i++) {
				CoNLLPart part = document.getParts().get(i);

				ArrayList<Entity> chains = part.getChains();
				double all = 0;
				double zero = 0;
				for (Entity entity : chains) {
					for (EntityMention m : entity.mentions) {
						all++;
						if (m.end == -1) {
							zero++;
						}
					}
				}
				System.out.println(zero + ":" + all + "#" + (zero / all));

				overall += all;
				zeroall += zero;

			}
		}
		System.out.println("=====");
		System.out.println(zeroall + ":" + overall + "#" + (zeroall / overall));
	}

}
