package codelab.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;

// https://tips4java.wordpress.com/2009/02/15/combo-box-with-separators/

public class SeparatorComboBox extends JComboBox<Object> implements KeyListener {

	private static final long serialVersionUID = 1L;

	private boolean released = true;
	private boolean separatorSelected = false;

	public SeparatorComboBox() {
		super();
		init();
	}

	public SeparatorComboBox(ComboBoxModel<Object> model) {
		super(model);
		init();
	}

	public SeparatorComboBox(Object[] items) {
		super(items);
		init();
	}

	public SeparatorComboBox(Vector<Object> items) {
		super(items);
		init();
	}

	private void init() {
		setRenderer( new SeparatorRenderer() );
		addKeyListener(this);
	}

	@Override
	public void setSelectedIndex(int index) {
		Object value = getItemAt(index);

		if (value instanceof JSeparator) {
			if (released) {
				separatorSelected = true;
				return;
			}
			int current = getSelectedIndex();
			index += (index > current) ? 1 : -1;

			if (index == -1 || index >= dataModel.getSize()) return;
		}
		super.setSelectedIndex(index);
	}

	@Override
	public void setPopupVisible(boolean visible) {
		if (separatorSelected) {
			separatorSelected = false;
			return;
		}
		super.setPopupVisible(visible);
	}

	public void keyPressed(KeyEvent e) {
		released = false;
	}

	public void keyReleased(KeyEvent e) {
		released = true;
	}

	public void keyTyped(KeyEvent e) {}

	class SeparatorRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof JSeparator) return (JSeparator)value;
			return this;
		}
	}
}
