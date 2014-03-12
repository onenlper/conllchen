package test;

import java.util.ArrayList;
import java.util.HashSet;

import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import util.Common;

public class GetPredict {
	public static void main(String args[]) {
		ArrayList<String> files1 = Common.getLines("chinese_list_all_train");
		ArrayList<String> files2 = Common.getLines("chinese_list_all_test");
		ArrayList<String> files3 = Common.getLines("chinese_list_all_development");
		ArrayList<String> files = new ArrayList<String>();
		files.addAll(files1);
		files.addAll(files2);
		files.addAll(files3);
		HashSet<String> predicts = new HashSet<String>();
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			ArrayList<CoNLLPart> parts = document.getParts();
			for(CoNLLPart part : parts) {
				for(CoNLLSentence sentence : part.getCoNLLSentences()) {
					for(CoNLLWord word : sentence.words) {
						if(!word.predicateLemma.equals("-")) {
							predicts.add(word.word);
						}
					}
				}
			}
		}
		Common.outputHashSet(predicts, "predicts");
	}
}
