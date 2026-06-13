package errores;


public class Error {

    private final TipoError tipo;
    private final String    descripcion;
    private final int       linea;
    private final int       columna;

    /**
     * @param tipo        Fase del compilador donde se detectó
     * @param descripcion Mensaje descriptivo del error
     * @param linea       Línea en el archivo fuente
     * @param columna     Columna en el archivo fuente 
     */
    public Error(TipoError tipo, String descripcion, int linea, int columna) {
        this.tipo        = tipo;
        this.descripcion = descripcion;
        this.linea       = linea;
        this.columna     = columna;
    }

    // Getters 

    public TipoError getTipo()        { return tipo;        }
    public String    getDescripcion() { return descripcion; }
    public int       getLinea()       { return linea;       }
    public int       getColumna()     { return columna;     }

    // Representación 


    @Override
    public String toString() {
        return String.format("[ERROR %s] Línea %d, Col %d: %s",
                tipo.name(), linea, columna, descripcion);
    }

  
    public String toReportRow(int numero) {
        return String.format("| %-4d | %-45s | %-6d | %-7d | %-10s |",
                numero, descripcion, linea, columna, tipo.name().toLowerCase());
    }
}