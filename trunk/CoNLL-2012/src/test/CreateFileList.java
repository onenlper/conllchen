package test;

import java.io.File;
import java.io.FileWriter;

public class CreateFileList {
	public static void main(String args[]) throws Exception {
		String languages[] = { "arabic", "chinese", "english" };
		String types[] = { "train", "development", "test"};
		for (String type : types) {
			String base = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/" + type + "/data/";
			for (String language : languages) {
				FileWriter fwAll = new FileWriter(language + "_list_all_" + type);
				String folder = base + language + "/annotations";
				for (File subFolder : (new File(folder).listFiles())) {
					FileWriter fw = new FileWriter(language + "_list_" + subFolder.getName() + "_" + type);
					for (File subFolder2 : subFolder.listFiles()) {
						FileWriter subFw = new FileWriter(language + "_list_" + subFolder.getName()+"-"+subFolder2.getName()+"_"+type);
						for (File subFolder3 : subFolder2.listFiles()) {
							for (File file : subFolder3.listFiles()) {
								if (type.equals("train")) {
									if (file.getAbsolutePath().endsWith("v4_gold_conll")) {
										subFw.write(file.getAbsolutePath() + "\n");
										fw.write(file.getAbsolutePath() + "\n");
										fwAll.write(file.getAbsolutePath() + "\n");
									}
								} else {
									if (file.getAbsolutePath().endsWith("v4_auto_conll")) {
										subFw.write(file.getAbsolutePath() + "\n");
										fw.write(file.getAbsolutePath() + "\n");
										fwAll.write(file.getAbsolutePath() + "\n");
									}
								}
							}
						}
						subFw.close();
					}
					fw.close();
				}
				fwAll.close();
			}
		}
	}
}
