package ast;

import entorno.Entorno;
import java.util.List;
import java.util.ArrayList;

public class NodoAppend extends NodoExpresion {
    private final Nodo slice;
    private final Nodo elemento;

    public NodoAppend(Nodo slice, Nodo elemento, int linea, int columna) {
        super(linea, columna);
        this.slice = slice;
        this.elemento = elemento;
    }

    @Override
    public Object execute(Entorno entorno) {
        // 1. Ejecutar el nodo del slice
        Object valorSlice = slice.execute(entorno);
        
        // 2. Validación de seguridad (Previene crashes)
        if (!(valorSlice instanceof List<?>)) {
            System.err.println("Error en línea " + linea + ": El primer argumento de append debe ser un slice/lista.");
            return null; // O lanza una excepción propia de tu compilador
        }

        // 3. Obtener el elemento a insertar
        Object valorElemento = elemento.execute(entorno);

        // 4. Realizar la operación
        @SuppressWarnings("unchecked")
        List<Object> lista = (List<Object>) valorSlice;
        lista.add(valorElemento);
        
        return lista;
    }

    @Override
    public Object getValue(Entorno entorno) {
        // Un append no es una constante, pero debe ejecutar su lógica
        return execute(entorno);
    }

 @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Append (Llamada Función)\n");
    if (slice != null) sb.append(slice.toAST(nivel + 1));
    if (elemento != null) sb.append(elemento.toAST(nivel + 1));
    return sb.toString();
} 
}