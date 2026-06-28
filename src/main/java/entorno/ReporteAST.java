package entorno;

import ast.Nodo;
import ast.NodoPrograma;
import gui.Reportable;
import java.util.List;
import java.util.Stack;

public class ReporteAST implements Reportable {

    private NodoPrograma raiz;
    private List<Nodo> nodosGenerados;

    public ReporteAST() { 
        this.raiz = null; 
        this.nodosGenerados = null;
    }

    public void setRaiz(NodoPrograma raiz) { 
        this.raiz = raiz; 
    }
    
    public void setNodosGenerados(List<Nodo> nodos) { 
        this.nodosGenerados = nodos; 
    }

    @Override
    public int totalElementos() { 
        if (raiz != null) return 1;
        return (nodosGenerados != null) ? nodosGenerados.size() : 0;
    }

    @Override
    public void reset() { 
        raiz = null; 
        if (nodosGenerados != null) nodosGenerados.clear();
    }

    @Override
    public String generarReporte() {
        // Prioridad 1: Si hay una raíz del AST completa
        if (raiz != null) return raiz.toAST(0);
        
        // Prioridad 2: Si no, intentamos generar el reporte desde la lista de nodos
        if (nodosGenerados == null || nodosGenerados.isEmpty()) {
            return "AST no disponible (error en el análisis sintáctico o lista vacía).\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Programa\n");
        // Agregamos dos espacios de indentación para que el visualizador entienda la jerarquía
        for (Nodo n : nodosGenerados) {
            sb.append("  ").append(n.toString()).append("\n"); 
        }
        return sb.toString();
    }

    @Override
    public String generarReporteHTML() {
        if (raiz == null && (nodosGenerados == null || nodosGenerados.isEmpty())) {
            return "<h2>AST no disponible</h2><p style='color:red;'>Revisa los errores sintácticos.</p>";
        }

        // Si hay raíz, usamos toAST, sino usamos la construcción desde la lista
        String textoAST = (raiz != null) ? raiz.toAST(0) : generarReporte();
        return construirGraficoVisJS(textoAST);
    }

    // Algoritmo que convierte el texto indentado en un árbol gráfico interactivo
    private String construirGraficoVisJS(String texto) {
        StringBuilder jsonNodes = new StringBuilder();
        StringBuilder jsonEdges = new StringBuilder();

        String[] lineas = texto.split("\n");
        Stack<int[]> pila = new Stack<>();
        int idCounter = 0;

        for (String linea : lineas) {
            if (linea.trim().isEmpty()) continue;

            // Contar espacios para saber el nivel de jerarquía
            int espacios = 0;
            while (espacios < linea.length() && linea.charAt(espacios) == ' ') {
                espacios++;
            }

            // Limpiar etiqueta para evitar errores en JS
            String etiqueta = linea.trim().replace("'", "\\'").replace("\"", "\\\"");

            // Asignar colores según el tipo de nodo
            String colorFondo = "#ffffff";
            String colorTexto = "#333333";
            String shape = "box";

            if (etiqueta.startsWith("Programa") || etiqueta.startsWith("Funcion") || etiqueta.startsWith("Struct")) {
                colorFondo = "#dcdcaa"; colorTexto = "#000000";
            } else if (etiqueta.matches("^(If|For|Break|Continue|Else|Bloque).*")) {
                colorFondo = "#c586c0"; colorTexto = "#ffffff";
            } else if (etiqueta.startsWith("Literal")) {
                colorFondo = "#ce9178"; colorTexto = "#ffffff"; shape = "ellipse";
            } else if (etiqueta.matches("^(Identificador|DeclVar|Asignacion|Parametro).*")) {
                colorFondo = "#9cdcfe"; colorTexto = "#000000";
            } else {
                colorFondo = "#e0e0e0"; 
            }

            int currentId = idCounter++;
            
            // Crear el nodo en JavaScript
            jsonNodes.append(String.format("{id: %d, label: '%s', shape: '%s', color: {background: '%s', border: '#777'}, font: {color: '%s'}},\n", 
                    currentId, etiqueta, shape, colorFondo, colorTexto));

            // Conectar con el padre correcto basándonos en la indentación
            while (!pila.isEmpty() && pila.peek()[1] >= espacios) {
                pila.pop();
            }

            if (!pila.isEmpty()) {
                int parentId = pila.peek()[0];
                jsonEdges.append(String.format("{from: %d, to: %d},\n", parentId, currentId));
            }

            pila.push(new int[]{currentId, espacios});
        }

        // Ensamblar la plantilla HTML
        return "<!DOCTYPE html>\n" +
               "<html lang=\"es\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <title>Reporte AST - GoLite</title>\n" +
               "    <script type=\"text/javascript\" src=\"https://unpkg.com/vis-network/standalone/umd/vis-network.min.js\"></script>\n" +
               "    <style type=\"text/css\">\n" +
               "        body { margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; overflow: hidden; }\n" +
               "        #info { position: absolute; top: 15px; left: 15px; z-index: 10; background: rgba(255, 255, 255, 0.9); padding: 15px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
               "        #info h2 { margin: 0 0 5px 0; color: #1e376d; }\n" +
               "        #info p { margin: 0; font-size: 13px; color: #555; }\n" +
               "        #mynetwork { width: 100vw; height: 100vh; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div id=\"info\">\n" +
               "        <h2>Árbol de Sintaxis Abstracta (AST)</h2>\n" +
               "        <p>Puedes arrastrar los nodos o hacer scroll para acercar/alejar.</p>\n" +
               "    </div>\n" +
               "    <div id=\"mynetwork\"></div>\n" +
               "    <script type=\"text/javascript\">\n" +
               "        var nodes = new vis.DataSet([\n" + jsonNodes.toString() + "        ]);\n" +
               "        var edges = new vis.DataSet([\n" + jsonEdges.toString() + "        ]);\n" +
               "        var container = document.getElementById('mynetwork');\n" +
               "        var data = { nodes: nodes, edges: edges };\n" +
               "        var options = {\n" +
               "            layout: { hierarchical: { direction: 'UD', sortMethod: 'directed', nodeSpacing: 160, levelSeparation: 120 } },\n" +
               "            physics: false,\n" +
               "            edges: { smooth: { type: 'cubicBezier', forceDirection: 'vertical' }, arrows: { to: { enabled: true, scaleFactor: 0.6 } }, color: '#888' },\n" +
               "            nodes: { font: { size: 15, face: 'monospace' }, borderWidth: 2, shadow: true }\n" +
               "        };\n" +
               "        var network = new vis.Network(container, data, options);\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }
}