package excepciones;


public class ContinueException extends RuntimeException {

    private final int linea;
    private final int columna;

    public ContinueException(int linea, int columna) {
        super("continue");
        this.linea   = linea;
        this.columna = columna;
    }

    public int getLinea()   { return linea;   }
    public int getColumna() { return columna; }
}