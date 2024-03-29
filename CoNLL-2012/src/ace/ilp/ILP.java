package ace.ilp;

/* demo.java */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import model.EntityMention;
import util.Common;
import ace.ACECommon;
import ace.model.EventMention;

public class ILP {

	ArrayList<EventMention> mentions;
	HashMap<String, Double> confMap;

	HashSet<String> negativeConstraint;

	int s = 0;

	HashMap<EventMention[], Integer> corefOutput = new HashMap<EventMention[], Integer>();

	public ILP(ArrayList<EventMention> mentions, HashMap<String, Double> confMap, HashSet<String> negativeConstraint) {
		// read trigger type probabilities
		this.mentions = mentions;
		this.confMap = confMap;
		this.s = this.mentions.size();
		System.err.println("S:" + s);
		// read maxent coref probabilities
		this.negativeConstraint = negativeConstraint;
	}

	HashMap<String, Double> probMap = new HashMap<String, Double>();

	HashMap<String, Integer> nameMap = new HashMap<String, Integer>();

	public int execute() throws LpSolveException {
		LpSolve lp;
		int Ncol, m, ret = 0;

		/*
		 * We will build the model row by row So we start with creating a model
		 * with 0 rows and 2 columns
		 */
		Ncol = s * 34 + s * (s - 1) / 2; /* there are two variables in the model */
		if (Ncol == 0) {
			return 0;
		}
		/* create space large enough for one row */
		int[] colno = new int[Ncol * 2];
		double[] row = new double[Ncol * 2];

		lp = LpSolve.makeLp(0, Ncol);
		if (lp.getLp() == 0)
			ret = 1; /* couldn't construct a new model... */

		// set binary
		for (int i = 1; i < Ncol; i++) {
			lp.setBinary(i, true);
		}
		if (ret == 0) {
			/*
			 * let us name our variables. Not required, but can be usefull for
			 * debugging
			 */
			int vNo = 1;

			for (int i = 0; i < s; i++) {
				for (int k = 1; k <= 34; k++) {
					String name = "y(" + i + "," + k + ")";
					lp.setColName(vNo, name);
					nameMap.put(name, vNo);
					probMap.put(name, this.mentions.get(i).typeConfidences[k - 1]);
					vNo++;
				}
			}

			for (int j = 0; j < s; j++) {
				for (int i = 0; i < j; i++) {
					String name = "z(" + i + "," + j + ")";
					lp.setColName(vNo, name);
					nameMap.put(name, vNo);
					probMap.put(name, this.confMap.get(i + "_" + j));
					vNo++;
				}
			}

			lp.setAddRowmode(true);
		}
		// constraint 1: only one type & has type <=> trigger
		if (ret == 0) {
			/* construct xi=sum y(i, k) over all k */
			for (int i = 0; i < s; i++) {
				m = 0;
				int yi34 = nameMap.get("y(" + i + ",34)");
				colno[m] = yi34;
				row[m++] = 1;
				for (int k = 1; k <= 33; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					colno[m] = yik;
					row[m++] = 1;
				}
				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);
			}
		}

		// constraint 2: if coreference, then trigger
		if (ret == 0) {
			/* construct z(i, j)<=xi */
			for (int i = 0; i < s; i++) {
				int yi34 = nameMap.get("y(" + i + ",34)");
				for (int j = i + 1; j < s; j++) {
					m = 0;
					colno[m] = yi34;
					row[m++] = 1;
					int zij = nameMap.get("z(" + i + "," + j + ")");
					colno[m] = zij;
					row[m++] = 1;
					/* add the row to lp_solve */
					lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
				}
			}
		}

		// constraint 3: if coreference, then trigger
		if (ret == 0) {
			/* construct z(i, j)<=xj */
			for (int j = 0; j < s; j++) {
				int yj34 = nameMap.get("y(" + j + ",34)");
				for (int i = 0; i < j; i++) {
					m = 0;
					colno[m] = yj34;
					row[m++] = 1;
					int zij = nameMap.get("z(" + i + "," + j + ")");
					colno[m] = zij;
					row[m++] = 1;
					/* add the row to lp_solve */
					lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
				}
			}
		}

