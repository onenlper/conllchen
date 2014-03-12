package Main;

import java.util.ArrayList;

import util.Common;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;

public class Main {
	public static void main(String args[]) {
		String fn = "/users/yzcchen/workspace/AndrewCoNLL/src/test.output2";
		
		CoNLLDocument document = new CoNLLDocument(fn);
		System.out.println(document.getParts().size());
		
		ArrayList<String> lines = new ArrayList<String>();
		
		for(CoNLLPart part : document.getParts()) {
			ArrayList<Entity> entities = part.getChains();
			for(Entity entity : entities) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention m : entity.mentions) {
					sb.append(m.source).append(" # ");
				}
				lines.add(sb.toString());
			}
			lines.add("================================");
		}
		Common.outputLines(lines, "entities.system");
		
		lines = new ArrayList<String>();
		String goldFn = "/users/yzcchen/workspace/CoNLL-2012/src/key.gold.english";
		CoNLLDocument goldDocument = new CoNLLDocument(goldFn);
		for(int i=0;i<goldDocument.getParts().size();i++) {
			CoNLLPart goldPart = goldDocument.getParts().get(i);
			CoNLLPart part = document.getParts().get(i);
			ArrayList<Entity> entities = goldPart.getChains();
			for(Entity entity : entities) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention m : entity.mentions) {
					for(int j = m.start;j<=m.end;j++) {
						sb.append(part.getWord(j).word).append(" ");
					}
					sb.append("# ");
				}
				lines.add(sb.toString());
			}
			lines.add("================================");
			
		}
		Common.outputLines(lines, "entities.gold");
	}
}
