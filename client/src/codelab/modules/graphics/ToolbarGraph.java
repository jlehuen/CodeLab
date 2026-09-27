package codelab.modules.graphics;

import codelab.AbstractToolbar;
import codelab.ToolButton;

/**
*	Classe de la barre du module graphique
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class ToolbarGraph extends AbstractToolbar {

	private ModuleGraphics module;
	private ToolButton turtleButton;
	private ToolButton debugButton;
	private ToolButton button_turtle_choose;
	private ToolButton button_back;
	private boolean isChoosingTurtle = false;
	private boolean isChoosingBackground = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolbarGraph(ModuleGraphics module) {
		super(module);
		this.module = (ModuleGraphics) module;

		/*
		// Réglage de la vitesse de la tortue
		slider = new JSlider(JSlider.HORIZONTAL, 0, 95, 100 - graphicpane.DELAY);
		slider.setPreferredSize(new Dimension(130, 20));
		slider.setMaximumSize(new Dimension(130, 20));
		slider.setMinimumSize(new Dimension(130, 20));
		slider.setToolTipText(LABEL("Turtle_delay"));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				graphicpane.DELAY = 100 - slider.getValue();
				//System.out.println(graphicpane.DELAY);
			}
		});
		*/

		addSeparator();
		addButton("NUMPAD");
		addButton("STICK");
		addButton("CTRL");
		addSeparator();
		addButton("RESET");
		add(button_back = new ToolButton("BACK", LABEL("Btn_background"), LABEL("Btn_background_tooltip"), icon_back, this));
		add(button_turtle_choose = new ToolButton("TURTLE_CHOOSE", LABEL("Btn_turtle_choose"), LABEL("Btn_turtle_choose_tooltip"), icon_turtle, this));
		add(turtleButton = new ToolButton("TURTLE_SHOW", LABEL("Btn_turtle"), LABEL("Btn_turtle_tooltip"), icon_turtle_on, this));
		add(debugButton = new ToolButton("DEBUG", LABEL("Btn_canvas"), LABEL("Btn_canvas_tooltip"), icon_toggle_off, this));
		add(new ToolButton("CLEAR", LABEL("Btn_clear"), LABEL("Btn_clear_tooltip"), icon_clear, this));
		//add(slider);
		addSeparatorflex();
		endToolbar();
	}

	public void turtleOn() {
		turtleButton.setIcon(icon_turtle_on);
	}

	public void turtleOff() {
		turtleButton.setIcon(icon_turtle_off);
	}

	///////////////////////////////////////////////////
	// Action handler
	///////////////////////////////////////////////////

	public void action(String ident) {
		switch (ident) {
			case "TURTLE_CHOOSE": action_turtle_choose(); break;
			case "TURTLE_SHOW": action_turtle_show(); break;
			case "CLEAR": action_clear(); break;
			case "DEBUG": action_debug(); break;
			case "BACK": action_back(); break;
			default: super.action(ident);
		}
	}

	private void action_turtle_choose() {
		if (isChoosingTurtle) return;
		isChoosingTurtle = true;
		if (button_turtle_choose != null) button_turtle_choose.setEnabled(false);
		try {
			int current_index = module.canvas.getTurtleIndex();
			new TurtleChooser(module, current_index);
			// Montrer la tortue le cas échéant
			if (!module.canvas.isTurtleVisible()) {
				turtleButton.setIcon(icon_turtle_on);
				module.canvas.turtleSwitch();
				module.canvas.repaint();
			}
		} finally {
			if (button_turtle_choose != null) button_turtle_choose.setEnabled(true);
			isChoosingTurtle = false;
		}
	}

	private void action_back() {
		if (isChoosingBackground) return;
		isChoosingBackground = true;
		if (button_back != null) button_back.setEnabled(false);
		try {
			int current_index = module.canvas.getBackgroundIndex();
			new BackgroundChooser(module, current_index);
		} finally {
			if (button_back != null) button_back.setEnabled(true);
			isChoosingBackground = false;
		}
	}

	private void action_clear() {
		module.canvas.clearScreen();
	}

	private void action_turtle_show() {
		if (module.canvas.isTurtleVisible()) {
			turtleButton.setIcon(icon_turtle_off);
			module.canvas.turtleSwitch();
			module.canvas.repaint();
		}
		else {
			turtleButton.setIcon(icon_turtle_on);
			module.canvas.turtleSwitch();
			module.canvas.repaint();
		}
	}

	private void action_debug() {
		if (module.canvas.DEBUG) {
			debugButton.setIcon(icon_toggle_off);
			module.canvas.DEBUG = false;
			module.canvas.repaint();
		}
		else {
			debugButton.setIcon(icon_toggle_on);
			module.canvas.DEBUG = true;
			module.canvas.repaint();
		}
	}
}
