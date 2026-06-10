package compilador;

import ast.NodoPrograma;
import entorno.Entorno;
import entorno.ReporteAST;
import entorno.TablaSimbolos;
import errores.ErrorManager;
import excepciones.BreakException;
import excepciones.ContinueException;
import excepciones.ErrorSemanticoException;
import lexer.Lexer;
import parser.Parser;
import tokens.TablaTokens;
import tokens.Token;

import java.io.StringReader;
import java.util.List;

/**
 * Compiler — Controlador principal del intérprete GoLite.
 * Completamente acoplado al ecosistema tolerante a fallos y recolección múltiple de errores.
 */
public class Compiler {

    private static Compiler instancia;
    private Compiler() {}

    public static synchronized Compiler getInstance() {
        if (instancia == null) instancia = new Compiler();
        return instancia;
    }

    private List<Token>  tokens;
    private NodoPrograma ast;
    private Entorno      entornoGlobal;
    private String       salidaConsola = "";

    public String compilar(String codigoFuente) {
        // 1. Reset total de estados (higiene de memoria entre ejecuciones)
        resetEstado();

        StringBuilder consola = new StringBuilder();
        consola.append("[INFO] Compilación iniciada\n─────────────────────────────────\n");

        // ════════════════════════════════════════════════════════════════════
        // FASE 1 — ANÁLISIS LÉXICO
        // ════════════════════════════════════════════════════════════════════
        Lexer lexer;
        try {
            lexer = new Lexer(new StringReader(codigoFuente));
        } catch (Exception e) {
            return "[FATAL] Error crítico al inicializar el Lexer: " + e.getMessage();
        }

        // ════════════════════════════════════════════════════════════════════
        // FASE 2 — ANÁLISIS SINTÁCTICO
        // ════════════════════════════════════════════════════════════════════
        try {
            Parser parser = new Parser(lexer);
            java_cup.runtime.Symbol resultado = parser.parse();

            this.tokens = lexer.getListaTokens();
            if (this.tokens != null) {
                for (Token t : tokens) TablaTokens.getInstance().agregar(t);
            }

            consola.append("[INFO] Tokens reconocidos: ").append(TablaTokens.getInstance().totalTokens()).append("\n");

            if (ErrorManager.getInstance().hayErroresLexicos()) {
                consola.append("[WARN] Se encontraron errores léxicos.\n");
            }

            // Si hay errores sintácticos severos, interrumpimos antes de pasar al AST
            if (ErrorManager.getInstance().hayErroresSintacticos()) {
                consola.append("[ERROR] Errores sintácticos detectados. No se puede construir el AST.\n");
                this.salidaConsola = consola.toString();
                return this.salidaConsola;
            }

            if (resultado != null && resultado.value instanceof NodoPrograma) {
                this.ast = (NodoPrograma) resultado.value;
                consola.append("[OK]   AST generado con éxito.\n");
            } else {
                consola.append("[ERROR] El parser no generó un AST válido.\n");
                this.salidaConsola = consola.toString();
                return this.salidaConsola;
            }
        } catch (Exception e) {
            return "[ERROR] Fallo crítico en la fase sintáctica: " + e.getMessage();
        }

        // ════════════════════════════════════════════════════════════════════
        // FASE 3 — INTÉRPRETE (TOLERANTE A FALLOS SEMÁNTICOS)
        // ════════════════════════════════════════════════════════════════════
        consola.append("─────────────────────────────────\n[INFO] Ejecutando AST...\n");

        if (this.ast == null) {
            return consola.append("[ERROR] AST inexistente, ejecución abortada.").toString();
        }

        try {
            // Se ejecuta la raíz del programa. Las fallas semánticas internas poblarán 
            // el ErrorManager sin arrojar excepciones que rompan el ciclo del Compiler.
            this.ast.execute(this.entornoGlobal);
            
            // Recuperar lo que se haya acumulado de fmt.Println en el entorno global
            String salidaPrograma = entornoGlobal.getSalidaConsola();
            if (salidaPrograma != null && !salidaPrograma.isEmpty()) {
                consola.append(salidaPrograma);
            }

            // Comprobación de salud post-ejecución
            if (ErrorManager.getInstance().hayErroresSemanticos()) {
                consola.append("\n[WARN] Ejecución finalizada, pero se registraron errores semánticos (Ver Reporte).\n");
            } else {
                consola.append("\n[OK]   Ejecución completada con éxito.\n");
            }

        } catch (ErrorSemanticoException e) {
            // Salvaguarda para nodos legados que aún dependan de excepciones puntuales
            ErrorManager.getInstance().agregarSemantico(e.getDescripcion(), e.getLinea(), e.getColumna());
            consola.append("[ERROR Semántico Intermitente] ").append(e.getDescripcion()).append("\n");
        } catch (BreakException | ContinueException e) {
            // Ajuste defensivo: Evitamos usar métodos get que causan errores de compilación
            int ln = 0;
            int cl = 0;
            
            ErrorManager.getInstance().agregarSemantico(
                "Sentencia de control de bucle (break o continue) fuera de ámbito.", 
                ln, cl
            );
            consola.append("[ERROR] Sentencia de control fuera de un ciclo activo.\n");
        } catch (Exception e) {
            consola.append("[CRITICAL] Excepción no controlada en ejecución: ").append(e.toString()).append("\n");
            e.printStackTrace(); 
        }

        // ════════════════════════════════════════════════════════════════════
        // CIERRE Y RESUMEN METRICAS
        // ════════════════════════════════════════════════════════════════════
        consola.append("─────────────────────────────────\n");
        int totalErrores = ErrorManager.getInstance().totalErrores();
        
        if (totalErrores > 0) {
            consola.append("[RESUMEN] Proceso terminado. Se detectaron ").append(totalErrores).append(" errores en total.\n");
        } else {
            consola.append("[RESUMEN] ¡Compilación y ejecución exitosa sin fallos!\n");
        }

        this.salidaConsola = consola.toString();
        return this.salidaConsola;
    }

    private void resetEstado() {
        ErrorManager.getInstance().reset();
        TablaTokens.getInstance().reset();
        this.ast = null;
        this.entornoGlobal = new Entorno();
        this.salidaConsola = "";
        this.tokens = null;
    }

    // ─── Getters Defensivos ────────────────────────────────────────────────────
    
    public String getSalidaConsola() { return salidaConsola != null ? salidaConsola : ""; }
    public boolean hayErrores()       { return ErrorManager.getInstance().hayErrores(); }
    public boolean hayAST()           { return ast != null; }

    public String getReporteErroresHTML() { 
        return ErrorManager.getInstance().generarReporteHTML(); 
    }

    public String getReporteTokensHTML() { 
        return TablaTokens.getInstance().generarReporteHTML(); 
    }

    public String getReporteSimbolosHTML() {
        try {
            if (entornoGlobal == null) return "Ejecuta el código primero.";
            return new TablaSimbolos(entornoGlobal.getHistoricoSimbolos()).generarReporteHTML();
        } catch (Exception e) {
            return "Error al generar tabla de símbolos: " + e.getMessage();
        }
    }

    public String getReporteASTHTML() {
        try {
            ReporteAST rpt = new ReporteAST();
            if (ast != null) rpt.setRaiz(ast);
            return rpt.generarReporteHTML();
        } catch (Exception e) {
            return "Error al generar reporte AST: " + e.getMessage();
        }
    }
}