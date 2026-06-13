package ast;

import entorno.Entorno;
import errores.ErrorManager;


public class NodoParseFloat { 
    
    
    private final Object expresion; 
    private final int linea;
    private final int columna;

    public NodoParseFloat(Object expresion, int linea, int columna) {
        this.expresion = expresion;
        this.linea = linea;
        this.columna = columna;
    }

    // Método resolver o evaluate 
    public Object resolver(Entorno env) {
      
        Object valor = null; 

        if (valor == null) return 0.0;

        if (valor instanceof String) {
            try {
                return Double.parseDouble((String) valor);
            } catch (NumberFormatException e) {
                ErrorManager.getInstance().agregarSemantico(
                    "strconv.ParseFloat: La cadena '" + valor + "' no tiene un formato numérico válido.", 
                    linea, columna
                );
                return 0.0; 
            }
        } else {
            ErrorManager.getInstance().agregarSemantico(
                "strconv.ParseFloat requiere un argumento de tipo string.", 
                linea, columna
            );
            return 0.0;
        }
    }
}