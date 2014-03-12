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

public class Train {

	String folder;
	AnaphorFeatures aFea;

	public Train(String folder) {
		this.folder = folder;
		aFea = new AnaphorFeatures(true, folder);
		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train");
		ArrayList<String> lines = new ArrayList<String>();

		ChCommon ontoCommon = new ChCommon("chinese");
		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);

			CoNLLDocument document = new CoNLLDocument(conllFn);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				MentionDetect md = new ParseTreeMention();
				ArrayList<EntityMention> mentions = md.getMentions(part);
				Collections.sort(mentions);

				HashSet<String> anaphors = new HashSet<String>();
				ArrayList<Entity> goldEntities = part.getChains();
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
				}
			}
		}
		Common.outputLines(lines, "anaphorTrain." + folder);
		Common.outputHashMap(aFea.StringIndex, "anaphorStrFeaIdx." + folder);
	}

	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ folder");
			System.exit(1);
		}
		new Train(args[0]);
	}
}
