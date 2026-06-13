package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;


public class NodoDeclaracionVar extends NodoSentencia {

    private final String        nombre;
    private final NodoTipo      tipo;
    private final NodoExpresion inicializador;
    private final boolean       esExplicita;   // true = var, false = :=

    public NodoDeclaracionVar(String nombre,
                               NodoTipo tipo,
                               NodoExpresion inicializador,
                               boolean esExplicita,
                               int linea, int columna) {
        super(linea, columna);
        this.nombre        = nombre;
        this.tipo          = tipo;
        this.inicializador = inicializador;
        this.esExplicita   = esExplicita;
    }

    @Override
    public Object execute(Entorno entorno) {
        // Verificar que no exista en el mismo ámbito
        if (entorno.existeEnActual(nombre)) {
            throw new ErrorSemanticoException(
                "La variable '" + nombre + "' ya fue declarada en este ámbito.",
                linea, columna
            );
        }

        String tipoFinal;
        Object valorFinal;

        if (esExplicita) {
            // ── var id tipo [= expr] ────────────────────────────────────────
            tipoFinal = tipo.getNombre();

            if (inicializador != null) {
                Object valorExpr = inicializador.getValue(entorno);
                valorFinal = convertirYValidar(valorExpr, tipoFinal);
            } else {
                // Sin inicializador → valor por defecto del tipo
                valorFinal = tipo.valorPorDefecto();
            }
        } else {
            // ── id := expr ──────────────────────────────────────────────────
            Object valorExpr = inicializador.getValue(entorno);
            tipoFinal  = inferirTipo(valorExpr);
            valorFinal = valorExpr;
        }

        entorno.declarar(nombre, tipoFinal, valorFinal, linea, columna);
        return null;
    }

    /**
     * Valida compatibilidad de tipos y aplica conversión implícita 
     */
    private Object convertirYValidar(Object valor, String tipoDeclarado) {
        if (valor == null) return null;  // nil

        String tipoValor = inferirTipo(valor);

        // Conversión implícita int  float64 (
        if (tipoDeclarado.equals("float64") && tipoValor.equals("int")) {
            return ((Integer) valor).doubleValue();
        }

        // Tipos idénticos OK
        if (tipoDeclarado.equals(tipoValor)) return valor;

        // rune es alias de int en nuestro modelo
        if (tipoDeclarado.equals("rune") && tipoValor.equals("int")) return valor;
        if (tipoDeclarado.equals("int")  && tipoValor.equals("rune")) return valor;

        throw new ErrorSemanticoException(
            "No se puede asignar un valor de tipo '" + tipoValor +
            "' a la variable '" + nombre + "' de tipo '" + tipoDeclarado + "'.",
            linea, columna
        );
    }

    private String inferirTipo(Object valor) {
        if (valor instanceof Integer) return "int";
        if (valor instanceof Double)  return "float64";
        if (valor instanceof String)  return "string";
        if (valor instanceof Boolean) return "bool";
        if (valor instanceof Character) return "rune";
        return "nil";  
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel))
          .append(esExplicita ? "DeclVar: " : "DeclCorta: ")
          .append(nombre);
        if (tipo != null) sb.append(" ").append(tipo.getNombre());
        sb.append("\n");
        if (inicializador != null) sb.append(inicializador.toAST(nivel + 1));
        return sb.toString();
    }
}