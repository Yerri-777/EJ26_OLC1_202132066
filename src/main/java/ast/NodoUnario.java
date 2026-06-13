package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;


public class NodoUnario extends NodoExpresion {

    private final String        operador;
    private final NodoExpresion operando;

    public NodoUnario(String operador, NodoExpresion operando, int linea, int columna) {
        super(linea, columna);
        this.operador = operador;
        this.operando = operando;
    }

    @Override
    public Object getValue(Entorno entorno) {
        Object val = operando.getValue(entorno);

        if (val == null) {
            throw new ErrorSemanticoException(
                "Operación unaria '" + operador + "' no válida sobre nil.",
                linea, columna
            );
        }

        switch (operador) {
            case "-":
                if (val instanceof Integer) return -(Integer) val;
                if (val instanceof Double)  return -(Double)  val;
                throw new ErrorSemanticoException(
                    "Negación unaria '-' solo aplica a tipos numéricos. " +
                    "Se recibió: " + tipoDe(val) + ".",
                    linea, columna
                );

            case "!":
                if (val instanceof Boolean) return !(Boolean) val;
                throw new ErrorSemanticoException(
                    "Operador '!' solo aplica a bool. Se recibió: " + tipoDe(val) + ".",
                    linea, columna
                );

            default:
                throw new ErrorSemanticoException(
                    "Operador unario desconocido: " + operador, linea, columna
                );
        }
    }

    private String tipoDe(Object v) {
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "float64";
        if (v instanceof Boolean) return "bool";
        if (v instanceof String)  return "string";
        return "desconocido";
    }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "Unario: " + operador + "\n" +
               operando.toAST(nivel + 1);
    }
}