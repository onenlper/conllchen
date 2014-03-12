package machineLearning.english;

import machineLearning.CorefFeature;
import machineLearning.ML;
import mentionDetect.GoldMention;
import mentionDetect.MentionDetect;

public class EnMLTrain extends ML {

	public EnMLTrain(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if (args.length < 1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML enMLTrain = new EnMLTrain("english_list_" + args[0] + "_train");
		MentionDetect md = new GoldMention();
		CorefFeature corefFeature = new EnCorefFeature(enMLTrain);
		enMLTrain.config(md, corefFeature);
		enMLTrain.creatAltafFormat();
	}
}
