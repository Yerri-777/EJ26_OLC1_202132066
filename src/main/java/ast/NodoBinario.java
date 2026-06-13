package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;

/**
 * NodoBinario — Operación con dos operandos.
 *
 * Cubre TODOS los operadores del PDF:
 *   Aritméticos : +  -  *  /  %
 *   Asignación  : los usa NodoAsignacionCompuesta delegando aquí
 *   Comparación : ==  !=  <  >  <=  >=
 *   Lógicos     : &&  ||
 *
 *Operadores Aritméticos):
 *   int op int          int
 *   int op float64      float64
 *   float64 op int      float64
 *   float64 op float64  float64
 *   string + string     string   (solo para +)
 */
public class NodoBinario extends NodoExpresion {

    private final String        operador;
    private final NodoExpresion izquierdo;
    private final NodoExpresion derecho;

    public NodoBinario(String operador,
                       NodoExpresion izquierdo,
                       NodoExpresion derecho,
                       int linea, int columna) {
        super(linea, columna);
        this.operador  = operador;
        this.izquierdo = izquierdo;
        this.derecho   = derecho;
    }

    @Override
    public Object getValue(Entorno entorno) {
        Object izq = izquierdo.getValue(entorno);
        Object der = derecho.getValue(entorno);

        // Nil check — ninguna operación sobre nil es válida
        if (izq == null || der == null) {
            throw new ErrorSemanticoException(
                "Operación '" + operador + "' no válida sobre un valor nil.",
                linea, columna
            );
        }

        switch (operador) {
            case "+":  return opSuma(izq, der);
            case "-":  return opResta(izq, der);
            case "*":  return opMult(izq, der);
            case "/":  return opDiv(izq, der);
            case "%":  return opMod(izq, der);
            case "==": return opIgualdad(izq, der, true);
            case "!=": return opIgualdad(izq, der, false);
            case "<":  return opRelacional(izq, der, "<");
            case ">":  return opRelacional(izq, der, ">");
            case "<=": return opRelacional(izq, der, "<=");
            case ">=": return opRelacional(izq, der, ">=");
            case "&&": return opAnd(izq, der);
            case "||": return opOr(izq, der);
            default:
                throw new ErrorSemanticoException(
                    "Operador binario desconocido: '" + operador + "'.", linea, columna
                );
        }
    }

    //Suma 
    private Object opSuma(Object izq, Object der) {
        // string + string → concatenación
        if (izq instanceof String && der instanceof String) {
            return (String) izq + (String) der;
        }
        // int + int → int
        if (isInt(izq) && isInt(der)) {
            return toInt(izq) + toInt(der);
        }
        // cualquier combinación numérica con float → float64
        if (isNumerico(izq) && isNumerico(der)) {
            return toDouble(izq) + toDouble(der);
        }
        throw errorTipos("+", izq, der);
    }

    //  Resta 
    private Object opResta(Object izq, Object der) {
        if (isInt(izq) && isInt(der))           return toInt(izq) - toInt(der);
        if (isNumerico(izq) && isNumerico(der)) return toDouble(izq) - toDouble(der);
        throw errorTipos("-", izq, der);
    }

    // Multiplicación 
    private Object opMult(Object izq, Object der) {
        if (isInt(izq) && isInt(der))           return toInt(izq) * toInt(der);
        if (isNumerico(izq) && isNumerico(der)) return toDouble(izq) * toDouble(der);
        throw errorTipos("*", izq, der);
    }

    // División
    private Object opDiv(Object izq, Object der) {
        if (!isNumerico(izq) || !isNumerico(der)) throw errorTipos("/", izq, der);

        // Verificar división por cero
        if (toDouble(der) == 0.0) {
            throw new ErrorSemanticoException(
                "No se puede dividir entre cero.", linea, columna
            );
        }
        // int / int → int (trunca, como en Go)
        if (isInt(izq) && isInt(der)) return toInt(izq) / toInt(der);
        return toDouble(izq) / toDouble(der);
    }

