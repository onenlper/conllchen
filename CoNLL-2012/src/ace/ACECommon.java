package ace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import util.ChCommon;
import util.Common;
import ace.model.EventChain;
import ace.model.EventMention;
import ace.model.EventMentionArgument;
import ace.reader.APFXMLReader;
import ace.reader.EventChainReader;
import ace.reader.SGMXMLReader;
import ace.reader.TimeXLMReader;
import ace.reader.ValueXMLReader;
import crfMentionDetect.MentionInstance;

public class ACECommon {
	
	public static boolean goldEntityCorefTrain = true;
	public static boolean goldEntityCorefTest = true;

	public static boolean goldAttributeTrain = true;
	public static boolean goldAttributeTest = true;
	
	public static boolean goldEventMention = true;
	
	
	public static boolean goldEntityMention = false;
	public static boolean goldEventArgument = true;
	public static boolean goldSemantic = true;
	
	static HashMap<String, String> pos2 = Common.readFile2Map2("dict/10POSDIC");

	// identify zero and recognize its antecedent
	public static boolean isZeroPronoun(EventMention eventMention, CoNLLPart part,
			ArrayList<EntityMention> candidateMentions) {
		int position[] = ChCommon.getPosition(eventMention, part.getCoNLLSentences());
		CoNLLSentence sentence = part.getCoNLLSentences().get(position[0]);
		int wordPos = position[1];
		// if (wordPos < 2) {
		// eventMention.isZeroPronoun = -1;
		// return false;
		// }
		ArrayList<CoNLLWord> words = sentence.words;
		MyTreeNode leaf = sentence.getSyntaxTree().leaves.get(wordPos);
		if (leaf.parent.value.equalsIgnoreCase("vv") && leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent.getLeftSisters();
			boolean NP = false;
			for (MyTreeNode sister : beforeSisters) {
				if (sister.value.equalsIgnoreCase("np")) {
					NP = true;
				}
			}
			if (!NP) {
				eventMention.isZeroPronoun = 1;
				eventMention.zeroSubjects = new ArrayList<EntityMention>();
				for (int p = wordPos - 1; p >= 0; p--) {
					if (words.get(p).equals("，") || p == 1) {
						boolean got = false;
						ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
						int start = sentence.positions.get(0)[0];
						int end = eventMention.headCharStart - 1;

						for (EntityMention candidate : candidateMentions) {
							if (candidate.headCharStart >= start && candidate.headCharEnd <= end
									&& !candidate.semClass.equals("time")) {
								eventMention.zeroSubjects.add(candidate);
								got = true;
							}
						}
						Collections.sort(eventMention.zeroSubjects);
						Collections.reverse(eventMention.zeroSubjects);
						// 的
						ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
						MyTreeNode npAncestor = null;
						for (MyTreeNode node : ancestors) {
							if (node.value.equalsIgnoreCase("np")) {
								npAncestor = node;
								break;
							}
						}
						if (npAncestor != null) {
							ArrayList<MyTreeNode> leafs = npAncestor.getLeaves();
							MyTreeNode deLeaf = null;
							for (MyTreeNode tmp : leafs) {
								if (tmp.value.equals("的")) {
									deLeaf = tmp;
									break;
								}
							}
							if (deLeaf != null) {
								end = sentence.positions.get(leafs.get(leafs.size() - 1).leafIdx)[1];
								EntityMention zero = null;
								for (EntityMention candidate : candidateMentions) {
									if (candidate.headCharEnd == end) {
										zero = candidate;
										break;
									}
								}
								if (zero != null) {
									eventMention.zeroSubjects.add(0, zero);
									got = true;
								}
							}
						}
						// 中华民国 and 中华民国政府
						if (eventMention.zeroSubjects.size() != 0) {
							EntityMention zero = eventMention.zeroSubjects.get(0);
							for (EntityMention mention : mentions) {
								if (mention.start == zero.start && mention.end > zero.end) {
									eventMention.zeroSubjects.add(0, mention);
									zero = mention;
								}
							}
						}
						if (got) {
							return true;
						}
					}
				}
				return false;
			} else {
				eventMention.isZeroPronoun = -1;
				return false;
			}
		} else {
			return false;
		}
	}

