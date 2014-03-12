package ACL13.ruleCoreference.chinese;

import java.util.ArrayList;

import util.Common;

public class CombineResults {
	public static void main(String args[]) {
		if(args.length!=2) {
			System.out.println("java ~ [test|development] [open|close]");
		}
		mode = args[0];
		if(args[1].equals("true")) {
			track = "open";
		} else {
			track = "close";
		}
		String folder[] = {"nw", "mz", "bn", "tc", "wb", "bc"};
		
		ArrayList<String> totalGold = new ArrayList<String>();
		
		ArrayList<String> total = new ArrayList<String>();
		for(String f : folder) {
			total.addAll(Common.getLines(getFilePath(f)));
			
			totalGold.addAll(Common.getLines(getFilePathGold(f)));
		}
		Common.outputLines(total, "/users/yzcchen/chen3/conll12/chinese/key.chinese." + mode + "." + track);
		
		Common.outputLines(totalGold, "/users/yzcchen/chen3/conll12/chinese/key.gold");
		System.out.println("combined");
	}
	
	static String mode;

	static String track;
	
	public static String getFilePath(String str) {
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/" + str + "_" + mode + "_" + track + "/key.system";
		return baseFolder;
	}
	
	public static String getFilePathGold(String str) {
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/" + str + "_" + mode + "_" + track + "/key2.gold";
		return baseFolder;
	}
}
