package ace;

import java.util.ArrayList;
import java.util.HashSet;

import util.Common;

/*
 * convert coreference start-end to real characters
 * java ~ all.txt [mp|cr|mr]
 */
public class SeparateEventEntity {

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			System.err.println("java ~ all.text [mp|cr]");
			System.exit(1);
		}
		ArrayList<String> stems = Common.getLines(args[0]);
		for (int k = 0; k < stems.size(); k++) {
			String fn = stems.get(k) + ".entities." + args[1];
			ArrayList<String> entitiesStr = Common.getLines(fn);

			HashSet<String> eventHash = new HashSet<String>(Common.getLines(stems.get(k) + ".eventLines"));

			HashSet<String> entityHash = new HashSet<String>(Common.getLines(stems.get(k) + ".entityLines"));
			
			ArrayList<String> bothLines = new ArrayList<String>();
			ArrayList<String> entityLines = new ArrayList<String>();
			ArrayList<String> eventLines = new ArrayList<String>();
			for (String entityStr : entitiesStr) {
				bothLines.add(entityStr);

				String tokens[] = entityStr.split("\\s+");
				checkConsistent(eventHash, tokens, fn);
				if (eventHash.contains(tokens[0])) {
					eventLines.add(entityStr);
				}
				if (entityHash.contains(tokens[0])) {
					entityLines.add(entityStr);
				}

			}
			Common.outputLines(bothLines, stems.get(k) + ".entities." + args[1] + ".both");
			Common.outputLines(entityLines, stems.get(k) + ".entities." + args[1] + ".entity");
			Common.outputLines(eventLines, stems.get(k) + ".entities." + args[1] + ".event");
		}
	}

	private static void checkConsistent(HashSet<String> hash, String tokens[], String fn) {
		boolean contain = hash.contains(tokens[0]);
		for (int i = 1; i < tokens.length; i++) {
			if (hash.contains(tokens[i]) != contain) {
				System.err.println("Event coreferent with Entity!!!!");
				System.err.println(fn);
				System.err.println(tokens);
//				System.exit(1);
			}
		}
	}

}
