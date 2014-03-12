package ruleCoreference.arabic;

import java.io.File;
import java.util.ArrayList;

import util.Common;

public class Combine {
	public static void main(String args[]) {
		String folder = "all";
		ArrayList<String> files = Common.getLines(args[0] + "_list_" + folder + "_test");
		ArrayList<String> lines = new ArrayList<String>();
		for(String file : files) {
			int k = file.indexOf(".");
			file = file.substring(0, k+1) + "v4_gold_skel";
			ArrayList<String> line = Common.getLines(file);
			lines.addAll(line);
		}
		Common.outputLines(lines, args[0] + ".keyT");
	}
}
