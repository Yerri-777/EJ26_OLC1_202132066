package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.ArrayList;
import java.util.List;


public class NodoLlamadaFuncion extends NodoExpresion {

    private final String              nombre;
    private final List<NodoExpresion> argumentos;

    public NodoLlamadaFuncion(String nombre,
                               List<NodoExpresion> argumentos,
                               int linea, int columna) {
        super(linea, columna);
        this.nombre     = nombre;
        this.argumentos = argumentos;
    }

    public String getNombre() { return nombre; }

    @Override
    public Object getValue(Entorno entorno) {
        NodoFuncion fn = entorno.buscarFuncion(nombre);
        if (fn == null) {
            throw new ErrorSemanticoException(
                "La función '" + nombre + "' no está declarada.",
                linea, columna
            );
        }

        // Evaluar argumentos en el entorno actual
        List<Object> valores = new ArrayList<>();
        for (NodoExpresion arg : argumentos) {
            valores.add(arg.getValue(entorno));
        }

        return fn.ejecutarCon(entorno, valores);
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("LlamadaFuncion: ").append(nombre).append("\n");
        for (NodoExpresion a : argumentos) {
            sb.append(a.toAST(nivel + 1));
        }
        return sb.toString();
    }
}