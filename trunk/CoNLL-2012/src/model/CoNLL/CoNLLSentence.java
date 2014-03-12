package model.CoNLL;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import model.syntaxTree.MyTree;
import util.Common;
import ace.SemanticRole;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeReader;

public class CoNLLSentence {
	
	public ArrayList<SemanticRole> roles = new ArrayList<SemanticRole>();
	
	public String getText() {
		StringBuilder sb = new StringBuilder();
		for(CoNLLWord word : this.words) {
			sb.append(word.word).append(" ");
		}
		return sb.toString().trim();
	}
	
	public String getText(int k) {
		StringBuilder sb = new StringBuilder();
		for(int i=k;i<this.words.size();i++) {
			sb.append(this.words.get(k).word).append(" ");
		}
		return sb.toString();
	}
	
	private int startWordIdx;

	private int endWordIdx;

	private String sentence = "";

	private int sentenceIdx;
	
	public int getSentenceIdx() {
		return sentenceIdx;
	}

	public void setSentenceIdx(int sentenceIdx) {
		this.sentenceIdx = sentenceIdx;
	}

	public int getStartWordIdx() {
		return startWordIdx;
	}

	public void setStartWordIdx(int startWordIdx) {
		this.startWordIdx = startWordIdx;
	}

	public MyTree getSyntaxTree() {
		return syntaxTree;
	}

	public void setSyntaxTree(MyTree syntaxTree) {
		this.syntaxTree = syntaxTree;
	}

	public ArrayList<String> getDepends() {
		return depends;
	}

	public void setDepends(ArrayList<String> depends) {
		this.depends = depends;
	}

	public ArrayList<int[]> getPositions() {
		return positions;
	}

	public void setPositions(ArrayList<int[]> positions) {
		this.positions = positions;
	}

	public ArrayList<CoNLLWord> getWords() {
		return words;
	}

	public void setWords(ArrayList<CoNLLWord> words) {
		this.words = words;
	}

	public int getEndWordIdx() {
		return endWordIdx;
	}

	public void setEndWordIdx(int endWordIdx) {
		this.endWordIdx = endWordIdx;
	}

	private String speaker;

	private int wordsCount;

	public int getWordsCount() {
		return wordsCount;
	}

	public void setWordsCount(int wordsCount) {
		this.wordsCount = wordsCount;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String getSpeaker() {
		return speaker;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public MyTree syntaxTree;

	public Tree stdTree;

	public ArrayList<String> depends;

	public ArrayList<int[]> positions;

	public ArrayList<CoNLLWord> words;

	public CoNLLSentence() {
		words = new ArrayList<CoNLLWord>();
	}

	public void addWord(CoNLLWord word) {
		this.words.add(word);
		word.setSentence(this);
		word.indexInSentence = this.words.size()-1;
	}

	public CoNLLWord getWord(int index) {
		return this.words.get(index);
	}

	public void addSyntaxTree(String syntaxStr) {
		this.syntaxTree = Common.constructTree(syntaxStr);
	}

	private static LabeledScoredTreeFactory treeFactory = new LabeledScoredTreeFactory();

	public void addStdTree(String syntaxStr) {
		TreeReader treeReader = new PennTreeReader(new StringReader(syntaxStr), treeFactory);
		try {
			this.stdTree = treeReader.readTree();
			this.stdTree.indexSpans(0);
			treeReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
