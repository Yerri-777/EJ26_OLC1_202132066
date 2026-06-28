package ast;
import entorno.Entorno;
import java.util.ArrayList;
import java.util.List;

public class NodoLiteralSlice extends NodoExpresion {
    private final String tipo;
    private final List<NodoExpresion> elementos;

    public NodoLiteralSlice(String tipo, List<NodoExpresion> elementos, int linea, int columna) {
        super(linea, columna);
        this.tipo = tipo;
        this.elementos = elementos;
    }

    /**
     * Al ser un NodoExpresion, este método debe retornar el valor resultante.
     * En este caso, retorna la lista de valores ya evaluados.
     */
    @Override
    public Object execute(Entorno entorno) {
        return getValue(entorno);
    }

    @Override
    public Object getValue(Entorno entorno) {
        // Creamos la lista que representará nuestro Slice en memoria (durante la ejecución)
        List<Object> listaValores = new ArrayList<>();
        
        for (NodoExpresion nodo : elementos) {
            // Evaluamos cada elemento para obtener su valor real (int, float, etc.)
            Object valor = nodo.execute(entorno);
            listaValores.add(valor);
        }
        
        // Retornamos la estructura poblada. 
        // Nota: En tu entorno, el tipo del objeto retornado será tu clase 'Slice' o 'ArrayList'
        return listaValores;
    }

    @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("LiteralSlice\n");
    if (tipo != null) {
        sb.append(indent(nivel + 1)).append("Tipo: ").append(tipo).append("\n");
    }
    if (elementos != null) {
        for (NodoExpresion n : elementos) {
            if (n != null) {
                sb.append(n.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
} }