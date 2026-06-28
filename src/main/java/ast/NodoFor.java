package ast;

import entorno.Entorno;
import excepciones.BreakException;
import excepciones.ContinueException;
import excepciones.ErrorSemanticoException;

public class NodoFor extends NodoSentencia {

    private final Nodo inicializacion; 
    private final NodoExpresion condicion;
    private final Nodo incremento;      
    private final NodoBloque cuerpo;

    public NodoFor(Nodo inicializacion, NodoExpresion condicion, 
                   Nodo incremento, NodoBloque cuerpo, int linea, int columna) {
        super(linea, columna);
        this.inicializacion = inicializacion;
        this.condicion = condicion;
        this.incremento = incremento;
        this.cuerpo = cuerpo;
    }

    @Override
    public Object execute(Entorno entorno) {
        Entorno entornoFor = new Entorno(entorno);
        
        // 1. Ejecutar inicialización si existe (ej. i:=0)
        if (inicializacion != null) {
            inicializacion.execute(entornoFor);
        }

        while (true) {
            // 2. Validación: Solo ejecutamos si hay condición y es true
            if (condicion != null) {
                Object valCond = condicion.getValue(entornoFor);
                if (!(valCond instanceof Boolean)) {
                    throw new ErrorSemanticoException("La condición del 'for' debe ser booleana.", linea, columna);
                }
                if (!(Boolean) valCond) {
                    break; // Salir del ciclo
                }
            }

            // 3. Ejecución del cuerpo con control de excepciones
            try {
                if (cuerpo != null) {
                    cuerpo.execute(entornoFor);
                }
            } catch (BreakException b) {
                break; // Rompe el bucle
            } catch (ContinueException c) {
                // Se ignora el resto del cuerpo, salta al incremento
            }

            // 4. Ejecución del incremento (ej. i++)
            if (incremento != null) {
                incremento.execute(entornoFor);
            }
        }
        return null;
    }

   @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("For\n");
    if (inicializacion != null) sb.append(inicializacion.toAST(nivel + 1));
    if (condicion != null)      sb.append(condicion.toAST(nivel + 1));
    if (incremento != null)     sb.append(incremento.toAST(nivel + 1));
    if (cuerpo != null)         sb.append(cuerpo.toAST(nivel + 1));
    return sb.toString();
}
}