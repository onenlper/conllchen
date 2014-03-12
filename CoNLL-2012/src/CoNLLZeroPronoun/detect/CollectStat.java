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

public class CollectStat {
	
	public static ArrayList<EntityMention> sortNPs(ArrayList<EntityMention> ms, EntityMention zero, CoNLLPart part) {
		ArrayList<EntityMention> ret = new ArrayList<EntityMention>();
		Collections.sort(ms);
		ArrayList<EntityMention> ret1 = new ArrayList<EntityMention>();
		ArrayList<EntityMention> ret2 = new ArrayList<EntityMention>();
		ArrayList<EntityMention> ret3 = new ArrayList<EntityMention>();
		for(int k=ms.size()-1;k>=0;k--) {
			EntityMention m = ms.get(k);
			int dis = part.getWord(m.start).sentence.getSentenceIdx()
			- part.getWord(zero.start).sentence.getSentenceIdx();
			
			if(dis>0) {
				continue;
			} else if (dis==0){
				if(m.compareTo(zero)>0) {
					if(m.end!=-1) {
						ret2.add(0, m);
					}
				} else if(m.compareTo(zero)<0) {
					ret1.add(m);
				}
			} else {
				ret3.add(m);
			}
		}
		ret.addAll(ret1);
		ret.addAll(ret2);
		ret.addAll(ret3);
		return ret;
	}
	
	public static void main(String args[]) {
		String folder = args[0];
		ArrayList<String> files = new ArrayList<String>();
		files.addAll(Common.getLines("chinese_list_" + folder + "_train"));
//		files.addAll(Common.getLines("chinese_list_" + folder + "_development"));

		double dises[] = new double[10000];

		double overall = 0;

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int i = 0; i < document.getParts().size(); i++) {
				CoNLLPart part = document.getParts().get(i);
				ArrayList<Entity> entities = part.getChains();
				for (Entity entity : entities) {
					for (int m = 0; m < entity.mentions.size(); m++) {
						EntityMention m2 = entity.mentions.get(m);
						if (m2.end == -1) {
							int dis = -1;
							ArrayList<EntityMention> cands = sortNPs(entity.mentions,m2, part);
							for (EntityMention m1 : cands) {
								int d = part.getWord(m2.start).sentence.getSentenceIdx()
										- part.getWord(m1.start).sentence.getSentenceIdx();
								if (d >= 0 && m1!=m2
										&& m1.compareTo(m2)<0
										&& !(m1.end==-1 && m2.compareTo(m1)<0) 
										&& m1.end!=-1
								) {
									// if(m1.end!=-1) {
									dis = d;
									if(m1.compareTo(m2)>0) {
										
										CoNLLSentence s = part.getWord(m2.start).sentence;
										StringBuilder sb = new StringBuilder();
										for(int t=part.getWord(m2.start).indexInSentence;t<s.words.size();t++) {
											sb.append(s.words.get(t).word).append(" ");
										}
										System.out.println(part.getWord(m2.start).indexInSentence + ":" + sb.toString()
												+ " # " + m1.source);
										
										
									}
									break;
								}
								// }
							}
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
		for (int k = 0; k < dises.length; k++) {
			if (dises[k] != 0) {
				System.out.println(k + ":" + dises[k] + " : " + dises[k] / overall);
			}
		}
	}
}
