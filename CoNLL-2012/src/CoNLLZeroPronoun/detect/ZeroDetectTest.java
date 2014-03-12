package CoNLLZeroPronoun.detect;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;

import CoNLLZeroPronoun.coref.RuleZeroCoref;

import jnisvmlight.SVMLightModel;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;

public class ZeroDetectTest {

	String folder;
	SVMLightModel model;

	@SuppressWarnings("deprecation")
	public ZeroDetectTest(String folder) {
		this.folder = folder;
		folder = "all";
		det = new ZeroDetect(false, folder);
		try {
			this.model = SVMLightModel.readSVMLightModelFromURL(new java.io.File(
					"/users/yzcchen/tool/JNI_SVM-light-6.01/src/svmlight-6.01/zeroModel." + folder).toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	ArrayList<ArrayList<EntityMention>> goldZeroses = new ArrayList<ArrayList<EntityMention>>();
	ArrayList<ArrayList<EntityMention>> systemZeroses = new ArrayList<ArrayList<EntityMention>>();
	ZeroDetect det;

	public void test() {
		String folder = this.folder;
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_development/");

		for (String file : files) {
			System.out.println(file);
			ArrayList<ArrayList<EntityMention>>[] ret = testFile(file);

			systemZeroses.addAll(ret[1]);
			goldZeroses.addAll(ret[0]);
		}
		Common.getRPF(goldZeroses, systemZeroses);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<EntityMention>>[] testFile(String file) {
		CoNLLDocument document = new CoNLLDocument(file
				.replace("auto_conll", "gold_conll")
				);

		OntoCorefXMLReader.addGoldZeroPronouns(document, false);
		
		ArrayList<ArrayList<EntityMention>> predictZeroses = new ArrayList<ArrayList<EntityMention>>();
		ArrayList<ArrayList<EntityMention>> goldZeroses = new ArrayList<ArrayList<EntityMention>>();
		
		ArrayList<ArrayList<EntityMention>>[] ret = new ArrayList[2];
		int a = file.lastIndexOf("/");
		String name = file.substring(a + 1);
		for (int i = 0; i < document.getParts().size(); i++) {
			CoNLLPart part = document.getParts().get(i);
			ArrayList<EntityMention> goldZerosArr = RuleZeroCoref.getAnaphorZeros(part.getChains());
			HashSet<EntityMention> goldZeros = new HashSet<EntityMention>(goldZerosArr);
			goldZeroses.add(goldZerosArr);
			ArrayList<EntityMention> systemPredictZeros = detectZeros(part, goldZeros);
			predictZeroses.add(systemPredictZeros);
		}
		ret[0] = goldZeroses;
		ret[1] = predictZeroses;
		return ret;
	}

	public ArrayList<EntityMention> detectZeros(CoNLLPart part,
			HashSet<EntityMention> goldZeros) {
		det.setPart(part);
		det.setGoldZeros(goldZeros);
		ArrayList<EntityMention> herusiticZeros = det.getHeuristicZeros();

		ArrayList<EntityMention> systemPredictZeros = new ArrayList<EntityMention>();

		for (EntityMention zero : herusiticZeros) {
			StringBuilder sb = new StringBuilder();
			if (goldZeros!=null && goldZeros.contains(zero)) {
				sb.append("+1 ");
			} else {
				sb.append("-1 ");
			}
			sb.append(det.getZeroDetectFea(zero));

			
			double d = model.classify(Common.SVMStringToFeature(sb.toString()));
			System.out.println(d + ":\t" + Common.SVMStringToFeature(sb.toString()));
			if (d > -000.5) {
				systemPredictZeros.add(zero);
			}
		}
		System.out.println(systemPredictZeros.size());
		return systemPredictZeros;
	}
}
