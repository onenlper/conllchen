package machineLearning;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mentionDetect.GoldMention;
import mentionDetect.MentionDetect;
import mentionDetect.ParseTreeMention;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;

public abstract class ML {

	String basePath = "/users/yzcchen/chen2/conll12/";

	String outputFolder;

	ArrayList<String> fileList;

	ArrayList<CoNLLPart> parts;

	protected MentionDetect md;

	CorefFeature corefFeature;

	boolean train;

	String language;

	String folder;

	public ChCommon chCommon;

	public void config(MentionDetect md, CorefFeature corefFeature) {
		this.md = md;
		this.corefFeature = corefFeature;
	}

	public ML(String fileListFn) {
		if (!(new File(fileListFn)).exists()) {
			fileListFn = fileListFn.replace("v4", "v5");
		}
		this.fileList = Common.getLines(fileListFn);
		String tokens[] = fileListFn.split("_");
		this.language = tokens[0];
		this.chCommon = new ChCommon(this.language);
		this.folder = tokens[2];
		if (tokens[3].equalsIgnoreCase("train")) {
			this.train = true;
		} else {
			this.train = false;
		}
		this.outputFolder = basePath + this.language + File.separator + this.folder + "_" + tokens[3] + File.separator;
		if (!new File(this.outputFolder).exists()) {
			new File(this.outputFolder).mkdir();
		}

		this.parts = new ArrayList<CoNLLPart>();
		for (String filename : fileList) {
			CoNLLDocument document = new CoNLLDocument(filename);
			String documentID = document.getDocumentID();
			int a = documentID.indexOf(File.separator);
			this.folder = documentID.substring(0, a);
			parts.addAll(document.getParts());
		}
	}

