package ast;

import entorno.Entorno;
import excepciones.BreakException;
import java.util.ArrayList;
import java.util.List;

public class NodoSwitch extends Nodo {

    private Nodo inicializacion; // Puede ser null
    private Nodo expresion;      // Puede ser null
    private List<Nodo> casos;

    // CONSTRUCTOR CORREGIDO
    public NodoSwitch(Nodo inicializacion, Nodo expresion, List<Nodo> casos, int linea, int columna) {
        super(linea, columna);
        this.inicializacion = inicializacion;
        this.expresion = expresion;
        this.casos = casos;
    }

    @Override
    public Object execute(Entorno entorno) {
        Entorno entornoSwitch = new Entorno(entorno);

        if (inicializacion != null) {
            inicializacion.execute(entornoSwitch);
        }

        Object valorSwitch = null;
        if (expresion != null) {
            valorSwitch = (expresion instanceof NodoExpresion)
                    ? ((NodoExpresion) expresion).getValue(entornoSwitch)
                    : expresion.execute(entornoSwitch);
        }

        for (Nodo n : casos) {
            NodoCase c = (NodoCase) n;
            boolean esDefault = (c.getCondicion() == null);
            boolean coincide = false;

            if (!esDefault && expresion != null) {
                Object valorCase = ((NodoExpresion) c.getCondicion()).getValue(entornoSwitch);
                // Asegúrate de que valorSwitch no sea null antes de comparar
                if (valorSwitch != null && valorSwitch.equals(valorCase)) {
                    coincide = true;
                }
            }

            if (esDefault || coincide) {
                try {
                    c.execute(entornoSwitch);
                    break; 
                } catch (BreakException e) {
                    break; 
                }
            }
        }
        return null;
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("Switch\n");
        if (inicializacion != null) sb.append(inicializacion.toAST(nivel + 1));
        if (expresion != null)      sb.append(expresion.toAST(nivel + 1));
        if (casos != null) {
            for (Nodo c : casos) {
                if (c != null) sb.append(c.toAST(nivel + 1));
            }
        }
        return sb.toString();
    }
}