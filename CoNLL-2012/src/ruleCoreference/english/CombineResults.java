package ruleCoreference.english;

import java.util.ArrayList;

import util.Common;

public class CombineResults {
	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ [test|development]");
		}
		mode = args[0];
		String folder[] = {"nw-xinhua", "nw-wsj", "mz", "bn", "tc", "wb", "bc", "pt"};
		ArrayList<String> total = new ArrayList<String>();
		for(String f : folder) {
			total.addAll(Common.getLines(getFilePath(f)));
		}
		Common.outputLines(total, "/users/yzcchen/chen3/conll12/english/key.english." + mode + ".close");
		System.out.println("combined");
	}
	
	static String mode;
	
	public static String getFilePath(String str) {
		String baseFolder = "/users/yzcchen/chen3/conll12/english/" + str + "_" + mode + "/key.system";
		return baseFolder;
	}
}
