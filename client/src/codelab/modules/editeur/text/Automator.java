package codelab.modules.editeur.text;

import java.util.Random;

import javax.swing.JTextArea;

import codelab.utils.Utils;

/**
*	Classe de l'automate Lorem Ipsum
*	@author Jérôme Lehuen
*	@version 25/01/22
*/

public class Automator extends Thread {

	private String[] loremIpsum = {
		"Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
		"Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor.",
		"Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi.",
		"Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat.",
		"Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue.",
		"Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede.",
		"Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales.",
		"Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh.",
		"Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.",
		"Ut velit mauris, egestas sed, gravida nec, ornare ut, mi.",
		"Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin.",
		"Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula.",
		"Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam.",
		"Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
		"Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus,",
		"convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum",
		"posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.",
		"Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis,",
		"ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien.",
		"Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet.",
		"Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi.",
		"Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue",
		"et ultricies lacus lorem varius purus. Curabitur eu amet." };

	private JTextArea textArea;
	private String line;
	private int index, i;

	private Random random = new Random();

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Automator(JTextArea textArea) {
		this.textArea = textArea;
		index = 0;
		line = loremIpsum[index];
		i = 0;
	}

	///////////////////////////////////////////////////
	// Boucle infinie du thread
	///////////////////////////////////////////////////

	public void run() {
		while (true) {
			if (i >= line.length()) {
				i = 0;
				if (++index >= loremIpsum.length) index = 0;
				line = loremIpsum[index];
				textArea.append("\n");
			}
			textArea.append(String.valueOf(line.charAt(i++)));
			int delay = (random.nextInt(300) + 5);
			Utils.wait(delay);
		}
	}
}
