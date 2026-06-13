package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.List;


public class NodoReflectTypeOf extends NodoExpresion {

    private final NodoExpresion argumento;

    public NodoReflectTypeOf(NodoExpresion argumento, int linea, int columna) {
        super(linea, columna);
        this.argumento = argumento;
    }

    @Override
    public Object getValue(Entorno entorno) {
        Object val = argumento.getValue(entorno);
        return tipoDe(val);
    }

    private String tipoDe(Object v) {
        if (v == null)            return "nil";
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "float64";
        if (v instanceof String)  return "string";
        if (v instanceof Boolean) return "bool";
        if (v instanceof List)    return "[]interface";   // slices Fase 2
        return v.getClass().getSimpleName();
    }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "ReflectTypeOf\n" + argumento.toAST(nivel + 1);
    }
}