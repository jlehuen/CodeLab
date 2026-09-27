package codelab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import codelab.console.ConsoleInterface;
import codelab.helper.DefaultToolbar;

/**
*	Classe abstraite des modules applicatifs
*	@author Jérôme Lehuen
*	@version 19/01/24
*/

public abstract class AbstractModule {

	public CodeLab codelab;

	protected AbstractToolbar toolbar;
	protected JSplitPane sp1; // Séparateur vertical (datatable à droite)
	protected JSplitPane sp2; // Séparateur horizontal (console en bas)
	protected ConsoleInterface console;
	protected DataTable dashboard;
	protected JComponent component;

	private boolean running = false;
	protected String title = "no title";
	protected String name;

	protected String unknownCommandError(String cmd) {
		return codelab.unknownCommandError(cmd);
	}

	// Pour simplifier...
	protected String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public AbstractModule(CodeLab codelab) {
		this.codelab = codelab;
		this.name = getClass().getSimpleName().toUpperCase();
		toolbar = new DefaultToolbar(this);
		console = codelab.getConsole();
		dashboard = new DataTable();
	}

	///////////////////////////////////////////////////
	// Getters et Setters
	///////////////////////////////////////////////////

	public String getName() {
		return name;
	}

	public JComponent getFullComponent() {
		return (JComponent) sp2;
	}

	public ConsoleInterface getConsole() {
		return console;
	}

	public AbstractToolbar getToolbar() {
		return toolbar;
	}

	public DataTable getDashboard() {
		return dashboard;
	}

	protected String getTitle() {
		return title;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean value) {
		running = value;
	}

	protected void setTitle(String title) {
		this.title = title;
	}

	public void setToolbar(AbstractToolbar toolbar) {
		this.toolbar = toolbar;
	}

	protected void setSystem(DataTableInterface system) {
		// Cette méthode établit des références croisées entre un simulateur (system) et une DataTable (dashboard)
		// Permet à la DataTable de récupérer des propriétés du simulateur à afficher avec getPropKey() et getPropVal()
		// Permet à la DataTable de demander une mise-à-jour des propriétés avec updateValues()
		// Exemple complet disponible dans MotorModule
		dashboard.setSystem(system);
		system.setDashboard(dashboard);
		dashboard.update();
	}

	public JComponent getComponent() {
		return component;
	}

	protected void setComponent(JComponent component) {
		this.component = component;
		component.setBorder(BorderFactory.createEmptyBorder());

		// Module applicatif à gauche et rien à droite pour l'instant
		sp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, component, null);
		sp1.setBorder(BorderFactory.createEmptyBorder());
		sp1.setOneTouchExpandable(false);
		sp1.setDividerLocation(0);
		sp1.setDividerSize(0);

