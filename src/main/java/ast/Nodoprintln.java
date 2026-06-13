package ast;

import entorno.Entorno;
import errores.ErrorManager;
import java.util.List;


public class Nodoprintln  {

    private final List<Nodo> expresiones; 
// Puede recibir múltiples argumentos: fmt.Println("A", 1, true)
    private final int linea;
    private final int columna;

    public Nodoprintln(List<Nodo> expresiones, int linea, int columna) {
        this.expresiones = expresiones;
        this.linea = linea;
        this.columna = columna;
    }

    // @Override
    public Object ejecutar(Entorno entorno) {
        StringBuilder salida = new StringBuilder();

        try {
            for (int i = 0; i < expresiones.size(); i++) {
                Nodo expr = expresiones.get(i);
                Object valor = expr.execute(entorno); // Evaluamos cada expresión
                
                // Si la expresión falla (ej. variable no declarada), valor será null
                if (valor != null) {
                    salida.append(valor.toString());
                } else {
                    salida.append("nil"); // Representación de nulo en GoLite
                }

                // Agregamos un espacio entre argumentos, como lo hace Go
                if (i < expresiones.size() - 1) {
                    salida.append(" ");
                }
            }
            
            // Aquí deberías concatenar esto a tu consola principal
            
            System.out.println(salida.toString()); 

        } catch (Exception e) {
            ErrorManager.getInstance().agregarSemantico(
                "Error al ejecutar fmt.Println: " + e.getMessage(), linea, columna
            );
        }

        return null; // Println no retorna ningún valor en GoLite
    }

    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
}