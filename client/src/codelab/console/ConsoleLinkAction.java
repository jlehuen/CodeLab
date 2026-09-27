package codelab.console;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.utils.Utils;

/**
*	Classe des actions associées aux hyperliens
*	@author Jérôme Lehuen
*	@version 08/05/23
*/

public class ConsoleLinkAction extends AbstractAction {

	private String url;

	ConsoleLinkAction(String url) {
		this.url = url;
	}

	protected void execute() {
		//System.out.println(url);
		if (url.startsWith("http")) Utils.openBrowser(url); // Attention -> http:// ou https://
		else if (url.startsWith("method:")) {
			try {
				String name = url.substring(7);
				CodeLab.class.getDeclaredMethod(name).invoke(CodeLab.INSTANCE);
			}
			catch (Exception e) {
				ExceptionManager.process(e);
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		execute();
	}
}
