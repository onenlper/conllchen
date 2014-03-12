package ace.reader;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ace.PlainText;

public class SGMXMLReader extends DefaultHandler {
	PlainText sgm;
	ArrayList<String> tags = new ArrayList<String>();
	public SGMXMLReader(PlainText sgm) {
		this.sgm = sgm;
	}
	
	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		tags.add(qName);
		if(qName.equalsIgnoreCase("TEXT")) {
			sgm.start = sgm.content.length();
		}
	}
	
	public void characters(char ch[], int start, int length)
	throws SAXException {
		String topTag = tags.get(tags.size()-1);
		if(topTag.equalsIgnoreCase("DOCID")) {
//			for(int i=0;i<length;i++) {
//				sb.append(' ');
//			}
			String str = new String(ch, start, length);
			sgm.content +=str;
		} else {
			String str = new String(ch, start, length);
			if(str.endsWith(" ")) {
				str = str.substring(0,str.length()-1) + "\r";
			}
			sgm.content +=str;
		}
	}
	
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		tags.remove(tags.size()-1);
		if(qName.equalsIgnoreCase("TEXT")) {
			sgm.end = sgm.content.length() - 1;
		}
	}
}