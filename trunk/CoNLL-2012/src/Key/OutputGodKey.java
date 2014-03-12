package Key;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import util.Common;

import coref.OntoAltafToSemEvalOffical.CRFElement;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;

public class OutputGodKey {

	public static void main(String args[]) throws Exception {
		ArrayList<String> files = Common.getLines("chinese_list_nw_train");
		FileWriter fw = new FileWriter("/users/yzcchen/tool/Scorer/testSystem.key");
		for(String file : files) {
			CoNLLDocument document = new CoNLLDocument(file);
			for (CoNLLPart part : document.getParts()) {
				ArrayList<Entity> entities = part.getChains();
				writerKey(fw, entities, part);
			}
		}
		fw.close();
	}

	public static void writerKey(FileWriter systemKeyFw, ArrayList<Entity> systemChain, CoNLLPart part)
			throws IOException {
		HashSet<Integer> sentenceEnds = new HashSet<Integer>();
		for (CoNLLSentence sentence : part.getCoNLLSentences()) {
			sentenceEnds.add(sentence.getEndWordIdx());
		}

		systemKeyFw.write(part.label + "\n");
		ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
		int maxWord = part.getWordCount();
		for (int i = 0; i < maxWord; i++) {
			elements.add(new CRFElement());
		}
		for (int i = 0; i < systemChain.size(); i++) {
			Entity en = systemChain.get(i);
			for (EntityMention em : en.mentions) {
				int start = em.start;
				int end = em.end;
				StringBuilder sb = new StringBuilder();

				// get mention type
				System.out.println(part.getWord(em.end).posTag + ":" + em.source);
				String headPOS = "";
				for(int h=end;h>=start;h--) {
					String POS = part.getWord(em.end).posTag;
					if(POS.startsWith("PN")||POS.startsWith("NN")||POS.startsWith("NR")) {
						headPOS = POS;
						break;
					}
				}
				String type = "";
//				if(headPOS.startsWith("PN")) {
//					type = "PN";
//				} else if(headPOS.startsWith("NR")) {
//					type = "NR";
//				} else {
//					type = "NN";
//				}
				 
				if (start == end) {
					sb.append("(").append(i + 1).append("@").append(type).append(")");
					elements.get(start).append(sb.toString());
				} else {
					elements.get(start).append("(" + Integer.toString(i + 1) + "@" + type);
					elements.get(end).append(Integer.toString(i + 1) + ")");
				}
			}
		}
		for (int i = 0; i < elements.size(); i++) {
			CRFElement element = elements.get(i);
			String sourceLine = part.getWord(i).sourceLine;
			if (element.predict.isEmpty()) {
				systemKeyFw.write(sourceLine + "	" + "-\n");
			} else {
				systemKeyFw.write(sourceLine + "	" + element.predict + "\n");
			}
			if (sentenceEnds.contains(i)) {
				systemKeyFw.write("\n");
			}
		}
		systemKeyFw.write("#end document\n");
	}
}
