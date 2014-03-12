package ace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ACECorefCommon {

	// public static int maxLone[] = {3, 3, 2, 2, -2, 3, -2, -2};
	// public static int maxDiff[] = {2, 2, 2, 3, 3, 2, 2, -2,
	// 3, -2, -2, 7, -2, 8, 2, 2,
	// 3, 3, 3, 3, 3, 3, 3, 3,
	// 2, 2, 11, 5, 2, -2, 10, 6,
	// -2, 4, 5, 2
	// /* modifier and head*/, 2, 2};

	// public static int maxLone[] = {-2, -2, 2, 2, -2, 3, -2, -2};
	// public static int maxDiff[] = {2, 2, 2, -2, -2, 2, 2, -2,
	// 3, -2, -2, 7, -2, 8, 2, 2,
	// 3, 3, -2, -2, -2, 3, 3, 3,
	// 2, 2, 11, 5, 2, -2, 10, 6,
	// -2, 4, 5, 2
	// /* modifier and head*/, 2, 2};

	public static ArrayList<Element> getGoldenTypeElements(ArrayList<Entity> entities) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (Entity entity : entities) {
			ArrayList<EntityMention> ems = entity.mentions;
			for (EntityMention em : ems) {
				int start = em.getS();
				int end = em.getE();
				String content = entity.type.toLowerCase();
				Element element = new Element(start, end, content);
				elements.add(element);
			}
		}
		return elements;
	}

	public static ArrayList<EntityMention> getGoldenEM(ArrayList<Entity> entities) {
		ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
		for (Entity entity : entities) {
			for (EntityMention em : entity.mentions) {
				em.semClass = entity.getType();
				em.subType = entity.getType().substring(0, 1) + "-" + entity.subType;
				ems.add(em);
			}
		}
		return ems;
	}

	public static ArrayList<EntityMention> getGoldMentions(String sgmFile) {
		String apfFn = ACECommon.getRelateApf(sgmFile);
		ArrayList<Entity> entities = ACECommon.getEntities(apfFn);
		return getGoldenEM(entities);
	}

	public static ArrayList<Element> getGoldenSubTypeElements(ArrayList<Entity> entities) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (Entity entity : entities) {
			ArrayList<EntityMention> ems = entity.mentions;
			for (EntityMention em : ems) {
				int start = em.getS();
				int end = em.getE();
				String content = entity.type.substring(0, 1) + "-" + entity.subType;
				Element element = new Element(start, end, content.toLowerCase());
				elements.add(element);
			}
		}
		return elements;
	}

	public static int iii = 0;
	public static int jjj = 0;

	public static String getSemanticSymbol(EntityMention em, String head) {
		if (head.charAt(0) == '副') {
			head = head.substring(1);
		}
		if (!em.head.endsWith(head)) {
			head = em.head;
			// System.out.println(head + " " + em.head + "######");
		} else {

		}
		String semantics[] = Common.getSemanticDic().get(head);
		String semantic = "";
		if (semantics != null) {
			semantic = semantics[0];
		} else {
			boolean findNer = false;
			if (em.ner.equalsIgnoreCase("PERSON")) {
				semantic = "A0000001";
			} else if (em.ner.equalsIgnoreCase("LOC")) {
				semantic = "Be000000";
			} else if (em.ner.equalsIgnoreCase("GPE")) {
				semantic = "Di020000";
			} else if (em.ner.equalsIgnoreCase("ORG")) {
				semantic = "Dm000000";
			} else {
				// System.out.println(ele.content + " " + em.head);
			}
			findNer = true;
			if (!findNer) {
				if (head.endsWith("们") || head.endsWith("人") || head.endsWith("者") || head.endsWith("哥")
						|| head.endsWith("员") || head.endsWith("弟") || head.endsWith("爸")) {
					semantic = "A0000001";
				}
			}
		}
		return semantic;
	}

	public static void heuristcSemanticNER(EntityMention em, PlainText sgm, ParseResult pr, int endWordIdx) {
		if (em.head.charAt(em.head.length() - 1) == '们') {
			em.subType = "p-group";
			em.semClass = "PER";
		}
		if (Common.getSemanticDic().containsKey(pr.words.get(endWordIdx)) || !em.ner.equalsIgnoreCase("other")) {
			iii++;
		} else {
			jjj++;
			// System.out.println(em.head);
		}
	}

	public static ArrayList<ParseResult> readStanfordParseFile(String filename, PlainText plainText) {
		ArrayList<ParseResult> parseResults = new ArrayList<ParseResult>();
		// System.out.println(filename);
		ArrayList<String> lines = Common.getLines(filename);
		int idx = - 1;
		String content = plainText.content;
		boolean first = true;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String sentence = line;
			StringBuilder sb = new StringBuilder();
			int j = i + 2;
			while (!lines.get(j).trim().isEmpty()) {
				sb.append(lines.get(j));
				// System.out.println(lines.get(j));
				j++;
			}
			String treeStr = sb.toString();
			int k = j + 1;
			ArrayList<String> depends = new ArrayList<String>();
			while (!lines.get(k).trim().isEmpty()) {
				line = lines.get(k);
				// System.out.println(line);
				int pos = line.indexOf('(');
				String type = line.substring(0, pos);
				String tokens[] = line.split(" ");
				pos = tokens[0].lastIndexOf('-');
				String t1 = tokens[0].substring(pos + 1, tokens[0].length() - 1);
				pos = tokens[1].lastIndexOf('-');
				String t2 = tokens[1].substring(pos + 1, tokens[1].length() - 1);
				depends.add(type + " " + t1 + " " + t2);
				k++;
			}
			i = k;
			ParseResult pr = new ParseResult(sentence, Common.constructTree(treeStr), depends);

			if (first) {
				StringBuilder headlineSB = new StringBuilder();
				String tokens[] = sentence.split(" ");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}
					int pos = token.lastIndexOf('/');
					String word = token.substring(0, pos);
					headlineSB.append(word);
				}
				plainText.headline = headlineSB.toString();
				first = false;
			}

			ArrayList<String> words = pr.words;
			ArrayList<int[]> positions = new ArrayList<int[]>();
			for (int n = 0; n < words.size(); n++) {
				String token = words.get(n);
				int[] p = new int[2];
				idx = content.indexOf(token.charAt(0), idx + 1);
				p[0] = idx;
				for (int m = 1; m < token.length(); m++) {
					idx = content.indexOf(token.charAt(m), idx + 1);
				}
				p[1] = idx;
				positions.add(p);
			}
			pr.positions = positions;

			parseResults.add(pr);
		}
		return parseResults;
	}

	// get all mentions from CRF predicted files
	public static HashMap<String, ArrayList<EntityMention>> getMentionsFromCRFFile(ArrayList<String> files,
			String crfFile) {
		ArrayList<ArrayList<EntityMention>> entityMentionses = new ArrayList<ArrayList<EntityMention>>();
		ArrayList<String> lines = Common.getLines(crfFile);
		int fileIdx = 0;
		PlainText plainText = ACECommon.getPlainText(files.get(fileIdx));
		int idx = plainText.start - 1;
		String content = plainText.content;
		int start = 0;
		int end = 0;
		int lastIdx = 0;
		ArrayList<EntityMention> currentArrayList = new ArrayList<EntityMention>();
		entityMentionses.add(currentArrayList);
		for (int i = 0; i < lines.size();) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) {
				i++;
				continue;
			}

			idx = content.indexOf(line.charAt(0), idx + 1);
			// System.out.println(line);
			if (idx == -1 || idx>plainText.end) {
				fileIdx++;
				currentArrayList = new ArrayList<EntityMention>();
				entityMentionses.add(currentArrayList);
				// System.out.println(files.get(fileIdx));
				plainText = ACECommon.getPlainText(files.get(fileIdx));
				idx = plainText.start - 1;
				content = plainText.content;
				continue;
			}
			i++;
			if (line.endsWith("B")) {
				start = idx;
				while (true) {
					lastIdx = idx;
					if (!lines.get(i).endsWith("I") || lines.get(i).isEmpty()) {
						break;
					}
					idx = content.indexOf(lines.get(i++).charAt(0), lastIdx + 1);
				}
				end = lastIdx;
				EntityMention em = new EntityMention();
				// using head to do co-reference
				em.head = content.substring(start, end + 1).replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
				em.headCharStart = start;
				em.headCharEnd = end;
				currentArrayList.add(em);
				
				if(crfFile.contains("time")) {
					em.semClass = "time";
					em.subType = "time";
				} else if(crfFile.contains("value")) {
					em.semClass = "value";
					em.subType = "value";
				}
				
			}
		}
		HashMap<String, ArrayList<EntityMention>> maps = new HashMap<String, ArrayList<EntityMention>>();
		for (int i = 0; i < files.size(); i++) {
			maps.put(files.get(i), entityMentionses.get(i));
		}
		return maps;
	}

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_0");
		String crfFile = "D:\\ACL12\\model\\ACE\\semantic\\FAC\\result";
		ArrayList<ArrayList<Element>> elementses = ACECorefCommon.getSemanticsFromCRFFile(files, crfFile);
		for (ArrayList<Element> elements : elementses) {
			for (Element el : elements) {
				System.out.println(el.start + " " + el.end + " " + el.content + " " + el.confidence);
			}
		}
	}

	// get all semantic class from CRF predicted files
	public static ArrayList<ArrayList<Element>> getSemanticsFromCRFFile(ArrayList<String> files, String crfFile) {
		// System.out.println(crfFile);
		ArrayList<ArrayList<Element>> entityMentionses = new ArrayList<ArrayList<Element>>();
		ArrayList<String> lines = Common.getLines(crfFile);
		int fileIdx = 0;
		PlainText sgm = ACECommon.getPlainText(files.get(fileIdx));
		// System.out.println(files.get(fileIdx));
		int idx = sgm.start - 1;
		String content = sgm.content;
		int start = 0;
		int end = 0;
		int lastIdx = 0;
		ArrayList<Element> currentArrayList = new ArrayList<Element>();
		entityMentionses.add(currentArrayList);
		for (int i = 0; i < lines.size();) {
			String line = lines.get(i);
			if (line.trim().isEmpty() || (line.charAt(0) == '#') && line.split("\\s+").length == 2) {
				i++;
				continue;
			}
			String tokens[] = line.trim().split("\\s+");
			String predict = tokens[tokens.length - 1];
			idx = content.indexOf(line.charAt(0), idx + 1);
			// System.out.println(line);
			if (idx == -1) {
				fileIdx++;
				currentArrayList = new ArrayList<Element>();
				entityMentionses.add(currentArrayList);
				// System.out.println(files.get(fileIdx));
				// System.out.println(line);
				sgm = ACECommon.getPlainText(files.get(fileIdx));

				idx = sgm.start - 1;
				content = sgm.content;
				continue;
			}
			i++;
			double totalConfidence = 0;
			int pos = predict.lastIndexOf('/');
			if (pos > 0) {
				totalConfidence += Double.parseDouble(predict.substring(pos + 1));
			}
			String type = "";
			if (predict.startsWith("B")) {
				start = idx;
				if (pos > 0) {
					type = predict.substring(2, pos);
				} else {
					type = predict.substring(2);
				}

				while (true) {
					lastIdx = idx;
					line = lines.get(i);
					tokens = line.trim().split("\\s+");
					predict = tokens[tokens.length - 1];
					if (!predict.startsWith("I") || lines.get(i).isEmpty() || (line.charAt(0) == '#')
							&& line.split("\\s+").length == 2) {
						break;
					}
					pos = predict.lastIndexOf('/');
					if (pos > 0) {
						totalConfidence += Double.parseDouble(predict.substring(pos + 1));
					}
					idx = content.indexOf(lines.get(i++).charAt(0), lastIdx + 1);
				}
				end = lastIdx;

				Element em = new Element(start, end, type.replace("_", ""));
				em.confidence = totalConfidence / ((double) (end + 1 - start));

				currentArrayList.add(em);
			}
		}
		return entityMentionses;
	}

	public static class CRFSegment {
		HashMap<Integer, CRFChar> crfChars;

		public CRFSegment() {
			this.crfChars = new HashMap<Integer, CRFChar>();
		}

		public void addOneLine(int index, String line) {
			String tokens[] = line.split("\\s+");
			CRFChar crfChar = new CRFChar(tokens[0]);
			for (int i = 1; i < tokens.length; i++) {
				if (tokens[i].indexOf('/') >= 0) {
					crfChar.put(tokens[i]);
				}
			}
			crfChars.put(index, crfChar);
		}

		public ArrayList<Pair> getType(int start, int end, PlainText plainText) {
			HashMap<String, Pair> pairs = new HashMap<String, Pair>();
			for (int i = start; i <= end; i++) {
				char c = plainText.content.charAt(i);
				if (c == '\n' || c == ' ') {
					continue;
				}
				CRFChar crfChar = crfChars.get(i);
				for (String type : crfChar.confidences.keySet()) {
					double value = crfChar.confidences.get(type);
					if (pairs.containsKey(type)) {
						pairs.get(type).value += value;
					} else {
						Pair pair = new Pair(type, value);
						pairs.put(type, pair);
					}
				}
			}
			ArrayList<Pair> types = new ArrayList<Pair>();
			types.addAll(pairs.values());
			Collections.sort(types);
			Collections.reverse(types);
			return types;
		}
	}

	public static class Pair implements Comparable {
		public String key;
		public double value;

		public Pair(String key, double value) {
			this.key = key;
			this.value = value;
		}

		public int hashCode() {
			return key.hashCode();
		}

		public boolean equal(Object obj) {
			return this.key.equals(((Pair) obj).key);
		}

		@Override
		public int compareTo(Object arg0) {
			if (this.value > ((Pair) arg0).value) {
				return 1;
			} else if (this.value < ((Pair) arg0).value) {
				return -1;
			}
			return 0;
		}
	}

	public static class CRFChar {
		String word;
		HashMap<String, Double> confidences;

		public CRFChar(String w) {
			this.word = w;
			confidences = new HashMap<String, Double>();
		}

		public void put(String str) {
			if (!str.startsWith("O/")) {
				int a = str.indexOf('-');
				int b = str.indexOf('/');
				this.confidences.put(str.substring(a + 1, b), Double.valueOf(str.substring(b + 1)));
			}
		}
	}

	public static ArrayList<CRFSegment> getSemanticsFromCRFFile2(ArrayList<String> files, String crfFile) {
		ArrayList<String> lines = Common.getLines(crfFile);
		int fileIdx = 0;
		ArrayList<CRFSegment> crfSegments = new ArrayList<CRFSegment>();
		CRFSegment crfSegment = new CRFSegment();
		PlainText plainText = ACECommon.getPlainText(files.get(fileIdx));
		String content = plainText.content;
		int idx = plainText.start - 1;
		for (String line : lines) {
			if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
				continue;
			}
			char c = line.charAt(0);
			idx = content.indexOf(c, idx + 1);
			if (idx == -1) {
				crfSegments.add(crfSegment);
				fileIdx++;
				plainText = ACECommon.getPlainText(files.get(fileIdx));
				content = plainText.content;
				idx = plainText.start - 1;
				idx = content.indexOf(c, idx + 1);
				crfSegment = new CRFSegment();
			}
			crfSegment.addOneLine(idx, line);
		}
		crfSegments.add(crfSegment);
		return crfSegments;
	}

	// create file of file
	public static void createFoF(String path, String files[]) {
		try {
			int p = path.lastIndexOf(File.separator);
			String prefix = path.substring(0, p);
			// System.out.println(path);
			FileWriter fw = new FileWriter(path);
			ArrayList<String> allLines = new ArrayList<String>();
			for (String file : files) {
				ArrayList<String> lines = Common.getLines(file);
				for (String str : lines) {
					int pos1 = str.lastIndexOf(File.separator);
					int pos2 = str.lastIndexOf('.');
					allLines.add(prefix + File.separator + str.substring(pos1 + 1, pos2));
				}
			}
			for (int i=0;i<allLines.size();i++) {
				String line = allLines.get(i);
				fw.write(line + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// find the position of one entity mention in one file
	public static int[] findParseFilePosition(EntityMention em, PlainText sgm, ArrayList<ParseResult> parseResults) {
		// sentenceIdx, startWordIdx, startCharIdx, endWordIdx, endCharIdx
		int position[] = new int[5];
		int start = em.getS();
		int end = em.getE();
		String content = sgm.content;
		int idx = -1;
		int startWordIdx = 0;
		int startCharIdx = 0;
		int endWordIdx = 0;
		int endCharIdx = 0;
		int sentenceIdx = 0;
		boolean find = false;
		for (int i = 0; i < parseResults.size(); i++) {
			ArrayList<String> words = parseResults.get(i).words;
			for (int j = 1; j < words.size(); j++) {
				String word = words.get(j);
				int k = 0;
				for (; k < word.length(); k++) {
					idx = content.indexOf(word.charAt(k), idx + 1);
					if (idx == start) {
						startWordIdx = j;
						startCharIdx = k;
					}
					if (idx == end) {
						endWordIdx = j;
						endCharIdx = k;
						find = true;
						// if(startWordIdx==endWordIdx && startCharIdx==0 &&
						// endCharIdx==word.length()) {
						// wholeWord = true;
						// }
						//						
						break;
					}
				}
				if (find) {
					break;
				}
			}
			if (find) {
				sentenceIdx = i;
				break;
			}
		}
		position[0] = sentenceIdx;
		position[1] = startWordIdx;
		position[2] = startCharIdx;
		position[3] = endWordIdx;
		position[4] = endCharIdx;
		return position;
	}

	// heuristic way to find extent
	public static void assingStartEnd(EntityMention em, CoNLLPart part) {
		CoNLLSentence sentence = null;
		for(CoNLLSentence temp : part.getCoNLLSentences()) {
			ArrayList<int[]> poses = temp.getPositions();
			if(em.headCharStart>=poses.get(0)[0] && em.headCharStart<=poses.get(poses.size()-1)[1]) {
				sentence = temp;
				break;
			}
		}
		em.sentenceID = sentence.getSentenceIdx();
		
		int charEnd = em.headCharEnd;
		int charStart = em.headCharStart;
		int endWordIdx = -1;
		for(int i=0;i<sentence.words.size();i++) {
			int[] position = sentence.positions.get(i);
			if(charEnd>=position[0] && charEnd<=position[1]) {
				endWordIdx = i;
				em.end = sentence.getWord(i).index;
				em.headStart = sentence.getWord(i).index;
				em.headEnd = sentence.getWord(i).index;
			}
			if(charStart>=position[0] && charStart<=position[1]) {
				em.start = sentence.getWord(i).index;
			}
		}
		// mention wrongly span two sentences
		if(endWordIdx==-1) {
			em.end = sentence.getWords().get(sentence.getWords().size()-1).index;
			em.headStart = sentence.getWords().get(sentence.getWords().size()-1).index;
			em.headEnd = sentence.getWords().get(sentence.getWords().size()-1).index;
		}
	} 

//	public static String getHeuristicExtent(EntityMention em, PlainText sgm, ArrayList<ParseResult> parseResults) {
//		String extent = "";
//		int[] position = findParseFilePosition(em, sgm, parseResults);
//		int sentenceIdx = position[0];
//		// int startWordIdx = position[1];
//		int endWordIdx = position[3];
//		ParseResult pr = parseResults.get(sentenceIdx);
//		MyTree tree = pr.tree;
//		MyTreeNode parent = tree.leaves.get(endWordIdx).parent;
//		// System.out.println(tree.leaves.get(endWordIdx).parent.value);
//		ArrayList<String> words = pr.words;
//		ArrayList<String> posTags = pr.posTags;
//		boolean hasVPNP = true;
//		if (parent.value.equalsIgnoreCase("NR")) {
//			return em.getHead().replace("\n", "").replace("\r", "").replace("\\s+", "");
//		}
//
//		if (parent.value.equalsIgnoreCase("NN") || parent.value.equalsIgnoreCase("NR")
//				|| parent.value.equalsIgnoreCase("M")) {
//			while (!parent.value.equalsIgnoreCase("NP")) {
//				parent = parent.parent;
//				if (parent == tree.root) {
//					hasVPNP = false;
//					break;
//				}
//			}
//		} else if (tree.leaves.get(endWordIdx).parent.value.equalsIgnoreCase("VV")) {
//			while (!parent.value.equalsIgnoreCase("VP")) {
//				parent = parent.parent;
//				if (parent == tree.root) {
//					hasVPNP = false;
//					break;
//				}
//			}
//		} else {
//			return em.head;
//		}
//		// System.out.println(parent.value);
//		if (hasVPNP) {
//			MyTreeNode firstChild = parent;
//			while (firstChild.children != null && firstChild.children.size() != 0) {
//				firstChild = firstChild.children.get(0);
//			}
//
//			MyTreeNode lastChild = parent;
//			while (lastChild.children != null && lastChild.children.size() != 0) {
//				lastChild = lastChild.children.get(lastChild.children.size() - 1);
//			}
//			if (lastChild.leafIdx == endWordIdx
//					&& lastChild.value.charAt(lastChild.value.length() - 1) == (em.head.charAt(em.head.length() - 1))) {
//				for (int i = endWordIdx; i >= firstChild.leafIdx; i--) {
//					if (posTags.get(i).equalsIgnoreCase("PU")) {
//						break;
//					}
//					extent = words.get(i) + extent;
//				}
//
//				int idx = firstChild.leafIdx - 1;
//				while (idx > 0) {
//					String previousPOS = posTags.get(idx);
//					if (previousPOS.equalsIgnoreCase("DT") || previousPOS.equalsIgnoreCase("JJ")
//							|| previousPOS.equals("NR") || previousPOS.equals("NN") || previousPOS.equals("M")
//							|| previousPOS.equals("CD") || previousPOS.equals("DEG") || previousPOS.equals("DEC")) {
//						extent = words.get(idx) + extent;
//						idx--;
//					} else {
//						break;
//					}
//				}
//
//				String head = em.getHead().replace("\n", "").replace("\r", "").replace("\\s+", "");
//				extent = extent.replace("\n", "").replace("\r", "").replace("\\s+", "");
//				if (extent.length() < head.length()) {
//					extent = head;
//				}
//			} else {
//				extent = em.getContent();
//			}
//		}
//		// System.out.println(sb.toString());
//
//		// System.out.println(sentenceIdx + " " + em.getContent() + " " +
//		// sb.toString());
//		// System.out.println(em.getContent() + "\t" + sb.toString());
//		return extent;
//	}




	public static MyTreeNode getNPMyTreeNode(EntityMention np, ArrayList<ParseResult> prs, int npSenIdx,
			int npWordStartIdx, int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = prs.get(npSenIdx).tree.leaves;
			MyTreeNode leftNp = leaves.get(npWordStartIdx);
			MyTreeNode rightNp = leaves.get(npWordEndIdx);
			// System.out.println(npWordEndIdx +np.getContent());
			ArrayList<MyTreeNode> leftAncestors = leftNp.getAncestors();
			ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
			for (int i = 0; i < leftAncestors.size() && i < rightAncestors.size(); i++) {
				if (leftAncestors.get(i) == rightAncestors.get(i)) {
					NP = leftAncestors.get(i);
				} else {
					break;
				}

			}
		} catch (Exception e) {
			System.out.println("ERROR when finding tree node");
			return null;
		}
		return NP;
	}

}
