package ast;

import entorno.Entorno;

public class NodoConversion extends NodoExpresion {
    private String tipoDestino;
    private NodoExpresion expresion;

    public NodoConversion(String tipoDestino, NodoExpresion expresion, int linea, int columna) {
        super(linea, columna);
        this.tipoDestino = tipoDestino;
        this.expresion = expresion;
    }

    public String getTipoDestino() {
        return tipoDestino;
    }

    public NodoExpresion getExpresion() {
        return expresion;
    }
    
   

    @Override
    public Object getValue(Entorno entorno) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

@Override
public String toAST(int nivel) {
    return indent(nivel) + "InstruccionEjecutable\n";
}
}