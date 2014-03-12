package machineLearning.english;

import machineLearning.CorefFeature;
import machineLearning.ML;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;

public class EnMLDevelopment extends ML{

	public EnMLDevelopment(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML enMLTest = new EnMLDevelopment("english_list_" + args[0] + "_test");
		MentionDetect md = new ParseTreeMention();
//		MentionDetect md = new GoldMention();
		CorefFeature corefFeature = new EnCorefFeature(enMLTest);
		enMLTest.config(md, corefFeature);
		enMLTest.creatAltafFormat();
	}
}
