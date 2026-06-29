package ast;

import entorno.Entorno;
import excepciones.ReturnException;


public class NodoReturn extends Nodo {
    
    // Puede ser un Nodo 
    private Object expresion;

    public NodoReturn(Object expresion, int linea, int columna) {
        super(linea, columna);
        this.expresion = expresion;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object valorRetorno = null;
        
        // Evaluar la expresión si existe (debe ser un nodo AST)
        if (this.expresion != null && this.expresion instanceof Nodo) {
            valorRetorno = ((Nodo) this.expresion).execute(entorno);
        }
        
          
        throw new ReturnException(valorRetorno); 
    }

  @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Return\n");
    
    if (this.expresion != null && this.expresion instanceof Nodo) {
        sb.append(((Nodo) this.expresion).toAST(nivel + 1));
    } else if (this.expresion != null) {
        sb.append(indent(nivel + 1)).append("Valor: ").append(this.expresion.toString()).append("\n");
    } else {
        sb.append(indent(nivel + 1)).append("Valor: void\n");
    }
    
    return sb.toString();
} }