package ace.ml;

import java.util.ArrayList;
import java.util.List;

import model.EntityMention;
import ace.ACECommon;
import ace.model.EventMention;

public class ChCorefRuleFeature extends CorefFeature {

	public ChCorefRuleFeature(ACEML ml) {
		super(ml);
	}

	public List<Feature> getLoneFeature(boolean train, EntityMention mention) {
		boolean eventMention = false;
		if (mention instanceof EventMention) {
			eventMention = true;
		}

		List<Feature> features = new ArrayList<Feature>();

		if (!eventMention) {
			// 1 PRONOUN_1
			if (mention.isPronoun) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
			// 2 subject_1
			if (mention.isSub) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
			// 3 nested_1
			if (mention.isNNP) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
			// 4 number_2
			features.add(new Feature(mention.number.ordinal(), 4));
			// 5 gender_2
			features.add(new Feature(mention.gender.ordinal(), 5));
			// 6 animacy_2
			features.add(new Feature(mention.animacy.ordinal(), 4));

			features.add(new Feature(4, 5));
			features.add(new Feature(2, 3));
			features.add(new Feature(2, 3));
			features.add(new Feature(4, 5));
			features.add(new Feature(2, 3));
			
			if(mention.argument!=null) {
				argNo++;
			}
		}
		if (eventMention) {
			features.add(new Feature(2, 3));
			features.add(new Feature(2, 3));
			features.add(new Feature(2, 3));

			features.add(new Feature(3, 4));
			features.add(new Feature(4, 5));
			features.add(new Feature(3, 4));

			if (((EventMention) mention).posTag.startsWith("V")) {
				features.add(new Feature(0, 5));
			} else if (((EventMention) mention).posTag.startsWith("N")) {
				features.add(new Feature(1, 5));
			} else if (((EventMention) mention).posTag.startsWith("J")) {
				features.add(new Feature(2, 5));
			} else {
				features.add(new Feature(3, 5));
			}

			// modality, polarity, tense, genericity
			features
					.add(new Feature(EventMention.POLARITY.indexOf(((EventMention) mention).polarity.toUpperCase()), 3));
			features.add(new Feature(
					EventMention.GENERICITY.indexOf(((EventMention) mention).genericity.toUpperCase()), 3));
			features.add(new Feature(EventMention.TENSE.indexOf(((EventMention) mention).tense.toUpperCase()), 5));
			features
					.add(new Feature(EventMention.MODALITY.indexOf(((EventMention) mention).modality.toUpperCase()), 3));
		}
		return features;
	}

	static int argNo = 0;

	public List<Feature> getBilateralFea(boolean train, EntityMention[] pair) {
		ArrayList<Feature> features = new ArrayList<Feature>();
		eventCoref = false;
		entityCoref = false;
		boolean eventMention0 = (pair[0] instanceof EventMention);
		boolean eventMention1 = (pair[1] instanceof EventMention);

		if (eventMention0 && eventMention1) {
			eventCoref = true;
		}
		if (!eventMention0 && !eventMention1) {
			entityCoref = true;
		}

		// event features
		features.addAll(this.getEventsBiFea2(train, pair));

//		if (eventMention0 || eventMention1) {
//			if (eventMention0 && eventMention1) {
//				features.add(new Feature(0, 3));
//			} else {
//				features.add(new Feature(1, 3));
//			}
//		} else {
//			features.add(new Feature(2, 3));
//		}

		features.addAll(this.sievesFeature(train, pair));

		return features;
	}

	static boolean coref;

	public List<Feature> getEventsBiFea2(boolean train, EntityMention[] pair) {
		List<Feature> features = new ArrayList<Feature>();
		if (!eventCoref) {
			features.add(new Feature(-1, 2));
//			features.add(new Feature(-1, 2));
//			features.add(new Feature(-1, 2));
		} else {
			EventMention ant = (EventMention) pair[1];
			EventMention em = (EventMention) pair[0];

			if (EventFeatures.sameTrigger(ant, em, part) && EventFeatures.corefArgument(ant, em, part)
					&& !EventFeatures.polarityConflict(ant, em, part) && !EventFeatures.modalityConflict(ant, em, part)
					&& !EventFeatures.genericityConflict(ant, em, part) && !EventFeatures.tenseConflict(ant, em, part)) {
//				System.err.println("GEEE: " + ant.head + "#" + em.head);
				features.add(new Feature(0, 2));
			} else {
				features.add(new Feature(1, 2));
			}

//			if (EventFeatures.sameMeaning(ant, em, part) && EventFeatures.corefArgument(ant, em, part)
//					&& !EventFeatures.polarityConflict(ant, em, part) && !EventFeatures.modalityConflict(ant, em, part)
//					&& !EventFeatures.genericityConflict(ant, em, part) && !EventFeatures.tenseConflict(ant, em, part)) {
//				features.add(new Feature(0, 2));
//			} else {
//				features.add(new Feature(1, 2));
//			}
//
//			if (EventFeatures.sameBV(ant, em, part) && EventFeatures.corefArgument(ant, em, part)
//					&& !EventFeatures.polarityConflict(ant, em, part) && !EventFeatures.modalityConflict(ant, em, part)
//					&& !EventFeatures.genericityConflict(ant, em, part) && !EventFeatures.tenseConflict(ant, em, part)) {
//				features.add(new Feature(0, 2));
//			} else {
//				features.add(new Feature(1, 2));
//			}
		}
		return features;
	}

