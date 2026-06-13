package tokens;


public class Token {

    private final TipoToken tipo;
    private final String    lexema;
    private final int       linea;
    private final int       columna;

    
     // @param tipo    Tipo del token según TipoToken
     //@param lexema  Texto exacto reconocido en el fuente
     //@param linea   Número de línea (base 1)
     //@param columna Número de columna (base 1)
     
    public Token(TipoToken tipo, String lexema, int linea, int columna) {
        this.tipo    = tipo;
        this.lexema  = lexema;
        this.linea   = linea;
        this.columna = columna;
    }

    // Getters 

    public TipoToken getTipo()    { return tipo;    }
    public String    getLexema()  { return lexema;  }
    public int       getLinea()   { return linea;   }
    public int       getColumna() { return columna; }

    // Representación textual 

   
    @Override
    public String toString() {
        return String.format("[%-25s | %-20s | L:%-4d C:%-4d]",
                tipo.name(), "\"" + lexema + "\"", linea, columna);
    }

    
      //Formato compacto para la tabla de tokens del reporte.
     // Columnas: Lexema | Tipo | Línea | Columna
     
    public String toReportRow(int numero) {
        return String.format("| %-4d | %-25s | %-15s | %-6d | %-7d |",
                numero, lexema, tipo.name(), linea, columna);
    }
}