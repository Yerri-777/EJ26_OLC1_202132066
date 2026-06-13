package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;

public class NodoStrconvParseFloat extends NodoExpresion {

    private final NodoExpresion argumento;

    public NodoStrconvParseFloat(NodoExpresion argumento, int linea, int columna) {
        super(linea, columna);
        this.argumento = argumento;
    }

    @Override
    public Object getValue(Entorno entorno) {
        Object val = argumento.getValue(entorno);

        if (!(val instanceof String)) {
            throw new ErrorSemanticoException(
                "strconv.ParseFloat espera un argumento de tipo string. " +
                "Se recibió: " + tipoDe(val) + ".",
                linea, columna
            );
        }

        String s = (String) val;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new ErrorSemanticoException(
                "strconv.ParseFloat: no se puede convertir \"" + s + "\" a float64.",
                linea, columna
            );
        }
    }

    private String tipoDe(Object v) {
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "float64";
        if (v instanceof Boolean) return "bool";
        if (v instanceof String)  return "string";
        return "nil";
    }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "StrconvParseFloat\n" + argumento.toAST(nivel + 1);
    }
}