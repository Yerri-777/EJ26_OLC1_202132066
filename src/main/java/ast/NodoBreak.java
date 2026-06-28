package ast;

import entorno.Entorno;
import excepciones.BreakException;


public class NodoBreak extends NodoSentencia {

    public NodoBreak(int linea, int columna) {
        super(linea, columna);
    }

    @Override
    public Object execute(Entorno entorno) {
        throw new BreakException(linea, columna);
    }

@Override
public String toAST(int nivel) {
    return indent(nivel) + "Break\n";
}}