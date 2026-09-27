package codelab.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
*	Classe du parseur XML générique
*	@author Jérôme Lehuen
*	@version 18/05/21
*/

// méthode 1 :
// new XMLParser(this).parse(new File("data/file.xml"));

// méthode 2 :
// InputStream stream = getClass().getResourceAsStream("/data/file.xml");
// new XMLParser(this).parse(stream);

public class XMLParser extends DefaultHandler {

	private final Stack<XMLObject> pile = new Stack<XMLObject>();
	private final XMLObject racine;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public XMLParser(XMLObject racine) {
		this.racine = racine;
	}

	public boolean parse(InputStream stream) {
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);

		try {
			final SAXParser parser = factory.newSAXParser();
			parser.parse(stream, this);
			return true;
		}
		catch (SAXParseException e) {
			System.err.format("ERROR\n   XML ERROR: (line %d) %s\n", e.getLineNumber(), e.getMessage());
			return false;
		}
		catch (SAXException | ParserConfigurationException | IOException e) {
			System.err.format("ERROR\n   PARSE ERROR: %s\n", e.getMessage());
			return false;
		}
	}

	///////////////////////////////////////////////////
	// Surcharge des méthodes de DefaultHandler
	///////////////////////////////////////////////////

	public void startDocument() throws SAXException {
		//System.out.print("[ ");
	}

	public void endDocument() throws SAXException {
		//System.out.println("]");
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes attributs) throws SAXException {
		//System.out.print(localName + " ");
		XMLObject tmp, top;

		if (!pile.empty()) {
			top = pile.peek();
			tmp = top.add(localName, attributs);
			// Le XMLObject tmp est null si pas de classe associée
		}
		else tmp = top = racine;
		pile.push(tmp);

		if (tmp != null && attributs != null)
			for (int i = 0; i < attributs.getLength(); i++)
				tmp.set(attributs.getLocalName(i), attributs.getValue(i));
	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		final XMLObject top = pile.pop();
		if (top != null) top.end();
	}
}
