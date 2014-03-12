package automatic;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

import coref.OntoAltafToSemEval;

public class TuneEnglishThreshold {
	public static void main(String args[]) throws Exception {
		String language = "english";
		String folder = args[1];
		fw = new FileWriter(language + "_" + folder + "_threshold");
		
		ArrayList<Double> t5s = new ArrayList<Double>();
		ArrayList<Boolean> bestBs = new ArrayList<Boolean>(); 
		for (int i = 0; i < 36; i++) {
			bestBs.add(new Boolean(true));
		}
		
		for (double a = -0.05; a < 0.15;) {
			t5s.add(a);
			a += 0.05;
		}
		double t1s[] = { -0.1, 0, 0.05, 0.15};
		double t2s[] = { -0.1, 0, 0.05, 0.15};
		double t3s[] = { 1.1, 1.0, 0.95, 0.85};
		double t4s[] = { 1.1, 1.0, 0.95, 0.85};

		double maxfscore = 0;
		double best[] = new double[5];
		boolean first = true;
		double baseline = 0;
		for (double t1 : t1s) {
			for (double t2 : t2s) {
				for (double t3 : t3s) {
					for (double t4 : t4s) {
						for (double t5 : t5s) {
							System.out.println("================================");
							fw.write("=======================\n");
							String a[] = new String[7];
							a[0] = "development";
							a[1] = folder;
							a[2] = Double.toString(t1);
							a[3] = Double.toString(t2);
							a[4] = Double.toString(t3);
							a[5] = Double.toString(t4);
							a[6] = Double.toString(t5);
							String a2[] = new String[2];
							a2[0] = "/users/yzcchen/chen3/conll12/" + language + "/" + folder + "_development1/";
							a2[1] = "sieve";
							runSieve(a, bestBs, "1");
							double fscore = evaluate(language, folder, "1");
							if (fscore > maxfscore) {
								maxfscore = fscore;
								best[0] = t1;
								best[1] = t2;
								best[2] = t3;
								best[3] = t4;
								best[4] = t5;
							}
							if (first) {
								baseline = fscore;
								first = false;
							}
							System.out.println("baseline: " + baseline);
							System.out.println("BEST: " + best[0] + " " + best[1] + " " + best[2] + " " + best[3] + " "
									+ best[4] + " " + maxfscore);
							fw.write("baseline: " + baseline + "\n");
							fw.write("BEST: " + best[0] + " " + best[1] + " " + best[2] + " " + best[3] + " "
									+ best[4] + " " + maxfscore + "\n");
							fw.flush();
						}
					}
				}
			}
		}
	}

	static FileWriter fw;

	public static void runSieve(String args[], ArrayList<Boolean> bs2, String sur) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		System.out.println(sb.toString());
		fw.write(sb.toString() + "\n");
		ruleCoreference.english.RuleCoref.run(args, bs2, sur);
	}

	public static double evaluate(String language, String folder, String folderSur) throws Exception {
		String baseFolder = "/users/yzcchen/chen3/conll12/" + language + "/" + folder + "_development" + folderSur + "/key.";
		String metrixes[] = { "MUC", "BCUB", "CEAFE" };
		double fTotal = 0;
		for (String metrix : metrixes) {
			String result = "";
			String cmd3 = "perl scorer.pl " + metrix + " " + baseFolder + "gold" + " " + baseFolder + "system";
//			System.out.println(cmd3);
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
//				 System.out.println(line);
			}
			while ((lineErr = errBr.readLine()) != null) {
//				 System.out.println(line);
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
