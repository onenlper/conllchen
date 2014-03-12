package crfMentionDetect;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.Element;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class MentionDetectUtil {

	private static HashSet<String> pronouns = null;

	public static void assignPronounFea(ArrayList<MentionInstance> mis) {
		if (pronouns == null) {
			pronouns = Common.readFile2Set(Common.dicPath + "pronoun");
		}
		for (MentionInstance mi : mis) {
			String word = mi.word;
			if (pronouns.contains(word)) {
				mi.setIsPronoun1(1);
			} else {
				mi.setIsPronoun1(0);
			}
		}
	}

	public static void assignNEFea(ArrayList<MentionInstance> mis, ArrayList<Element> NEElements) {
		for (Element NE : NEElements) {
			mis.get(NE.start).setNerFea("B-" + NE.content);
			for (int i = NE.start + 1; i <= NE.end; i++) {
				mis.get(i).setNerFea("I-" + NE.content);
			}
		}
	}
	public static HashMap<String, String> POS09DIC;
	
	public static HashMap<String, String> POS10DIC;
	
	public static void assignNounPhrasePOSFea(ArrayList<MentionInstance> mis, CoNLLPart part) {
		if(POS09DIC==null) {
			POS09DIC = Common.readFile2Map2(Common.dicPath + "09POSDIC");
		}
		if(POS10DIC==null) {
			POS10DIC = Common.readFile2Map2(Common.dicPath + "10POSDIC");;
		}
		for (int i = 0; i < mis.size(); i++) {
			MentionInstance mi = mis.get(i);
			CoNLLWord word = part.getWord(i);
			mi.setPosFea(word.posTag);
			String token = mi.getWord();
			mi.setPOS09(POS09DIC.get(token));
			mi.setPOS10(POS10DIC.get(token));
			CoNLLSentence sentence = word.getSentence();
			int leafIdx = i - sentence.getStartWordIdx();
			MyTreeNode leaf = sentence.syntaxTree.leaves.get(leafIdx);
			ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
			boolean isInNP = false;
			for (MyTreeNode treeNode : ancestors) {
				if (treeNode.value.toLowerCase().startsWith("np") || treeNode.value.toLowerCase().startsWith("qp")) {
					isInNP = true;
					break;
				}
			}
			if (isInNP) {
				mi.setIsInNP(1);
			} else {
				mi.setIsInNP(0);
			}
		}
	}

	public static void assignEnglishFea(ArrayList<MentionInstance> instances) {
		for (MentionInstance instance : instances) {
			String word = instance.getWord();
			boolean english = true;
			boolean upcase = true;
			for (int i = 0; i < word.length(); i++) {
				char c = word.charAt(i);
				if (!(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
					english = false;
				}
				if (!(c >= 'A' && c <= 'Z')) {
					upcase = false;
				}
			}
			if (english) {
				instance.setIsEnglish(0);
			} else {
				instance.setIsEnglish(1);
			}
			if (upcase) {
				instance.setIsUpcaseEnglish(0);
			} else {
				instance.setIsUpcaseEnglish(1);
			}
		}
	}

	public static void outputInstances(ArrayList<MentionInstance> instances, FileWriter fw, ArrayList<CoNLLSentence> sentences) {
		HashSet<Integer> ends = new HashSet<Integer>();
		for(CoNLLSentence sentence : sentences) {
			ends.add(sentence.getEndWordIdx());
		}
		try {
			for (int i = 0; i < instances.size(); i++) {
				MentionInstance instance = instances.get(i);
				fw.write(instance.toString() + " \n");
				if(ends.contains(i)) {
					fw.write("\n");
				}
			}
			fw.write("\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static HashSet<String> locations = Common.readFile2Set(Common.dicPath + "location2");
	public static HashSet<String> orgs_intl = Common.readFile2Set(Common.dicPath + "orgs_intl");
	public static HashSet<String> proper_industry = Common.readFile2Set(Common.dicPath + "propernames_industry");
	public static HashSet<String> proper_org = Common.readFile2Set(Common.dicPath + "propernames_org");
	public static HashSet<String> proper_other = Common.readFile2Set(Common.dicPath + "propernames_other");
	public static HashSet<String> proper_people = Common.readFile2Set(Common.dicPath + "propernames_people");
	public static HashSet<String> proper_place = Common.readFile2Set(Common.dicPath + "propernames_place");
	public static HashSet<String> proper_press = Common.readFile2Set(Common.dicPath + "propernames_press");
	public static HashSet<String> who_china = Common.readFile2Set(Common.dicPath + "whoswho_china");
	public static HashSet<String> who_intl = Common.readFile2Set(Common.dicPath + "whoswho_international");

	// generate IN_LOCATION_NAME(c-1c0), IN_LOCATION_NAME(c0c1) features, and
	// other dic features
	public static void assignInLocationFea(ArrayList<MentionInstance> instances) {
		for (int i = 0; i < instances.size(); i++) {
			MentionInstance instance = (MentionInstance) instances.get(i);
			if (Common.isPun(instance.getWord().charAt(0))) {
				continue;
			}
			int start = i;
			int end = i;
			ArrayList<String> strs = new ArrayList<String>();
			while (!Common.isPun(instances.get(end).getWord().charAt(0))) {
				strs.add(instances.get(end).getWord());
				end++;
				// the maximum length of location is 15
				if (end - start == 5 || end == instances.size()) {
					break;
				}
			}
			end--;
			for (int j = strs.size(); j >= 0; j--) {
				StringBuilder sb = new StringBuilder();
				for(int k=0;k<j;k++) {
					sb.append(strs.get(k));
				}
				String str = sb.toString();
				if (locations.contains(str)) {
					((MentionInstance) instances.get(start)).setInLocation1("B");
					for (int k = start + 1; k < end; k++) {
						((MentionInstance) instances.get(k)).setInLocation1("I");
					}
				}
				if (orgs_intl.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_ORGS_INTL("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_ORGS_INTL("I");
					}
				}
				if (proper_industry.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_INDUSTRY("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_INDUSTRY("I");
					}
				}
				if (proper_org.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_ORG("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_ORG("I");
					}
				}
				if (proper_other.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_OTHER("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_OTHER("I");
					}
				}
				if (proper_people.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_PEOPLE("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_PEOPLE("I");
					}
				}
				if (proper_place.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_PLACE("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_PLACE("I");
					}
				}
				if (proper_press.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_PROP_PRESS("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_PROP_PRESS("I");
					}
				}
				if (who_china.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_WHOWHO_CHINA("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_WHOWHO_CHINA("I");
					}
				}
				if (who_intl.contains(str)) {
					((MentionInstance) instances.get(start)).setIN_WHOWHO_INTER("B");
					for (int k = start + 1; k <= end; k++) {
						((MentionInstance) instances.get(k)).setIN_WHOWHO_INTER("I");
					}
				}
				end--;
			}
		}
	}
}
