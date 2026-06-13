package excepciones;


 // BreakException
 
  //Se lanza cuando el intérprete encuentra una sentencia "break".
 // El NodoFor la atrapa para terminar el bucle.
  //El NodoSwitch la atrapa para terminar su bloque.

public class BreakException extends RuntimeException {

    private final int linea;
    private final int columna;

    public BreakException(int linea, int columna) {
        super("break");
        this.linea   = linea;
        this.columna = columna;
    }

    public int getLinea()   { return linea;   }
    public int getColumna() { return columna; }
}