	protected void createAllFile() {
		ArrayList<String> all = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all.add(this.outputFolder + part.getDocument().getDocumentID().replace("/", "-") + "_" + part.getPartID());
		}
		Common.outputLines(all, this.outputFolder + "all.txt");
		ArrayList<String> all2 = new ArrayList<String>();
		for (CoNLLPart part : parts) {
			all2.add(part.getDocument().getFilePath() + "_" + part.getPartID());
		}
		Common.outputLines(all2, this.outputFolder + "all.txt2");

	}

	public void creatAltafFormat() {
		String name = this.language + "_" + this.folder;
		this.corefFeature.init(train, name);
		this.createAllFile();
		for (CoNLLPart part : this.parts) {
			this.corefFeature.setPart(part);
			this.corefFeature.setSentences(part.getCoNLLSentences());
			System.out.println(part.getPartName());
			ArrayList<ArrayList<EntityMention>> chains = this.getMentions(part);
			ArrayList<EntityMention> mentions = this.createNPSpan(part, chains);
			this.assignFeature(mentions, part);
			ArrayList<EntityMention[]> pairs = this.createMentionPair(mentions);
			this.createFeature(part, pairs);
		}
		if (train) {
			Common.outputHashMap(this.corefFeature.stringFea1, name + "_stringFea1");
			Common.outputHashMap(this.corefFeature.stringFea2, name + "_stringFea2");
			Common.outputHashMap(this.corefFeature.stringFea3, name + "_stringFea3");
		}
	}

	protected void assignFeature(ArrayList<EntityMention> mentions, CoNLLPart part) {
		Collections.sort(mentions);
		for (EntityMention mention : mentions) {
			chCommon.calAttribute(mention, part);
		}
	}

	protected void createFeature(CoNLLPart part, ArrayList<EntityMention[]> pairs) {
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<String> apposLines = new ArrayList<String>();

		ArrayList<String> conflictLines = new ArrayList<String>();

		this.corefFeature.appoPairs.clear();
		boolean firstLone = true;
		boolean firstBi = true;
		for (EntityMention[] pair : pairs) {
			EntityMention current = pair[0];
			EntityMention candidate = pair[1];
			StringBuilder sb = new StringBuilder();
			List<Feature> feas;
			if (current.equals(candidate)) {
				feas = this.corefFeature.getLoneFeature(train, current);
				if (firstLone) {
					StringBuilder s = new StringBuilder();
					for (Feature fea : feas) {
						if (fea.limit != -1) {
							s.append(fea.limit).append(" ");
						}
					}
					lines.add(0, s.toString());
					firstLone = false;
				}
			} else {
				feas = this.corefFeature.getBilateralFea(train, pair);
				if (firstBi) {
					StringBuilder s = new StringBuilder();
					for (Feature fea : feas) {
						if (fea.limit != -1) {
							s.append(fea.limit).append(" ");
						}
					}
					if (firstLone) {
						lines.add(0, s.toString());
					} else {
						lines.add(1, s.toString());
					}
					firstBi = false;
				}

				boolean validate = this.corefFeature.validate(pair[1], pair[0]);
				if (!validate) {
					conflictLines.add(candidate.start + "," + candidate.end + "," + current.start + "," + current.end);
				}
			}
			sb.append(current.start).append(",").append(current.end).append(",").append(candidate.start).append(",")
					.append(candidate.end).append(" ");
			for (Feature fea : feas) {
				sb.append(",").append(fea.index);
			}
			lines.add(sb.toString());
		}
		for (EntityMention name : this.corefFeature.appoPairs.keySet()) {
			StringBuilder appSb = new StringBuilder();
			EntityMention title = this.corefFeature.appoPairs.get(name);
			appSb.append(title.start).append(",").append(title.end).append(" ").append(name.start).append(",").append(
					name.end);
			apposLines.add(appSb.toString());
		}
		Common.outputLines(lines, this.outputFolder + part.getPartName() + ".feat");
		Common.outputLines(apposLines, this.outputFolder + part.getPartName() + ".appos");
		Common.outputLines(conflictLines, this.outputFolder + part.getPartName() + ".conflict");

		System.out.println(this.outputFolder + part.getPartName() + ".feat");
	}

	protected ArrayList<EntityMention[]> createMentionPair(ArrayList<EntityMention> mentions) {
		Collections.sort(mentions);
		ArrayList<EntityMention[]> pairs = new ArrayList<EntityMention[]>();

		for (int i = 0; i < mentions.size(); i++) {
			EntityMention currentMention = mentions.get(i);
			for (int j = i; j >= 0; j--) {
				EntityMention candidateMention = mentions.get(j);
				EntityMention[] mentionPair = new EntityMention[2];
				mentionPair[0] = currentMention;
				mentionPair[1] = candidateMention;
				pairs.add(mentionPair);
			}
		}

		return pairs;
	}

	protected ArrayList<EntityMention> createNPSpan(CoNLLPart part, ArrayList<ArrayList<EntityMention>> chains) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<String> lines = new ArrayList<String>();
		for (ArrayList<EntityMention> chain : chains) {
			StringBuilder sb = new StringBuilder();
			for (EntityMention mention : chain) {
				sb.append(mention.start).append(",").append(mention.end).append(" ");
			}
			lines.add(sb.toString().trim());
			mentions.addAll(chain);
		}
		Collections.sort(mentions);
		Common.outputLines(lines, this.outputFolder + part.getPartName() + ".npspan");
		return mentions;
	}

	ArrayList<Entity> goldChain;

	public void loadGoldMaps() {
		for (Entity entity : this.goldChain) {
			for (EntityMention em : entity.mentions) {
				HashSet<EntityMention> ems = new HashSet<EntityMention>();
				for (EntityMention em2 : entity.mentions) {
					if (!em.equals(em2)) {
						ems.add(em2);
					}
				}
				goldMaps.put(em, ems);
			}
		}
	}

	public HashMap<EntityMention, HashSet<EntityMention>> goldMaps = new HashMap<EntityMention, HashSet<EntityMention>>();

	public ArrayList<ArrayList<EntityMention>> getMentions(CoNLLPart part) {
		this.goldChain = part.getChains();
		loadGoldMaps();
		if (this.md == null || this.md instanceof GoldMention) {
			ArrayList<ArrayList<EntityMention>> chains = new ArrayList<ArrayList<EntityMention>>();

			// add singleton mentions
			HashSet<EntityMention> entityMentions = new HashSet<EntityMention>();

			ArrayList<Entity> entities = part.getChains();
			for (Entity entity : entities) {
				ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
				mentions.addAll(entity.mentions);
				entityMentions.addAll(mentions);
				chains.add(mentions);
			}

			ArrayList<EntityMention> treeMentions = (new ParseTreeMention()).getMentions(part);
			for (EntityMention mention : treeMentions) {
				if (entityMentions.contains(mention)) {
					continue;
				}
				ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
				chain.add(mention);
				chains.add(chain);
			}
			return chains;
		} else {
			ArrayList<ArrayList<EntityMention>> chains = new ArrayList<ArrayList<EntityMention>>();
			ArrayList<EntityMention> mentions = this.md.getMentions(part);
			for (EntityMention mention : mentions) {
				ArrayList<EntityMention> chain = new ArrayList<EntityMention>();
				chain.add(mention);
				chains.add(chain);
			}
			return chains;
		}
	}

	public static void main(String args[]) {

	}
}
