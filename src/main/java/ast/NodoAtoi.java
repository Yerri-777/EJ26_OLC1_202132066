package ast;

import entorno.Entorno;
import errores.ErrorManager;

/**
 * Función embebida: strconv.Atoi(cadena)
 */
public class NodoAtoi { 
    
    private final Object expresion;
    private final int linea;
    private final int columna;

    public NodoAtoi(Object expresion, int linea, int columna) {
        this.expresion = expresion;
        this.linea = linea;
        this.columna = columna;
    }

    public Object resolver(Entorno env) {
    
        Object valor = null; 

        if (valor == null) return 0;

        if (valor instanceof String) {
            try {
                return Integer.parseInt((String) valor);
            } catch (NumberFormatException e) {
                ErrorManager.getInstance().agregarSemantico(
                    "strconv.Atoi: La cadena '" + valor + "' no es un entero válido.", 
                    linea, columna
                );
                return 0; // Tolerancia a fallos
            }
        } else {
            ErrorManager.getInstance().agregarSemantico(
                "strconv.Atoi requiere un argumento de tipo string.", 
                linea, columna
            );
            return 0;
        }
    }
}