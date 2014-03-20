package CoNLLZeroPronoun.coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.YYFeature;
import util.Common.Feature;

public class ZeroCorefFea extends YYFeature {

	ArrayList<EntityMention> zeros;
	ArrayList<EntityMention> candidates;

	EntityMention cand;
	EntityMention zero;

	CoNLLPart part;

	HashMap<String, HashMap<String, Integer>> detailMapGram2;
	HashMap<String, HashMap<String, Integer>> detailMapGram3;

	HashMap<String, HashMap<String, Double>> detailMapGramDistri2;
	HashMap<String, HashMap<String, Double>> detailMapGramDistri3;

	public ZeroCorefFea(boolean train, String name) {
		super(train, name);
		// detailMapGram2 = readMap("gram2detail.allY.new");
		// detailMapGram3 = readMap("gram3detail.allY.new");
		//
		// detailMapGramDistri2 = calculateDistribution(detailMapGram2);
		// detailMapGramDistri3 = calculateDistribution(detailMapGram3);
	}

	private EntityMention currentZero = null;

	ArrayList<ArrayList<Double>> lists = new ArrayList<ArrayList<Double>>();

	private void insertList(double d, ArrayList<Double> list) {
		for (double t : list) {
			if (t == d) {
				return;
			}
		}
		list.add(d);
	}

	private int getRank(double d, ArrayList<Double> list) {
		for (int i = 0; i < list.size(); i++) {
			double t = list.get(i);
			if (t == d) {
				return i;
			}
		}
		Common.bangErrorPOS("Not find cand");
		return -1;
	}

	private void updateSemList() {
		lists.clear();
		for (int i = 0; i < 6; i++) {
			ArrayList<Double> list = new ArrayList<Double>();
			lists.add(list);
		}

		for (EntityMention can : this.candidates) {
			if (can.compareTo(this.zero) < 0) {
				double[] sems = getSemanticLayer(can);
				for (int i = 0; i < sems.length; i++) {
					double sem = sems[i];
					if (sem == 2 || sem == 3) {
						continue;
					}
					ArrayList<Double> list = lists.get(i);
					this.insertList(sem, list);
				}
			}
		}
		for (ArrayList<Double> list : lists) {
			Collections.sort(list);
			Collections.reverse(list);
		}
	}

	private ArrayList<Feature> getSemanticFeatures() {

		if (this.zero != this.currentZero) {
			updateSemList();
			this.currentZero = this.zero;
		}

		ArrayList<Feature> features = new ArrayList<Feature>();
		double[] sems = getSemanticLayer(this.cand);
		StringBuilder sb = new StringBuilder();
		int ranks[] = new int[6];
		for (int i = 0; i < sems.length; i++) {
			double sem = sems[i];
			ArrayList<Double> list = this.lists.get(i);
			int rank = -1;
			if (sem == 3) {
				features.add(new Feature(12, 1, 16));
			} else if (sem == 2) {
				features.add(new Feature(11, 1, 16));
			} else {
				rank = getRank(sem, list);
				if (rank > 4) {
					rank = 4;
				}
				features.add(new Feature(rank, 1, 16));
			}
			sb.append(sem).append("\t");
			ranks[i] = rank;
		}
		if ((ranks[0] == 4) && ranks[1] == 4 && ranks[2] == 4 && ranks[3] == 4 && ranks[4] == 4 && ranks[5] == 4) {
			this.skip = true;
		}
		semStr = ranks[0] + "_" + ranks[1] + "_" + ranks[2] + "_" + ranks[3] + "_" + ranks[4] + "_" + ranks[5];
		sb.append(this.cand.head);
		System.out.print(sb.toString());
		return features;
	}

	String semStr;

