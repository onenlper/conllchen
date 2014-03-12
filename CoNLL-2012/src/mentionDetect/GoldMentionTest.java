package mentionDetect;

import java.util.ArrayList;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;

public class GoldMentionTest extends MentionDetect{

	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		ArrayList<EntityMention> mentions;
		String filePath = part.getDocument().getFilePath();
		int idx = filePath.lastIndexOf('.');
		String goldMentionFile = filePath.substring(0, idx) + ".v5_auto_mentions_skel";
		CoNLLDocument document2 = new CoNLLDocument(goldMentionFile);
		CoNLLPart part2 = document2.getParts().get(part.getPartID());
		mentions = part2.getMentions();
		return mentions;
	}
	
}
