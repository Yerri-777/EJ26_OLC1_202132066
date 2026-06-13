package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;


public class NodoIncrementoDecremento extends NodoSentencia {

    private final String nombre;
    private final String operador; 
    // "++" o "--"

    public NodoIncrementoDecremento(String nombre, String operador,
                                     int linea, int columna) {
        super(linea, columna);
        this.nombre   = nombre;
        this.operador = operador;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object val = entorno.obtener(nombre);
        if (val == Entorno.NO_ENCONTRADO) {
            throw new ErrorSemanticoException(
                "La variable '" + nombre + "' no está declarada.",
                linea, columna
            );
        }

        int delta = operador.equals("++") ? 1 : -1;

        if (val instanceof Integer) {
            entorno.asignar(nombre, (Integer) val + delta);
        } else if (val instanceof Double) {
            entorno.asignar(nombre, (Double) val + delta);
        } else {
            throw new ErrorSemanticoException(
                "Operador '" + operador + "' solo aplica a tipos numéricos. " +
                "La variable '" + nombre + "' es de tipo " + tipoDe(val) + ".",
                linea, columna
            );
        }
        return null;
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
        return indent(nivel) + "IncDec: " + nombre + operador + "\n";
    }
}