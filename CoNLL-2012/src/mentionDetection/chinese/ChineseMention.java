package mentionDetection.chinese;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import mentionDetect.GoldMentionTest;
import mentionDetect.MentionDetect;
import model.Element;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.ChCommon;

public class ChineseMention {

	private ChCommon chCommon;

	public ChineseMention() {
		this.chCommon = new ChCommon("chinese");
	}

	public static boolean goldNE = false;

	public ArrayList<EntityMention> getChineseMention(CoNLLPart part) {
		if (part.getDocument().getFilePath().contains("development")
				|| part.getDocument().getFilePath().contains("test")) {
			if (ruleCoreference.chinese.RuleCoref.open) {
				part.setNameEntities(getChNE(part));
				// part.setNameEntities(getChGoldNE(part));
			}
			if (goldNE) {
				part.setNameEntities(getChGoldNE(part));
			}
		}
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		// setSystemParseTree(part);
		mentions.addAll(this.getChNPMention(part));
		// setGoldParseTree(part);

		// addSamewordEM(part, mentions);

		removeDuplicateMentions(mentions);
		pruneChMentions(mentions, part);

		return mentions;
	}

	private void addSamewordEM(CoNLLPart part, ArrayList<EntityMention> mentions) {
		HashMap<String, Integer> words = new HashMap<String, Integer>();
		for (EntityMention predict : mentions) {
			if (ChCommon.dict.allPronouns.contains(predict.source) || predict.source.length() == 1) {
				continue;
			}
			words.put(predict.source, predict.source.split("\\s+").length);
			// words.put(part.getWord(predict.end).word, 1);
		}
		HashSet<EntityMention> mentionSet = new HashSet<EntityMention>();
		mentionSet.addAll(mentions);
		// add more mentions here
		for (CoNLLSentence sentence : part.getCoNLLSentences()) {
			for (int i = 0; i < sentence.getWords().size(); i++) {
				String text = sentence.getText(i);
				for (String word : words.keySet()) {
					if (text.startsWith(word)) {
						EntityMention em = new EntityMention();
						em.start = sentence.getWords().get(i).index;
						em.end = em.start + words.get(word).intValue() - 1;
						em.source = word;
						em.original = word;
						mentionSet.add(em);
						em.tag = true;
					}
				}
			}
		}
		mentions.clear();
		mentions.addAll(mentionSet);
	}

	HashMap<String, ArrayList<Element>> EMs;

	private void setGoldParseTree(CoNLLPart part) {
		String documentID = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/test/data/chinese/annotations/"
				+ part.getDocument().getDocumentID() + ".v4_auto_conll";
		System.out.println(documentID);
		CoNLLDocument document = new CoNLLDocument(documentID);
		CoNLLPart goldPart = document.getParts().get(part.getPartID());

		for (int i = 0; i < goldPart.getCoNLLSentences().size(); i++) {
			CoNLLSentence goldSentence = goldPart.getCoNLLSentences().get(i);
			part.getCoNLLSentences().get(i).setSyntaxTree(goldSentence.getSyntaxTree());
		}
	}

	private void setSystemParseTree(CoNLLPart part) {
		String documentID = "/users/yzcchen/chen3/CoNLL/conll-2012_system_parse/v4/data/test/data/chinese/annotations/"
				+ part.getDocument().getDocumentID() + ".v5_auto_conll";
		System.out.println(documentID);
		CoNLLDocument document = new CoNLLDocument(documentID);
		CoNLLPart goldPart = document.getParts().get(part.getPartID());

		for (int i = 0; i < goldPart.getCoNLLSentences().size(); i++) {
			CoNLLSentence goldSentence = goldPart.getCoNLLSentences().get(i);
			part.getCoNLLSentences().get(i).setSyntaxTree(goldSentence.getSyntaxTree());
		}
	}

	private ArrayList<Element> getChNE(CoNLLPart part) {
		String key = part.getDocument().getDocumentID() + "_" + part.getPartID();
		ArrayList<Element> elements = ChCommon.predictNEs.get(key);
		if (elements == null) {
			elements = new ArrayList<Element>();
		}
		for (Element element : elements) {
			for (int i = element.start; i <= element.end; i++) {
				part.getWord(i).setRawNamedEntity(element.content);
			}
		}
		return elements;
	}

