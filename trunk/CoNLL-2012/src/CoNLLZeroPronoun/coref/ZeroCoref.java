package CoNLLZeroPronoun.coref;

import java.util.ArrayList;

import util.Common;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;

public abstract class ZeroCoref {

	protected void assignVNode(ArrayList<EntityMention> zeros, CoNLLPart part) {
		for (EntityMention zero : zeros) {
			this.assignVNode(zero, part);
		}
	}

	public ZeroCorefFea fea;
	// private String getPredicate(MyTreeNode vp) {
	// ArrayList<MyTreeNode> leaves = vp.getLeaves();
	// // for (MyTreeNode leaf : leaves) {
	// // if (leaf.parent.value.startsWith("V") && !leaf.value.equals("会") &&
	// !leaf.value.equals("独立")
	// // && !leaf.value.equals("可以")) {
	// // return leaf.value;
	// // }
	// // }
	//		
	// // find VP's child VP, first VV
	// MyTreeNode tmp = vp;
	// while(true) {
	// boolean find = false;
	// for(MyTreeNode child : tmp.children) {
	// if(child.value.equals("VP")) {
	// tmp = child;
	// find = true;
	// break;
	// }
	// }
	// if(!find) {
	// break;
	// }
	// }
	//		
	// String value = tmp.children.get(0).value;
	// if(!value.startsWith("V")) {
	// System.out.println(value + "#" + vp.getPlainText(true));
	// }
	//		
	// return "";
	// }
	//
	// protected String getObjectNP(EntityMention zero) {
	// MyTreeNode vp = zero.V;
	// ArrayList<MyTreeNode> leaves = vp.getLeaves();
	// for (MyTreeNode leaf : leaves) {
	// if (leaf.parent.value.startsWith("V")) {
	// ArrayList<MyTreeNode> possibleNPs = leaf.parent.getRightSisters();
	// for(MyTreeNode tmp : possibleNPs) {
	// if(tmp.value.startsWith("NP") || tmp.value.startsWith("QP") ) {
	// return tmp.getLeaves().get(tmp.getLeaves().size()-1).value;
	// }
	// }
	// }
	// }
	// return "";
	// }

	static String prefix = "/shared/mlrdir1/disk1/mlr/corpora/CoNLL-2012/conll-2012-train-v0/data/files/data/chinese/annotations/";
	static String anno = "annotations/";
	static String suffix = ".coref";

	protected void printZero(EntityMention zero, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start + "#" + fea.getPredicate2(zero.V) + "#"
				+ fea.getObjectNP2(zero));
	}

	protected void printResult(EntityMention zero, EntityMention systemAnte, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start);
		System.out.println(systemAnte != null ? systemAnte.source + "#" + part.getWord(systemAnte.end + 1).word : "");
		if (systemAnte != null) {
			System.out.println(systemAnte.isSub);
		}
		// System.out.println("========");
	}

	protected void assignVNode(EntityMention zero, CoNLLPart part) {
		MyTreeNode V = null;
		zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
		CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
		MyTreeNode root = s.syntaxTree.root;
		CoNLLWord word = part.getWord(zero.start);
		MyTreeNode leaf = root.getLeaves().get(word.indexInSentence);

		for (MyTreeNode node : leaf.getAncestors()) {
			if (node.value.toLowerCase().startsWith("vp") && node.getLeaves().get(0) == leaf) {
				V = node;
			}
		}

		if (V == null) {
			for (MyTreeNode node : leaf.getAncestors()) {
				if (node.value.startsWith("DFL") && node.getLeaves().get(0) == leaf) {
					V = node;
				}
			}
		}

		if (V == null) {
			int offset = 1;
			while (true) {
				word = part.getWord(zero.start + (offset++));
				leaf = root.getLeaves().get(word.indexInSentence);
				for (MyTreeNode node : leaf.getAncestors()) {
					if (node.value.toLowerCase().startsWith("vp") && node.getLeaves().get(0) == leaf) {
						V = node;
					}
				}
				if (V != null) {
					break;
				}
				if (zero.start + offset == part.getWordCount()) {
					break;
				}
			}
		}

		if (V == null) {
			leaf = root.getLeaves().get(part.getWord(zero.start).indexInSentence);
			for (MyTreeNode node : leaf.getAncestors()) {
				if (node.value.startsWith("NP") && node.getLeaves().get(0) == leaf) {
					V = node;
				}
			}
		}
		zero.V = V;
	}

	public void addEmptyCategoryNode(EntityMention zero) {
		MyTreeNode V = zero.V;
		MyTreeNode newNP = new MyTreeNode();
		newNP.value = "NP";
		int VIdx = V.childIndex;
		V.parent.addChild(VIdx, newNP);

		MyTreeNode empty = new MyTreeNode();
		empty.value = "-NONE-";
		newNP.addChild(empty);

		MyTreeNode child = new MyTreeNode();
		child.value = "*pro*";
		empty.addChild(child);
		child.emptyCategory = true;
		zero.NP = newNP;
	}

	public void assignNPNode(ArrayList<EntityMention> mentions, CoNLLPart part) {
		for (EntityMention mention : mentions) {
			this.assignNPNode(mention, part);
		}
	}

	public boolean subjectNP(EntityMention np, CoNLLPart part) {
		if (np.end == -1) {
			return true;
		}
		MyTreeNode treeNode = np.NP;
//		ArrayList<MyTreeNode> rightSisters = treeNode.getRightSisters();
//		for (MyTreeNode sister : rightSisters) {
		for (int i = treeNode.childIndex + 1; i < treeNode.parent.children.size(); i++) {
			MyTreeNode sibling = treeNode.parent.children.get(i);
			if (sibling.value.equals("VP")) {
				np.isSub = true;
				return true;
			}
		}
		return false;
	}

	private boolean objectNP(EntityMention np, CoNLLPart part) {
		MyTreeNode npNode = np.NP;
		ArrayList<MyTreeNode> leftSisters = npNode.getLeftSisters();
		for (MyTreeNode sister : leftSisters) {
			if (sister.value.startsWith("V")) {
				return true;
			}
		}
		return false;
	}

	public void assignNPNode(EntityMention mention, CoNLLPart part) {
		CoNLLSentence s = part.getCoNLLSentences().get(mention.sentenceID);
		MyTreeNode root = s.syntaxTree.root;
		MyTreeNode leftLeaf = root.getLeaves().get(part.getWord(mention.start).indexInSentence);
		MyTreeNode rightLeaf = root.getLeaves().get(part.getWord(mention.end).indexInSentence);
		MyTreeNode NPNode = Common.getLowestCommonAncestor(leftLeaf, rightLeaf);
		mention.NP = NPNode;

		// subject or object
		mention.isSubject = this.subjectNP(mention, part);
		mention.isObject = this.objectNP(mention, part);
	}
}
