package ast;

import entorno.Entorno;
import excepciones.BreakException;
import excepciones.ContinueException;
import excepciones.ErrorSemanticoException;
import java.util.List;

// Cambiado: Ahora hereda de NodoSentencia directamente
public class NodoForRange extends NodoSentencia { 

    private final String idIndice;
    private final String idValor;
    private final NodoExpresion slice;
    private final NodoBloque cuerpo;

    public NodoForRange(String idIndice, String idValor, NodoExpresion slice, 
                        NodoBloque cuerpo, int linea, int columna) {
        super(linea, columna); // Solo necesita esto
        this.idIndice = idIndice;
        this.idValor = idValor;
        this.slice = slice;
        this.cuerpo = cuerpo;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object obj = slice.execute(entorno);
        
        if (!(obj instanceof List)) {
            throw new ErrorSemanticoException("El rango debe ser sobre un slice o arreglo.", linea, columna);
        }

        List<?> lista = (List<?>) obj;
        
        for (int i = 0; i < lista.size(); i++) {
            Entorno entornoFor = new Entorno(entorno);
            
            // Asignamos el índice y el elemento de la lista
            entornoFor.asignar(idIndice, i); 
            entornoFor.asignar(idValor, lista.get(i));

            try {
                if (cuerpo != null) {
                    cuerpo.execute(entornoFor);
                }
            } catch (BreakException b) {
                break; 
            } catch (ContinueException c) {
                continue; 
            }
        }
        return null;
    }

  @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("ForRange\n");
    if (idIndice != null) sb.append(indent(nivel + 1)).append("Indice: ").append(idIndice).append("\n");
    if (idValor != null)  sb.append(indent(nivel + 1)).append("Valor: ").append(idValor).append("\n");
    if (cuerpo != null)    sb.append(cuerpo.toAST(nivel + 1));
    return sb.toString();
}
}