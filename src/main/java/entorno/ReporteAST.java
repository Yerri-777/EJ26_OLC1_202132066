package entorno;

import ast.Nodo;
import ast.NodoPrograma;
import gui.Reportable;
import java.util.List;
import java.util.Stack;

public class ReporteAST implements Reportable {
private String textoASTDirecto = null;
    private NodoPrograma raiz;
    private List<Nodo> nodosGenerados;

    public void setTextoAST(String texto) {
    this.textoASTDirecto = texto;
}
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
    if (textoASTDirecto != null && !textoASTDirecto.isEmpty())
        return textoASTDirecto;
    if (raiz != null) return raiz.toAST(0);
    if (nodosGenerados == null || nodosGenerados.isEmpty())
        return "AST no disponible.\n";
    StringBuilder sb = new StringBuilder();
    sb.append("Programa\n");
    for (Nodo n : nodosGenerados)
        sb.append("  ").append(n.toString()).append("\n");
    return sb.toString();
}

@Override
public String generarReporteHTML() {
    String texto = generarReporte();
    if (texto == null || texto.trim().isEmpty())
        return "<h2>AST vacío</h2>";
    return construirGraficoVisJS(texto); 
    
}
private String construirGraficoVisJS(String texto) {
    String textoNorm = texto
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\t", "  ");

    String[] lineas = textoNorm.split("\n");

    // Construir lista JSON de nodos con nivel, label y color
    StringBuilder jsonNodos = new StringBuilder();
    jsonNodos.append("[\n");

    java.util.Stack<int[]> pila = new java.util.Stack<>();
    int idCounter = 0;
    boolean primero = true;

    for (String lineaRaw : lineas) {
        if (lineaRaw == null || lineaRaw.trim().isEmpty()) continue;

        int espacios = 0;
        for (int i = 0; i < lineaRaw.length(); i++) {
            if (lineaRaw.charAt(i) == ' ') espacios++;
            else break;
        }
        int nivel = espacios / 2;

        String etiqueta = lineaRaw.trim();
        if (etiqueta.isEmpty()) continue;
        etiqueta = etiqueta
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
        if (etiqueta.length() > 50) etiqueta = etiqueta.substring(0, 47) + "...";

        // Sacar de pila los del mismo nivel o más profundos
        while (!pila.isEmpty() && pila.peek()[1] >= nivel) pila.pop();
        int parentId = pila.isEmpty() ? -1 : pila.peek()[0];
        int currentId = idCounter++;

        // Color por tipo
        String color = "#555577";
        String etqL = etiqueta.toLowerCase();
        if (nivel == 0)                                                              color = "#1e376d";
        else if (etqL.startsWith("funcion") || etqL.startsWith("struct"))           color = "#7a7a30";
        else if (etqL.startsWith("for") || etqL.startsWith("switch"))               color = "#7a3a7a";
        else if (etqL.startsWith("if") || etqL.startsWith("else") || etqL.startsWith("bloque")) color = "#5a3a8a";
        else if (etqL.startsWith("return") || etqL.startsWith("break"))             color = "#8a2020";
        else if (etqL.startsWith("declvar") || etqL.startsWith("var"))              color = "#1a5a7a";
        else if (etqL.startsWith("asig"))                                            color = "#1a4a6a";
        else if (etqL.startsWith("llamada") || etqL.startsWith("call"))             color = "#1a4a8a";
        else if (etqL.startsWith("literal") || etqL.startsWith("lit"))              color = "#7a4a30";
        else if (etqL.startsWith("op"))                                              color = "#7a6a20";
        else { int s = Math.min(nivel * 12, 60); color = String.format("#%02x%02x%02x", 60+s, 60+s, 80+s); }

        if (!primero) jsonNodos.append(",\n");
        primero = false;
        jsonNodos.append(String.format(
            "{id:%d,label:'%s',nivel:%d,padre:%d,color:'%s'}",
            currentId, etiqueta, nivel, parentId, color
        ));

        pila.push(new int[]{currentId, nivel});
    }
    jsonNodos.append("\n]");

    int totalNodos = idCounter;

    // HTML con Canvas puro — CERO dependencias externas
    return "<!DOCTYPE html>\n"
        + "<html lang='es'><head><meta charset='UTF-8'>"
        + "<title>AST GoLite</title>"
        + "<style>"
        + "*{box-sizing:border-box;margin:0;padding:0}"
        + "body{background:#1e1e1e;color:#d4d4d4;font-family:'Segoe UI',Arial,sans-serif;overflow:hidden}"
        + "#toolbar{position:absolute;top:10px;left:10px;z-index:10;background:rgba(30,30,35,0.95);"
        + "border:1px solid #444;border-radius:8px;padding:10px 14px;width:280px;"
        + "box-shadow:0 4px 16px rgba(0,0,0,0.6)}"
        + "#toolbar h2{color:#dcdcaa;font-size:13px;margin-bottom:6px}"
        + "button{background:#2d2d30;border:1px solid #555;color:#d4d4d4;border-radius:4px;"
        + "padding:4px 10px;font-size:11px;cursor:pointer;margin:2px}"
        + "button:hover{background:#3e3e42}"
        + "#stats{font-size:11px;color:#4ec9b0;margin-top:6px}"
        + "#search{width:100%;background:#252526;border:1px solid #555;color:#d4d4d4;"
        + "border-radius:4px;padding:4px 8px;font-size:11px;margin-bottom:6px}"
        + "#legend{position:absolute;bottom:10px;right:10px;z-index:10;"
        + "background:rgba(30,30,35,0.92);border:1px solid #444;border-radius:8px;"
        + "padding:8px 12px;font-size:11px}"
        + "#legend h3{color:#dcdcaa;margin-bottom:5px;font-size:11px}"
        + ".li{display:flex;align-items:center;gap:5px;margin-bottom:2px}"
        + ".ld{width:10px;height:10px;border-radius:2px}"
        + "#canvas{display:block}"
        + "</style></head><body>"
        + "<div id='toolbar'>"
        + "<h2>&#x1F333; AST - GoLite</h2>"
        + "<input id='search' type='text' placeholder='Buscar nodo...' oninput='buscar(this.value)'>"
        + "<div>"
        + "<button onclick='ajustar()'>&#x26F6; Ajustar</button>"
        + "<button onclick='zoomIn()'>&#x2B;Zoom</button>"
        + "<button onclick='zoomOut()'>&#x2212;Zoom</button>"
        + "<button onclick='resetView()'>&#x21BA; Reset</button>"
        + "</div>"
        + "<div id='stats'>Nodos: " + totalNodos + "</div>"
        + "</div>"
        + "<div id='legend'>"
        + "<h3>Leyenda</h3>"
        + "<div class='li'><div class='ld' style='background:#1e376d'></div>Programa (raiz)</div>"
        + "<div class='li'><div class='ld' style='background:#7a7a30'></div>Funcion/Struct</div>"
        + "<div class='li'><div class='ld' style='background:#7a3a7a'></div>For/Switch</div>"
        + "<div class='li'><div class='ld' style='background:#5a3a8a'></div>If/Bloque</div>"
        + "<div class='li'><div class='ld' style='background:#8a2020'></div>Return/Break</div>"
        + "<div class='li'><div class='ld' style='background:#1a5a7a'></div>DeclVar</div>"
        + "<div class='li'><div class='ld' style='background:#1a4a8a'></div>Llamada</div>"
        + "<div class='li'><div class='ld' style='background:#7a4a30'></div>Literal</div>"
        + "</div>"
        + "<canvas id='canvas'></canvas>"
        + "<script>\n"
        + "var NODOS = " + jsonNodos.toString() + ";\n"
        + "var canvas = document.getElementById('canvas');\n"
        + "var ctx = canvas.getContext('2d');\n"
        + "var W = window.innerWidth, H = window.innerHeight;\n"
        + "canvas.width = W; canvas.height = H;\n"
        + "window.addEventListener('resize', function(){ W=canvas.width=window.innerWidth; H=canvas.height=window.innerHeight; dibujar(); });\n"

        // ── Layout: árbol jerárquico con posiciones X/Y ────────────
        + "var posiciones = {};\n"
        + "var contadorPorNivel = {};\n"
        + "var anchoPorNivel = {};\n"

        // Contar nodos por nivel para distribuir X
        + "NODOS.forEach(function(n){\n"
        + "  contadorPorNivel[n.nivel] = (contadorPorNivel[n.nivel]||0)+1;\n"
        + "});\n"

        // Primer pasada: asignar índice dentro del nivel
        + "var indicePorNivel = {};\n"
        + "NODOS.forEach(function(n){\n"
        + "  if(indicePorNivel[n.nivel]===undefined) indicePorNivel[n.nivel]=0;\n"
        + "  n._idx = indicePorNivel[n.nivel]++;\n"
        + "});\n"

        // Separación entre nodos
        + "var SEP_X = 160, SEP_Y = 90, NODO_W = 140, NODO_H = 32;\n"
        + "var offsetX = 0, offsetY = 40, escala = 1;\n"

        // Calcular posición X centrada por nivel
        + "NODOS.forEach(function(n){\n"
        + "  var total = contadorPorNivel[n.nivel];\n"
        + "  var anchoTotal = total * SEP_X;\n"
        + "  var x = (n._idx * SEP_X) - (anchoTotal/2) + SEP_X/2;\n"
        + "  var y = n.nivel * SEP_Y + 60;\n"
        + "  posiciones[n.id] = {x:x, y:y};\n"
        + "});\n"

        // ── Viewport: pan + zoom ───────────────────────────────────
        + "var panX = 0, panY = 0, zoom = 1;\n"
        + "var dragging = false, lastMX = 0, lastMY = 0;\n"
        + "canvas.addEventListener('mousedown', function(e){ dragging=true; lastMX=e.clientX; lastMY=e.clientY; });\n"
        + "canvas.addEventListener('mouseup',   function(){ dragging=false; });\n"
        + "canvas.addEventListener('mouseleave',function(){ dragging=false; });\n"
        + "canvas.addEventListener('mousemove', function(e){\n"
        + "  if(!dragging) return;\n"
        + "  panX += e.clientX - lastMX; panY += e.clientY - lastMY;\n"
        + "  lastMX=e.clientX; lastMY=e.clientY; dibujar();\n"
        + "});\n"
        + "canvas.addEventListener('wheel', function(e){\n"
        + "  e.preventDefault();\n"
        + "  var delta = e.deltaY > 0 ? 0.9 : 1.1;\n"
        + "  zoom = Math.max(0.1, Math.min(5, zoom*delta));\n"
        + "  dibujar();\n"
        + "}, {passive:false});\n"

        // ── Click en nodo ──────────────────────────────────────────
        + "var seleccionado = -1;\n"
        + "canvas.addEventListener('click', function(e){\n"
        + "  var rect = canvas.getBoundingClientRect();\n"
        + "  var mx = (e.clientX - rect.left - panX - W/2) / zoom;\n"
        + "  var my = (e.clientY - rect.top  - panY - 60) / zoom;\n"
        + "  seleccionado = -1;\n"
        + "  NODOS.forEach(function(n){\n"
        + "    var p = posiciones[n.id];\n"
        + "    if(Math.abs(mx-p.x)<NODO_W/2 && Math.abs(my-p.y)<NODO_H/2) seleccionado=n.id;\n"
        + "  });\n"
        + "  dibujar();\n"
        + "});\n"

        // ── Búsqueda ───────────────────────────────────────────────
        + "var termBusqueda = '';\n"
        + "function buscar(t){ termBusqueda=t.toLowerCase(); dibujar(); }\n"

        // ── Controles ──────────────────────────────────────────────
        + "function ajustar(){\n"
        + "  if(NODOS.length===0) return;\n"
        + "  var xs=NODOS.map(function(n){return posiciones[n.id].x;});\n"
        + "  var ys=NODOS.map(function(n){return posiciones[n.id].y;});\n"
        + "  var minX=Math.min.apply(null,xs), maxX=Math.max.apply(null,xs);\n"
        + "  var minY=Math.min.apply(null,ys), maxY=Math.max.apply(null,ys);\n"
        + "  var ancho=maxX-minX+NODO_W+40, alto=maxY-minY+NODO_H+80;\n"
        + "  zoom=Math.min(W/ancho, (H-80)/alto, 2);\n"
        + "  panX=-(minX+maxX)/2*zoom; panY=-(minY)/2*zoom+40;\n"
        + "  dibujar();\n"
        + "}\n"
        + "function zoomIn(){ zoom=Math.min(5,zoom*1.2); dibujar(); }\n"
        + "function zoomOut(){ zoom=Math.max(0.1,zoom*0.8); dibujar(); }\n"
        + "function resetView(){ zoom=1; panX=0; panY=0; dibujar(); }\n"

        // ── Dibujar ────────────────────────────────────────────────
        + "function dibujar(){\n"
        + "  ctx.clearRect(0,0,W,H);\n"
        + "  ctx.save();\n"
        + "  ctx.translate(W/2+panX, 60+panY);\n"
        + "  ctx.scale(zoom,zoom);\n"

        // Dibujar aristas primero
        + "  ctx.strokeStyle='#556';\n"
        + "  ctx.lineWidth=1/zoom;\n"
        + "  NODOS.forEach(function(n){\n"
        + "    if(n.padre<0) return;\n"
        + "    var p=posiciones[n.id], pp=posiciones[n.padre];\n"
        + "    if(!p||!pp) return;\n"
        + "    var resaltado = termBusqueda && n.label.toLowerCase().indexOf(termBusqueda)>=0;\n"
        + "    ctx.beginPath();\n"
        + "    ctx.strokeStyle = resaltado ? '#9cdcfe' : '#445566';\n"
        + "    ctx.lineWidth = (resaltado ? 2 : 1)/zoom;\n"
        // Bezier curva suave
        + "    ctx.moveTo(pp.x, pp.y+NODO_H/2);\n"
        + "    ctx.bezierCurveTo(pp.x, pp.y+NODO_H/2+SEP_Y*0.4, p.x, p.y-NODO_H/2-SEP_Y*0.4, p.x, p.y-NODO_H/2);\n"
        + "    ctx.stroke();\n"
        // Flecha
        + "    var ax=p.x, ay=p.y-NODO_H/2;\n"
        + "    ctx.fillStyle = resaltado ? '#9cdcfe' : '#445566';\n"
        + "    ctx.beginPath();\n"
        + "    ctx.moveTo(ax,ay);\n"
        + "    ctx.lineTo(ax-4/zoom,ay-8/zoom);\n"
        + "    ctx.lineTo(ax+4/zoom,ay-8/zoom);\n"
        + "    ctx.closePath(); ctx.fill();\n"
        + "  });\n"

        // Dibujar nodos
        + "  NODOS.forEach(function(n){\n"
        + "    var p=posiciones[n.id];\n"
        + "    if(!p) return;\n"
        + "    var esSel = n.id===seleccionado;\n"
        + "    var esBusq = termBusqueda && n.label.toLowerCase().indexOf(termBusqueda)>=0;\n"
        + "    var x=p.x-NODO_W/2, y=p.y-NODO_H/2;\n"
        // Sombra de selección
        + "    if(esSel){\n"
        + "      ctx.shadowColor='#9cdcfe'; ctx.shadowBlur=12/zoom;\n"
        + "    } else { ctx.shadowBlur=0; }\n"
        // Fondo
        + "    ctx.fillStyle = esBusq ? '#3a5a3a' : n.color;\n"
        + "    ctx.beginPath();\n"
        + "    ctx.roundRect(x,y,NODO_W,NODO_H,5/zoom);\n"
        + "    ctx.fill();\n"
        // Borde
        + "    ctx.strokeStyle = esSel ? '#9cdcfe' : esBusq ? '#6ec96e' : '#888';\n"
        + "    ctx.lineWidth = (esSel?2.5:1)/zoom;\n"
        + "    ctx.stroke();\n"
        + "    ctx.shadowBlur=0;\n"
        // Texto
        + "    ctx.fillStyle='#e8e8e8';\n"
        + "    ctx.font=(13/zoom)+'px Consolas,monospace';\n"
        + "    ctx.textAlign='center';\n"
        + "    ctx.textBaseline='middle';\n"
        + "    ctx.fillText(n.label, p.x, p.y);\n"
        + "  });\n"
        + "  ctx.restore();\n"
        + "}\n"

        // ── Init ───────────────────────────────────────────────────
        + "ajustar();\n"
        + "</script></body></html>";
}

// Aclara color hex para highlight
private String lighten(String hex) {
    try {
        if (hex == null || hex.length() < 7) return "#888888";
        int r = Integer.parseInt(hex.substring(1,3),16);
        int g = Integer.parseInt(hex.substring(3,5),16);
        int b = Integer.parseInt(hex.substring(5,7),16);
        return String.format("#%02x%02x%02x",
            Math.min(255,r+50), Math.min(255,g+50), Math.min(255,b+50));
    } catch(Exception e){ return "#888888"; }
} }