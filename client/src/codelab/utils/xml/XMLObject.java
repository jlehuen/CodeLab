package codelab.utils.xml;

import org.xml.sax.Attributes;

/**
*	Interface des objets XML
*	@author Jérôme Lehuen
*	@version 31/08/20
*/

public interface XMLObject {

	public XMLObject add(String element, Attributes attributs);
	public void set(String attribut, String valeur);
	public void end();
}
