package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.HashMap;
import java.util.List;

public class NodoAccesoStruct extends NodoExpresion {
    private final Nodo objeto; 
    private final String campo;      // Usado solo si es Struct
    private final Nodo expresion;    // Usado solo si es Array (el índice)
    private final boolean esArray;   // Bandera para saber qué ejecutar

    // Constructor para Struct: objeto.campo
    public NodoAccesoStruct(Nodo objeto, String campo, int linea, int col) {
        super(linea, col);
        this.objeto = objeto;
        this.campo = campo;
        this.expresion = null;
        this.esArray = false;
    }

    // Constructor para Array: objeto[expresion]
    public NodoAccesoStruct(Nodo objeto, Nodo expresion, int linea, int col) {
        super(linea, col);
        this.objeto = objeto;
        this.campo = null;
        this.expresion = expresion;
        this.esArray = true;
    }
    
    @Override
    public Object getValue(Entorno entorno) {
        return execute(entorno);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object execute(Entorno entorno) {
        Object obj = objeto.execute(entorno);
        
        if (esArray) {
            // LÓGICA PARA ARRAYS 
            if (obj instanceof List) {
                Object idxObj = expresion.getValue(entorno);
                if (!(idxObj instanceof Integer)) {
                    throw new ErrorSemanticoException("El índice del arreglo debe ser de tipo int.", linea, columna);
                }
                
                int idx = (Integer) idxObj;
                List<Object> lista = (List<Object>) obj;
                
                if (idx < 0 || idx >= lista.size()) {
                    throw new ErrorSemanticoException("Índice fuera de límites: " + idx + " (Tamaño: " + lista.size() + ")", linea, columna);
                }
                return lista.get(idx);
            }
            throw new ErrorSemanticoException("El identificador no es un arreglo válido.", linea, columna);
            
        } else {
            // LÓGICA PARA STRUCTS
            if (obj instanceof HashMap) {
                return ((HashMap<String, Object>) obj).get(campo);
            }
            throw new ErrorSemanticoException("El identificador no es un Struct válido.", linea, columna);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void assign(Entorno entorno, Object valor) {
        Object obj = objeto.execute(entorno);

        if (esArray) {
            // ASIGNACIÓN PARA ARRAYS COMPLETA
            if (obj instanceof List) {
                Object idxObj = expresion.getValue(entorno);
                if (!(idxObj instanceof Integer)) {
                    throw new ErrorSemanticoException("El índice debe ser de tipo int.", linea, columna);
                }
                
                int idx = (Integer) idxObj;
                List<Object> lista = (List<Object>) obj;

                if (idx < 0 || idx >= lista.size()) {
                    throw new ErrorSemanticoException("Índice fuera de límites al asignar: " + idx, linea, columna);
                }
                lista.set(idx, valor); 
            } else {
                throw new ErrorSemanticoException("El objeto no es un arreglo (slice).", linea, columna);
            }
        } else {
            // ASIGNACIÓN PARA STRUCTS
            if (obj instanceof HashMap) {
                ((HashMap<String, Object>) obj).put(campo, valor); 
            } else {
                throw new ErrorSemanticoException("Error: El objeto no es un Struct válido.", linea, columna);
            }
        }
    }

@Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        
        if (esArray) {
            sb.append(indent(nivel)).append("AccesoArray\n");
            if (objeto != null) {
                sb.append(objeto.toAST(nivel + 1));
            }
            sb.append(indent(nivel + 1)).append("Indice\n");
            if (expresion != null) {
                sb.append(expresion.toAST(nivel + 2));
            } else {
                sb.append(indent(nivel + 2)).append("null\n");
            }
        } else {
            sb.append(indent(nivel)).append("AccesoStruct\n");
            sb.append(indent(nivel + 1)).append("Campo: ").append(campo).append("\n");
            if (objeto != null) {
                sb.append(objeto.toAST(nivel + 1));
            }
        }
        
        return sb.toString();
    }
}