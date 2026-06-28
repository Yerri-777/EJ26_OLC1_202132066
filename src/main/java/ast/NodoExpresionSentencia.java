package ast;

import entorno.Entorno;


public class NodoExpresionSentencia extends NodoSentencia {

    private final NodoExpresion expresion;

    public NodoExpresionSentencia(NodoExpresion expresion, int linea, int columna) {
        super(linea, columna);
        this.expresion = expresion;
    }

    @Override
    public Object execute(Entorno entorno) {
        expresion.getValue(entorno);  
        return null;
    }

  @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("ExprSentencia\n");
    if (expresion != null) {
        sb.append(expresion.toAST(nivel + 1));
    }
    return sb.toString();
} }