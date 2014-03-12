package ace;

import java.util.ArrayList;
import java.util.HashMap;

import mentionDetect.MentionDetect;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;

// get mentions from a CRF test result file
public class CRFMention extends MentionDetect{

	static HashMap<String, ArrayList<EntityMention>> allSemanticResult;
	
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		if(allSemanticResult==null) {
			allSemanticResult= ChCommon.loadSemanticResult();
		}
		ArrayList<EntityMention> mentions = mentionses.get(part.getDocument().getFilePath());
		// sign start, end
		for(EntityMention mention : mentions) {
			ACECorefCommon.assingStartEnd(mention, part);
		}
		// assign semantic
		ArrayList<EntityMention> semanticM = allSemanticResult.get(part.getDocument().getFilePath());
		for(EntityMention mention : mentions) {
			for(EntityMention sm : semanticM) {
				if(mention.headCharStart==sm.headStart && mention.headCharEnd==sm.headEnd) {
					mention.semClass = sm.semClass;
					mention.subType = sm.subType;
					break;
				}
			}
//			System.out.println(mention.head + "#" + mention.semClass + "#" + mention.subType);
		}
		return mentions;
	}
	
	String crfFile;
	ArrayList<EntityMention> currentArrayList;
	
	public CRFMention() {
		this.crfFile = "/users/yzcchen/tool/CRF/CRF++-0.54/yy" + Common.part;
		if(mentionses==null) {
			mentionses = ACECorefCommon.getMentionsFromCRFFile(Common.getLines("ACE_" + Common.part), crfFile);
		}
	}
	
	static HashMap<String, ArrayList<EntityMention>> mentionses;
}
