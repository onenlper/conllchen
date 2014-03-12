package mentionDetection.arabic;

public class ArabicCRFFeature {
	
	public String utf8;
	
	public String getUtf8() {
		return utf8;
	}

	public void setUtf8(String utf8) {
		this.utf8 = utf8;
	}

	public String orig;
	
	public String lemma;
	
	public String unBuck;
	
	public String Buck;
	
	public String getOrig() {
		return orig;
	}

	public void setOrig(String orig) {
		this.orig = orig;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getUnBuck() {
		return unBuck;
	}

	public void setUnBuck(String unBuck) {
		this.unBuck = unBuck;
	}

	public String getBuck() {
		return Buck;
	}

	public void setBuck(String buck) {
		Buck = buck;
	}

	public String getPosTag() {
		return posTag;
	}

	public void setPosTag(String posTag) {
		this.posTag = posTag;
	}

	public String getInNP() {
		return inNP;
	}

	public void setInNP(String inNP) {
		this.inNP = inNP;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String posTag;
	
	public String inNP;
	
	public String label = "O";
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.utf8).append("\t").append(this.orig).append("\t").append(this.lemma).append("\t").
		append(this.unBuck).append("\t").append(this.Buck).append("\t").append(this.posTag).append("\t").append(this.inNP).append("\t").
		append(this.filePath).append("\t").append(this.documentID).append("\t").append(this.partID).append("\t").append(this.wordIndex).append("\t").append(label);
		return sb.toString();
	}
	
	public String filePath;
	
	public String documentID;
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public String getPartID() {
		return partID;
	}

	public void setPartID(String partID) {
		this.partID = partID;
	}

	public String getWordIndex() {
		return wordIndex;
	}

	public void setWordIndex(String wordIndex) {
		this.wordIndex = wordIndex;
	}

	public String partID;
	
	public String wordIndex;
}
