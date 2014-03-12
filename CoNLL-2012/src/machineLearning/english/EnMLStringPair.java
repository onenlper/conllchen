package machineLearning.english;

import java.util.ArrayList;
import java.util.Collections;

import machineLearning.CorefFeature;
import machineLearning.ML;
import machineLearning.english.EnCorefFeature.Atom;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import util.Common;

public class EnMLStringPair extends ML{

	public EnMLStringPair(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML chMLTrain = new EnMLStringPair("english_list_" + args[0] + "_train");
		MentionDetect md = new ParseTreeMention();
		CorefFeature corefFeature = new EnCorefFeature(chMLTrain);
		chMLTrain.config(md, corefFeature);
		chMLTrain.creatAltafFormat();
		
		ArrayList<Atom> positiveHeadPairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeHeadPairs = new ArrayList<Atom>();
		positiveHeadPairs.addAll(EnCorefFeature.positiveHeadPair.values());
		negativeHeadPairs.addAll(EnCorefFeature.negativeHeadPair.values());
		
		Collections.sort(positiveHeadPairs);
		Collections.sort(negativeHeadPairs);
		
		ArrayList<Atom> allHeadPairs = new ArrayList<Atom>();
		
		for(String pair : EnCorefFeature.allHeadPairs) {
			double positive = 0;
			double negative = 0;
			if(EnCorefFeature.positiveHeadPair.containsKey(pair)) {
				positive = EnCorefFeature.positiveHeadPair.get(pair).count;
			}
			if(EnCorefFeature.negativeHeadPair.containsKey(pair)) {
				negative = EnCorefFeature.negativeHeadPair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allHeadPairs.add(new Atom(pair, possi));
		}
		Collections.sort(allHeadPairs);
		outputPair(positiveHeadPairs, "english_" + args[0]+"_head_positive");
		outputPair(negativeHeadPairs, "english_" + args[0]+"_head_negative");
		outputPair(allHeadPairs, "english_" + args[0]+"_head_all");
		
		
		ArrayList<Atom> positiveSourcePairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeSourcePairs = new ArrayList<Atom>();
		positiveSourcePairs.addAll(EnCorefFeature.positiveSourcePair.values());
		negativeSourcePairs.addAll(EnCorefFeature.negativeSourcePair.values());
		
		Collections.sort(positiveSourcePairs);
		Collections.sort(negativeSourcePairs);
		
		ArrayList<Atom> allSourcePairs = new ArrayList<Atom>();
		
		for(String pair : EnCorefFeature.allSourcePairs) {
			double positive = 0;
			double negative = 0;
			if(EnCorefFeature.positiveSourcePair.containsKey(pair)) {
				positive = EnCorefFeature.positiveSourcePair.get(pair).count;
			}
			if(EnCorefFeature.negativeSourcePair.containsKey(pair)) {
				negative = EnCorefFeature.negativeSourcePair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allSourcePairs.add(new Atom(pair, possi));
		}
		Collections.sort(allSourcePairs);
		outputPair(positiveSourcePairs, "english_" + args[0]+"_source_positive");
		outputPair(negativeSourcePairs, "english_" + args[0]+"_source_negative");
		outputPair(allSourcePairs, "english_" + args[0]+"_source_all");
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