	public static void identBVs(EventMention em, CoNLLPart part) {
		String posTag = em.posTag;
		if (em.head.length() == 1 && posTag.equalsIgnoreCase("VV")) {
			em.bvs.put(em.head, "BV");
		} else if (em.head.length() == 2) {
			String trigger = em.head;
			String str1 = Character.toString(trigger.charAt(0));
			String str2 = Character.toString(trigger.charAt(1));
			if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
				if (str2.equals("了")) {
					em.bvs.put(str1, "BV_comp");
				} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
					em.bvs.put(str1, "BV_verb");
				} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("N")) {
					em.bvs.put(str1, "BV_np");
				} else {
					em.bvs.put(str1, "BV_adj");
				}
			}
			if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
				if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
					em.bvs.put(str2, "verb_BV");
				} else if (pos2.containsKey(str1) && pos2.get(str1).startsWith("N")) {
					em.bvs.put(str2, "np_BV");
				} else {
					em.bvs.put(str2, "adj_BV");
				}
			}
		}
	}

	public static HashMap<EventMention, SemanticRole> readSemanticRole(String fileID) {
		fileID = fileID.replace("/users/yzcchen/ACL12/data/ACE2005/Chinese/",
				"/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese/");
		HashMap<EventMention, SemanticRole> semanticRoles = new HashMap<EventMention, SemanticRole>();
		ArrayList<String> srlInLines = Common.getLines(Common.changeSurffix(fileID, "slrin"));
		ArrayList<String> srlOutLines = Common.getLines(Common.changeSurffix(fileID, "slrout"));

		for (int i = 0; i < srlInLines.size();) {
			if (srlInLines.get(i).isEmpty()) {
				i++;
				continue;
			}
			ArrayList<SemanticRole> roles = new ArrayList<SemanticRole>();
			int semanticSize = srlOutLines.get(i).split("\\s+").length - 14;
			for (int j = 0; j < semanticSize; j++) {
				SemanticRole role = new SemanticRole();
				roles.add(role);
			}
			int predictIndex = 0;
			while (true) {
				String slrIn = srlInLines.get(i);
				String slrOut = srlOutLines.get(i);
				if (slrIn.trim().isEmpty()) {
					break;
				}

				String tokens[] = slrIn.split("\\s+");
				int start = Integer.parseInt(tokens[2]);
				int end = Integer.parseInt(tokens[3]);

				EventMention word = new EventMention();
				word.headCharStart = start;
				word.headCharEnd = end;

				tokens = slrOut.split("\\s+");
				word.head = tokens[1];

				if (tokens[12].equalsIgnoreCase("Y")) {
					roles.get(predictIndex).predict = word;
					predictIndex++;
				}
				for (int j = 0; j < semanticSize; j++) {
					String label = tokens[14 + j];
					if (!label.equals("_")) {
						EntityMention entityMention = new EntityMention();
						entityMention.headCharStart = start;
						entityMention.headCharEnd = end;
						entityMention.head = tokens[1];

						ArrayList<EntityMention> args = roles.get(j).args.get(label);
						if (args == null) {
							args = new ArrayList<EntityMention>();
							roles.get(j).args.put(label, args);
						}
						args.add(entityMention);
					}
				}
				i++;
			}
			for (SemanticRole role : roles) {
				semanticRoles.put(role.predict, role);
			}
		}
		return semanticRoles;
	}

	public static void assignSemanticRole(ArrayList<EventMention> eventMentions,
			ArrayList<EntityMention> entityMentions, HashMap<EventMention, SemanticRole> roles) {
		if (eventMentions != null) {
			HashMap<Integer, EntityMention> entityMap = new HashMap<Integer, EntityMention>();

			for (EntityMention mention : entityMentions) {
				entityMap.put(mention.headCharEnd, mention);
			}
			for (EventMention mention : eventMentions) {
				if (roles.containsKey(mention)) {
					SemanticRole role = roles.get(mention);
					HashMap<String, ArrayList<EntityMention>> args = role.args;
					HashMap<String, ArrayList<EntityMention>> newArgs = new HashMap<String, ArrayList<EntityMention>>();
					for (String key : args.keySet()) {
						ArrayList<EntityMention> news = new ArrayList<EntityMention>();
						for (EntityMention old : args.get(key)) {
							if (entityMap.containsKey(old.headCharEnd)) {
								news.add(entityMap.get(old.headCharEnd));
							}
						}
						if (news.size() != 0) {
							newArgs.put(key, news);
						}
					}
					mention.srlArgs = newArgs;
				}
			}
		}
	}

	private static HashMap<String, HashMap<String, EventMention>> systemEventMentions;

	public static HashMap<String, HashMap<String, EventMention>> getSystemEventMentions() {
		if (systemEventMentions == null) {
			systemEventMentions = readSystemEventMention();
		}
		return systemEventMentions;
	}

	private static HashMap<String, ArrayList<EntityMention>> getTimeExpressions() {
		if (timeExpressions == null) {
			timeExpressions = ACECorefCommon.getMentionsFromCRFFile(Common.getLines("ACE_" + Common.part),
					"/users/yzcchen/tool/CRF/CRF++-0.54/yy_time" + Common.part);
		}
		return timeExpressions;
	}

	public static ArrayList<EntityMention> getTimeExpression(CoNLLPart part) {
		ArrayList<EntityMention> mentions = getTimeExpressions().get(part.getDocument().getFilePath());
		for (EntityMention mention : mentions) {
			ACECorefCommon.assingStartEnd(mention, part);
		}
		return mentions;
	}

	private static HashMap<String, ArrayList<EntityMention>> timeExpressions;

	private static HashMap<String, ArrayList<EntityMention>> getValueExpressions() {
		if (valueExpressions == null) {
			valueExpressions = ACECorefCommon.getMentionsFromCRFFile(Common.getLines("ACE_" + Common.part),
					"/users/yzcchen/tool/CRF/CRF++-0.54/yy_value" + Common.part);
		}
		return valueExpressions;
	}

	private static HashMap<String, ArrayList<EntityMention>> valueExpressions;

	public static ArrayList<EntityMention> getValueExpression(CoNLLPart part) {
		ArrayList<EntityMention> mentions = getValueExpressions().get(part.getDocument().getFilePath());
		for (EntityMention mention : mentions) {
			ACECorefCommon.assingStartEnd(mention, part);
		}
		return mentions;
	}

	public static HashMap<String, HashMap<String, EventMention>> loadSystemEventMentions() {
		HashMap<String, HashMap<String, EventMention>> systemEMses = readSystemEventMention();
		// try {
		// loadSystemAtrribute("polarity", systemEMses);
		// loadSystemAtrribute("modality", systemEMses);
		// loadSystemAtrribute("genericity", systemEMses);
		// loadSystemAtrribute("tense", systemEMses);
		// } catch (IllegalArgumentException e) {
		// e.printStackTrace();
		// } catch (SecurityException e) {
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// e.printStackTrace();
		// } catch (NoSuchFieldException e) {
		// e.printStackTrace();
		// }
		return systemEMses;
	}

	public static void outputResult(HashMap<String, HashMap<String, EventMention>> allMentions, String filename) {
		System.out.println(filename);
		ArrayList<String> lines = new ArrayList<String>();
		for (String file : allMentions.keySet()) {
			for (String key : allMentions.get(file).keySet()) {
				EventMention mention = allMentions.get(file).get(key);
				ArrayList<Object> atts = new ArrayList<Object>();
				String file2 = file.substring(0, file.length() - 4).replace(
						"/users/yzcchen/ACL12/data/ACE2005/Chinese",
						"/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese");
				atts.add(file2);
				atts.add(mention.headCharStart);
				atts.add(mention.headCharEnd);
				atts.add(mention.confidence);
				atts.add(mention.type);
				atts.add(mention.typeConfidence);
				atts.add(mention.subType);
				atts.add(mention.subTypeConfidence);
				atts.add("-1");
				atts.add(mention.inferFrom);
				atts.add("-1");
				atts.add("-1");
				atts.add("-1");

				for (double confidence : mention.typeConfidences) {
					atts.add(Double.toString(confidence));
				}

				lines.add(convert(atts));
				// System.out.println(document.content.substring(start,
				// end+1).replace("\n", "").replace(" ", "") + "\t" +
				// mention.confidence);
				for (EventMentionArgument argument : mention.eventMentionArguments) {
					// System.out.println(document.content.substring(start2,
					// end2+1).replace("\n", "").replace(" ", "") + "#" +
					// argument.getRole());

					atts = new ArrayList<Object>();
					atts.add(file2);
					atts.add(mention.headCharStart);
					atts.add(mention.headCharEnd);
					atts.add(mention.confidence);
					atts.add(mention.type);
					atts.add(mention.typeConfidence);
					atts.add(mention.subType);
					atts.add(mention.subTypeConfidence);
					atts.add(argument.getStart());
					atts.add(argument.getEnd());
					atts.add(argument.confidence);
					atts.add(argument.getRole());
					atts.add(argument.roleConfidence);

					// role confidences
					for (double confidence : argument.roleConfidences) {
						atts.add(Double.toString(confidence));
					}

					lines.add(convert(atts));
				}
				// System.out.println("==============");
			}
		}
		Common.outputLines(lines, filename);
	}

	public static String convert(ArrayList<Object> atts) {
		StringBuilder sb = new StringBuilder();
		for (Object att : atts) {
			sb.append(att.toString()).append(" ");
		}
		return sb.toString().trim();
	}

	private static HashMap<String, HashMap<String, EventMention>> polarityMaps;
	private static HashMap<String, HashMap<String, EventMention>> tenseMaps;
	private static HashMap<String, HashMap<String, EventMention>> generecityMaps;
	private static HashMap<String, HashMap<String, EventMention>> modalityMaps;

	public static HashMap<String, HashMap<String, EventMention>> getPolarityMaps() {
		if (polarityMaps == null) {
			try {
				polarityMaps = getSystemAtrribute("polarity");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		return polarityMaps;
	}

	public static HashMap<String, HashMap<String, EventMention>> getTenseMaps() {
		if (tenseMaps == null) {
			try {
				tenseMaps = getSystemAtrribute("tense");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		return tenseMaps;
	}

	public static HashMap<String, HashMap<String, EventMention>> getGenerecityMaps() {
		if (generecityMaps == null) {
			try {
				generecityMaps = getSystemAtrribute("genericity");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		return generecityMaps;
	}

	public static HashMap<String, HashMap<String, EventMention>> getModalityMaps() {
		if (modalityMaps == null) {
			try {
				modalityMaps = getSystemAtrribute("modality");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		return modalityMaps;
	}

	public static void assignSystemAttribute(String fileID, EventMention mention) {
		mention.polarity = getPolarityMaps().get(fileID).get(mention.toString()).polarity;
		mention.tense = getTenseMaps().get(fileID).get(mention.toString()).tense;
		mention.genericity = getGenerecityMaps().get(fileID).get(mention.toString()).genericity;
		mention.modality = getModalityMaps().get(fileID).get(mention.toString()).modality;
	}

	public static void assginSystemArguments(String fileID, EventMention mention) {
		// TODO
		mention.eventMentionArguments = getSystemEventMentions().get(fileID).get(mention.toString()).eventMentionArguments;
	}
	
	public static HashMap<String, HashMap<String, EventMention>> getSystemAtrribute(String attribute)
			throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		HashMap<String, HashMap<String, EventMention>> systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		for (int folder = 0; folder < 5; folder++) {
			String f = Integer.toString(folder);
			String emFn = "/users/yzcchen/workspace/NAACL2013-B/src/data/chinese_" + attribute + "_test_em" + f;
			String predicFn = "/users/yzcchen/tool/maxent/bin/test_" + attribute + ".txt" + f;
			if (goldEventMention || Common.train) {
				emFn = "/users/yzcchen/workspace/NAACL2013-B/src/data/goldEventMentions/chinese_" + attribute + "_test_em" + f;
				predicFn = "/users/yzcchen/tool/maxent/bin/goldEventMentions/test_" + attribute + ".txt" + f;
			}
			
			ArrayList<String> emLines = Common.getLines(emFn);
			ArrayList<String> predictLines = Common.getLines(predicFn);
			for (int i = 0; i < emLines.size(); i++) {
				String predictLine = predictLines.get(i);
				String emLine = emLines.get(i);

				String tokens[] = emLine.split("\\s+");
				String file = tokens[0].replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
						"/users/yzcchen/ACL12/data/ACE2005/Chinese")
						+ ".sgm";
				int start = Integer.valueOf(tokens[1]);
				int end = Integer.valueOf(tokens[2]);
				EventMention em = new EventMention();
				em.headCharStart = start;
				em.headCharEnd = end;

				tokens = predictLine.split("\\s+");
				String label = "";
				double maxVal = -1;
				for (int k = 0; k < tokens.length / 2; k++) {
					String l = tokens[k * 2];
					double val = Double.valueOf(tokens[k * 2 + 1]);
					if (val > maxVal) {
						label = l;
						maxVal = val;
					}
				}
				HashMap<String, EventMention> map = systemEMses.get(file);
				if (map == null) {
					map = new HashMap<String, EventMention>();
					systemEMses.put(file, map);
				}
				map.put(em.toString(), em);
				em.getClass().getField(attribute).set(em, label);
			}
		}
		return systemEMses;
	}

	public static void loadSystemAtrribute(String attribute, HashMap<String, HashMap<String, EventMention>> systemEMses)
			throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		ArrayList<String> emLines = Common.getLines("/users/yzcchen/workspace/NAACL2013-B/src/data/chinese_"
				+ attribute + "_test_em" + Common.part);
		ArrayList<String> predictLines = Common.getLines("/users/yzcchen/tool/maxent/bin/test_" + attribute + ".txt"
				+ Common.part);
		for (int i = 0; i < emLines.size(); i++) {
			String predictLine = predictLines.get(i);
			String emLine = emLines.get(i);

			String tokens[] = emLine.split("\\s+");
			String file = tokens[0].replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
					"/users/yzcchen/ACL12/data/ACE2005/Chinese")
					+ ".sgm";
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.headCharStart = start;
			em.headCharEnd = end;

			tokens = predictLine.split("\\s+");
			String label = "";
			double maxVal = -1;
			for (int k = 0; k < tokens.length / 2; k++) {
				String l = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val > maxVal) {
					label = l;
					maxVal = val;
				}
			}
			EntityMention set = systemEMses.get(file).get(em.toString());
			set.getClass().getField(attribute).set(set, label);
		}
	}

	public static List<String> subTypes = Arrays.asList("Start-Position", "Elect", "Transfer-Ownership", "Extradite",
			"Declare-Bankruptcy", "Marry", "Demonstrate", "Start-Org", "End-Org", "Appeal", "Trial-Hearing", "Attack",
			"Sue", "Convict", "Meet", "Pardon", "Charge-Indict", "Divorce", "End-Position", "Nominate", "Fine",
			"Release-Parole", "Transfer-Money", "Phone-Write", "Merge-Org", "Die", "Arrest-Jail", "Be-Born", "Injure",
			"Transport", "Sentence", "Acquit", "Execute", "null");

	public static double svmTh = 0;

	private static HashMap<String, HashMap<String, EventMention>> readSystemEventMention() {
		if (pipelineResults == null) {
			pipelineResults = readSystemPipelineEventMention();
		}
		HashMap<String, HashMap<String, EventMention>> eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		for (int folder = 0; folder < 5; folder++) {
			
			String inter = "joint_svm_systemEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			if(ACECommon.goldEventMention && ACECommon.goldEntityMention && ACECommon.goldSemantic) {
				inter = "joint_svm_goldEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			} else if(ACECommon.goldEventMention && ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
				inter = "joint_svm_goldEventMention_systemArgument_goldEntityMentions_systemSemantic//";
			} else if(ACECommon.goldEventArgument && !ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
				inter = "joint_svm_goldEventMention_systemArgument_systemEntityMentions_systemSemantic///";
			} else if(!ACECommon.goldEventArgument && ACECommon.goldEntityMention && ACECommon.goldSemantic) {
				inter = "joint_svm_systemEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			} else if(!ACECommon.goldEventArgument && !ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
				inter = "joint_svm_systemEventMention_systemArgument_systemEntityMentions_systemSemantic/";
			} 
			
			String filename = "/users/yzcchen/workspace/NAACL2013-B/src/" + inter + "/result" + Integer.toString(folder);
			
			
			ArrayList<String> lines = Common.getLines(filename);
			int size = 0;
			HashMap<String, PlainText> documentCache = new HashMap<String, PlainText>();

			for (String line : lines) {
				String tokens[] = line.split("\\s+");

				String fileID = tokens[0].replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
						"/users/yzcchen/ACL12/data/ACE2005/Chinese")
						+ ".sgm";

				PlainText document = documentCache.get(fileID);
				if (document == null) {
					document = ACECommon.getPlainText(fileID);
					documentCache.put(fileID, document);
				}

				HashMap<String, EventMention> eventMentions = eventMentionsMap.get(fileID);
				if (eventMentions == null) {
					eventMentions = new HashMap<String, EventMention>();
					eventMentionsMap.put(fileID, eventMentions);
				}

				int emStart = Integer.parseInt(tokens[1]);
				int emEnd = Integer.parseInt(tokens[2]);
				double emConfidence = Double.parseDouble(tokens[3]);
				String type = tokens[4];
				double typeConfidence = Double.parseDouble(tokens[5]);
				String subType = tokens[6];

				double subTypeConfidence = Double.parseDouble(tokens[7]);

				EventMention temp = new EventMention();
				temp.headCharStart = emStart;
				temp.headCharEnd = emEnd;
				temp.head = document.content.substring(emStart, emEnd + 1).replace("\n", "").replace(" ", "");
				temp.confidence = emConfidence;
				temp.type = type;
				temp.typeConfidence = typeConfidence;
				temp.subType = subType;

				if (temp.subType.equalsIgnoreCase("null") || temp.confidence < svmTh) {
					continue;
				}

				if (temp.subType.equalsIgnoreCase("null")) {
					temp.subType = pipelineResults.get(fileID).get(temp.toString()).subType;
					System.err.println("GE: " + temp.subType);
				}

				temp.subTypeConfidence = subTypeConfidence;

				EventMention eventMention = eventMentions.get(temp.toString());
				if (eventMention == null) {
					eventMention = temp;
					eventMentions.put(temp.toString(), eventMention);
					size++;
				}

				if (Integer.parseInt(tokens[8]) == -1) {
					ArrayList<Double> confidences = new ArrayList<Double>();
					for (int k = 13; k < tokens.length; k++) {
						confidences.add(Double.valueOf(tokens[k]));
					}
					// eventMention.typeConfidences = confidences;
					eventMention.inferFrom = tokens[9];
					continue;
				}

				EventMentionArgument argument = new EventMentionArgument();
				argument.setStart(Integer.parseInt(tokens[8]));
				argument.setEnd(Integer.parseInt(tokens[9]));
				argument.confidence = Double.parseDouble(tokens[10]);
				argument.setRole(tokens[11]);
				if (tokens[11].equalsIgnoreCase("null")) {
					continue;
				}
				argument.roleConfidence = Double.parseDouble(tokens[12]);
				argument.setEventMention(eventMention);
				ArrayList<Double> confidences = new ArrayList<Double>();
				for (int k = 13; k < tokens.length; k++) {
					confidences.add(Double.valueOf(tokens[k]));
				}
				argument.roleConfidences = confidences;
				eventMention.getEventMentionArguments().add(argument);
			}
		}
		return eventMentionsMap;
	}

	public static HashMap<String, HashMap<String, EventMention>> pipelineResults;

	public static HashMap<String, HashMap<String, EventMention>> readSystemPipelineEventMention() {
		String filename = "/users/yzcchen/workspace/NAACL2013-B/src/pipe_svm/result" + Common.part;
		HashMap<String, HashMap<String, EventMention>> eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines(filename);

		int size = 0;

		HashMap<String, PlainText> documentCache = new HashMap<String, PlainText>();

		for (String line : lines) {
			String tokens[] = line.split("\\s+");

			String fileID = tokens[0].replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
					"/users/yzcchen/ACL12/data/ACE2005/Chinese")
					+ ".sgm";

			PlainText document = documentCache.get(fileID);
			if (document == null) {
				document = ACECommon.getPlainText(fileID);
				documentCache.put(fileID, document);
			}

			HashMap<String, EventMention> eventMentions = eventMentionsMap.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionsMap.put(fileID, eventMentions);
			}

			int emStart = Integer.parseInt(tokens[1]);
			int emEnd = Integer.parseInt(tokens[2]);
			double emConfidence = Double.parseDouble(tokens[3]);
			String type = tokens[4];
			double typeConfidence = Double.parseDouble(tokens[5]);
			String subType = tokens[6];

			double subTypeConfidence = Double.parseDouble(tokens[7]);

			EventMention temp = new EventMention();
			temp.headCharStart = emStart;
			temp.headCharEnd = emEnd;
			temp.head = document.content.substring(emStart, emEnd + 1).replace("\n", "").replace(" ", "");
			temp.confidence = emConfidence;
			temp.type = type;
			temp.typeConfidence = typeConfidence;
			temp.subType = subType;

			if (temp.subType.equalsIgnoreCase("null") || temp.confidence < svmTh) {
				// continue;
			}

			temp.subTypeConfidence = subTypeConfidence;

			EventMention eventMention = eventMentions.get(temp.toString());
			if (eventMention == null) {
				eventMention = temp;
				eventMentions.put(temp.toString(), eventMention);
				size++;
			}

			if (Integer.parseInt(tokens[8]) == -1) {
				ArrayList<Double> confidences = new ArrayList<Double>();
				for (int k = 13; k < tokens.length; k++) {
					confidences.add(Double.valueOf(tokens[k]));
				}
				// eventMention.typeConfidences = confidences;
				eventMention.inferFrom = tokens[9];
				continue;
			}

			EventMentionArgument argument = new EventMentionArgument();
			argument.setStart(Integer.parseInt(tokens[8]));
			argument.setEnd(Integer.parseInt(tokens[9]));
			argument.confidence = Double.parseDouble(tokens[10]);
			argument.setRole(tokens[11]);
			if (tokens[11].equalsIgnoreCase("null")) {
				continue;
			}
			argument.roleConfidence = Double.parseDouble(tokens[12]);
			argument.setEventMention(eventMention);
			ArrayList<Double> confidences = new ArrayList<Double>();
			for (int k = 13; k < tokens.length; k++) {
				confidences.add(Double.valueOf(tokens[k]));
			}
			argument.roleConfidences = confidences;
			eventMention.getEventMentionArguments().add(argument);
		}
		return eventMentionsMap;
	}

	public static List<String> roles = Arrays.asList("Crime", "Victim", "Origin", "Adjudicator", "Time-Holds",
			"Time-Before", "Target", "Time-At-End", "Org", "Recipient", "Vehicle", "Plaintiff", "Attacker", "Place",
			"Buyer", "Money", "Giver", "Beneficiary", "Agent", "Time-Ending", "Time-After", "Time-Starting", "Seller",
			"Defendant", "Time-Within", "Artifact", "Time-At-Beginning", "Prosecutor", "Sentence", "Price", "Position",
			"Instrument", "Destination", "Person", "Entity");

	public static HashMap<String, String> filePathFolderMap = new HashMap<String, String>();
	
	public static ArrayList<String> getFileList(String posts[]) {
		ArrayList<String> fileLists = new ArrayList<String>();
		for (String post : posts) {
			ArrayList<String> list = Common.getLines("ACE_" + post);
			for (String str : list) {
				fileLists.add(str);
				filePathFolderMap.put(str, post);
			}
		}
		return fileLists;
	}

	public static ArrayList<EventChain> readGoldEventChain(String fileID) {
		ArrayList<EventChain> eventChains2 = new ArrayList<EventChain>();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(new File(Common.changeSurffix(fileID, "apf.xml")));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			EventChainReader reader = new EventChainReader(eventChains2);
			sp.parse(new InputSource(inputStream), reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return eventChains2;
	}

	// public static String aceDataPath =
	// "/users/yzcchen/ACL12/data/ACE2005/Chinese";
	public static String aceDataPath;

	// public static String aceModelPath = "/users/yzcchen/ACL12/model/ACE";
	public static String aceModelPath;

	public static String getRelateApf(String sgm) {
		return sgm.substring(0, sgm.length() - 4) + ".apf.xml";
	}

	public static ArrayList<Entity> getEntities(String apfFn) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		try {
			InputStream inputStream = new FileInputStream(new File(apfFn));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			APFXMLReader reader = new APFXMLReader(entities);
			sp.parse(new InputSource(inputStream), reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entities;
	}

	public static PlainText getPlainText(String sgmFn) {
		sgmFn = Common.changeSurffix(sgmFn, "sgm");
		PlainText sgm = new PlainText();
		// fix the bug: there may be a newline character in the head of file
		try {
			BufferedReader br = new BufferedReader(new FileReader(sgmFn));
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					sgm.content += "\n";
				} else {
					break;
				}
			}
			br.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			InputStream inputStream = new FileInputStream(new File(sgmFn));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			SGMXMLReader reader = new SGMXMLReader(sgm);
			sp.parse(new InputSource(inputStream), reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sgm.content = sgm.content.replace("&", "&amp;").replace("•", "&#8226;");
		return sgm;
	}

	public static ArrayList<String> getACEFiles(String postfix) {
		ArrayList<String> filenames = new ArrayList<String>();
		String folders[] = { File.separator + "bn", File.separator + "nw", File.separator + "wl" };
		for (String folder : folders) {
			String subFolder = aceDataPath + folder + File.separator + "adj" + File.separator;
			File files[] = (new File(subFolder)).listFiles();
			for (File file : files) {
				if (file.getName().endsWith(postfix)) {
					filenames.add(file.getAbsolutePath());
				}
			}
		}
		return filenames;
	}

	public static ArrayList<ArrayList<Element>> getPredictNerElements(ArrayList<String> files, String crfFilePath) {
		ArrayList<ArrayList<Element>> elementses = new ArrayList<ArrayList<Element>>();
		elementses = ACECorefCommon.getSemanticsFromCRFFile(files, crfFilePath);
		return elementses;
	}

	public static ArrayList<EntityMention> getTimeMentions(String apfFn) {
		apfFn = Common.changeSurffix(apfFn, "apf.xml");
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<Entity> entities = new ArrayList<Entity>();
		try {
			InputStream inputStream = new FileInputStream(new File(apfFn));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			TimeXLMReader reader = new TimeXLMReader(entities);
			sp.parse(new InputSource(inputStream), reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Entity entity : entities) {
			mentions.addAll(entity.mentions);
		}
		return mentions;
	}

	public static ArrayList<EntityMention> getValueMentions(String apfFn) {
		apfFn = Common.changeSurffix(apfFn, "apf.xml");
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<Entity> entities = new ArrayList<Entity>();
		try {
			InputStream inputStream = new FileInputStream(new File(apfFn));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			ValueXMLReader reader = new ValueXMLReader(entities);
			sp.parse(new InputSource(inputStream), reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Entity entity : entities) {
			mentions.addAll(entity.mentions);
		}
		return mentions;
	}

	// public static ArrayList<Element> getPredictNerElements(String sgmFn,
	// PlainText sgm) {
	// ArrayList<Element> elements = new ArrayList<Element>();
	// int startIdx = 0;
	// try {
	// int pos = sgmFn.lastIndexOf(File.separator);
	// String name = sgmFn.substring(pos, sgmFn.length()-3);
	// BufferedReader br = Common.getBr(aceDataPath+
	// "/utf8ner/"+name+"source.ner");
	// StringBuilder sb = new StringBuilder();
	// String line;
	// while((line=br.readLine())!=null) {
	// sb.append(line);
	// }
	// br.close();
	// String nerContent = sb.toString();
	// nerContent = nerContent.replace("&", "&amp;").replace("•", "&#8226;");
	// for(int i=0;i<nerContent.length();i++) {
	// if(nerContent.charAt(i)=='\n' ||
	// nerContent.charAt(i)=='\r'||nerContent.charAt(i)==' '||nerContent.charAt(i)==' ')
	// {
	// continue;
	// }
	// if(nerContent.charAt(i)=='[') {
	// int start = i+1;
	// while(nerContent.charAt(i)!=']') {
	// i++;
	// }
	// int end = i;
	// int[] position = Common.findPosition(sgm.content,
	// nerContent.substring(start,end), startIdx);
	// startIdx = position[1] + 1;
	// StringBuilder sbLabel = new StringBuilder();
	// i++;
	// while(nerContent.charAt(i)!=' ') {
	// i++;
	// sbLabel.append(nerContent.charAt(i));
	// }
	// String label = sbLabel.toString().trim();
	// Element element = new Element(position[0], position[1], label);
	// elements.add(element);
	// } else {
	// try {
	// while(sgm.content.charAt(startIdx)!=nerContent.charAt(i)) {
	// startIdx++;
	// } }
	// catch (Exception e) {
	// System.out.println(nerContent.charAt(i));
	// System.out.println("=====");
	// System.out.println(sgm.content.charAt(startIdx));
	// }
	// }
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// return elements;
	// }

	public static void genACENerFea(ArrayList<MentionInstance> instances, String sgmFn, ArrayList<Element> elements) {
		for (Element element : elements) {
			((MentionInstance) instances.get(element.getStart())).setNerFea("B-" + element.getContent());
			for (int m = element.getStart() + 1; m <= element.getEnd(); m++) {
				((MentionInstance) instances.get(m)).setNerFea("I-" + element.getContent());
			}
		}
	}

}
