package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.HashMap;
import java.util.List;


public class NodoInstanciaStruct extends NodoExpresion {

    private final String nombreStruct;
    private final List<Nodo> valores;

    public NodoInstanciaStruct(String nombreStruct, List<Nodo> valores, int linea, int col) {
        super(linea, col);
        this.nombreStruct = nombreStruct;
        this.valores = valores;
    }

    public String getTipo(Entorno entorno) {
     
        return nombreStruct;
    }

    @Override
    public Object execute(Entorno entorno) {
 
        NodoStruct def = entorno.buscarStruct(nombreStruct);

        if (def == null) {
            throw new ErrorSemanticoException(
                "El tipo de struct '" + nombreStruct + "' no existe.",
                linea,
                columna
            );
        }

        // Crear un mapa que represente la instancia 
        HashMap<String, Object> instancia = new HashMap<>();
        List<NodoParametro> campos = def.getCampos();

        // Validar cantidad de argumentos
        if (valores.size() != campos.size()) {
             throw new ErrorSemanticoException(
                "El struct '" + nombreStruct + "' espera " + campos.size() + 
                " valores, pero se dieron " + valores.size(),
                linea,
                columna
            );
        }

        // Ejecutar cada valor y mapearlo con el nombre del campo
        for (int i = 0; i < valores.size(); i++) {
            instancia.put(
                campos.get(i).getNombre(),
                valores.get(i).execute(entorno)
            );
        }

        return instancia;
    }

    
    @Override
    public Object getValue(Entorno entorno) {
        return this.execute(entorno);
    }

@Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("InstanciaStruct\n");
    sb.append(indent(nivel + 1)).append("Struct: ").append(nombreStruct).append("\n");
    return sb.toString();
}
}