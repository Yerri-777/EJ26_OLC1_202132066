package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.HashMap;
import java.util.List;

/**
 * Representa la instanciación de un Struct: Nombre{val1, val2...}
 */
public class NodoInstanciaStruct extends NodoExpresion {

    private final String nombreStruct;
    private final List<Nodo> valores;

    public NodoInstanciaStruct(String nombreStruct, List<Nodo> valores, int linea, int col) {
        super(linea, col);
        this.nombreStruct = nombreStruct;
        this.valores = valores;
    }

    public String getTipo(Entorno entorno) {
        // En un futuro podrías verificar si el struct existe, pero
        // por ahora devolvemos el nombre que define el tipo.
        return nombreStruct;
    }

    @Override
    public Object execute(Entorno entorno) {
        // Buscamos la definición del struct
        NodoStruct def = entorno.buscarStruct(nombreStruct);

        if (def == null) {
            throw new ErrorSemanticoException(
                "El tipo de struct '" + nombreStruct + "' no existe.",
                linea,
                columna
            );
        }

        // Crear un mapa que represente la instancia (Campo -> Valor)
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

    /**
     * IMPORTANTE: Aquí estaba el error.
     * Al ser una expresión, cuando el compilador necesite el valor de este nodo,
     * simplemente ejecutamos la lógica de creación del struct.
     */
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