		// constraint 4: if coreference, then type equal
		if (ret == 0) {
			/* construct 1 - zij >= yik - yjk, for all k */
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = 1; k <= 34; k++) {
						int yik = nameMap.get("y(" + i + "," + k + ")");
						int yjk = nameMap.get("y(" + j + "," + k + ")");

						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = yik;
						row[m++] = 1;

						colno[m] = yjk;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 5: if coreference, then type equal
		if (ret == 0) {
			/* construct 1 - zij >= yjk - yik, for all k */
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = 1; k <= 34; k++) {
						int yik = nameMap.get("y(" + i + "," + k + ")");
						int yjk = nameMap.get("y(" + j + "," + k + ")");

						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = yjk;
						row[m++] = 1;

						colno[m] = yik;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

//		// constraint 6: if transitive constraint
		if (ret == 0) {
			/* construct z(i,j)+z(j,k)-z(i,k)<=1 */
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < s; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = 1;

						colno[m] = zik;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 7: if transitive constraint
		if (ret == 0) {
			/* construct z(i,j)+z(i,k)-z(j,k)<=1 */
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < s; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = zik;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 8: if transitive constraint
		if (ret == 0) {
			/* construct z(i,k)+z(j,k)-z(i,j)<=1 */
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < s; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zik;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = 1;

						colno[m] = zij;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 8: best first constraint
		// if (ret == 0) {
		// /* construct z(i,k)+z(j,k)-z(i,j)<=1 */
		// for (int j = 0; j < s; j++) {
		// m = 0;
		// for (int i = j - 1; i >= 0; i--) {
		// int zij = nameMap.get("z(" + i + "," + j + ")");
		//
		// colno[m] = zij;
		// row[m++] = 1;
		//
		// }
		// /* add the row to lp_solve */
		// lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
		// }
		// }

		// constraint 9: negative constraint
		System.err.println("Neg:" + this.negativeConstraint.size());
		if (ret == 0) {
			for (int j = 0; j < s; j++) {
				for (int i = j - 1; i >= 0; i--) {
					m = 0;
					EntityMention m1 = this.mentions.get(i);
					EntityMention m2 = this.mentions.get(j);
					String pair = m1.headCharStart + "," + m1.headCharEnd + "," + m2.headCharStart + ","
							+ m2.headCharEnd;
					if (this.negativeConstraint.contains(pair)) {
						int zij = nameMap.get("z(" + i + "," + j + ")");
						colno[m] = zij;
						row[m++] = 1;
						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
					}
				}
			}
		}

		HashMap<Integer, Double> obj = new HashMap<Integer, Double>();
		if (ret == 0) {
			/* set the objective function */
			m = 0;
			for (int i = 0; i < s; i++) {
				for (int j = i + 1; j < s; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");
					double pij = (probMap.get("z(" + i + "," + j + ")") * 2.0) - 1;

					double cij = -1.0 * Math.log(pij);
					double cij2 = -1.0 * Math.log(1 - pij);
					colno[m] = zij;
					row[m++] = pij * lemda/s;

					obj.put(zij, pij * lemda/s);
				}
			}
			for (int i = 0; i < s; i++) {
				double v = 0;
				for (int k = 1; k <= 34; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					double pik = probMap.get("y(" + i + "," + k + ")");
					v += pik;
					colno[m] = yik;
					row[m++] = pik * (1 - lemda);

//					obj.put(yik, pik * (1 - lemda));
				}
				// System.err.println("V:" + v);
			}
			/* set the objective in lp_solve */
			lp.setObjFnex(m, row, colno);
		}

		if (ret == 0) {
			lp.setAddRowmode(false); /*
									 * rowmode should be turned off again when
									 * done building the model
									 */
			/* set the object direction to maximize */
			lp.setMaxim();
			// lp.setMinim();
			/*
			 * just out of curioucity, now generate the model in lp format in
			 * file model.lp
			 */
			lp.writeLp("model.lp");
			// lp.writeMps("model.mps");

			/* I only want to see importand messages on screen while solving */
			lp.setVerbose(LpSolve.IMPORTANT);

			/* Now let lp_solve calculate a solution */
			ret = lp.solve();
			if (ret == LpSolve.OPTIMAL)
				ret = 0;
			else
				ret = 5;
		}
		System.err.println("Return: " + ret);
		if (ret == 0) {
			/* a solution is calculated, now lets get some results */
			/* objective value */
			System.err.println("Objective value: " + lp.getObjective());

			/* variable values */
			lp.getVariables(row);

			double sum = 0;
			for (Integer key : obj.keySet()) {
				double time = obj.get(key);
				double term = time * row[key.intValue() - 1];
				sum += term;
			}
			System.err.println("left:\t" + sum);
			System.err.println("right:\t" + (lp.getObjective() - sum));
			for (m = 0; m < Ncol; m++) {
				System.out.println(lp.getColName(m + 1) + ": " + row[m]);

				String name = lp.getColName(m + 1);
				int a = name.indexOf("(");
				int b = name.indexOf(")");
				String content = name.substring(a + 1, b);
				double value = row[m];
				if (name.startsWith("x")) {
					int idx = Integer.valueOf(content);
					if (value == 0) {
						mentions.get(idx).confidence = -1;
					} else {
						mentions.get(idx).confidence = 1;
					}
				} else if (name.startsWith("y")) {
					String tokens[] = content.split(",");
					int idx = Integer.valueOf(tokens[0]);
					String subType = ACECommon.subTypes.get(Integer.parseInt(tokens[1]) - 1);
					if (value == 1) {
						mentions.get(idx).subType = subType;
					}
				} else if (name.startsWith("z")) {
					String tokens[] = content.split(",");
					EventMention m1 = mentions.get(Integer.parseInt(tokens[0]));
					EventMention m2 = mentions.get(Integer.parseInt(tokens[1]));
					EventMention pair[] = new EventMention[2];
					pair[0] = m1;
					pair[1] = m2;
					if (value == 1 && !m1.subType.equals("null") && !m2.subType.equals("null")) {
						this.corefOutput.put(pair, 1);
						if (!m1.subType.equals(m2.subType) || m1.confidence < 0 || m2.confidence < 0) {
							System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
						}
					} else {
						this.corefOutput.put(pair, -1);
					}
				}
			}
			for (int i = 0; i < s; i++) {
				EventMention mention = mentions.get(i);
				if (mention.confidence > 0 && mention.subType.equals("null")) {
					System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				} else if (mention.confidence < 0 && !mention.subType.equals("null")) {
					System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				}
			}
			/* we are done now */

		}

		/* clean up such that all used memory by lp_solve is freeed */
		if (lp.getLp() != 0)
			lp.deleteLp();
		return (ret);
	}

	public void printResult(LpSolve lp) {

	}

	static double lemda = 0.5;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("java ~ folder lemda");
			System.exit(1);
		}
		lemda = Double.parseDouble(args[1]);
		EventMention.ace = true;
		Common.part = args[0];
		// ILPUtil.loadTriggerProb();

		ILPUtil.loadSVMResult();

		int size = 0;
		try {
			String baseFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" + Common.part + "/";
			ArrayList<String> allLines2 = Common.getLines(baseFolder + "/all.txt2");
			for (int k = 0; k < allLines2.size(); k++) {
				System.err.println(k);
				String file = allLines2.get(k);
				System.err.println(baseFolder + k);
				ArrayList<EventMention> mentions = new ArrayList<EventMention>();
				if (ILPUtil.systemEMses.containsKey(file)) {
					mentions.addAll(ILPUtil.systemEMses.get(file).values());
				}
				size += mentions.size();
				Collections.sort(mentions);
				// HashMap<String, Double> confMap =
				// ILPUtil.loadCorefProb(mentions, file);
				HashMap<String, Double> confMap = ILPUtil.loadCorefSVMProb(mentions, file);

				HashSet<String> negativeConstraint = ILPUtil.loadNegativeConstrain(mentions, file);

				ILP ilp = new ILP(mentions, confMap, negativeConstraint);
				ilp.execute();

				HashMap<EventMention[], Integer> corefOutput = ilp.corefOutput;
				ArrayList<String> ilpPred = new ArrayList<String>();
				ArrayList<String> ilpExtend = new ArrayList<String>();
				for (EventMention[] pair : corefOutput.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append(pair[0].headCharStart).append(",").append(pair[0].headCharEnd).append(",").append(
							pair[1].headCharStart).append(",").append(pair[1].headCharEnd);
					ilpExtend.add(sb.toString());
					ilpPred.add(Integer.toString(corefOutput.get(pair)));
				}
				Common.outputLines(ilpPred, baseFolder + k + ".ilppred");
				Common.outputLines(ilpExtend, baseFolder + k + ".ilpextent");
				// break;
			}
			ACECommon.outputResult(ILPUtil.systemEMses, "/users/yzcchen/workspace/NAACL2013-B/src/joint_ilp/result"
					+ Common.part);

		} catch (LpSolveException e) {
			e.printStackTrace();
		}
	}
}
