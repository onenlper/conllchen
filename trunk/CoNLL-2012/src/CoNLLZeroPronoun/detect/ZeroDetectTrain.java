package CoNLLZeroPronoun.detect;

import java.util.ArrayList;
import java.util.HashSet;

import CoNLLZeroPronoun.coref.RuleZeroCoref;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;

public class ZeroDetectTrain {

	String folder;

	public ZeroDetectTrain(String folder) {
		this.folder = folder;
	}

	public void train() {
		Common.outputLines(this.getTrainInstances(), "zeroTrain." + this.folder);
	}

	public ArrayList<String> getTrainInstances() {
		String folder = this.folder;
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train/");

		ArrayList<String> lines = new ArrayList<String>();

		double heru = 0;
		double gold = 0;

		ArrayList<ArrayList<EntityMention>> goldZeroses = new ArrayList<ArrayList<EntityMention>>();
		ArrayList<ArrayList<EntityMention>> systemZeroses = new ArrayList<ArrayList<EntityMention>>();

		ZeroDetect det = new ZeroDetect(true, folder);

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file);

//			ArrayList<ArrayList<EntityMention>> documentZeroses = OntoCorefXMLReader
//					.getGoldZeroPronouns(document, false);
			
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);
			
			int a = file.lastIndexOf("/");
			String name = file.substring(a + 1);
			for (int i = 0; i < document.getParts().size(); i++) {
				CoNLLPart part = document.getParts().get(i);

				ArrayList<EntityMention> goldZerosArr = RuleZeroCoref.getAnaphorZeros(part.getChains());
				HashSet<EntityMention> goldZeros = new HashSet<EntityMention>(goldZerosArr);
				goldZeroses.add(goldZerosArr);
				
				det.setPart(part);
				
				ArrayList<String> tokenized = Common.getLines("conll-source/" + name + "_" + part.getPartID()
						+ ".token");
				
				det.setTokenized(tokenized);
				det.setGoldZeros(goldZeros);
				
				ArrayList<EntityMention> herusiticZeros = det.getHeuristicZeros();

				systemZeroses.add(herusiticZeros);

				gold += goldZeros.size();
				heru += herusiticZeros.size();
//				System.out.println(herusiticZeros.size() + "#" + goldZeros.size());

				for (EntityMention zero : herusiticZeros) {
					StringBuilder sb = new StringBuilder();
					if (goldZeros.contains(zero)) {
						sb.append("+1 ");
					} else {
						sb.append("-1 ");
					}
					sb.append(det.getZeroDetectFea(zero));
					lines.add(sb.toString());

				}
			}
		}
		det.detectFea.freeze();
		System.out.println(gold / heru);
		Common.getRPF(goldZeroses, systemZeroses);
		return lines;
	}
}
