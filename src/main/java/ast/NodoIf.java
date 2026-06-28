package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;

public class NodoIf extends NodoSentencia {

    private final NodoExpresion condicion;
    private final NodoBloque bloqueIf;
    private final NodoBloque bloqueElse;
    private final NodoIf elseIf;

    public NodoIf(NodoExpresion condicion, NodoBloque bloqueIf, 
                  NodoBloque bloqueElse, NodoIf elseIf, int linea, int columna) {
        super(linea, columna);
        this.condicion = condicion;
        this.bloqueIf = bloqueIf;
        this.bloqueElse = bloqueElse;
        this.elseIf = elseIf;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object valCond = condicion.getValue(entorno);
        if (!(valCond instanceof Boolean)) {
            throw new ErrorSemanticoException("La condición del 'if' debe ser bool.", linea, columna);
        }

        if ((Boolean) valCond) {
            bloqueIf.execute(entorno);
        } else if (elseIf != null) {
            elseIf.execute(entorno);
        } else if (bloqueElse != null) {
            bloqueElse.execute(entorno);
        }
        return null;
    }

 @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("If\n");
    if (condicion != null)  sb.append(condicion.toAST(nivel + 1));
    if (bloqueIf != null)   sb.append(bloqueIf.toAST(nivel + 1));
    if (elseIf != null)     sb.append(elseIf.toAST(nivel + 1));
    if (bloqueElse != null) sb.append(bloqueElse.toAST(nivel + 1));
    return sb.toString();
}
}