		// Module applicatif en haut et console en bas
		sp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp1, (JComponent)console);
		sp2.setBorder(BorderFactory.createEmptyBorder());
		sp2.setOneTouchExpandable(false);
		sp2.setBottomComponent(null);
		sp2.setDividerSize(0);

		// Listener sur le séparateur vertical (dashboard)
		BasicSplitPaneUI ui1 = (BasicSplitPaneUI) sp1.getUI();
		ui1.getDivider().addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int positionDivider = sp1.getDividerLocation();
				dashboardSize = sp1.getWidth() - positionDivider;
			}
		});

		// Listener sur le séparateur horizontal (console)
		BasicSplitPaneUI ui2 = (BasicSplitPaneUI) sp2.getUI();
		ui2.getDivider().addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int positionDivider = sp2.getDividerLocation();
				CodeLab.CONSOLE_SIZE = (sp2.getHeight() - positionDivider);
			}
		});
	}

	public void detach() {
		// Détachement expérimental du module dans une fenêtre autonome
		JFrame frame = new JFrame();
		//frame.getContentPane().add(this.getToolbar(), BorderLayout.NORTH); // Impossible
		frame.getContentPane().add(this.getComponent(), BorderLayout.CENTER);
		frame.setPreferredSize(new Dimension(1200, 900));
		frame.pack();
		frame.setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques à impémenter
	///////////////////////////////////////////////////

	public abstract void init(); // Initialisations après pack()
	public abstract void exit(); // Quitter proprement l'application

	public abstract void stop();
	public abstract void start();
	public abstract void reset();

	public abstract String handleRequest(Map<String, String> params);

	///////////////////////////////////////////////////

	public void updateDividersLocation() {
		int loc1 = sp1.getWidth() - dashboardSize;
		int loc2 = sp2.getHeight() - CodeLab.CONSOLE_SIZE;
		sp1.setDividerLocation(loc1);
		sp2.setDividerLocation(loc2);
	}

	///////////////////////////////////////////////////
	// Méthodes sur la table de données
	///////////////////////////////////////////////////

	protected final int DIV_SIZE = new JSplitPane().getDividerSize();

	private int dashboardSize = 400; // Largeur du dashboard
	private boolean tableFlag = false;

	public boolean getDataFlag() {
		return tableFlag;
	}

	public void toggleDataTable() {
		if (tableFlag) hideDataTable();
		else showDataTable();
	}

	public void showDataTable() {
		sp1.setRightComponent(dashboard);
		sp1.setDividerSize(DIV_SIZE);
		updateDividersLocation();
		tableFlag = true;
	}

	public void hideDataTable() {
		sp1.setRightComponent(null);
		sp1.setDividerSize(0);
		tableFlag = false;
	}

	public void dashboardUpdate() {
		dashboard.update();
	}

	///////////////////////////////////////////////////
	// Méthodes sur la console
	///////////////////////////////////////////////////

	public void toggleConsole() {
		if (codelab.getConsoleFlag()) hideConsole();
		else showConsole();
	}

	public void showConsole() {
		sp2.setRightComponent((JComponent)console);
		sp2.setDividerSize(DIV_SIZE);
		updateDividersLocation();
		codelab.setConsoleFlag(true);
	}

	public void hideConsole() {
		sp2.setRightComponent(null);
		sp2.setDividerSize(0);
		codelab.setConsoleFlag(false);
	}

	public void printToConsole(String str, Color color, boolean caret) {
		codelab.printToConsole(str, color, caret);
	}

	public void printlnToConsole(String str, Color color, boolean caret) {
		codelab.printlnToConsole(str, color, caret);
	}

	public void printToConsole(String str, Color color) {
		codelab.printToConsole(str, color, false);
	}

	protected void updateConsole() {
		// Méthode invoquée par activation du module
		sp2.setResizeWeight(1);
		if (codelab.getConsoleFlag()) showConsole();
		else hideConsole();
	}

	///////////////////////////////////////////////////
	// Properties des plugins (lang, etc.)
	///////////////////////////////////////////////////

	private List<PropertyResourceBundle> PROPERTIES = new ArrayList<PropertyResourceBundle>();

	protected void loadProperties(String filename) {
		try {
			InputStream inputStream = this.getClass().getResourceAsStream("/data/" + filename);
			InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			PROPERTIES.add(new PropertyResourceBundle(reader));
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	protected void printProperties() {
		System.out.format("\n");
		System.out.format("----------------------------------------------\n");
		System.out.format("Properties of module %s :\n", title);
		System.out.format("----------------------------------------------\n");
		for (PropertyResourceBundle bundle : PROPERTIES) {
			for (Enumeration<String> e = bundle.getKeys(); e.hasMoreElements(); ) {
				String key = e.nextElement();
				System.out.format("%s=%s\n", key, bundle.getString(key));
			}
		}
	}

	public String getProperty(String key) {
		for (PropertyResourceBundle bundle : PROPERTIES)
			if (bundle.containsKey(key)) return bundle.getString(key);
		return "undefined key: " + key;
	}

	public String getLangProperty(String key) {
		key = String.format("%s_%s", key, CodeLab.LANG);
		return getProperty(key);
	}

	///////////////////////////////////////////////////
	// Autres méthodes utilitaires
	///////////////////////////////////////////////////

	public ImageIcon loadImageIcon(String filename) {
		return new ImageIcon(this.getClass().getResource("/data/" + filename));
	}

	public BufferedImage loadBufferedImage(String filename) {
		try {
			return ImageIO.read(this.getClass().getResourceAsStream("/data/" + filename));
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}
}
