package codelab;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
*	Classe de la table des panels droits
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class DataTable extends JScrollPane {

	private static final long serialVersionUID = 1L;

	private DataTableInterface system;
	private DefaultTableModel model;
	private JTable table;

	private DefaultTableCellRenderer centerRenderer;

	// Pour simplifier...
	private String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public DataTable() {
		table = new JTable(model);
		table.setShowGrid(true);
		table.setEnabled(false);
		table.setFocusable(false);
		table.getTableHeader().setUI(null); // Pas de titres
		table.setRowSelectionAllowed(false);
		table.setGridColor(new Color(238, 238, 238));
		table.getTableHeader().setReorderingAllowed(false);

		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setBorder(BorderFactory.createEmptyBorder());
		setViewportView(table);

		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		centerRenderer.setBackground(new Color(218, 228, 237));
	}

	///////////////////////////////////////////////////
	// Changement de système
	///////////////////////////////////////////////////

	public synchronized void setSystem(DataTableInterface system) {
		this.system = system;
		String[] titles = { LABEL("DataTable_1"), LABEL("DataTable_2") };
		String[][] data = new String[system.getNbProp()][2];
		model = new DefaultTableModel(data, titles);
		table.setModel(model);
		table.getColumnModel().getColumn(0).setHeaderRenderer(centerRenderer);
		table.getColumnModel().getColumn(1).setHeaderRenderer(centerRenderer);
	}

	///////////////////////////////////////////////////
	// Actualisation des données
	///////////////////////////////////////////////////

	public synchronized void update() {
		if (system != null && model != null) {
			system.updateValues();
			int count = Math.min(system.getNbProp(), model.getRowCount());
			for (int i = 0 ; i < count ; i++) {
				try {
					model.setValueAt(system.getPropKey(i), i, 0);
					model.setValueAt(system.getPropVal(i), i, 1);
				} catch (Exception e) {
					CodeLab.logger("Exception in DataTable.update: " + e.getMessage());
				}
			}
		}
	}
}
