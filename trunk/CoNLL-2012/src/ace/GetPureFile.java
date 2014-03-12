package ace;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;

import model.Entity;
import model.EntityMention;
import util.Common;
import ace.model.EventChain;
import ace.model.EventMention;

/*
 * output golden coreference chain
 * java ~ folder 0_1_2_3_4
 */
public class GetPureFile {
	
	static boolean entity = false;
	static boolean event = false;
	static String mode;
	public static void main(String args[]) throws Exception{
		if(args.length!=3) {
			System.err.println("java ~ /users/yzcchen/chen3/conll12/chinese/ACE/ 0 [entity|event|both]");
		}
		String baseFolder = args[0];
		String tokens[] = args[1].split("_");
		if(args[2].equals("entity")) {
			entity = true;
		} else if(args[2].equals("event")) {
			event = true;
		} else {
			entity = true;
			event = true;
		}
		mode = args[2];
		ArrayList<String> lines = new ArrayList<String>();
		for(String token:tokens) {
			if(token.equals("0")) {
				lines.addAll(Common.getLines("ACE_0"));
			} else if(token.equals("1")) {
				lines.addAll(Common.getLines("ACE_1"));
			} else if(token.equals("2")) {
				lines.addAll(Common.getLines("ACE_2"));
			} else if(token.equals("3")) {
				lines.addAll(Common.getLines("ACE_3"));
			} else if(token.equals("4")) {
				lines.addAll(Common.getLines("ACE_4"));
			}
		}
		
		ArrayList<String> files = ACECommon.getFileList(tokens);
		for(int i=0;i<files.size();i++) {
			String file = files.get(i);
			String output = baseFolder+Integer.toString(i)+".entities.golden." + mode;
//			System.out.println(output);
			FileWriter fw = new FileWriter(output);
			String apfFn = ACECommon.getRelateApf(file);
			if(entity) {
				ArrayList<Entity> entities = new ArrayList<Entity>();
				entities.addAll(ACECommon.getEntities(apfFn));
				Collections.sort(entities);
				for(Entity entity:entities) {
					ArrayList<EntityMention> ems = entity.mentions;
					Collections.sort(ems);
					StringBuilder sb = new StringBuilder();
					for(EntityMention em:ems) {
						sb.append(em.headCharStart).append(",").append(em.headCharEnd).append(" ");
					}
					fw.write(sb.toString()+"\n");
				}
			}
			if(event) {
				ArrayList<EventChain> eventChains = ACECommon.readGoldEventChain(file);
				for(EventChain chain : eventChains) {
					StringBuilder sb = new StringBuilder();
					for(EventMention em : chain.getEventMentions()) {
						sb.append(em.headCharStart).append(",").append(em.headCharEnd).append(" ");
					}
					fw.write(sb.toString()+"\n");
				}
			}
			fw.close();
		}
	}
}