	private ArrayList<Element> getChGoldNE(CoNLLPart part) {
		String documentID = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/test_gold/data/chinese/annotations/"
				+ part.getDocument().getDocumentID() + ".v4_gold_skel";
		System.out.println(documentID);
		CoNLLDocument document = new CoNLLDocument(documentID);
		CoNLLPart goldPart = document.getParts().get(part.getPartID());

		for (Element ner : goldPart.getNameEntities()) {
			int start = ner.start;
			int end = ner.end;
			String ne = ner.content;

			StringBuilder sb = new StringBuilder();
			for (int k = start; k <= end; k++) {
				sb.append(part.getWord(k).word).append(" ");
			}
			// System.out.println(sb.toString() + " # " + ne);

			// System.out.println(goldPart.);
		}

		return goldPart.getNameEntities();
	}

	private ArrayList<EntityMention> getChNamedEntityMention(CoNLLPart part) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<Element> namedEntities = part.getNameEntities();
		System.out.println(part.getDocument().getDocumentID());
		for (Element element : namedEntities) {
			if (element.content.equalsIgnoreCase("QUANTITY") || element.content.equalsIgnoreCase("CARDINAL")
					|| element.content.equalsIgnoreCase("PERCENT")) {
				continue;
			}
			int end = element.end;
			int start = element.start;

			EntityMention mention = new EntityMention(start, end);
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			mention.source = sb.toString().trim().toLowerCase();
			mention.original = sb2.toString().trim();
			if (!mentions.contains(mention)) {
				mentions.add(mention);
			}
		}
		return mentions;
	}

	private ArrayList<EntityMention> getChNPMention(CoNLLPart part) {
		ArrayList<EntityMention> npMentions = chCommon.getAllNounPhrase(part.getCoNLLSentences());

		// MentionDetect md = new GoldBoundaryMentionTest();
		// npMentions = md.getMentions(part);

		// Gold Mention
		// MentionDetect md = new GoldMentionTest();
		// npMentions = md.getMentions(part);

		for (int g = 0; g < npMentions.size(); g++) {
			EntityMention npMention = npMentions.get(g);
			int end = npMention.end;
			int start = npMention.start;
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			npMention.source = sb.toString().trim().toLowerCase();
			npMention.original = sb2.toString().trim();
			// for (Element NE : part.getNameEntities()) {
			// if (NE.content.equalsIgnoreCase("QUANTITY") ||
			// NE.content.equalsIgnoreCase("QUANTITY")
			// || NE.content.equalsIgnoreCase("PERCENT")) {
			// continue;
			// }
			// if (npMention.start >= NE.start && npMention.end <= NE.end) {
			// npMentions.remove(g);
			// g--;
			// break;
			// }
			// }
		}
		return npMentions;
	}

	private void removeDuplicateMentions(ArrayList<EntityMention> mentions) {
		HashSet<EntityMention> mentionsHash = new HashSet<EntityMention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}

	private void assignNE(ArrayList<EntityMention> mentions, CoNLLPart part) {
		for (EntityMention mention : mentions) {
			int headStart = mention.headStart;
			for (Element element : part.getNameEntities()) {
				if (element.start <= headStart && headStart <= element.end) {
					// if (headStart == element.end) {
					mention.ner = element.content;
					// System.out.println(mention.source + " : " + mention.ner);
				}
			}
		}
	}

	private void goldPruneChMentions(ArrayList<EntityMention> mentions, CoNLLPart part) {
		// TODO
		MentionDetect md = new GoldMentionTest();
		HashSet<EntityMention> goldEMs = new HashSet<EntityMention>();
		goldEMs.addAll(md.getMentions(part));
		ArrayList<EntityMention> removes = new ArrayList<EntityMention>();
		for (EntityMention mention : mentions) {
			if (!goldEMs.contains(mention)) {
				removes.add(mention);
			}
		}
		mentions.removeAll(removes);
	}

	private void pruneChMentions(ArrayList<EntityMention> mentions, CoNLLPart part) {
		for (EntityMention mention : mentions) {
			this.chCommon.calChAttribute(mention, part);
		}
		assignNE(mentions, part);
		ArrayList<EntityMention> removes = new ArrayList<EntityMention>();
		Collections.sort(mentions);
		ArrayList<EntityMention> copyMentions = new ArrayList<EntityMention>(mentions.size());
		copyMentions.addAll(mentions);
		// goldPrune
		// this.goldPruneChMentions(mentions, part);
		//		

		for (int i = 0; i < mentions.size(); i++) {
			EntityMention mention = mentions.get(i);
			for (int j = 0; j < i; j++) {
				EntityMention antecedent = mentions.get(j);
				if (this.chCommon.isRoleAppositive(antecedent, mention, part)) {
					mention.roleSet.add(antecedent);
					antecedent.roleSet.add(mention);
				}
			}
		}
		for (int i = 0; i < mentions.size(); i++) {
			EntityMention em = mentions.get(i);
			for (int j = 0; j < copyMentions.size(); j++) {
				EntityMention em2 = copyMentions.get(j);
				if (em.end == em2.end && (em.end - em.start < em2.end - em2.start)) {
					if (!part.label.contains("nw/") && em.roleSet.size() == 0) {
//						if (em.start > 0 && part.getWord(em.start - 1).posTag.equalsIgnoreCase("CC")) {
//							continue;
//						}
						removes.add(em);
						break;
					}
				}
			}
		}

		mentions.removeAll(removes);
		removes.clear();

		for (int i = 0; i < mentions.size(); i++) {
			EntityMention mention = mentions.get(i);
			mention.position = chCommon.getPosition(mention, part.getCoNLLSentences());
			if (mention.ner.equalsIgnoreCase("QUANTITY") || mention.ner.equalsIgnoreCase("CARDINAL")
					|| mention.ner.equalsIgnoreCase("PERCENT") || mention.ner.equalsIgnoreCase("MONEY")) {
				removes.add(mention);
				continue;
			}

			if (mention.original.equalsIgnoreCase("我") && (mention.end + 2) < part.getWordCount()
					&& part.getWord(mention.end + 1).word.equals("啊") && part.getWord(mention.end + 2).word.equals("，")) {
				removes.add(mention);
				continue;
			}

			if (this.chCommon.getChDictionary().removeChars.contains(mention.head)) {
				removes.add(mention);
				continue;
			}

			// 没 问题
			if (mention.source.equalsIgnoreCase("问题") && mention.start > 0
					&& part.getWord(mention.start - 1).word.equals("没")) {
				removes.add(mention);
				continue;
			}

			// 你 知道
			if (mention.source.equalsIgnoreCase("你") && mention.start > 0
					&& part.getWord(mention.start + 1).word.equals("知道")) {
				removes.add(mention);
				continue;
			}

			// 
			if (mention.source.contains("什么") || mention.source.contains("多少")) {
				removes.add(mention);
				continue;
			}
			String lastWord = part.getWord(mention.end).word;
			if (mention.source.endsWith("的")
					|| (mention.source.endsWith("人") && mention.start == mention.end && this.chCommon.getChDictionary().countries
							.contains(lastWord.substring(0, lastWord.length() - 1)))) {
				removes.add(mention);
				continue;
			}
			// ｑｕｏｔ
			if (this.chCommon.getChDictionary().removeWords.contains(mention.source)) {
				removes.add(mention);
				continue;
			}
			// if (ruleCoreference.chinese.RuleCoref.tuneSwitch) {
			String head = mention.head.toLowerCase();
			if (ruleCoreference.chinese.RuleCoref.mention_stats.containsKey(head)) {
				if (ruleCoreference.chinese.RuleCoref.mention_stats.get(head) < ruleCoreference.chinese.RuleCoref.t5) {
					removes.add(mention);
					if (part.getMentions().contains(mention)) {
						System.out.println("X Prune: " + mention);
					} else {
						System.out.println("O Prune: " + mention);
					}
					continue;
				}
			}
			// }
		}
		for (EntityMention remove : removes) {
			if (remove.roleSet.size() == 0) {
				mentions.remove(remove);
			}
		}
		HashSet<EntityMention> mentionsHash = new HashSet<EntityMention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}
}
