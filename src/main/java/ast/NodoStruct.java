package ast;

import entorno.Entorno;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class NodoStruct extends Nodo {

    private String nombre;
    private List<NodoParametro> campos;
    private Map<String, String> atributosMap; 
    public NodoStruct(String nombre, List<NodoParametro> campos, int linea, int columna) {
        super(linea, columna);
        this.nombre = nombre;
        this.campos = campos;
        this.atributosMap = new HashMap<>(); 
    }

   
    public NodoStruct(Map<String, String> atributosMap) {
        super(0, 0); 
        this.nombre = "StructDinámico"; 
        this.campos = new ArrayList<>(); 
        this.atributosMap = atributosMap;
    }

    @Override
    public Object execute(Entorno entorno) {
        entorno.registrarStruct(nombre, this);
        return null;
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("Struct: ").append(nombre).append("\n");

      
        if (campos != null && !campos.isEmpty()) {
            for (NodoParametro p : campos) {
                sb.append(p.toAST(nivel + 1));
            }
        } 
        else if (atributosMap != null && !atributosMap.isEmpty()) {
            for (Map.Entry<String, String> entry : atributosMap.entrySet()) {
                sb.append(indent(nivel + 1))
                        .append("Campo: ").append(entry.getKey())
                        .append(" - Tipo: ").append(entry.getValue())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    // GETTERS Y SETTERS
    public List<NodoParametro> getCampos() {
        return campos;
    }

    
    public Map<String, String> getAtributosMap() {
        return atributosMap;
    }


    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
