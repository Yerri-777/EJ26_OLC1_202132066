package excepciones;


public class ErrorSemanticoException extends RuntimeException {

    private final String descripcion;
    private final int    linea;
    private final int    columna;

    public ErrorSemanticoException(String descripcion, int linea, int columna) {
     
        super("[Línea " + linea + ", Col " + columna + "] " + descripcion);
        this.descripcion = descripcion;
        this.linea       = linea;
        this.columna     = columna;
    }

    public String getDescripcion() { 
        return descripcion; 
    }

   
    public int getLinea() { 
        return linea; 
    }

   
    public int getLine() { 
        return linea; 
    }

    public int getColumna() { 
        return columna; 
    }

    @Override
    public String toString() {
        return getMessage();
    }
}