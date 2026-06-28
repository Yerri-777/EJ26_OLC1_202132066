package ast;

import entorno.Entorno;


public class NodoLiteral extends NodoExpresion {

    private final Object valor;
    private final String tipo;

    public NodoLiteral(Object valor, String tipo, int linea, int columna) {
        super(linea, columna);
        this.valor = valor;
        this.tipo  = tipo;
    }

    public Object getValor() { return valor; }
    public String getTipo()  { return tipo;  }

    @Override
    public Object getValue(Entorno entorno) {
        return valor;
    }

   @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Literal\n");
    sb.append(indent(nivel + 1)).append("Tipo: ").append(tipo).append("\n");
    sb.append(indent(nivel + 1)).append("Valor: ").append(valor).append("\n");
    return sb.toString();
}
}