	private double[] getSemanticLayer(EntityMention cand) {
		double distrs[] = new double[6];
		String predicate = this.getPredicate2(this.zero.V);
		String objectNP = this.getObjectNP2(this.zero);
		HashMap<String, Double> map = null;
		if (objectNP.isEmpty()) {
			String key = "* " + predicate;
			map = detailMapGramDistri2.get(key);
		} else {
			String key = "* " + predicate + " " + objectNP;
			map = detailMapGramDistri3.get(key);
			if (map == null) {
				key = "* " + predicate;
				map = detailMapGramDistri2.get(key);
			}
		}

		String head = cand.head;
		// System.out.println(head + "#" + cand);

		if (Common.getSemantic(head) == null && cand.ner.equals("PERSON")) {
			head = "他";
		}

		if (Common.getSemantic(head) == null && (cand.ner.equals("TIME") || cand.ner.equals("DATE"))) {
			head = "那时";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("LAW")) {
			head = "法律";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("ORG")) {
			head = "公司";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("LOC")) {
			head = "那里";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("PRODUCT")) {
			head = "产品";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("WORK_OF_ART")) {
			head = "书";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("NORP")) {
			head = "人们";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("LANGUAGE")) {
			head = "语言";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("EVENT")) {
			head = "事件";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("FAC")) {
			head = "部门";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("GPE")) {
			head = "城市";
		}

		if (Common.getSemantic(head) == null && cand.ner.equals("OTHER")) {
			head = head.substring(head.length() - 1);
		}

		if (Common.getSemantic(head) != null) {
			String sem = Common.getSemantic(head)[0];

			String sems[] = Common.getSemantic(head);
			double max[] = new double[6];
			for (String tmp : sems) {
				String l1 = tmp.substring(0, 1);
				String l2 = tmp.substring(0, 2);
				String l3 = tmp.substring(0, 4);
				String l4 = tmp.substring(0, 5);
				String l5 = tmp.substring(0, 7);
				String l6 = head;

				distrs[0] = Common.getValue(map, l1);
				distrs[1] = Common.getValue(map, l2);
				distrs[2] = Common.getValue(map, l3);
				distrs[3] = Common.getValue(map, l4);
				distrs[4] = Common.getValue(map, l5);
				distrs[5] = Common.getValue(map, l6);

				if (max[0] < distrs[0]) {
					sem = tmp;
					max[0] = distrs[0];
				} else if (max[0] == distrs[0] && max[1] < distrs[1]) {
					sem = tmp;
					max[1] = distrs[1];
				} else if (max[1] == distrs[1] && max[2] < distrs[2]) {
					sem = tmp;
					max[2] = distrs[2];
				} else if (max[2] == distrs[2] && max[3] < distrs[3]) {
					sem = tmp;
					max[3] = distrs[3];
				} else if (max[3] == distrs[3] && max[4] < distrs[4]) {
					sem = tmp;
					max[4] = distrs[4];
				} else if (max[0] == distrs[0] && max[5] < distrs[5]) {
					sem = tmp;
					max[5] = distrs[5];
				}
			}

			String l1 = sem.substring(0, 1);
			String l2 = sem.substring(0, 2);
			String l3 = sem.substring(0, 4);
			String l4 = sem.substring(0, 5);
			String l5 = sem.substring(0, 7);
			String l6 = head;

			distrs[0] = Common.getValue(map, l1);
			distrs[1] = Common.getValue(map, l2);
			distrs[2] = Common.getValue(map, l3);
			distrs[3] = Common.getValue(map, l4);
			distrs[4] = Common.getValue(map, l5);
			distrs[5] = Common.getValue(map, l6);
		} else {
			distrs[0] = 0;
			distrs[1] = 0;
			distrs[2] = 0;
			distrs[3] = 0;
			distrs[4] = 0;
			distrs[5] = 0;
		}
		if (distrs[0] != 2 && distrs[0] != 3) {
			// System.out.println(distrs[0]);
		}
		return distrs;
	}

