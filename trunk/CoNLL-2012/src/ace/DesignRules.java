package ace;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class DesignRules {
	public static void main(String args[]) {
		HashMap<String, int[]> map = new HashMap<String, int[]>();
		
		ArrayList<String> lines = Common.getLines("triggerTypeOritented");
		for(String line : lines) {
			if(line.trim().isEmpty()) {
				continue;
			}
			String token[] = line.split("\\s+");
			int[] stat = map.get(token[0]);
			if(stat==null) {
				stat = new int[2];
				map.put(token[0], stat);
			}
			String values[] = token[1].split("#");
			stat[0] += Integer.parseInt(values[0]);
			stat[1] += Integer.parseInt(values[1]);
		}
		
		for(String key : map.keySet()) {
			StringBuilder sb = new StringBuilder();
			int stat[] = map.get(key);
			sb.append(key).append(" ").append(stat[0]).append("#").append(stat[1]);
			System.err.println(sb.toString());
		}
	}
}
