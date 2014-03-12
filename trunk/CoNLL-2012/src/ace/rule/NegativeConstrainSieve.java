package ace.rule;

import java.util.ArrayList;

import model.EntityMention;
import util.Common;

public class NegativeConstrainSieve extends Sieve{

	@Override
	public boolean applicable(EntityMention antecedent, EntityMention em, RuleCoref ruleCoref) {
		// TODO Auto-generated method stub
		if (antecedent.ner.equals(em.ner)) {
			String head1 = antecedent.head;
			String head2 = em.head;
			if ((antecedent.ner.equals("PERSON"))) {
				int similarity = 0;
				for (int i = 0; i < head1.length(); i++) {
					if (head2.indexOf(head1.charAt(i)) != -1) {
						similarity++;
					}
				}
				if (similarity == 0) {
					// System.out.println(head1 + " " + head2 + " " +
					// em11.ner);
					return true;
				}
			} else if (antecedent.ner.equals("LOC") || antecedent.ner.equals("GPE") || antecedent.ner.equals("ORG")) {
				if (!Common.isAbbreviation(head1, head2)) {
					int similarity = 0;
					for (int i = 0; i < head1.length(); i++) {
						if (head2.indexOf(head1.charAt(i)) != -1) {
							similarity++;
						}
					}
					if (similarity == 0) {
						// System.out.println(head1 + " " + head2 + " "
						// + em11.ner);
						return true;
					}
				}
			} 
		}
		return false;
	}

	@Override
	public void sieve(RuleCoref ruleCoref, EntityMention em, ArrayList<EntityMention> orderedAntecedents) {
		return;
	}

}
