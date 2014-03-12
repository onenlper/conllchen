package mentionDetect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mentionDetection.arabic.ArabicMention;
import mentionDetection.chinese.ChineseMention;
import model.Element;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLDocument.DocType;
import util.OntoCommon;
import util.Common.Person;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class ParseTreeMention extends MentionDetect {

	OntoCommon ontoCommon;

	@Override
	public ArrayList<EntityMention> getMentions(CoNLLPart part) {
		if (part.getDocument().getLanguage().equalsIgnoreCase("chinese")) {
			ChineseMention ch = new ChineseMention();
			return ch.getChineseMention(part);
		} else if (part.getDocument().getLanguage().equalsIgnoreCase("english")) {
			ontoCommon = new OntoCommon("english");
			return getEnglishMention(part);
		} else if (part.getDocument().getLanguage().equalsIgnoreCase("arabic")) {
			ArabicMention ar = new ArabicMention();
			return ar.getArabicMention(part);
		}
		return null;
	}

	private ArrayList<EntityMention> getNamedEntityMention(CoNLLPart part) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<Element> namedEntities = part.getNameEntities();
		for (Element element : namedEntities) {
			if (element.content.equalsIgnoreCase("QUANTITY") || element.content.equalsIgnoreCase("CARDINAL")
					|| element.content.equalsIgnoreCase("PERCENT")) {
				continue;
			}
			// Mr. Mandela
			if (element.start > 0
					&& this.ontoCommon.getEnDictionary().titles.contains(part.getWord(element.start - 1).word)) {
				continue;
			}
			int end = element.end;
			int start = element.start;
			if (element.end + 1 < part.getWordCount()) {
				String lastWord = part.getWord(element.end + 1).word;
				if (lastWord.equalsIgnoreCase("'s")) {
					end++;
				}
			}
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

	private ArrayList<EntityMention> getNPorPRPMention(CoNLLPart part) {
		ArrayList<EntityMention> npMentions = new ArrayList<EntityMention>();
		npMentions = ontoCommon.getAllNounPhrase(part.getCoNLLSentences());

		// MentionDetect md = new GoldBoundaryMentionTest();
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
			// System.out.println(start + " " + end + " " + npMention.source);

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

	private void pruneMentions(ArrayList<EntityMention> mentions, CoNLLPart part) {
		assignNE(mentions, part);
		for (EntityMention mention : mentions) {
			this.ontoCommon.calEnAttribute(mention, part);
		}
		ArrayList<EntityMention> removes = new ArrayList<EntityMention>();

		Collections.sort(mentions);
		for (int i = 0; i < mentions.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (this.ontoCommon.isEnglishAppositive2(mentions.get(j), mentions.get(i), part)) {
					removes.add(mentions.get(i));
					removes.add(mentions.get(j));
				}
			}
		}
		// remove smaller mention with the same head as the longer mention
		ArrayList<EntityMention> copyMentions = new ArrayList<EntityMention>(mentions.size());
		copyMentions.addAll(mentions);
		for (int i = 0; i < mentions.size(); i++) {
			EntityMention em = mentions.get(i);
			for (int j = 0; j < copyMentions.size(); j++) {
				EntityMention em2 = copyMentions.get(j);
				if (em.headStart == em2.headStart && em.headEnd == em2.headEnd
						&& (em.end - em.start < em2.end - em2.start)) {
					if (em.end + 1 < part.getWordCount()
							&& (part.getWord(em.end + 1).posTag.equalsIgnoreCase("CC") || part.getWord(em.end + 1).word
									.equals(","))) {
						continue;
					}
					removes.add(em);
					break;
				}
			}
		}

		mentions.removeAll(removes);
		removes.clear();

		for (int i = 0; i < mentions.size(); i++) {
			EntityMention mention = mentions.get(i);
			mention.position = ontoCommon.getPosition(mention, part.getCoNLLSentences());
			if (isPleonastic(mention, part.getCoNLLSentences().get(mention.position[0]).stdTree)) {
				removes.add(mention);
				continue;
			}

			if (mention.head.equalsIgnoreCase("%")) {
				removes.add(mention);
				continue;
			}
			if (mention.ner.equalsIgnoreCase("QUANTITY") || mention.ner.equalsIgnoreCase("CARDINAL")
					|| mention.ner.equalsIgnoreCase("PERCENT") || mention.ner.equalsIgnoreCase("MONEY")) {
				removes.add(mention);
				continue;
			}
			// non word such as 'hmm'
			if (ontoCommon.getEnDictionary().nonWords.contains(mention.head.toLowerCase())) {
				removes.add(mention);
				continue;
			}
			// quantRule : not starts with 'any', 'all' etc
			if (ontoCommon.getEnDictionary().quantifiers.contains(part.getWord(mention.start).word)) {
				removes.add(mention);
				continue;
			}
			// partitiveRule
			if (mention.start > 2 && part.getWord(mention.start - 1).word.equals("of")
					&& ontoCommon.getEnDictionary().parts.contains(part.getWord(mention.start - 2).word)) {
				removes.add(mention);
				continue;
			}
			// bareNPRule
			if (part.getWord(mention.headStart).posTag.equals("NN")
					&& !ontoCommon.getEnDictionary().temporals.contains(mention.head)
					&& (mention.end - mention.start == 0 || part.getWord(mention.start).posTag.equalsIgnoreCase("JJ"))) {
				removes.add(mention);
				continue;
			}
			// adjective form of nations
			if (ontoCommon.getEnDictionary().adjectiveNation.contains(mention.source)) {
				removes.add(mention);
				continue;
			}
			// stop list (e.g., U.S., there)
			if (inStopList(mention)) {
				removes.add(mention);
				continue;
			}

			if (ruleCoreference.english.RuleCoref.tuneSwitch) {
				String head = mention.head.toLowerCase();
				if (stats.containsKey(head)) {
					if (stats.get(head) < t5) {
						if (part.getMentions().contains(mention)) {
							// System.out.println("X Prune: " + mention);
						} else {
							// System.out.println("O Prune: " + mention);
						}
						removes.add(mention);
					}
				}
			}
		}
		mentions.removeAll(removes);
		HashSet<EntityMention> mentionsHash = new HashSet<EntityMention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}

	public static HashMap<String, Double> stats;

	public static double t5 = -1;

	/** Check whether pleonastic 'it'. E.g., It is possible that ... */
	private static boolean isPleonastic(EntityMention m, Tree tree) {
		if (!m.original.equalsIgnoreCase("it"))
			return false;
		final String[] patterns = {
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (VP < (VBN $.. /S|SBAR/))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP $.. (/S|SBAR/))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP < (/S|SBAR/))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP < /S|SBAR/)))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP $.. ADVP $.. /S|SBAR/)))",
				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (VP < (VBN $.. /S|SBAR/))))))",
				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP $.. (/S|SBAR/))))))",
				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP < (/S|SBAR/))))))",
				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP < /S|SBAR/)))))",
				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP $.. ADVP $.. /S|SBAR/)))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:seems|appears|means|follows)/) $.. /S|SBAR/))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:turns|turned)/) $.. PRT $.. /S|SBAR/))" };
		for (String p : patterns) {
			if (checkPleonastic(m, tree, p)) {
				return true;
			}
		}
		return false;
	}

	private static boolean checkPleonastic(EntityMention m, Tree tree, String pattern) {
		try {
			TregexPattern tgrepPattern = TregexPattern.compile(pattern);
			TregexMatcher matcher = tgrepPattern.matcher(tree);
			while (matcher.find()) {
				Tree np1 = matcher.getNode("m1");
				if (((CoreLabel) np1.label()).get(BeginIndexAnnotation.class) == m.position[1]) {
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	private static boolean inStopList(EntityMention m) {
		String mentionSpan = m.source.toLowerCase();
		if (mentionSpan.equals("u.s.") || mentionSpan.equals("u.k.") || mentionSpan.equals("u.s.s.r"))
			return true;
		if (mentionSpan.equals("there") || mentionSpan.startsWith("etc.") || mentionSpan.equals("ltd."))
			return true;
		if (mentionSpan.startsWith("'s "))
			return true;
		if (mentionSpan.endsWith("etc."))
			return true;
		return false;
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

	private ArrayList<EntityMention> getEnglishMention(CoNLLPart part) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		mentions.addAll(this.getNamedEntityMention(part));
		mentions.addAll(this.getNPorPRPMention(part));
		removeDuplicateMentions(mentions);
		this.pruneMentions(mentions, part);
		this.setBarePlural(mentions, part);
		return mentions;
	}

	private void setBarePlural(List<EntityMention> mentions, CoNLLPart part) {
		Collections.sort(mentions);
		for (EntityMention m : mentions) {
			String pos = part.getWord(m.start).posTag;
			if (m.start == m.end && pos.equals("NNS")) {
				m.generic = true;
			}
			// set generic 'you' : e.g., you know in conversation
			if (part.getDocument().getType() != DocType.Article && m.person == Person.YOU
					&& m.end + 1 < part.getWordCount() && part.getWord(m.end + 1).orig.equals("know")) {
				m.generic = true;
			}
		}
	}

	private void removeDuplicateMentions(ArrayList<EntityMention> mentions) {
		HashSet<EntityMention> mentionsHash = new HashSet<EntityMention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}
}
