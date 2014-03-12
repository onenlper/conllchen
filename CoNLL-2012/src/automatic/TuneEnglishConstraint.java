package automatic;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

import util.Common;

import coref.OntoAltafToSemEval;

public class TuneEnglishConstraint {
	public static void main(String args[]) throws Exception {
		// java ~ english nw
		String language = "english";
		String folder = args[1];
		fw = new FileWriter(language + "_" + folder + "_threshold2");
		double t1s = -1;
		double t2s = -1;
		double t3s = 2;
		double t4s = 2;
		double t5s = -1;
		double baseline = 0;
		String optFn = language + "_" + folder + "_opt2_only_constraint";
//		String thFile = "english_" + folder + "_optTh";
//		String thLine[] = Common.getLines(thFile).get(0).split("\\s+");
//		t1s = Double.valueOf(thLine[0]);
//		t2s = Double.valueOf(thLine[1]);
//		t3s = Double.valueOf(thLine[2]);
//		t4s = Double.valueOf(thLine[3]);
//		t5s = Double.valueOf(thLine[4]);

		ArrayList<Boolean> bestBs = new ArrayList<Boolean>();
		for (int i = 0; i < 36; i++) {
			bestBs.add(new Boolean(true));
		}
		double maxfscore = 0;
		boolean first = true;
		for (int h = 0; h < 36; h++) {
			int bestChange = -1;
			double localBest = -1;
			for (int i = 0; i < 36; i++) {
				System.out.println(h + ":" + i);
				if(!bestBs.get(i)) {
					continue;
				}
				ArrayList<Boolean> copyBs = new ArrayList<Boolean>();
				copyBs.addAll(bestBs);
				if(!first) {
					copyBs.set(i, false);
				}
				String a[] = new String[7];
				a[0] = "development";
				a[1] = folder;
				a[2] = Double.toString(t1s);
				a[3] = Double.toString(t2s);
				a[4] = Double.toString(t3s);
				a[5] = Double.toString(t4s);
				a[6] = Double.toString(t5s);
				runSieve(a, copyBs, "4");
				double fscore = evaluate(language, folder, "4");
				if(first) {
					baseline = fscore;
					i--;
					first = false;
				}
				if(fscore>maxfscore) {
					maxfscore = fscore;
					updateOpt(a, bestBs, optFn, fscore);
				}
				if(fscore>localBest) {
					bestChange = i;
					localBest = fscore;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("======");
				fw.write("baseline: " + baseline + "\n");
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n");
				fw.flush();
			}
			bestBs.set(bestChange, false);
		}
	}
	
	public static void updateOpt(String args[], ArrayList<Boolean> bs2, String filename, double maxScore) {
		ArrayList<String> outs = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for(String arg : args) {
			sb.append(arg).append(" ");
		}
		outs.add(sb.toString());
		sb = new StringBuilder();
		for(Boolean b : bs2) {
			sb.append(b.toString()).append(" ");
		}
		outs.add(sb.toString());
		outs.add(Double.toString(maxScore));
		Common.outputLines(outs, filename);
	}

	static FileWriter fw;

	public static void runSieve(String args[], ArrayList<Boolean> bs2, String sur) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		System.out.println(sb.toString());
		fw.write(sb.toString() + "\n");
		sb = new StringBuilder();
		for(Boolean b : bs2) {
			sb.append(b.toString()).append(" ");
		}
		fw.write(sb.toString() + "\n");
		System.out.println(sb.toString());
		ruleCoreference.english.RuleCoref.run(args, bs2, sur);
	}

	public static double evaluate(String language, String folder, String folderSur) throws Exception {
		String baseFolder = "/users/yzcchen/chen3/conll12/" + language + "/" + folder + "_development" + folderSur
				+ "/key.";
		String metrixes[] = { "MUC", "BCUB", "CEAFE" };
		double fTotal = 0;
		for (String metrix : metrixes) {
			String result = "";
			String cmd3 = "perl scorer.pl " + metrix + " " + baseFolder + "gold" + " " + baseFolder + "system";
			File runFolder = new File("/users/yzcchen/tool/Scorer/");
			Runtime rt = Runtime.getRuntime();
			Process p3 = rt.exec(cmd3, null, runFolder);
			BufferedInputStream in = new BufferedInputStream(p3.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			BufferedInputStream err = new BufferedInputStream(p3.getErrorStream());
			BufferedReader errBr = new BufferedReader(new InputStreamReader(err));
			String line;
			String lineErr;
			while ((line = inBr.readLine()) != null) {
				result = line;
				// System.out.println(line);
			}
			while ((lineErr = errBr.readLine()) != null) {
				// System.out.println(line);
			}
			inBr.close();
			errBr.close();
			if (p3.waitFor() != 0) {
				if (p3.exitValue() == 1) {
					System.out.println("Fail");
				} else {
					System.out.println("Success");
				}
			}
			double fscore = getFScore(result);
			fTotal += fscore;
			System.out.print(metrix + ":" + fscore + " ");
			fw.write(metrix + ":" + fscore + " ");
		}
		System.out.println(" " + fTotal / 3);
		fw.write(" " + fTotal / 3 + "\n");
		return fTotal / 3;
	}

	public static double getFScore(String result) {
		// System.out.println(result);
		int a = result.lastIndexOf(' ');
		return Double.valueOf(result.substring(a + 1, result.length() - 1));
	}
}
