package excepciones;


public class ReturnException extends RuntimeException {

    private final Object valor;
    private final int linea;
    private final int columna;

   
    public ReturnException(Object valor, int linea, int columna) {
        super("return");
        this.valor = valor;
        this.linea = linea;
        this.columna = columna;
    }

  
    public ReturnException(Object valor) {
        this(valor, 0, 0);
    }

   
    public ReturnException() {
        this(null, 0, 0);
    }


    public Object getValor()    { return valor; }
    public int    getLinea()    { return linea; }
    public int    getColumna()  { return columna; }
    
    public boolean tieneValor() { return valor != null; }
}