package ast;

import entorno.Entorno;

/**
 * Nodo para representar conversiones de tipo explícitas (casting)
 */
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
    
    // Si tus otros nodos tienen un método como 'ejecutar', 'evaluar' o 'getTipo',
    // agrégalo aquí siguiendo el mismo patrón, por ejemplo:
    // @Override
    // public Object ejecutar(Entorno e) { ... }

    @Override
    public Object getValue(Entorno entorno) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

@Override
public String toAST(int nivel) {
    return indent(nivel) + "InstruccionEjecutable\n";
}
}