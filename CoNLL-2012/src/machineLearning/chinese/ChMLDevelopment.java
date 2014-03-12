package machineLearning.chinese;

import util.ChCommon;
import machineLearning.CorefFeature;
import machineLearning.ML;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;

public class ChMLDevelopment extends ML{

	public ChMLDevelopment(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder [test|develpment]");
			System.exit(1);
		}
		ChCommon.loadPredictNE(args[0], args[1]);
		ML chMLTrain = new ChMLTrain("chinese_list_" + args[0] + "_" + args[1]);
		MentionDetect md = new ParseTreeMention();
		CorefFeature corefFeature = new ChCorefFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
	}
}
