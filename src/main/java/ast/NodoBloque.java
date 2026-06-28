package ast;

import entorno.Entorno;
import java.util.List;

public class NodoBloque extends NodoSentencia {

    private final List<Nodo> sentencias;

    public NodoBloque(List<Nodo> sentencias, int linea, int columna) {
        super(linea, columna);
        // Evitamos nulos, si viene vacío dejamos una lista vacía
        this.sentencias = sentencias;
    }

    @Override
    public Object execute(Entorno entornoParent) {
        if (sentencias == null) return null;

        Entorno entornoLocal = new Entorno(entornoParent);

        for (Nodo n : sentencias) {
            if (n != null) {
                n.execute(entornoLocal);
            }
        }
        return null;
    }

@Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Bloque\n");
    if (sentencias != null) {
        for (Nodo n : sentencias) {
            if (n != null) {
                sb.append(n.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
} }