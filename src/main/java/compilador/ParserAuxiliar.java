// =====================================================================
// compilador/ParserAuxiliar.java — VERSIÓN FINAL COMPLETA
// Soporta: for clásico, for range, slices con índice dinámico,
//          slices 2D, métodos de struct, funciones de usuario con return,
//          break solo-de-switch, recursión, append, len, slices.Index,
//          strings.Join, operador módulo %, operadores <= >= !=
// =====================================================================
package compilador;

import ast.*;
import entorno.Entorno;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import tokens.Token;
import tokens.TipoToken;
import static tokens.TipoToken.*;

public class ParserAuxiliar {

    private List<Token> tokens;
    private int pc = 0;
    private Entorno entorno;
    private List<Nodo> nodosGenerados;
    private String ultimoError = null;

    // Almacén de funciones de usuario definidas en el script
    // clave: "nombreFunc" o "TipoReceptor.nombreMetodo"
    private Map<String, FuncionUsuario> funcionesUsuario = new HashMap<>();

    public String getUltimoError() { return this.ultimoError; }

    // ─── Clase interna para guardar funciones de usuario ─────────────
    private static class FuncionUsuario {
        String nombre;
        String receptor;          // null si función normal; "Punto" si es método
        List<String> paramNombres;
        List<String> paramTipos;
        String tipoRetorno;       // "void" si no retorna nada
        int tokenInicio;          // índice en tokens[] donde empieza el cuerpo (tras '{')
    }

    // ─── Excepción return ─────────────────────────────────────────────
    public class ReturnException extends RuntimeException {
        public Object valor;
        public ReturnException(Object valor) { this.valor = valor; }
    }

    // ─── Excepción break (sale solo del switch o del for) ────────────
    public static class BreakException extends RuntimeException {
        public BreakException() { super(null, null, true, false); }
    }

    // ─── Constructor ─────────────────────────────────────────────────
    public ParserAuxiliar(List<Token> tokens, Entorno entorno) {
        this.tokens = tokens;
        this.entorno = entorno;
        this.nodosGenerados = new ArrayList<>();
    }

    public List<Nodo> getNodosGenerados() { return this.nodosGenerados; }

