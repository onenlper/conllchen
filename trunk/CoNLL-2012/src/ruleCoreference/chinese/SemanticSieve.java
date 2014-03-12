package ruleCoreference.chinese;
//package ruleCoreference;
//
//import java.util.ArrayList;
//
//import model.EntityMention;
//import util.Common;
//import util.OntoCommon;
//
///*
// * use semantic knowledge
// */
//public class SemanticSieve extends Sieve {
//
//	@Override
//	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
//		for (EntityMention antecedent : orderedAntecedents) {
//			if (antecedent.isPronoun && !antecedent.head.equalsIgnoreCase(em.head)) {
//				continue;
//			}
//			// Entity antecedentCluster = antecedent.entity;
//			// Collections.sort(antecedentCluster.mentions);
//			// EntityMention representive;
//			// int maxScore = -1;
//			// for(int i=0;i<antecedentCluster.mentions.size();i++) {
//			// EntityMention candidate = antecedentCluster.mentions.get(i);
//			// int score = 0;
//			// if(candidate.isPronoun) {
//			// score += 10;
//			// } else if(candidate.isProperNoun) {
//			// score += 1000;
//			// } else {
//			// score += 100;
//			// }
//			// score += candidate.extent.length();
//			// if(score>maxScore) {
//			// representive = antecedentCluster.mentions.get(i);
//			// maxScore = score;
//			// }
//			// }
//			// boolean modiferCompatible1 = true;
//			// boolean modiferCompatible2 = true;
//			// ArrayList<String> curModifiers = em.modifyList;
//			// ArrayList<String> canModifiers = antecedent.modifyList;
//			// HashSet<String> curModifiersHash = new HashSet<String>();
//			// curModifiersHash.addAll(curModifiers);
//			// HashSet<String> canModifiersHash = new HashSet<String>();
//			// canModifiersHash.addAll(canModifiers);
//			//			
//			//			
//			// for(String canModifier : canModifiers) {
//			// if(!curModifiersHash.contains(canModifier)) {
//			// modiferCompatible1 = false;
//			// break;
//			// }
//			// }
//			// for(String curModifier : curModifiers) {
//			// if(!canModifiersHash.contains(curModifier)) {
//			// modiferCompatible2 = false;
//			// break;
//			// }
//			// }
//			// if(modiferCompatible1 || modiferCompatible2) {
//			boolean iWithi = OntoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
//			if (!iWithi) {
//				String semantic1[] = Common.semanticDic.get(antecedent.head);
//				String semantic2[] = Common.semanticDic.get(em.head);
//				if (semantic1 != null && semantic2 != null) {
//					for (String str1 : semantic1) {
//						for (String str2 : semantic2) {
//							if (str1.equals(str2) && !str1.endsWith("#") && antecedent.semClass.equals(em.semClass)
//									&& antecedent.subType.equals(em.subType) && !antecedent.head.equals(em.head)
//									&& Math.abs(antecedent.sentenceID - em.sentenceID) < 2
//									&& antecedent.semClass.equals(em.semClass) && antecedent.subType.equals(em.subType)
//							// && !(antecedent.isProperNoun && em.isProperNoun)
//							) {
//								if(ruleCoref.goldMaps.get(em)!=null && ruleCoref.goldMaps.get(em).contains(antecedent)) {
//									System.out.println(antecedent.extent + " " + antecedent.sentenceID + " " + em.extent + " " + em.sentenceID + " SemanticSieve");
//									System.out.println("RIGHT");
//									right ++;
//								} else {
//									System.out.println(antecedent.extent + " " + antecedent.sentenceID + " " + em.extent + " " + em.sentenceID + " SemanticSieve");
//									System.out.println("WRONG");
//									wrong ++;
//								}
//								if (ruleCoref.combine2Entities(antecedent, em, ruleCoref.sentences)) {
//									return;
//								}
//							}
//						}
//					}
//				}
//			}
//			// }
//		}
//	}
//	
//
//	public static int right = 0;
//	public static int wrong = 0;
//
//	@Override
//	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
//		if (antecedent.isPronoun && !antecedent.head.equalsIgnoreCase(em.head)) {
//			return false;
//		}
//		boolean iWithi = OntoCommon.isIWithI(antecedent, em, ruleCoref.sentences);
//		if (!iWithi) {
//			String semantic1[] = Common.semanticDic.get(antecedent.head);
//			String semantic2[] = Common.semanticDic.get(em.head);
//			if (semantic1 != null && semantic2 != null) {
//				for (String str1 : semantic1) {
//					for (String str2 : semantic2) {
//						if (str1.equals(str2) && !str1.endsWith("#") && antecedent.semClass.equals(em.semClass)
//								&& antecedent.subType.equals(em.subType) && !antecedent.head.equals(em.head)
//								&& Math.abs(antecedent.sentenceID - em.sentenceID) < 1
//								&& antecedent.semClass.equals(em.semClass) && antecedent.subType.equals(em.subType)
//						// && !(antecedent.isProperNoun && em.isProperNoun)
//						) {
//							System.out.println(antecedent.extent + " " + em.extent + " SemanticSieve ");
//							if(ruleCoref.compatible(antecedent, em, ruleCoref.sentences)) {
//								return true;
//							}
//						}
//					}
//				}
//			}
//		}
//		return false;
//	}
//}
