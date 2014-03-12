package test;

import java.util.ArrayList;

import model.Entity;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;

public class Stats {
	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("chinese_list_all_test");
		double ch = 0;
		double me = 0;
		for(String file : lines) {
			file = file.replace("/test/", "/test_gold/").split("\\.")[0] + ".v4_gold_conll";
			CoNLLDocument document = new CoNLLDocument(file);
			for(CoNLLPart part : document.getParts()) {
				ArrayList<Entity> chains = part.getChains();
				ch += chains.size();
				for(Entity chain : chains) {
					me += chain.mentions.size();
				}
			}
		}
		System.out.println(lines.size());
		System.out.println("chains: " + ch);
		System.out.println("menions: " + me);
		System.out.println("Per: " + me/ch);
	}
}
