package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import model.syntaxTree.MyTreeNode;
import util.Common.Animacy;
import util.Common.Gender;
import util.Common.Numb;
import util.Common.Person;
import ace.model.EventMentionArgument;

public class EntityMention implements Comparable<EntityMention> {

	public boolean isSubject;
	
	public boolean isObject;
	
	public MyTreeNode V;
	
	public MyTreeNode NP;
	
	public boolean tag = false;
	
	public static boolean ace = false; 
	
	public boolean findComp = false;
	
	public boolean notInChainZero = false;
	
	public int goldChainID = -1;
	
	public int sequence;
	
	public enum MentionType {
		Pronominal, Nominal, Proper
	};
	
	public EventMentionArgument argument;
	
	public String refID;
	
	public MentionType mentionType;

	public static boolean isAce() {
		return ace;
	}

	public static void setAce(boolean ace) {
		EntityMention.ace = ace;
	}

	public String getRefID() {
		return refID;
	}

	public void setRefID(String refID) {
		this.refID = refID;
	}

	public MentionType getMentionType() {
		return mentionType;
	}

	public void setMentionType(MentionType mentionType) {
		this.mentionType = mentionType;
	}

	public String getSemClass() {
		return semClass;
	}

	public void setSemClass(String semClass) {
		this.semClass = semClass;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getBuckWalter() {
		return buckWalter;
	}

	public void setBuckWalter(String buckWalter) {
		this.buckWalter = buckWalter;
	}

	public String getOriginal() {
		return original;
	}

	public void setOriginal(String original) {
		this.original = original;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getBuckUnWalter() {
		return buckUnWalter;
	}

	public void setBuckUnWalter(String buckUnWalter) {
		this.buckUnWalter = buckUnWalter;
	}

	public MyTreeNode getMaxTreeNode() {
		return maxTreeNode;
	}

	public void setMaxTreeNode(MyTreeNode maxTreeNode) {
		this.maxTreeNode = maxTreeNode;
	}

	public MyTreeNode getMinTreeNode() {
		return minTreeNode;
	}

	public void setMinTreeNode(MyTreeNode minTreeNode) {
		this.minTreeNode = minTreeNode;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public int getPRONOUN_TYPE() {
		return PRONOUN_TYPE;
	}

	public void setPRONOUN_TYPE(int pRONOUNTYPE) {
		PRONOUN_TYPE = pRONOUNTYPE;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public boolean isNT() {
		return isNT;
	}

	public void setNT(boolean isNT) {
		this.isNT = isNT;
	}

	public int getAnaphoric() {
		return anaphoric;
	}

	public void setAnaphoric(int anaphoric) {
		this.anaphoric = anaphoric;
	}

	public HashSet<EntityMention> getRoleSet() {
		return roleSet;
	}

	public void setRoleSet(HashSet<EntityMention> roleSet) {
		this.roleSet = roleSet;
	}

	public boolean isGeneric() {
		return generic;
	}

	public void setGeneric(boolean generic) {
		this.generic = generic;
	}

	public double getTypeConfidence() {
		return typeConfidence;
	}

	public void setTypeConfidence(double typeConfidence) {
		this.typeConfidence = typeConfidence;
	}

	public double getSubTypeConfidence() {
		return subTypeConfidence;
	}

	public void setSubTypeConfidence(double subTypeConfidence) {
		this.subTypeConfidence = subTypeConfidence;
	}

	public int getExtentCharStart() {
		return extentCharStart;
	}

	public void setExtentCharStart(int extentCharStart) {
		this.extentCharStart = extentCharStart;
	}

	public int getExtendCharEnd() {
		return extendCharEnd;
	}

	public void setExtendCharEnd(int extendCharEnd) {
		this.extendCharEnd = extendCharEnd;
	}

	public int getHeadCharStart() {
		return headCharStart;
	}

	public void setHeadCharStart(int headCharStart) {
		this.headCharStart = headCharStart;
	}

	public int getHeadCharEnd() {
		return headCharEnd;
	}

	public void setHeadCharEnd(int headCharEnd) {
		this.headCharEnd = headCharEnd;
	}

	public MyTreeNode getTreeNode() {
		return treeNode;
	}

	public void setTreeNode(MyTreeNode treeNode) {
		this.treeNode = treeNode;
	}

	public int getSentenceID() {
		return sentenceID;
	}

	public void setSentenceID(int sentenceID) {
		this.sentenceID = sentenceID;
	}

	public int getStartLeaf() {
		return startLeaf;
	}

	public void setStartLeaf(int startLeaf) {
		this.startLeaf = startLeaf;
	}

	public int getEndLeaf() {
		return endLeaf;
	}

	public void setEndLeaf(int endLeaf) {
		this.endLeaf = endLeaf;
	}

	public boolean isNNP() {
		return isNNP;
	}

	public void setNNP(boolean isNNP) {
		this.isNNP = isNNP;
	}

	public boolean isSub() {
		return isSub;
	}

	public void setSub(boolean isSub) {
		this.isSub = isSub;
	}

	public boolean isPronoun() {
		return isPronoun;
	}

	public void setPronoun(boolean isPronoun) {
		this.isPronoun = isPronoun;
	}

	public ArrayList<String> getModifyList() {
		return modifyList;
	}

	public void setModifyList(ArrayList<String> modifyList) {
		this.modifyList = modifyList;
	}

	public boolean isProperNoun() {
		return isProperNoun;
	}

	public void setProperNoun(boolean isProperNoun) {
		this.isProperNoun = isProperNoun;
	}

	public Numb getNumber() {
		return number;
	}

	public void setNumber(Numb number) {
		this.number = number;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Animacy getAnimacy() {
		return animacy;
	}

	public void setAnimacy(Animacy animacy) {
		this.animacy = animacy;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public String getNer() {
		return ner;
	}

	public void setNer(String ner) {
		this.ner = ner;
	}

	public int[] getPosition() {
		return position;
	}

	public void setPosition(int[] position) {
		this.position = position;
	}

	public boolean isEmbed() {
		return embed;
	}

	public void setEmbed(boolean embed) {
		this.embed = embed;
	}

	public List<String> getWordSet() {
		return Arrays.asList(this.original.split("\\s+"));
	}
	
	public String semClass = "";
	public String subType = "";
	
	public String lemma = "";
	public String buckWalter = "";
	public String original = "";
	public String source = "";
	public String buckUnWalter = "";

	public int hashCode() {
		String str = this.getS() + "," + this.getE();
		return str.hashCode();
	}

	public MyTreeNode maxTreeNode;
	public MyTreeNode minTreeNode;
	public boolean singleton = false;
	public int PRONOUN_TYPE;
	public Entity entity = new Entity();

	public boolean isNT;

	public int anaphoric = 0;

	public HashSet<EntityMention> roleSet = new HashSet<EntityMention>();

	public boolean equals(Object em2) {
		if (this.getS() == ((EntityMention) em2).getS() && this.getE() == ((EntityMention) em2).getE()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean moreRepresentativeThan(EntityMention m) {
		if (m == null) {
			return true;
		}
		if (mentionType != m.mentionType) {
			if ((mentionType == MentionType.Proper && m.mentionType != MentionType.Proper)
					|| (mentionType == MentionType.Nominal && m.mentionType == MentionType.Pronominal)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (this.headStart - this.start > m.headStart - m.start) {
				return true;
			} else if (this.headStart < m.headStart) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean generic = false;

	public double typeConfidence = Double.NEGATIVE_INFINITY;

	public double subTypeConfidence = Double.NEGATIVE_INFINITY;

	public int start=-1;
	public int end=-1;

	public String extent = "";

	public int extentCharStart;
	public int extendCharEnd;
	
	public int headCharStart;
	public int headCharEnd;
	
	public MyTreeNode treeNode;

	public int sentenceID;
	public int startLeaf;
	public int endLeaf;

	public int headStart=-1;
	public int headEnd=-1;
	public String head = "";

	public boolean isNNP = false;
	public boolean isSub = false;
	public boolean isPronoun = false;
	public ArrayList<String> modifyList = new ArrayList<String>();// record all
	// the
	// modifiers
	public boolean isProperNoun;

	public Numb number = Numb.UNKNOWN;
	public Gender gender = Gender.UNKNOWN;
	public Animacy animacy = Animacy.UNKNOWN;
	public Person person;

	public String ner = "OTHER";
	// public String semClass="OTHER";
	// public String subType = "O-OTHER";

	public int index;
	public EntityMention antecedent;
	public int entityIndex;

	public int position[];

	public String getContent() {
		return this.head;
	}

	public int getS() {
		if(ace) {
			return this.headCharStart;
		} else {
			return this.start;
		}
	}

	public int getE() {
		if(ace) {
			return this.headCharEnd;
		} else {
			return this.end;
		}
	}

	public boolean flag = false;

	public String type;

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public String getExtent() {
		return extent;
	}

	public void setExtent(String extent) {
		this.extent = extent;
	}

	public int getHeadStart() {
		return headStart;
	}

	public void setHeadStart(int headStart) {
		this.headStart = headStart;
	}

	public int getHeadEnd() {
		return headEnd;
	}

	public void setHeadEnd(int headEnd) {
		this.headEnd = headEnd;
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public EntityMention getAntecedent() {
		return antecedent;
	}

	public void setAntecedent(EntityMention antecedent) {
		this.antecedent = antecedent;
	}

	public int getEntityIndex() {
		return entityIndex;
	}

	public void setEntityIndex(int entityIndex) {
		this.entityIndex = entityIndex;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public EntityMention() {

	}

	boolean embed = true;

	public EntityMention(int start, int end) {
		this.start = start;
		this.end = end;
	}

	// (14, 15) (20, -1) (10, 20)
	public int compareTo(EntityMention emp2) {
		int diff = this.getS() - emp2.getS();
		if (diff == 0)
			return emp2.getE() - this.getE();
		else
			return diff;
		// if(this.getE()!=-1 && emp2.getE()!=-1) {
		// int diff = this.getE() - emp2.getE();
		// if(diff==0) {
		// return this.getS() - emp2.getS();
		// } else
		// return diff;
		// } else if(this.getE()==-1 && emp2.headEnd!=-1){
		// int diff = this.getS() - emp2.getE();
		// if(diff==0) {
		// return -1;
		// } else
		// return diff;
		// } else if(this.headEnd!=-1 && emp2.headEnd==-1){
		// int diff = this.getE() - emp2.getS();
		// if(diff==0) {
		// return 1;
		// } else
		// return diff;
		// } else {
		// return this.getS()-emp2.getS();
		// }
	}

	public String toName() {
		String str = this.start + "," + this.end; 
		return str;
	}
	
	public String toString() {
		// sb.append("start: ").append(this.start).append(" end: ").append(this.end).append(" ").append(this.source);
		// sb.append("headstart: ").append(this.headStart).append(" headend: ").append(this.headEnd).append(
		// " ").append(this.head);
//		String str = this.head + " @ " + this.original + " (" + this.animacy + " " + this.number + " " + this.gender
//				+ " " + this.ner + " " + this.headStart + ") [" + this.sentenceID + ":" + this.position[1] + "," + this.position[2] + " "
//				+ this.start + " " + this.end+"]";
		String str = this.headCharStart + "," + this.headCharEnd;
		return str;
	}
}