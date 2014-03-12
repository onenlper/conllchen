package CoNLLZeroPronoun.coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTreeNode;
import util.ChCommon;
import util.Common;

public class RuleZeroCoref {
	
	public RuleZeroCoref() {
		
	}
	
	private MyTreeNode findTreeNode(EntityMention np, CoNLLPart part) {
		CoNLLWord leftWord = part.getWord(np.start);
		CoNLLWord rightWord = part.getWord(np.end);
		CoNLLSentence s = part.getWord(np.start).getSentence();
		MyTreeNode root = s.syntaxTree.root;
		MyTreeNode leftLeaf = root.getLeaves().get(leftWord.indexInSentence);
		MyTreeNode rightLeaf = root.getLeaves().get(rightWord.indexInSentence);
		
		MyTreeNode NP = Common.getLowestCommonAncestor(leftLeaf, rightLeaf);
		return NP;
	}
	
	private boolean subjectNP(EntityMention np, CoNLLPart part) {
		MyTreeNode npNode = this.findTreeNode(np, part);
		ArrayList<MyTreeNode> rightSisters = npNode.getRightSisters();
		for(MyTreeNode sister : rightSisters) {
			if(sister.value.equals("VP")) {
				return true;
			}
		}
		return false;
	}
	
	private boolean objectNP(EntityMention np, CoNLLPart part) {
		MyTreeNode npNode = this.findTreeNode(np, part);
		ArrayList<MyTreeNode> leftSisters = npNode.getLeftSisters();
		for(MyTreeNode sister : leftSisters) {
			if(sister.value.equals("VV")) {
				return true;
			}
		}
		return false;
	}
	
	private boolean firstGapOfSentence(EntityMention zero, CoNLLPart part) {
		if(zero.start==0) {
			return true;
		}
		if(part.getWord(zero.start).indexInSentence==0) {
			return true;
		}
		return false;
	}
	
	public EntityMention testRule(EntityMention zero, ArrayList<EntityMention> candidates, CoNLLPart part) {
		// if the first gap of the sentence, find the maximum subject NP of previous sentence
		
		int index = candidates.indexOf(zero);
		EntityMention candidate = candidates.get(index-1);
		for(int k =index-1;k>=0;k--) {
			EntityMention cand = candidates.get(k);
			if(cand.end==-1) {
				candidate = cand;
				break;
			} else if(this.subjectNP(cand, part)){
				candidate = cand;
				break;
			}
		}
		
		
		if(candidate.end==-1) {
			zero.antecedent = candidate.antecedent;
		} else {
			zero.antecedent = candidate;
		}
		
		
		return zero.antecedent;
	}
	
	public static ArrayList<EntityMention> getAnaphorZeros(ArrayList<Entity> chains) {
		ArrayList<EntityMention> zeros = new ArrayList<EntityMention>();
		for(Entity entity : chains) {
			
			for(int i=0;i<entity.mentions.size();i++) {
				
				EntityMention m2 = entity.mentions.get(i);
				if(m2.end!=-1) {
					continue;
				}
				
				for(int j=0;j<i;j++) {
					EntityMention m1 = entity.mentions.get(j);
					if(m1.end!=-1) {
						zeros.add(m2);
						break;
					}
				}
			}
		}
		return zeros;
	}
	
	
	public static void main(String args[]) {
		
		String folder = args[0];
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_development/");
		ChCommon.loadPredictNE(folder, "development");
		double good = 0;
		double bad = 0;

		RuleZeroCoref coref = new RuleZeroCoref();
		
		loop: for (String file : files) {

			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace("auto_conll", "gold_conll"));

			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Entity> goldChains = part.getChains();

				HashMap<String, Integer> chainMap = formChainMap(goldChains);

				ParseTreeMention ptm = new ParseTreeMention();

				ArrayList<EntityMention> nps = ptm.getMentions(part);
				
				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				ArrayList<EntityMention> zeros = getAnaphorZeros(part.getChains());
				candidates.addAll(nps);
				candidates.addAll(zeros);
				
				Collections.sort(candidates);
				
				Collections.sort(zeros);
				System.out.println(zeros.size());
				for (EntityMention zero : zeros) {
					EntityMention antecedent = coref.testRule(zero, candidates, part);
					if (antecedent!=null && antecedent.end!=-1 && chainMap.containsKey(zero.toName()) && chainMap.containsKey(antecedent.toName())
							&& chainMap.get(zero.toName()).intValue() == chainMap.get(antecedent.toName()).intValue()) {
						
						good++;
						System.out.println("========right=========");
					} else {
						bad++;
						System.out.println("========wrong=========");
					}
					System.out.println(antecedent.source);
					CoNLLSentence s = part.getWord(zero.start).sentence;
					StringBuilder sb = new StringBuilder();
					for(int i=part.getWord(zero.start).indexInSentence;i<s.words.size();i++) {
						sb.append(s.words.get(i).word).append(" ");
					}
					System.out.println(sb.toString());
				}
//				break loop;
			}
		}
		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad));
	}
	
	
	
	public static HashMap<String, Integer> formChainMap(ArrayList<Entity> entities) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < entities.size(); i++) {
			for (EntityMention m : entities.get(i).mentions) {
				map.put(m.toName(), i);
			}
		}
		return map;
	}
}
