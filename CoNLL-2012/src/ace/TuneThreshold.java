//package ace;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import ace.model.EventMention;
//
//import util.Common;
//
//public class TuneThreshold {
//	
//	static HashMap<String, HashMap<String, EventMention>> allMaps;
//	
//	public static void loadAllEvents() {
//		allMaps = new HashMap<String, HashMap<String, EventMention>>();
//		for(int i=0;i<5;i++) {
//			Common.part = Integer.toString(i);
//			HashMap<String, HashMap<String, EventMention>> maps = ACECommon.getSystemEventMentions();
//			allMaps.putAll(maps);
//		}
//	}
//	
//	public static void main(String args[]) throws Exception {
//		if(args.length!=1) {
//			System.err.println("java ~ threshold");
//			System.exit(1);
//		}
//		ACECommon.svmTh = Double.parseDouble(args[0]);
//		loadAllEvents();
//		
//		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/ACE_test_";
//		ArrayList<String> allKeys = new ArrayList<String>();
//		for(int i=0;i<5;i++) {
//			Common.part = Integer.toString(i);
//			ArrayList<String> allLines = Common.getLines(baseFolder + Integer.toString(i) + "/all.txt");
//			ArrayList<String> allLines2 = Common.getLines(baseFolder + Integer.toString(i) + "/all.txt2");
//			for(int k=0;k<allLines.size();k++) {
//				ArrayList<String> entities = Common.getLines(baseFolder + Integer.toString(i) + "/" + k + ".entities.sieve.event");
//				ArrayList<String> newEntities = new ArrayList<String>();
//				HashMap<String, EventMention> allEvents = allMaps.get(allLines2.get(k));
//				if (allEvents == null) {
//					allEvents = new HashMap<String, EventMention>();
//				}
//				for(String entity : entities) {
//					String tokens[] = entity.split("\\s+");
//					StringBuilder sb = new StringBuilder();
//					
//					for(String mention : tokens) {
//						if(allEvents.containsKey(mention)) {
//							sb.append(mention).append(" ");
//						}
//					}
//					if(!sb.toString().trim().isEmpty()) {
//						newEntities.add(sb.toString().trim());
//					}
//				}
//				Common.outputLines(newEntities, baseFolder + Integer.toString(i) + "/" + k + ".entities.yy.event");
//			}
//			
//			String altafArg[] = new String[4];
//			altafArg[0] = baseFolder + Integer.toString(i) + "/";
//			altafArg[1] = Integer.toString(i);
//			altafArg[2] = "yy";
//			altafArg[3] = "event";
//			AltafToSemEval.convert(altafArg);
//
//			allKeys.addAll(Common.getLines(baseFolder + Integer.toString(i) + "/key.yy.event"));
//		}
//		Common.outputLines(allKeys, "/users/yzcchen/chen3/conll12/chinese/key.yy.event");
//	}
//}
