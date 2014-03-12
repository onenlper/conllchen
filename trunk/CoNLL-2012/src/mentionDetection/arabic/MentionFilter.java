package mentionDetection.arabic;

import java.util.ArrayList;
import java.util.HashSet;

import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;

public class MentionFilter {
	public static void main(String args[]) {
		 HashSet<String> goldMentions = new HashSet<String>();
		 HashSet<String> predictMentions = new HashSet<String>();
		 ParseTreeMention md = new ParseTreeMention();
		 ArrayList<String> files = Common.getLines("arabic_list_all_train");
		 for(String file : files) {
			 System.out.println();
			 CoNLLDocument document = new CoNLLDocument(file);
			 for(CoNLLPart part : document.getParts()) {
				 ArrayList<Entity> entities = part.getChains();
				 for(Entity entity : entities) {
					 for(EntityMention mention : entity.mentions) {
						 goldMentions.add(mention.original);
					 }
				 }
				 ArrayList<EntityMention> predicts = md.getMentions(part);
				 for(EntityMention mention : predicts) {
					 predictMentions.add(mention.original);
				 }
			 }
		 }
		 ArrayList<String> outputs = new ArrayList<String>();
		 for(String predict : predictMentions) {
			 if(!goldMentions.contains(predict)) {
				 outputs.add(predict);
			 }
		 }
		 Common.outputLines(outputs, "arabic_not_mention");
	}
}
