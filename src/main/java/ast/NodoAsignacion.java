package ast;

import entorno.Entorno;
import errores.ErrorManager;

/**
 * NodoAsignacion — id = expr
 * Permite conversión implícita int → float64.
 */
public class NodoAsignacion extends NodoSentencia {

    private final String        nombre;
    private final NodoExpresion expresion;

    public NodoAsignacion(String nombre, NodoExpresion expresion, int linea, int columna) {
        super(linea, columna);
        this.nombre    = nombre;
        this.expresion = expresion;
    }

    // Getters 
    
    public String getNombre() {
        return nombre;
    }

    public NodoExpresion getExpresion() {
        return expresion;
    }

    // Ejecución 

    @Override
    public Object execute(Entorno entorno) {
        // Verificar que la variable exista
        if (entorno.obtener(nombre) == Entorno.NO_ENCONTRADO) {
            ErrorManager.getInstance().agregarSemantico(
                "La variable '" + nombre + "' no está declarada en este ámbito.",
                linea, columna
            );
             // Fallback seguro, abortar asignación
            return null;
        }

        Object nuevoValor = expresion.getValue(entorno);
        
        // Si la expresión falló 
        if (nuevoValor == null) return null;

        String tipoActual = entorno.obtenerTipo(nombre);
        String tipoNuevo  = inferirTipo(nuevoValor);

        // Conversión implícita int float64
        if ("float64".equals(tipoActual) && "int".equals(tipoNuevo)) {
            nuevoValor = ((Integer) nuevoValor).doubleValue();
            tipoNuevo  = "float64";
        }

        if (!tipoActual.equals(tipoNuevo)) {
            ErrorManager.getInstance().agregarSemantico(
                "No se puede asignar un valor de tipo '" + tipoNuevo +
                "' a la variable '" + nombre + "' de tipo '" + tipoActual + "'.",
                linea, columna
            );
            return null; // Fallback seguro, abortar asignación
        }

        entorno.asignar(nombre, nuevoValor);
        return null;
    }

    private String inferirTipo(Object v) {
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "float64";
        if (v instanceof String)  return "string";
        if (v instanceof Boolean) return "bool";
        return "nil";
    }

    @Override
    public String toAST(int nivel) {
        return indent(nivel) + "Asignacion: " + nombre + "\n" +
               expresion.toAST(nivel + 1);
    }
}