    // ═════════════════════════════════════════════════════════════════
    //  PUNTO DE ENTRADA
    // ═════════════════════════════════════════════════════════════════
    public void ejecutar() {
        // PASE 1: registrar todas las funciones/métodos sin ejecutarlos
        registrarFuncionesGlobales();

        // PASE 2: buscar y ejecutar main()
        pc = 0;
        try {
            while (pc < tokens.size()) {
                if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }
                Token t = peek();
                if (t == null) break;
                if (t.getTipo() == TipoToken.RES_FUNC) {
                    // Saltamos definiciones de función en el nivel global
                    saltarDefinicionFunc();
                } else if (t.getTipo() == TipoToken.RES_TYPE) {
                    saltarDefinicionStruct();
                } else if (t.getTipo() == TipoToken.IDENTIFICADOR
                        && pc + 1 < tokens.size()
                        && tokens.get(pc + 1).getTipo() == TipoToken.LLAVE_ABRE) {
                    // struct sin 'type' keyword (struct Nombre {)
                    saltarDefinicionStruct();
                } else {
                    procesarSentencia(t);
                }
            }
        } catch (ReturnException e) {
            // return de main, normal
        } catch (Exception e) {
            this.ultimoError = "Error en posición " + pc + ": " + e.getMessage();
            System.err.println("[ParserAuxiliar] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Pase 1: registrar funciones globales ────────────────────────
    private void registrarFuncionesGlobales() {
        int savedPc = pc;
        pc = 0;
        while (pc < tokens.size()) {
            Token t = peek();
            if (t == null) break;
            if (t.getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }
            if (t.getTipo() == TipoToken.RES_FUNC) {
                registrarFunc();
            } else if (t.getTipo() == TipoToken.RES_TYPE) {
                registrarStruct();
            } else if (t.getTipo() == TipoToken.IDENTIFICADOR
                    && pc + 1 < tokens.size()
                    && tokens.get(pc + 1).getTipo() == TipoToken.LLAVE_ABRE) {
                registrarStruct();
            } else {
                consume(); // saltar lo demás en pase 1
            }
        }
        pc = savedPc;
    }

    private void registrarStruct() {
        // type NombreStruct struct { campos }
        // o bien: NombreStruct { campos }  (sin type)
        if (peek() != null && peek().getTipo() == TipoToken.RES_TYPE) consume(); // 'type'
        String nombre = consume().getLexema(); // nombre struct
        // puede venir 'struct' keyword
        if (peek() != null && peek().getLexema().equals("struct")) consume();
        if (!esToken(TipoToken.LLAVE_ABRE)) return;
        consume(); // '{'
        Map<String, String> campos = new HashMap<>();
        while (peek() != null && peek().getTipo() != TipoToken.LLAVE_CIERRA) {
            if (peek().getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }
            if (peek().getTipo() == TipoToken.IDENTIFICADOR) {
                String nombreCampo = consume().getLexema();
                if (peek() == null) break;
                String tipoCampo = consume().getLexema();
                // tipo puede ser []int etc
                while (peek() != null && peek().getTipo() == TipoToken.COR_ABRE) {
                    tipoCampo += consume().getLexema();
                    if (peek() != null && peek().getTipo() == TipoToken.COR_CIERRA)
                        tipoCampo += consume().getLexema();
                }
                campos.put(nombreCampo, tipoCampo);
            } else {
                consume();
            }
        }
        if (peek() != null) consume(); // '}'
        entorno.registrarStruct(nombre, new ast.NodoStruct(campos));
        this.nodosGenerados.add(new ast.NodoStruct(campos));
    }

    private void registrarFunc() {
        consume(); // 'func'
        if (peek() == null) return;

        FuncionUsuario fu = new FuncionUsuario();
        fu.paramNombres = new ArrayList<>();
        fu.paramTipos   = new ArrayList<>();
        fu.receptor     = null;

        // ¿método de struct? func (p Tipo) nombre(...)
        if (esToken(TipoToken.PAREN_ABRE)) {
            consume(); // '('
            consume(); // nombre receptor, e.g. 'p'
            fu.receptor = consume().getLexema(); // tipo receptor, e.g. 'Punto'
            if (!esToken(TipoToken.PAREN_CIERRA))
                throw new RuntimeException("Se esperaba ')' en receptor");
            consume(); // ')'
        }

        fu.nombre = consume().getLexema(); // nombre función

        // Parámetros
        if (!esToken(TipoToken.PAREN_ABRE))
            throw new RuntimeException("Se esperaba '(' en función " + fu.nombre);
        consume(); // '('
        while (peek() != null && peek().getTipo() != TipoToken.PAREN_CIERRA) {
            if (peek().getTipo() == TipoToken.COMA) { consume(); continue; }
            String pNombre = consume().getLexema();
            if (peek() == null || peek().getTipo() == TipoToken.PAREN_CIERRA) {
                fu.paramNombres.add(pNombre);
                fu.paramTipos.add("auto");
                break;
            }
            String pTipo = consume().getLexema();
            // tipo puede ser []int etc
            while (peek() != null && peek().getTipo() == TipoToken.COR_ABRE) {
                pTipo += consume().getLexema();
                if (peek() != null && peek().getTipo() == TipoToken.COR_CIERRA)
                    pTipo += consume().getLexema();
            }
            fu.paramNombres.add(pNombre);
            fu.paramTipos.add(pTipo);
        }
        consume(); // ')'

        // Tipo de retorno opcional
        fu.tipoRetorno = "void";
        while (peek() != null && peek().getTipo() != TipoToken.LLAVE_ABRE) {
            fu.tipoRetorno = peek().getLexema();
            consume();
        }

        if (!esToken(TipoToken.LLAVE_ABRE))
            throw new RuntimeException("Se esperaba '{' en función " + fu.nombre);
        consume(); // '{'

        fu.tokenInicio = pc; // guardamos posición del cuerpo

        // Saltamos el cuerpo
        saltarBloqueLlaves();

        // Registrar
        String clave = fu.receptor != null
                       ? fu.receptor + "." + fu.nombre
                       : fu.nombre;
        funcionesUsuario.put(clave, fu);
        this.nodosGenerados.add(new NodoFuncion(fu.nombre, new ArrayList<>(), null,
                new NodoBloque(new ArrayList<>(), 0, 0), 0, 0));
    }

    // ─── Saltar definición de función en pase 2 ─────────────────────
    private void saltarDefinicionFunc() {
        consume(); // 'func'
        // receptor opcional
        if (esToken(TipoToken.PAREN_ABRE)) {
            consume();
            while (peek() != null && !esToken(TipoToken.PAREN_CIERRA)) consume();
            consume();
        }
        // nombre
        if (peek() != null) consume();
        // parámetros
        if (esToken(TipoToken.PAREN_ABRE)) {
            consume();
            int p = 1;
            while (p > 0 && peek() != null) {
                if (esToken(TipoToken.PAREN_ABRE)) p++;
                else if (esToken(TipoToken.PAREN_CIERRA)) p--;
                consume();
            }
        }
        // tipo retorno
        while (peek() != null && !esToken(TipoToken.LLAVE_ABRE)) consume();
        // cuerpo
        if (esToken(TipoToken.LLAVE_ABRE)) {
            consume();
            saltarBloqueLlaves();
        }
    }

    private void saltarDefinicionStruct() {
        if (peek() != null && peek().getTipo() == TipoToken.RES_TYPE) consume();
        if (peek() != null) consume(); // nombre
        if (peek() != null && peek().getLexema().equals("struct")) consume();
        if (esToken(TipoToken.LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }

    // ═════════════════════════════════════════════════════════════════
    //  PROCESAR SENTENCIA
    // ═════════════════════════════════════════════════════════════════
    private void procesarSentencia(Token t) {
        switch (t.getTipo()) {
            case RES_VAR:    parseDeclaracion(); break;
            case RES_FOR:    parseFor();         break;
            case RES_SWITCH: parseSwitch();      break;
            case RES_IF:     parseIf();          break;
            case RES_FUNC:   saltarDefinicionFunc(); break;
            case RES_RETURN: parseReturn();      break;
            case RES_TYPE:   saltarDefinicionStruct(); break;
            case RES_BREAK:
                consume(); // consumir 'break'
                if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
                throw new BreakException();

            case IDENTIFICADOR:
                // ¿for range ya consumió var? No — esto viene de asignación o llamada
                if (pc + 1 < tokens.size() && tokens.get(pc + 1).getTipo() == TipoToken.PAREN_ABRE) {
                    String fn = consume().getLexema();
                    Object res = parseLlamadaFuncion(fn);
                    this.nodosGenerados.add(new NodoLlamadaFuncion(fn, new ArrayList<>(), 0, 0));
                } else {
                    parseAsignacion();
                }
                break;

            case RES_FMT_PRINTLN:
            case RES_STRCONV_ATOI:
            case RES_STRCONV_PARSEFLOAT:
            case RES_REFLECT_TYPEOF:
            case RES_APPEND:
            case RES_LEN:
            case RES_STRINGS_JOIN: {
                String fn = consume().getLexema();
                parseLlamadaFuncion(fn);
                this.nodosGenerados.add(new NodoLlamadaFuncion(fn, new ArrayList<>(), 0, 0));
                break;
            }

            case PAREN_ABRE:
                evaluarExpresion();
                if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
                break;

            case LLAVE_ABRE: case LLAVE_CIERRA: case PUNTO_COMA:
                consume(); break;

            default:
                // token inesperado — lo saltamos para recuperación de errores
                consume();
                break;
        }
        if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
    }

    // ═════════════════════════════════════════════════════════════════
    //  FOR  (clásico i:=0; i<n; i++ | range | infinito)
    // ═════════════════════════════════════════════════════════════════
    private void parseFor() {
        consume(); // 'for'
        this.nodosGenerados.add(new NodoFor(null, null, null,
                new NodoBloque(new ArrayList<>(), 0, 0), 0, 0));

        // ¿bucle infinito? for { ... }
        if (esToken(TipoToken.LLAVE_ABRE)) {
            int seguridad = 0;
            int inicio = pc;
            while (true) {
                consume(); // '{'
                try { ejecutarBloque(); } catch (BreakException e) { return; }
                pc = inicio;
                if (seguridad++ > 1_000_000) throw new RuntimeException("Bucle infinito");
            }
        }

        // ¿for range?  for i, v := range slice { ... }
        // Lookahead: buscar la palabra 'range' antes del siguiente '{'
        if (esForRange()) {
            parseForRange();
            return;
        }

        // ¿for clásico?  for init ; cond ; post { ... }
        if (esForClasico()) {
            parseForClasico();
            return;
        }

        // for con solo condición: for x < 10 { ... }
        int inicioCondicion = pc;
        int seguridad = 0;
        while (evaluarCondicion()) {
            if (!esToken(TipoToken.LLAVE_ABRE)) break;
            consume(); // '{'
            try { ejecutarBloque(); } catch (BreakException e) { return; }
            pc = inicioCondicion;
            if (seguridad++ > 1_000_000) throw new RuntimeException("Bucle infinito");
        }
        // saltar bloque si condición false
        if (esToken(TipoToken.LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }

    private boolean esForRange() {
        int saved = pc;
        int profundidad = 0;
        while (saved < tokens.size()) {
            Token t = tokens.get(saved);
            if (t.getTipo() == TipoToken.LLAVE_ABRE && profundidad == 0) break;
            if (t.getTipo() == TipoToken.PAREN_ABRE) profundidad++;
            if (t.getTipo() == TipoToken.PAREN_CIERRA) profundidad--;
            if (t.getLexema().equals("range")) return true;
            saved++;
        }
        return false;
    }

    private boolean esForClasico() {
        // Hay un ';' antes del primer '{'
        int saved = pc;
        int profundidad = 0;
        while (saved < tokens.size()) {
            Token t = tokens.get(saved);
            if (t.getTipo() == TipoToken.LLAVE_ABRE && profundidad == 0) break;
            if (t.getTipo() == TipoToken.PAREN_ABRE) profundidad++;
            if (t.getTipo() == TipoToken.PAREN_CIERRA) profundidad--;
            if (t.getTipo() == TipoToken.PUNTO_COMA && profundidad == 0) return true;
            saved++;
        }
        return false;
    }

    // for i, v := range slice { ... }
    private void parseForRange() {
        // Leer variable(s) de índice / valor
        String varIdx = null;
        String varVal = null;

        Token t1 = consume(); // primer identificador (puede ser '_')
        varIdx = t1.getLexema();

        if (esToken(TipoToken.COMA)) {
            consume(); // ','
            varVal = consume().getLexema(); // segundo identificador
        }

        // consumir ':=' o '='
        if (esToken(TipoToken.OP_ASIGN_CORTO) || esToken(TipoToken.OP_ASIGNACION))
            consume();

        // consumir 'range'
        if (peek() != null && peek().getLexema().equals("range")) consume();

        // evaluar la expresión del slice
        Object coleccion = evaluarExpresion();

        if (!esToken(TipoToken.LLAVE_ABRE))
            throw new RuntimeException("Se esperaba '{' en for range");

        int inicioCuerpo = pc;
        List<?> lista = toList(coleccion);

        for (int i = 0; i < lista.size(); i++) {
            pc = inicioCuerpo;
            consume(); // '{'

            Entorno entornoLocal = new Entorno(entorno);
            ParserAuxiliar subParser = new ParserAuxiliar(tokens, entornoLocal);
            subParser.funcionesUsuario = this.funcionesUsuario;

            if (!"_".equals(varIdx)) entornoLocal.declarar(varIdx, "int", i);
            if (varVal != null && !"_".equals(varVal))
                entornoLocal.declarar(varVal, "auto", lista.get(i));

            try {
                subParser.ejecutarBloqueParcial();
                this.nodosGenerados.addAll(subParser.getNodosGenerados());
            } catch (BreakException e) {
                break;
            } catch (ReturnException e) {
                throw e;
            }
        }

        // Asegurar que pc esté después del bloque
        // Si el for terminó sin break, ya habremos consumido el cuerpo en la última iteración
        // Si la lista estaba vacía, saltar el cuerpo
        if (lista.isEmpty()) {
            if (esToken(TipoToken.LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
        }
    }

    // for i := 0; i < n; i++ { ... }
    private void parseForClasico() {
        // Init
        Token primera = peek();
        if (primera != null && primera.getTipo() == TipoToken.RES_VAR) {
            parseDeclaracion();
        } else if (primera != null && primera.getTipo() == TipoToken.IDENTIFICADOR) {
            // puede ser i := 0
            parseAsignacion();
        }
        // El ';' después del init puede haber sido consumido por parseDeclaracion/Asignacion
        if (esToken(TipoToken.PUNTO_COMA)) consume();

        int inicioCondicion = pc;
        int seguridad = 0;

        while (true) {
            pc = inicioCondicion;

            // Evaluar condición
            boolean condicion = evaluarCondicion();
            if (esToken(TipoToken.PUNTO_COMA)) consume(); // ';' entre condición y post

            // Guardar posición del post y del cuerpo
            int inicioPc = pc;

            // Saltar hasta el '{'
            int profundidad = 0;
            while (pc < tokens.size()) {
                Token tt = tokens.get(pc);
                if (tt.getTipo() == TipoToken.PAREN_ABRE) profundidad++;
                else if (tt.getTipo() == TipoToken.PAREN_CIERRA) profundidad--;
                else if (tt.getTipo() == TipoToken.LLAVE_ABRE && profundidad == 0) break;
                pc++;
            }
            int inicioCuerpo = pc;

            // Saltar el cuerpo (para calcular posición post-cuerpo)
            if (condicion) {
                // Ejecutar cuerpo
                pc = inicioCuerpo;
                consume(); // '{'
                try {
                    ejecutarBloque();
                } catch (BreakException e) {
                    return;
                }

                // Ejecutar el post (i++, i += 1, i = i + 1, etc.)
                int posCuerpoFin = pc;
                pc = inicioPc;
                ejecutarPost();
                pc = posCuerpoFin;

                if (seguridad++ > 1_000_000)
                    throw new RuntimeException("Bucle infinito en for clásico");
            } else {
                // condición false → saltar cuerpo y salir
                pc = inicioCuerpo;
                if (esToken(TipoToken.LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
                return;
            }
        }
    }

    // Ejecuta la sentencia post de un for (i++, i--, i+=1, i=i+1, etc.)
    private void ejecutarPost() {
        Token t = peek();
        if (t == null || t.getTipo() == TipoToken.LLAVE_ABRE) return;

        // i++ o i--
        if (pc + 1 < tokens.size()) {
            Token next = tokens.get(pc + 1);
            if (next.getTipo() == TipoToken.OP_INCREMENTO) {
                String nombre = consume().getLexema();
                consume(); // '++'
                Object val = entorno.obtener(nombre);
                if (val instanceof Number)
                    entorno.asignar(nombre, ((Number) val).intValue() + 1);
                if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
                return;
            }
            if (next.getTipo() == TipoToken.OP_DECREMENTO) {
                String nombre = consume().getLexema();
                consume(); // '--'
                Object val = entorno.obtener(nombre);
                if (val instanceof Number)
                    entorno.asignar(nombre, ((Number) val).intValue() - 1);
                if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
                return;
            }
        }

        // i += expr / i -= expr / i = expr
        if (t.getTipo() == TipoToken.IDENTIFICADOR) {
            String nombre = consume().getLexema();
            Token op = peek();
            if (op != null && (op.getTipo() == TipoToken.OP_ASIGNACION
                    || op.getTipo() == TipoToken.OP_ASIGN_CORTO
                    || op.getTipo() == TipoToken.OP_SUMA
                    || op.getTipo() == TipoToken.OP_RESTA)) {
                consume(); // op
                Object expr = evaluarExpresion();
                if (op.getTipo() == TipoToken.OP_SUMA) {
                    Object actual = entorno.obtener(nombre);
                    expr = numSum(actual, expr);
                } else if (op.getTipo() == TipoToken.OP_RESTA) {
                    Object actual = entorno.obtener(nombre);
                    expr = numSub(actual, expr);
                }
                entorno.asignar(nombre, expr);
            }
        }
        if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
    }

    // ═════════════════════════════════════════════════════════════════
    //  SWITCH
    // ═════════════════════════════════════════════════════════════════
    private void parseSwitch() {
        consume(); // 'switch'
        Object valorSwitch = evaluarExpresion();

        if (!esToken(TipoToken.LLAVE_ABRE))
            throw new RuntimeException("Se esperaba '{' en switch");
        consume();

        this.nodosGenerados.add(new NodoSwitch(null, null, new ArrayList<>(), 0, 0));

        boolean casoEjecutado = false;

        while (peek() != null && !esToken(TipoToken.LLAVE_CIERRA)) {
            Token t = peek();
            if (t.getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }

            if (t.getTipo() == TipoToken.RES_CASE) {
                consume();
                Object valorCase = evaluarExpresion();
                if (!esToken(TipoToken.DOS_PUNTOS))
                    throw new RuntimeException("Se esperaba ':' en case");
                consume();

                if (!casoEjecutado && objetosIguales(valorSwitch, valorCase)) {
                    try { ejecutarBloqueSwitch(); } catch (BreakException e) { /* sale del switch */ }
                    casoEjecutado = true;
                } else {
                    saltarBloqueSwitch();
                }

            } else if (t.getTipo() == TipoToken.RES_DEFAULT) {
                consume();
                if (esToken(TipoToken.DOS_PUNTOS)) consume();

                if (!casoEjecutado) {
                    try { ejecutarBloqueSwitch(); } catch (BreakException e) { /* sale del switch */ }
                    casoEjecutado = true;
                } else {
                    saltarBloqueSwitch();
                }
            } else {
                consume(); // recuperación
            }
        }

        if (esToken(TipoToken.LLAVE_CIERRA)) consume();
    }

    /** Ejecuta sentencias de un case hasta el siguiente case/default/} */
    private void ejecutarBloqueSwitch() {
        while (peek() != null) {
            Token t = peek();
            if (t.getTipo() == TipoToken.RES_CASE
                    || t.getTipo() == TipoToken.RES_DEFAULT
                    || t.getTipo() == TipoToken.LLAVE_CIERRA) return;
            procesarSentencia(t); // BreakException sube sin capturar
        }
    }

    /** Salta sentencias de un case sin ejecutar */
    private void saltarBloqueSwitch() {
        while (peek() != null) {
            Token t = peek();
            if (t.getTipo() == TipoToken.RES_CASE
                    || t.getTipo() == TipoToken.RES_DEFAULT
                    || t.getTipo() == TipoToken.LLAVE_CIERRA) return;
            consume();
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  IF / ELSE
    // ═════════════════════════════════════════════════════════════════
    private void parseIf() {
        if (esToken(TipoToken.RES_IF)) consume();
        this.nodosGenerados.add(new NodoIf(null,
                new NodoBloque(new ArrayList<>(), 0, 0), null, null, 0, 0));

        boolean condicion = evaluarCondicion();

        if (!esToken(TipoToken.LLAVE_ABRE))
            throw new RuntimeException("Se esperaba '{' en if");
        consume();

        if (condicion) ejecutarBloque();
        else           saltarBloqueLlaves();

        if (esToken(TipoToken.RES_ELSE)) {
            consume();
            if (esToken(TipoToken.RES_IF)) {
                parseIf();
            } else {
                if (esToken(TipoToken.LLAVE_ABRE)) consume();
                if (!condicion) ejecutarBloque();
                else            saltarBloqueLlaves();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  BLOQUES
    // ═════════════════════════════════════════════════════════════════
    private void ejecutarBloque() {
        int llavesAbiertas = 1;
        while (llavesAbiertas > 0 && pc < tokens.size()) {
            Token t = tokens.get(pc);
            if (t.getTipo() == TipoToken.LLAVE_ABRE) { llavesAbiertas++; pc++; continue; }
            if (t.getTipo() == TipoToken.LLAVE_CIERRA) {
                llavesAbiertas--; pc++;
                if (llavesAbiertas == 0) break;
                continue;
            }
            if (llavesAbiertas > 0) {
                int prev = pc;
                procesarSentencia(t);
                if (pc == prev) pc++;
            }
        }
    }

    /** Para for range: ejecuta el bloque en el entorno local del subParser */
    private void ejecutarBloqueParcial() {
        int llavesAbiertas = 1;
        while (llavesAbiertas > 0 && pc < tokens.size()) {
            Token t = tokens.get(pc);
            if (t.getTipo() == TipoToken.LLAVE_ABRE) { llavesAbiertas++; pc++; continue; }
            if (t.getTipo() == TipoToken.LLAVE_CIERRA) {
                llavesAbiertas--; pc++;
                if (llavesAbiertas == 0) break;
                continue;
            }
            if (llavesAbiertas > 0) {
                int prev = pc;
                procesarSentencia(t);
                if (pc == prev) pc++;
            }
        }
    }

    private void saltarBloqueLlaves() {
        int llavesAbiertas = 1;
        while (llavesAbiertas > 0 && pc < tokens.size()) {
            Token t = tokens.get(pc);
            if (t.getTipo() == TipoToken.LLAVE_ABRE)   llavesAbiertas++;
            else if (t.getTipo() == TipoToken.LLAVE_CIERRA) llavesAbiertas--;
            pc++;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  RETURN
    // ═════════════════════════════════════════════════════════════════
    private void parseReturn() {
        consume(); // 'return'
        Object valor = null;
        if (peek() != null
                && peek().getTipo() != TipoToken.PUNTO_COMA
                && peek().getTipo() != TipoToken.LLAVE_CIERRA) {
            valor = evaluarExpresion();
        }
        this.nodosGenerados.add(new NodoReturn(null, 0, 0));
        throw new ReturnException(valor);
    }

    // ═════════════════════════════════════════════════════════════════
    //  DECLARACIÓN  var x tipo = val   |   x := val
    // ═════════════════════════════════════════════════════════════════
    private void parseDeclaracion() {
        consume(); // 'var'
        String nombre = consume().getLexema();
        String tipo   = consume().getLexema();

        while (peek() != null && peek().getTipo() == TipoToken.COR_ABRE) {
            tipo += consume().getLexema();
            if (esToken(TipoToken.COR_CIERRA)) tipo += consume().getLexema();
        }

        if (esToken(TipoToken.OP_ASIGNACION)) {
            consume();
            if (peek() != null && (peek().getTipo() == TipoToken.LLAVE_ABRE
                    || peek().getLexema().equals("{"))) {
                entorno.declarar(nombre, tipo, parseSliceLiteral());
            } else {
                entorno.declarar(nombre, tipo, evaluarExpresion());
            }
        } else {
            Object defVal = defaultValue(tipo);
            entorno.declarar(nombre, tipo, defVal);
        }

        this.nodosGenerados.add(new NodoDeclaracionVar(
                nombre, new NodoTipo(tipo, 0, 0), null, true, 0, 0));
    }

    private Object defaultValue(String tipo) {
        switch (tipo) {
            case "int":     return 0;
            case "float64": return 0.0;
            case "bool":    return false;
            case "string":  return "";
            default:        return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  ASIGNACIÓN  x = val | x := val | x.campo = val | x[i] = val
    //              x += val | x -= val | x++ | x--
    // ═════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void parseAsignacion() {
        String nombre = consume().getLexema(); // identificador base

        // ── x++ / x-- ────────────────────────────────────────────────
        if (esToken(TipoToken.OP_INCREMENTO)) {
            consume();
            Object val = entorno.obtener(nombre);
            entorno.asignar(nombre, toInt(val) + 1);
            return;
        }
        if (esToken(TipoToken.OP_DECREMENTO)) {
            consume();
            Object val = entorno.obtener(nombre);
            entorno.asignar(nombre, toInt(val) - 1);
            return;
        }

        // ── x[i] = val  o  x[i][j] = val ────────────────────────────
        if (esToken(TipoToken.COR_ABRE)) {
            List<Object> indices = new ArrayList<>();
            while (esToken(TipoToken.COR_ABRE)) {
                consume(); // '['
                indices.add(evaluarExpresion());
                if (!esToken(TipoToken.COR_CIERRA))
                    throw new RuntimeException("Se esperaba ']'");
                consume(); // ']'
            }

            // ¿operador de asignación?
            Token op = peek();
            if (op != null && (op.getTipo() == TipoToken.OP_ASIGNACION
                    || op.getTipo() == TipoToken.OP_ASIGN_CORTO)) {
                consume();
                Object nuevoVal = evaluarExpresion();
                Object base = entorno.obtener(nombre);
                if (indices.size() == 1) {
                    int idx = toInt(indices.get(0));
                    ((List<Object>) base).set(idx, nuevoVal);
                } else if (indices.size() == 2) {
                    int i = toInt(indices.get(0));
                    int j = toInt(indices.get(1));
                    List<Object> fila = (List<Object>) ((List<Object>) base).get(i);
                    fila.set(j, nuevoVal);
                }
            }
            // += -=
            if (op != null && op.getTipo() == TipoToken.OP_SUMA) {
                consume();
                Object delta = evaluarExpresion();
                Object base = entorno.obtener(nombre);
                int idx = toInt(indices.get(0));
                Object actual = ((List<Object>) base).get(idx);
                ((List<Object>) base).set(idx, numSum(actual, delta));
            }
            if (op != null && op.getTipo() == TipoToken.OP_RESTA) {
                consume();
                Object delta = evaluarExpresion();
                Object base = entorno.obtener(nombre);
                int idx = toInt(indices.get(0));
                Object actual = ((List<Object>) base).get(idx);
                ((List<Object>) base).set(idx, numSub(actual, delta));
            }
            if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
            return;
        }

        // ── x.campo.subcampo = val ────────────────────────────────────
        List<String> ruta = new ArrayList<>();
        while (esToken(TipoToken.PUNTO)) {
            consume(); // '.'
            if (peek() != null && peek().getTipo() == TipoToken.IDENTIFICADOR)
                ruta.add(consume().getLexema());
        }

        Token op = peek();

        // No hay operador → podría ser una llamada como método suelto; salir
        if (op == null || (op.getTipo() != TipoToken.OP_ASIGNACION
                && op.getTipo() != TipoToken.OP_ASIGN_CORTO
                && op.getTipo() != TipoToken.OP_SUMA
                && op.getTipo() != TipoToken.OP_RESTA)) {
            return;
        }
        consume(); // consumir operador

        Object valor = evaluarExpresion();

        if (ruta.isEmpty()) {
            if (op.getTipo() == TipoToken.OP_ASIGN_CORTO) {
                if (entorno.buscar(nombre) != null) {
                    entorno.asignar(nombre, valor);
                } else {
                    entorno.declarar(nombre, "auto", valor);
                }
                this.nodosGenerados.add(
                        new NodoDeclaracionVar(nombre, null, null, false, 0, 0));
            } else if (op.getTipo() == TipoToken.OP_SUMA) {
                Object actual = entorno.obtener(nombre);
                entorno.asignar(nombre, numSum(actual, valor));
            } else if (op.getTipo() == TipoToken.OP_RESTA) {
                Object actual = entorno.obtener(nombre);
                entorno.asignar(nombre, numSub(actual, valor));
            } else {
                entorno.asignar(nombre, valor);
            }
        } else {
            // Asignación a campo de struct
            Object objetoBase = entorno.obtener(nombre);
            if (objetoBase == null || objetoBase == Entorno.NO_ENCONTRADO) {
                objetoBase = new HashMap<String, Object>();
                entorno.declarar(nombre, "struct", objetoBase);
            }
            if (objetoBase instanceof Map) {
                String campoFinal = ruta.get(ruta.size() - 1);
                ((Map<String, Object>) objetoBase).put(campoFinal, valor);
            }
        }

        if (peek() != null && peek().getTipo() == TipoToken.PUNTO_COMA) consume();
    }

    // ═════════════════════════════════════════════════════════════════
    //  SLICE LITERAL  {1, 2, 3} o {{1,2},{3,4}}
    //  También []int{} []string{"a","b"}
    // ═════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private Object parseSliceLiteral() {
        consume(); // '{'
        List<Object> lista = new ArrayList<>();

        while (peek() != null && !peek().getLexema().equals("}")) {
            if (peek().getTipo() == TipoToken.COMA) { consume(); continue; }
            if (peek().getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }

            if (peek().getTipo() == TipoToken.LLAVE_ABRE) {
                lista.add(parseSliceLiteral()); // sublista 2D
            } else {
                lista.add(evaluarExpresion());
            }
        }
        if (peek() != null) consume(); // '}'
        return lista;
    }

    // ═════════════════════════════════════════════════════════════════
    //  STRUCT
    // ═════════════════════════════════════════════════════════════════
    private void parseStructDef() {
        saltarDefinicionStruct();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInstanciacionStruct(String nombreStruct) {
        consume(); // '{'
        Map<String, Object> instancia = new HashMap<>();
        this.nodosGenerados.add(new NodoInstanciaStruct(nombreStruct, new ArrayList<>(), 0, 0));

        while (peek() != null && !esToken(TipoToken.LLAVE_CIERRA)) {
            if (peek().getTipo() == TipoToken.PUNTO_COMA) { consume(); continue; }
            if (peek().getTipo() != TipoToken.IDENTIFICADOR) { consume(); continue; }

            String campo = consume().getLexema();
            if (!esToken(TipoToken.DOS_PUNTOS))
                throw new RuntimeException("Se esperaba ':' en struct " + nombreStruct);
            consume();

            Object val = evaluarExpresion();

            // Si el valor es un Map (struct anidado), se almacena directamente
            instancia.put(campo, val);

            if (esToken(TipoToken.COMA)) consume();
        }
        if (esToken(TipoToken.LLAVE_CIERRA)) consume();
        return instancia;
    }

    // ═════════════════════════════════════════════════════════════════
    //  LLAMADA A FUNCIÓN
    // ═════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private Object parseLlamadaFuncion(String nombre) {
        if (!esToken(TipoToken.PAREN_ABRE))
            throw new RuntimeException("Se esperaba '(' en llamada a " + nombre);
        consume(); // '('

        List<Object> argumentos = evaluarArgumentos();

        if (!esToken(TipoToken.PAREN_CIERRA))
            throw new RuntimeException("Se esperaba ')' en llamada a " + nombre);
        consume(); // ')'

        // ── Nativas del entorno ───────────────────────────────────────
        if (entorno.esNativa(nombre)) {
            // Manejo especial de append / len con tipo correcto
            if (nombre.equals("append")) {
                return ejecutarAppend(argumentos);
            }
            if (nombre.equals("len")) {
                return ejecutarLen(argumentos);
            }
            return entorno.ejecutarNativa(nombre, argumentos, this.entorno);
        }

        // ── Funciones de usuario registradas ─────────────────────────
        FuncionUsuario fu = funcionesUsuario.get(nombre);
        if (fu == null)
            throw new RuntimeException("Función '" + nombre + "' no declarada");

        return ejecutarFuncionUsuario(fu, argumentos, null);
    }

    private Object ejecutarAppend(List<Object> args) {
        if (args.size() < 2) return args.isEmpty() ? new ArrayList<>() : args.get(0);
        Object sliceObj = args.get(0);
        List<Object> lista = sliceObj instanceof List
                ? new ArrayList<>((List<Object>) sliceObj)
                : new ArrayList<>();
        for (int i = 1; i < args.size(); i++) lista.add(args.get(i));
        return lista;
    }

    private Object ejecutarLen(List<Object> args) {
        if (args.isEmpty()) return 0;
        Object o = args.get(0);
        if (o instanceof List)   return ((List<?>) o).size();
        if (o instanceof String) return ((String) o).length();
        return 0;
    }

    // ═════════════════════════════════════════════════════════════════
    //  EJECUTAR FUNCIÓN DE USUARIO (incluye métodos de struct)
    // ═════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private Object ejecutarFuncionUsuario(FuncionUsuario fu, List<Object> args, Object receptor) {
        int savedPc  = pc;
        Entorno savedEntorno = entorno;

        Entorno nuevoEntorno = new Entorno(entorno, fu.nombre);

        // Bind receptor si es método
        if (fu.receptor != null && receptor != null) {
            // 'receptor' es la variable de tipo struct (Map)
            // el nombre que usa dentro de la función es el primer param implícito
            // lo buscamos en los tokens: ya tenemos fu.receptor como tipo
            // el nombre real del receptor se guardó en fu.paramNombres[0]... no exactamente
            // Convención: guardamos receptor como "self"
            nuevoEntorno.declarar("self", fu.receptor, receptor);
            // También declaramos con el nombre del receptor si lo guardamos
            if (!fu.paramNombres.isEmpty()
                    && fu.paramNombres.get(0).equals(fu.receptor)) {
                // no hacer nada extra
            }
        }

        // Bind parámetros
        for (int i = 0; i < fu.paramNombres.size(); i++) {
            Object val = i < args.size() ? args.get(i) : null;
            nuevoEntorno.declarar(fu.paramNombres.get(i), fu.paramTipos.get(i), val);
        }

        entorno = nuevoEntorno;
        pc      = fu.tokenInicio;

        Object retorno = null;
        try {
            ejecutarBloque();
        } catch (ReturnException e) {
            retorno = e.valor;
        } finally {
            entorno = savedEntorno;
            pc      = savedPc;
        }
        return retorno;
    }

    public List<Object> evaluarArgumentos() {
        List<Object> args = new ArrayList<>();
        if (esToken(TipoToken.PAREN_CIERRA)) return args;
        do {
            args.add(evaluarExpresion());
            if (esToken(TipoToken.COMA)) consume();
            else break;
        } while (!esToken(TipoToken.PAREN_CIERRA));
        return args;
    }

    // ═════════════════════════════════════════════════════════════════
    //  EVALUACIÓN DE EXPRESIONES
    // ═════════════════════════════════════════════════════════════════
    public Object evaluarExpresion()  { return evaluarOr(); }
    private boolean evaluarCondicion() {
        Object r = evaluarExpresion();
        return esVerdadero(r);
    }

    private Object evaluarOr() {
        Object izq = evaluarAnd();
        while (esToken(TipoToken.OP_OR)) {
            consume(); Object der = evaluarAnd();
            izq = esVerdadero(izq) || esVerdadero(der);
        }
        return izq;
    }

    private Object evaluarAnd() {
        Object izq = evaluarIgualdad();
        while (esToken(TipoToken.OP_AND)) {
            consume(); Object der = evaluarIgualdad();
            izq = esVerdadero(izq) && esVerdadero(der);
        }
        return izq;
    }

    private Object evaluarIgualdad() {
        Object izq = evaluarComparacion();
        while (esToken(TipoToken.OP_IGUAL) || esToken(TipoToken.OP_DIFERENTE)) {
            Token op = consume(); Object der = evaluarComparacion();
            boolean eq = objetosIguales(izq, der);
            izq = (op.getTipo() == TipoToken.OP_IGUAL) ? eq : !eq;
        }
        return izq;
    }

    private Object evaluarComparacion() {
        Object izq = evaluarTermino();
        while (esToken(TipoToken.OP_MENOR)   || esToken(TipoToken.OP_MAYOR)
            || esToken(TipoToken.OP_MENOR_IGUAL) || esToken(TipoToken.OP_MAYOR_IGUAL)) {
            Token op = consume(); Object der = evaluarTermino();
            double v1 = toDouble(izq), v2 = toDouble(der);
            switch (op.getTipo()) {
                case OP_MENOR:       izq = v1 <  v2; break;
                case OP_MAYOR:       izq = v1 >  v2; break;
                case OP_MENOR_IGUAL: izq = v1 <= v2; break;
                case OP_MAYOR_IGUAL: izq = v1 >= v2; break;
                default: break;
            }
        }
        return izq;
    }

    private Object evaluarTermino() {
        Object izq = evaluarModulo();
        while (esToken(TipoToken.OP_SUMA) || esToken(TipoToken.OP_RESTA)) {
            Token op = consume(); Object der = evaluarModulo();
            if (op.getTipo() == TipoToken.OP_SUMA
                    && (izq instanceof String || der instanceof String)) {
                izq = str(izq) + str(der);
            } else {
                izq = op.getTipo() == TipoToken.OP_SUMA
                        ? numSum(izq, der) : numSub(izq, der);
            }
        }
        return izq;
    }

    private Object evaluarModulo() {
        Object izq = evaluarFactor();
        while (esToken(TipoToken.OP_MULT) || esToken(TipoToken.OP_DIV)
            || esToken(TipoToken.OP_MOD)) {
            Token op = consume(); Object der = evaluarFactor();
            double v1 = toDouble(izq), v2 = toDouble(der);
            switch (op.getTipo()) {
                case OP_MULT: izq = preserveInt(izq, der, v1 * v2); break;
                case OP_DIV:
                    if (v2 == 0) throw new RuntimeException("División por cero");
                    izq = preserveInt(izq, der, v1 / v2); break;
                case OP_MOD:
                    izq = (int) v1 % (int) v2; break;
                default: break;
            }
        }
        return izq;
    }

    private Object evaluarFactor() { return evaluarPrimario(); }

    @SuppressWarnings("unchecked")
    private Object evaluarPrimario() {
        Token t = peek();
        if (t == null) throw new RuntimeException("Fin inesperado de expresión");

        // Unario
        if (t.getTipo() == TipoToken.OP_RESTA) {
            consume(); Object v = evaluarPrimario();
            return v instanceof Integer ? -(Integer) v : -toDouble(v);
        }
        if (t.getTipo() == TipoToken.OP_NOT) {
            consume(); return !esVerdadero(evaluarPrimario());
        }

        t = consume();

        switch (t.getTipo()) {
            case LIT_ENTERO:   return Integer.parseInt(t.getLexema());
            case LIT_STRING:   return t.getLexema().replace("\"", "");
            case LIT_BOOLEANO: return Boolean.parseBoolean(t.getLexema());
            case LIT_NIL:      return null;
            case LIT_FLOTANTE:    return Double.parseDouble(t.getLexema());

            // Tipo de slice: []int{...} []string{...} []float64{...} [][]int{...}
            case COR_ABRE: {
                // consumir tipo del slice hasta '{'
                while (peek() != null && peek().getTipo() != TipoToken.LLAVE_ABRE) consume();
                return parseSliceLiteral();
            }

            case IDENTIFICADOR: {
                String nombreId = t.getLexema();

                // _ (blank identifier)
                if ("_".equals(nombreId)) return null;

                // 'range' keyword dentro de for range — no debería llegar aquí
                if ("range".equals(nombreId)) return null;

                // ¿Instanciación de struct?  Nombre{campo: val, ...}
                boolean yaExiste = entorno.buscar(nombreId) != null;
                if (!yaExiste && esToken(TipoToken.LLAVE_ABRE)) {
                    return parseInstanciacionStruct(nombreId);
                }

                // ¿Llamada a función de usuario o nativa?
                if (esToken(TipoToken.PAREN_ABRE)) {
                    return parseLlamadaFuncion(nombreId);
                }

                // Variable o acceso a slice/struct
                Object obj = entorno.obtener(nombreId);
                if (obj == Entorno.NO_ENCONTRADO) {
                    // Recuperación de errores: devolver null
                    return null;
                }

                // Acceso a slice:  nombre[i] o nombre[i][j]
                while (esToken(TipoToken.COR_ABRE)) {
                    consume(); // '['
                    Object idxObj = evaluarExpresion();
                    int idx = toInt(idxObj);
                    if (!esToken(TipoToken.COR_CIERRA))
                        throw new RuntimeException("Se esperaba ']'");
                    consume(); // ']'

                    if (obj instanceof List) {
                        List<Object> lista = (List<Object>) obj;
                        if (idx < 0 || idx >= lista.size())
                            throw new RuntimeException("Índice " + idx + " fuera de rango");
                        obj = lista.get(idx);
                    }
                }

                // Acceso a campo / llamada a método de struct
                while (esToken(TipoToken.PUNTO)) {
                    consume(); // '.'
                    String campo = consume().getLexema();

                    // ¿es llamada a método?
                    if (esToken(TipoToken.PAREN_ABRE)) {
                        // Buscar método por tipo del receptor
                        String tipoReceptor = obtenerTipoStruct(obj);
                        String clave = tipoReceptor + "." + campo;
                        FuncionUsuario metodo = funcionesUsuario.get(clave);
                        if (metodo != null) {
                            consume(); // '('
                            List<Object> argsMet = evaluarArgumentos();
                            consume(); // ')'
                            obj = ejecutarFuncionUsuario(metodo, argsMet, obj);
                        } else {
                            // Recuperación
                            consume(); evaluarArgumentos(); consume();
                            obj = null;
                        }
                        continue;
                    }

                    // Acceso a campo
                    if (obj instanceof Map) {
                        obj = ((Map<String, Object>) obj).get(campo);
                    } else {
                        obj = null;
                    }
                }

                return obj;
            }

            case RES_FMT_PRINTLN:
            case RES_STRCONV_ATOI:
            case RES_STRCONV_PARSEFLOAT:
            case RES_REFLECT_TYPEOF:
            case RES_APPEND:
            case RES_LEN:
            case RES_STRINGS_JOIN:
                return parseLlamadaFuncion(t.getLexema());

            case PAREN_ABRE: {
                Object val = evaluarExpresion();
                if (!esToken(TipoToken.PAREN_CIERRA))
                    throw new RuntimeException("Se esperaba ')'");
                consume();
                return val;
            }

            default:
                // Recuperación silenciosa
                return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═════════════════════════════════════════════════════════════════

  @SuppressWarnings("unchecked")
private String obtenerTipoStruct(Object obj) {
    if (!(obj instanceof Map)) return "";
    Map<String, Object> m = (Map<String, Object>) obj;

    for (String clave : funcionesUsuario.keySet()) {
        if (!clave.contains(".")) continue;
        
        String tipo = clave.substring(0, clave.indexOf('.'));
        ast.NodoStruct ns = entorno.buscarStruct(tipo);
        
        if (ns != null) {
            // 1. Creamos un Set para guardar los nombres de los campos del struct
            java.util.Set<String> clavesDelStruct = new java.util.HashSet<>();

            // 2. Extraemos los nombres desde la lista de campos
            if (ns.getCampos() != null) {
                for (ast.NodoParametro p : ns.getCampos()) {
                    // ASUNCIÓN: NodoParametro tiene un método getNombre(). 
                    // Cambia getNombre() por el nombre real de tu método en NodoParametro
                    clavesDelStruct.add(p.getNombre()); 
                }
            }

            // 3. Extraemos los nombres si existen en el mapa de atributos
            if (ns.getAtributosMap() != null) {
                clavesDelStruct.addAll(ns.getAtributosMap().keySet());
            }

            // 4. Ahora sí, comparamos
            if (!clavesDelStruct.isEmpty() && m.keySet().containsAll(clavesDelStruct)) {
                return tipo;
            }
        }
    }
    return "";
}

    private boolean objetosIguales(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number)
            return toDouble(a) == toDouble(b);
        return a.equals(b);
    }

    private boolean esVerdadero(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Integer) return ((Integer) v) != 0;
        if (v instanceof Double)  return ((Double) v)  != 0.0;
        if (v instanceof String)  return !((String) v).isEmpty();
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Object> toList(Object o) {
        if (o instanceof List) return (List<Object>) o;
        return new ArrayList<>();
    }

    private int toInt(Object o) {
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Double)  return ((Double) o).intValue();
        if (o instanceof Number)  return ((Number) o).intValue();
        return 0;
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }

    private String str(Object o) {
        return o == null ? "nil" : o.toString();
    }

    private Object numSum(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a + (Integer) b;
        return toDouble(a) + toDouble(b);
    }

    private Object numSub(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a - (Integer) b;
        return toDouble(a) - toDouble(b);
    }

    private Object preserveInt(Object a, Object b, double result) {
        if (a instanceof Integer && b instanceof Integer && result == (int) result)
            return (int) result;
        return result;
    }

    // ─── Navegación de tokens ─────────────────────────────────────────
    private boolean esToken(TipoToken tipo) {
        return pc < tokens.size() && tokens.get(pc).getTipo() == tipo;
    }

    private Token consume() {
        if (pc >= tokens.size())
            throw new RuntimeException("Fin inesperado del archivo en posición " + pc);
        return tokens.get(pc++);
    }

    private Token peek() {
        return pc < tokens.size() ? tokens.get(pc) : null;
    }
}
