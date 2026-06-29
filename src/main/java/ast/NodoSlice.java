package ast;

import entorno.Entorno;
import java.util.ArrayList;
import java.util.List;

public class NodoSlice extends NodoExpresion {
    private final List<Nodo> elementos;
    private final String tipo;

    public NodoSlice(String tipo, List<Nodo> elementos, int linea, int columna) {
        super(linea, columna);
        this.tipo = tipo; 
        this.elementos = elementos;
    }

    @Override
    public Object execute(Entorno entorno) {
        List<Object> lista = new ArrayList<>();
        for (Nodo n : elementos) {
       
            lista.add(n instanceof NodoExpresion ? ((NodoExpresion) n).getValue(entorno) : n.execute(entorno));
        }
        return lista;
    }


    @Override
    public Object getValue(Entorno entorno) {
        return execute(entorno);
    }

@Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Slice\n");
    if (tipo != null) {
        sb.append(indent(nivel + 1)).append("Tipo: ").append(tipo).append("\n");
    }
    if (elementos != null) {
        for (Nodo n : elementos) {
            if (n != null) {
                sb.append(n.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
} }