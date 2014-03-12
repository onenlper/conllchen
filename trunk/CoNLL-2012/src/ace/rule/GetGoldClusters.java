package ace.rule;

import java.util.ArrayList;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;

public class GetGoldClusters {
	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines(args[0] + "_list_" + args[1] + "_development");
		HashSet<String> pronouns = new HashSet<String>();
		HashSet<String> pronouns2 = new HashSet<String>();
		MentionDetect md = new ParseTreeMention();
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			CoNLLDocument doc = new CoNLLDocument(conllFn);
			for (CoNLLPart part : doc.getParts()) {
				ArrayList<EntityMention> mentions = md.getMentions(part);
				for(EntityMention mention: mentions) {
					if(mention.isPronoun) {
						pronouns2.add(mention.head);
					}
				}
				ArrayList<Entity> chains = part.getChains();
				for (Entity chain : chains) {
					for (EntityMention mention : chain.mentions) {
						if (part.getWord(mention.end).posTag.startsWith("PN")) {
							pronouns.add(part.getWord(mention.end).word);
						}
					}
				}
			}
		}
		for (String str : pronouns2) {
			if(!pronouns.contains(str)) {
				System.out.println(str);
			}
		}
	}
}