	protected HashMap<String, HashMap<String, Double>> calculateDistribution(
			HashMap<String, HashMap<String, Integer>> bigMap) {
		HashMap<String, HashMap<String, Double>> newMap = new HashMap<String, HashMap<String, Double>>();
		for (String key : bigMap.keySet()) {
			HashMap<String, Integer> map = bigMap.get(key);

			HashMap<String, Integer> countMap = new HashMap<String, Integer>();
			int overall = 0;
			for (String subKey : map.keySet()) {
				String fill = subKey.split("\\s+")[1];
				int val = map.get(subKey);
				String sem = Common.getSemantic(fill)[0];
				String l1 = sem.substring(0, 1);
				String l2 = sem.substring(0, 2);
				String l3 = sem.substring(0, 4);
				String l4 = sem.substring(0, 5);
				String l5 = sem.substring(0, 7);
				String l6 = fill;
				Common.addMap(countMap, l1, val);
				Common.addMap(countMap, l2, val);
				Common.addMap(countMap, l3, val);
				Common.addMap(countMap, l4, val);
				Common.addMap(countMap, l5, val);
				Common.addMap(countMap, l6, val);
				overall += val;
			}

			HashMap<String, Double> distriMap = new HashMap<String, Double>();
			for (String subKey : countMap.keySet()) {
				double distri = (countMap.get(subKey)) * 1.0 / (overall * 1.0);
				distriMap.put(subKey, distri);
			}
			newMap.put(key, distriMap);
		}

		return newMap;
	}

	private HashMap<String, HashMap<String, Integer>> readMap(String filename) {
		HashMap<String, HashMap<String, Integer>> map = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, Integer> lines = Common.readFile2Map(filename);
		for (String key : lines.keySet()) {
			int value = lines.get(key);
			int p = key.indexOf('\t');
			String mainKey = key.substring(0, p);
			String subKey = key.substring(p + 1);

			HashMap<String, Integer> subMap = map.get(mainKey);
			if (subMap == null) {
				subMap = new HashMap<String, Integer>();
				map.put(mainKey, subMap);
			}
			subMap.put(subKey, value);
		}
		return map;
	}

	public boolean skip = false;

	public static boolean group1 = true;
	public static boolean group2 = true;
	public static boolean group3 = true;

	@Override
	public ArrayList<Feature> getCategoryFeatures() {
		skip = false;

		ArrayList<Feature> feas = new ArrayList<Feature>();

		feas.addAll(this.getNPFeature());
		feas.addAll(this.getZeroAnaphorFeature());
		feas.addAll(this.getZeroFeature());

		feas.addAll(this.getCFeatures());

		// feas.addAll(this.getSemanticFeatures());
		// this.getSemanticFeatures();
		return feas;
	}

	public ArrayList<Feature> getCFeatures() {
		ArrayList<Feature> features = new ArrayList<Feature>();

		// if (cand.compareTo(zero) > 0) {
		// features.add(new Feature(0, 1, 2));
		// } else {
		// features.add(new Feature(1, 1, 2));
		// }
		// if (cand.end == -1) {
		// features.add(new Feature(0, 1, 2));
		// } else {
		// features.add(new Feature(1, 1, 2));
		// }
		// same VV
		CoNLLSentence sentence = part.getWord(zero.start).sentence;

		// end with ? or .
		String lastWord = sentence.words.get(sentence.words.size() - 1).word;
		String zeroSpeaker = part.getWord(zero.start).speaker;
		String candSpeaker = part.getWord(cand.start).speaker;

		if (group3) {
			if (lastWord.equals("?")) {
				if ((zeroSpeaker.equals(candSpeaker) && cand.source.equals("你"))
						|| (!zeroSpeaker.equals(candSpeaker) && cand.source.equals("我"))) {
					features.add(new Feature(0, 1, 4));
				} else if ((zeroSpeaker.equals(candSpeaker) && cand.source.equals("我"))
						|| (!zeroSpeaker.equals(candSpeaker) && cand.source.equals("你"))) {
					features.add(new Feature(1, 1, 4));
				} else {
					features.add(new Feature(2, 1, 4));
				}
			} else {
				features.add(new Feature(3, 1, 4));
			}

			if (lastWord.equals(".")) {
				if ((zeroSpeaker.equals(candSpeaker) && cand.source.equals("你"))
						|| (!zeroSpeaker.equals(candSpeaker) && cand.source.equals("我"))) {
					features.add(new Feature(0, 1, 4));
				} else if ((zeroSpeaker.equals(candSpeaker) && cand.source.equals("我"))
						|| (!zeroSpeaker.equals(candSpeaker) && cand.source.equals("你"))) {
					features.add(new Feature(1, 1, 4));
				} else {
					features.add(new Feature(2, 1, 4));
				}
			} else {
				features.add(new Feature(3, 1, 4));
			}
		}

		return features;
	}

