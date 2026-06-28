package ast;

import entorno.Entorno;
import errores.ErrorManager;

public class NodoIdentificador extends NodoExpresion {

    private final String nombre;

    public NodoIdentificador(String nombre, int linea, int columna) {
        super(linea, columna);
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }

    @Override
    public Object getValue(Entorno entorno) {
        Object val = entorno.obtener(nombre);
        
        if (val == Entorno.NO_ENCONTRADO) {
            ErrorManager.getInstance().agregarSemantico(
                "La variable '" + nombre + "' no está declarada en este ámbito.",
                linea, columna
            );
            return null; 
        }
        
        return val;
    }

    // --- NUEVO MÉTODO IMPLEMENTADO ---
    @Override
    public void assign(Entorno entorno, Object valor) {
        // Verificamos antes de asignar
        if (entorno.obtener(nombre) == Entorno.NO_ENCONTRADO) {
            ErrorManager.getInstance().agregarSemantico(
                "La variable '" + nombre + "' no está declarada. No se puede asignar.",
                linea, columna
            );
            return;
        }
        
        // Ejecutamos la asignación en el entorno
        entorno.asignar(nombre, valor);
    }

    @Override
public String toAST(int nivel) {
    return indent(nivel) + "Identificador: " + nombre + "\n";
}
}