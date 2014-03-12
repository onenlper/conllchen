package ace.reader;

import java.util.ArrayList;

import model.Entity;
import model.EntityMention;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ValueXMLReader extends DefaultHandler {
		ArrayList<String> tags = new ArrayList<String>();
		ArrayList<Entity> entities;
		
		Entity entity;
		
		EntityMention entityMention;
		
		public ValueXMLReader(ArrayList<Entity> entities) {
			super();
			this.entities = entities;
		}
		
		public void startElement(String uri, String name, String qName,
				Attributes atts) {
			tags.add(qName);
			if(qName.equalsIgnoreCase("value")) {
				entity = new Entity();
				entities.add(entity);
				entity.type = "value";
				entity.subType = "value";
			}
			if(qName.equalsIgnoreCase("value_mention")) {
				entityMention = new EntityMention();
				entity.mentions.add(entityMention);
				entityMention.semClass = "value";
				entityMention.subType = "value";
				entityMention.entity = entity;
			}
			if(qName.equalsIgnoreCase("charseq")) {
				if(tags.get(tags.size()-2).equalsIgnoreCase("extent") && 
						tags.get(tags.size()-3).equalsIgnoreCase("value_mention") &&
						tags.get(tags.size()-4).equalsIgnoreCase("value")) {
					int start = Integer.valueOf(atts.getValue("START"));
					int end = Integer.valueOf(atts.getValue("END"));
					entityMention.extentCharStart = start;
					entityMention.extendCharEnd = end;
					
					entityMention.headCharStart = start;
					entityMention.headCharEnd = end;
				}
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {
			String str = new String(ch, start, length);
			str = str.replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
			if(tags.get(tags.size()-1).equalsIgnoreCase("charseq") &&
					tags.get(tags.size()-2).equalsIgnoreCase("extent") && 
					tags.get(tags.size()-3).equalsIgnoreCase("value_mention") &&
					tags.get(tags.size()-4).equalsIgnoreCase("value")) {
				entityMention.extent += str;
				entityMention.head += str;
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			tags.remove(tags.size() - 1);
		}
	}