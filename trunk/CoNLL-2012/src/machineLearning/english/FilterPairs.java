package machineLearning.english;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class FilterPairs {
	public static void main(String args[]) {
		HashMap<String, Double> maps = Common.readFile2Map5("english_nw-wsj_source_all");
		ArrayList<String> lines = new ArrayList<String>();
		for(String key : maps.keySet()) {
			String token[] = key.split("_");
			double value = maps.get(key);
			if(value>=0.15 && value<=0.85 || token[0].equalsIgnoreCase(token[1])) {
				continue;
			}
			lines.add(key + " " + value);
		}
		Common.outputLines(lines, "english_nw-wsj_source_all_");
	}
}
