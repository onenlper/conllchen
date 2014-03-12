package ace;

import java.io.FileWriter;
import java.util.ArrayList;

import util.Common;

/*
 * convert coreference start-end to real characters
 * java ~ all.txt [mp|cr|mr]
 */
public class ExplainEntities {
	static boolean entity = false;
	static boolean event = false;
	static String mode;
	public static void main(String args[]) throws Exception {
		if (args.length != 3) {
			System.err.println("java ~ all.text [system|gold] [event|entity|both]");
		}
		Common.part = args[0];
		if(args[2].equals("entity")) {
			entity = true;
		} else if(args[2].equals("event")) {
			event = true;
		} else {
			entity = true;
			event = true;
		}
		mode = args[2];
		ArrayList<String> sgms = Common.getLines(args[0] + "2");
		ArrayList<String> stems = Common.getLines(args[0]);
		for (int k = 0; k < stems.size(); k++) {
			String stem = stems.get(k);
			String content = getContent(sgms.get(k));
			String fn = stem + ".entities." + args[1] + "." + mode;
			ArrayList<String> entitiesStr = Common.getLines(fn);
			FileWriter fw = new FileWriter(stem + ".chains." + args[1] + "." + mode);
			for (String entityStr : entitiesStr) {
				StringBuilder sb = new StringBuilder();
				String tokens[] = entityStr.split("\\s+");
				for (String token : tokens) {
					String t[] = token.trim().split(",");
					int start = Integer.valueOf(t[0]);
					int end = Integer.valueOf(t[1]);
					sb.append(start).append(",").append(end).append(":").append(
							content.substring(start, end + 1).replace("\n", "")).append(" ");
				}
				fw.write(sb.toString() + "\n");
			}
			fw.close();
		}
	}

	private static String getContent(String file) {
		PlainText plainText = ACECommon.getPlainText(file);
		return plainText.content;
	}

}
