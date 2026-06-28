package entorno;

import gui.Reportable;
import java.util.List;

public class TablaSimbolos implements Reportable {

    private final List<Simbolo> simbolos;
    // Constantes para mejorar mantenimiento
    private static final String HEADER_TEXT = String.format("| %-4s | %-20s | %-12s | %-12s | %-15s | %-6s | %-7s |",
            "No.", "ID", "Tipo Símb.", "Tipo Dato", "Ámbito", "Línea", "Col");

    public TablaSimbolos(List<Simbolo> simbolos) {
        this.simbolos = simbolos;
    }

    @Override
    public int totalElementos() { return simbolos.size(); }

    @Override
    public void reset() { simbolos.clear(); }

    @Override
    public String generarReporte() {
        if (simbolos.isEmpty()) return "No hay símbolos registrados.\n";

        StringBuilder sb = new StringBuilder("\n=== TABLA DE SÍMBOLOS GOLITE ===\n");
        String sep = "+------+----------------------+--------------+--------------+-----------------+--------+---------+\n";
        sb.append(sep).append(HEADER_TEXT).append("\n").append(sep);
        
        for (int i = 0; i < simbolos.size(); i++) {
            Simbolo s = simbolos.get(i);
            // Si tu objeto Simbolo tiene un método getNivel(), podrías indentar aquí
            sb.append(s.toReportRow(i + 1)).append("\n");
        }
        sb.append(sep);
        sb.append(String.format("Total: %d símbolo(s)\n", simbolos.size()));
        return sb.toString();
    }

    @Override
    public String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>")
          .append("body{font-family:'Segoe UI', sans-serif; background:#121212; color:#e0e0e0; padding:20px;}")
          .append("table{border-collapse:collapse; width:100%; margin-top:20px;}")
          .append("th{background:#1f1f1f; color:#bb86fc; padding:12px; text-align:left; border-bottom:2px solid #333;}")
          .append("td{padding:10px; border-bottom:1px solid #333;}")
          .append("tr:nth-child(even){background:#1a1a1a;}") // Zebra striping
          .append("tr:hover{background:#2c2c2c;}")
          .append(".funcion{color:#dcdcaa; font-weight:bold;}")
          .append(".variable{color:#9cdcfe;}")
          .append("</style></head><body>");

        sb.append("<h2>Tabla de Símbolos — GoLite</h2>");

        if (simbolos.isEmpty()) {
            sb.append("<p>No hay símbolos registrados.</p>");
        } else {
            sb.append("<table><thead><tr>")
              .append("<th>No.</th><th>ID</th><th>Tipo Símbolo</th>")
              .append("<th>Tipo Dato</th><th>Ámbito</th><th>Línea</th><th>Columna</th>")
              .append("</tr></thead><tbody>");
            
            for (int i = 0; i < simbolos.size(); i++) {
                Simbolo s = simbolos.get(i);
                String css = s.getTipoSimbolo().name().toLowerCase();
                sb.append(String.format(
                    "<tr><td>%d</td><td class='%s'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%d</td></tr>",
                    i + 1, css, s.getNombre(), s.getTipoSimbolo(), 
                    s.getTipoDato(), s.getAmbito(), s.getLinea(), s.getColumna()
                ));
            }
            sb.append("</tbody></table>");
        }
        return sb.toString() + "</body></html>";
    }
}