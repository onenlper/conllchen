package ace.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;

public class EventMention extends EntityMention {

	public HashMap<String, String> bvs = new HashMap<String, String>();
	
	public boolean noun = false;

	public HashMap<String, ArrayList<EventMentionArgument>> argHash = new HashMap<String, ArrayList<EventMentionArgument>>();

	public int isZeroPronoun = 0;

	public ArrayList<EntityMention> zeroSubjects = null;

	public HashMap<String, Integer> typeHash = new HashMap<String, Integer>();

	public void increaseType(String type) {
		if (typeHash.containsKey(type)) {
			int count = typeHash.get(type);
			typeHash.put(type, count + 1);
		} else {
			typeHash.put(type, 1);
		}
	}

	public HashMap<String, ArrayList<EntityMention>> srlArgs = new HashMap<String, ArrayList<EntityMention>>();
	
	public void addArgument(EventMentionArgument argument) {
		this.eventMentionArguments.add(argument);
	}

	public String ID;
	public String extent;
	public int start;
	public int end;
	public String type = "null";

	public String inferFrom = "-";

	public String subType = "null";

	public boolean svm = false;
	public boolean maxent = false;

	public String polarity = "";
	public String modality = "";
	public String genericity = "";
	public String tense = "";

	public static ArrayList<String> MODALITY = new ArrayList<String>(Arrays.asList("ASSERTED", "OTHER"));

	public static ArrayList<String> GENERICITY = new ArrayList<String>(Arrays.asList("SPECIFIC", "GENERIC"));

	public static ArrayList<String> TENSE = new ArrayList<String>(Arrays.asList("PAST", "FUTURE", "PRESENT",
			"UNSPECIFIED"));

	public static ArrayList<String> POLARITY = new ArrayList<String>(Arrays.asList("NEGATIVE", "POSITIVE"));

	public String fileID;

	public CoNLLDocument document;

	public double confidence;
	
	public double typeConfidences[];

	public double subTypeConfidence;

	public double typeConfidence;

	public String pattern;

	public String label;

	public String posTag;

	public String getID() {
		return ID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getExtent() {
		return extent;
	}

	public void setExtent(String extent) {
		this.extent = extent;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int extentStart) {
		this.start = extentStart;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int extentEnd) {
		this.end = extentEnd;
	}

	public String getLdcScope() {
		return ldcScope;
	}

	public void setLdcScope(String ldcScope) {
		this.ldcScope = ldcScope;
	}

	public int getLdcScopeStart() {
		return ldcScopeStart;
	}

	public void setLdcScopeStart(int ldcScopeStart) {
		this.ldcScopeStart = ldcScopeStart;
	}

	public int getLdcScopeEnd() {
		return ldcScopeEnd;
	}

	public void setLdcScopeEnd(int ldcScopeEnd) {
		this.ldcScopeEnd = ldcScopeEnd;
	}

	public ArrayList<EventMentionArgument> getEventMentionArguments() {
		return eventMentionArguments;
	}

	public void setEventMentionArguments(ArrayList<EventMentionArgument> eventMentionArguments) {
		this.eventMentionArguments = eventMentionArguments;
	}

	String ldcScope;
	int ldcScopeStart;
	int ldcScopeEnd;
	public ArrayList<EventMentionArgument> eventMentionArguments;

	public EventChain eventChain;

	public int chainID;

	public int getChainID() {
		return chainID;
	}

	public void setEventChainID(int eventChainID) {
		this.chainID = eventChainID;
	}

	public EventChain getEventChain() {
		return eventChain;
	}

	public void setEventChain(EventChain eventChain) {
		this.eventChain = eventChain;
	}

	public EventMention() {
		this.eventMentionArguments = new ArrayList<EventMentionArgument>();
		this.setExtent("");
		this.setID("");
		this.setLdcScope("");
	}

	public EventMention antecedent;

	public int hashCode() {
		String str = this.headCharStart + "," + this.headCharEnd;
		return str.hashCode();
	}
}
