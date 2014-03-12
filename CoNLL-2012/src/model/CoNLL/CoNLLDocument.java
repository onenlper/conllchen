package model.CoNLL;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import model.Element;
import model.Entity;
import model.EntityMention;
import util.Common;
import util.OntoCommon;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

/*
 * CoNLL-2012 Format Document
 */
public class CoNLLDocument {
	
	public boolean addZero;
	
	public enum DocType {
		Article, Conversation
	}
	
	private DocType type;
	
	public DocType getType() {
		return type;
	}

	public void setType(DocType type) {
		this.type = type;
	}

	public static WordnetStemmer getStemmer() {
		return stemmer;
	}

	public static void setStemmer(WordnetStemmer stemmer) {
		CoNLLDocument.stemmer = stemmer;
	}

	private ArrayList<String> rawLines;
	
	private String documentID;
	
	private String filePath;
	
	private String filePrefix;
	
	private String language;
	
	private ArrayList<CoNLLPart> parts;
	
	public OntoCommon ontoCommon;
	
	public CoNLLDocument() {
		this.parts = new ArrayList<CoNLLPart>();
		this.ontoCommon = new OntoCommon("chinese");
	}
	
	public CoNLLDocument(String path) {
		path = path.replace("v6", "v4").replace("_gold_parse_conll", "_auto_conll");
		if(!(new File(path).exists())) {
			path = path.replace("v5", "v4").replace("v6", "v4");
		}
		
		if(!(new File(path).exists())) {
			path = path.replace("_auto_conll", "_gold_conll");
		}
		this.setType(DocType.Article);
		this.filePath = path;
		if(filePath.contains("chinese") || filePath.contains("chi")) {
			this.language = "chinese";
		} else if (filePath.contains("english")) {
			this.language = "english";
			if(stemmer==null) {
				loadStemmer();
			}
		} else if (filePath.contains("arabic")) {
			this.language = "arabic";
		} else {
			//default english
			this.language = "english";
			if(stemmer==null) {
				loadStemmer();
			}
//			System.err.println("Unsupport language");
//			Exception e = new Exception();
//			e.printStackTrace();
//			System.exit(1);
		}
		this.ontoCommon = new OntoCommon(this.getLanguage());
		this.rawLines = Common.getLines(path);
		int i = path.lastIndexOf(".");
		if(i!=-1) {
			this.filePrefix = path.substring(0, i);
		}
		this.parts = new ArrayList<CoNLLPart>();
		this.parseFile();
	}
	
	public static WordnetStemmer stemmer;
	
