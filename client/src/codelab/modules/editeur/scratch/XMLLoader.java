package codelab.modules.editeur.scratch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.console.Console;
import codelab.modules.editeur.scratch.widgets.*;

/**
*	Classe du chargeur de programmes Scratch
*	@author Jérôme Lehuen
*	@version 20/01/24
*/

public class XMLLoader extends DefaultHandler {

	private final GlassPane glasspane;
	private final Stack<AbstractWidget> pile = new Stack<AbstractWidget>();
	private AbstractWidget lastwidget = null;
	private boolean enfant = false;
	private boolean enfant1 = false;
	private boolean enfant2 = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public XMLLoader(File file, GlassPane glasspane) {
		this.glasspane = glasspane;
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);

		try {
			final FileInputStream stream = new FileInputStream(file);
			final SAXParser parser = factory.newSAXParser();
			parser.parse(stream, this);
		} catch (FileNotFoundException e) {
			ExceptionManager.process(e);
		} catch (ParserConfigurationException e) {
			ExceptionManager.process(e);
		} catch (SAXException | IOException e) {
			String msg = String.format(CodeLab.LABEL("FILE_ERROR"), file);
			CodeLab.INSTANCE.printlnToConsole(msg, Console.COLOR_ERROR);
			//CodeLab.exit();
		}
	}

	///////////////////////////////////////////////////
	// Surcharge des méthodes de DefaultHandler
	///////////////////////////////////////////////////

	public void startDocument() throws SAXException {}
	public void endDocument() throws SAXException {}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		//System.out.format("</%s>\n", localName);
		switch (localName) {
			// Dépiler le widget et enlever l'attente (en cas de bloc vide)
			case "main":
			case "function":
			case "while":
			case "repeat":
			case "if":		lastwidget = pile.pop(); enfant = false; break;
			case "ifelse":	lastwidget = pile.pop(); enfant2 = false; break;
			case "else":	pile.pop(); enfant1 = false; break;
			case "end":		lastwidget = null; break; // Pas de précédent pour le suivant
		}
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes attributs) throws SAXException {
		//System.out.format("<%s>\n", localName);
		AbstractWidget newwidget = null;
		switch (localName) {
			case "call":			newwidget = new Widget_CALL(glasspane); break;
			case "clearscreen":		newwidget = new Widget_CLEARSCREEN(glasspane); break;
			case "comment":			newwidget = new Widget_COMMENT(glasspane); break;
			case "drawcircle":		newwidget = new Widget_DRAWCIRCLE(glasspane); break;
			case "drawline":		newwidget = new Widget_DRAWLINE(glasspane); break;
			case "function":		newwidget = new Widget_FUNCTION(glasspane); break;
			case "getcolor":		newwidget = new Widget_GETCOLOR(glasspane); break;
			case "getjoystick_x":	newwidget = new Widget_GETJOYSTICKX(glasspane); break;
			case "getjoystick_y":	newwidget = new Widget_GETJOYSTICKY(glasspane); break;
			case "getmotor":		newwidget = new Widget_GETMOTOR(glasspane); break;
			case "getnumpad":		newwidget = new Widget_GETNUMPAD(glasspane); break;
			case "getsensor":		newwidget = new Widget_GETSENSOR(glasspane); break;
			case "setBackground":	newwidget = new Widget_SETBACKGROUND(glasspane); break;
			case "setRobotconfig":	newwidget = new Widget_SETROBOTCONFIG(glasspane); break;
			case "if":				newwidget = new Widget_IF(glasspane); break;
			case "ifelse":			newwidget = new Widget_IFELSE(glasspane); break;
			case "input":			newwidget = new Widget_INPUT(glasspane); break;
			case "let":				newwidget = new Widget_LET(glasspane); break;
			case "letcall":			newwidget = new Widget_LETCALL(glasspane); break;
			case "main":			newwidget = new Widget_MAIN(glasspane); break;
			case "motoroff":		newwidget = new Widget_MOTOROFF(glasspane); break;
			case "motoron":			newwidget = new Widget_MOTORON(glasspane); break;
			case "motorreset":		newwidget = new Widget_MOTORRESET(glasspane); break;
			case "playtone":		newwidget = new Widget_PLAYTONE(glasspane); break;
			case "playtoneon":		newwidget = new Widget_PLAYTONEON(glasspane); break;
			case "playtoneoff":		newwidget = new Widget_PLAYTONEOFF(glasspane); break;
			case "playwav":			newwidget = new Widget_PLAYWAV(glasspane); break;
			case "print":			newwidget = new Widget_PRINT(glasspane); break;
			case "repeat":			newwidget = new Widget_REPEAT(glasspane); break;
			case "return1":			newwidget = new Widget_RETURN1(glasspane); break;
			case "return2":			newwidget = new Widget_RETURN2(glasspane); break;
			case "setcolor":		newwidget = new Widget_SETCOLOR(glasspane); break;
			case "setwidth":		newwidget = new Widget_SETWIDTH(glasspane); break;
			case "stopaudio":		newwidget = new Widget_STOPAUDIO(glasspane); break;
			case "turtle_forward":	newwidget = new Widget_TURTLEFORWARD(glasspane); break;
			case "turtle_goto":		newwidget = new Widget_TURTLEGOTO(glasspane); break;
			case "turtle_hide":		newwidget = new Widget_TURTLEHIDE(glasspane); break;
			case "turtle_show":		newwidget = new Widget_TURTLESHOW(glasspane); break;
			case "turtle_penup":	newwidget = new Widget_TURTLEPENUP(glasspane); break;
			case "turtle_pendown":	newwidget = new Widget_TURTLEPENDOWN(glasspane); break;
			case "turtle_reset":	newwidget = new Widget_TURTLERESET(glasspane); break;
			case "turtle_turnleft":	newwidget = new Widget_TURTLETURNLEFT(glasspane); break;
			case "turtle_turnright":newwidget = new Widget_TURTLETURNRIGHT(glasspane); break;
			case "wait":			newwidget = new Widget_WAIT(glasspane); break;
			case "while":			newwidget = new Widget_WHILE(glasspane); break;

			case "scratch": break;
			case "else": break;
			case "end": break;

			default:
				System.out.format("XMLLoader ERROR: instruction %s does not exist\n", localName);
				//CodeLab.exit();
		}

		// Lecture des attributs
		String attr = null;
		String value = null;
		if (newwidget != null && attributs != null) {
			for (int i = 0; i < attributs.getLength(); i++) {
				try {
					attr = attributs.getLocalName(i);
					value = attributs.getValue(i);
					if (!value.equals("null")) newwidget.set(attr, value);
				} catch (Exception e) {
					System.out.format("XMLLoader ERROR: instruction %s does not accept attibute %s\n", localName, attr);
					//CodeLab.exit();
				}
			}
		}

		// Réalisation des références
		if (newwidget != null) {
			if (enfant) {
				final AbstractWidget parent = pile.peek();
				((AbstractBlocSimple) parent).setEnfant(newwidget);
				newwidget.setPrecedent(parent);
				enfant = false;
			} else if (enfant1) {
				final AbstractWidget parent = pile.peek();
				((AbstractBlocDouble) parent).setEnfant1(newwidget);
				newwidget.setPrecedent(parent);
				enfant1 = false;
			} else if (enfant2) {
				final AbstractWidget parent = pile.peek();
				((AbstractBlocDouble)parent).setEnfant2(newwidget);
				newwidget.setPrecedent(parent);
				enfant2 = false;
			}
			else if (lastwidget != null) {
				lastwidget.setSuivant(newwidget);
				newwidget.setPrecedent(lastwidget);
			}
			lastwidget = newwidget;
			glasspane.add(newwidget);
		}

		// Déclaration des attentes (flags)
		switch (localName) {
			case "main":
			case "function":
			case "while":
			case "repeat":
			case "if":		pile.push(newwidget); enfant = true; break;
			case "ifelse":	pile.push(newwidget); enfant1 = true; break;
			case "else":	pile.push(newwidget); enfant2 = true; break;
		}
	}
}
