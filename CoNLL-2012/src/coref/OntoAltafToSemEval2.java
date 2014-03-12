package coref;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;

// java ~ /users/yzcchen/ACL12/model/ACE/coref_test_predict/ mp
/*
 */
public class OntoAltafToSemEval2 {
	
	public static String basePath;
	public static String model;
	public static void main(String args[]) throws Exception{
		if(args.length<2) {
			System.out.println("java ~ /users/yzcchen/conll12/chinese/train/ mp");
			return;
		}
		appoStrs = new HashSet<String>();
		int a = args[0].lastIndexOf(File.separator);
		basePath = args[0].substring(0, a+1);
		ArrayList<String> lines1 = Common.getLines(args[0] + File.separator + "all.txt");
		ArrayList<String> lines2 = Common.getLines(args[0] + File.separator + "all2.txt");
		HashMap<String, ArrayList<String>> lines3 = createLines3(lines1, lines2);
		model = args[1];
		outputSemFormat(lines3, "entities." + model, basePath + "key." + model, basePath + "key.gold");
	}
	
	// create a mapping from CONLL file to Altaf format file
	public static HashMap<String, ArrayList<String>> createLines3(ArrayList<String> lines1, ArrayList<String> lines2) {
		HashMap<String, ArrayList<String>> lines3 =new HashMap<String, ArrayList<String>>();
		for(String line2 : lines2) {
			int a = line2.lastIndexOf("_");
			String conllFn = line2.substring(0, a);
			lines3.put(conllFn, new ArrayList<String>());
		}
		for(String line1 : lines1) {
			int a = line1.lastIndexOf("_");
			int b = line1.lastIndexOf(File.separator);
			String documentID = line1.substring(b+1, a).replace("-", File.separator);
			for(String line3: lines3.keySet()) {
				if(line3.contains(documentID)) {
					lines3.get(line3).add(line1);
				}
			}
		}
		return lines3;
	}
	
	public static void outputSemFormat(HashMap<String, ArrayList<String>> lines3, String suffix, String outputPath, String goldOutputPath) throws Exception {
		ArrayList<String> allGoldLines = new ArrayList<String>();
		ArrayList<String> allPredictLines = new ArrayList<String>();
		FileWriter systemKeyFw = new FileWriter(outputPath);
		FileWriter goldKeyFw = new FileWriter(goldOutputPath);
		
		for (String conllFn : lines3.keySet()) {
			CoNLLDocument document = new CoNLLDocument(conllFn);
			ArrayList<String> goldLines = document.getRawLines();
			allGoldLines.addAll(goldLines);
			System.out.println("ConllFn:" + conllFn);
			for(String line : lines3.get(conllFn)) {
				System.out.println("Line: " + line);
				int a = line.lastIndexOf("_");
				int partIdx = Integer.valueOf(line.substring(a+1));
				CoNLLPart part = document.getParts().get(partIdx);
				appoStrs.clear();
				System.out.println(line);
				int pos2 = line.lastIndexOf(File.separator);
				loadAppos(basePath + File.separator + line.substring(pos2+1)+".appos");
				String systemEntityPath = basePath + File.separator + line.substring(pos2+1)+"."+suffix;
				ArrayList<Entity> systemChain = loadEntities(systemEntityPath);
				String goldEntityPath = basePath + File.separator + line.substring(pos2+1)+".entities.gold";
				ArrayList<Entity> goldChain = loadEntities(goldEntityPath);
				writerKey(systemKeyFw, systemChain, line);
				writerKey(goldKeyFw, goldChain, line);
				maxWord = 0;
				explainChain(part, goldChain, basePath + File.separator + line.substring(pos2+1) + ".chain.gold");
				explainChain(part, systemChain, basePath + File.separator + line.substring(pos2+1) + ".chain." + model);
			}
		}
		systemKeyFw.close();
		goldKeyFw.close();
	}
	
	private static void explainChain(CoNLLPart part, ArrayList<Entity> entities, String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for(Entity entity : entities) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention em : entity.mentions) {
					sb.append(em.start).append(",").append(em.end).append(" ");
					for(int i=em.start;i<=em.end;i++) {
						sb.append(part.getWord(i).word);
					}
					sb.append("#");
				}
				fw.write(sb.toString() + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void loadAppos(String path) {
		ArrayList<String> lines = Common.getLines(path);
		for(String line : lines) {
			String tokens[] = line.split("\\s+");
			for(String token : tokens) {
				appoStrs.add(token);
			}
		}
	}

	public static HashSet<String> appoStrs;

	private static void writerKey(FileWriter systemKeyFw, ArrayList<Entity> systemChain, String line) throws IOException {
		systemKeyFw.write("#begin document " + line + "\n");
		ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
		for(int i=0;i<=maxWord;i++) {
			elements.add(new CRFElement());
		}
		for(int i=0;i<systemChain.size();i++) {
			Entity en = systemChain.get(i);
			for(EntityMention em:en.mentions) {
				int start = em.start;
				int end = em.end;
				
				StringBuilder sb = new StringBuilder();
				if(start==end) {
					sb.append("(").append(i+1).append(")");
					elements.get(start).append(sb.toString());
				} else {
					elements.get(start).append("("+Integer.toString(i+1));
					elements.get(end).append(Integer.toString(i+1) + ")");
				}
			}
		}
		for(int i=0;i<elements.size();i++) {
			CRFElement element = elements.get(i);
			if(element.predict.isEmpty()) {
				systemKeyFw.write(Integer.toString(i+1) + "	" + "_\n");
			} else {
				systemKeyFw.write(Integer.toString(i+1) + "	" +element.predict + "\n");
			}
		}
		systemKeyFw.write("#end document " + line + "\n");
	}
	
	static int maxWord;
	
	public static class CRFElement {
		String word;
		String predict = "";
		
		public void append(String str) {
			if(predict.isEmpty()) {
				this.predict = str;
			} else {
				this.predict = str + "|" + this.predict;
			}
		}
	}

	public static ArrayList<Entity> loadEntities(String iFileName) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		try {
			BufferedReader input = new BufferedReader(new FileReader(iFileName));

			String line;
			while ((line = input.readLine()) != null) {
				String[] mentions = line.split("\\s");
				// remove singleton
				if(mentions.length<2) {
					continue;
				}
				Entity anEntity = new Entity();
				anEntity.mentions = new ArrayList<EntityMention>();

				for (int i = 0; i < mentions.length; i++) {
					if(appoStrs.contains(mentions[i])) {
						continue;
					}
					if (!mentions[i].equals("")) {
						String[] tokens = mentions[i].split(",");
						anEntity.mentions.add(new EntityMention(Integer
								.parseInt(tokens[0]), Integer
								.parseInt(tokens[1])));
						if(Integer.parseInt(tokens[1])>maxWord) {
							maxWord = Integer.parseInt(tokens[1]);
						}
					}
				}
				Collections.sort(anEntity.mentions);
				if (anEntity.mentions.size() > 0)
					entities.add(anEntity);
			}
			input.close();
		} catch (Exception e) {
			System.err.println("Gotcha creating entities : " + e);
		}
		Collections.sort(entities);
		return entities;
	}
}
