
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
    private int profundidadAST = 0;
private StringBuilder textoAST = new StringBuilder();
public String getTextoAST() { return textoAST.toString(); }
  
    private Map<String, FuncionUsuario> funcionesUsuario = new HashMap<>();

    public String getUltimoError() { return this.ultimoError; }


    private static class FuncionUsuario {
        String nombre;
        String tipoReceptor    = null;  
        String nombreReceptor  = null;   
        List<String> paramNombres = new ArrayList<>();
        List<String> paramTipos   = new ArrayList<>();
        String tipoRetorno    = "void";
        int tokenInicio;                
    }
private void agregarNodoAST(String etiqueta) {
    for (int i = 0; i < profundidadAST; i++) textoAST.append("  ");
    textoAST.append(etiqueta).append("\n");
}

    public class ReturnException extends RuntimeException {
        public Object valor;
        public ReturnException(Object v) { this.valor = v; }
    }

  
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


    public void ejecutar() {
        // PASE 1: registrar structs y funciones
        pc = 0;
        registrarGlobales();

        // PASE 2: ejecutar main()
        pc = 0;
        try {
            while (pc < tokens.size()) {
                Token t = peek();
                if (t == null) break;
                if (t.getTipo() == PUNTO_COMA) { consume(); continue; }

                if (t.getTipo() == RES_FUNC) {
                    // Solo ejecutamos main, el resto se salta
                    int saved = pc;
                    consume(); 
                    
                    if (esToken(PAREN_ABRE)) {
                        consume(); saltarHastaToken(PAREN_CIERRA); consume();
                    }
                    String nombre = peek() != null ? peek().getLexema() : "";
                    if ("main".equals(nombre)) {
                        pc = saved; 
                        procesarSentencia(peek());
                    } else {
                        pc = saved;
                        saltarDefFunc();
                    }
                } else if (t.getTipo() == RES_TYPE) {
                    saltarDefStruct();
                } else if (t.getTipo() == IDENTIFICADOR
                        && esStructSinType()) {
                    saltarDefStructSinType();
                } else {
                    procesarSentencia(t);
                }
            }
        } catch (ReturnException e) {
            // return desde main — normal
        } catch (Exception e) {
            this.ultimoError = "Error en pos " + pc + ": " + e.getMessage();
            System.err.println("[ParserAuxiliar] " + this.ultimoError);
            e.printStackTrace();
        }
    }


    private boolean esStructSinType() {
        if (pc + 1 >= tokens.size()) return false;
        return tokens.get(pc + 1).getTipo() == LLAVE_ABRE;
    }

    private void registrarGlobales() {
        while (pc < tokens.size()) {
            Token t = peek();
            if (t == null) break;
            if (t.getTipo() == PUNTO_COMA) { consume(); continue; }

            if (t.getTipo() == RES_TYPE)  { registrarStruct(true);  continue; }
            if (t.getTipo() == RES_FUNC)  { registrarFunc();         continue; }
            // struct sin keyword 'type': Nombre { campos }
            if (t.getTipo() == IDENTIFICADOR && esStructSinType()) {
                registrarStruct(false); continue;
            }
            consume();
        }
    }


    private void registrarStruct(boolean tieneType) {
        if (tieneType) consume(); 
        if (peek() == null) return;
        String nombre = consume().getLexema();
        // keyword 'struct' opcional
        if (peek() != null && "struct".equals(peek().getLexema())) consume();
        if (!esToken(LLAVE_ABRE)) return;
        consume(); // '{'
        Map<String, String> campos = new HashMap<>();
        while (peek() != null && !esToken(LLAVE_CIERRA)) {
            if (esToken(PUNTO_COMA)) { consume(); continue; }
            if (esToken(IDENTIFICADOR)) {
                String nc = consume().getLexema();
                if (peek() == null) break;
                String tc = consume().getLexema();
                // tipo puede ser []int, etc.
                while (esToken(COR_ABRE)) {
                    tc += consume().getLexema();
                    if (esToken(COR_CIERRA)) tc += consume().getLexema();
                }
                campos.put(nc, tc);
            } else { consume(); }
        }
        if (esToken(LLAVE_CIERRA)) consume();
        ast.NodoStruct ns = new ast.NodoStruct(campos);
        entorno.registrarStruct(nombre, ns);
        this.nodosGenerados.add(ns);
    }

    // ── Registrar función o método ───────────────────────────────────
    private void registrarFunc() {
        consume(); // 'func'
        if (peek() == null) return;

        FuncionUsuario fu = new FuncionUsuario();

        // ¿Método? func (nombreRec TipoRec) nombre(...)
        if (esToken(PAREN_ABRE)) {
            consume();
            fu.nombreReceptor = peek() != null ? consume().getLexema() : null;
            fu.tipoReceptor   = peek() != null ? consume().getLexema() : null;
            if (esToken(PAREN_CIERRA)) consume();
        }

        fu.nombre = peek() != null ? consume().getLexema() : "anonima";

        // Parámetros
        if (!esToken(PAREN_ABRE)) { saltarHastaToken(LLAVE_CIERRA); consume(); return; }
        consume(); // '('
        while (peek() != null && !esToken(PAREN_CIERRA)) {
            if (esToken(COMA)) { consume(); continue; }
            String pn = consume().getLexema();
            if (esToken(PAREN_CIERRA)) { fu.paramNombres.add(pn); fu.paramTipos.add("auto"); break; }
            String pt = consume().getLexema();
            // tipo puede ser []int, etc.
            while (esToken(COR_ABRE)) {
                pt += consume().getLexema();
                if (esToken(COR_CIERRA)) pt += consume().getLexema();
            }
            fu.paramNombres.add(pn);
            fu.paramTipos.add(pt);
        }
        if (esToken(PAREN_CIERRA)) consume();

        // Tipo de retorno
        StringBuilder tr = new StringBuilder();
        while (peek() != null && !esToken(LLAVE_ABRE)) {
            tr.append(peek().getLexema()); consume();
        }
        if (tr.length() > 0) fu.tipoRetorno = tr.toString().trim();

        if (!esToken(LLAVE_ABRE)) return;
        consume(); // '{'
        fu.tokenInicio = pc; // inicio del cuerpo

        saltarBloqueLlaves(); // saltar el cuerpo en el pase 1

        String clave = fu.tipoReceptor != null
                ? fu.tipoReceptor + "." + fu.nombre
                : fu.nombre;
        funcionesUsuario.put(clave, fu);
        this.nodosGenerados.add(new NodoFuncion(fu.nombre, new ArrayList<>(), null,
                new NodoBloque(new ArrayList<>(), 0, 0), 0, 0));
    }

    // ── Saltar definición func completa ─────────────────────────────
    private void saltarDefFunc() {
        consume(); // 'func'
        if (esToken(PAREN_ABRE)) { consume(); saltarHastaToken(PAREN_CIERRA); consume(); }
        if (peek() != null) consume(); // nombre
        if (esToken(PAREN_ABRE)) {
            consume(); int p = 1;
            while (p > 0 && peek() != null) {
                if (esToken(PAREN_ABRE)) p++;
                else if (esToken(PAREN_CIERRA)) p--;
                consume();
            }
        }
        while (peek() != null && !esToken(LLAVE_ABRE)) consume();
        if (esToken(LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }

    private void saltarDefStruct() {
        consume(); // 'type'
        if (peek() != null) consume(); // nombre
        if (peek() != null && "struct".equals(peek().getLexema())) consume();
        if (esToken(LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }

    private void saltarDefStructSinType() {
        consume(); // nombre
        if (esToken(LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }


    private void procesarSentencia(Token t) {
        if (t == null) return;
        String lex = t.getLexema();

        // break / continue — por lexema (más seguro que enum)
        if ("break".equals(lex)) {
            consume();
            if (esToken(PUNTO_COMA)) consume();
            throw new BreakException();
        }
        if ("continue".equals(lex)) {
            consume();
            if (esToken(PUNTO_COMA)) consume();
            return;
        }

        switch (t.getTipo()) {
            case RES_VAR:    parseDeclaracion(); break;
            case RES_FOR:    parseFor();         break;
            case RES_SWITCH: parseSwitch();      break;
            case RES_IF:     parseIf();          break;
            case RES_RETURN: parseReturn();      break;
            case RES_TYPE:   saltarDefStruct();  break;
            case RES_FUNC:   parseFuncEnPase2(); break;

            case IDENTIFICADOR:
                if (esStructSinType()) { saltarDefStructSinType(); break; }
                if (siguienteEs(PAREN_ABRE)) {
                    String fn = consume().getLexema();
                    parseLlamadaFuncion(fn);
                    this.nodosGenerados.add(new NodoLlamadaFuncion(fn, new ArrayList<>(), 0, 0));
                } else {
                    parseAsignacion();
                }
                break;

            case RES_FMT_PRINTLN: case RES_STRCONV_ATOI:
            case RES_STRCONV_PARSEFLOAT: case RES_REFLECT_TYPEOF:
            case RES_APPEND: case RES_LEN: case RES_STRINGS_JOIN: {
                String fn = consume().getLexema();
                parseLlamadaFuncion(fn);
                this.nodosGenerados.add(new NodoLlamadaFuncion(fn, new ArrayList<>(), 0, 0));
                break;
            }

            case LLAVE_ABRE: case LLAVE_CIERRA: case PUNTO_COMA:
                consume(); break;

            default:
               
                if (siguienteEs(PAREN_ABRE)) {
                    String fn = consume().getLexema();
                    parseLlamadaFuncion(fn);
                    this.nodosGenerados.add(new NodoLlamadaFuncion(fn, new ArrayList<>(), 0, 0));
                } else {
                    consume();
                }
                break;
        }

        if (esToken(PUNTO_COMA)) consume();
    }

    private void parseFuncEnPase2() {
    int saved = pc;
    consume(); 
    if (esToken(PAREN_ABRE)) { consume(); saltarHastaToken(PAREN_CIERRA); consume(); }
    String nombre = peek() != null ? peek().getLexema() : "";
    pc = saved;

    if ("main".equals(nombre)) {
        FuncionUsuario main = funcionesUsuario.get("main");
        if (main != null) {
            saltarDefFunc();
            int afterDef = pc;

            textoAST.setLength(0); 
            textoAST.append("Programa\n");
            profundidadAST = 1;
            agregarNodoAST("Funcion: main()");
            profundidadAST = 2;

            pc = main.tokenInicio;
            try { ejecutarBloque(); }
            catch (ReturnException e) { }
            pc = afterDef;
            profundidadAST = 0;
        } else {
            saltarDefFunc();
        }
    } else {
        saltarDefFunc();
    }
}

    // =================================================================
    //  FOR — clásico, range, infinito, condición simple
    // =================================================================
    private void parseFor() {
        consume(); // 'for'
        this.nodosGenerados.add(new NodoFor(null, null, null,
                new NodoBloque(new ArrayList<>(), 0, 0), 0, 0));

        // for { } — infinito
        if (esToken(LLAVE_ABRE)) {
            int inicio = pc; int seg = 0;
            while (true) {
                pc = inicio; consume();
                try { ejecutarBloque(); } catch (BreakException e) { return; }
                if (seg++ > 2_000_000) throw new RuntimeException("Bucle infinito");
            }
        }

        if (hayPalabraAntesDeLlave("range"))  { parseForRange();   return; }
        if (haySemicolonAntesDeLlave())       { parseForClasico(); return; }

        // for condicion { }
        int inicio = pc; int seg = 0;
        while (true) {
            pc = inicio;
            boolean cond = evaluarCondicion();
            if (!esToken(LLAVE_ABRE) && !recuperarHastaLlave()) break;
            if (!cond) { consume(); saltarBloqueLlaves(); return; }
            consume();
            try { ejecutarBloque(); } catch (BreakException e) { return; }
            if (seg++ > 2_000_000) throw new RuntimeException("Bucle infinito");
        }
    }

    private boolean hayPalabraAntesDeLlave(String palabra) {
        int i = pc; int prof = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);
            if (t.getTipo() == PAREN_ABRE) prof++;
            if (t.getTipo() == PAREN_CIERRA) prof--;
            if (t.getTipo() == LLAVE_ABRE && prof == 0) break;
            if (t.getLexema().equals(palabra) && prof == 0) return true;
            i++;
        }
        return false;
    }

    private boolean haySemicolonAntesDeLlave() {
        int i = pc; int prof = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);
            if (t.getTipo() == PAREN_ABRE) prof++;
            if (t.getTipo() == PAREN_CIERRA) prof--;
            if (t.getTipo() == LLAVE_ABRE && prof == 0) break;
            if (t.getTipo() == PUNTO_COMA && prof == 0) return true;
            i++;
        }
        return false;
    }

    // ── for i, v := range coleccion { } ─────────────────────────────
    @SuppressWarnings("unchecked")
    private void parseForRange() {
        String varIdx = null, varVal = null;
        Token t1 = consume();
        varIdx = t1.getLexema();

        if (esToken(COMA)) { consume(); varVal = consume().getLexema(); }

        if (esToken(OP_ASIGN_CORTO) || esToken(OP_ASIGNACION)) consume();
        if (peek() != null && "range".equals(peek().getLexema())) consume();

        Object coleccion = evaluarExpresion();
        if (!esToken(LLAVE_ABRE) && !recuperarHastaLlave()) {
            System.err.println("[ParserAuxiliar] Aviso: se esperaba '{' en for range (pos " + pc + "), sentencia omitida");
            return;
        }

        int inicioCuerpo = pc;
        List<Object> lista = coleccion instanceof List
                ? (List<Object>) coleccion : new ArrayList<>();

        for (int i = 0; i < lista.size(); i++) {
            pc = inicioCuerpo;
            consume(); // '{'
            Entorno prev = this.entorno;
            Entorno iter = new Entorno(prev);
            this.entorno = iter;

            if (!"_".equals(varIdx) && varIdx != null) iter.declarar(varIdx, "int", i);
            if (varVal != null && !"_".equals(varVal))  iter.declarar(varVal, "auto", lista.get(i));

            try {
                ejecutarBloque();
            } catch (BreakException e) {
                this.entorno = prev; break;
            } catch (ReturnException e) {
                this.entorno = prev; throw e;
            }
            this.entorno = prev;
        }
        if (lista.isEmpty() && esToken(LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
    }

    // ── for init; cond; post { } ────────────────────────────────────
    private void parseForClasico() {
        // INIT
        Token init = peek();
        if (init != null && init.getTipo() == RES_VAR) {
            parseDeclaracion();
        } else if (init != null && init.getTipo() == IDENTIFICADOR) {
            parseAsignacion();
        }
        if (esToken(PUNTO_COMA)) consume();

        int inicioCondicion = pc;
        int seg = 0;

        while (true) {
            pc = inicioCondicion;

            // CONDICIÓN
            boolean cond = evaluarCondicion();
            if (esToken(PUNTO_COMA)) consume();

            // Localizar inicio del POST y del CUERPO
            int inicioPost = pc;
            int prof = 0;
            while (pc < tokens.size()) {
                Token tt = tokens.get(pc);
                if (tt.getTipo() == PAREN_ABRE) prof++;
                else if (tt.getTipo() == PAREN_CIERRA) prof--;
                else if (tt.getTipo() == LLAVE_ABRE && prof == 0) break;
                pc++;
            }
            int inicioCuerpo = pc;

            if (!cond) {
                pc = inicioCuerpo;
                if (esToken(LLAVE_ABRE)) { consume(); saltarBloqueLlaves(); }
                return;
            }

            // CUERPO
            consume(); // '{'
            try { ejecutarBloque(); }
            catch (BreakException e) { return; }

            int despuesCuerpo = pc;

            // POST
            pc = inicioPost;
            ejecutarPost();
            pc = despuesCuerpo;

            if (seg++ > 2_000_000) throw new RuntimeException("Bucle infinito");
        }
    }

    // ── Sentencia post: i++, i--, i+=1, i=i+1 ──────────────────────
    private void ejecutarPost() {
        Token t = peek();
        if (t == null || esToken(LLAVE_ABRE)) return;

        if (t.getTipo() == IDENTIFICADOR) {
            if (pc + 1 < tokens.size()) {
                TipoToken sig = tokens.get(pc + 1).getTipo();
                if (sig == OP_INCREMENTO) {
                    String n = consume().getLexema(); consume();
                    entorno.asignar(n, toInt(entorno.obtener(n)) + 1);
                    if (esToken(PUNTO_COMA)) consume();
                    return;
                }
                if (sig == OP_DECREMENTO) {
                    String n = consume().getLexema(); consume();
                    entorno.asignar(n, toInt(entorno.obtener(n)) - 1);
                    if (esToken(PUNTO_COMA)) consume();
                    return;
                }
            }
            parseAsignacion();
        }
    }

   
    private void parseSwitch() {
    consume(); // 'switch'
    Object val = evaluarExpresion();

    agregarNodoAST("Switch: " + str(val));
    profundidadAST++;
    this.nodosGenerados.add(new NodoSwitch(null, null, new ArrayList<>(), 0, 0));

    if (!esToken(LLAVE_ABRE) && !recuperarHastaLlave()) {
        profundidadAST--;
        return;
    }
    consume();
    boolean ejecutado = false;

    while (peek() != null && !esToken(LLAVE_CIERRA)) {
        Token t = peek();
        if (t.getTipo() == PUNTO_COMA) { consume(); continue; }
        if (t.getTipo() == RES_CASE) {
            consume();
            Object valCase = evaluarExpresion();
            if (esToken(DOS_PUNTOS)) consume();
            agregarNodoAST("Case: " + str(valCase));
            profundidadAST++;
            if (!ejecutado && iguales(val, valCase)) {
                try { ejecutarSentenciasCase(); } catch (BreakException ignored) {}
                ejecutado = true;
            } else {
                saltarSentenciasCase();
            }
            profundidadAST--;
        } else if (t.getTipo() == RES_DEFAULT) {
            consume();
            if (esToken(DOS_PUNTOS)) consume();
            agregarNodoAST("Default:");
            profundidadAST++;
            if (!ejecutado) {
                try { ejecutarSentenciasCase(); } catch (BreakException ignored) {}
                ejecutado = true;
            } else {
                saltarSentenciasCase();
            }
            profundidadAST--;
        } else {
            consume();
        }
    }
    if (esToken(LLAVE_CIERRA)) consume();
    profundidadAST--;
}

    private void ejecutarSentenciasCase() {
        while (peek() != null) {
            TipoToken tt = peek().getTipo();
            if (tt == RES_CASE || tt == RES_DEFAULT || tt == LLAVE_CIERRA) return;
            procesarSentencia(peek()); // BreakException sube
        }
    }

    private void saltarSentenciasCase() {
        while (peek() != null) {
            TipoToken tt = peek().getTipo();
            if (tt == RES_CASE || tt == RES_DEFAULT || tt == LLAVE_CIERRA) return;
            consume();
        }
    }

    // =================================================================
    //  IF / ELSE
    // =================================================================
    private void parseIf() {
    if (esToken(RES_IF)) consume();
    this.nodosGenerados.add(new NodoIf(null,
            new NodoBloque(new ArrayList<>(),0,0),null,null,0,0));

    // Capturar la condicion como texto
    int pcCond = pc;
    agregarNodoAST("If:");
    profundidadAST++;

    boolean cond = evaluarCondicion();
    agregarNodoAST("Condicion: " + (cond ? "true" : "false"));

    if (!esToken(LLAVE_ABRE) && !recuperarHastaLlave()) {
        profundidadAST--;
        return;
    }
    consume();

    agregarNodoAST("Entonces:");
    profundidadAST++;
    if (cond) ejecutarBloque(); else saltarBloqueLlaves();
    profundidadAST--;

    if (esToken(RES_ELSE)) {
        consume();
        agregarNodoAST("Sino:");
        profundidadAST++;
        if (esToken(RES_IF)) {
            profundidadAST--; 
            parseIf();
        } else {
            if (!esToken(LLAVE_ABRE)) recuperarHastaLlave();
            if (esToken(LLAVE_ABRE)) consume();
            if (!cond) ejecutarBloque(); else saltarBloqueLlaves();
            profundidadAST--;
        }
    }
    profundidadAST--;
}
    
    
    private void ejecutarBloque() {
        int llaves = 1;
        while (llaves > 0 && pc < tokens.size()) {
            Token t = tokens.get(pc);
            if (t.getTipo() == LLAVE_ABRE)  { llaves++; pc++; continue; }
            if (t.getTipo() == LLAVE_CIERRA) {
                llaves--; pc++;
                if (llaves == 0) break;
                continue;
            }
            if (llaves > 0) {
                int prev = pc;
                procesarSentencia(t);
                if (pc == prev) pc++;
            }
        }
    }

    private void saltarBloqueLlaves() {
        int llaves = 1;
        while (llaves > 0 && pc < tokens.size()) {
            Token t = tokens.get(pc);
            if (t.getTipo() == LLAVE_ABRE)   llaves++;
            else if (t.getTipo() == LLAVE_CIERRA) llaves--;
            pc++;
        }
    }

    // ─── Recuperación defensiva si falta '{' ──────────────────────────
    // Busca la '{' real dentro de una ventana razonable de tokens, en vez
    
    private boolean recuperarHastaLlave() {
        int limite = Math.min(tokens.size(), pc + 300);
        while (pc < limite && peek() != null && !esToken(LLAVE_ABRE)) {
            if (esToken(PUNTO_COMA) || esToken(LLAVE_CIERRA)) return false;
            consume();
        }
        return esToken(LLAVE_ABRE);
    }

    // =================================================================
    //  RETURN
    // =================================================================
    private void parseReturn() {
        consume();
        Object valor = null;
        if (peek() != null && peek().getTipo() != PUNTO_COMA
                && peek().getTipo() != LLAVE_CIERRA) {
            valor = evaluarExpresion();
        }
        this.nodosGenerados.add(new NodoReturn(null, 0, 0));
        throw new ReturnException(valor);
    }

 
    private void parseDeclaracion() {
    consume(); // 'var'
    String nombre = consume().getLexema();
    String tipo   = consume().getLexema();

    while (esToken(COR_ABRE)) {
        tipo += consume().getLexema();
        if (esToken(COR_CIERRA)) tipo += consume().getLexema();
    }

    agregarNodoAST("DeclVar: " + nombre + " [" + tipo + "]");
    profundidadAST++;

    if (esToken(OP_ASIGNACION)) {
        consume();
        Object val;
        if (esToken(LLAVE_ABRE)) {
            agregarNodoAST("SliceLiteral");
            val = parseSliceLiteral();
        } else {
            val = evaluarExpresionAST();
        }
        entorno.declarar(nombre, tipo, val);
    } else {
        agregarNodoAST("ValorDefecto: " + valorDefecto(tipo));
        entorno.declarar(nombre, tipo, valorDefecto(tipo));
    }

    profundidadAST--;
    this.nodosGenerados.add(new NodoDeclaracionVar(
            nombre, new NodoTipo(tipo, 0, 0), null, true, 0, 0));
}

    private Object valorDefecto(String tipo) {
        if (tipo.contains("[]"))    return null;
        switch (tipo) {
            case "int":     return 0;
            case "float64": return 0.0;
            case "bool":    return false;
            case "string":  return "";
            default:        return null;
        }
    }

    // =================================================================
    //  ASIGNACIÓN — x=v | x:=v | x.c=v | x[i]=v | x++ | x-- | x+=v/-=v/*=v//=v
    // =================================================================
    @SuppressWarnings("unchecked")
    private void parseAsignacion() {
        String nombre = consume().getLexema();

        // x++ / x--
        if (esToken(OP_INCREMENTO)) {
            consume();
            Object v = entorno.obtener(nombre);
            if (v != null && v != Entorno.NO_ENCONTRADO) entorno.asignar(nombre, toInt(v) + 1);
            return;
        }
        if (esToken(OP_DECREMENTO)) {
            consume();
            Object v = entorno.obtener(nombre);
            if (v != null && v != Entorno.NO_ENCONTRADO) entorno.asignar(nombre, toInt(v) - 1);
            return;
        }

        // x[i] = v  o  x[i][j] = v   (soporta también *= y /=)
        if (esToken(COR_ABRE)) {
            List<Object> indices = new ArrayList<>();
            while (esToken(COR_ABRE)) {
                consume(); indices.add(evaluarExpresion());
                if (esToken(COR_CIERRA)) consume();
            }
            Token op = peek();
            if (op == null) return;
            TipoToken opt = op.getTipo();
            boolean esCompuesto = opt == OP_SUMA || opt == OP_RESTA || opt == OP_MULT || opt == OP_DIV;
            if (opt == OP_ASIGNACION || opt == OP_ASIGN_CORTO || esCompuesto) {
                consume();
                Object newVal = evaluarExpresion();
                Object base = entorno.obtener(nombre);
                if (!(base instanceof List)) { if (esToken(PUNTO_COMA)) consume(); return; }
                List<Object> lista = (List<Object>) base;

    
                if (indices.size() == 1) {
                    int idx = toInt(indices.get(0));
                    Object actual = obtenerEnIndice(lista, idx, "asignacion x[i]");
                    if (esCompuesto) newVal = aplicarOperadorCompuesto(opt, actual, newVal);
                    asignarEnIndice(lista, idx, newVal, "asignacion x[i]");
                } else if (indices.size() == 2) {
                    int i = toInt(indices.get(0)), j = toInt(indices.get(1));
                    Object filaObj = obtenerEnIndice(lista, i, "asignacion x[i][j] (fila)");
                    if (filaObj instanceof List) {
                        List<Object> fila = (List<Object>) filaObj;
                        Object actual = obtenerEnIndice(fila, j, "asignacion x[i][j] (columna)");
                        if (esCompuesto) newVal = aplicarOperadorCompuesto(opt, actual, newVal);
                        asignarEnIndice(fila, j, newVal, "asignacion x[i][j] (columna)");
                    }
                }
            }
            if (esToken(PUNTO_COMA)) consume();
            return;
        }

        // x.campo = v
        List<String> ruta = new ArrayList<>();
        while (esToken(PUNTO)) {
            consume();
            if (esToken(IDENTIFICADOR)) ruta.add(consume().getLexema());
        }

        Token op = peek();
        if (op == null) return;
        TipoToken opt = op.getTipo();
        if (opt != OP_ASIGNACION && opt != OP_ASIGN_CORTO
                && opt != OP_SUMA && opt != OP_RESTA
                && opt != OP_MULT && opt != OP_DIV) return;
        consume();

        Object valor = evaluarExpresion();

        if (ruta.isEmpty()) {
            if (opt == OP_ASIGN_CORTO) {
                if (entorno.buscar(nombre) != null) entorno.asignar(nombre, valor);
                else entorno.declarar(nombre, "auto", valor);
                this.nodosGenerados.add(new NodoDeclaracionVar(nombre, null, null, false, 0, 0));
            } else if (opt == OP_ASIGNACION) {
                entorno.asignar(nombre, valor);
            } else {
                // += -= *= /=
                Object actual = entorno.obtener(nombre);
                entorno.asignar(nombre, aplicarOperadorCompuesto(opt, actual, valor));
            }
        } else {
            Object base = entorno.obtener(nombre);
            if (base == null || base == Entorno.NO_ENCONTRADO) {
                base = new HashMap<String, Object>();
                entorno.declarar(nombre, "struct", base);
            }
            if (base instanceof Map) {
                String cf = ruta.get(ruta.size() - 1);
                Map<String, Object> m = (Map<String, Object>) base;
                Object nuevoValor = (opt == OP_ASIGNACION || opt == OP_ASIGN_CORTO)
                        ? valor : aplicarOperadorCompuesto(opt, m.get(cf), valor);
                m.put(cf, nuevoValor);
            }
        }
        if (esToken(PUNTO_COMA)) consume();
    }


    private Object parseSliceLiteral() {
        consume(); // '{'
        List<Object> lista = new ArrayList<>();
        while (peek() != null && !esLexema(peek(), "}")) {
            if (esToken(COMA) || esToken(PUNTO_COMA)) { consume(); continue; }
            if (esToken(LLAVE_ABRE)) lista.add(parseSliceLiteral());
            else                     lista.add(evaluarExpresion());
        }
        if (esToken(LLAVE_CIERRA)) consume();
        return lista;
    }

    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInstanciacionStruct(String nombreStruct) {
        consume(); // '{'
        Map<String, Object> inst = new HashMap<>();
        this.nodosGenerados.add(new NodoInstanciaStruct(nombreStruct, new ArrayList<>(), 0, 0));

        while (peek() != null && !esToken(LLAVE_CIERRA)) {
            if (esToken(PUNTO_COMA) || esToken(COMA)) { consume(); continue; }
            if (!esToken(IDENTIFICADOR)) { consume(); continue; }
            String campo = consume().getLexema();
            if (esToken(DOS_PUNTOS)) consume();
            inst.put(campo, evaluarExpresion());
            if (esToken(COMA)) consume();
        }
        if (esToken(LLAVE_CIERRA)) consume();
        return inst;
    }

    // =================================================================
    //  LLAMADA A FUNCIÓN
    // =================================================================
    private Object parseLlamadaFuncion(String nombre) {
        if (!esToken(PAREN_ABRE)) throw new RuntimeException("Se esperaba '(' en llamada a " + nombre);
        consume();
        List<Object> args = evaluarArgumentos();
        if (!esToken(PAREN_CIERRA)) throw new RuntimeException("Se esperaba ')' en llamada a " + nombre);
        consume();

        // fmt.Println — interceptado para formato Go
        if ("fmt.Println".equals(nombre)) {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(str(args.get(i)));
                }
            }
            sb.append("\n");
            this.entorno.agregarSalida(sb.toString());
            return null;
        }

        if ("append".equals(nombre)) return ejecutarAppend(args);
        if ("len".equals(nombre))    return ejecutarLen(args);

        // Otras nativas del entorno (slices.Index, strings.Join, strconv.Atoi, etc.)
        if (entorno.esNativa(nombre))
            return entorno.ejecutarNativa(nombre, args, this.entorno);

        // Funciones de usuario
        FuncionUsuario fu = funcionesUsuario.get(nombre);
        if (fu == null) {
            // Recuperación: reportar y devolver null
            System.err.println("[ParserAuxiliar] Funcion no encontrada: " + nombre);
            return null;
        }
        return ejecutarFuncionUsuario(fu, args, null);
    }

    @SuppressWarnings("unchecked")
    private Object ejecutarAppend(List<Object> args) {
        if (args.isEmpty()) return new ArrayList<>();
        List<Object> lista = args.get(0) instanceof List
                ? new ArrayList<>((List<Object>) args.get(0)) : new ArrayList<>();
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

    // =================================================================
    //  EJECUTAR FUNCIÓN DE USUARIO
    // =================================================================
    private Object ejecutarFuncionUsuario(FuncionUsuario fu, List<Object> args, Object receptor) {
        int savedPc      = pc;
        Entorno savedEnv = this.entorno;

        Entorno nuevoEnv = new Entorno(savedEnv, fu.nombre);

        // Bind receptor si es método
        if (fu.tipoReceptor != null && fu.nombreReceptor != null && receptor != null)
            nuevoEnv.declarar(fu.nombreReceptor, fu.tipoReceptor, receptor);

        // Bind parámetros
        for (int i = 0; i < fu.paramNombres.size(); i++) {
            Object val = i < args.size() ? args.get(i) : null;
            nuevoEnv.declarar(fu.paramNombres.get(i), fu.paramTipos.get(i), val);
        }

        this.entorno = nuevoEnv;
        pc           = fu.tokenInicio;

        Object retorno = null;
        try {
            ejecutarBloque();
        } catch (ReturnException e) {
            retorno = e.valor;
        } finally {
            this.entorno = savedEnv;
            pc           = savedPc;
        }
        return retorno;
    }

    // =================================================================
    //  ARGUMENTOS
    // =================================================================
    public List<Object> evaluarArgumentos() {
        List<Object> args = new ArrayList<>();
        if (esToken(PAREN_CIERRA)) return args;
        do {
            args.add(evaluarExpresion());
            if (esToken(COMA)) consume(); else break;
        } while (!esToken(PAREN_CIERRA) && peek() != null);
        return args;
    }

    // =================================================================
    //  EXPRESIONES
    // =================================================================
    public Object evaluarExpresion() { return evaluarOr(); }
    private boolean evaluarCondicion() { return esVerdadero(evaluarExpresion()); }

    private Object evaluarOr() {
        Object izq = evaluarAnd();
        while (esToken(OP_OR)) { consume(); izq = esVerdadero(izq) || esVerdadero(evaluarAnd()); }
        return izq;
    }

    private Object evaluarAnd() {
        Object izq = evaluarIgualdad();
        while (esToken(OP_AND)) { consume(); izq = esVerdadero(izq) && esVerdadero(evaluarIgualdad()); }
        return izq;
    }

    private Object evaluarIgualdad() {
        Object izq = evaluarComparacion();
        while (esToken(OP_IGUAL) || esToken(OP_DIFERENTE)) {
            Token op = consume();
            boolean eq = iguales(izq, evaluarComparacion());
            izq = op.getTipo() == OP_IGUAL ? eq : !eq;
        }
        return izq;
    }

    private Object evaluarComparacion() {
        Object izq = evaluarSuma();
        while (esComparacion()) {
            Token op = consume();
            Object der = evaluarSuma();
            double v1 = toDouble(izq), v2 = toDouble(der);
            String lex = op.getLexema();
            if ("<".equals(lex)  || op.getTipo() == OP_MENOR)       izq = v1 <  v2;
            else if (">".equals(lex) || op.getTipo() == OP_MAYOR)   izq = v1 >  v2;
            else if ("<=".equals(lex))                               izq = v1 <= v2;
            else if (">=".equals(lex))                               izq = v1 >= v2;
            else {
                // Fallback por tipo de enum si existe OP_MENOR_IGUAL / OP_MAYOR_IGUAL
                try {
                    if (op.getTipo().name().equals("OP_MENOR_IGUAL")) { izq = v1 <= v2; }
                    else if (op.getTipo().name().equals("OP_MAYOR_IGUAL")) { izq = v1 >= v2; }
                } catch (Exception ignored) { izq = false; }
            }
        }
        return izq;
    }

    private boolean esComparacion() {
        if (pc >= tokens.size()) return false;
        Token t = tokens.get(pc);
        TipoToken tt = t.getTipo();
        if (tt == OP_MENOR || tt == OP_MAYOR) return true;
        String lex = t.getLexema();
        if ("<=".equals(lex) || ">=".equals(lex)) return true;
        try { String name = tt.name(); return name.equals("OP_MENOR_IGUAL") || name.equals("OP_MAYOR_IGUAL"); }
        catch (Exception e) { return false; }
    }

    private Object evaluarSuma() {
        Object izq = evaluarProducto();
        while (esToken(OP_SUMA) || esToken(OP_RESTA)) {
            Token op = consume(); Object der = evaluarProducto();
            if (op.getTipo() == OP_SUMA && (izq instanceof String || der instanceof String))
                izq = str(izq) + str(der);
            else izq = op.getTipo() == OP_SUMA ? numSum(izq, der) : numSub(izq, der);
        }
        return izq;
    }

    private Object evaluarProducto() {
        Object izq = evaluarUnario();
        while (esToken(OP_MULT) || esToken(OP_DIV) || esMod()) {
            Token op = consume(); Object der = evaluarUnario();
            if ("%".equals(op.getLexema()) || esTipoEnum(op, "OP_MOD")) {
                int divisor = toInt(der);
                if (divisor == 0) {
                    System.err.println("[ParserAuxiliar] Aviso: modulo por cero, se devuelve 0");
                    izq = 0;
                } else {
                    izq = toInt(izq) % divisor;
                }
            } else if (op.getTipo() == OP_MULT) {
                izq = preserveInt(izq, der, toDouble(izq) * toDouble(der));
            } else {
             
                double v2 = toDouble(der);
                if (v2 == 0) {
                    System.err.println("[ParserAuxiliar] Aviso: division por cero, se devuelve 0");
                    izq = 0;
                } else {
                    izq = preserveInt(izq, der, toDouble(izq) / v2);
                }
            }
        }
        return izq;
    }

    private boolean esMod() {
        if (pc >= tokens.size()) return false;
        Token t = tokens.get(pc);
        if ("%".equals(t.getLexema())) return true;
        return esTipoEnum(t, "OP_MOD");
    }

    private boolean esTipoEnum(Token t, String nombre) {
        try { return t.getTipo().name().equals(nombre); } catch (Exception e) { return false; }
    }

    private Object evaluarUnario() {
        if (esToken(OP_RESTA)) { consume(); Object v = evaluarUnario(); return v instanceof Integer ? -(Integer)v : -toDouble(v); }
        if (esToken(OP_NOT))   { consume(); return !esVerdadero(evaluarUnario()); }
        return evaluarPrimario();
    }


    @SuppressWarnings("unchecked")
    private Object evaluarPrimario() {
        Token t = peek();
        if (t == null) throw new RuntimeException("Fin inesperado en expresión");

        // Tipo de slice: []int{...}
        if (esToken(COR_ABRE)) {
            while (peek() != null && !esToken(LLAVE_ABRE)) consume();
            return parseSliceLiteral();
        }

        t = consume();
        switch (t.getTipo()) {
            case LIT_ENTERO:   return Integer.parseInt(t.getLexema());
            case LIT_STRING:   return t.getLexema().replace("\"", "");
            case LIT_BOOLEANO: return Boolean.parseBoolean(t.getLexema());
            case LIT_NIL:      return null;

            case IDENTIFICADOR: {
                String id = t.getLexema();
                if ("_".equals(id) || "range".equals(id)) return null;

                
                try { return Double.parseDouble(id); } catch (NumberFormatException ignored) {}

                Object obj;
                boolean existe = entorno.buscar(id) != null;

         
                if (!existe && esToken(LLAVE_ABRE)) {
                    obj = parseInstanciacionStruct(id);
                }
                // ¿Llamada a función?
                else if (esToken(PAREN_ABRE)) {
                    obj = parseLlamadaFuncion(id);
                }
                // Variable
                else {
                    Object v = entorno.obtener(id);
                    if (v == Entorno.NO_ENCONTRADO) {
                        // Puede ser una función de usuario — intentar como llamada
                        if (funcionesUsuario.containsKey(id) && esToken(PAREN_ABRE)) {
                            obj = parseLlamadaFuncion(id);
                        } else {
                            obj = null; // recuperación silenciosa
                        }
                    } else {
                        obj = v;
                    }
                }

              
                while (esToken(COR_ABRE) || esToken(PUNTO)) {
                    if (esToken(COR_ABRE)) {
                        consume();
                        Object idxObj = evaluarExpresion();
                        int idx = toInt(idxObj);
                        if (esToken(COR_CIERRA)) consume();

                        if (obj instanceof List) {
                            obj = obtenerEnIndice((List<Object>) obj, idx, "acceso a slice");
                        } else {
                            obj = null;
                        }
                    } else { // PUNTO
                        consume();
                        if (!esToken(IDENTIFICADOR)) break;
                        String campo = consume().getLexema();

      
                        if (!(obj instanceof Map)) {
                            if (esToken(PAREN_ABRE)) {
                                consume(); evaluarArgumentos();
                                if (esToken(PAREN_CIERRA)) consume();
                            }
                            obj = null;
                            continue;
                        }

                        if (esToken(PAREN_ABRE)) {
                            // Llamada a método de struct
                            String tipoRec = buscarTipoStruct(obj);
                            FuncionUsuario met = funcionesUsuario.get(tipoRec + "." + campo);
                            if (met != null) {
                                consume(); List<Object> argsMet = evaluarArgumentos();
                                if (esToken(PAREN_CIERRA)) consume();
                                obj = ejecutarFuncionUsuario(met, argsMet, obj);
                            } else {
                                consume(); evaluarArgumentos();
                                if (esToken(PAREN_CIERRA)) consume();
                                obj = null;
                            }
                        } else {
                            // Acceso a campo
                            obj = ((Map<String, Object>) obj).get(campo);
                        }
                    }
                }
                return obj;
            }

            case RES_FMT_PRINTLN: case RES_STRCONV_ATOI:
            case RES_STRCONV_PARSEFLOAT: case RES_REFLECT_TYPEOF:
            case RES_APPEND: case RES_LEN: case RES_STRINGS_JOIN:
                return parseLlamadaFuncion(t.getLexema());

            case PAREN_ABRE: {
                Object val = evaluarExpresion();
                if (esToken(PAREN_CIERRA)) consume();
                return val;
            }

            default:
            
                if (esToken(PAREN_ABRE)) {
                    return parseLlamadaFuncion(t.getLexema());
                }
                // Último intento: número flotante
                try { return Double.parseDouble(t.getLexema()); } catch (NumberFormatException ignored) {}
                return null;
        }
    }


    @SuppressWarnings("unchecked")
    private String buscarTipoStruct(Object obj) {

        if (obj == null)             return "";
        if (obj instanceof List)     return "";   
        if (!(obj instanceof Map))   return "";   

        Map<?, ?> m;
        try { m = (Map<?, ?>) obj; }
        catch (Exception e) { return ""; }
        if (m.isEmpty()) return "";

        for (Map.Entry<String, FuncionUsuario> entry : funcionesUsuario.entrySet()) {
            FuncionUsuario fu = entry.getValue();
            if (fu == null || fu.tipoReceptor == null) continue;
            try {
                ast.NodoStruct ns = entorno.buscarStruct(fu.tipoReceptor);
                if (ns == null) continue;
                Map<String, String> campos = null;
                try { campos = (Map<String, String>) ns.getCampos(); }
                catch (Exception ex) { continue; }
                if (campos == null || campos.isEmpty()) continue;
            
                boolean match = false;
                for (String c : campos.keySet()) {
                    if (m.containsKey(c)) { match = true; break; }
                }
                if (match) return fu.tipoReceptor;
            } catch (Exception ex) { /* continuar con el siguiente */ }
        }
        return "";
    }

    
    private boolean iguales(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) return toDouble(a) == toDouble(b);
        return a.equals(b);
    }

    private boolean esVerdadero(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Integer) return ((Integer) v) != 0;
        if (v instanceof Double)  return ((Double)  v) != 0.0;
        if (v instanceof String)  return !((String)  v).isEmpty();
        return false;
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
        if (o == null) return "<nil>";
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> l = (List<?>) o;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(str(l.get(i)));
            }
            sb.append("]"); return sb.toString();
        }
        // Doubles que son enteros: mostrar sin decimal
        if (o instanceof Double) {
            double d = (Double) o;
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        return o.toString();
    }

    private Object numSum(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer)a + (Integer)b;
        return toDouble(a) + toDouble(b);
    }

    private Object numSub(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer)a - (Integer)b;
        return toDouble(a) - toDouble(b);
    }

    private Object preserveInt(Object a, Object b, double r) {
        if (a instanceof Integer && b instanceof Integer && r == (int)r) return (int)r;
        return r;
    }

   
    private Object aplicarOperadorCompuesto(TipoToken opt, Object actual, Object nuevo) {
        if (opt == OP_SUMA) {
            return (actual instanceof String || nuevo instanceof String)
                    ? str(actual) + str(nuevo) : numSum(actual, nuevo);
        }
        if (opt == OP_RESTA) return numSub(actual, nuevo);
        if (opt == OP_MULT)  return preserveInt(actual, nuevo, toDouble(actual) * toDouble(nuevo));
        if (opt == OP_DIV) {
            double d = toDouble(nuevo);
            if (d == 0) {
                // NUEVO: división por cero "suave" en operador compuesto.
                System.err.println("[ParserAuxiliar] Aviso: division por cero en operador compuesto, valor sin cambios");
                return actual;
            }
            return preserveInt(actual, nuevo, toDouble(actual) / d);
        }
        return nuevo; // OP_ASIGNACION / OP_ASIGN_CORTO -> reemplazo directo
    }

    
    private Object obtenerEnIndice(List<Object> lista, int idx, String contexto) {
        if (lista == null) return null;
        if (idx < 0 || idx >= lista.size()) {
            System.err.println("[ParserAuxiliar] Aviso: indice " + idx
                    + " fuera de rango (longitud " + lista.size() + ") en " + contexto + ", se devuelve nil");
            return null;
        }
        return lista.get(idx);
    }

    
    private boolean asignarEnIndice(List<Object> lista, int idx, Object valor, String contexto) {
        if (lista == null) return false;
        if (idx < 0 || idx >= lista.size()) {
            System.err.println("[ParserAuxiliar] Aviso: indice " + idx
                    + " fuera de rango (longitud " + lista.size() + ") en " + contexto + ", asignacion omitida");
            return false;
        }
        lista.set(idx, valor);
        return true;
    }

    private boolean esLexema(Token t, String s) {
        return t != null && t.getLexema().equals(s);
    }

    // ─── Navegación ──────────────────────────────────────────────────
    private boolean esToken(TipoToken tipo) {
        return pc < tokens.size() && tokens.get(pc).getTipo() == tipo;
    }

    private boolean siguienteEs(TipoToken tipo) {
        return pc + 1 < tokens.size() && tokens.get(pc + 1).getTipo() == tipo;
    }

    private void saltarHastaToken(TipoToken stop) {
        while (peek() != null && !esToken(stop)) consume();
    }

    private Token consume() {
        if (pc >= tokens.size())
            throw new RuntimeException("Fin inesperado del archivo (pos " + pc + ")");
        return tokens.get(pc++);
    }

    private Token peek() {
        return pc < tokens.size() ? tokens.get(pc) : null;
    }

