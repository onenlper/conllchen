package chNEClassifier;

import java.util.ArrayList;

import util.Common;

public class Combine {

	public static void main(String args[]) {

		ArrayList<String> goldNers = Common.getLines("/users/yzcchen/workspace/CoNLL-2012/src/ner_test/chinese_"
				+ args[0] + ".neresult.test.gold");

		ArrayList<String> systemNers = Common.getLines("/users/yzcchen/workspace/CoNLL-2012/src/ner_test/chinese_"
				+ args[0] + ".neresult.test");

		ArrayList<String> ners = new ArrayList<String>();
		
		int k = 0;
		
		for(int i=0;i<goldNers.size();i++) {
			String goldLine = goldNers.get(i);
			if(goldLine.trim().isEmpty()) {
				ners.add(goldLine);
				continue;
			}
			
			String tokens[] = goldLine.split("\\s+");

			String tokens2[] = systemNers.get(k).split("\\s+");
			
			while(!tokens2[0].equals(tokens[0])) {
				if(!systemNers.get(k).trim().isEmpty()) {
					System.err.println("Error");
					System.exit(1);
				}
				k++;
				tokens2 = systemNers.get(k).split("\\s+");
			}
			
			StringBuilder sb = new StringBuilder();
			for(String token : tokens) {
				sb.append(token).append("\t");
			}
//			System.out.println(tokens2[tokens2.length-1] + "#" + tokens[tokens.length-1]);
			sb.append(tokens2[tokens2.length-1]);
			goldLine = sb.toString();
			ners.add(goldLine);
//			System.out.println(goldLine);
			k++;
		}
		
		Common.outputLines(ners, "/users/yzcchen/workspace/CoNLL-2012/src/ner_test/chinese_"
				+ args[0] + ".neresult.test.system");
	}

}
