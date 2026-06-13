package compilador;

import ast.NodoFuncion;
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


public class Compiler {

    private static Compiler instancia;

    private Compiler() {
    }

    public static synchronized Compiler getInstance() {
        if (instancia == null) {
            instancia = new Compiler();
        }
        return instancia;
    }

    private List<Token> tokens;
    private NodoPrograma ast;
    private Entorno entornoGlobal;
    private String salidaConsola = "";

    public String compilar(String codigoFuente) {

        // Reset de estado
        resetEstado();

        StringBuilder consola = new StringBuilder();
        consola.append("[INFO] Compilación iniciada\n");
        consola.append("─────────────────────────────────\n");

   
        // FASE 1 - LÉXICO
    

        Lexer lexer;

        try {
            lexer = new Lexer(new StringReader(codigoFuente));
        } catch (Exception e) {
            return "[FATAL] Error al inicializar el Lexer: " + e.getMessage();
        }

        
        // FASE 2 - SINTÁCTICO
     

        try {

            Parser parser = new Parser(lexer);
            java_cup.runtime.Symbol resultado = parser.parse();

            this.tokens = lexer.getListaTokens();

            if (this.tokens != null) {
                for (Token t : tokens) {
                    TablaTokens.getInstance().agregar(t);
                }
            }

            consola.append("[INFO] Tokens reconocidos: ")
                    .append(TablaTokens.getInstance().totalTokens())
                    .append("\n");

            if (ErrorManager.getInstance().hayErroresLexicos()) {
                consola.append("[WARN] Se encontraron errores léxicos.\n");
            }

            if (ErrorManager.getInstance().hayErroresSintacticos()) {
                consola.append("[ERROR] Errores sintácticos detectados. No se puede construir el AST.\n");
                this.salidaConsola = consola.toString();
                return this.salidaConsola;
            }

            if (resultado != null && resultado.value instanceof NodoPrograma) {

                this.ast = (NodoPrograma) resultado.value;
                consola.append("[OK] AST generado con éxito.\n");

                consola.append("─────────────────────────────────\n");
                consola.append("[DEBUG] Estructura del AST:\n");
                consola.append(this.ast.toAST(0)); 
                consola.append("─────────────────────────────────\n");

            } else {

                consola.append("[ERROR] El parser no generó un AST válido.\n");
                this.salidaConsola = consola.toString();
                return this.salidaConsola;
            }

        } catch (Exception e) {

            return "[ERROR] Fallo crítico en la fase sintáctica: "
                    + e.getMessage();
        }

     
        // REGISTRO Y EJECUCIÓN (MODO HÍBRIDO / SCRIPT)


        consola.append("[INFO] Analizando AST...\n");

        if (this.ast == null) {
            return consola.append("[ERROR] AST inexistente, ejecución abortada.")
                    .toString();
        }

        try {

            // Registrar funciones y declaraciones globales
            this.ast.registrar(this.entornoGlobal);

            // Buscar función principal 'main'
            NodoFuncion mainFunc = this.entornoGlobal.buscarFuncion("main");

            consola.append("[INFO] Iniciando ejecución...\n");
            if (mainFunc != null) {
                consola.append("[INFO] Función 'main' encontrada. Iniciando ejecución...\n");
                mainFunc.execute(this.entornoGlobal);
                consola.append("[OK] Ejecución de 'main' completada.\n");
            } else {
                consola.append("[INFO] No se detectó 'main'. Ejecutando código en modo script...\n");
                this.ast.execute(this.entornoGlobal); 
                consola.append("[OK] Ejecución de script completada.\n");
            }

            consola.append("[OK] Ejecución completada sin errores.\n");
            consola.append("─────────────────────────────────\n");

            // Imprimir el estado final del entorno
            consola.append("Estado final del entorno:\n");
            if (this.entornoGlobal.getHistoricoSimbolos() != null && !this.entornoGlobal.getHistoricoSimbolos().isEmpty()) {
                this.entornoGlobal.getHistoricoSimbolos().forEach(sim -> {
                    consola.append("   [").append(sim.getAmbito()).append("] ")
                           .append(sim.getNombre()).append(" = ")
                           .append(sim.getValor() != null ? sim.getValor() : "nil")
                           .append(" (").append(sim.getTipoDato()).append(")\n");
                });
            } else {
                consola.append("   (No se encontraron símbolos registrados en el histórico)\n");
            }
            consola.append("─────────────────────────────────\n");

            // Recuperar salida acumulada 
            String salidaPrograma = entornoGlobal.getSalidaConsola();

            if (salidaPrograma != null && !salidaPrograma.isEmpty()) {

                consola.append("\nSALIDA:\n");
                consola.append(salidaPrograma);
                consola.append("\n");
            }

            if (ErrorManager.getInstance().hayErroresSemanticos()) {

                consola.append("\n[WARN] Ejecución finalizada con errores semánticos.\n");

            } else {

                consola.append("\n[OK] Ejecución completada sin errores.\n");
            }

        } catch (ErrorSemanticoException e) {

            ErrorManager.getInstance().agregarSemantico(
                    e.getDescripcion(),
                    e.getLine(),
                    e.getColumna()
            );

            consola.append("[ERROR Semántico] ")
                    .append(e.getDescripcion())
                    .append("\n");

        } catch (BreakException | ContinueException e) {

            ErrorManager.getInstance().agregarSemantico(
                    "Sentencia de control de bucle (break/continue) fuera de ámbito.",
                    0,
                    0
            );

            consola.append("[ERROR] Sentencia de control fuera de un ciclo activo.\n");

        } catch (Exception e) {

            ErrorManager.getInstance().agregarSemantico(
                    "Error crítico de ejecución: " + e.getMessage(),
                    0,
                    0
            );

            consola.append("[CRITICAL] Excepción no controlada: ")
                    .append(e.toString())
                    .append("\n");

            e.printStackTrace();
        }


        // RESUMEN
    

        consola.append("─────────────────────────────────\n");

        int totalErrores = ErrorManager.getInstance().totalErrores();

        if (totalErrores > 0) {

            consola.append("[RESUMEN] Proceso terminado. Se detectaron ")
                    .append(totalErrores)
                    .append(" errores en total.\n");

        } else {

            consola.append("[RESUMEN] ¡Compilación y análisis exitosos sin fallos!\n");
        }

        this.salidaConsola = consola.toString();
        return this.salidaConsola;
    }

