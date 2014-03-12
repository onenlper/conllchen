package machineLearning.chinese;

import java.util.ArrayList;
import java.util.Collections;

import util.Common;

import machineLearning.CorefFeature;
import machineLearning.ML;
import machineLearning.chinese.ChCorefFeature.Atom;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;

public class ChMLStringPair extends ML{

	public ChMLStringPair(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML chMLTrain = new ChMLStringPair("chinese_list_" + args[0] + "_train");
		MentionDetect md = new ParseTreeMention();
		CorefFeature corefFeature = new ChCorefFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
		
		ArrayList<Atom> positiveHeadPairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeHeadPairs = new ArrayList<Atom>();
		positiveHeadPairs.addAll(ChCorefFeature.positiveHeadPair.values());
		negativeHeadPairs.addAll(ChCorefFeature.negativeHeadPair.values());
		
		Collections.sort(positiveHeadPairs);
		Collections.sort(negativeHeadPairs);
		
		ArrayList<Atom> allHeadPairs = new ArrayList<Atom>();
		
		for(String pair : ChCorefFeature.allHeadPairs) {
			double positive = 0;
			double negative = 0;
			if(ChCorefFeature.positiveHeadPair.containsKey(pair)) {
				positive = ChCorefFeature.positiveHeadPair.get(pair).count;
			}
			if(ChCorefFeature.negativeHeadPair.containsKey(pair)) {
				negative = ChCorefFeature.negativeHeadPair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allHeadPairs.add(new Atom(pair, possi));
		}
		Collections.sort(allHeadPairs);
		outputPair(positiveHeadPairs, "chinese_" + args[0]+"_head_positive");
		outputPair(negativeHeadPairs, "chinese_" + args[0]+"_head_negative");
		outputPair(allHeadPairs, "chinese_" + args[0]+"_head_all");
		
		
		ArrayList<Atom> positiveSourcePairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeSourcePairs = new ArrayList<Atom>();
		positiveSourcePairs.addAll(ChCorefFeature.positiveSourcePair.values());
		negativeSourcePairs.addAll(ChCorefFeature.negativeSourcePair.values());
		
		Collections.sort(positiveSourcePairs);
		Collections.sort(negativeSourcePairs);
		
		ArrayList<Atom> allSourcePairs = new ArrayList<Atom>();
		
		for(String pair : ChCorefFeature.allSourcePairs) {
			double positive = 0;
			double negative = 0;
			if(ChCorefFeature.positiveSourcePair.containsKey(pair)) {
				positive = ChCorefFeature.positiveSourcePair.get(pair).count;
			}
			if(ChCorefFeature.negativeSourcePair.containsKey(pair)) {
				negative = ChCorefFeature.negativeSourcePair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allSourcePairs.add(new Atom(pair, possi));
		}
		Collections.sort(allSourcePairs);
		outputPair(positiveSourcePairs, "chinese_" + args[0]+"_source_positive");
		outputPair(negativeSourcePairs, "chinese_" + args[0]+"_source_negative");
		outputPair(allSourcePairs, "chinese_" + args[0]+"_source_all");
	}

	public static void outputPair(ArrayList<Atom> atoms, String filePath) {
		Collections.reverse(atoms);
		ArrayList<String> lines = new ArrayList<String>();
		for(Atom atom : atoms) {
			StringBuilder sb = new StringBuilder();
			sb.append(atom.key).append(" ").append(atom.count);
			lines.add(sb.toString());
		}
		Common.outputLines(lines, filePath);
	}
}
