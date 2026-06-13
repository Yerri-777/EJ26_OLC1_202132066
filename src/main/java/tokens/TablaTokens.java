package tokens;

import java.util.ArrayList;
import java.util.List;


public class TablaTokens {

    private static TablaTokens instancia;
    private final List<Token>  tokens;

    private TablaTokens() {
        tokens = new ArrayList<>();
    }

    public static TablaTokens getInstance() {
        if (instancia == null) {
            instancia = new TablaTokens();
        }
        return instancia;
    }

    public void reset()                       { tokens.clear();     }
    public void agregar(Token t)              { tokens.add(t);      }
    public List<Token> getTokens()            { return tokens;      }
    public int         totalTokens()          { return tokens.size(); }

    //  Reporte texto plano 

    public String generarReporte() {
        StringBuilder sb = new StringBuilder();
        String sep = "+------+---------------------------+-----------------+--------+---------+\n";

        sb.append("\n").append(sep);
        sb.append(String.format("| %-4s | %-25s | %-15s | %-6s | %-7s |\n",
                "No.", "Lexema", "Tipo", "Línea", "Columna"));
        sb.append(sep);

        for (int i = 0; i < tokens.size(); i++) {
            sb.append(tokens.get(i).toReportRow(i + 1)).append("\n");
        }

        sb.append(sep);
        sb.append(String.format("Total de tokens: %d\n", tokens.size()));
        return sb.toString();
    }

    //  Reporte HTML 

    public String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:monospace;margin:20px;background:#1e1e1e;color:#d4d4d4;}");
        sb.append("h2{color:#569cd6;}");
        sb.append("table{border-collapse:collapse;width:100%;}");
        sb.append("th{background:#264f78;color:white;padding:8px;text-align:left;}");
        sb.append("td{padding:6px 8px;border-bottom:1px solid #3c3c3c;}");
        sb.append("tr:hover{background:#2d2d2d;}");
        sb.append(".kw{color:#c586c0;}.lit{color:#ce9178;}.id{color:#9cdcfe;}.op{color:#d4d4d4;}");
        sb.append("</style></head><body>");
        sb.append("<h2>Tabla de Tokens — GoLite</h2>");
        sb.append("<table>");
        sb.append("<tr><th>No.</th><th>Lexema</th><th>Tipo</th><th>Línea</th><th>Columna</th></tr>");

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            String css = getCSSClass(t.getTipo());
            sb.append(String.format(
                "<tr><td>%d</td><td class='%s'>%s</td><td>%s</td><td>%d</td><td>%d</td></tr>",
                i + 1, css,
                escapeHTML(t.getLexema()),
                t.getTipo().name(),
                t.getLinea(),
                t.getColumna()
            ));
        }

        sb.append("</table>");
        sb.append(String.format("<p><strong>Total: %d token(s)</strong></p>", tokens.size()));
        sb.append("</body></html>");
        return sb.toString();
    }

    private String getCSSClass(TipoToken tipo) {
        String name = tipo.name();
        if (name.startsWith("RES_") || name.startsWith("TIPO_")) return "kw";
        if (name.startsWith("LIT_"))                             return "lit";
        if (name.equals("IDENTIFICADOR"))                        return "id";
        return "op";
    }

    private String escapeHTML(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}