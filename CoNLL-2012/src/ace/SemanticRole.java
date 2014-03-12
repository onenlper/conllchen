package ace;

import java.util.ArrayList;
import java.util.HashMap;

import model.EntityMention;
import ace.model.EventMention;

public class SemanticRole implements Comparable<SemanticRole>{
	
	public ArrayList<EntityMention> arg0;

	public ArrayList<EntityMention> arg1;
	
	public ArrayList<EntityMention> tmp;
	
	public HashMap<String, ArrayList<EntityMention>> args = new HashMap<String, ArrayList<EntityMention>>();
	
	public EventMention predict;
	
	public SemanticRole() {
		this.arg0 = new ArrayList<EntityMention>();
		this.arg1 = new ArrayList<EntityMention>();
		this.tmp = new ArrayList<EntityMention>();
		this.predict = new EventMention();
	}

	@Override
	public int compareTo(SemanticRole arg0) {
		return this.predict.start-((SemanticRole)arg0).predict.start;
	}

}
