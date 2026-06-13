package entorno;

import ast.NodoPrograma;
import gui.Reportable;


 // ReporteAST — Genera la representación visual del AST.
 
  // "El AST deberá incluir todas las estructuras sintácticas del programa,
    // como declaraciones de variables, funciones, sentencias de control,
  //   expresiones y cualquier otro elemento del lenguaje."
 
public class ReporteAST implements Reportable {

    private NodoPrograma raiz;

    public ReporteAST() { this.raiz = null; }

    public void setRaiz(NodoPrograma raiz) { this.raiz = raiz; }

    @Override
    public int totalElementos() { return raiz == null ? 0 : 1; }

    @Override
    public void reset() { raiz = null; }

    @Override
    public String generarReporte() {
        if (raiz == null) return "AST no disponible (error en el análisis sintáctico).\n";
        return raiz.toAST(0);
    }

    @Override
    public String generarReporteHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:monospace;font-size:13px;margin:20px;background:#1e1e1e;color:#d4d4d4;}");
        sb.append("h2{color:#569cd6;}");
        sb.append("pre{background:#0d0d0d;padding:16px;border-radius:6px;");
        sb.append("    overflow:auto;white-space:pre;color:#d4d4d4;line-height:1.5;}");
        sb.append(".nodo-func{color:#dcdcaa;font-weight:bold;}");
        sb.append(".nodo-ctrl{color:#c586c0;}");
        sb.append(".nodo-lit {color:#ce9178;}");
        sb.append(".nodo-var {color:#9cdcfe;}");
        sb.append(".nodo-op  {color:#d4d4d4;}");
        sb.append("</style></head><body>");
        sb.append("<h2>Reporte AST — GoLite</h2>");

        if (raiz == null) {
            sb.append("<p style='color:#f48771;'>AST no disponible (revisa los errores sintácticos).</p>");
        } else {
            String texto = raiz.toAST(0);
            sb.append("<pre>").append(colorearAST(escapeHTML(texto))).append("</pre>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    
     // Aplica color a palabras clave del AST para mejor legibilidad.
    
    private String colorearAST(String texto) {
        // Nodos de función
        texto = texto.replaceAll("(Funcion:.*)", "<span class='nodo-func'>$1</span>");
        texto = texto.replaceAll("(Programa)", "<span class='nodo-func'>$1</span>");
        // Nodos de control
        texto = texto.replaceAll("(If|For|Break|Continue|ElseIf|Else|Then|Condicion:)",
                                  "<span class='nodo-ctrl'>$1</span>");
        // Literales
        texto = texto.replaceAll("(Literal\\([^)]+\\):.*)", "<span class='nodo-lit'>$1</span>");
        // Variables
        texto = texto.replaceAll("(Identificador:.*|DeclVar:.*|DeclCorta:.*|Asignacion:.*)",
                                  "<span class='nodo-var'>$1</span>");
        return texto;
    }

    private String escapeHTML(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}