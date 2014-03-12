package ACL13.ruleCoreference.chinese;

import java.util.ArrayList;

import model.Entity;
import model.EntityMention;

public class GenerateTrainNoun {
	public static void main(String args[]) {
		
	}
	
	public static void generateTrainSet(ArrayList<Entity> entities) {
		ArrayList<Entity> clusters = new ArrayList<Entity>();
		for(Entity cluster: entities) {
			if(allNounMention(cluster)) {
				clusters.add(cluster);
			}
		}
		
		
		
		for(int i=0;i<clusters.size();i++) {
			Entity c1 = clusters.get(i);
			for(int j=0;j<i;j++) {
				Entity c2 = clusters.get(j);
				
				
				
				
				
			}
			
		}
		
	}
	
	public static boolean allNounMention(Entity cluster) {
		for(EntityMention em : cluster.mentions) {
			if(em.isPronoun) {
				return false;
			}
		}
		return true;
	}
}