	private static void loadStemmer() {
		String path = Common.wordnet + File.separator + "dict";
		try {
			URL url = new URL("file", null , path);
			IDictionary dict = new Dictionary(url);
			dict.open();
			stemmer = new WordnetStemmer(dict);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * parse CoNLL format file
	 */
	private void parseFile() {
		CoNLLPart part = null;
		CoNLLSentence sentence = null;
		StringBuilder parseBits = null;
		StringBuilder sentenceStr = null;
		int wordIdx=0;
		String previousSpeaker = "";
		for(String line : rawLines) {
//			System.out.println(line);
			if(line.startsWith("#end document")) {
				part.postProcess();
				continue;
			}
			// new part
			if(line.startsWith("#begin document")) {
				part = new CoNLLPart();
				part.label = line;
				wordIdx = 0;
				part.setDocument(this);
				this.parts.add(part);
				sentence = null;
				continue;
			}
			// end of one sentence
			if(line.trim().isEmpty()) {
				sentence.setSentence(sentenceStr.toString());
				sentence.setWordsCount(sentence.words.size());
				String parseTree = parseBits.toString().replace("(", " (").trim();
				if(parseTree.startsWith("( (")) {
					parseTree = "(TOP " + parseTree.substring(1);
				}
				sentence.addSyntaxTree(parseTree);
				if(this.language.equalsIgnoreCase("english")) {
					sentence.addStdTree(parseTree);
				}
				part.addSentence(sentence);
				sentence = null;
				continue;
			}
			if(sentence==null) {
				sentence = new CoNLLSentence();
				parseBits = new StringBuilder();
				sentenceStr = new StringBuilder();
			}
			String tokens[] = line.split("\\s+");
			CoNLLWord word = new CoNLLWord();
			
			word.sourceLine = line;
			
			// 1 	Document ID
			this.documentID = tokens[0];
			// 2 	Part number
			part.setPartID(Integer.valueOf(tokens[1]));
			part.setPartName(part.getDocument().getDocumentID().replace("/", "-") + "_" + part.getPartID());
			// 3 	Word number
			// 5 	Part-of-Speech
			String pos = tokens[4];
			word.setPosTag(pos);
			// 4 	Word itself
			String wordStr = tokens[3];
			String stem = wordStr;
			// if language is english, use  wordnet to do stem
			if(this.language.equalsIgnoreCase("english")) {
				List<String> stems = new ArrayList<String>();
				if(pos.equalsIgnoreCase("jj") || pos.equalsIgnoreCase("jjs") || pos.equalsIgnoreCase("JJR")) {
					stems = stemmer.findStems(wordStr, POS.ADJECTIVE);
				} else if(pos.equalsIgnoreCase("rb") || pos.equalsIgnoreCase("RBR") || pos.equalsIgnoreCase("RBS")
						|| pos.equalsIgnoreCase("WRB")) {
					stems = stemmer.findStems(wordStr, POS.ADVERB);
				} else if(pos.equalsIgnoreCase("NN") || pos.equalsIgnoreCase("NNPS") || pos.equalsIgnoreCase("WP$")
						|| pos.equalsIgnoreCase("PRP") || pos.equalsIgnoreCase("WDT") || pos.equalsIgnoreCase("WP")
						|| pos.equalsIgnoreCase("PRP$") || pos.equalsIgnoreCase("NNS") || pos.equalsIgnoreCase("NNP")) {
					try {
					stems = stemmer.findStems(wordStr, POS.NOUN);}
					catch (Exception e) {
						
					}
				} else if(pos.equalsIgnoreCase("VBN") || pos.equalsIgnoreCase("VB") || pos.equalsIgnoreCase("VBP")
						|| pos.equalsIgnoreCase("VBZ") || pos.equalsIgnoreCase("VBG") || pos.equalsIgnoreCase("VBD")) {
					stems = stemmer.findStems(wordStr, POS.VERB);
				} 
				if(stems!=null && stems.size()!=0) {
					stem = stems.get(0);
				}
				word.setOrig(wordStr);
				word.setWord(stem.toLowerCase());
			} else if(this.language.equalsIgnoreCase("arabic")) {
				String ts[] = wordStr.split("#");
				word.setWord(ts[0]);
				word.setOrig(wordStr);
				if(ts.length==4) {
					word.setArLemma(ts[1]);
					word.setArUnBuck(ts[2]);
					word.setArBuck(ts[3]);
				}
			} else if(this.language.equalsIgnoreCase("chinese")) {
				word.setOrig(wordStr);
				word.setWord(stem.toLowerCase());
			}
			
			// 6 	Parse bit
			parseBits.append(tokens[5].replace("*", " ("+pos+" "+wordStr.toLowerCase()+")").replace("(", " ("));
			// 7 	Predicate lemma
			word.setPredicateLemma(tokens[6]);
			// 8 	Predicate Frameset ID
			word.setPredicateFramesetID(tokens[7]);
			// 9 	Word sense
			word.setWordSense(tokens[8]);
			//10 	Speaker/Author
			if(!previousSpeaker.isEmpty() && !tokens[9].equalsIgnoreCase(previousSpeaker)) {
				this.setType(DocType.Conversation);
			}
			previousSpeaker = tokens[9];
			sentence.setSpeaker(tokens[9]);
			word.speaker = tokens[9];
			//11 	Named Entities
			word.setRawNamedEntity(tokens[10]);
			//12	Predicate Arguments
			StringBuilder argument = new StringBuilder();
			for(int i=11;i<tokens.length-1;i++) {
				argument.append(tokens[i]).append(" ");
			}
			word.setPredicateArgument(argument.toString().trim());
			//13	Coreference
			word.setRawCoreference(tokens[tokens.length-1]);
			sentence.addWord(word);
			word.setIndex(wordIdx);
			sentenceStr.append(wordStr).append(" ");
			wordIdx++;
		}
	}
	
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ArrayList<String> getRawLines() {
		return rawLines;
	}

	public void setRawLines(ArrayList<String> rawLines) {
		this.rawLines = rawLines;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePrefix() {
		return filePrefix;
	}

	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	public ArrayList<CoNLLPart> getParts() {
		return parts;
	}

	public void setParts(ArrayList<CoNLLPart> parts) {
		this.parts = parts;
	}
	
	/*
	 * Test CoNLL format converter
	 */
	public static void main(String args[]) {
		String conllPath = "";
//		conllPath = "/users/yzcchen/CoNLL-2012/conll-2012/v1/data/train/data/chinese/annotations/nw/xinhua/00/chtb_0001.v1_gold_conll";
		conllPath = "/users/yzcchen/CoNLL-2012/conll-2012/v1/data/train/data/english/annotations/bc/cctv/00/cctv_0001.v1_auto_conll";
		CoNLLDocument document = new CoNLLDocument(conllPath);
		System.out.println("Document ID: " + document.getDocumentID());
		for(CoNLLPart part : document.parts) {
			System.out.println("Part ID: " + part.getPartID());
			System.out.println("===================sentences===================");
			for(CoNLLSentence sentence : part.getCoNLLSentences()) {
				System.out.println(sentence.getSentence());
			}
		}
		for(CoNLLPart part : document.parts) {
			System.out.println("Part ID: " + part.getPartID());
			System.out.println("===================named entities================");
			for(Element element : part.getNameEntities()) {
				System.out.println(element);
			}
		}
		for(CoNLLPart part : document.parts) {
			System.out.println("Part ID: " + part.getPartID());
			System.out.println("===================coreference chains================");
			for(Entity entity : part.getChains()) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention em : entity.mentions) {
					sb.append(em).append(" ");
				}
				System.out.println(sb.toString());
			}
		}
	}
}
