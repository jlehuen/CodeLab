package codelab.generators;

/**
 * Point d'entrée principal des générateurs CDK (API XML & Documentation Markdown).
 * 
 * Invocations :
 *   java -jar codelab.jar Parser <api.xml>           -> Build_lib & Build_doc (génération des stubs API)
 *   java -jar codelab.jar Markdown <in.md> <out.html> -> Build_man (génération de la doc HTML)
 * 
 * @author Jérôme Lehuen + Antigravity
 * @version 05/10/26
 */
public class Parser {

    public Parser() {}

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("Markdown") || args[0].equalsIgnoreCase("Man"))) {
            if (args.length >= 3) {
                Build_man.build(args[1], args[2]);
            } else if (args.length == 2) {
                String in = args[1];
                String out = in.replaceAll("\\.md$", ".html");
                Build_man.build(in, out);
            }
            return;
        }

        if (args.length > 1) {
            try {
                Class<?> blClass = Class.forName("codelab.generators.Build_lib");
                blClass.getDeclaredConstructor(String.class).newInstance(args[1]);
                Class<?> bdClass = Class.forName("codelab.generators.Build_doc");
                bdClass.getDeclaredConstructor(String.class).newInstance(args[1]);
            } catch (Exception e) {
                System.err.println("Error invoking XML generators: " + e.getMessage());
            }
        }
    }
}
