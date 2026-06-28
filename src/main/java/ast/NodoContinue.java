package ast;

import entorno.Entorno;
import excepciones.ContinueException;


 // Lanza ContinueException que solo NodoFor puede capturar
 //Si se usa fuera de un for  error semántico
 
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