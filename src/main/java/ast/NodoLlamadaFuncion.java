package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import excepciones.ReturnException;
import java.util.ArrayList;
import java.util.List;

public class NodoLlamadaFuncion extends NodoExpresion {

    private final String nombre;
    private final List<NodoExpresion> argumentos;

    public NodoLlamadaFuncion(String nombre,
                              List<NodoExpresion> argumentos,
                              int linea, int columna) {
        super(linea, columna);
        this.nombre = nombre;
        this.argumentos = argumentos;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public Object getValue(Entorno entorno) {
        NodoFuncion fn = entorno.buscarFuncion(nombre);

        if (fn == null) {
            throw new ErrorSemanticoException(
                "La función '" + nombre + "' no está declarada.",
                linea,
                columna
            );
        }

        // Evaluar argumentos en el entorno actual
        List<Object> valores = new ArrayList<>();
        for (NodoExpresion arg : argumentos) {
            valores.add(arg.getValue(entorno));
        }

        try {
            return fn.ejecutarCon(entorno, valores);
        } catch (ReturnException e) {
            // Recupera el valor propagado por un nodo return
            return e.getValor();
        }
    }

  @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("LlamadaFuncion\n");
    sb.append(indent(nivel + 1)).append("Funcion: ").append(nombre).append("\n");
    if (argumentos != null) {
        for (NodoExpresion a : argumentos) {
            if (a != null) {
                sb.append(a.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
} }
