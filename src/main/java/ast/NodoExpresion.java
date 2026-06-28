package ast;

import entorno.Entorno;


public abstract class NodoExpresion extends Nodo {

    protected NodoExpresion(int linea, int columna) {
        super(linea, columna);
    }

  
    public abstract Object getValue(Entorno entorno);

    @Override
    public Object execute(Entorno entorno) {
        return getValue(entorno);
    }

    @Override
public String toAST(int nivel) {
    return indent(nivel) + "EvaluacionExpresion\n";
}
}