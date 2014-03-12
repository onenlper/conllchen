package ruleCoreference.english;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import util.Common.Person;

public abstract class Sieve {

	RuleCoref ruleCoref;

	public abstract void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents);

	public abstract boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref);

	public void act(RuleCoref ruleCoref) {
		this.ruleCoref = ruleCoref;
		ArrayList<EntityMention> selectedMentions = this.mentionSelection(ruleCoref.mentions);
		for (int i = 0; i < selectedMentions.size(); i++) {
			EntityMention em = selectedMentions.get(i);
			ArrayList<EntityMention> orderedAntecedents = ruleCoref.getOrderedAntecedent(em);
			sieve(ruleCoref, em, orderedAntecedents);
		}
	}

	//******Pali (INANIMATE UNKNOWN UNKNOWN ORG Pali) [7:0,0 146 146] ^-^#Pali Rural Township Government (INANIMATE SINGULAR NEUTRAL ORG Township)
	public boolean neShort(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		for (EntityMention ant : antecedent.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if(ant.ner.equalsIgnoreCase(em.ner) && 
						(ant.ner.equalsIgnoreCase("ORG") || ant.ner.equalsIgnoreCase("GPE"))) {
					if((ant.start==ant.end && part.getWord(em.start).word.equals(ant.original.trim())) 
							|| (em.start==em.end|| part.getWord(ant.start).word.equals(em.original.trim()))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean haveIncompatibleModify(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		for (EntityMention ant : antecedent.entity.mentions) {
			for (EntityMention em : mention.entity.mentions) {
				if (!ant.head.equalsIgnoreCase(em.head)) {
					continue;
				}
				boolean thisHasExtra = false;
				Set<String> thisWordSet = new HashSet<String>();
				Set<String> antWordSet = new HashSet<String>();
				Set<String> locationModifier = new HashSet<String>(Arrays.asList("east", "west", "north", "south",
						"eastern", "western", "northern", "southern", "upper", "lower"));
				String mPRP = "";
				String antPRP = "";
				for (int i = em.start; i <= em.end; i++) {
					String w1 = part.getWord(i).orig.toLowerCase();
					String pos1 = part.getWord(i).posTag;
					if(pos1.startsWith("PRP")) {
						mPRP = w1;
					}
					if (!(pos1.startsWith("N") || pos1.startsWith("JJ") || pos1.equals("CD") || pos1.startsWith("V")
							 || w1.equalsIgnoreCase(em.head)))
						continue;
					thisWordSet.add(w1);
				}
				for (int j = ant.start; j <= ant.end; j++) {
					String w2 = part.getWord(j).orig.toLowerCase();
					String pos2 = part.getWord(j).posTag;
					if(pos2.startsWith("PRP")) {
						antPRP = w2;
					}
					antWordSet.add(w2);
				}
				if(!mPRP.isEmpty() && !antPRP.isEmpty() && !mPRP.equals(antPRP)) {
					return false;
				}
				for (String w : thisWordSet) {
					if (!antWordSet.contains(w))
						thisHasExtra = true;
				}
				boolean hasLocationModifier = false;
				for (String l : locationModifier) {
					if (antWordSet.contains(l) && !thisWordSet.contains(l)) {
						hasLocationModifier = true;
					}
				}
				if (thisHasExtra || hasLocationModifier) {
					return true;
				}
			}
		}
		return false;
	}

	// only resolve mentions that are currently first in textual order of
	// mentions
	// 
	public ArrayList<EntityMention> mentionSelection(ArrayList<EntityMention> allMentions) {
		ArrayList<EntityMention> selectedMentions = new ArrayList<EntityMention>();
		for (EntityMention mention : allMentions) {
			if (!skipThisMention(mention)) {
				selectedMentions.add(mention);
			} else {
			}
		}
		return selectedMentions;
	}

	public boolean skipThisMention(EntityMention mention) {
		boolean skip = false;
		// only do for the first mention in its cluster
		if (!mention.entity.getFirstMention().equals(mention)) {
			return true;
		}
		if ((mention.original.toLowerCase().startsWith("a ") || mention.original.toLowerCase().startsWith("an "))
				&& !(this instanceof ExactMatchSieve)) {
			skip = true; // A noun phrase starting with an indefinite article -
			// unlikely to have an antecedent (e.g.
			// "A commission" was set up to .... )
		}
		if (this.ruleCoref.ontoCommon.getEnDictionary().indefinitePronouns.contains(mention.original.toLowerCase())) {
			skip = true; // An indefinite pronoun - unlikely to have an
			// antecedent (e.g. "Some" say that... )
		}
		for (String indef : this.ruleCoref.ontoCommon.getEnDictionary().indefinitePronouns) {
			if (mention.original.toLowerCase().startsWith(indef + " ")) {
				skip = true; // A noun phrase starting with an indefinite
				// adjective - unlikely to have an antecedent
				// (e.g. "Another opinion" on the topic is...)
				break;
			}
		}
		return skip;
	}

	public boolean relaxHeadMatch(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (antecedent.isPronoun || em.isPronoun) {
			return false;
		}
		if (antecedent.ner.equalsIgnoreCase(em.ner)
				&& !antecedent.ner.equalsIgnoreCase("OTHER")
				&& (included(ruleCoref.part.getWord(antecedent.headStart), em) || included(ruleCoref.part
						.getWord(em.headStart), antecedent))) {
			return true;
		} else {
			return false;
		}
	}

	private boolean included(CoNLLWord headWord, EntityMention mention) {
		String small = headWord.orig.toLowerCase();
		if (headWord.posTag.equals("NNP")) {
			for (String word : mention.source.split("\\s+")) {
				word = word.toLowerCase();
				if (small.equals(word) || small.length() > 2 && word.startsWith(small)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean clusterHeadMatch(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		if (antecedent.isPronoun || em.isPronoun
				|| ruleCoref.ontoCommon.getEnDictionary().allPronouns.contains(antecedent.original.toLowerCase())
				|| ruleCoref.ontoCommon.getEnDictionary().allPronouns.contains(em.original.toLowerCase())) {
			return false;
		}
		boolean match = false;
		for (EntityMention candidate : antecedent.entity.mentions) {
			if (em.head.equalsIgnoreCase(candidate.head)) {
				match = true;
				break;
			}
		}
		return match;
	}

	public boolean wordInclusion(EntityMention antecedent, EntityMention mention, RuleCoref ruleCoref) {
		HashSet<String> mentionClusterStrs = new HashSet<String>();
		for (EntityMention em : mention.entity.mentions) {
			for (int i = em.start; i <= em.end; i++) {
				mentionClusterStrs.add(ruleCoref.part.getWord(i).orig.toLowerCase());
			}
		}
		mentionClusterStrs.removeAll(Arrays.asList(new String[] { "the", "this", "mr.", "miss", "mrs.", "dr.", "ms.",
				"inc.", "ltd.", "corp.", "'s" }));
		mentionClusterStrs.remove(mention.head.toLowerCase());
		HashSet<String> candidateClusterStrs = new HashSet<String>();
		for (EntityMention e : antecedent.entity.mentions) {
			for (int i = e.start; i <= e.end; i++) {
				candidateClusterStrs.add(ruleCoref.part.getWord(i).orig.toLowerCase());
			}
		}
		if (candidateClusterStrs.containsAll(mentionClusterStrs))
			return true;
		else
			return false;
	}

	public boolean sameProperHeadLastWordCluster(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		for (EntityMention ante : antecedent.entity.mentions) {
			for (EntityMention cur : em.entity.mentions) {
				if (sameProperHeadLastWord(ante, cur, ruleCoref)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean sameProperHeadLastWord(EntityMention a, EntityMention m, RuleCoref ruleCoref) {
		CoNLLPart part = ruleCoref.part;
		if (!part.getWord(a.headStart).posTag.startsWith("NNP") || !part.getWord(m.headStart).posTag.startsWith("NNP")
				|| !a.head.equalsIgnoreCase(m.head)) {
			return false;
		}

		if (!ruleCoref.ontoCommon.wordBeforeHead(m, part).toLowerCase().endsWith(m.head)
				|| !ruleCoref.ontoCommon.wordBeforeHead(a, part).toLowerCase().endsWith(a.head)) {
			return false;
		}

		Set<String> mProperNouns = new HashSet<String>();
		Set<String> aProperNouns = new HashSet<String>();
		for (int i = a.start; i <= a.headStart; i++) {
			if (part.getWord(i).posTag.startsWith("NNP")) {
				aProperNouns.add(part.getWord(i).orig);
			}
		}
		for (int i = m.start; i <= m.headStart; i++) {
			if (part.getWord(i).posTag.startsWith("NNP")) {
				mProperNouns.add(part.getWord(i).orig);
			}
		}
		boolean mHasExtra = false;
		boolean aHasExtra = false;

		for (String str : mProperNouns) {
			if (!aProperNouns.contains(str)) {
				mHasExtra = true;
			}
		}
		for (String str : aProperNouns) {
			if (!mProperNouns.contains(str)) {
				aHasExtra = true;
			}
		}
		if (mHasExtra && aHasExtra) {
			return false;
		}
		return true;
	}

	public boolean clusterPersonDisagree(EntityMention antecedent, EntityMention mention, CoNLLPart part) {
		for (EntityMention ante : antecedent.entity.mentions) {
			for (EntityMention m : mention.entity.mentions) {
				if (mentionPersonDisagree(ante, m, part)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean mentionPersonDisagree(EntityMention ant, EntityMention m, CoNLLPart part) {
		String speak1 = part.getWord(ant.headStart).speaker;
		String speak2 = part.getWord(m.headStart).speaker;
		boolean sameSpeaker = speak1.equals(speak2);

		if (sameSpeaker && m.person != ant.person) {
			if ((m.person == Person.IT && ant.person == Person.THEY)
					|| (m.person == Person.THEY && ant.person == Person.IT)
					|| (m.person == Person.THEY && ant.person == Person.THEY))
				return false;
			else if (m.person != Person.UNKNOWN && ant.person != Person.UNKNOWN)
				return true;
		}
		if (sameSpeaker) {
			if (!ant.isPronoun) {
				if (m.person == Person.I || m.person == Person.WE || m.person == Person.YOU)
					return true;
			} else if (!m.isPronoun) {
				if (ant.person == Person.I || ant.person == Person.WE || ant.person == Person.YOU)
					return true;
			}
		}
		if (m.person == Person.YOU && ant.compareTo(m) < 0) {
			if (!part.getWord(m.headStart).speaker.equals("-")) {
				int currentUtterOrder = part.getWord(m.headStart).utterOrder;
				int anteUtterOrder = part.getWord(ant.headStart).utterOrder;
				if (anteUtterOrder != -1 && anteUtterOrder == currentUtterOrder - 1 && ant.person == Person.I) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

}
