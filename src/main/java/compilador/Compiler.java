package compilador;

import errores.ErrorManager;
import tokens.TablaTokens;
/**
import lexer.Lexer;
*/
import tokens.Token;

import java.io.StringReader;
import java.util.List;

/**
 * Compiler — Controlador principal del intérprete GoLite.
 *
 * Orquesta las fases del compilador en este orden:
 *   1. Análisis Léxico   (Lexer generado por JFlex)
 *   2. Análisis Sintáctico (Parser generado por CUP)  ← Día 2
 *   3. Análisis Semántico / Intérprete                ← Día 3
 *
 * La GUI llama a compilar(String codigoFuente) y obtiene el resultado
 * para mostrarlo en la consola y habilitar los reportes.
 */
public class Compiler {

    // ─── Singleton ─────────────────────────────────────────────────────────────

    private static Compiler instancia;

    private Compiler() {}

    public static Compiler getInstance() {
        if (instancia == null) {
            instancia = new Compiler();
        }
        return instancia;
    }

    // ─── Estado de la última compilación ───────────────────────────────────────

    private String       salidaConsola = "";
    private List<Token>  tokens;

    // ─── Punto de entrada principal ────────────────────────────────────────────

    /**
     * Ejecuta todas las fases del compilador sobre el código fuente recibido.
     *
     * @param codigoFuente Texto del archivo .glt
     * @return Texto que debe mostrarse en la consola del IDE GoLite
     */
    public String compilar(String codigoFuente) {

        // ── Limpiar estado de compilaciones anteriores ──────────────────────
        ErrorManager.getInstance().reset();
        TablaTokens.getInstance().reset();
        salidaConsola = "";

        StringBuilder consola = new StringBuilder();
        consola.append("> Ejecutando análisis...\n");
        consola.append("[INFO] Compilación iniciada\n");
/**
        // ── FASE 1: Análisis Léxico ──────────────────────────────────────────
        try {
            Lexer lexer = new Lexer(new StringReader(codigoFuente));
            tokens = lexer.getListaTokens();

            // Avanzar el lexer para llenar la lista de tokens
            // (CUP lo hará automáticamente en Fase 2, aquí lo hacemos manual)
            java_cup.runtime.Symbol s;
            do {
                s = lexer.next_token();
                // Los tokens ya se registran dentro del método token() del lexer
            } while (s.sym != sym.EOF);

            // Pasar tokens a la tabla global
            for (Token t : lexer.getListaTokens()) {
                TablaTokens.getInstance().agregar(t);
            }

            if (ErrorManager.getInstance().hayErroresLexicos()) {
                consola.append("[WARN] Se encontraron errores léxicos.\n");
            } else {
                consola.append("[OK]   Análisis léxico completado sin errores.\n");
            }

            consola.append(String.format("[INFO] Tokens reconocidos: %d\n",
                    TablaTokens.getInstance().totalTokens()));

        } catch (Exception e) {
            ErrorManager.getInstance().agregarLexico(
                "Error inesperado en el análisis léxico: " + e.getMessage(), 0, 0
            );
            consola.append("[ERROR] Fallo en el análisis léxico: ").append(e.getMessage()).append("\n");
            
        }

        // ── FASE 2: Análisis Sintáctico (se integra en Día 2) ───────────────
        consola.append("[INFO] Análisis sintáctico pendiente (Día 2).\n");

        // ── FASE 3: Análisis Semántico / Intérprete (Día 3) ─────────────────
        consola.append("[INFO] Intérprete pendiente (Día 3).\n");

        // ── Resumen final ────────────────────────────────────────────────────
        if (ErrorManager.getInstance().hayErrores()) {
            consola.append(String.format("\n[RESUMEN] %d error(es) encontrado(s).\n",
                    ErrorManager.getInstance().totalErrores()));
        } else {
            consola.append("\n[RESUMEN] Compilación exitosa.\n");
        }
 */
// TODO Día 2: descomentar cuando exista sym y Parser

        salidaConsola = consola.toString();
        return salidaConsola;
    }

    // ─── Getters para la GUI ────────────────────────────────────────────────────

    public String getSalidaConsola() {
        return salidaConsola;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public String getReporteErrores() {
        return ErrorManager.getInstance().generarReporte();
    }

    public String getReporteErroresHTML() {
        return ErrorManager.getInstance().generarReporteHTML();
    }

    public String getReporteTokens() {
        return TablaTokens.getInstance().generarReporte();
    }

    public String getReporteTokensHTML() {
        return TablaTokens.getInstance().generarReporteHTML();
    }

    public boolean hayErrores() {
        return ErrorManager.getInstance().hayErrores();
    }
}
