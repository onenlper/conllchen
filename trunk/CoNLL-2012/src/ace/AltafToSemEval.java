//package altaf.emnlp11.coref;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.util.ArrayList;
//import java.util.Collections;
//
//import chen.util.Common;
//
///*
// * java ~ folder [mp|cr] 0_1_2_3_4
// */
//public class AltafToSemEval {
//	
//	// public static String basePath =
//	// "/users/yzcchen/ACL12/model/ACE/coref_test_predict"+File.separator;
//	static String basePath = "/users/yzcchen/chen3/ijcai13/data2/";
//	
//	public static void main(String args[]) throws Exception {
//		convert(args);
//	}
//
//	public static void convert(String[] args) throws Exception {
//		if(args.length<2) {
//			System.err.println("");
//			System.exit(1);
//		}
//		ArrayList<String> lines = Common.getLines(basePath + args[0]);
//		
//		for (int i = 0; i < lines.size(); i++) {
//			lines.set(i, basePath + lines.get(i).toLowerCase());
//		}
//		
//		outputSemFormat(lines, ".entities", "key.system");
//		outputSemFormat(lines, ".coref", "key.gold");
//	}
//
//	public static void outputSemFormat(ArrayList<String> files, String suffix, String outputPath) throws Exception {
//		FileWriter fw = new FileWriter(outputPath);
//		for (int k = 0; k < files.size(); k++) {
//			String line = files.get(k);
//			fw.write("#begin document " + line + "\n");
//			String raw = chen.util.Common.getFileContent(line + ".raw");
//			ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
//			for (int i = 0; i < raw.length(); i++) {
//				CRFElement element = new CRFElement();
//				elements.add(element);
//			}
//			// System.out.println(line);
//			String entityPath = basePath + suffix;
//			ArrayList<Entity> entities = loadEntities(entityPath);
//
//			for (int i = 0; i < entities.size(); i++) {
//				Entity en = entities.get(i);
//				for (EntityMention em : en.mentions) {
//					int start = em.start;
//					int end = em.end;
//
//					StringBuilder sb = new StringBuilder();
//					if (start == end) {
//						sb.append("(").append(i + 1).append(")");
//						elements.get(start).append(sb.toString());
//					} else {
//						elements.get(start).append("(" + Integer.toString(i + 1));
//						elements.get(end).append(Integer.toString(i + 1) + ")");
//					}
//				}
//			}
//			for (int i = 0; i < elements.size(); i++) {
//				CRFElement element = elements.get(i);
//				if (element.predict.isEmpty()) {
//					fw.write(Integer.toString(i + 1) + "	" + "_\n");
//				} else {
//					fw.write(Integer.toString(i + 1) + "	" + element.predict + "\n");
//				}
//			}
//
//			fw.write("#end document " + line + "\n");
//		}
//		fw.close();
//	}
//
//	public static class CRFElement {
//		String word;
//		String predict = "";
//
//		public void append(String str) {
//			if (predict.isEmpty()) {
//				this.predict = str;
//			} else {
//				this.predict = str + "|" + this.predict;
//			}
//		}
//
//	}
//
//	public static ArrayList<Entity> loadEntities(String iFileName) {
//		ArrayList<Entity> entities = new ArrayList<Entity>();
//		try {
//			BufferedReader input = new BufferedReader(new FileReader(iFileName));
//
//			String line;
//			while ((line = input.readLine()) != null) {
//				String[] mentions = line.split("\\s");
//
//				Entity anEntity = new Entity();
//				anEntity.mentions = new ArrayList<EntityMention>();
//
//				for (int i = 0; i < mentions.length; i++) {
//					if (!mentions[i].equals("")) {
//						String[] tokens = mentions[i].split(",");
//						anEntity.mentions.add(new EntityMention(Integer.parseInt(tokens[0]), Integer
//								.parseInt(tokens[1])));
//					}
//				}
//				Collections.sort(anEntity.mentions);
//				if (anEntity.mentions.size() > 0)
//					entities.add(anEntity);
//			}
//			input.close();
//		} catch (Exception e) {
//			System.err.println("Gotcha creating entities : " + e);
//		}
//		Collections.sort(entities);
//		return entities;
//	}
//}
