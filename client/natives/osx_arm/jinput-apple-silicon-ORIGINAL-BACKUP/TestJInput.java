import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import java.io.File;

public class TestJInput {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" Test d'exécution JInput sur macOS Apple Silicon");
        System.out.println("=================================================");
        System.out.println("Java Vendor  : " + System.getProperty("java.vendor"));
        System.out.println("Java Version : " + System.getProperty("java.version"));
        System.out.println("OS Arch      : " + System.getProperty("os.arch"));
        System.out.println("OS Name      : " + System.getProperty("os.name"));
        System.out.println("Library Path : " + System.getProperty("java.library.path"));
        System.out.println("-------------------------------------------------");

        try {
            System.out.println("Chargement de DefaultControllerEnvironment...");
            ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
            System.out.println("Environnement instancié : " + env.getClass().getName());

            System.out.println("Scan des manettes et périphériques d'entrée...");
            Controller[] controllers = env.getControllers();
            System.out.println("Nombre de contrôleurs détectés : " + controllers.length);
            for (int i = 0; i < controllers.length; i++) {
                Controller c = controllers[i];
                System.out.println("  [" + (i + 1) + "] Nom  : " + c.getName());
                System.out.println("      Type : " + c.getType());
                System.out.println("      Port : " + c.getPortType());
            }

            System.out.println("-------------------------------------------------");
            System.out.println("RÉSULTAT : Succès ! La bibliothèque JNI JInput");
            System.out.println("s'est chargée sans aucune UnsatisfiedLinkError.");
            System.out.println("=================================================");
        } catch (Throwable t) {
            System.err.println("ÉCHEC : " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
