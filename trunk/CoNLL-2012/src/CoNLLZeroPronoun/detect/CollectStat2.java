package CoNLLZeroPronoun.detect;

import java.util.ArrayList;
import java.util.Collections;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;

public class CollectStat2 {

	public static EntityMention getAntecedent(ArrayList<EntityMention> mentions, EntityMention zero, int idx) {
		for (int i = idx - 1; i >= 0; i--) {
			EntityMention em = mentions.get(i);
			if (em.end != -1) {
				return em;
			}
		}
		return null;
	}

	public static void main(String args[]) {
		String folder = args[0];
		ArrayList<String> files = new ArrayList<String>();
		files.addAll(Common.getLines("chinese_list_" + folder + "_development"));
		// files.addAll(Common.getLines("chinese_list_" + folder +
		// "_development"));

		double dises[] = new double[10000];

		double overall = 0;
		
		int sentences = 0;
		int words = 0;
		int ZP = 0;
		int AZP = 0;
		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

//			OntoCorefXMLReader.addGoldZeroPronouns(document, true);

			ArrayList<ArrayList<EntityMention>> zeroses = OntoCorefXMLReader.getGoldZeroPronouns(document, true);
			for(ArrayList<EntityMention> zeros : zeroses) {
				ZP += zeros.size();
			}
			
			for (int i = 0; i < document.getParts().size(); i++) {
				CoNLLPart part = document.getParts().get(i);
				for(CoNLLSentence s : part.getCoNLLSentences()) {
					words += s.getWords().size();
				}
				sentences += part.getCoNLLSentences().size();
				
				ArrayList<Entity> entities = part.getChains();
				for (Entity entity : entities) {
					Collections.sort(entity.mentions);
					for (int m = 0; m < entity.mentions.size(); m++) {
						EntityMention m2 = entity.mentions.get(m);
						if (m2.end == -1) {
							EntityMention m1 = getAntecedent(entity.mentions, m2, m);
							if (m1 != null) {
								AZP++;
								int dis = part.getWord(m2.start).sentence.getSentenceIdx()
										- part.getWord(m1.start).sentence.getSentenceIdx();
								if (dis == -1) {
									dises[9999]++;
								} else {
									dises[dis]++;
								}
								overall++;
							}
						}
					}
				}
			}
		}
		for (int k = 0; k < dises.length; k++) {
			if (dises[k] != 0) {
				System.out.println(k + ":" + dises[k] + " : " + dises[k] / overall);
			}
		}
		System.out.println("S: " + sentences);
		System.out.println("W: " + words);
		System.out.println("ZP: " + ZP);
		System.out.println("AZP: " + AZP);
	}
}
