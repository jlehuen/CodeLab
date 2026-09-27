package codelab.modules.editeur.scratch;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import codelab.AbstractToolbar;
import codelab.CodeLab;
import codelab.ToolButton;
import codelab.controllers.widgets.Joystick;
import codelab.controllers.widgets.Numpad;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.Utils;

/**
*	Classe de la barre d'outils de l'éditeur Scratch
*	@author Jérôme Lehuen
*	@version 29/01/24
*/

public class ToolBarScratch extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	public static final double SMIN = 0.5; // Zoom mini
	public static final double SMAX = 1.5; // Zoom maxi
	public static final double ZOOM_INIT = 1; // Zoom initial

	private ModuleEditor module;
	private ScratchEditor editor;

	private ToolButton pythonButton;
	private JSlider slider;

	private boolean pythonFlag = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolBarScratch(ModuleEditor module, ScratchEditor editor) {
		super(module);
		this.module = module;
		this.editor = editor;

		// Curseur du zoom
		slider = new JSlider(JSlider.HORIZONTAL, (int)(SMIN*10), (int)(SMAX*10), (int)(ZOOM_INIT*10));
		slider.setPreferredSize(new Dimension(130, 20));
		slider.setMaximumSize(new Dimension(130, 20));
		slider.setMinimumSize(new Dimension(130, 20));
		slider.setToolTipText(LABEL("Slider_scale"));
		slider.setFocusable(false);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				editor.setScale((double)slider.getValue() / 10);
			}
		});

		addSeparator();
		add(numpadButton = new ToolButton("NUMPAD", LABEL("Btn_numpad"), LABEL("Btn_numpad_tooltip"), icon_numpad, this));
		add(joystickButton = new ToolButton("STICK", LABEL("Btn_stick"), LABEL("Btn_stick_tooltip"), icon_joystick, this));
		add(controllerButton = new ToolButton("CTRL", LABEL("Btn_ctrl"), LABEL("Btn_ctrl_tooltip"), icon_xbox360, this));
		addSeparator();
		add(new ToolButtonDisable(LABEL("Btn_undo"), icon_undo));
		add(new ToolButtonDisable(LABEL("Btn_redo"),icon_redo));
		add(new ToolButtonDisable(LABEL("Btn_untab"), icon_detab));
		add(new ToolButtonDisable(LABEL("Btn_tab"), icon_entab));
		add(new ToolButtonDisable(LABEL("Btn_com"), icon_encom));
		add(new ToolButtonDisable(LABEL("Btn_uncom"), icon_decom));

		addSeparator();
		add(pythonButton = new ToolButton("PYTHON", LABEL("Btn_python"), LABEL("Btn_python_tooltip"), icon_python_toggle_off, this));
		add(slider);
		addSeparatorflex();
		add(new ToolButton("LANG", "Blocs", CodeLab.PROP("URL_BLOCS"), logo_scratch, this));
		endToolbar();
	}

	///////////////////////////////////////////////////
	// Actualisation des boutons
	///////////////////////////////////////////////////

	public void setZoom(double n) {
		slider.setValue((int)(n * 10));
	}

	public void update() {
		super.update();
		// Actualisation des textes en dessous
		for (Component c : getComponents())
			if (c instanceof ToolButtonDisable) ((ToolButtonDisable)c).update();
	}

	public void action(String ident) {
		switch (ident) {
			case "NEW":		module.new_file(); break;
			case "NUMPAD":	Numpad.INSTANCE.open(numpadButton.getAbsolutePosition()); break;
			case "STICK":	Joystick.INSTANCE.open(joystickButton.getAbsolutePosition()); break;
			case "PYTHON":	action_python(); break;
			case "LANG":	Utils.openBrowser(CodeLab.PROP("URL_BLOCS")); break;
			default: super.action(ident);
		}
	}

	private void action_python() {
		if (pythonFlag) {
			pythonButton.setIcon(icon_python_toggle_off);
			pythonFlag = false;
			editor.getGlassPane().pythonFlag = false;
			editor.getGlassPane().repaint();
		}
		else {
			pythonButton.setIcon(icon_python_toggle_on);
			pythonFlag = true;
			editor.getGlassPane().pythonFlag = true;
			editor.getGlassPane().repaint();
		}
	}

	private class ToolButtonDisable extends JButton {

		private static final long serialVersionUID = 1L;

		private String text;

		public ToolButtonDisable(String text, Icon icon) {
			super(icon);
			this.text = text;
			setText(text);
			setFont(new Font(getFont().getName(), getFont().getStyle(), ToolButton.size));
			setVerticalTextPosition(BOTTOM);
			setHorizontalTextPosition(CENTER);
			setRolloverEnabled(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setOpaque(false);
			setEnabled(false);
		}

		public void update() {
			if (CodeLab.TEXT_UNDER_TOOLBUTTON) setText(text);
			else setText(null);
		}
	}
}
