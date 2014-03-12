package ACL13.anaphoricity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;

public class Test {

	String folder;
	AnaphorFeatures aFea;

	public Test(String folder) {
		this.folder = folder;
		aFea = new AnaphorFeatures(false, folder);
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_test");
		ArrayList<String> lines = new ArrayList<String>();

		ChCommon ontoCommon = new ChCommon("chinese");
		ChCommon.loadPredictNE(folder, "test");
		ArrayList<String> mids = new ArrayList<String>();
		
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);

			CoNLLDocument document = new CoNLLDocument(conllFn
//					.replace("test", "test_gold")
					);
			
			CoNLLDocument goldDocument = new CoNLLDocument(conllFn.replace("test", "test_gold"));

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				CoNLLPart goldPart = goldDocument.getParts().get(k);
				
				MentionDetect md = new ParseTreeMention();
				ArrayList<EntityMention> mentions = md.getMentions(part);
				Collections.sort(mentions);

				HashSet<String> anaphors = new HashSet<String>();
				ArrayList<Entity> goldEntities = goldPart.getChains();
				for (Entity entity : goldEntities) {
					Collections.sort(entity.mentions);
					
					for(int i=1;i<entity.mentions.size();i++) {
						anaphors.add(entity.mentions.get(i).toName());
					}
				}

				for (int i = 0; i < mentions.size(); i++) {
					EntityMention m = mentions.get(i);
					ontoCommon.calAttribute(m, part);
					String feature = aFea.getFea(mentions.subList(0, i), m, 
							i+1<mentions.size()?mentions.subList(i + 1, mentions.size()):new ArrayList<EntityMention>(),
							part);

					StringBuilder sb = new StringBuilder();
					if (anaphors.contains(m.toName())) {
						sb.append("+1 ").append(feature);
					} else {
						sb.append("-1 ").append(feature);
					}
					lines.add(sb.toString());
					
					sb = new StringBuilder();
					sb.append(conllFn).append(" ").append(k).append(" ").append(m.toName());
					mids.add(sb.toString());
				}
			}
		}
		Common.outputLines(lines, "anaphorTest." + folder);
		
		Common.outputLines(mids, "mids." + folder);
	}

	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		new Test(args[0]);
	}
}
