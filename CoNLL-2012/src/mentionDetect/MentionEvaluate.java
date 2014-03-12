package mentionDetect;

import java.util.ArrayList;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;

public class MentionEvaluate {
	public static void main(String args[]) {
		if (args.length < 1) {
			System.out.println("java ~ chinese");
			return;
		}
		ruleCoreference.chinese.RuleCoref.open = false;
		String language = args[0];
		// String folder = args[1];
		// ChCommon.loadPredictNE(folder, "test");
		MentionDetect md = new ParseTreeMention();

		evaluate(language, md);
	}

	public static void evaluate(String language, MentionDetect dm) {
		MentionDetect gm = new GoldMention();
//		MentionDetect gm = new ParseTreeMention();
		ArrayList<String> files = Common.getLines(language + "_list_all_test");
		int goldEmAmount = 0;
		int emsesAmount = 0;
		int matchAmount = 0;
		
		double yes = 0;
		double no = 0;
		
		// files.clear();
		// files.add("/users/yzcchen/CoNLL-2012/conll-2012/v2/data/development/data/english/annotations/bc/cctv/00/cctv_0000.v2_auto_conll");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);
			CoNLLDocument document = new CoNLLDocument(conllFn);

			String goldPath = conllFn
//			.replace("/test/", "/test_gold/")
			.split("\\.")[0] + ".v6_gold_parse_mention_boundaries_skel"
			;
			CoNLLDocument goldDocument = new CoNLLDocument(goldPath);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<EntityMention> predictEMs = dm.getMentions(document.getParts().get(k));
				ArrayList<EntityMention> goldEMs = gm.getMentions(goldDocument.getParts().get(k));
				goldEmAmount += goldEMs.size();
				emsesAmount += predictEMs.size();
				for (EntityMention goldEm : goldEMs) {
					boolean find = false;
					for (int j = 0; j < predictEMs.size(); j++) {
						EntityMention em = predictEMs.get(j);
						if (goldEm.equals(em)) {
							matchAmount++;
							// predictEMs.remove(j);
							j--;
							find = true;
							break;
						}
					}
				}

				HashSet<EntityMention> predicts = new HashSet<EntityMention>();
				HashSet<EntityMention> golds = new HashSet<EntityMention>();
				predicts.addAll(predictEMs);
				golds.addAll(goldEMs);
				
				HashSet<String> words = new HashSet<String>();
				for(EntityMention predict : predicts) {
					words.add(predict.source);
					words.add(part.getWord(predict.end).word);
				}
				
				// System.out.println("Precision error: ");
//				for (EntityMention mention : predicts) {
//					if (!golds.contains(mention) && mention.tag) {
//						 System.out.println(mention.source);
//					}
//				}
				System.out.println("Recall error: ");
				for (EntityMention mention : golds) {
					if (!predicts.contains(mention)) {
						System.out.println(mention.source + "#" + part.getWord(mention.end).sentence.getSentenceIdx() + "#" +  part.getWord(mention.end).indexInSentence + words.contains(mention.source));
						if(words.contains(mention.source)) {
							yes++;
						} else {
							no++;
						}
					}
				}
			}
		}

		System.out.println("Golden mention amount: " + goldEmAmount);
		System.out.println("Predicted mention amount: " + emsesAmount);
		double recall = (double) matchAmount / (double) goldEmAmount;
		System.out.println("Recall is: " + recall * 100);
		double precision = (double) matchAmount / (double) emsesAmount;
		System.out.println("Precision is: " + precision * 100);
		System.out.println("F score is: " + 2 * precision * recall / (precision + recall) * 100);
		System.out.println("Hits: " + matchAmount);

		System.out.println(yes/(yes+no));
	}
}
