package ace.rule;

import java.util.ArrayList;

import ace.model.EventMention;

import ruleCoreference.chinese.ChDictionary;

import model.EntityMention;
import model.CoNLL.CoNLLWord;
import util.Common;
import util.Common.Numb;
import util.Common.Person;

public class DiscourseProcessSieve extends Sieve {

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		if(em instanceof EventMention) {
			return;
		}
		for (EntityMention ant : orderedAntecedents) {
			String mString = em.original.toLowerCase();
			String antString = ant.original.toLowerCase();
			ChDictionary dict = ruleCoref.ontoCommon.getChDictionary();
			String mSpeaker = ruleCoref.part.getWord(em.headStart).speaker;
			String antSpeaker = ruleCoref.part.getWord(ant.headStart).speaker;
			int mUtterOrder = ruleCoref.part.getWord(em.headStart).utterOrder;
			int antUtterOrder = ruleCoref.part.getWord(ant.headStart).utterOrder;
			CoNLLWord antWord = ruleCoref.part.getWord(ant.headStart);
			CoNLLWord mWord = ruleCoref.part.getWord(em.headStart);
			// (I - I) in the same speaker's quotation.
			if (ruleCoref.ontoCommon.getChDictionary().firstPersonPronouns.contains(mString)
					&& em.number == Numb.SINGULAR
					&& ruleCoref.ontoCommon.getChDictionary().firstPersonPronouns.contains(antString)
					&& ant.number == Numb.SINGULAR && mSpeaker.equals(antSpeaker) && !Common.isPronoun(antSpeaker)) {
				if(RuleCoref.bs.get(1))
					if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					return;
				}
			}
			// (speaker - I)
			if (ruleCoref.ontoCommon.isSpeaker(ant, em, ruleCoref.part)
					&& ((dict.firstPersonPronouns.contains(mString) && em.number == Numb.SINGULAR) || (dict.firstPersonPronouns
							.contains(antString) && ant.number == Numb.SINGULAR))) {
				if(RuleCoref.bs.get(2))
					if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					// System.out.print("Rule2: ");
					// System.out.println(ant.original + " " + ant.position[1] +
					// " # " + mention.original +" " + mention.position[1] );
						return;
				}
			}
			// You - You
			if (mSpeaker.equalsIgnoreCase(antSpeaker) && 
					(antWord.toSpeaker.containsAll(mWord.toSpeaker) || mWord.toSpeaker.containsAll(antWord.toSpeaker))
					&& !mSpeaker.equalsIgnoreCase("-")
					&& dict.secondPersonPronouns.contains(mString) && dict.secondPersonPronouns.contains(antString)
					&& ant.number == Numb.SINGULAR && em.number == Numb.SINGULAR) {
				if(RuleCoref.bs.get(3))
					if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					// System.out.print("Rule3: ");
					// System.out.println(ant.original + " " + ant.position[1] +
					// " # " + mention.original +" " + mention.position[1] );
						return;
				}
			}
			// previous I - you or previous you - I in two person conversation
			if ((em.person == Person.I && ant.person == Person.YOU 
			&& antWord.toSpeaker.contains(mSpeaker))
					|| (em.person == Person.YOU && ant.person == Person.I && 
							mWord.toSpeaker.contains(antSpeaker))) {
				if(RuleCoref.bs.get(4))
					if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					// System.out.print("Rule4: ");
					// System.out.println(ant.original + " " + ant.position[1] +
					// " # " + mention.original +" " + mention.position[1] );
						return;
				}
			}
		}

	}

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		boolean applicable = false;
		int start = 0;
		int end = 0;
		String sentence = "";
		int sentenceId = 0;
		if (ruleCoref.part.getWord(em.end + 1).word.equals("说")) {
			sentence = ruleCoref.sentences.get(em.sentenceID).getSyntaxTree().root.toString();
			start = sentence.indexOf("“");
			end = sentence.indexOf("”");
			if (start != -1 && end != -1) {
				sentenceId = em.sentenceID;
				applicable = true;
			} else if (em.sentenceID + 1 < ruleCoref.sentences.size()) {
				sentence = ruleCoref.sentences.get(em.sentenceID + 1).getSyntaxTree().root.toString();
				start = sentence.indexOf("“");
				end = sentence.indexOf("”");
				if (start != -1 && end != -1) {
					sentenceId = em.sentenceID + 1;
					applicable = true;
				}
			}
		}
		if (applicable) {
			// System.out.println(sentence);
			if (antecedent.head.contains("我") && antecedent.headStart > start && antecedent.headEnd < end) {
				// System.out.println(antecedent.extent + " " + em.extent +
				// " DiscourseProcessSieve ");
				if (ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
					return true;
				}
			}
		}
		return false;
	}

}
