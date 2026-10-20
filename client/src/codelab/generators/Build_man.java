package codelab.generators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
*   Générateur automatique de documentation HTML pour les modules CodeLab.
*   Convertit un fichier Markdown (.md) en document HTML autonome et élégant.
*   @author Gemini 3.8
*   @version 05/10/26
*/

public class Build_man {

    public static void build(String inputPath, String outputPath) {
        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.err.println("Error: Markdown file not found: " + inputPath);
                return;
            }

            String md = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
            String title = extractTitle(md, inputFile.getName().replaceAll("\\.md$", ""));

            // Prétraitement des tableaux Markdown et des images
            String preprocessed = preprocessMarkdown(md);

            // Configuration du parseur Flexmark
            MutableDataSet options = new MutableDataSet();
            options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
            options.set(HtmlRenderer.RENDER_HEADER_ID, true);

            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            Node document = parser.parse(preprocessed);
            String bodyHtml = renderer.render(document);

            // Habillage avec gabarit HTML et styles CSS CodeLab
            String fullHtml = wrapInHtmlTemplate(title, bodyHtml);

            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.writeString(outputFile.toPath(), fullHtml, StandardCharsets.UTF_8);
            System.out.println("   Documentation HTML générée : " + outputPath);
        } catch (Exception e) {
            System.err.println("Error generating documentation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String extractTitle(String md, String defaultTitle) {
        Pattern p = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher m = p.matcher(md);
        if (m.find()) {
            return m.group(1).trim().replaceAll("`", "");
        }
        return defaultTitle;
    }

    private static String preprocessMarkdown(String md) {
        // Remplacement des images markdown pour garantir le rendu correct dans les balises HTML
        Pattern imgPattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)");
        Matcher imgMatcher = imgPattern.matcher(md);
        StringBuffer sbImg = new StringBuffer();
        while (imgMatcher.find()) {
            String alt = imgMatcher.group(1);
            String src = imgMatcher.group(2);
            imgMatcher.appendReplacement(sbImg, "<img src=\"" + src + "\" alt=\"" + alt + "\" />");
        }
        imgMatcher.appendTail(sbImg);
        md = sbImg.toString();

        // Prise en charge des tableaux Markdown
        return convertTables(md);
    }

    private static String convertTables(String md) {
        String[] lines = md.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.startsWith("|") && line.endsWith("|") && i + 1 < lines.length && isTableSeparator(lines[i + 1].trim())) {
                List<String> headers = splitRow(line);
                List<String> aligns = parseAlignments(lines[i + 1].trim());
                i += 2;
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    rows.add(splitRow(lines[i].trim()));
                    i++;
                }
                sb.append(renderHtmlTable(headers, aligns, rows)).append("\n\n");
            } else {
                sb.append(lines[i]).append("\n");
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isTableSeparator(String line) {
        if (!line.startsWith("|") || !line.endsWith("|")) return false;
        String inner = line.substring(1, line.length() - 1);
        for (String part : inner.split("\\|")) {
            part = part.trim();
            if (!part.matches(":?-+:?")) return false;
        }
        return true;
    }

    private static List<String> splitRow(String line) {
        String inner = line.substring(1, line.length() - 1);
        String[] parts = inner.split("\\|", -1);
        List<String> res = new ArrayList<>();
        for (String p : parts) res.add(p.trim());
        return res;
    }

    private static List<String> parseAlignments(String line) {
        String inner = line.substring(1, line.length() - 1);
        String[] parts = inner.split("\\|", -1);
        List<String> res = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            boolean left = p.startsWith(":");
            boolean right = p.endsWith(":");
            if (left && right) res.add("center");
            else if (right) res.add("right");
            else res.add("left");
        }
        return res;
    }

    private static String renderHtmlTable(List<String> headers, List<String> aligns, List<List<String>> rows) {
        StringBuilder h = new StringBuilder();
        h.append("<table>\n<thead>\n<tr>\n");
        for (int c = 0; c < headers.size(); c++) {
            String align = c < aligns.size() ? aligns.get(c) : "left";
            h.append(String.format("  <th style=\"text-align:%s;\">%s</th>\n", align, formatInline(headers.get(c))));
        }
        h.append("</tr>\n</thead>\n<tbody>\n");
        for (List<String> row : rows) {
            h.append("<tr>\n");
            for (int c = 0; c < headers.size(); c++) {
                String val = c < row.size() ? row.get(c) : "";
                String align = c < aligns.size() ? aligns.get(c) : "left";
                h.append(String.format("  <td style=\"text-align:%s;\">%s</td>\n", align, formatInline(val)));
            }
            h.append("</tr>\n");
        }
        h.append("</tbody>\n</table>");
        return h.toString();
    }

    private static String formatInline(String text) {
        text = text.replaceAll("`([^`]+)`", "<code>$1</code>");
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*([^*]+)\\*", "<em>$1</em>");
        return text;
    }

    private static String wrapInHtmlTemplate(String title, String body) {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"fr\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <title>" + escapeHtml(title) + "</title>\n" +
            "  <style>\n" +
            "    * { box-sizing: border-box; }\n" +
            "    body {\n" +
            "      font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Helvetica, Arial, sans-serif;\n" +
            "      font-size: 15.5px;\n" +
            "      line-height: 1.55;\n" +
            "      color: #24292f;\n" +
            "      background-color: #ffffff;\n" +
            "      margin: 0;\n" +
            "      padding: 2.5rem 1.5rem;\n" +
            "    }\n" +
            "    .container {\n" +
            "      max-width: 880px;\n" +
            "      margin: 0 auto;\n" +
            "      background: #ffffff;\n" +
            "    }\n" +
            "    h1, h2, h3, h4 {\n" +
            "      color: #1f2328;\n" +
            "      font-weight: 600;\n" +
            "      margin-top: 2rem;\n" +
            "      margin-bottom: 0.9rem;\n" +
            "      line-height: 1.25;\n" +
            "    }\n" +
            "    h1 { font-size: 2rem; border-bottom: 1px solid #d0d7de; padding-bottom: 0.4rem; }\n" +
            "    h2 { font-size: 1.45rem; border-bottom: 1px solid #eaecef; padding-bottom: 0.3rem; margin-top: 2.2rem; }\n" +
            "    h3 { font-size: 1.2rem; margin-top: 1.6rem; }\n" +
            "    p { margin-top: 0; margin-bottom: 0.9rem; }\n" +
            "    a { color: #0969da; text-decoration: none; }\n" +
            "    a:hover { text-decoration: underline; }\n" +
            "    ul, ol { margin-top: 0; margin-bottom: 1rem; padding-left: 2rem; }\n" +
            "    li { margin-bottom: 0.3rem; }\n" +
            "    li > ul, li > ol { margin-top: 0.35rem; margin-bottom: 0.5rem; }\n" +
            "    hr {\n" +
            "      height: 1px;\n" +
            "      background-color: #d0d7de;\n" +
            "      border: 0;\n" +
            "      margin: 2.5rem 0;\n" +
            "    }\n" +
            "    code {\n" +
            "      font-family: ui-monospace, SFMono-Regular, \"SF Mono\", Menlo, Consolas, monospace;\n" +
            "      font-size: 0.9em;\n" +
            "      background-color: #eff1f3;\n" +
            "      color: #0550ae;\n" +
            "      padding: 0.2em 0.4em;\n" +
            "      border-radius: 4px;\n" +
            "    }\n" +
            "    pre {\n" +
            "      background-color: #f6f8fa;\n" +
            "      border: 1px solid #d0d7de;\n" +
            "      border-radius: 6px;\n" +
            "      padding: 1rem 1.2rem;\n" +
            "      overflow-x: auto;\n" +
            "      font-size: 0.9em;\n" +
            "      line-height: 1.45;\n" +
            "      margin: 1.2rem 0;\n" +
            "    }\n" +
            "    pre code {\n" +
            "      background-color: transparent;\n" +
            "      padding: 0;\n" +
            "      color: #24292f;\n" +
            "    }\n" +
            "    table {\n" +
            "      width: 100%;\n" +
            "      border-collapse: collapse;\n" +
            "      margin: 1.5rem 0;\n" +
            "      border: 1px solid #d0d7de;\n" +
            "    }\n" +
            "    th, td {\n" +
            "      border: 1px solid #d0d7de;\n" +
            "      padding: 10px 14px;\n" +
            "      vertical-align: middle;\n" +
            "    }\n" +
            "    th {\n" +
            "      background-color: #f6f8fa;\n" +
            "      font-weight: 600;\n" +
            "      color: #1f2328;\n" +
            "    }\n" +
            "    tbody tr:nth-child(even) { background-color: #fafbfc; }\n" +
            "    tbody tr:hover { background-color: #f3f6fa; }\n" +
            "    img {\n" +
            "      max-width: 100%;\n" +
            "      height: auto;\n" +
            "    }\n" +
            "    img[alt=center] {\n" +
            "      display: block;\n" +
            "      margin: 1.5rem auto;\n" +
            "    }\n" +
            "    blockquote {\n" +
            "      margin: 1rem 0;\n" +
            "      padding: 0 1rem;\n" +
            "      color: #57606a;\n" +
            "      border-left: 0.25rem solid #d0d7de;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"container\">\n" +
            body + "\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>\n";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Build_man <input.md> <output.html>");
            return;
        }
        build(args[0], args[1]);
    }
}
