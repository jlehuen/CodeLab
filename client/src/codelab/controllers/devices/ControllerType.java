package codelab.controllers.devices;

// https://www.javadoc.io/doc/net.java.jinput/jinput/latest/net/java/games/input/Controller.Type.html

public enum ControllerType {
	FINGERSTICK,
	GAMEPAD,
	HEADTRACKER,
	KEYBOARD,
	MOUSE,
	RUDDER,
	STICK,
	TRACKBALL,
	TRACKPAD,
	UNKNOWN,
	WHEEL;

	public String description() {
		switch (this) {
			case FINGERSTICK: return "Fingerstick (sometimes treated as mouse or stick)";
			case GAMEPAD: return "Gamepad";
			case HEADTRACKER: return "Headtracker";
			case KEYBOARD: return "Keyboard";
			case MOUSE: return "Mouse";
			case RUDDER: return "Rudder";
			case STICK: return "Stick (such as a joystick or flightstick)";
			case TRACKBALL: return "Trackball (sometimes treated as mouse)";
			case TRACKPAD: return "Trackpad (tablet, touchpad and some mouses)";
			case WHEEL: return "Wheel controller (such as a mouse wheel)";
			case UNKNOWN: return "Unkown controller";
			default: return "Unkown controller";
		}
	}

	public boolean getDefaulState() {
		// Prise en compte de STICK, GAMEPAD, KEYBOARD et TRACKPAD/MOUSE par défaut
		switch (this) {
			case STICK:
			case GAMEPAD:
			case KEYBOARD:
			case MOUSE:
			case TRACKPAD: return true;
			default: return false;
		}
	}
}
