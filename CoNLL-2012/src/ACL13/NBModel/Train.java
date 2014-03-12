//package ACL13.NBModel;
//
//import java.io.File;
//import java.util.ArrayList;
//
//import ACL13.ruleCoreference.chinese.DiscourseProcessSieve;
//import ACL13.ruleCoreference.chinese.ExactMatchSieve;
//import ACL13.ruleCoreference.chinese.PreciseConstructSieve;
//import ACL13.ruleCoreference.chinese.SameHeadSieve;
//import ACL13.ruleCoreference.chinese.Sieve;
//import ACL13.ruleCoreference.chinese.StrictHeadMatchSieve1;
//import ACL13.ruleCoreference.chinese.StrictHeadMatchSieve2;
//import ACL13.ruleCoreference.chinese.StrictHeadMatchSieve3;
//import ACL13.ruleCoreference.chinese.StrictHeadMatchSieve4;
//import ACL13.ruleCoreference.chinese.TimeSieve;
//
//import model.CoNLL.CoNLLDocument;
//import model.CoNLL.CoNLLPart;
//import util.Common;
//
//public class Train {
//
//	static ArrayList<Sieve> sieves;
//
//	public static void loadSieves() {
//		sieves = new ArrayList<Sieve>();
//
//		Sieve sameHeadSieve = new SameHeadSieve();
//		sieves.add(sameHeadSieve);
//
//		Sieve discourseProcessSieve = new DiscourseProcessSieve();
//		sieves.add(discourseProcessSieve);
//		//
//		Sieve timeSieve = new TimeSieve();
//		sieves.add(timeSieve);
//
//		Sieve exactMatchSieve = new ExactMatchSieve();
//		sieves.add(exactMatchSieve);
//
//		Sieve preciseConstructSieve = new PreciseConstructSieve();
//		sieves.add(preciseConstructSieve);
//
//		Sieve strictHeadMatchSieve1 = new StrictHeadMatchSieve1();
//		sieves.add(strictHeadMatchSieve1);
//
//		Sieve strictHeadMatchSieve2 = new StrictHeadMatchSieve2();
//		sieves.add(strictHeadMatchSieve2);
//
//		Sieve strictHeadMatchSieve3 = new StrictHeadMatchSieve3();
//		sieves.add(strictHeadMatchSieve3);
//
//		Sieve strictHeadMatchSieve4 = new StrictHeadMatchSieve4();
//		sieves.add(strictHeadMatchSieve4);
//	}
//
//	public Train(String folder) {
//		ArrayList<String> files = Common.getLines("chinese_list_" + folder + "_train/");
//
//		for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
//			String conllFn = files.get(fileIdx);
//			System.out.println(conllFn);
//			int a = conllFn.lastIndexOf(File.separator);
//			int b = conllFn.lastIndexOf(".");
//			String stem = conllFn.substring(a + 1, b);
//
//			CoNLLDocument goldDocument = new CoNLLDocument(conllFn);
//
//			for (int k = 0; k < goldDocument.getParts().size(); k++) {
//				CoNLLPart goldPart = goldDocument.getParts().get(k);
//
//				for (Sieve sieve : sieves) {
//					ruleCoref.currentSieve = sieve;
//					sieve.act(ruleCoref);
//				}
//
//			}
//		}
//	}
//
//	public static void main(String args[]) {
//		if (args.length != 2) {
//			System.out.println("java ~ folder [train|test]");
//			System.exit(1);
//		}
//		loadSieves();
//		String folder = args[1];
//
//	}
//}
