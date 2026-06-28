package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import errores.ErrorManager;
import java.util.List;

public class NodoPrograma extends Nodo {

    private final List<Nodo> declaraciones;

    public NodoPrograma(List<Nodo> declaraciones, int linea, int columna) {
        super(linea, columna);
        this.declaraciones = declaraciones;
    }

    /**
     *  Registro de todas las funciones globales.
     */
    public void registrar(Entorno entorno) {
        for (Nodo d : declaraciones) {
            if (d instanceof NodoFuncion) {
                NodoFuncion fn = (NodoFuncion) d;
                entorno.declararFuncion(fn.getNombre(), fn);
            }
        }
    }

    /**
     *  Ejecución del programa (MODO SCRIPT / HÍBRIDO).
     */
    @Override
    public Object execute(Entorno entorno) {
        try {
            for (Nodo d : declaraciones) {
                if (!(d instanceof NodoFuncion)) {
                    if (d instanceof NodoSentencia) {
                        ((NodoSentencia) d).execute(entorno);
                    } else if (d instanceof NodoExpresion) {
                        ((NodoExpresion) d).getValue(entorno);
                    }
                }
            }
            
            // IMPRESIÓN DEL ENTORNO FINAL AL TERMINAR EL SCRIPT
            System.out.println("[DEBUG] Estado final del entorno:");
          
            entorno.imprimirVariables(); 
            
        } catch (ErrorSemanticoException e) {
            ErrorManager.getInstance().agregarSemantico(
                e.getDescripcion(), e.getLinea(), e.getColumna()
            );
        }

        return null;
    }

   @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Programa\n");
    if (declaraciones != null) {
        for (Nodo d : declaraciones) {
            if (d != null) {
                sb.append(d.toAST(nivel + 1));
            }
        }
    }
    return sb.toString();
} }