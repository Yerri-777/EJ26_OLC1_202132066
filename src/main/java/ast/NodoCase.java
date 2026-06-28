package ast;

import entorno.Entorno;

public class NodoCase extends Nodo {
    private final Object condicion; // Puede ser null si es default
    private final NodoBloque cuerpo;

    public NodoCase(Object condicion, NodoBloque cuerpo, int linea, int columna) {
        super(linea, columna);
        this.condicion = condicion;
        this.cuerpo = cuerpo;
    }

    public boolean esDefault() {
        return condicion == null;
    }

    public Object getCondicion() {
        return condicion;
    }

    @Override
    public Object execute(Entorno entorno) {
        return cuerpo.execute(entorno);
    }

   @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    if (esDefault()) {
        sb.append(indent(nivel)).append("Default\n");
    } else {
        sb.append(indent(nivel)).append("Case\n");
        if (condicion != null) {
            sb.append(condicion.equals(nivel + 1));
        }
    }
    if (cuerpo != null) {
        sb.append(cuerpo.toAST(nivel + 1));
    }
    return sb.toString();
}}