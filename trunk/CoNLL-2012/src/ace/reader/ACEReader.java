package ace.reader;

import java.util.ArrayList;
import java.util.HashMap;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import util.Common;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.ParseResult;
import ace.PlainText;

public class ACEReader {

	public static ArrayList<ArrayList<Element>> nerElementses;
	public static ArrayList<String> testFiles;

	public static CoNLLDocument read(String sgmFile, boolean trainB) {
		if (nerElementses == null) {
			if (trainB) {
				int k = 0;
				String train[] = new String[4];
				for (int i = 0; i <= 4; i++) {
					if (i != Integer.valueOf(Common.part)) {
						train[k++] = Integer.toString(i);
					}
				}
				String concat = train[0] + "_" + train[1] + "_" + train[2] + "_" + train[3];
				testFiles = ACECommon.getFileList(train);
				nerElementses = ACECommon.getPredictNerElements(testFiles,
						"/users/yzcchen/tool/CRF/CRF++-0.54/ACE/Ner/" + concat + ".result");
			} else {
				String test[] = new String[1];
				test[0] = Common.part;
				testFiles = ACECommon.getFileList(test);
				String concat = test[0];
				nerElementses = ACECommon.getPredictNerElements(testFiles,
						"/users/yzcchen/tool/CRF/CRF++-0.54/ACE/Ner/" + concat + ".result");
			}
		}
		CoNLLDocument document = new CoNLLDocument();
		CoNLLPart part = new CoNLLPart();
		document.getParts().add(part);
		part.setDocument(document);
		document.setFilePath(sgmFile);
		PlainText plainText = ACECommon.getPlainText(sgmFile);
		part.rawText = plainText.content;
		ArrayList<ParseResult> parseResults = ACECorefCommon.readStanfordParseFile(sgmFile.substring(0, sgmFile
				.length() - 3)
				+ "parse2", plainText);
		int wordIndex = 0;

		for (int k = 0; k < parseResults.size(); k++) {
			ParseResult parseResult = parseResults.get(k);
			CoNLLSentence sentence = new CoNLLSentence();
			sentence.setSentenceIdx(k);
			sentence.syntaxTree = parseResult.tree;
			sentence.setStartWordIdx(wordIndex);
			sentence.positions = parseResult.positions;
			sentence.depends = parseResult.depends;

			for (int i = 0; i < parseResult.words.size(); i++) {
				CoNLLWord token = new CoNLLWord();
				token.word = parseResult.words.get(i);
				token.posTag = parseResult.posTags.get(i);
				token.speaker = "*";
				token.index = wordIndex++;
				token.orig = token.word;
				token.rawNamedEntity = "*";
				sentence.addWord(token);
			}
			sentence.setEndWordIdx(wordIndex - 1);
			part.addSentence(sentence);
		}
		ArrayList<Element> nameEntities = nerElementses.get(testFiles.indexOf(sgmFile));
		part.setNameEntities(nameEntities);

		part.setPartName(Integer.toString(testFiles.indexOf(sgmFile)));
		ArrayList<Entity> chains = ACECommon.getEntities(sgmFile.substring(0, sgmFile.length() - 3) + "apf.xml");

		HashMap<String, String> headExtendMap = new HashMap<String, String>();

		for (Entity chain : chains) {
			for (EntityMention mention : chain.mentions) {
				ACECorefCommon.assingStartEnd(mention, part);
				headExtendMap.put(mention.headCharStart + "," + mention.headCharEnd, mention.extentCharStart + ","
						+ mention.extendCharEnd);
			}
		}
		part.headExtendMap = headExtendMap;
		part.semanticRoles = ACECommon.readSemanticRole(sgmFile);

		part.setChains(chains);
		return document;
	}
}
