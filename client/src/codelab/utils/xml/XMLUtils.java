package codelab.utils.xml;

import org.xml.sax.Attributes;

public class XMLUtils {

	public static Boolean contains(Attributes attributs, String name, String valeur) {
		if (attributs != null)
			for (int i = 0; i < attributs.getLength(); i++)
				if (attributs.getLocalName(i).equals(name) && attributs.getValue(i).equals(valeur))
					return true;
		return false;
	}

	public static String toString(Attributes attributs) {
		if (attributs != null) {
			String str = String.format("{%s='%s'", attributs.getLocalName(0), attributs.getValue(0));
			for (int i = 1 ; i < attributs.getLength() ; i++)
				str += String.format(", %s='%s'", attributs.getLocalName(i), attributs.getValue(i));
			return str + "}";
		}
		else return "no attributes";
	}
}
