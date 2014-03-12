package mentionDetection.arabic;

import java.util.ArrayList;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ArabicTrain {
	
	public static void main(String args[]) throws Exception{
		String language = args[0];
		String folder = args[1];
		ArrayList<String> files = Common.getLines(language + "_list_" + folder + "_train");
		ArrayList<String> features = new ArrayList<String>(); 
		for(String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);
			String predictFile = file.replace("gold", "auto");
			CoNLLDocument predictDocument = new CoNLLDocument(predictFile);
			for(int i=0;i<document.getParts().size();i++) {
				CoNLLPart part = document.getParts().get(i);
				CoNLLPart predictPart = predictDocument.getParts().get(i);
				ArrayList<Entity> entities = part.getChains();
				HashSet<Integer> Bs = new HashSet<Integer>();
				HashSet<Integer> Is = new HashSet<Integer>();
				for(Entity entity : entities) {
					for(EntityMention mention : entity.mentions) {
						Bs.add(mention.start);
						for(int m=mention.start+1;m<=mention.end;m++) {
							Is.add(m);
						}
					}
				}
				for(int k=0;k<part.getCoNLLSentences().size();k++) {
					CoNLLSentence sentence = part.getCoNLLSentences().get(k);
					CoNLLSentence predictSentence = predictPart.getCoNLLSentences().get(k);
					for(int j=0;j<sentence.getWords().size();j++) {
						CoNLLWord word = sentence.getWord(j);
						CoNLLWord predictWord = predictSentence.getWord(j);
						ArabicCRFFeature feature = new ArabicCRFFeature();
						feature.setBuck(word.getArBuck());
						feature.setLemma(word.getArLemma());
						feature.setOrig(word.orig);
						feature.setUnBuck(word.getArUnBuck());
						feature.setUtf8(word.word);
						feature.setPosTag(predictWord.getPosTag());
						
						feature.setWordIndex(Integer.toString(word.getIndex()));
						feature.setPartID(Integer.toString(part.getPartID()));
						feature.setDocumentID(document.getDocumentID());
						feature.setFilePath(document.getFilePath());
						
						int wordIndex = word.getIndex();
						if(Is.contains(wordIndex)) {
							feature.setLabel("I");
						}
						if(Bs.contains(wordIndex)) {
							feature.setLabel("B");
						}
						MyTreeNode leaf = sentence.getSyntaxTree().leaves.get(j);
						ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
						boolean isINNP = false;
						for(MyTreeNode tn : ancestors) {
							if(tn.value.startsWith("NP")) {
								isINNP = true;
								break;
							}
						}
						if(isINNP) {
							feature.setInNP("I");
						} else {
							feature.setInNP("O");
						}
						features.add(feature.toString());
					}
					features.add("\n");
				}
			}
		}
		Common.outputLines(features, language + "_list_" + folder + ".mdTrain.short");
	}
}
