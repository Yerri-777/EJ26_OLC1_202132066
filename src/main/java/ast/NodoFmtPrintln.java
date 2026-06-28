package ast;

import entorno.Entorno;
import java.util.List;


public class NodoFmtPrintln extends NodoExpresion {

    private final List<NodoExpresion> argumentos;

    public NodoFmtPrintln(List<NodoExpresion> argumentos, int linea, int columna) {
        super(linea, columna);
        this.argumentos = argumentos;
    }

    @Override
    public Object getValue(Entorno entorno) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) sb.append(" ");
            Object val = argumentos.get(i).getValue(entorno);
            sb.append(formatearValor(val));
        }
        sb.append("\n");

        entorno.agregarSalida(sb.toString());
        return null;
    }

 
    private String formatearValor(Object val) {
        if (val == null)            return "nil";
        if (val instanceof Boolean) return val.toString();           // "true" / "false"
        if (val instanceof Integer) return val.toString();           // int y rune
        if (val instanceof Double)  return formatearDouble((Double) val);
        if (val instanceof String)  return (String) val;
        // Listas (slices Fase 2) → [v1 v2 v3]
        if (val instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> lista = (List<?>) val;
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(formatearValor(lista.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return val.toString();
    }

    private String formatearDouble(Double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) return d.toString();
        if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            long l = d.longValue();
            return Long.toString(l);
        }
        // Eliminar trailing zeros innecesarios
        String s = Double.toString(d);
        if (s.contains("E") || s.contains("e")) return s;
       
        return s;
    }

@Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("FmtPrintln\n");
    if (argumentos != null) {
        for (NodoExpresion a : argumentos) {
            if (a != null) {
                sb.append(a.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
}
}