package CoNLLZeroPronoun.coref;

import java.util.ArrayList;
import java.util.HashMap;

import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class CollectTransitivityVerb {
	public static void main(String args[]) {
		HashMap<String, Integer> tranMap = new HashMap<String, Integer>();
		HashMap<String, Integer> inTranMap = new HashMap<String, Integer>();
		
		ArrayList<String> fileList = Common.getLines("chinese_list_all_train");
		for(String file : fileList) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);
			for(CoNLLPart part : document.getParts()) {
				
				for(CoNLLSentence ss : part.getCoNLLSentences()) {
					ArrayList<MyTreeNode> leaves = ss.syntaxTree.leaves;
					
					for(MyTreeNode leaf : leaves) {
						MyTreeNode posNode = leaf.parent;
						if(posNode.value.startsWith("V")) {
							boolean trans = false;
							String key = leaf.value;
							ArrayList<MyTreeNode> laterSisters = posNode.getRightSisters(); 
							for(MyTreeNode node : laterSisters) {
								if(node.value.startsWith("NP") || node.value.startsWith("IP")) {
									trans = true;
									break;
								}
							}
							if(trans) {
								Common.increaseKey(tranMap, key);
							} else {
								Common.increaseKey(inTranMap, key);
							}
						}
					}
				}
			}
		}
		
		Common.outputHashMap(tranMap, "trans.verb");
		Common.outputHashMap(inTranMap, "intrans.verb");
	}
}
