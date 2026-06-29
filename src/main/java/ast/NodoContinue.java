package ast;

import entorno.Entorno;
import excepciones.ContinueException;



public class NodoContinue extends NodoSentencia {

    public NodoContinue(int linea, int columna) {
        super(linea, columna);
    }

    @Override
    public Object execute(Entorno entorno) {
        throw new ContinueException(linea, columna);
    }

  @Override
    public String toAST(int nivel) {
        return indent(nivel) + "Continue\n";
    }
}