	HashMap<String, Integer> chainMap;

	public String getPredicate(MyTreeNode vp) {
		ArrayList<MyTreeNode> leaves = vp.getLeaves();
		for (MyTreeNode leaf : leaves) {
			if (leaf.parent.value.startsWith("V")
			// && !leaf.value.equals("会") && !leaf.value.equals("独立")
			// && !leaf.value.equals("可以")
			) {
				return leaf.value;
			}
		}
		return "";
	}

	public MyTreeNode getPredicateNode(MyTreeNode vp) {
		ArrayList<MyTreeNode> leaves = vp.getLeaves();
		for (MyTreeNode leaf : leaves) {
			if (leaf.parent.value.startsWith("V")
			// && !leaf.value.equals("会") && !leaf.value.equals("独立")
			// && !leaf.value.equals("可以")
			) {
				return leaf;
			}
		}
		return null;
	}

	public String getObjectNP(EntityMention zero) {
		MyTreeNode vp = zero.V;
		ArrayList<MyTreeNode> leaves = vp.getLeaves();
		for (MyTreeNode leaf : leaves) {
			if (leaf.parent.value.startsWith("V")) {
				ArrayList<MyTreeNode> possibleNPs = leaf.parent.getRightSisters();
				for (MyTreeNode tmp : possibleNPs) {
					if (tmp.value.startsWith("NP") || tmp.value.startsWith("QP")) {
						return tmp.getLeaves().get(tmp.getLeaves().size() - 1).value;
					}
				}
			}
		}
		return "";
	}

	public String getObjectNP2(EntityMention zero) {
		MyTreeNode tmp = zero.V;
		while (true) {
			boolean haveVP = false;
			for (MyTreeNode child : tmp.children) {
				if (child.value.equalsIgnoreCase("VP")) {
					haveVP = true;
					tmp = child;
					break;
				}
			}
			if (!haveVP) {
				break;
			}
		}
		// System.out.println(tmp.children.get(0).value);

		ArrayList<MyTreeNode> possibleNPs = tmp.children;
		for (MyTreeNode tm : possibleNPs) {
			if (tm.value.startsWith("NP") || tm.value.startsWith("QP")) {
				return tm.getLeaves().get(tm.getLeaves().size() - 1).value;
			}
		}
		return "";
	}

	public String getPredicate2(MyTreeNode vp) {
		MyTreeNode tmp = vp;
		while (true) {
			boolean haveVP = false;
			for (MyTreeNode child : tmp.children) {
				if (child.value.equalsIgnoreCase("VP")) {
					haveVP = true;
					tmp = child;
					break;
				}
			}
			if (!haveVP) {
				break;
			}
		}
		// System.out.println(tmp.children.get(0).value);
		return tmp.getLeaves().get(0).value;
	}

