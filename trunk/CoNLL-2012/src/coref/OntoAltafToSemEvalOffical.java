package coref;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;

// java ~ /users/yzcchen/ACL12/model/ACE/coref_test_predict/ mp
/*
 * not zero pronoun chain
 */
public class OntoAltafToSemEvalOffical {
	
	public static String basePath;
	public static String model;
	public static void main(String args[]) throws Exception{
		runOutputKey(args);
	}

	public static void runOutputKey(String[] args) throws Exception {
		if(args.length<2) {
			System.out.println("java ~ /users/yzcchen/conll12/chinese/train/ mp");
			return;
		}
		int a = args[0].lastIndexOf(File.separator);
		basePath = args[0].substring(0, a+1);
		ArrayList<String> lines = Common.getLines(args[0] + File.separator + "all.txt");
		ArrayList<String> lines2 = Common.getLines(args[0] + File.separator + "all.txt2");
		model = args[1];
		outputSemFormat(lines,lines2, "entities." + model, basePath + "key." + model, basePath + "key.gold");
	}
	
	public static void outputSemFormat(ArrayList<String> files, ArrayList<String> lines2, String suffix, String outputPath, String goldOutputPath) throws Exception {
		FileWriter systemKeyFw = new FileWriter(outputPath);
//		FileWriter goldKeyFw = new FileWriter(goldOutputPath);
		ArrayList<String> goldKey2 = new ArrayList<String>();
		HashSet<String> processedFile = new HashSet<String>();
		for (int i=0;i<files.size();i++) {
			String line = files.get(i);
			String line2 = lines2.get(i);
			int a = line2.lastIndexOf("_");
			int partIdx = Integer.valueOf(line2.substring(a+1));
			String conllFn = line2.substring(0, a);
			if(!processedFile.contains(conllFn)) {
				if(!(new File(conllFn)).exists()) {
					conllFn = conllFn.replace("v5", "v4");
				}
				String goldKeyFile = conllFn.replace("/test/", "/test_gold/").split("\\.")[0] + ".v4_gold_skel";
				if(!(new File(goldKeyFile)).exists()) {
					goldKeyFile = goldKeyFile.replace("v6", "v4");
				}
				goldKey2.addAll(Common.getLines(goldKeyFile));
				processedFile.add(conllFn);
			}
			CoNLLDocument document = new CoNLLDocument(conllFn);
			CoNLLPart part = document.getParts().get(partIdx);
//			System.out.println(line);
			int pos2 = line.lastIndexOf(File.separator);
			String systemEntityPath = basePath + File.separator + line.substring(pos2+1)+"."+suffix;
			ArrayList<Entity> systemChain = loadEntities(systemEntityPath);
			String goldEntityPath = basePath + File.separator + line.substring(pos2+1)+".entities.gold";
			ArrayList<Entity> goldChain = loadEntities(goldEntityPath);
			String beginLine = part.label;
			writerKey(systemKeyFw, systemChain, beginLine, part);
//			writerKey(goldKeyFw, goldChain, beginLine, part);
			explainChain(part, goldChain, basePath + File.separator + line.substring(pos2+1) + ".chain.gold");
			explainChain(part, systemChain, basePath + File.separator + line.substring(pos2+1) + ".chain." + model);
		}
		Common.outputLines(goldKey2, basePath + File.separator + "key2.gold");
		systemKeyFw.close();
//		goldKeyFw.close();
	}
	
	private static void explainChain(CoNLLPart part, ArrayList<Entity> entities, String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for(Entity entity : entities) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention em : entity.mentions) {
					sb.append(em.start).append(",").append(em.end).append(" ");
					for(int i=em.start;i<=em.end;i++) {
						sb.append(part.getWord(i).word).append(" ");
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
	
	public static void writerKey(FileWriter systemKeyFw, ArrayList<Entity> systemChain, String line, CoNLLPart part) throws IOException {
		HashSet<Integer> sentenceEnds = new HashSet<Integer>();
		for(CoNLLSentence sentence : part.getCoNLLSentences()) {
			sentenceEnds.add(sentence.getEndWordIdx());
		}
		
		systemKeyFw.write(line + "\n");
		ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
		int maxWord = part.getWordCount();
		for(int i=0;i<maxWord;i++) {
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
			String sourceLine = part.getWord(i).sourceLine;
			if(element.predict.isEmpty()) {
				systemKeyFw.write(sourceLine + "	" + "-\n");
			} else {
				systemKeyFw.write(sourceLine + "	" +element.predict + "\n");
			}
			if(sentenceEnds.contains(i)) {
				systemKeyFw.write("\n");
			}
		}
		systemKeyFw.write("#end document\n");
	}
	
	public static class CRFElement {
		String word;
		public String predict = "";
		
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
					if (!mentions[i].equals("")) {
						String[] tokens = mentions[i].split(",");
						anEntity.mentions.add(new EntityMention(Integer
								.parseInt(tokens[0]), Integer
								.parseInt(tokens[1])));
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
