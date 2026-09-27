package codelab.controllers.devices;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.SwingUtilities;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import codelab.utils.Utils;

/**
*	Classe de l'observateur de controlleurs
*	@author Jérôme Lehuen
*	@version 14/01/24
*/

// https://jinput.github.io/jinput/
// https://www.javadoc.io/doc/net.java.jinput/jinput/latest/net/java/games/input/package-summary.html
// https://gist.github.com/charlierm/5688023

public class EventReader implements Runnable {

	private EventViewer viewer;
	private Controller[] controllers = new Controller[0];
	private ControllerType[] contollerType;
	private ConcurrentLinkedQueue<String> queue;

	// Table des états (activé ou non) le ControllerPopupMenu
	public Hashtable<Controller,Boolean> controlEnabled = new Hashtable<Controller,Boolean>(20);

	// https://github.com/jinput/jinput/issues/42
	// https://rollbar.com/blog/java-unsatisfiedlinkerror-runtime-error/
	public static boolean natives_OK = true;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public EventReader(EventViewer viewer) {
		this.viewer = viewer;
		queue = new ConcurrentLinkedQueue<String>();
		if (!natives_OK) return;
		try {
			System.out.println("   Scanning controllers...");
			controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
			contollerType = new ControllerType[controllers.length];
			for (int i = 0; i < controllers.length; i++) {
				if (controllers[i].getName() == null) continue;
				String name = controllers[i].getName();
				contollerType[i] = getControllerType(controllers[i].getType());
				boolean state = contollerType[i].getDefaulState(); // Etat initial
				controlEnabled.put(controllers[i], state); // Pour le ControllerPopupMenu
				System.out.format("      %s: %s on %s\n",
					controllers[i].getType().toString(),
					name,
					controllers[i].getPortType().toString());
			}
		} catch (Throwable t) {
			System.err.println("   Erreur scan controllers: " + t.getMessage());
		}
		new Thread(this).start();
	}

	///////////////////////////////////////////////////
	// Méthode de Runnable
	///////////////////////////////////////////////////

	public void run() {
		while (true) {
			emptyEventQueue();
			emptyLocalQueue();
			while (viewer.isVisible()) {
				for (Controller controller : controllers) {
					if (controller.getName() == null) continue;
					String name = controller.getName();
					Boolean enabled = controlEnabled.get(controller);
					if (enabled != null && enabled) {
						controller.poll();
						EventQueue controllerQueue = controller.getEventQueue();
						Event event = new Event();
						while (controllerQueue.getNextEvent(event)) {
							Component comp = event.getComponent();
							float value = event.getValue();
							String strvalue = Float.toString(value);
							String data = String.format("device=%s:name=%s:value=%s",
								name,
								comp.getName(),
								comp.isAnalog() ? strvalue : value == 1.0f ? "ON" : "OFF"
							);
							this.print(data);
							this.add(data);
						}
					}
				}
				Utils.wait(10); // Pour ne pas saturer le thread inutilement à 100% CPU
			}
			Utils.wait(100); // Pour ne pas charger le thread inutilement lorsque la fenêtre est fermée
		}
	}

	///////////////////////////////////////////////////
	// Méthodes puliques
	///////////////////////////////////////////////////

	public Controller[] getControllers() {
		return controllers;
	}

	public int hasNextEvent() {
		return queue.isEmpty() ? 0 : 1;
	}

	public String getNextEvent() {
		String data = queue.poll();
		return (data != null) ? data : "ERROR: there is no event in the queue";
	}

	public int reset() {
		emptyEventQueue();
		emptyLocalQueue();
		return 0;
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void print(String data) {
		// Sinon plantage en cas de clic souris dans le viewer
		SwingUtilities.invokeLater(() -> {
			viewer.append(data);
		});
	}

	private void add(String data) {
		queue.add(data);
	}

	private void emptyLocalQueue() {
		queue.clear();
	}

	private void emptyEventQueue() {
		for (Controller c : controllers) {
			if (c != null) {
				c.poll();
				EventQueue controllerQueue = c.getEventQueue();
				Event event = new Event();
				while (controllerQueue.getNextEvent(event));
			}
		}
	}

	private ControllerType getControllerType(Controller.Type type) {
		if (type == null) return ControllerType.UNKNOWN;
		if (type.equals(Controller.Type.FINGERSTICK)) return ControllerType.FINGERSTICK;
		if (type.equals(Controller.Type.GAMEPAD)) return ControllerType.GAMEPAD;
		if (type.equals(Controller.Type.HEADTRACKER)) return ControllerType.HEADTRACKER;
		if (type.equals(Controller.Type.KEYBOARD)) return ControllerType.KEYBOARD;
		if (type.equals(Controller.Type.MOUSE)) return ControllerType.MOUSE;
		if (type.equals(Controller.Type.RUDDER)) return ControllerType.RUDDER;
		if (type.equals(Controller.Type.STICK)) return ControllerType.STICK;
		if (type.equals(Controller.Type.TRACKBALL)) return ControllerType.TRACKBALL;
		if (type.equals(Controller.Type.TRACKPAD)) return ControllerType.TRACKPAD;
		if (type.equals(Controller.Type.WHEEL)) return ControllerType.WHEEL;
		return ControllerType.UNKNOWN;
	}
}
