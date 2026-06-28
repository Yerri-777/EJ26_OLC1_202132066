package ast;

import entorno.Entorno;
import java.util.List;

public class NodoLen extends NodoExpresion {
    private Nodo expresion;

    public NodoLen(Nodo expresion, int linea, int columna) {
        super(linea, columna);
        this.expresion = expresion;
    }

    @Override
    public Object execute(Entorno entorno) {
        return getValue(entorno);
    }

    @Override
    public Object getValue(Entorno entorno) {
        // Obtenemos el valor de la expresión
        Object val = expresion.execute(entorno);

        // 1. Validamos tipos permitidos (String y Slices/Arrays)
        if (val instanceof String) {
            return ((String) val).length();
        } 
        
        if (val instanceof List) {
            return ((List<?>) val).size();
        }

        // 2. ERROR SEMÁNTICO: Si llega aquí, es porque el tipo no es válido
        // Ya no devolvemos 0. Lanzamos un error o notificamos al sistema.
        String tipoRecibido = (val != null) ? val.getClass().getSimpleName() : "null";
        String mensaje = "Error semántico en línea " + this.linea + ", columna " + this.columna + 
                         ": La función 'len()' no puede aplicarse a un tipo '" + tipoRecibido + "'.";
        
        // Aquí deberías integrar con tu manejador de errores global
        // Ejemplo: ManejadorErrores.getInstance().agregar(mensaje);
        
        throw new RuntimeException(mensaje); // Detenemos la ejecución para no propagar datos erróneos
    }

    @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Len\n");
    if (expresion != null) {
        sb.append(expresion.toAST(nivel + 1));
    }
    return sb.toString();
}
}