package CoNLLZeroPronoun.coref;

import java.util.ArrayList;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ZeroUtil {
	public static String getTree(EntityMention m, EntityMention zero,
			CoNLLPart part) {

		CoNLLSentence s1 = part.getWord(m.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;

		int mS = part.getWord(m.start).indexInSentence;
		int mE = part.getWord(m.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;
		int zE = zS;

		MyTreeNode bigRoot = null;
		MyTreeNode mST = null;
		MyTreeNode mET = null;
		MyTreeNode zST = null;
		MyTreeNode zET = null;
		if (s1 == s2) {
			bigRoot = s1.getSyntaxTree().root.copy();
			mST = bigRoot.getLeaves().get(mS);
			mET = bigRoot.getLeaves().get(mE);

			zST = bigRoot.getLeaves().get(zS);
			zET = bigRoot.getLeaves().get(zE);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i).getSyntaxTree().root
						.copy();
				bigRoot.addChild(root);
			}
			mST = bigRoot.children.get(0).getLeaves().get(mS);
			mET = bigRoot.children.get(0).getLeaves().get(mE);

			zST = bigRoot.children.get(bigRoot.children.size() - 1).getLeaves()
					.get(zS);
			zET = zST;
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mST, zST);
		lowest.mark = true;
		// find VP node
		ArrayList<MyTreeNode> vps = zST.getXAncestors("VP");
		for (MyTreeNode vp : vps) {
			if (vp.getLeaves().get(0) == zST) {
				// attach zp
				int vpChildID = vp.childIndex;
				MyTreeNode zeroNP = new MyTreeNode("NP");
				MyTreeNode zeroNode = new MyTreeNode("XXX");
				zeroNP.addChild(zeroNode);
				vp.parent.addChild(vpChildID, zeroNP);
				break;
			}
		}
		// mark shortest path
		for (int i = mST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = mST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		for (int i = mET.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = mET.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		for (int i = zST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = zST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		
		int startLeaf = 0;
		int endLeaf = bigRoot.getLeaves().size()-1; 
		for(int i=0;i<bigRoot.getLeaves().size();i++) {
			if(bigRoot.getLeaves().get(i)==mST) {
				startLeaf = i;
			} else if(bigRoot.getLeaves().get(i)==zST) {
				endLeaf = i;
			}
		}
		
		// attach competitors
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			// if under np, mark all np
			for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
				MyTreeNode node = leaf.getAncestors().get(j);
				if (node == lowest) {
					break;
				}
				if (node.value.equalsIgnoreCase("np")) {
					node.setAllMark(true);
					// find predicate
					for (MyTreeNode sibling : node.parent.children) {
						if (sibling.value.equalsIgnoreCase("VV")) {
							sibling.setAllMark(true);
						}
					}
					break;
				}
			}
		}

		// attach verb
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			if (leaf.parent.value.startsWith("V")) {
				// if predicate, see if there is subject or object
				for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
					MyTreeNode node = leaf.getAncestors().get(j);
					if (node == lowest) {
						break;
					}
					node.mark = true;
				}
			}
		}

		// prune it!!! single in and single out , attach to grand
		ArrayList<MyTreeNode> offsprings = lowest.getDepthFirstOffsprings();
		for (MyTreeNode node : offsprings) {
			// skip pos tag
			if (node.children.size() == 1
					&& node.children.get(0).children.size() == 0) {
				continue;
			}
			if(node.children.size()==0) {
				continue;
			}
			// remove this
			if(node.parent!=null && node.parent.numberMarkChildren()==1 && node.numberMarkChildren()==1) {
				node.parent.children.clear();
				node.parent.children.addAll(node.children);
			}
		}
		// mark min-expansion
		return lowest.getTreeBankStyle(true);
	}

}
