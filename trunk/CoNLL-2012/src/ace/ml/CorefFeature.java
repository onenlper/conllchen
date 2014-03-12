package ace.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;

public abstract class CorefFeature {
	public HashMap<String, Integer> stringFea1;

	public HashMap<String, int[]> stringFeaCount;

	public HashMap<String, int[]> commonBV;

	public HashMap<String, int[]> commonPair;

	public void init(boolean train, String name) {
		stringFeaCount = Common.readFile2Map3(name + "_stringFeaCount");
		if (train) {

			commonBV = new HashMap<String, int[]>();
			commonPair = new HashMap<String, int[]>();

			stringFea1 = new HashMap<String, Integer>();
		} else {
			stringFea1 = Common.readFile2Map(name + "_stringFea1");
		}
	}

	protected int getNERelationFeaIdx(String relation) {
		return stringFea1.get(relation.toLowerCase());
	}

	public ArrayList<CoNLLSentence> getSentences() {
		return sentences;
	}

	public void setSentences(ArrayList<CoNLLSentence> sentences) {
		this.sentences = sentences;
	}

	protected ArrayList<CoNLLSentence> sentences;

	protected ACEML ml;

	protected CoNLLPart part;

	public CorefFeature(ACEML ml) {
		this.ml = ml;
	}

	public void setPart(CoNLLPart part) {
		this.part = part;
	}

	public abstract List<Feature> getLoneFeature(boolean train, EntityMention mention);

	public abstract List<Feature> getBilateralFea(boolean train, EntityMention[] pair);

	boolean link = false;

	boolean entityCoref = false;
	boolean eventCoref = false;

	public ArrayList<Feature> sievesFeature(boolean train, EntityMention[] pair) {
		EntityMention ant = pair[1];
		EntityMention em = pair[0];
		link = false;
		ArrayList<Feature> features = new ArrayList<Feature>();
		// same head sieve
		this.addSameHeadSieve(features, ant, em);
		// Discourse sieve
		this.addDiscourseSieve(features, ant, em);
		// ExactMatchSieve
		this.addExactMatchSieve(features, ant, em);
		// PreciseConstructSieve
		this.addPreciseConstructSieve(features, ant, em);
		// StrictHeadMatchSieve1
		this.addStrictHeadMatchSieve1(features, ant, em);
		// StrictHeadMatchSieve2
		this.addStrictHeadMatchSieve2(features, ant, em);
		// StrictHeadMatchSieve3
		this.addStrictHeadMatchSieve3(features, ant, em);
		// StrictHeadMatchSieve4
		this.addStrictHeadMatchSieve4(features, ant, em);

		this.addRelaxHeadMatchSieve(features, ant, em);
		// pronoun sieve
		this.addPronounSieve(features, ant, em);

		if (entityCoref) {
			boolean coref = (ant.entity == em.entity);
			int quant = this.getQuant(ant.head, em.head, "#1", train, coref);
			if (quant >= 2) {
				features.add(new Feature(0, 4));
			} else if (quant <= 1) {
				features.add(new Feature(1, 4));
			} else {
				features.add(new Feature(-1, 4));
			}
			// features.add(new Feature(this.getQuant(ant.head, em.head, "#1",
			// train, coref), 4));
			// features.add(new Feature(this.getQuant(ant.semClass, em.semClass,
			// "#2", train, coref), 4));
			// features.add(new Feature(this.getQuant(ant.subType, em.subType,
			// "#3", train, coref), 4));

			if (ant.argument != null && em.argument != null) {
				if (ant.argument.role.equals(em.argument.role)) {
					features.add(new Feature(0, 4));
				} else {
					features.add(new Feature(1, 4));
				}
			} else {
				features.add(new Feature(2, 4));
			}

			// if (ant.argument != null && em.argument != null) {
			// String roleEqual =
			// Boolean.toString(ant.argument.role.equals(em.argument.role));
			// features.add(new Feature(this.getQuant(roleEqual, "", "#4",
			// train, coref), 4));
			// features.add(new Feature(this.getQuant(ant.argument.role,
			// em.argument.role, "#5", train, coref), 4));
			// } else {
			// features.add(new Feature(-1, 4));
			// features.add(new Feature(-1, 4));
			// }
			// if (ant.argument != null) {
			// features.add(new Feature(this.getQuant(ant.argument.role,
			// em.head, "#6", train, coref), 4));
			// } else {
			// features.add(new Feature(-1, 4));
			// }
			// if (em.argument != null) {
			// features.add(new Feature(this.getQuant(em.argument.role,
			// ant.head, "#6", train, coref), 4));
			// } else {
			// features.add(new Feature(-1, 4));
			// }
		} else {

			if (!em.head.equals(ant.head)) {
				String bv = "";
				loop: for (int i = 0; i < em.head.length(); i++) {
					for (int j = 0; j < ant.head.length(); j++) {
						if (em.head.charAt(i) == ant.head.charAt(j)) {
							bv = Character.toString(em.head.charAt(i));
							break loop;
						}
					}
				}
				boolean coref = (em.goldChainID==ant.goldChainID);
				String str = em.head.compareTo(ant.head) > 0 ? (em.head + "#" + ant.head) : (ant.head + "#" + em.head);
				if (!bv.isEmpty()) {
					int[] a = commonBV.get(bv);
					if (a == null) {
						a = new int[2];
						commonBV.put(bv, a);
					}
					if(coref) {
						a[0]++;
					} else {
						a[1]++;
					}
				}
				int[] b = commonPair.get(str);
				if(b==null) {
					b = new int[2];
					commonPair.put(str, b);
				}
				if(coref) {
					b[0]++;
				} else {
					b[1]++;
				}
			}

			// features.add(new Feature(-1, 4));
			// features.add(new Feature(-1, 4));
			// features.add(new Feature(-1, 4));
			// features.add(new Feature(-1, 4));
			// features.add(new Feature(-1, 4));
			features.add(new Feature(-1, 4));
			features.add(new Feature(-1, 4));
		}
		// else if (eventCoref) {
		// EventMention an = (EventMention) ant;
		// EventMention e = (EventMention) em;
		// boolean coref = (an.eventChain == e.eventChain);
		// features.add(new Feature(this.getStringFea(ant.head, em.head, "##1",
		// train, coref), -1));
		// features.add(new Feature(this.getStringFea(ant.getType(),
		// em.getType(), "##2", train, coref), -1));
		// features.add(new Feature(this.getStringFea(ant.getSubType(),
		// em.getSubType(), "##3", train, coref), -1));
		// }

		return features;
	}

