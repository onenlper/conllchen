package ace;

import java.util.ArrayList;

import model.syntaxTree.MyTree;

public class ParseResult {
	
	public String sentence="";
	
	public ArrayList<String> words;
	
	public ArrayList<String> posTags;
	
	public MyTree tree;
	
	public ArrayList<String> depends;
	
	public ArrayList<int[]> positions;
	
	public ParseResult() {
		
	}
	
	public ParseResult(String sentence, MyTree tree, ArrayList<String> depends) {
		this.sentence = sentence;
		this.tree = tree;
		this.depends = depends;
		words = new ArrayList<String>();
		posTags = new ArrayList<String>();
		String tokens[] = sentence.split(" ");
		for(String token:tokens) {
			if(token.isEmpty()) {
				continue;
			}
			int pos = token.lastIndexOf('/');
			String word = token.substring(0,pos);
			String posTag = token.substring(pos+1);
			words.add(word);
			posTags.add(posTag);
		}
	}
}

