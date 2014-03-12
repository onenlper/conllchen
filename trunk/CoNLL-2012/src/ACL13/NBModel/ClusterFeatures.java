package ACL13.NBModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;

public class ClusterFeatures extends IFeatures {

	public HashMap<String, Integer> StringIndex;

	boolean train;

	public String folder;

	public ClusterFeatures(boolean train, String folder) {
		this.train = train;
		if (train) {
			this.StringIndex = new HashMap<String, Integer>();
		} else {
			this.StringIndex = Common.readFile2Map("strFeaIdx." + folder);
		}
		this.folder = folder;
	}

	public void outputIdx() {
		Common.outputHashMap(this.StringIndex, "strFeaIdx." + folder);
	}

	public int getStringFeaIdx(String str) {
		if(str.isEmpty()) {
			return -1;
		}
		if (this.StringIndex.containsKey(str)) {
			return this.StringIndex.get(str);
		} else {
			if (!train) {
				return -1;
			} else {
				int idx = this.StringIndex.size();
				this.StringIndex.put(str, idx);
				return idx;
			}
		}
	}

	public String getOwnFeature(Entity cluster, CoNLLPart part) {
		HashSet<Integer> feaStrs = new HashSet<Integer>();
		feaStrs.add(this.getStringFeaIdx(cluster.mentions.get(0).head));
		feaStrs.add(this.getStringFeaIdx(cluster.mentions.get(0).extent));
		
		feaStrs.add(this.getStringFeaIdx(part.getWord(cluster.mentions.get(0).start).word));
		feaStrs.add(this.getStringFeaIdx(Character.toString(part.getWord(cluster.mentions.get(0).start).word.charAt(0))));
		
		ArrayList<Integer> strFeaIdxes = new ArrayList<Integer>();
		strFeaIdxes.addAll(feaStrs);
		Collections.sort(strFeaIdxes);

		StringBuilder sb = new StringBuilder();

		Collections.sort(strFeaIdxes);
		for (Integer idx : strFeaIdxes) {
			if (idx.intValue() != -1) {
				sb.append(1000 + idx).append(":1 ");
			}
		}

		return sb.toString();
	}

	public String getClusterFeatures(Entity c1, Entity c2, CoNLLPart part) {
		ArrayList<ArrayList<Boolean>> feases = new ArrayList<ArrayList<Boolean>>();
		HashSet<String> strFeas = new HashSet<String>();
		for (EntityMention m1 : c1.mentions) {
			for (EntityMention m2 : c2.mentions) {
				ArrayList<Boolean> feas = this.getMentionLevelFeatures(m1, m2, part);
				feases.add(feas);
				strFeas.addAll(this.getMentionLevelStrFeature(m1, m2, part));
			}
		}
		ArrayList<Boolean> clusterFeas = getClusterLevelFeatures(c1, c2, part);
		ArrayList<Feature> features = new ArrayList<Feature>();
		features.addAll(categorizedInterMentionFeatures(feases));
		features.addAll(categorizeInterClusterFeatures(clusterFeas));

		StringBuilder sb = new StringBuilder();
		int start = 2;
		for (Feature feature : features) {
			int idx = feature.idx;
			int value = feature.value;
			int space = feature.space;

			if (value != 0) {
				sb.append((start + idx)).append(":1 ");
			}
			start += space;
		}

		ArrayList<Integer> strFeaIdxes = new ArrayList<Integer>();

		for (String str : strFeas) {
			strFeaIdxes.add(this.getStringFeaIdx(str));
		}

		if (start >= 1000) {
			System.err.println("Not enough space");
			System.exit(1);
		}
		start = 1000;
		Collections.sort(strFeaIdxes);
		for (Integer idx : strFeaIdxes) {
			if (idx.intValue() != -1) {
				sb.append(start + idx).append(":1 ");
			}
		}

		return sb.toString().trim();
	}

	public ArrayList<Feature> categorizeInterClusterFeatures(ArrayList<Boolean> feas) {
		ArrayList<Feature> features = new ArrayList<Feature>();
		for (Boolean fea : feas) {
			if (fea) {
				features.add(new Feature(0, 1, 1));
			} else {
				features.add(new Feature(0, 0, 1));
			}
		}
		return features;
	}

	public ArrayList<Feature> categorizedInterMentionFeatures(ArrayList<ArrayList<Boolean>> feases) {
		ArrayList<Feature> features = new ArrayList<Feature>();
		for (int i = 0; i < feases.get(0).size(); i++) {
			boolean value = false;
			// System.err.println(feases.size() + "#" + feases.get(0).size());
			for (int k = 0; k < feases.size(); k++) {
				// System.err.println(feases.get(k).size());
				value = value | feases.get(k).get(i);
			}
			if (value) {
				features.add(new Feature(0, 1, 1));
			} else {
				features.add(new Feature(0, 0, 1));
			}
		}
		return features;
	}

	public ArrayList<Boolean> getClusterLevelFeatures(Entity c1, Entity c2, CoNLLPart part) {
		ArrayList<Boolean> feas = new ArrayList<Boolean>();

//		if (attributeAgree(c1, c2, part)) {
//			feas.add(true);
//		}
//		if (personDisagree(c1, c2, part)) {
//			feas.add(true);
//		}
//		if (negativeRule(c1, c2, part)) {
//			feas.add(true);
//		}
//		if (wordInclude(c1, c2, part)) {
//			feas.add(true);
//		}
		return feas;
	}

	public ArrayList<Boolean> getMentionLevelFeatures(EntityMention m1, EntityMention m2, CoNLLPart part) {
		ArrayList<Boolean> feas = new ArrayList<Boolean>();

		//2
		if (sameHeadFeatures(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//3
		if (I2IFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//4
		if (speaker2IFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//5
		if (you2youFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//6
		if (I2youFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//7
		if (exactMatch(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//8
		if (roleFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//9
		if (aliasFeature(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//10
		if (headMatch(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//11
		if (inWithIn(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//12
		if (sameModifier(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//13
		if (relaxHeadMatch(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//14
		if (mentionPersonDisagree(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//15
		if (this.conflictModifier(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//16
		if (this.headContain(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//17
		if (this.headStart(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//18
		if (this.headEnd(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//19
		if (this.headEqualFirstSentence(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//20
		if (this.headNoEqualExtentContain(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}
		//21
		if (this.headNoEqualExtentStart(m1, m2, part)) {
			feas.add(true);
		} else {
			feas.add(false);
		}

		return feas;
	}

	public ArrayList<String> getMentionLevelStrFeature(EntityMention m1, EntityMention m2, CoNLLPart part) {
		ArrayList<String> feas = new ArrayList<String>();

		String pair = headPair(m1, m2, part);
		String extentPair = this.extentPair(m1, m2, part);

		feas.add(pair);
		feas.add(extentPair);

		// semantic
		String commonSemantic = this.getCommonSemantic(m1, m2, part);
		if (!commonSemantic.isEmpty()) {
			feas.add(Integer.toString(commonSemantic.length()));
			for (int i = 0; i < commonSemantic.length(); i++) {
				feas.add(commonSemantic.substring(0, i));
			}
		}

		return feas;
	}

	static class Feature {
		int idx;
		int value;
		int space;

		public Feature(int idx, int value, int space) {
			this.idx = idx;
			this.value = value;
			this.space = space;
		}
	}
}
