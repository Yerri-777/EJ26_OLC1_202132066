package entorno;


 
   // ID | Tipo símbolo | Tipo dato | Ámbito | Línea | Columna
  //TipoSimbolo:
   // VARIABLE   declarada con var o :=
    //FUNCION    declarada con func
  //PARAMETRO parámetro formal de función

public class Simbolo {

    public enum TipoSimbolo { VARIABLE, FUNCION, PARAMETRO }

    //  Campos del reporte 
    private final String      nombre;
    private final TipoSimbolo tipoSimbolo;
    private final String      tipoDato;    // "int", "float64", "string", "bool", "rune", nombre struct
    private final String      ambito;      // "Global", nombre de función, "Bloque"
    private final int         linea;
    private final int         columna;

    // Valor en tiempo de ejecución 
    private Object valor;

    public Simbolo(String nombre, TipoSimbolo tipoSimbolo, String tipoDato,
                   String ambito, int linea, int columna, Object valor) {
        this.nombre      = nombre;
        this.tipoSimbolo = tipoSimbolo;
        this.tipoDato    = tipoDato;
        this.ambito      = ambito;
        this.linea       = linea;
        this.columna     = columna;
        this.valor       = valor;
    }

    // Getters
    public String      getNombre()      { return nombre;      }
    public TipoSimbolo getTipoSimbolo() { return tipoSimbolo; }
    public String      getTipoDato()    { return tipoDato;    }
    public String      getAmbito()      { return ambito;      }
    public int         getLinea()       { return linea;       }
    public int         getColumna()     { return columna;     }
    public Object      getValor()       { return valor;       }

    public void setValor(Object valor)  { this.valor = valor; }

    //  Formato para el reporte
    public String toReportRow(int numero) {
        return String.format("| %-4d | %-20s | %-10s | %-12s | %-15s | %-6d | %-7d |",
                numero,
                nombre,
                tipoSimbolo.name().toLowerCase(),
                tipoDato,
                ambito,
                linea,
                columna);
    }
}