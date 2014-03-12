package ace.ml;

import mentionDetect.MentionDetect;
import model.EntityMention;
import ace.CRFMention;

public class ChMLDevelopment extends ACEML{

	public ChMLDevelopment(String args[]) {
		super(args);
	}

	public static void main(String args[]) {
		EntityMention.ace = true;
		String as[] = {"0", "test"};
		ACEML.goldMentions = true;
		ACEML chMLTrain = new ChMLDevelopment(as);
		MentionDetect md = new CRFMention();
		CorefFeature corefFeature = new ChCorefRuleFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
	}
}