    // Módulo
    private Object opMod(Object izq, Object der) {
        
        if (!isInt(izq) || !isInt(der)) {
            throw new ErrorSemanticoException(
                "Operación '%' solo es válida entre int e int. " +
                "Se recibió '" + tipoDe(izq) + "' y '" + tipoDe(der) + "'.",
                linea, columna
            );
        }
        if (toInt(der) == 0) {
            throw new ErrorSemanticoException(
                "No se puede calcular el módulo con divisor cero.", linea, columna
            );
        }
        return toInt(izq) % toInt(der);
    }

    // Igualdad / Desigualdad 
    private Object opIgualdad(Object izq, Object der, boolean esIgual) {
        boolean resultado;

        // Numéricos: comparar como double para int==float64 
        if (isNumerico(izq) && isNumerico(der)) {
            resultado = toDouble(izq) == toDouble(der);
        } else if (izq instanceof Boolean && der instanceof Boolean) {
            resultado = izq.equals(der);
        } else if (izq instanceof String && der instanceof String) {
            // Las comparaciones entre cadenas se hacen lexicográficamente
            resultado = izq.equals(der);
        } else {
            throw new ErrorSemanticoException(
                "Comparación '" + (esIgual ? "==" : "!=") + "' no válida entre '" +
                tipoDe(izq) + "' y '" + tipoDe(der) + "'.",
                linea, columna
            );
        }
        return esIgual ? resultado : !resultado;
    }

    // Relacionales 
    private Object opRelacional(Object izq, Object der, String op) {
        // Numéricos (int float64 rune)
        if (isNumerico(izq) && isNumerico(der)) {
            double i = toDouble(izq), d = toDouble(der);
            switch (op) {
                case "<":  return i < d;
                case ">":  return i > d;
                case "<=": return i <= d;
                case ">=": return i >= d;
            }
        }
        // String comparación lexicográfica
        if (izq instanceof String && der instanceof String) {
            int cmp = ((String) izq).compareTo((String) der);
            switch (op) {
                case "<":  return cmp < 0;
                case ">":  return cmp > 0;
                case "<=": return cmp <= 0;
                case ">=": return cmp >= 0;
            }
        }
        throw errorTipos(op, izq, der);
    }

    // Lógicos 
    private Object opAnd(Object izq, Object der) {
        if (!(izq instanceof Boolean) || !(der instanceof Boolean)) {
            throw new ErrorSemanticoException(
                "Operador '&&' requiere dos operandos bool. " +
                "Se recibió '" + tipoDe(izq) + "' y '" + tipoDe(der) + "'.",
                linea, columna
            );
        }
        return (Boolean) izq && (Boolean) der;
    }

    private Object opOr(Object izq, Object der) {
        if (!(izq instanceof Boolean) || !(der instanceof Boolean)) {
            throw new ErrorSemanticoException(
                "Operador '||' requiere dos operandos bool. " +
                "Se recibió '" + tipoDe(izq) + "' y '" + tipoDe(der) + "'.",
                linea, columna
            );
        }
        return (Boolean) izq || (Boolean) der;
    }

    // Utilidades de tipos 

    /**int y rune son ambos Integer en Java */
    private boolean isInt(Object v)      { return v instanceof Integer; }
    private boolean isNumerico(Object v) { return v instanceof Integer || v instanceof Double; }

    private int    toInt(Object v)    {
        if (v instanceof Double) return ((Double) v).intValue();
        return (Integer) v;
    }
    private double toDouble(Object v) {
        if (v instanceof Integer) return ((Integer) v).doubleValue();
        return (Double) v;
    }

    private String tipoDe(Object v) {
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "float64";
        if (v instanceof String)  return "string";
        if (v instanceof Boolean) return "bool";
        if (v == null)            return "nil";
        return v.getClass().getSimpleName();
    }

    private ErrorSemanticoException errorTipos(String op, Object izq, Object der) {
        return new ErrorSemanticoException(
            "Operación '" + op + "' no válida entre '" +
            tipoDe(izq) + "' y '" + tipoDe(der) + "'.",
            linea, columna
        );
    }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "Binario: " + operador + "\n" +
               izquierdo.toAST(nivel + 1) +
               derecho.toAST(nivel + 1);
    }
}