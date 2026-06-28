package ast;

import entorno.Entorno;

public abstract class Nodo {

    protected final int linea;
    protected final int columna;

    protected Nodo(int linea, int columna) {
        this.linea   = linea;
        this.columna = columna;
    }

    public int getLinea()   { return linea;   }
    public int getColumna() { return columna; }

    // Ejecución de sentencias 
    public abstract Object execute(Entorno entorno);

  
    public Object getValue(Entorno entorno) {
        throw new UnsupportedOperationException("El nodo no retorna valor: " + this.getClass().getSimpleName());
    }

    
    public void assign(Entorno entorno, Object valor) {
        throw new UnsupportedOperationException("El nodo no admite asignación: " + this.getClass().getSimpleName());
    }

    
    
    public abstract String toAST(int nivel);

    protected String indent(int nivel) {
        return "  ".repeat(nivel);
    }
    }