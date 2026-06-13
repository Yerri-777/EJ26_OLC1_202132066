package ast;

import entorno.Entorno;


public class NodoTipo extends Nodo {

    private final String nombre;

    public NodoTipo(String nombre, int linea, int columna) {
        super(linea, columna);
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }

    /** Retorna el valor por defecto del tipo según el PDF. */
    public Object valorPorDefecto() {
        switch (nombre) {
            case "int":     return 0;
            case "float64": return 0.0;
            case "string":  return "";
            case "bool":    return false;
            case "rune":    return 0;
            default:        return null;  
// nil para tipos compuestos
        }
    }

    @Override
    public Object execute(Entorno entorno) { return nombre; }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "Tipo: " + nombre + "\n";
    }
}