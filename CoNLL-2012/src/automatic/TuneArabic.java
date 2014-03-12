package automatic;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

import util.Common;

import coref.OntoAltafToSemEval;

public class TuneArabic {
	public static void main(String args[]) throws Exception {
		String language = "arabic";
		String folder = args[1];
		String optFn = language + "_" + folder + "_opt";
		fw = new FileWriter(language + "_" + folder + "_tuning_final");
		ArrayList<Double> t5s = new ArrayList<Double>();
		for (double a = -0.05; a <=0.2;) {
			t5s.add(a);
			a += 0.05;
		}
		double t1s[] = { -0.1, 0, 0.15};
		double t2s[] = { -0.1, 0, 0.15};
		double t3s[] = { 1.1, 1.0, 0.85};
		double t4s[] = { 1.1, 1.0, 0.95, 0.85, 0.75, 0.65, 0.55, 0.45, 0.35, 0.25, 0.15, 0.12, 0.10, 0.08, 0.05 };

		double maxfscore = 0;
		boolean first = true;
		double baseline = 0;
		
		double bestT1 = t1s[0];
		double bestT2 = t2s[0];
		double bestT3 = t3s[0];
		double bestT4 = t4s[0];
		double bestT5 = t5s.get(0);
		
		String a[] = new String[7];
		a[0] = "development";
		a[1] = folder;
		sur = "1";
		String a2[] = new String[2];
		a2[0] = "/users/yzcchen/chen3/conll12/arabic/" + folder + "_development" + sur + "/";
		a2[1] = "system";
		int iteration = 0;
		while(true) {
			iteration++;
			double subMaxfScore = 0;
			for(double t1 : t1s) {
				a[2] = Double.toString(t1);
				a[3] = Double.toString(bestT2);
				a[4] = Double.toString(bestT3);
				a[5] = Double.toString(bestT4);
				a[6] = Double.toString(bestT5);
				runSieve(a);
				double fscore = evaluate(language, folder);
				if (fscore > subMaxfScore) {
					bestT1 = t1;
					subMaxfScore = fscore;
				}
				if(fscore > maxfscore) {
					maxfscore = fscore;
					updateOpt(a, optFn, fscore);
				}
				if (first) {
					baseline = fscore;
					first = false;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("\n======\n");
				fw.write("baseline: " + baseline + "\n"); 
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n"); 
			}
			
			for(double t2 : t2s) {
				a[2] = Double.toString(bestT1);
				a[3] = Double.toString(t2);
				a[4] = Double.toString(bestT3);
				a[5] = Double.toString(bestT4);
				a[6] = Double.toString(bestT5);
				runSieve(a);
				OntoAltafToSemEval.runOutputKey(a2);
				double fscore = evaluate(language, folder);
				if (fscore > subMaxfScore) {
					bestT2 = t2;
					subMaxfScore = fscore;
				}
				if(fscore > maxfscore) {
					updateOpt(a, optFn, fscore);
					maxfscore = fscore;
				}
				if (first) {
					baseline = fscore;
					first = false;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("\n======\n");
				fw.write("baseline: " + baseline + "\n"); 
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n"); 
			}
			
			for(double t3 : t3s) {
				a[2] = Double.toString(bestT1);
				a[3] = Double.toString(bestT2);
				a[4] = Double.toString(t3);
				a[5] = Double.toString(bestT4);
				a[6] = Double.toString(bestT5);
				runSieve(a);
				OntoAltafToSemEval.runOutputKey(a2);
				double fscore = evaluate(language, folder);
				if (fscore > subMaxfScore) {
					bestT3 = t3;
					subMaxfScore = fscore;
				}
				if(fscore > maxfscore) {
					maxfscore = fscore;
					updateOpt(a, optFn, fscore);
				}
				if (first) {
					baseline = fscore;
					first = false;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("\n======\n");
				fw.write("baseline: " + baseline + "\n"); 
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n"); 
			}
			
			for(double t4 : t4s) {
				a[2] = Double.toString(bestT1);
				a[3] = Double.toString(bestT2);
				a[4] = Double.toString(bestT3);
				a[5] = Double.toString(t4);
				a[6] = Double.toString(bestT5);
				runSieve(a);
				OntoAltafToSemEval.runOutputKey(a2);
				double fscore = evaluate(language, folder);
				if (fscore > subMaxfScore) {
					bestT4 = t4;
					subMaxfScore = fscore;
				}
				if(fscore > maxfscore) {
					maxfscore = fscore;
					updateOpt(a, optFn, fscore);
				}
				if (first) {
					baseline = fscore;
					first = false;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("\n======\n");
				fw.write("baseline: " + baseline + "\n"); 
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n");
				fw.flush();
			}
			double fscore = 0;
			for(double t5 : t5s) {
				a[2] = Double.toString(bestT1);
				a[3] = Double.toString(bestT2);
				a[4] = Double.toString(bestT3);
				a[5] = Double.toString(bestT4);
				a[6] = Double.toString(t5);
				runSieve(a);
				OntoAltafToSemEval.runOutputKey(a2);
				fscore = evaluate(language, folder);
				if (fscore > subMaxfScore) {
					bestT5 = t5;
					subMaxfScore = fscore;
				}
				if(fscore > maxfscore) {
					maxfscore = fscore;
					updateOpt(a, optFn, fscore);
				}
				if (first) {
					baseline = fscore;
					first = false;
				}
				System.out.println("======");
				System.out.println("baseline: " + baseline);
				System.out.println("nowscore: " + fscore);
				System.out.println("maxscore: " + maxfscore);
				fw.write("\n======\n");
				fw.write("baseline: " + baseline + "\n"); 
				fw.write("nowscore: " + fscore + "\n");
				fw.write("maxscore: " + maxfscore + "\n"); 
				fw.flush();
			}
			fw.write("iteration: " + iteration + "\n");
			fw.write("maxscore: " + maxfscore);
			fw.write("===================\n");
			StringBuilder sb = new StringBuilder();
			sb.append(bestT1).append(" ").append(bestT2).append(" ")
				.append(bestT3).append(" ").append(bestT4).append(" ").append(bestT5).append("\n");
			fw.write(sb.toString() + "\n");
			sb = new StringBuilder();
			fw.write(sb.toString() + "\n");
			fw.write("===================\n");
			fw.flush();
		}
	}

	static String sur;
	
	static FileWriter fw;
	
	public static void updateOpt(String args[], String filename, double maxScore) {
		ArrayList<String> outs = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for(String arg : args) {
			sb.append(arg).append(" ");
		}
		outs.add(sb.toString());
		outs.add(Double.toString(maxScore));
		Common.outputLines(outs, filename);
	}
	
	public static void runSieve(String args[]) throws Exception {
		StringBuilder sb = new StringBuilder();
		for(String arg : args) {
			sb.append(arg).append(" ");
		}
		System.out.println(sb.toString());
		fw.write(sb.toString() + "\n");
		ruleCoreference.arabic.RuleCoref.run(args, sur);
	}
	
	public static double evaluate(String language, String folder) throws Exception {
		String baseFolder = "/users/yzcchen/chen3/conll12/" + language + "/" + folder + "_development" + sur + "/key.";
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
		fw.write(" " + fTotal / 3);
		fw.flush();
		return fTotal / 3;
	}

	public static double getFScore(String result) {
//		 System.out.println(result); 
		int a = result.lastIndexOf(' ');
		return Double.valueOf(result.substring(a + 1, result.length() - 1));
	}
}
