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
        //  Entorno local protegido para el ciclo
        Entorno entornoFor = new Entorno(entorno);
        if (inicializacion != null) {
            inicializacion.execute(entornoFor);
        }

        //  Bucle de ejecución infinita controlado por el AST
        while (true) {
            // Validación semántica obligatoria del PDF para la condición
            Object valCond = condicion.getValue(entornoFor);
            if (!(valCond instanceof Boolean)) {
                throw new ErrorSemanticoException("La condición del 'for' debe ser bool.", linea, columna);
            }
            
            // Si la condición evalúa a false, rompemos el ciclo inmediatamente
            if (!(Boolean) valCond) {
                break;
            }

            // Ejecución del cuerpo con captura de transferencias de control (break/continue)
            try {
                if (cuerpo != null) {
                    cuerpo.execute(entornoFor);
                }
            } catch (BreakException b) {
                break; // Detiene el while nativo por completo
            } catch (ContinueException c) {
                // Se ignora el resto del cuerpo y cae directo al incremento
            }

            // Ejecución del paso de incremento o actualización 
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