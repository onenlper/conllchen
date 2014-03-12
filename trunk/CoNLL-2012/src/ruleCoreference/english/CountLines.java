package ruleCoreference.english;

import java.util.ArrayList;

import util.Common;

public class CountLines {
	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("english_list_" + args[1] + "_" + args[0]);
		int total = 0;
		for(String file : files) {
			ArrayList<String> lines = Common.getLines(file);
			total += lines.size();
		}
		System.out.println(total);
	}
}
