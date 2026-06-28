package ast;
import entorno.Entorno;

public class NodoAccesoSlice extends Nodo {
    private final Nodo slice; // El objeto (ej. miSlice)
    private final Nodo indice; // El índice (ej. 0)

    public NodoAccesoSlice(Nodo slice, Nodo indice, int linea, int columna) {
        super(linea, columna);
        this.slice = slice;
        this.indice = indice;
    }

    @Override
    public Object execute(Entorno entorno) {
        
        return null; 
    }

@Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    // Reemplaza 'this.nombre' por el atributo que guarda el nombre del slice/variable
    sb.append(indent(nivel)).append("AccesoSlice: ").append(this.toAST(nivel)).append("\n");
    if (slice != null) sb.append(slice.toAST(nivel + 1));
    if (indice != null) sb.append(indice.toAST(nivel + 1));
    return sb.toString();
}}
