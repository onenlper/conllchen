package ace;

import java.util.HashMap;

import util.Common;

public class AnalysisCount {
	public static void main(String args[]) {
		HashMap<String, int[]> stringFeaCount = Common.readFile2Map3("ACE_stringFeaCount");
		int k = 0;
		for(String key : stringFeaCount.keySet()) {
			int[] count = stringFeaCount.get(key);
			if(key.contains("#6") && count[0]!=0 && count[0]>count[1]) {
				System.err.println(key + ":" + count[0] + "#" + count[1]);
				k++;
			}
		}
		System.err.println(k);
	}
}
