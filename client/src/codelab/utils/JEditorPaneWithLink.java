package codelab.utils;

import java.awt.Font;
import java.awt.Color;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class JEditorPaneWithLink extends JEditorPane {

	private static final long serialVersionUID = 1L;

	public JEditorPaneWithLink(String msg) {
		super("text/html", "<html><body style=\"" + getStyle() + "\">" + msg);
		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					Utils.openBrowser(e.getURL().toString());
				}
			}
		});
		setEditable(false);
	}

	public JEditorPaneWithLink() {
		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					Utils.openBrowser(e.getURL().toString());
				}
			}
		});
		setEditable(false);
	}

	static StringBuffer getStyle() {
		// For copying style
		JLabel label = new JLabel();
		Font font = label.getFont();
		Color color = label.getBackground();
		// Create some css from the label's font
		StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;");
		style.append("background-color: rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+");");
		style.append("margin:0; padding:0; border:0");
		return style;
	}
}