	public List<Feature> getEventsBiFea(boolean train, EntityMention[] pair) {

		// EventFeatures fea = new EventFeatures();
		// if (eventCoref) {
		// coref = (((EventMention) pair[0]).eventChain == ((EventMention)
		// pair[1]).eventChain);
		// if (coref) {
		// System.out.println(pair[1].head + "#" + pair[1].headCharStart + ":" +
		// pair[1].headCharEnd);
		// System.out.println(pair[0].head + "#" + pair[0].headCharStart + ":" +
		// pair[0].headCharEnd);
		// System.err.println(this.part.getDocument().getFilePath());
		// Method methods[] = EventFeatures.class.getMethods();
		// for (Method method : methods) {
		// Class<?> argTypes[] = method.getParameterTypes();
		// if (argTypes.length == 2 && argTypes[0] == EventMention.class &&
		// argTypes[1] == EventMention.class) {
		// Object b;
		// try {
		// b = method.invoke(fea, pair[1], pair[0]);
		// StringBuilder sb = new StringBuilder();
		// sb.append(method.getName()).append(":\n");
		// if (b instanceof Boolean) {
		// sb.append(b);
		// } else if (b instanceof Integer) {
		// sb.append(b);
		// } else if (b instanceof int[]) {
		// for (int i=0;i<((int[]) b).length;i++) {
		// if (((int[]) b)[i] != 0) {
		// sb.append(ACECommon.roles.get(i)).append(",");
		// }
		// }
		// }
		// System.err.println(sb.toString());
		// } catch (IllegalArgumentException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (InvocationTargetException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// }
		// System.err.println("========================");
		// }
		// }

		ArrayList<Feature> features = new ArrayList<Feature>();
		if (!eventCoref) {
			// trigger match, meaning...
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));

			// distance...
			features.add(new Feature(-1, 4));
			features.add(new Feature(-1, 4));
			features.add(new Feature(-1, 4));

			// overlap
			features.add(new Feature(-1, 5));
			for (int i = 0; i < ACECommon.roles.size(); i++) {
				// features.add(new Feature(-1, 2));
			}

			// prior_num
			features.add(new Feature(-1, 5));
			for (int i = 0; i < ACECommon.roles.size(); i++) {
				// features.add(new Feature(-1, 2));
			}

			// act_num
			features.add(new Feature(-1, 5));
			for (int i = 0; i < ACECommon.roles.size(); i++) {
				// features.add(new Feature(-1, 2));
			}

			// coref_num
			features.add(new Feature(-1, 5));

			// place, time...
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));

			// genericty, tense...
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			features.add(new Feature(-1, 2));
			return features;
		}

		EventMention ant = (EventMention) pair[1];
		EventMention em = (EventMention) pair[0];
		// trigger match, meaning...
		if (EventFeatures.sameTrigger(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		if (EventFeatures.sameMeaning(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// distance...
		features.add(new Feature(EventFeatures.tokenDis(ant, em, part), 4));
		features.add(new Feature(EventFeatures.sentDis(ant, em, part), 4));
		features.add(new Feature(EventFeatures.eventDis(ant, em, part), 4));

		// overlap_num, overlap_roles
		features.add(new Feature(EventFeatures.overlapRoleAndEntity(ant, em, part), 5));
		int overlapRoles[] = EventFeatures.overlapRoles(ant, em, part);
		for (int k : overlapRoles) {
			// features.add(new Feature(k, 2));
		}

		// prior_num, prior_roles
		features.add(new Feature(EventFeatures.priorNumber(ant, em, part), 5));
		int prior_roles[] = EventFeatures.priorRoles(ant, em, part);
		for (int k : prior_roles) {
			// features.add(new Feature(k, 2));
		}

		// act_num, act_roles
		features.add(new Feature(EventFeatures.actNumber(ant, em, part), 5));
		int act_roles[] = EventFeatures.actRoles(ant, em, part);
		for (int k : act_roles) {
			// features.add(new Feature(k, 2));
		}

		// coref_num
		features.add(new Feature(EventFeatures.corefNumber(ant, em, part), 5));

		// place, time...
		if (EventFeatures.placeConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		if (EventFeatures.timeConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// genericty, tense...
		if (EventFeatures.modalityConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		if (EventFeatures.polarityConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		if (EventFeatures.genericityConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		if (EventFeatures.tenseConflict(ant, em, part)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		return features;
	}

}
