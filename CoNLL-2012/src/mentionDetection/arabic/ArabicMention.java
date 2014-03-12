package mentionDetection.arabic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mentionDetect.GoldMentionTest;
import mentionDetect.MentionDetect;
import model.Element;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.ArCommon;
import util.Common;

public class ArabicMention {

	private ArCommon arCommon;

	public ArabicMention() {
		this.arCommon = new ArCommon("arabic");
	}

	public ArrayList<EntityMention> getArabicMention(CoNLLPart part) {
		MyTreeNode.arabic = true;
		if (part.getDocument().getFilePath().contains("development")
				|| part.getDocument().getFilePath().contains("test")) {
			part.setNameEntities(getChNE(part));
		}
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		// mentions.addAll(this.getChNamedEntityMention(part));
		mentions.addAll(this.getChNPMention(part));
		removeDuplicateMentions(mentions);
//		pruneChMentions(mentions, part);
		// this.setChBarePlural(mentions, part);
		return mentions;
	}

	HashMap<String, ArrayList<Element>> EMs;

	private ArrayList<Element> getChNE(CoNLLPart part) {
		String key = part.getDocument().getDocumentID() + "_" + part.getPartID();
		// ArrayList<Element> elements = arCommon.predictNEs.get(key);
		// if(elements==null) {
		// elements = new ArrayList<Element>();
		// }
		ArrayList<Element> elements = new ArrayList<Element>();
		for (Element element : elements) {
			for (int i = element.start; i <= element.end; i++) {
				part.getWord(i).setRawNamedEntity(element.content);
			}
		}
		return elements;
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
		ArrayList<EntityMention> npMentions = arCommon.getAllNounPhrase(part.getCoNLLSentences());
	
		MentionDetect md = new GoldMentionTest();
		npMentions = md.getMentions(part);
		
		for (int g = 0; g < npMentions.size(); g++) {
			EntityMention npMention = npMentions.get(g);
			int end = npMention.end;
			int start = npMention.start;
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			StringBuilder sb3 = new StringBuilder();
			StringBuilder sb4 = new StringBuilder();
			StringBuilder sb5 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).orig.split("#")[0]);
				sb2.append(part.getWord(i).orig).append(" ");
				sb3.append(part.getWord(i).arBuck).append(" ");
				sb4.append(part.getWord(i).arUnBuck).append(" ");
				sb5.append(part.getWord(i).arLemma).append(" ");
			}
			npMention.source = sb.toString().trim();
			npMention.original = sb2.toString().trim();
			npMention.buckWalter = sb3.toString().trim();
			npMention.buckUnWalter = sb4.toString().trim();
			npMention.lemma = sb5.toString().trim();
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
					mention.ner = element.content;
				}
			}
		}
	}

	private void pruneChMentions(ArrayList<EntityMention> mentions, CoNLLPart part) {
		for (EntityMention mention : mentions) {
			this.arCommon.calAttribute(mention, part);
		}
		assignNE(mentions, part);
		ArrayList<EntityMention> removes = new ArrayList<EntityMention>();
		Collections.sort(mentions);
		ArrayList<EntityMention> copyMentions = new ArrayList<EntityMention>(mentions.size());
		copyMentions.addAll(mentions);
		for (int i = 0; i < mentions.size(); i++) {
			EntityMention em = mentions.get(i);
			for (int j = 0; j < copyMentions.size(); j++) {
				EntityMention em2 = copyMentions.get(j);
				if (em.headStart == em2.headStart && (em.end - em.start > em2.end - em2.start)) {
//					if (em.lemma.equals("clitics")) {
//						removes.add(em2);
//					} else {
//						removes.add(em);
//					}
//					if(part.getWord(em.headStart).getArLemma().equals("clitics")) {
//						continue;
//					}
//					removes.add(em);
					break;
				}
			}
		}
		mentions.removeAll(removes);
		removes.clear();
		if(not_mention==null) {
			not_mention = Common.readFile2Set("arabic_not_mention");
		}
		
		Set<String> notResolve = new HashSet<String>(Arrays.asList("DT"));
		for(int i=0;i<mentions.size();i++) {
			EntityMention mention = mentions.get(i);
			String posTag = part.getWord(mention.start).posTag;
			if(mention.start==mention.end && notResolve.contains(part.getWord(mention.start).posTag)) {
				removes.add(mention);
				continue;
			}
			CoNLLWord word = part.getWord(mention.start);
			String buckWalter = part.getWord(mention.start).getArBuck();

			for(int j=0;j<mention.source.length();j++) {
				char c = mention.source.charAt(j);
				if(c<='9' && c>='0') {
					removes.add(mention);
//					System.out.println("Remove: " + mention);
					continue;
				}
			}
			
			if(mention.source.contains("المِئَةِ")) {
				removes.add(mention);
				continue;
			}
			if(not_mention.contains(mention.original)) {
				removes.add(mention);
				continue;
			}
			String head = mention.head.toLowerCase();
			if (ruleCoreference.arabic.RuleCoref.mention_stats.containsKey(head)) {
				if (ruleCoreference.arabic.RuleCoref.mention_stats.get(head) <=  ruleCoreference.arabic.RuleCoref.t5) {
					removes.add(mention);
					continue;
				}
			}
			
			// bareNPRule
			if (part.getWord(mention.headStart).posTag.equals("NN") &&
					(mention.end - mention.start == 0 || part.getWord(mention.start).posTag.equalsIgnoreCase("JJ"))) {
//				removes.add(mention);
				continue;
			}
			if(mention.start==mention.end && part.getWord(mention.start).getArBuck().equals("-hu")) {
//				removes.add(mention);
			}
		}
		mentions.removeAll(removes);
		removes.clear();
	}
	
	public static HashSet<String> not_mention;
}
