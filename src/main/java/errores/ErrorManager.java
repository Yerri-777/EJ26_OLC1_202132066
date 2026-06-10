package errores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gestor central de errores del compilador GoLite.
 * Blindado: Thread-safe y tolerante a fallos de renderizado.
 */
public class ErrorManager {

    // ─── Singleton ─────────────────────────────────────────────────────────────
    private static ErrorManager instancia;
    private final List<Error> errores;

    private ErrorManager() {
        this.errores = new ArrayList<>();
    }

    public static synchronized ErrorManager getInstance() {
        if (instancia == null) {
            instancia = new ErrorManager();
        }
        return instancia;
    }

    // ─── API pública ───────────────────────────────────────────────────────────

    public synchronized void reset() {
        errores.clear();
    }

    public synchronized void agregarLexico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.LEXICO, asegurarString(descripcion), linea, columna));
    }

    public synchronized void agregarSintactico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.SINTACTICO, asegurarString(descripcion), linea, columna));
    }

    public synchronized void agregarSemantico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.SEMANTICO, asegurarString(descripcion), linea, columna));
    }

    public synchronized void agregar(Error error) {
        if (error != null) {
            errores.add(error);
        }
    }

    // ─── Consultas ─────────────────────────────────────────────────────────────

    /**
     * Devuelve una copia inmutable de la lista de errores para evitar
     * modificaciones externas.
     */
    public synchronized List<Error> getErrores() {
        return Collections.unmodifiableList(new ArrayList<>(errores));
    }

    public synchronized boolean hayErrores() {
        return !errores.isEmpty();
    }

    public synchronized int totalErrores() {
        return errores.size();
    }

    public synchronized boolean hayErroresLexicos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.LEXICO);
    }

    public synchronized boolean hayErroresSintacticos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.SINTACTICO);
    }

    public synchronized boolean hayErroresSemanticos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.SEMANTICO);
    }

    // ─── Reporte en texto ──────────────────────────────────────────────────────

    public synchronized String generarReporte() {
        if (errores.isEmpty()) {
            return "✓ No se encontraron errores.\n";
        }

        StringBuilder sb = new StringBuilder();
        String separador =
                "+------+-----------------------------------------------+--------+---------+------------+\n";

        sb.append("\n").append(separador);
        sb.append(String.format(
                "| %-4s | %-45s | %-6s | %-7s | %-10s |\n",
                "No.", "Descripción", "Línea", "Columna", "Tipo"));
        sb.append(separador);

        for (int i = 0; i < errores.size(); i++) {
            sb.append(errores.get(i).toReportRow(i + 1)).append("\n");
        }

        sb.append(separador);
        sb.append(String.format("Total de errores: %d\n", errores.size()));

        return sb.toString();
    }

    // ─── Reporte HTML ──────────────────────────────────────────────────────────

    public synchronized String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;");
        sb.append("margin:20px;background:#1e1e1e;color:#d4d4d4;}");
        sb.append("h2{color:#569cd6;border-bottom:2px solid #569cd6;padding-bottom:10px;}");
        sb.append("table{border-collapse:collapse;width:100%;margin-top:20px;}");
        sb.append("th{background:#264f78;color:white;padding:10px;text-align:left;}");
        sb.append("td{padding:8px;border-bottom:1px solid #3c3c3c;}");
        sb.append(".lexico{color:#f48771;font-weight:bold;}");
        sb.append(".sintactico{color:#dcdcaa;font-weight:bold;}");
        sb.append(".semantico{color:#ce9178;font-weight:bold;}");
        sb.append("tr:hover{background:#2d2d2d;}");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<h2>Reporte de Errores — GoLite</h2>");

        if (errores.isEmpty()) {

            sb.append("<p style='color:#4ec9b0;'>✓ Compilación exitosa: No se encontraron errores.</p>");

        } else {

            sb.append("<table>");
            sb.append("<tr>");
            sb.append("<th>No.</th>");
            sb.append("<th>Descripción</th>");
            sb.append("<th>Línea</th>");
            sb.append("<th>Columna</th>");
            sb.append("<th>Tipo</th>");
            sb.append("</tr>");

            for (int i = 0; i < errores.size(); i++) {

                Error e = errores.get(i);

                String css = (e.getTipo() != null)
                        ? e.getTipo().name().toLowerCase()
                        : "desconocido";

                sb.append(String.format(
                        "<tr><td>%d</td><td>%s</td><td>%d</td><td>%d</td><td class='%s'>%s</td></tr>",
                        i + 1,
                        escapeHTML(e.getDescripcion()),
                        e.getLinea(),
                        e.getColumna(),
                        css,
                        e.getTipo()
                ));
            }

            sb.append("</table>");
            sb.append(String.format(
                    "<p><strong>Total: %d error(es) detectados.</strong></p>",
                    errores.size()));
        }

        sb.append("</body></html>");

        return sb.toString();
    }

    // ─── Utilidades ────────────────────────────────────────────────────────────

    private String escapeHTML(String texto) {
        return (texto == null)
                ? ""
                : texto.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;");
    }

    private String asegurarString(String str) {
        return (str == null || str.trim().isEmpty())
                ? "Error desconocido"
                : str;
    }
}