package codelab;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import codelab.utils.Utils;

/**
*	Classe de la console de log
*	@author Jérôme Lehuen
*	@version 05/06/23
*/

public class ConsoleLog extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;
	public static boolean OPENNED = false;
	public static ConsoleLog FRAME = null;

	private JTextArea ta;
	private boolean windaube;
	private boolean isrunning = true;

	public static void open(boolean windaube) {
		FRAME = new ConsoleLog(windaube);
	}

	public static void raise() {
		FRAME.toFront();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ConsoleLog(boolean windaube) {
		this.windaube = windaube;
		ta = new JTextArea(40,140);
		ta.setMargin(new Insets(5,5,5,5));
		ta.setEditable(false);
		ta.setLineWrap(false);
		ta.setWrapStyleWord(false);
		ta.setBackground(Color.DARK_GRAY);
        ta.setForeground(Color.ORANGE);
		ta.setFont(new Font("Monospaced", Font.PLAIN, 12));

		DefaultCaret caret = (DefaultCaret) ta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane sp = new JScrollPane(ta);
		sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		sp.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(sp);
		pack();

		setTitle("CodeLab Log");
		setLocationRelativeTo(CodeLab.FRAME);
		setAlwaysOnTop(true);
		setResizable(true);
		setVisible(true);
		OPENNED = true;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				isrunning = false;
				setVisible(false);
				OPENNED = false;
				FRAME = null;
				dispose();
			}
		});

		new Thread(this).start();
	}

	private void log(final String line) {
		SwingUtilities.invokeLater(() -> {
			try {
				String decodedToUTF8 = new String(line.getBytes("ISO-8859-1"), "UTF-8");
				if (windaube) decodedToUTF8 = decodedToUTF8.replaceAll("\\\\", "/");
				ta.append(decodedToUTF8 + "\n");
			}
			catch (UnsupportedEncodingException e) {
				ExceptionManager.process(e);
			}
		});
	}

	///////////////////////////////////////////////////
	// Méthode de Runnable
	///////////////////////////////////////////////////

	public void run() {
		Path path = Paths.get(CodeLab.LOGFILE);
		RandomAccessFile raf;
		long nblines = 0;

		while (isrunning) {
			long count = 0;
			try {
				// Compter le nombre de lignes
				try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
					count = stream.count();
				}
			}
			catch (Exception e) {
				ExceptionManager.process(e);
			}
			if (count > nblines) {
				// Il y a des lignes en plus
				try {
					raf = new RandomAccessFile(CodeLab.LOGFILE, "r");
					long i = 0;
					String line;
					while ((line = raf.readLine()) != null) {
						if (i++ >= nblines) log(line);
					}
					raf.close();
				}
				catch (Exception e) {
					ExceptionManager.process(e);
				}
				nblines = count;
			}
			Utils.wait(500);
		}
	}
}
