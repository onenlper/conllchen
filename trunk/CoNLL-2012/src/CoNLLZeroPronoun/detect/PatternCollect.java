package CoNLLZeroPronoun.detect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import util.Common;

public class PatternCollect {

	public static void main(String args[]) {
		ArrayList<String> files = new ArrayList<String>();
		files.addAll(Common.getLines("chinese_list_all_development"));
		files.addAll(Common.getLines("chinese_list_all_train"));

		ZeroDetect det = new ZeroDetect(true, "all");

		HashSet<String> patterns = new HashSet<String>();

		HashSet<String> wildcardPatterns = new HashSet<String>();
		int k = files.size();
		for (String file : files) {
			System.out.println(file + "#" + k--);
			CoNLLDocument document = new CoNLLDocument(file);

			int a = file.lastIndexOf("/");
			String name = file.substring(a + 1);
			for (CoNLLPart part : document.getParts()) {

				ArrayList<String> tokenized = Common.getLines("conll-source/" + name + "_" + part.getPartID()
						+ ".token");

				det.setPart(part);

				// System.out.println(part.getCoNLLSentences().size() + "#" +
				// tokenized.size());
				// if(part.getCoNLLSentences().size()!=tokenized.size()) {
				// Common.bangErrorPOS("!!!");
				// }

				ArrayList<EntityMention> herusiticZeros = det.getHeuristicZeros();
				System.out.println(herusiticZeros.size());
				for (EntityMention m : herusiticZeros) {
					// add4gramPattern(m, part, patterns, wildcardPatterns,
					// tokenized);
					patterns.addAll(add3gramPattern(m, part, tokenized));
					wildcardPatterns.addAll(add3gramPatternWild(m, part, tokenized));
				}
			}
		}
		Common.outputHashSet(patterns, "3-grams");
		Common.outputHashSet(wildcardPatterns, "4-grams-with-wildcard");
	}

