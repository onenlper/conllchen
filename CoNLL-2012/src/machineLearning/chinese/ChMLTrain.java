package machineLearning.chinese;

import machineLearning.CorefFeature;
import machineLearning.ML;
import mentionDetect.GoldMention;
import mentionDetect.MentionDetect;
import util.ChCommon;

public class ChMLTrain extends ML{

	public ChMLTrain(String fileListFn) {
		super(fileListFn);
		this.chCommon = new ChCommon("chinese");
	}
	
	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML chMLTrain = new ChMLTrain("chinese_list_" + args[0] + "_train");
		MentionDetect md = new GoldMention();
		CorefFeature corefFeature = new ChCorefFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
	}
}
