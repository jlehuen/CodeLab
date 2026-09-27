package codelab.modules.robotics;

import java.awt.Dimension;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import codelab.AbstractToolbar;
import codelab.ExceptionManager;
import codelab.ToolButton;

/**
*	Classe de la barre d'outils de l'atelier robotique
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class ToolbarSim extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	private ModuleRobotics simulatorpane;
	private JSlider slider;
	private ToolButton toolsButton, brushButton;

	public void setSlider(int value) {
		slider.setValue(value);
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolbarSim(ModuleRobotics module) {
		super(module);
		simulatorpane = module;

		// Curseur du zoom
		slider = new JSlider(JSlider.HORIZONTAL,
			(int)(simulatorpane.SMIN * 10),
			(int)(simulatorpane.SMAX * 10),
			simulatorpane.ZOOM_INIT); // min-max-init
		slider.setPreferredSize(new Dimension(130, 20));
		slider.setMaximumSize(new Dimension(130, 20));
		slider.setMinimumSize(new Dimension(130, 20));
		slider.setToolTipText(LABEL("Slider_scale"));
		slider.setFocusable(false);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				simulatorpane.getSimulator().setScale((double)slider.getValue() / 10);
				simulatorpane.getSimulator().updateViewport();
				simulatorpane.getSimulator().repaint();
				//System.out.println(getZoom());
			}
		});

		addSeparator();
		addButton("NUMPAD");
		addButton("STICK");
		addButton("CTRL");
		addSeparator();
		add(resetButton = new ToolButton("RESET", LABEL("Btn_reset"), LABEL("Btn_reset_tooltip"), icon_reset, this));
		add(new ToolButton("DATA", LABEL("Btn_data"), LABEL("Btn_data_tooltip"), icon_dashboard, this));
		add(toolsButton = new ToolButton("ROBOT", LABEL("Btn_robot"), LABEL("Btn_robot_tooltip"), icon_tools, this));
		add(brushButton = new ToolButton("BACKG", LABEL("Btn_background"), LABEL("Btn_background_tooltip"), icon_back, this));
		//addSeparator();
		add(slider);
		//add(new PmWidget(simulatorpane));
		addSeparatorflex();
		endToolbar();
	}

	///////////////////////////////////////////////////
	// Accès au réglage du zoom
	///////////////////////////////////////////////////

	public int getZoom() { return slider.getValue(); }
	public int getZoomMini() { return slider.getMinimum(); }
	public int getZoomMaxi() { return slider.getMaximum(); }
	public void setZoom(int n) { slider.setValue(n); }

	///////////////////////////////////////////////////
	// Actualisation des boutons
	///////////////////////////////////////////////////

	public void update() {
		super.update();
		toolsButton.setEnabled(!module.codelab.isRunning());
		brushButton.setEnabled(!module.codelab.isRunning());
	}

	public void action(String ident) {
		switch (ident) {
			case "DATA":	action_data(); break;
			case "ROBOT":	action_robot(); break;
			case "BACKG":	action_background(); break;
			default: super.action(ident);
		}
	}

	public void action_reset() {
		super.action_reset();
		//setZoom(simulatorpane.ZOOM_INIT);
	}

	private boolean isChoosingRobot = false;
	private boolean isChoosingBackground = false;

	private void action_robot() {
		if (isChoosingRobot) return;
		isChoosingRobot = true;
		toolsButton.setEnabled(false);
		try {
			new RobotChooser(simulatorpane);
		} catch (Exception e) {
			ExceptionManager.process(e);
		} finally {
			toolsButton.setEnabled(true);
			isChoosingRobot = false;
		}
	}

	private void action_background() {
		if (isChoosingBackground) return;
		isChoosingBackground = true;
		brushButton.setEnabled(false);
		try {
			new BackgroundChooser(simulatorpane);
		} catch (Exception e) {
			ExceptionManager.process(e);
		} finally {
			brushButton.setEnabled(true);
			isChoosingBackground = false;
		}
	}

	private void action_data() {
		Simulator simulator = simulatorpane.getSimulator();
		if (simulator.DEBUG) {
			simulator.DEBUG = false;
			simulator.repaint();
			module.hideDataTable();
		}
		else {
			simulator.DEBUG = true;
			simulator.repaint();
			module.showDataTable();
		}
	}
}
