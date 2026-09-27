package codelab.utils;

import java.awt.Font;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import codelab.CodeLab;

/**
*	Classe du sélectionneur de fonte
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class FontChooser extends JDialog {

	private final static int[] fontSizes = { 10, 12, 14, 16, 18 };
	private HashMap<String, Font> fontHashMap = new HashMap<String, Font>();
	private boolean validation = false;

	private JList<String> nameList;
	private JComboBox<Integer> sizeBox;
	private JLabel displayLabel;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public FontChooser() {
		super(CodeLab.FRAME, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		displayLabel = new JLabel("Lorem Ipsum { } etc.");
		displayLabel.setBorder(new EmptyBorder(20,5,20,5)); // Top,left,bottom,right

		// Récupération des polices monospaced acceptables
		Font fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font font : fonts) {
			if (isMonospaced(font) && !isRejected(font)) {
				fontHashMap.put(font.getName(), font);
			}
		}

		// Construction de la JList fonts
		Set<String> keys = fontHashMap.keySet();
		String names[] = keys.toArray(new String[keys.size()]);
		nameList = new JList<String>(names);
		nameList.setVisibleRowCount(10);
		nameList.setSelectedIndex(0);
		nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(getActualFontName()))
			nameList.setSelectedIndex(i);
		}
		nameList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				handleFontPropertyChange();
			}
		});

		// Construction de la JComboBox size
		int initialSize = 1;
		Integer sizes[] = new Integer[fontSizes.length];
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = Integer.valueOf(fontSizes[i]);
			if (sizes[i] == getActualSize()) initialSize = i;
		}
		sizeBox = new JComboBox<Integer>(sizes);
		sizeBox.setSelectedIndex(initialSize);
		sizeBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
			  handleFontPropertyChange();
			}
		});

		handleFontPropertyChange(); // Pour initialiser la police

		// Assemblage du panel
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createCenterPanel(), BorderLayout.CENTER);
		panel.add(createButtonPanel(), BorderLayout.SOUTH);
		panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); // Top,left,bottom,right
		getContentPane().add(panel);
		pack();

		// Fermeture sur la touche Échap
		getRootPane().registerKeyboardAction(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeFrame();
				}
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);

		setTitle(CodeLab.LABEL("itemFontButton"));
		setPreferredSize(new Dimension(300, 400));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public boolean getValidation() {
		return validation;
	}

	public String getSelectedName() {
		return nameList.getSelectedValue();
	}

	public int getSelectedSize() {
		return ((Integer)(sizeBox.getSelectedItem())).intValue();
	}

	///////////////////////////////////////////////////
	// Le panel central
	///////////////////////////////////////////////////

	private JPanel createCenterPanel() {
		JPanel emptySpace = new JPanel();
		emptySpace.setBorder(BorderFactory.createEmptyBorder(0,5,0,5)); // Top,left,bottom,right
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10)); // Top,left,bottom,right
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JScrollPane(nameList));
		panel.add(emptySpace);
		panel.add(sizeBox);
		panel.add(displayLabel);
		return panel;
	}

	///////////////////////////////////////////////////
	// Le panel inférieur
	///////////////////////////////////////////////////

	private JPanel createButtonPanel() {

		// Le bouton OK
		JButton btn_ok = new JButton(CodeLab.LABEL("OK"));
		btn_ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validation = true;
				closeFrame();
			}
		});

		// Le bouton CANCEL
		JButton btn_cancel = new JButton(CodeLab.LABEL("CANCEL"));
		btn_cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeFrame();
			}
		});

		// Construction du panel inférieur
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(0,10,10,10)); // Top,left,bottom,right
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		panel.add(btn_cancel);
		panel.add(btn_ok);
		return panel;
	}

	///////////////////////////////////////////////////
	// Autres méthodes privées
	///////////////////////////////////////////////////

	private String getActualFontName() {
		return CodeLab.FONT_NAME;
	}

	private int getActualSize() {
		return CodeLab.FONT_SIZE;
	}

	private boolean isMonospaced(Font font) {
		// Identifie les polices à espacement fixe
		FontRenderContext frc = new FontRenderContext(null,
			RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
			RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
		Rectangle2D iBounds = font.getStringBounds("i", frc);
		Rectangle2D mBounds = font.getStringBounds("m", frc);
		return (iBounds.getWidth() == mBounds.getWidth());
	}

	private boolean isRejected(Font font) {
		// Identifie les polices à rejeter
		String name = font.getFontName().toLowerCase();
		if (name.contains("bold") || name.contains("gras") || name.contains("condensed")) return true;
		if (name.contains("italic") || name.contains("italique") || name.contains("oblique")) return true;
		if (name.contains("script") || name.contains("ornaments") || name.contains("symbol")) return true;
		if (name.contains("sans") || name.contains("let") || name.contains("esri")) return true;
		if (name.contains("wingdings") || name.contains("webdings")) return true;
		return false;
	}

	private void handleFontPropertyChange() {
		Font font = new Font(getSelectedName(), Font.PLAIN, getSelectedSize());
		displayLabel.setFont(font);
	}

	private void closeFrame() {
		setVisible(false);
		dispose();
	}
}
