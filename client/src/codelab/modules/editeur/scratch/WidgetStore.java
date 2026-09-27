package codelab.modules.editeur.scratch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.BorderFactory;

import codelab.CodeLab;
import codelab.modules.editeur.scratch.widgets.*;

/**
*	Classe du magasin de widgets
*	@author Jérôme Lehuen
*	@version 20/01/24
*/

public class WidgetStore extends AbstractWidgetPane {

	private static final long serialVersionUID = 1L;
	private final int HBTN = 25 ; // Hauteur des boutons
	private final int WSTO = 250 ; // Largeur du store

	private WidgetStoreListener listener;

	private int pos_button = 0; // Position du dernier bouton ajouté
	private int pos_widget; // Position du dernier widget ajouté

	private final ArrayList<WidgetStoreButton> buttons = new ArrayList<WidgetStoreButton>();

	// https://htmlcolorcodes.com/fr/
	public static Color COLOR_STRUC = new Color(255, 158, 158);   // Rouge
	public static Color COLOR_PROCE = Color.ORANGE;               // Orange
	public static Color COLOR_OTHER = new Color(210, 255, 46);    // Jaune
	public static Color COLOR_INOUT = new Color(0, 239, 127);     // Vert
	public static Color COLOR_GRAPH = Color.CYAN;                 // Cyan normal
	public static Color COLOR_TURTL = new Color(0, 220, 220);     // Cyan foncé
	public static Color COLOR_ROBOT = new Color(110, 200, 255);   // Bleu-rouge

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public WidgetStore(ScratchEditor editor, GlassPane glasspane, boolean editable) {
		this.editor = editor;
		setBorder(BorderFactory.createEmptyBorder());
		setLayout(null);
		setOpaque(false);
		setVisible(true);

		listener = new WidgetStoreListener(this, glasspane);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		setEditable(editable);

		// Ajout des boutons
		addButton(CodeLab.LABEL("Btn_Struc"), "STRUC", COLOR_STRUC);
		addButton(CodeLab.LABEL("Btn_Proce"), "PROCE", COLOR_PROCE);
		addButton(CodeLab.LABEL("Btn_Other"), "OTHER", COLOR_OTHER);
		addButton(CodeLab.LABEL("Btn_Inout"), "INOUT", COLOR_INOUT);
		addButton(CodeLab.LABEL("Btn_Graph"), "GRAPH", COLOR_GRAPH);
		addButton(CodeLab.LABEL("Btn_Robot"), "ROBOT", COLOR_ROBOT);
	}

	public void init() {
		// Construction du premier store à la fin du constructeur de ScratchEditor
		// Une fois que le JScrollPane du store soit instancié
		buildStore("PROCE");
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public boolean isStore() { return true; }

	public void setEditable(boolean value) {
		listener.setEnable(value);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void updatePreferredSize() {
		int largeur_scaled = (int) (WSTO * scale);
		int hauteur_scaled = (int) (pos_widget * scale);
		setPreferredSize(new Dimension(largeur_scaled, hauteur_scaled));
		editor.getViewportStore().updateUI();
	}

	private void addButton(String text, String code, Color color) {
		final WidgetStoreButton button = new WidgetStoreButton(this, 0, pos_button, WSTO, HBTN, color, text, code);
		buttons.add(button); // Liste des boutons
		pos_button += HBTN; // Position suivante
	}

	private void addWidget(AbstractWidget widget) {
		widget.setPosition(new Point(10, pos_widget));
		pos_widget = widget.getPositionSuivant().y + 20;
		add(widget);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void setScale(double scale) {
		super.setScale(scale);
		updatePreferredSize();
	}

	public WidgetStoreButton findButton(Point point) {
		for (WidgetStoreButton button : buttons)
			if (button.contains(point)) return button;
		return null;
	}

	public void buildStore(String code) {
		pos_widget = pos_button + 20;
		clear();
		switch (code) {
			case "PROCE":
				addWidget(new Widget_MAIN(this));
				addWidget(new Widget_FUNCTION(this));
				addWidget(new Widget_RETURN1(this));
				addWidget(new Widget_RETURN2(this));
				addWidget(new Widget_CALL(this));
				addWidget(new Widget_LETCALL(this));
				break;
			case "STRUC":
				addWidget(new Widget_IF(this));
				addWidget(new Widget_IFELSE(this));
				addWidget(new Widget_WHILE(this));
				addWidget(new Widget_REPEAT(this));
				break;
			case "INOUT":
				addWidget(new Widget_PRINT(this));
				addWidget(new Widget_INPUT(this));
				addWidget(new Widget_PLAYTONE(this));
				addWidget(new Widget_PLAYTONEON(this));
				addWidget(new Widget_PLAYTONEOFF(this));
				addWidget(new Widget_PLAYWAV(this));
				addWidget(new Widget_STOPAUDIO(this));
				addWidget(new Widget_GETNUMPAD(this));
				addWidget(new Widget_GETJOYSTICKX(this));
				addWidget(new Widget_GETJOYSTICKY(this));
				break;
			case "OTHER":
				addWidget(new Widget_LET(this));
				addWidget(new Widget_WAIT(this));
				addWidget(new Widget_COMMENT(this));
				break;
			case "GRAPH":
				addWidget(new Widget_CLEARSCREEN(this));
				addWidget(new Widget_SETCOLOR(this));
				addWidget(new Widget_SETWIDTH(this));
				addWidget(new Widget_DRAWLINE(this));
				addWidget(new Widget_DRAWCIRCLE(this));
				// Commandes de la tortue
				addWidget(new Widget_TURTLESHOW(this));
				addWidget(new Widget_TURTLEHIDE(this));
				addWidget(new Widget_TURTLEPENUP(this));
				addWidget(new Widget_TURTLEPENDOWN(this));
				addWidget(new Widget_TURTLERESET(this));
				addWidget(new Widget_TURTLEFORWARD(this));
				addWidget(new Widget_TURTLETURNLEFT(this));
				addWidget(new Widget_TURTLETURNRIGHT(this));
				addWidget(new Widget_TURTLEGOTO(this));
				addWidget(new Widget_GETCOLOR(this));
				break;
			case "ROBOT":
				addWidget(new Widget_SETBACKGROUND(this));
				addWidget(new Widget_SETROBOTCONFIG(this));
				addWidget(new Widget_MOTORON(this));
				addWidget(new Widget_MOTOROFF(this));
				addWidget(new Widget_GETSENSOR(this));
				addWidget(new Widget_GETMOTOR(this));
				addWidget(new Widget_MOTORRESET(this));
				break;
		}
		updatePreferredSize();
		repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = getGraphics2D(g);
		for (WidgetStoreButton button : buttons) button.draw(g2d);
		for (AbstractWidget widget : widgets) widget.draw(g2d);
		g2d.dispose();
	}
}