private Object evaluarExpresionAST() {
    int pcAntes = pc;
    Object val = evaluarExpresion();
    // Reconstruir el texto de la expresión desde los tokens consumidos
    StringBuilder exprStr = new StringBuilder();
    for (int i = pcAntes; i < pc; i++) {
        exprStr.append(tokens.get(i).getLexema());
    }
    String textoExpr = exprStr.toString().trim();
    // Simplificar si es muy largo
    if (textoExpr.length() > 40) textoExpr = textoExpr.substring(0, 37) + "...";
    agregarNodoAST("Expr: " + textoExpr + " => " + str(val));
    return val;
}

public List<Object> evaluarArgumentosAST() {
    List<Object> args = new ArrayList<>();
    if (esToken(PAREN_CIERRA)) return args;
    int argNum = 0;
    do {
        int pcAntes = pc;
        Object val = evaluarExpresion();
        StringBuilder exprStr = new StringBuilder();
        for (int i = pcAntes; i < pc; i++) exprStr.append(tokens.get(i).getLexema());
        String textoExpr = exprStr.toString().trim();
        if (textoExpr.length() > 35) textoExpr = textoExpr.substring(0, 32) + "...";
        agregarNodoAST("Arg[" + argNum++ + "]: " + textoExpr + " => " + str(val));
        args.add(val);
        if (esToken(COMA)) consume(); else break;
    } while (!esToken(PAREN_CIERRA) && peek() != null);
    return args;
}}