package ast;

import entorno.Entorno;
import java.util.ArrayList;
import java.util.List;

public class NodoLiteralArray extends NodoExpresion {

    private final List<Nodo> elementos;

    public NodoLiteralArray(List<Nodo> elementos, int linea, int columna) {
        super(linea, columna);
        this.elementos = (elementos != null) ? elementos : new ArrayList<>();
    }

    public NodoLiteralArray(List<Nodo> elementos) {
        this(elementos, 0, 0);
    }

    @Override
    public Object getValue(Entorno entorno) {
        List<Object> valoresEvaluados = new ArrayList<>();
        
        for (Nodo nodo : elementos) {
            if (nodo instanceof NodoExpresion) {
                NodoExpresion expr = (NodoExpresion) nodo;
                valoresEvaluados.add(expr.getValue(entorno));
            } else {
                // Mejora: Lanza una excepción clara en lugar de solo imprimir.
                // Esto detiene la ejecución si hay un error grave en la estructura.
                throw new RuntimeException("Error en línea " + getLinea() + ": El elemento del array no es una expresión válida.");
            }
        }
        return valoresEvaluados;
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("LiteralArray (").append(elementos.size()).append(" elementos):\n");
        
        for (Nodo n : elementos) {
            sb.append(n.toAST(nivel + 1));
        }
        return sb.toString();
    }

 
    protected String indent(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
}