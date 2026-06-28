package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;

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
    public Object execute(Entorno entorno) {
        // CAMBIO CRÍTICO: Llamar a execute() de los hijos, NO a getValue()
        // Si un hijo retorna null, lo atrapamos aquí mismo.
        Object izq = izquierdo.execute(entorno);
        Object der = derecho.execute(entorno);

        if (izq == null || der == null) {
            throw new ErrorSemanticoException(
                "La operación '" + operador + "' no puede ejecutarse porque uno de sus operandos es nulo.",
                linea, columna
            );
        }

        Object resultado;
        switch (operador) {
            case "+":  resultado = opSuma(izq, der); break;
            case "-":  resultado = opResta(izq, der); break;
            case "*":  resultado = opMult(izq, der); break;
            case "/":  resultado = opDiv(izq, der); break;
            case "%":  resultado = opMod(izq, der); break;
            case "==": resultado = opIgualdad(izq, der, true); break;
            case "!=": resultado = opIgualdad(izq, der, false); break;
            case "<":  resultado = opRelacional(izq, der, "<"); break;
            case ">":  resultado = opRelacional(izq, der, ">"); break;
            case "<=": resultado = opRelacional(izq, der, "<="); break;
            case ">=": resultado = opRelacional(izq, der, ">="); break;
            case "&&": resultado = opAnd(izq, der); break;
            case "||": resultado = opOr(izq, der); break;
            default:
                throw new ErrorSemanticoException(
                    "Operador binario desconocido: '" + operador + "'.", linea, columna
                );
        }

        // Blindaje final: Nunca retornar null
        if (resultado == null) {
            throw new ErrorSemanticoException(
                "Resultado de operación '" + operador + "' es nulo (error interno).", linea, columna
            );
        }
        
        return resultado;
    }

    // Suma 
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

    // Resta 
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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("ExpresionBinaria\n");
        sb.append(indent(nivel + 1)).append("Operador: ").append(operador).append("\n");
        if (izquierdo != null) sb.append(izquierdo.toAST(nivel + 1));
        if (derecho != null) sb.append(derecho.toAST(nivel + 1));
        return sb.toString();
    }

    @Override
    public Object getValue(Entorno entorno) {
        
        return this.execute(entorno);
    }
}