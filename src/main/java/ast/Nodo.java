package ast;

import entorno.Entorno;

/**
 * Nodo Clase abstracta raíz de toda la jerarquía del AST GoLite.
 *   Herencia: todos los nodos extienden esta clase
 *  Polimorfismo: execute() y getValue() se sobreescriben en cada subclase
 *   Encapsulamiento: línea y columna son protected para subclases
 */
public abstract class Nodo {

    protected final int linea;
    protected final int columna;

    protected Nodo(int linea, int columna) {
        this.linea   = linea;
        this.columna = columna;
    }

    public int getLinea()   { return linea;   }
    public int getColumna() { return columna; }

   
     
    public abstract Object execute(Entorno entorno);

  
    public abstract String toAST(int nivel);

    // Utilidad para indentación del AST

    protected String indent(int nivel) {
        return "  ".repeat(nivel);
    }
}