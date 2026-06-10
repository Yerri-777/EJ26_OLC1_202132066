package errores;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor central de errores del compilador GoLite.
 *
 * Patrón Singleton: una única instancia acumula errores de todas las fases.
 * Se limpia antes de cada nueva compilación con reset().
 *
 * El PDF exige recolectar TODOS los errores, no detenerse en el primero.
 * Cada fase agrega sus errores aquí; al final se genera el reporte unificado.
 */
public class ErrorManager {

    // ─── Singleton ─────────────────────────────────────────────────────────────

    private static ErrorManager instancia;

    private ErrorManager() {
        errores = new ArrayList<>();
    }

    public static ErrorManager getInstance() {
        if (instancia == null) {
            instancia = new ErrorManager();
        }
        return instancia;
    }

    // ─── Estado ────────────────────────────────────────────────────────────────

    private final List<Error> errores;

    // ─── API pública ───────────────────────────────────────────────────────────

    /**
     * Limpia todos los errores. Llamar antes de cada compilación.
     */
    public void reset() {
        errores.clear();
    }

    /**
     * Agrega un error léxico (carácter no reconocido).
     */
    public void agregarLexico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.LEXICO, descripcion, linea, columna));
    }

    /**
     * Agrega un error sintáctico (estructura gramatical inválida).
     */
    public void agregarSintactico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.SINTACTICO, descripcion, linea, columna));
    }

    /**
     * Agrega un error semántico (tipos incompatibles, variable no declarada, etc).
     */
    public void agregarSemantico(String descripcion, int linea, int columna) {
        errores.add(new Error(TipoError.SEMANTICO, descripcion, linea, columna));
    }

    /**
     * Agrega un error ya construido (útil para el parser CUP).
     */
    public void agregar(Error error) {
        errores.add(error);
    }

    // ─── Consultas ─────────────────────────────────────────────────────────────

    public List<Error> getErrores()         { return errores;          }
    public boolean     hayErrores()         { return !errores.isEmpty(); }
    public int         totalErrores()       { return errores.size();   }

    public boolean hayErroresLexicos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.LEXICO);
    }

    public boolean hayErroresSintacticos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.SINTACTICO);
    }

    public boolean hayErroresSemanticos() {
        return errores.stream().anyMatch(e -> e.getTipo() == TipoError.SEMANTICO);
    }

    // ─── Reporte ───────────────────────────────────────────────────────────────

    /**
     * Genera el reporte de errores como string formateado.
     * Listo para mostrarse en la GUI o guardarse como archivo.
     */
    public String generarReporte() {
        if (errores.isEmpty()) {
            return "✓ No se encontraron errores.\n";
        }

        StringBuilder sb = new StringBuilder();
        String separador = "+------+-----------------------------------------------+--------+---------+------------+\n";

        sb.append("\n").append(separador);
        sb.append(String.format("| %-4s | %-45s | %-6s | %-7s | %-10s |\n",
                "No.", "Descripción", "Línea", "Columna", "Tipo"));
        sb.append(separador);

        for (int i = 0; i < errores.size(); i++) {
            sb.append(errores.get(i).toReportRow(i + 1)).append("\n");
        }

        sb.append(separador);
        sb.append(String.format("Total de errores: %d\n", errores.size()));

        return sb.toString();
    }

    /**
     * Genera el reporte en formato HTML para mostrar en ventana de reportes.
     */
    public String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:monospace;margin:20px;background:#1e1e1e;color:#d4d4d4;}");
        sb.append("h2{color:#569cd6;}");
        sb.append("table{border-collapse:collapse;width:100%;}");
        sb.append("th{background:#264f78;color:white;padding:8px;text-align:left;}");
        sb.append("td{padding:6px 8px;border-bottom:1px solid #3c3c3c;}");
        sb.append(".lexico{color:#f48771;}");
        sb.append(".sintactico{color:#dcdcaa;}");
        sb.append(".semantico{color:#ce9178;}");
        sb.append("tr:hover{background:#2d2d2d;}");
        sb.append("</style></head><body>");
        sb.append("<h2>Reporte de Errores — GoLite</h2>");

        if (errores.isEmpty()) {
            sb.append("<p style='color:#4ec9b0;'>✓ No se encontraron errores.</p>");
        } else {
            sb.append("<table>");
            sb.append("<tr><th>No.</th><th>Descripción</th><th>Línea</th><th>Columna</th><th>Tipo</th></tr>");
            for (int i = 0; i < errores.size(); i++) {
                Error e = errores.get(i);
                String css = e.getTipo().name().toLowerCase();
                sb.append(String.format(
                    "<tr><td>%d</td><td>%s</td><td>%d</td><td>%d</td><td class='%s'>%s</td></tr>",
                    i + 1,
                    escapeHTML(e.getDescripcion()),
                    e.getLinea(),
                    e.getColumna(),
                    css,
                    e.getTipo().name().toLowerCase()
                ));
            }
            sb.append("</table>");
            sb.append(String.format("<p><strong>Total: %d error(es)</strong></p>", errores.size()));
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // ─── Utilidades ────────────────────────────────────────────────────────────

    private String escapeHTML(String texto) {
        return texto.replace("&","&amp;")
                    .replace("<","&lt;")
                    .replace(">","&gt;")
                    .replace("\"","&quot;");
    }
}