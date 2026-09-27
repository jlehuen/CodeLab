/**
 * Class Hello
 */

class Hello {

	private final String VERSION = System.getProperty("java.version");

	Hello() {
		System.out.format("Hello world from Java version %s\n", VERSION);
	}

	public static void main(String args[]) {
		new Hello();
	}
}
