package ast;


public class NodoParametro {
    
    private final String nombre;
    private final TipoWrapper tipo;
    private final int linea;
    private final int columna;

    public NodoParametro(String nombre, String tipo, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = new TipoWrapper(tipo);
        this.linea = linea;
        this.columna = columna;
    }

    //  Getters 
    public String getNombre() {
        return nombre;
    }

    public TipoWrapper getTipo() {
        return tipo;
    }

    public String getTipoString() {
        return tipo.getNombre();
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    //Generación de fila para la Tabla de Símbolos 
    public entorno.Simbolo toSimbolo(String ambitoFuncion) {
        return new entorno.Simbolo(
            this.nombre,
            entorno.Simbolo.TipoSimbolo.PARAMETRO,
            this.tipo.getNombre(),
            ambitoFuncion,
            this.linea,
            this.columna,
            null
        );
    }

    // Representación en el Árbol AST (Requerido por NodoFuncion)
 
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel))
          .append("Parametro: ")
          .append(nombre)
          .append(" (")
          .append(tipo.getNombre())
          .append(")\n");
        return sb.toString();
    }

  
    private String indent(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("  "); 
// Dos espacios por nivel de profundidad
        }
        return sb.toString();
    }

    //Clase Envolvente Interna para Compatibilidad de Tipos 
    public static class TipoWrapper {
        private final String nombreTipo;

        public TipoWrapper(String nombreTipo) {
            this.nombreTipo = nombreTipo;
        }

        public String getNombre() {
            return nombreTipo;
        }

        @Override
        public String toString() {
            return nombreTipo;
        }
    }
}