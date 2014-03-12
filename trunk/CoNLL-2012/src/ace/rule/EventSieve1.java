package ace.rule;

import java.util.ArrayList;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import ace.event.coref.MaxEntEventFeas;
import ace.event.coref.MaxEntTest;
import ace.model.EventMention;
import ace.model.EventMentionArgument;

public class EventSieve1  extends Sieve {

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em1, ArrayList<EntityMention> orderedAntecedents) {
		if (!(em1 instanceof EventMention)) {
			return;
		}
		MaxEntEventFeas fea = new MaxEntEventFeas();
		CoNLLPart part = ruleCoref.part;
		EventMention em = (EventMention) em1;
		loop: for (EntityMention ant1 : orderedAntecedents) {
			EventMention ant = (EventMention) ant1;
			
//			if(!fea._triggerMatch_(ant, em, part) && !fea._commonBV_(ant, em, part)) {
//				continue;
//			}
			
			// each argument match, exact match
			if(ant.eventMentionArguments.size()==em.eventMentionArguments.size() && em.eventMentionArguments.size()>=0) {
				for(String role : ant.argHash.keySet()) {
					ArrayList<EventMentionArgument> args1 = ant.argHash.get(role);
					if(em.argHash.containsKey(role) && em.argHash.get(role).size()==args1.size()) {
						ArrayList<EventMentionArgument> args2 = em.argHash.get(role);
						for(EventMentionArgument a1 : args1) {
							EntityMention m1 = a1.mention;
							boolean coref = false;
							for(EventMentionArgument a2: args2) {
								EntityMention m2 = a2.mention;
								if(m1.entity==m2.entity) {
									coref = true;
									break;
								}
							}
							if(!coref) {
								continue loop;
							}
						}
						
					} else {
						continue loop;
					}
				}
				if (ruleCoref.combine2Entities(ant, em, ruleCoref.sentences)) {
					
//					int[] stat = MaxEntEventFeas.errors.get(ant.getSubType());
//					if (stat == null) {
//						stat = new int[2];
//						MaxEntEventFeas.errors.put(ant.getSubType(), stat);
//					}
//					EventMention gEM = RuleCoref.goldEventMentionMap.get(em.toString());
//					EventMention gAn = RuleCoref.goldEventMentionMap.get(ant.toString());
//					if (gEM != null && gAn != null && gEM.goldChainID == gAn.goldChainID) {
//						stat[0]++;
//					} else {
//						stat[1]++;
//					}
//					
//					
//					
					return;
				}
			}
		}
	}

}
