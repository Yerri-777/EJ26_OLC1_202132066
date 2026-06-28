package ast;

import entorno.Entorno;

public class NodoIncrementoDecremento extends NodoSentencia {

    private final Nodo target;
    private final String operador; // "++" o "--"

    public NodoIncrementoDecremento(Nodo target, String operador, int linea, int columna) {
        super(linea, columna);
        this.target = target;
        this.operador = operador;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object val = target.getValue(entorno);
        if (val == null) {
            return null;
        }

        int delta = operador.equals("++") ? 1 : -1;
        Object resultado = null;

        if (val instanceof Integer) {
            resultado = (Integer) val + delta;
        } else if (val instanceof Double) {
            resultado = (Double) val + delta;
        }

        if (resultado != null) {
            target.assign(entorno, resultado);
        }
        return null;
    }

   @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("IncDec\n");
    sb.append(indent(nivel + 1)).append("Operador: ").append(operador).append("\n");
    if (target != null) {
        sb.append(target.toAST(nivel + 1));
    }
    return sb.toString();
}
}