package ace.event.coref;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;
import ace.ACECommon;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.rule.RuleCoref;

public class ErrorAnalysis {
	public static void main(String args[]) {
		if(args.length!=1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		String basePath = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_" +  args[0] + "/";
		ArrayList<String> allFiles2 = Common.getLines(basePath + "all.txt2");
		
		for(int i=0;i<allFiles2.size();i++) {
			String file = allFiles2.get(i);
			HashMap<String, Integer> goldClusterMap = new HashMap<String, Integer>();
			HashMap<String, Integer> systemClusterMap = new HashMap<String, Integer>();

			HashMap<String, EventMention> goldEventMap = new HashMap<String, EventMention>();
			
			ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(file);
			for (int k = 0; k < eventChains.size(); k++) {
				EventChain chain = eventChains.get(k);
				for (EventMention eventMention : chain.getEventMentions()) {
					String key = eventMention.headCharStart+","+eventMention.headCharEnd;
					goldClusterMap.put(key, k);
					goldEventMap.put(key, eventMention);
				}
			}
			
			ArrayList<String> systemLines = Common.getLines(basePath + Integer.toString(i) + ".entities.mp.event");
			for(int k = 0;k<systemLines.size();k++) {
				String line = systemLines.get(k);
				String tokens[] = line.split("\\s+");
				for(String token : tokens) {
					systemClusterMap.put(token, k);
				}
			}
			
			// precision error:
			for(int k = 0;k<systemLines.size();k++) {
				String line = systemLines.get(k);
				String tokens[] = line.split("\\s+");
				for(int j=0;j<tokens.length-1;j++) {
					String m1 = tokens[j];
					String m2 = tokens[j+1];
					
					if(goldClusterMap.get(m1).intValue()!=goldClusterMap.get(m2).intValue()) {
						EventMention em1 = goldEventMap.get(m1);
						EventMention em2 = goldEventMap.get(m2);
						if(em1.head.equals(em2.head)) {
							System.err.println(file);
							RuleCoref.printPair(em1, em2);
						}
					}
				}
			}
			
			// recall error:
			for (int k = 0; k < eventChains.size(); k++) {
				EventChain chain = eventChains.get(k);
				for (int j=0;j<chain.getEventMentions().size()-1;j++) {
					EventMention em1 = chain.getEventMentions().get(j);
					EventMention em2 = chain.getEventMentions().get(j+1);
					
					String m1 = em1.headCharStart+","+em1.headCharEnd;
					String m2 = em2.headCharStart+","+em2.headCharEnd;
					
					if(systemClusterMap.get(m1).intValue()!=systemClusterMap.get(m2).intValue()) {
//						if(em1.head.equals(em2.head))
//							RuleCoref.printPair(em1, em2);
					}
				}
			}
			
		}
	}
}
