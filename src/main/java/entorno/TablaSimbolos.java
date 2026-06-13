package entorno;

import gui.Reportable;
import java.util.List;


 //TablaSimbolos — Genera el reporte de tabla de símbolos.
 //  ID | Tipo símbolo | Tipo dato | Ámbito | Línea | Columna
 
public class TablaSimbolos implements Reportable {

    private List<Simbolo> simbolos;

    public TablaSimbolos(List<Simbolo> simbolos) {
        this.simbolos = simbolos;
    }

    @Override
    public int totalElementos() { return simbolos.size(); }

    @Override
    public void reset() { simbolos.clear(); }

    // Reporte texto plano 

    @Override
    public String generarReporte() {
        if (simbolos.isEmpty()) return "No hay símbolos registrados.\n";

        StringBuilder sb = new StringBuilder();
        String sep = "+------+----------------------+------------+--------------+-----------------+--------+---------+\n";
        sb.append("\n").append(sep);
        sb.append(String.format("| %-4s | %-20s | %-10s | %-12s | %-15s | %-6s | %-7s |\n",
                "No.", "ID", "Tipo símbolo", "Tipo dato", "Ámbito", "Línea", "Columna"));
        sb.append(sep);
        for (int i = 0; i < simbolos.size(); i++) {
            sb.append(simbolos.get(i).toReportRow(i + 1)).append("\n");
        }
        sb.append(sep);
        sb.append(String.format("Total: %d símbolo(s)\n", simbolos.size()));
        return sb.toString();
    }

    //  Reporte HTML 

    @Override
    public String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:monospace;margin:20px;background:#1e1e1e;color:#d4d4d4;}");
        sb.append("h2{color:#569cd6;}");
        sb.append("table{border-collapse:collapse;width:100%;}");
        sb.append("th{background:#264f78;color:#fff;padding:8px;text-align:left;}");
        sb.append("td{padding:6px 8px;border-bottom:1px solid #3c3c3c;}");
        sb.append("tr:hover{background:#2d2d2d;}");
        sb.append(".funcion{color:#dcdcaa;}.variable{color:#9cdcfe;}.parametro{color:#ce9178;}");
        sb.append("</style></head><body>");
        sb.append("<h2>Tabla de Símbolos — GoLite</h2>");

        if (simbolos.isEmpty()) {
            sb.append("<p style='color:#4ec9b0;'>No hay símbolos registrados.</p>");
        } else {
            sb.append("<table>");
            sb.append("<tr><th>No.</th><th>ID</th><th>Tipo símbolo</th>")
              .append("<th>Tipo dato</th><th>Ámbito</th><th>Línea</th><th>Columna</th></tr>");
            for (int i = 0; i < simbolos.size(); i++) {
                Simbolo s = simbolos.get(i);
                String css = s.getTipoSimbolo().name().toLowerCase();
                sb.append(String.format(
                    "<tr><td>%d</td><td class='%s'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%d</td></tr>",
                    i + 1, css, s.getNombre(),
                    s.getTipoSimbolo().name().toLowerCase(),
                    s.getTipoDato(), s.getAmbito(),
                    s.getLinea(), s.getColumna()
                ));
            }
            sb.append("</table>");
            sb.append(String.format("<p><strong>Total: %d símbolo(s)</strong></p>", simbolos.size()));
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}