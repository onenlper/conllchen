package util;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RegularXMLReader extends DefaultHandler {
	StringBuilder sb;
	public RegularXMLReader(StringBuilder sb) {
		this.sb = sb;
	}
	
	public void startElement(String uri, String name, String qName,
			Attributes atts) {
	}
	
	public void characters(char ch[], int start, int length)
	throws SAXException {
		String str = new String(ch, start, length);
		sb.append(str);
	}
	
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
	}
}
