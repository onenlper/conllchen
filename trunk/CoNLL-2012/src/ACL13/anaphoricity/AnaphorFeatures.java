package ACL13.anaphoricity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import util.Common;
import util.Common.Animacy;
import util.Common.Numb;

public class AnaphorFeatures {
	
	public HashMap<String, Integer> StringIndex;
	
	boolean train;
	
	public AnaphorFeatures(boolean train, String folder) {
		this.train = train;
		if(train) {
			this.StringIndex = new HashMap<String, Integer>(); 
		} else {
			this.StringIndex = Common.readFile2Map("anaphorStrFeaIdx." + folder);
		}
	}
	
	public String getFea(List<EntityMention> ms, EntityMention m, List<EntityMention> msRight, CoNLLPart part) {
		
		ArrayList<Feature> feas = new ArrayList<Feature>();
		feas.addAll(this.gramaticFeatures(ms, m, msRight, part));
		feas.addAll(this.lexicalFeatures(ms, m, msRight, part));
		
		StringBuilder sb = new StringBuilder();
		
		int start = 2;
		for (Feature feature : feas) {
			int idx = feature.idx;
			int value = feature.value;
			int space = feature.space;

			if (value != 0) {
				sb.append((start + idx)).append(":1 ");
			}
			start += space;
		}
		
		ArrayList<Integer> strFeaIdxes = this.stringFeatures(m, part);
		
		start = 1000;
		Collections.sort(strFeaIdxes);
		for (Integer idx : strFeaIdxes) {
			if (idx.intValue() != -1) {
				sb.append(start + idx).append(":1 ");
			}
		}
		
		return sb.toString().trim();
	}
	
	public ArrayList<Feature> gramaticFeatures(List<EntityMention> ms, EntityMention m, List<EntityMention> msRight,  CoNLLPart part) { 
		ArrayList<Feature> features = new ArrayList<Feature>();
		
		if(m.isPronoun) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.isProperNoun) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(!m.isPronoun && !m.isProperNoun) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.animacy==Animacy.ANIMATE) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.animacy==Animacy.INANIMATE) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.animacy==Animacy.UNKNOWN) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.number==Numb.PLURAL) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.number==Numb.SINGULAR) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.number==Numb.UNKNOWN) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.start==m.end) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(!m.isProperNoun) {
			boolean proper = false;
			
			for(int i=m.start;i<m.end;i++) {
				if(!part.getWord(i).rawNamedEntity.equalsIgnoreCase("*")) {
					proper = true;
					break;
				}
			}
			if(proper) {
				features.add(new Feature(0, 1, 1));
			} else {
				features.add(new Feature(0, 0, 1));
			}
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		//same predicate
		
		//same modifier
		if(m.sentenceID==0) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		if(m.sentenceID==1) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}

		//same in Semantic Dic
		//TODO
		
		return features;
	}
	
	public ArrayList<Feature> lexicalFeatures(List<EntityMention> ms, EntityMention m, List<EntityMention> msRight,  CoNLLPart part) {
		ArrayList<Feature> features = new ArrayList<Feature>();
		//str match
		boolean strMatch = false;
		for(EntityMention m1 : ms) {
			if(m1.source.equalsIgnoreCase(m.source)) {
				strMatch = true;
				break;
			}
		}
		if(strMatch) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		//head match
		boolean headMatch = false;
		for(EntityMention m1 : ms) {
			if(m1.head.equalsIgnoreCase(m.head)) {
				headMatch = true;
				break;
			}
		}
		if(headMatch) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		//conjunction
		boolean conj = false;
		for(int i = m.start;i<=m.end;i++) {
			if(part.getWord(i).posTag.equalsIgnoreCase("CC")) {
				conj = true;
				break;
			}
		}
		if(conj) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		// head contain
		boolean headContain = false;
		for(EntityMention m1 : ms) {
			if(m1.head.contains(m.head)) {
				headContain = true;
				break;
			} 
		}
		if(headContain) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		//full contain
		boolean fullContain = false;
		for(EntityMention m1 : ms) {
			if(m1.source.contains(m.source)) {
				fullContain = true;
				break;
			} 
		}
		if(fullContain) {
			features.add(new Feature(0, 1, 1));
		} else {
			features.add(new Feature(0, 0, 1));
		}
		
		return features;
	}
	
	
	public ArrayList<Integer> stringFeatures(EntityMention m, CoNLLPart part) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		HashSet<Integer> set = new HashSet<Integer>();
		set.add(getStringFeaIdx("start_" + part.getWord(m.start).word));
		set.add(getStringFeaIdx("head_" + part.getWord(m.end).word));
		set.add(getStringFeaIdx("extent_" + m.source));
		set.add(getStringFeaIdx("ner_" + part.getWord(m.end).rawNamedEntity));
		set.add(getStringFeaIdx("StartPos_" + part.getWord(m.start).posTag));
		
		ret.addAll(set);
		Collections.sort(ret);
		return ret;
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