	@Override
	public ArrayList<String> getStrFeatures() {
		CoNLLWord zeroWord = part.getWord(zero.start);
		CoNLLWord candWord = part.getWord(cand.start);

		String zeroSpeaker = zeroWord.speaker;
		String candSpeaker = candWord.speaker;
		String canHead = cand.head;

		if (!zeroSpeaker.equals(candSpeaker)) {
			if (canHead.equals("我")) {
				canHead = "你";
			} else if (canHead.equals("你")) {
				canHead = "我";
			}
		}
		ArrayList<String> strFeas = new ArrayList<String>();
		if (group1) {
			strFeas.add(canHead);
			 strFeas.add(canHead + "#" + this.getPredicate(zero.V));
			 strFeas.add(canHead + "#" + this.getPredicate(zero.V) + "#" +
			 this.getObjectNP(zero));
		}
		if (group2) {
			MyTreeNode v1 = cand.V;
			MyTreeNode v2 = zero.V;
			if (v1 != null & v2 != null) {
				MyTreeNode pred1N = this.getPredicateNode(v1);
				MyTreeNode pred2N = this.getPredicateNode(v2);
				String pred1 = "";
				if(pred1N!=null) {
					pred1 = pred1N.value;
				}
				String pred2 = "";
				if(pred2N!=null) {
					pred2 = pred2N.value;
				}
				if (pred1N!=null && pred2N!=null && pred1N.value.equalsIgnoreCase(pred2N.value) && pred1N != pred2N
//						&& zero.sentenceID - cand.sentenceID < 1
//						&& !pred1.equals("是") && !pred1.equals("有") && !pred1.equals("要")
						) {
					allMatch++;
					if (label) {
						rightMatch++;
						// System.out.println("+" + pred1);
					} else {
//						this.printZero(zero, part);
//						System.out.println("================= " + path);
//						System.out.println("================= " + conllPath);
//						System.out.println(cand.extent + "-" + pred1N.value);
					}
				}
				strFeas.add(pred1 + "#" + pred2);
			} else {
				strFeas.add("#");
			}
		}
//		 strFeas.add(this.semStr);
		// strFeas.clear();
		return strFeas;
	}

	public String path;
	public String conllPath;
	
