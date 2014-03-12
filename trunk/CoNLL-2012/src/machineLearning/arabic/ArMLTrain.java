package machineLearning.arabic;

import machineLearning.CorefFeature;
import machineLearning.ML;
import machineLearning.english.EnCorefFeature;
import mentionDetect.GoldMention;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;

public class ArMLTrain extends ML {

	public ArMLTrain(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if (args.length !=2) {
			System.out.println("java ~ folder [train|development]");
			System.exit(1);
		}
		if(args[1].equals("train")) {
			ML arMLTrain = new ArMLTrain("arabic_list_" + args[0] + "_train");
			MentionDetect md = new GoldMention();
			CorefFeature corefFeature = new ArCorefFeature(arMLTrain);
			arMLTrain.config(md, corefFeature);
			arMLTrain.creatAltafFormat();
		} else if(args[1].equals("development")) {
			ML arMLTrain = new ArMLTrain("arabic_list_" + args[0] + "_development");
			MentionDetect md = new ParseTreeMention();
			CorefFeature corefFeature = new ArCorefFeature(arMLTrain);
			arMLTrain.config(md, corefFeature);
			arMLTrain.creatAltafFormat();
		}
	}
}
