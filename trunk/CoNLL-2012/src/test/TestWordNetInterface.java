package test;

import java.io.File;
import java.net.URL;
import java.util.List;

import util.Common;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class TestWordNetInterface {
	public static void main(String args[]) throws Exception {
		String path = Common.wordnet + File.separator + "dict";
		File f = new File(path);
		URL url = new URL("file", null , path);
		IDictionary dict = new Dictionary(url);
		dict.open();
		WordnetStemmer stemmer = new WordnetStemmer(dict);
		List<String> stems = stemmer.findStems("books", POS.NOUN);
		for(String stem : stems) {
			System.out.println(stem);
		}
	}
}
