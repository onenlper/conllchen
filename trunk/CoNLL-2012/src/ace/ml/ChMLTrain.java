package ace.ml;
import mentionDetect.MentionDetect;
import model.EntityMention;
import ace.CRFMention;

public class ChMLTrain extends ACEML{

	public ChMLTrain(String args[]) {
		super(args);
	}
	
	public static void main(String args[]) {
		EntityMention.ace = true;
		String as[] = {"1_2_3_4", "train"};
		ACEML.goldMentions = true;
		ACEML chMLTrain = new ChMLTrain(as);
		MentionDetect md = new CRFMention();
		CorefFeature corefFeature = new ChCorefRuleFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
	}
}
