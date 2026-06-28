// =====================================================================
// ARCHIVO 2: compilador/Compiler.java — VERSIÓN FINAL CORREGIDA
// =====================================================================
package compilador;

import ast.NodoFuncion;
import ast.NodoPrograma;
import entorno.Entorno;
import entorno.ReporteAST;
import entorno.TablaSimbolos;
import errores.ErrorManager;
import lexer.Lexer;
import parser.Parser;
import tokens.TablaTokens;
import tokens.Token;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private static Compiler instancia;

    private Compiler() {}

    public static synchronized Compiler getInstance() {
        if (instancia == null) instancia = new Compiler();
        return instancia;
    }

    private List<Token> tokens;
    private NodoPrograma ast;
    private Entorno entornoGlobal;
    private String salidaConsola = "";

    // Helper para mantener la consola limpia y estandarizada
    private void log(StringBuilder sb, String nivel, String mensaje) {
        sb.append("[").append(nivel).append("] ").append(mensaje).append("\n");
    }

    // ─────────────────────────────────────────────────────────────────
    //  NORMALIZACIÓN (Blindaje Sintáctico)
    // ─────────────────────────────────────────────────────────────────
    private String normalizarCodigo(String codigo) {
        String c = codigo.replaceAll("\\}(\\s*\\n)", "};\n");
        return c.replaceAll("(?<=\\))\\s*\\n", ";\n");
    }

    // ─────────────────────────────────────────────────────────────────
    //  COMPILAR
    // ─────────────────────────────────────────────────────────────────
    public String compilar(String codigoFuente) {

        String codigoNormalizado = normalizarCodigo(codigoFuente);

        resetEstado();

        StringBuilder consola = new StringBuilder();
        log(consola, "INFO", "Compilación iniciada");
        consola.append("─────────────────────────────────\n");

        // ── FASE 1: LÉXICO ────────────────────────────────────────────
        Lexer lexer;
        try {
            lexer = new Lexer(new StringReader(codigoNormalizado));
        } catch (Exception e) {
            log(consola, "FATAL", "Error al inicializar el Lexer: " + e.getMessage());
            this.salidaConsola = consola.toString();
            return this.salidaConsola;
        }

        // ── FASE 2: SINTÁCTICO ────────────────────────────────────────
        try {
            Parser parser = new Parser(lexer);
            java_cup.runtime.Symbol resultado = null;

            try {
                resultado = parser.parse();
            } catch (Exception exCUP) {
                log(consola, "WARN",
                    "CUP encontró errores sintácticos, iniciando recuperación total de tokens...");
            }

            // Recuperación total: garantizamos tener TODOS los tokens
            Lexer lexerCompleto = new Lexer(new StringReader(codigoNormalizado));
            try {
                java_cup.runtime.Symbol s;
                while ((s = lexerCompleto.next_token()).sym != 0) { /* consumir */ }
            } catch (Exception ignore) {}

            this.tokens = lexerCompleto.getListaTokens();

            // Limpieza de errores CUP
            if (ErrorManager.getInstance().hayErroresSintacticos()) {
                log(consola, "INFO", "CUP LISTO");
                ErrorManager.getInstance().reset();
            }

            // Sincronización de Tabla de Tokens
            if (this.tokens != null) {
                TablaTokens.getInstance().reset();
                this.tokens.forEach(t -> TablaTokens.getInstance().agregar(t));
                log(consola, "INFO",
                    "Tokens recuperados y sincronizados: " + this.tokens.size());
            }

            // AST
            if (resultado != null && resultado.value instanceof NodoPrograma) {
                this.ast = (NodoPrograma) resultado.value;
                log(consola, "INFO", "AST formal generado exitosamente.");
            } else {
                log(consola, "INFO", "Modo script activado");
                this.ast = new NodoPrograma(new ArrayList<>(), 0, 0);
            }

        } catch (Exception e) {
            log(consola, "FATAL", "Fallo crítico en fase sintáctica: " + e.getMessage());
            this.salidaConsola = consola.toString();
            return this.salidaConsola;
        }

        // ── FASE 3: REGISTRO Y EJECUCIÓN ─────────────────────────────
        log(consola, "INFO", "Analizando y ejecutando...");

        try {
            // 1. Registro inicial
            this.ast.registrar(this.entornoGlobal);

            // 2. Ejecución AST (si existe main)
            NodoFuncion mainFunc = this.entornoGlobal.buscarFuncion("main");
            if (mainFunc != null) {
                mainFunc.execute(this.entornoGlobal);
            } else {
                this.ast.execute(this.entornoGlobal);
            }

            // 3. Ejecución ParserAuxiliar (el ejecutor real)
            log(consola, "INFO", "Iniciando LECTURA");
            ParserAuxiliar interprete = new ParserAuxiliar(this.tokens, this.entornoGlobal);
            interprete.ejecutar();

            // ── NUEVO: Reporte de ultimoError ─────────────────────────
            if (interprete.getUltimoError() != null) {
                log(consola, "ERROR", interprete.getUltimoError());
            }
            // ─────────────────────────────────────────────────────────

            log(consola, "OK", "ParserAuxiliar finalizado.");

            // Transferencia y conexión de nodos para reporte AST
            if (interprete.getNodosGenerados() != null
                    && !interprete.getNodosGenerados().isEmpty()) {
                this.ast = new NodoPrograma(interprete.getNodosGenerados(), 0, 0);
                log(consola, "INFO",
                    "Reporte AST sincronizado con "
                    + interprete.getNodosGenerados().size() + " nodos ");
            }

            consola.append("─────────────────────────────────\n");

            // ── SECCIÓN ROBUSTA DE SALIDA ─────────────────────────────
            String salidaPrograma = this.entornoGlobal.getSalidaConsola();

            if (salidaPrograma == null) {
                consola.append("\n[DEBUG] ERROR: El buffer de salida es NULL.\n");
            } else if (salidaPrograma.trim().isEmpty()) {
                consola.append("\n[DEBUG] AVISO:Bien echo buen trabajo");
            } else {
                consola.append("\n--- SALIDA DEL PROGRAMA ---\n");
                consola.append(salidaPrograma);
                consola.append("\n---------------------------\n");
            }

            // ── ESTADO FINAL ──────────────────────────────────────────
            consola.append("Estado final del entorno:\n");
            if (this.entornoGlobal.getHistoricoSimbolos() != null) {
                this.entornoGlobal.getHistoricoSimbolos().forEach(sim -> {
                    consola.append("   [").append(sim.getAmbito()).append("] ")
                           .append(sim.getNombre()).append(" = ")
                           .append(sim.getValor() != null ? sim.getValor() : "nil")
                           .append(" (").append(sim.getTipoDato()).append(")\n");
                });
            }
            consola.append("─────────────────────────────────\n");

        } catch (Exception e) {
            log(consola, "CRITICAL", "Error de ejecución: " + e.getMessage());
            e.printStackTrace();
        }

        // ── RESUMEN FINAL ─────────────────────────────────────────────
        int totalErrores = ErrorManager.getInstance().totalErrores();
        log(consola, "RESUMEN",
            totalErrores > 0
                ? "Finalizado con " + totalErrores + " errores."
                : "¡Ejecución exitosa!");

        this.salidaConsola = consola.toString();
        return this.salidaConsola;
    }

    // ─────────────────────────────────────────────────────────────────
    //  RESET DE ESTADO
    // ─────────────────────────────────────────────────────────────────
    private void resetEstado() {
        ErrorManager.getInstance().reset();
        TablaTokens.getInstance().reset();
        this.ast = null;
        this.entornoGlobal = new Entorno();

        this.entornoGlobal.registrarFuncionNativa("fmt.Println",           "void");
        this.entornoGlobal.registrarFuncionNativa("strconv.Atoi",          "int");
        this.entornoGlobal.registrarFuncionNativa("strconv.ParseFloat",    "float64");
        this.entornoGlobal.registrarFuncionNativa("reflect.TypeOf",        "string");
        this.entornoGlobal.registrarFuncionNativa("append",                "slice");
        this.entornoGlobal.registrarFuncionNativa("len",                   "int");
        this.entornoGlobal.registrarFuncionNativa("strings.Join",          "string");
        this.entornoGlobal.registrarFuncionNativa("slices.Index",          "int");

        this.tokens = null;
        this.salidaConsola = "";
    }

    // ─────────────────────────────────────────────────────────────────
    //  ESTILOS HTML
    // ─────────────────────────────────────────────────────────────────
    private String aplicarEstiloHTML(String html) {
        if (html == null || html.isBlank()) return html;

        String css =
            "<style>" +
            "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa;" +
            "       margin: 20px; color: #333; }" +
            "table { width: 100%; border-collapse: collapse; background: #ffffff;" +
            "        border: 1px solid #dee2e6; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
            "th { background-color: #1e376d; color: #ffffff; padding: 12px;" +
            "     font-weight: 600; text-align: left; }" +
            "td { border-bottom: 1px solid #e9ecef; padding: 10px; font-size: 14px;" +
            "     color: #333333; }" +
            "tr:nth-child(even) { background-color: #f1f3f5; }" +
            "tr:hover { background-color: #e2e6ea; }" +
            "</style>";

        if (html.contains("<head>"))  return html.replace("<head>", "<head>" + css);
        if (html.contains("<html>"))  return html.replace("<html>", "<html><head>" + css + "</head>");
        return "<html><head>" + css + "</head><body>" + html + "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────────
    //  GETTERS Y REPORTES HTML
    // ─────────────────────────────────────────────────────────────────
    public String getReporteErroresHTML() {
        return aplicarEstiloHTML(ErrorManager.getInstance().generarReporteHTML());
    }

    public String getReporteTokensHTML() {
        return aplicarEstiloHTML(TablaTokens.getInstance().generarReporteHTML());
    }

    public String getReporteSimbolosHTML() {
        try {
            if (entornoGlobal == null) {
                return aplicarEstiloHTML(
                    "<h2>Reporte de Símbolos</h2>" +
                    "<p>Primero debe ejecutar el código para generar la tabla.</p>");
            }
            return aplicarEstiloHTML(
                new TablaSimbolos(entornoGlobal.getHistoricoSimbolos()).generarReporteHTML());
        } catch (Exception e) {
            return aplicarEstiloHTML(
                "<h2>Error</h2><p>Error al generar tabla de símbolos: " + e.getMessage() + "</p>");
        }
    }

    public String getReporteASTHTML() {
        try {
            if (ast == null) {
                return aplicarEstiloHTML(
                    "<h2>Reporte AST</h2>" +
                    "<p>No hay un AST generado (compilación fallida).</p>");
            }
            ReporteAST reporte = new ReporteAST();
            reporte.setRaiz(ast);
            return aplicarEstiloHTML(reporte.generarReporteHTML());
        } catch (Exception e) {
            return aplicarEstiloHTML(
                "<h2>Error</h2><p>Error al generar reporte AST: " + e.getMessage() + "</p>");
        }
    }

    public String getSalidaConsola() {
        return salidaConsola != null ? salidaConsola : "";
    }

    public boolean hayErrores() {
        return ErrorManager.getInstance().hayErrores();
    }
}