	private int getQuant(String str1, String str2, String group, boolean train, boolean coref) {
		String str = str1.compareTo(str2) > 0 ? (str1 + group + str2) : (str2 + group + str1);
		str = str.replace("\n", "").replaceAll("\\s+", "").replace("\r", "").toLowerCase();
		int ret = -1;
		if (this.stringFeaCount.containsKey(str)) {
			int count[] = this.stringFeaCount.get(str);
			ret = (int) Math.ceil((count[0] * 1.0) / ((count[0] + count[1]) * 1.0) * 3.0);
		}
		if (ret != -1 && ret != 0) {
			// System.err.println(str + ":" + ret);
		}
		return ret;
	}

	private int getStringFea(String str1, String str2, String group, boolean train, boolean coref) {
		String str = str1.compareTo(str2) > 0 ? (str1 + group + str2) : (str2 + group + str1);
		str = str.replace("\n", "").replaceAll("\\s+", "").replace("\r", "").toLowerCase();
		int ret = -1;
		if (this.stringFea1.containsKey(str)) {
			ret = this.stringFea1.get(str);
		} else {
			if (train) {
				int k = this.stringFea1.size();
				this.stringFea1.put(str, k);
				ret = k;
			}
		}
		return ret;
	}

	// private int getStringFea(String str1, String str2, String group, boolean
	// train, boolean coref) {
	// String str = str1.compareTo(str2) > 0 ? (str1 + group + str2) : (str2 +
	// group + str1);
	// str = str.replace("\n", "").replaceAll("\\s+", "").replace("\r",
	// "").toLowerCase();
	// int ret = -1;
	// if (this.stringFea1.containsKey(str)) {
	// ret = this.stringFea1.get(str);
	// } else {
	// if (train) {
	// int k = this.stringFea1.size();
	// this.stringFea1.put(str, k);
	// ret = k;
	// }
	// }
	// if(train) {
	// int count[] = this.stringFeaCount.get(str);
	// if(count==null) {
	// count = new int[2];
	// this.stringFeaCount.put(str, count);
	// }
	// if(coref) {
	// count[0]++;
	// } else {
	// count[1]++;
	// }
	// }
	// return ret;
	// }

	private void addRelaxHeadMatchSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}
		if (ant.isPronoun || em.isPronoun) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.relaxHeadMatchRule(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addPronounSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}

		if (!link && entityCoref && EntityFeatures.pronounRule(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve4(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.strictHeadMatchRule4(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve3(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.strictHeadMatchRule3(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve2(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.strictHeadMatchRule2(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addStrictHeadMatchSieve1(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.strictHeadMatchRule1(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addPreciseConstructSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			return;
		}

		// copular
		if (!link && entityCoref && EntityFeatures.preciseRule1(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// appositive
		if (!link && entityCoref && EntityFeatures.preciseRule2(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// +++++萨达姆·侯赛因 ANIMATE SINGULAR MALE PERSON 15:227,227 #萨达姆
		// 迈利萨尼迪斯 (ANIMATE SINGULAR UNKNOWN PERSON 374) [20:10,10 374 374] -
		// antecedent: 洛·迈利萨尼迪斯
		if (!link && entityCoref && EntityFeatures.preciseRule3(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// 李登辉总统 李登辉
		if (!link && entityCoref && EntityFeatures.preciseRule4(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		// 多国 INANIMATE PLURAL UNKNOWN NORP 13:635,635 #多明尼加
		if (!link && entityCoref && EntityFeatures.preciseRule5(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}

		if (!link && entityCoref && EntityFeatures.preciseRule6(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addExactMatchSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}
		if (!link && entityCoref && EntityFeatures.exactMatchRule(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addSameHeadSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			return;
		}

		if (!link && entityCoref && EntityFeatures.sameHead(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}

	private void addDiscourseSieve(ArrayList<Feature> features, EntityMention ant, EntityMention em) {
		if (!entityCoref) {
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			return;
		}

		// (I - I) in the same speaker's quotation.
		if (!link && entityCoref && EntityFeatures.discourseRule1(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// (speaker - I)
		if (!link && entityCoref && EntityFeatures.discourseRule2(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// previous I - you or previous you - I in two person conversation
		if (!link && entityCoref && EntityFeatures.discourseRule3(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
		// You - You
		if (!link && entityCoref && EntityFeatures.discourseRule4(ant, em, part)) {
			features.add(new Feature(0, 2));
			// link = true;
		} else {
			features.add(new Feature(1, 2));
		}
	}
}
