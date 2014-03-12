package CoNLLZeroPronoun.detect;

import java.util.ArrayList;

import util.Common;

public class Split {
	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("chinese_list_all_development");
		
		ArrayList<String> l1 = new ArrayList<String>();
		ArrayList<String> l2 = new ArrayList<String>();
		ArrayList<String> l3 = new ArrayList<String>();
		ArrayList<String> l4 = new ArrayList<String>();
		ArrayList<String> l5 = new ArrayList<String>();
		
		for(int i = 0;i<lines.size();i++) {
			String line = lines.get(i);
			if(i%5==1) {
				l1.add(line);
			} else if(i%5==2) {
				l2.add(line);
			} else if(i%5==3) {
				l3.add(line);
			} else if(i%5==4) {
				l4.add(line);
			} else if(i%5==0) {
				l5.add(line);
			}
		}
		
		Common.outputLines(l1, "chinese_list_a_development");
		Common.outputLines(l2, "chinese_list_b_development");
		Common.outputLines(l3, "chinese_list_c_development");
		Common.outputLines(l4, "chinese_list_d_development");
		Common.outputLines(l5, "chinese_list_e_development");
	}
}
