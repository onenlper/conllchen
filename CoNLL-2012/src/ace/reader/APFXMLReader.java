package ace.reader;

import java.util.ArrayList;


import model.Entity;
import model.EntityMention;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class APFXMLReader extends DefaultHandler {
		ArrayList<String> tags = new ArrayList<String>();
		ArrayList<Entity> entities;
		
		Entity entity;
		
		EntityMention entityMention;
		
		public APFXMLReader(ArrayList<Entity> entities) {
			super();
			this.entities = entities;
		}
		
		public void startElement(String uri, String name, String qName,
				Attributes atts) {
			tags.add(qName);
			if(qName.equalsIgnoreCase("entity")) {
				entity = new Entity();
				entities.add(entity);
				String type = atts.getValue("TYPE");
				String subType = atts.getValue("SUBTYPE");
				entity.type = type;
				entity.subType = subType;
			}
			if(qName.equalsIgnoreCase("entity_mention")) {
				entityMention = new EntityMention();
				entity.mentions.add(entityMention);
				entityMention.entity = entity;
				String type = atts.getValue("TYPE");
				entityMention.setType(type);
				entityMention.setRefID(atts.getValue("ID"));
				entityMention.semClass = entity.type.toLowerCase();
				entityMention.subType = (entity.type.substring(0, 1) + "-" + entity.subType).toLowerCase();
			}
			if(qName.equalsIgnoreCase("charseq")) {
				if(tags.get(tags.size()-2).equalsIgnoreCase("extent") && 
						tags.get(tags.size()-3).equalsIgnoreCase("entity_mention") &&
						tags.get(tags.size()-4).equalsIgnoreCase("entity")) {
					int start = Integer.valueOf(atts.getValue("START"));
					int end = Integer.valueOf(atts.getValue("END"));
					entityMention.extentCharStart = start;
					entityMention.extendCharEnd = end;
				}
				if(tags.get(tags.size()-2).equalsIgnoreCase("head") && 
						tags.get(tags.size()-3).equalsIgnoreCase("entity_mention") &&
						tags.get(tags.size()-4).equalsIgnoreCase("entity")) {
					int start = Integer.valueOf(atts.getValue("START"));
					int end = Integer.valueOf(atts.getValue("END"));
					entityMention.headCharStart = start;
					entityMention.headCharEnd = end;
				}
				
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {
			if(tags.get(tags.size()-1).equalsIgnoreCase("charseq") &&
					tags.get(tags.size()-2).equalsIgnoreCase("extent") && 
					tags.get(tags.size()-3).equalsIgnoreCase("entity_mention") &&
					tags.get(tags.size()-4).equalsIgnoreCase("entity")) {
				entityMention.extent += new String(ch, start, length);
			}
			
			if(tags.get(tags.size()-1).equalsIgnoreCase("charseq") &&
					tags.get(tags.size()-2).equalsIgnoreCase("head") && 
					tags.get(tags.size()-3).equalsIgnoreCase("entity_mention") &&
					tags.get(tags.size()-4).equalsIgnoreCase("entity")) {
				entityMention.head += new String(ch, start, length);
			}

		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			tags.remove(tags.size() - 1);
		}
	}