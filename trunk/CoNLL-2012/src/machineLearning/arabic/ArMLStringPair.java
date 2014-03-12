package machineLearning.arabic;

import java.util.ArrayList;
import java.util.Collections;

import util.Common;

import machineLearning.CorefFeature;
import machineLearning.ML;
import machineLearning.arabic.ArCorefFeature.Atom;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;

public class ArMLStringPair extends ML{

	public ArMLStringPair(String fileListFn) {
		super(fileListFn);
	}

	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		ML arMLTrain = new ArMLStringPair("arabic_list_" + args[0] + "_train");
		MentionDetect md = new ParseTreeMention();
		CorefFeature corefFeature = new ArCorefFeature(arMLTrain);
		arMLTrain.config(md, corefFeature);
		arMLTrain.creatAltafFormat();
		
		ArrayList<Atom> positiveHeadPairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeHeadPairs = new ArrayList<Atom>();
		positiveHeadPairs.addAll(ArCorefFeature.positiveHeadPair.values());
		negativeHeadPairs.addAll(ArCorefFeature.negativeHeadPair.values());
		
		Collections.sort(positiveHeadPairs);
		Collections.sort(negativeHeadPairs);
		
		ArrayList<Atom> allHeadPairs = new ArrayList<Atom>();
		
		for(String pair : ArCorefFeature.allHeadPairs) {
			double positive = 0;
			double negative = 0;
			if(ArCorefFeature.positiveHeadPair.containsKey(pair)) {
				positive = ArCorefFeature.positiveHeadPair.get(pair).count;
			}
			if(ArCorefFeature.negativeHeadPair.containsKey(pair)) {
				negative = ArCorefFeature.negativeHeadPair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allHeadPairs.add(new Atom(pair, possi));
		}
		Collections.sort(allHeadPairs);
		outputPair(positiveHeadPairs, "arabic_" + args[0]+"_head_positive");
		outputPair(negativeHeadPairs, "arabic_" + args[0]+"_head_negative");
		outputPair(allHeadPairs, "arabic_" + args[0]+"_head_all");
		
		
		ArrayList<Atom> positiveSourcePairs = new ArrayList<Atom>();
		ArrayList<Atom> negativeSourcePairs = new ArrayList<Atom>();
		positiveSourcePairs.addAll(ArCorefFeature.positiveSourcePair.values());
		negativeSourcePairs.addAll(ArCorefFeature.negativeSourcePair.values());
		
		Collections.sort(positiveSourcePairs);
		Collections.sort(negativeSourcePairs);
		
		ArrayList<Atom> allSourcePairs = new ArrayList<Atom>();
		
		for(String pair : ArCorefFeature.allSourcePairs) {
			double positive = 0;
			double negative = 0;
			if(ArCorefFeature.positiveSourcePair.containsKey(pair)) {
				positive = ArCorefFeature.positiveSourcePair.get(pair).count;
			}
			if(ArCorefFeature.negativeSourcePair.containsKey(pair)) {
				negative = ArCorefFeature.negativeSourcePair.get(pair).count;
			}
			double possi = (positive)/(negative+positive);
			allSourcePairs.add(new Atom(pair, possi));
		}
		Collections.sort(allSourcePairs);
		outputPair(positiveSourcePairs, "arabic_" + args[0]+"_source_positive");
		outputPair(negativeSourcePairs, "arabic_" + args[0]+"_source_negative");
		outputPair(allSourcePairs, "arabic_" + args[0]+"_source_all");
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
