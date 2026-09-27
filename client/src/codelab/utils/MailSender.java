package codelab.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import codelab.ExceptionManager;

public class MailSender {

	///////////////////////////////////////////////////
	// Envoi par MAILTO
	///////////////////////////////////////////////////

	public static void mailTo(String dest, String subject, String content) {
		URI uriMailTo = null;
		String mailTo = dest;
		mailTo += "?subject=" + subject;
		mailTo += "&body=" + content;

		if (Desktop.isDesktopSupported()) {
			if (Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
				try {
					uriMailTo = new URI("mailto", mailTo, null);
					Desktop.getDesktop().mail(uriMailTo);
				}
				catch (IOException e) {
					ExceptionManager.process(e);
				}
				catch (URISyntaxException e) {
					ExceptionManager.process(e);
				}
			}
		}
	}
}