	protected void printZero(EntityMention zero, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start + "#" + this.getPredicate2(zero.V) + "#"
				+ this.getObjectNP2(zero));
	}

	public boolean label = false;
	public static int allMatch = 0;
	public static int rightMatch = 0;

	public void set(ArrayList<EntityMention> zeros, ArrayList<EntityMention> npMentions, EntityMention np,
			EntityMention zero, CoNLLPart part) {
		this.zeros = zeros;
		this.candidates = npMentions;
		this.cand = np;
		this.zero = zero;
		this.part = part;
	}

	public ArrayList<Feature> getZeroAnaphorFeature() {
		ArrayList<Feature> features = new ArrayList<Feature>();
		int sentenceDis = Math.abs(zero.sentenceID - cand.sentenceID);
		sentenceDis = sentenceDis > 30 ? 30 : sentenceDis;
		
		if(zero.sentenceID>=cand.sentenceID) {
		features.add(new Feature(sentenceDis, 1, 62));
		} else {
			features.add(new Feature(sentenceDis + 31, 1, 62));
		}
//		if(zero.start>cand.start) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//				int sentenceDis = zero.sentenceID - cand.sentenceID;
//		sentenceDis = sentenceDis > 30 ? 30 : sentenceDis;
//		features.add(new Feature(sentenceDis, 1, 31));
		
		
		int segmentDis = 0;
		for (int i = cand.start; i <= zero.start; i++) {
			String word = part.getWord(i).word;
			if (word.equals("，") || word.equals("；") || word.equals("。") || word.equals("！") || word.equals("？")) {
				segmentDis++;
			}
		}
		segmentDis = segmentDis > 30 ? 30 : segmentDis;
		features.add(new Feature(segmentDis, 1, 31));

		// sibling
		if (cand.end != -1) {
			CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
			MyTreeNode root = s.syntaxTree.root;
			CoNLLWord word = part.getWord(zero.start);
			MyTreeNode V = zero.V;
			boolean sibling = false;

			if (sentenceDis == 0) {
				MyTreeNode leftLeaf = root.getLeaves().get(part.getWord(cand.start).indexInSentence);
				MyTreeNode rightLeaf = root.getLeaves().get(part.getWord(cand.end).indexInSentence);
				MyTreeNode NPNode = Common.getLowestCommonAncestor(leftLeaf, rightLeaf);
				if (V.parent == NPNode.parent) {
					if (V.childIndex - 1 == NPNode.childIndex) {
						sibling = true;
					}
					if (V.childIndex - 2 == NPNode.childIndex
							&& V.parent.children.get(V.childIndex - 2).children.get(0).value.equalsIgnoreCase("，")) {
						sibling = true;
					}
				}
			}
			if (sibling) {
				features.add(new Feature(0, 1, 2));
			} else {
				features.add(new Feature(1, 1, 2));
			}
		} else {
			features.add(new Feature(1, 0, 2));
		}

		// closet np
		int npIndex = this.candidates.indexOf(cand);
		if (npIndex == this.candidates.size() - 1 || this.candidates.get(npIndex + 1).compareTo(zero) > 0) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}
		return features;
	}

	private ArrayList<Feature> getZeroFeature() {
		ArrayList<Feature> features = new ArrayList<Feature>();
		CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
		MyTreeNode root = s.syntaxTree.root;

		MyTreeNode V = zero.V;

		// 0. Z_Has_anc_NP
		if (V.getXAncestors("NP") == null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}
		// if(!ancestors.get(0).value.contains("IP")) {
		// System.out.println();
		// }

		// 1. Z_Has_Anc_NP_In_IP
		if (V.getFirstXAncestor("NP") != null
				&& Common.isAncestor(V.getFirstXAncestor("NP"), V.getFirstXAncestor("IP"))) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 2. Z_Has_Anc_VP
		if (V.getXAncestors("VP") == null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 3. Z_Has_Anc_VP_In_IP
		if (V.getFirstXAncestor("VP") != null
				&& Common.isAncestor(V.getFirstXAncestor("VP"), V.getFirstXAncestor("IP"))) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 4. Z_Has_Anc_CP
		if (V.getXAncestors("CP") == null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 10. SUBJECT
		if (V.parent.value.equalsIgnoreCase("ip")) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}
		// 11. Clause
		int IPCounts = 0;
		MyTreeNode temp = V;
		while (temp != root) {
			if (temp.value.toLowerCase().startsWith("ip")) {
				IPCounts++;
			}
			temp = temp.parent;
		}
		if (IPCounts > 1) {
			// subordinate clause
			features.add(new Feature(2, 1, 3));
		} else {
			int totalIPCounts = 0;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				if (tn.value.toLowerCase().startsWith("ip")) {
					totalIPCounts++;
				}
				frontie.addAll(tn.children);
			}
			if (totalIPCounts > 1) {
				features.add(new Feature(0, 1, 3));
			} else {
				features.add(new Feature(1, 1, 3));
			}
		}
		// headline feature
		if (zero.sentenceID == 0) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 7. IS_FIRST_ZP
		int zeroIdx = this.zeros.indexOf(zero);
		if (zeroIdx == 0 || this.zeros.get(zeroIdx - 1).sentenceID != zero.sentenceID) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		if (zeroIdx == this.zeros.size() - 1 || this.zeros.get(zeroIdx + 1).sentenceID != zero.sentenceID) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		return features;
	}

	private ArrayList<Feature> getEmptyNPFeature() {
		ArrayList<Feature> features = new ArrayList<Feature>();

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(2, 0, 3));
		features.add(new Feature(2, 0, 3));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));

		features.add(new Feature(1, 0, 2));
		features.add(new Feature(1, 0, 2));

		return features;
	}

	private ArrayList<Feature> getNPFeature() {
		ArrayList<Feature> features = new ArrayList<Feature>();

		CoNLLSentence s = part.getCoNLLSentences().get(cand.sentenceID);
		MyTreeNode root = s.syntaxTree.root;

		MyTreeNode NPNode = cand.NP;
		// 0. A_HAS_ANC_NP
		if (NPNode.getFirstXAncestor("NP") != null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 5. A_HAS_ANC_NP_IN_IP
		if (NPNode.getFirstXAncestor("NP") != null
				&& Common.isAncestor(NPNode.getFirstXAncestor("NP"), NPNode.getFirstXAncestor("IP"))) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 6. HAS_ANC_VP
		if (NPNode.getFirstXAncestor("VP") != null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 7. A_HAS_ANC_VP_IN_IP
		if (NPNode.getFirstXAncestor("VP") != null
				&& Common.isAncestor(NPNode.getFirstXAncestor("VP"), NPNode.getFirstXAncestor("IP"))) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 8. HAS_ANC_CP
		if (NPNode.getFirstXAncestor("CP") != null) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		boolean object = false;
		boolean subject = false;
		ArrayList<MyTreeNode> rightSisters = NPNode.getRightSisters();
		ArrayList<MyTreeNode> leftSisters = NPNode.getLeftSisters();
		for (MyTreeNode node : rightSisters) {
			if (node.value.equalsIgnoreCase("VP")) {
				subject = true;
				break;
			}
		}

		for (MyTreeNode node : leftSisters) {
			if (node.value.equalsIgnoreCase("VV")) {
				object = true;
				break;
			}
		}

		// 9. A_GRAMMATICAL_ROLE
		if (subject) {
			features.add(new Feature(0, 1, 3));
		} else if (object) {
			features.add(new Feature(1, 1, 3));
		} else {
			features.add(new Feature(2, 1, 3));
		}
		// 10. A_CLAUSE
		int IPCounts = NPNode.getXAncestors("IP").size();
		if (IPCounts > 1) {
			// subordinate clause
			features.add(new Feature(0, 1, 3));
		} else {
			int totalIPCounts = 0;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				if (tn.value.toLowerCase().startsWith("ip")) {
					totalIPCounts++;
				}
				frontie.addAll(tn.children);
			}
			if (totalIPCounts > 1) {
				// matrix clause
				features.add(new Feature(1, 1, 3));
			} else {
				// independent clause
				features.add(new Feature(2, 1, 3));
			}
		}

		// A is an adverbial NP
		// if (NP.value.toLowerCase().contains("adv")) {
		// fea[7] = 0;
		// } else {
		// fea[7] = 1;
		// }

		// 12. A is a temporal NP
		if (NPNode.getLeaves().size() != 0
				&& NPNode.getLeaves().get(NPNode.getLeaves().size() - 1).parent.value.equals("NT")) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// is pronoun
		if (cand.isPronoun) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 14. A is a named entity
		if (!cand.ner.equalsIgnoreCase("other")) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}

		// 15. if in headline
		if (cand.sentenceID == 0) {
			features.add(new Feature(0, 1, 2));
		} else {
			features.add(new Feature(1, 1, 2));
		}
		return features;
	}

	private MyTreeNode findTreeNode(EntityMention np, CoNLLPart part) {
		CoNLLWord leftWord = part.getWord(np.start);
		CoNLLWord rightWord = part.getWord(np.end);
		CoNLLSentence s = part.getWord(np.start).getSentence();
		MyTreeNode root = s.syntaxTree.root;
		MyTreeNode leftLeaf = root.getLeaves().get(leftWord.indexInSentence);
		MyTreeNode rightLeaf = root.getLeaves().get(rightWord.indexInSentence);

		MyTreeNode NP = Common.getLowestCommonAncestor(leftLeaf, rightLeaf);
		return NP;
	}

}