	public static String addTokens(ArrayList<String> tokens) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if (token.trim().isEmpty()) {
				return "";
			}
			if (token.trim().equals("PU")) {
				if (i == 0) {
					token = "<S>";
				} else if (i == tokens.size() - 1) {
					token = "</S>";
				} else {
					return "";
				}
			}
			sb.append(token).append(" ");
		}
		return sb.toString().trim();
	}

	
	
	public static ArrayList<String> add3gramPattern(EntityMention zero, CoNLLPart part, ArrayList<String> tokenized) {
		ArrayList<String> ps = new ArrayList<String>();
		
		CoNLLSentence s = part.getWord(zero.start).sentence;

		String ngramTokens[] = tokenized.get(s.getSentenceIdx()).split("\\s+");

		ArrayList<String> tokens = null;
		int index = part.getWord(zero.start).indexInSentence;
		// get char index
		int charIdx = 0;
		for (int i = 0; i < index; i++) {

			charIdx += s.getWords().get(i).word.length();
		}

		int ngramIndex = 0;
		int charIdx2 = 0;
		for (int i = 0; i < ngramTokens.length; i++) {
			if (charIdx2 == charIdx) {
				ngramIndex = i;
				break;
			}
			String token = ngramTokens[i];
			charIdx2 += token.length();
			if (charIdx2 > charIdx) {
				ngramIndex = i - 1;
				break;
			}
		}
		index = ngramIndex;
		// pattern without wildcard
		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		String p = addTokens(tokens);
		if(!p.isEmpty()) {
			ps.add(p);
		}

		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add(getWord(ngramTokens, index));
		p = addTokens(tokens);
		if(!p.isEmpty()) {
			ps.add(p);
		}

		return ps;
	}
	
	public static ArrayList<String> add3gramPatternWild(EntityMention zero, CoNLLPart part, ArrayList<String> tokenized) {
		ArrayList<String> ps = new ArrayList<String>();
		
		CoNLLSentence s = part.getWord(zero.start).sentence;

		String ngramTokens[] = tokenized.get(s.getSentenceIdx()).split("\\s+");

		ArrayList<String> tokens = null;
		int index = part.getWord(zero.start).indexInSentence;
		// get char index
		int charIdx = 0;
		for (int i = 0; i < index; i++) {

			charIdx += s.getWords().get(i).word.length();
		}

		int ngramIndex = 0;
		int charIdx2 = 0;
		for (int i = 0; i < ngramTokens.length; i++) {
			if (charIdx2 == charIdx) {
				ngramIndex = i;
				break;
			}
			String token = ngramTokens[i];
			charIdx2 += token.length();
			if (charIdx2 > charIdx) {
				ngramIndex = i - 1;
				break;
			}
		}
		index = ngramIndex;

		// pattern with wildcard
		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add("*");
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		String p = addTokens(tokens);
		if(!p.isEmpty()) {
			ps.add(p);
		}
		
		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add("*");
		tokens.add(getWord(ngramTokens, index));
		p = addTokens(tokens);
		if(!p.isEmpty()) {
			ps.add(p);
		}
		return ps;
	}

	public static void add4gramPattern(EntityMention zero, CoNLLPart part, HashSet<String> patterns,
			HashSet<String> wildcardPatterns, ArrayList<String> tokenized) {
		CoNLLSentence s = part.getWord(zero.start).sentence;

		String ngramTokens[] = tokenized.get(s.getSentenceIdx()).split("\\s+");

		ArrayList<String> tokens = null;
		int index = part.getWord(zero.start).indexInSentence;
		// get char index
		int charIdx = 0;
		for (int i = 0; i < index; i++) {
			if(skipPU.contains(s.getWords().get(i).word)) {
				continue;
			}
			charIdx += s.getWords().get(i).word.length();
		}

		int ngramIndex = 0;
		int charIdx2 = 0;
		for (int i = 0; i < ngramTokens.length; i++) {
			if (charIdx2 == charIdx) {
				ngramIndex = i;
				break;
			}
			String token = ngramTokens[i];
			if(skipPU.contains(token)) {
				continue;
			}
			charIdx2 += token.length();
			if (charIdx2 > charIdx) {
				ngramIndex = i - 1;
				break;
			}
		}
		index = ngramIndex;
		// pattern without wildcard
		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		tokens.add(getWord(ngramTokens, index + 2));
		addTokens(tokens);

		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		addTokens(tokens);

		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 3));
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add(getWord(ngramTokens, index));
		addTokens(tokens);

		// pattern with wildcard
		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add("*");
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		tokens.add(getWord(ngramTokens, index + 2));
		addTokens(tokens);

		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add("*");
		tokens.add(getWord(ngramTokens, index));
		tokens.add(getWord(ngramTokens, index + 1));
		addTokens(tokens);

		tokens = new ArrayList<String>();
		tokens.add(getWord(ngramTokens, index - 3));
		tokens.add(getWord(ngramTokens, index - 2));
		tokens.add(getWord(ngramTokens, index - 1));
		tokens.add("*");
		tokens.add(getWord(ngramTokens, index));
		addTokens(tokens);
	}

	public static HashSet<String> periods = new HashSet<String>(Arrays.asList("，", "。", ",", "：", "？", "！", "；", "．",
			"?"));

	public static HashSet<String> skipPU = new HashSet<String>(Arrays.asList("＂", "）", "--", "」", "”", "“", "「", "、",
			"《", "’"));

	public static String[] numbers = { "１", "２", "３", "４", "６", "７", "" };

	public static String getWord(String[] words, int idx) {
		if (idx < -1) {
			return "";
		} else if (idx == -1) {
			return "<S>";
		} else if (idx == words.length) {
			return "</S>";
		} else if (idx > words.length) {
			return "";
		} else {
			if (periods.contains(words[idx])) {
				return "PU";
			}
			return words[idx];
		}
	}
}
