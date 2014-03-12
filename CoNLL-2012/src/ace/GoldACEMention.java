package ace;

import java.util.ArrayList;
import java.util.Collections;

import mentionDetect.MentionDetect;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;

public class GoldACEMention extends MentionDetect{

	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		ArrayList<Entity> entities = ACECommon.getEntities(part.getDocument().getFilePath());
		ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
		for(Entity en:entities) {
			for(EntityMention em: en.getMentions()) {
				em.source = em.extent;
				em.original = em.extent;

				ems.add(em);
			}
		}
		Collections.sort(ems);
		return ems;
	}

}