    private void resetEstado() {

        ErrorManager.getInstance().reset();
        TablaTokens.getInstance().reset();

        this.ast = null;
        this.entornoGlobal = new Entorno();

        this.entornoGlobal.registrarFuncionNativa("fmt.Println", "void");
        this.entornoGlobal.registrarFuncionNativa("strconv.Atoi", "int");
        this.entornoGlobal.registrarFuncionNativa("strconv.ParseFloat", "float64");
        this.entornoGlobal.registrarFuncionNativa("reflect.TypeOf", "string");

        this.tokens = null;
        this.salidaConsola = "";
    }

    
    // GETTERS Y REPORTES HTML 

    
    private String aplicarEstiloHTML(String html) {
        if (html == null || html.isBlank()) return html;

        String css =
           "<style>" +
           "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa; margin: 20px; color: #333; }" +
           "table { width: 100%; border-collapse: collapse; background: #ffffff; border: 1px solid #dee2e6; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
           "th { background-color: #1e376d; color: #ffffff; padding: 12px; font-weight: 600; text-align: left; }" +
           "td { border-bottom: 1px solid #e9ecef; padding: 10px; font-size: 14px; color: #333333; }" + 
           "tr:nth-child(even) { background-color: #f1f3f5; }" +
           "tr:hover { background-color: #e2e6ea; }" +
           "</style>";

        if (html.contains("<head>")) {
            return html.replace("<head>", "<head>" + css);
        }
        if (html.contains("<html>")) {
            return html.replace("<html>", "<html><head>" + css + "</head>");
        }
        return "<html><head>" + css + "</head><body>" + html + "</body></html>";
    }

    public String getReporteErroresHTML() {
        return aplicarEstiloHTML(ErrorManager.getInstance().generarReporteHTML());
    }

    public String getReporteTokensHTML() {
        return aplicarEstiloHTML(TablaTokens.getInstance().generarReporteHTML());
    }

    public String getReporteSimbolosHTML() {
        try {
            if (entornoGlobal == null) {
                return aplicarEstiloHTML("<h2>Reporte de Símbolos</h2><p>Primero debe ejecutar el código para generar la tabla.</p>");
            }
            return aplicarEstiloHTML(
                new TablaSimbolos(entornoGlobal.getHistoricoSimbolos()).generarReporteHTML()
            );
        } catch (Exception e) {
            return aplicarEstiloHTML("<h2>Error</h2><p>Error al generar tabla de símbolos: " + e.getMessage() + "</p>");
        }
    }

    public String getReporteASTHTML() {
        try {
            if (ast == null) {
                return aplicarEstiloHTML("<h2>Reporte AST</h2><p>No hay un AST generado (compilación fallida).</p>");
            }
            ReporteAST reporte = new ReporteAST();
            reporte.setRaiz(ast);
            return aplicarEstiloHTML(reporte.generarReporteHTML());
        } catch (Exception e) {
            return aplicarEstiloHTML("<h2>Error</h2><p>Error al generar reporte AST: " + e.getMessage() + "</p>");
        }
    }

    public String getSalidaConsola() {
        return salidaConsola != null ? salidaConsola : "";
    }

    public boolean hayErrores() {
        return ErrorManager.getInstance().hayErrores();
    }
}