package ruleCoreference.chinese;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.ChCommon;
import util.Common;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;

public class CheckPronoun {
	public static void main(String args[]) {
		ChCommon common = new ChCommon("chinese");
		ArrayList<String> files = Common.getLines(args[0] + "_list_" + args[1] + "_train");
		HashMap<String, Integer> animateHead = new HashMap<String, Integer>();
		HashMap<String, Integer> inanimateHead = new HashMap<String, Integer>();

		HashMap<String, Integer> singleHead = new HashMap<String, Integer>();
		HashMap<String, Integer> pluraHead = new HashMap<String, Integer>();

		HashMap<String, Integer> maleHead = new HashMap<String, Integer>();
		HashMap<String, Integer> femaleHead = new HashMap<String, Integer>();

		HashSet<String> pns = new HashSet<String>();

		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
			String conllFn = files.get(fileIdx);
			System.out.println(conllFn);
			CoNLLDocument doc = new CoNLLDocument(conllFn);
			for (CoNLLPart part : doc.getParts()) {
				ArrayList<Entity> chains = part.getChains();
				for (Entity chain : chains) {
					Animacy animate = Animacy.UNKNOWN;
					Numb numb = Numb.UNKNOWN;
					Gender gender = Gender.UNKNOWN;
					ArrayList<String> heads = new ArrayList<String>();
					StringBuilder sb = new StringBuilder();
					for (EntityMention mention : chain.mentions) {
						common.calChAttribute(mention, part);
						sb.append(mention.source.replace(" ", "")).append(" ");
						pns.add(mention.head);
						if (mention.animacy == Animacy.ANIMATE) {
							animate = Animacy.ANIMATE;
						} else if (mention.animacy == Animacy.INANIMATE) {
							animate = Animacy.INANIMATE;
						}
						if (mention.gender == Gender.MALE) {
							gender = Gender.MALE;
						} else if (mention.gender == Gender.FEMALE) {
							gender = Gender.FEMALE;
						}
						if (mention.number == Numb.SINGULAR) {
							numb = Numb.SINGULAR;
						} else if (mention.number == Numb.PLURAL) {
							numb = Numb.PLURAL;
						}
					}
					for (EntityMention mention : chain.mentions) {
						String head = mention.head;
						heads.add(head);
					}
					if (animate == Animacy.ANIMATE) {
						increase(animateHead, heads);
					} else if (animate == Animacy.INANIMATE) {
						increase(inanimateHead, heads);
					}
					if (gender == Gender.MALE) {
						increase(maleHead, heads);
					} else if (gender == Gender.FEMALE) {
						increase(femaleHead, heads);
					}
					if (numb == Numb.SINGULAR) {
						increase(singleHead, heads);
					} else if (numb == Numb.PLURAL) {
						increase(pluraHead, heads);
					}
				}
			}
		}
		System.out.println("1");
		Common.outputHashMap(animateHead, "chinese_animate");
		System.out.println("2");
		Common.outputHashMap(inanimateHead, "chinese_inanimate");
		System.out.println("3");
		Common.outputHashMap(maleHead, "chinese_male");
		System.out.println("4");
		Common.outputHashMap(femaleHead, "chinese_female");
		System.out.println("5");
		Common.outputHashMap(singleHead, "chinese_single");
		System.out.println("6");
		Common.outputHashMap(pluraHead, "chinese_plura");
		for (String str : pns) {
			System.out.println(str);
		}
	}

	public static void increase(HashMap<String, Integer> counts, ArrayList<String> heads) {
		for (String head : heads) {
			Integer in;
			if ((in = counts.get(head)) != null) {
				counts.put(head, in.intValue() + 1);
			} else {
				counts.put(head, 1);
			}
		}
	}
}
