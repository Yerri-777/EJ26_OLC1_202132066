package ast;

import entorno.Entorno;
import errores.ErrorManager;
import java.util.List;

public class Nodoprintln {

    private final List<Nodo> expresiones;

    private final int linea;
    private final int columna;

    public Nodoprintln(List<Nodo> expresiones, int linea, int columna) {
        this.expresiones = expresiones;
        this.linea = linea;
        this.columna = columna;
    }


    public Object ejecutar(Entorno entorno) {
        StringBuilder salida = new StringBuilder();

        try {
            for (int i = 0; i < expresiones.size(); i++) {
                Nodo expr = expresiones.get(i);
                Object valor = expr.execute(entorno); 

                
                if (valor != null) {
                    salida.append(valor.toString());
                } else {
                    salida.append("nil"); 
                }

             
                if (i < expresiones.size() - 1) {
                    salida.append(" ");
                }
            }

    
            System.out.println(salida.toString());

        } catch (Exception e) {
            ErrorManager.getInstance().agregarSemantico(
                    "Error al ejecutar fmt.Println: " + e.getMessage(), linea, columna
            );
        }

        return null;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }
}
