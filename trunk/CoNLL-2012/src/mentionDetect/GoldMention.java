package mentionDetect;

import java.util.ArrayList;

import util.Common;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;

public class GoldMention extends MentionDetect{

	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		return	part.getMentions();
	}
	
	public static void main(String args[]) {
		MentionDetect gm = new GoldMention();
		String language = args[0];
		String folder = args[1];
		ArrayList<String> files = Common.getLines(language + "_list_" + folder + "_test");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);
			CoNLLDocument document = new CoNLLDocument(conllFn);
			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<EntityMention> goldEMs = gm.getMentions(part);
				for(EntityMention mention : goldEMs) {
					int end = mention.end;
					if(part.getWord(end).word.equalsIgnoreCase("'s")) {
						System.out.println("@" + mention.original) ;
					} else if(part.getWord(end+1).word.equalsIgnoreCase("'s")) {
						System.out.println("#" + mention.original) ;
					}
				}
			}
		}
	}
}
