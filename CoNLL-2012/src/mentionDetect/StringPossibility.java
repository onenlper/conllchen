package mentionDetect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import util.Common;

public class StringPossibility {
	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ chinese folder");
			return;
		}
		String language = args[0];
		String folder = args[1];
		MentionDetect md = new ParseTreeMention();
		evaluate(language, md, folder);
	}
	
	public static void evaluate(String language, MentionDetect dm, String folder) {
		GoldMention gm = new GoldMention();
		ArrayList<String> files = Common.getLines(language + "_list_" + folder + "_train");
		HashMap<String, double[]> stats = new HashMap<String, double[]>();
		HashMap<String, double[]> statsSrc = new HashMap<String, double[]>();
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			for (int k = 0; k < document.getParts().size(); k++) {
				ArrayList<EntityMention> predictEMs = dm.getMentions(document.getParts().get(k));
				ArrayList<EntityMention> goldEMs = gm.getMentions(document.getParts().get(k));
				HashSet<EntityMention> golds = new HashSet<EntityMention>(); 
				golds.addAll(goldEMs);
				for(EntityMention em : predictEMs) {
					if(em.isPronoun) {
						continue;
					}
					String head = em.head.toLowerCase();
					double[] num;
					if(stats.containsKey(head)) {
						num = stats.get(head);
					} else {
						num = new double[2];
						stats.put(head, num);
					}
					if(goldEMs.contains(em)) {
						num[0]++;
					} else {
						num[1]++;
					}
					String src = em.source.toLowerCase();
					if(language.equals("chinese")) {
						src = src.replace(" ", "");
					}
					if(statsSrc.containsKey(head)) {
						num = statsSrc.get(head);
					} else {
						num = new double[2];
						statsSrc.put(head, num);
					}
					if(goldEMs.contains(em)) {
						num[0]++;
					} else {
						num[1]++;
					}
				}
			}
		}
		HashMap<String, String> stats2 = new HashMap<String, String>();
		for(String key : stats.keySet()) {
			double[] num = stats.get(key);
			double poss = num[0]/(num[0]+num[1]);
			stats2.put(key, Double.toString(poss));
		}
		HashMap<String, String> stats2Src = new HashMap<String, String>();
		for(String key : statsSrc.keySet()) {
			double[] num = statsSrc.get(key);
			double poss = num[0]/(num[0]+num[1]);
			stats2Src.put(key, Double.toString(poss));
		}
		Common.outputHashMap(stats2, language + "_" + folder + "_mention");
		Common.outputHashMap(stats2Src, language + "_" + folder + "_mention_source");
	}
}
