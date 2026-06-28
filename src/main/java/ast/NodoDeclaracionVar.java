package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.HashMap;
import java.util.ArrayList;

public class NodoDeclaracionVar extends NodoSentencia {

    private final String        nombre;
    private final NodoTipo      tipo;
    private final NodoExpresion inicializador; // Debe ser NodoExpresion
    private final boolean       esExplicita;   

   
    public NodoDeclaracionVar(String nombre, NodoTipo tipo, NodoExpresion inicializador, boolean esExplicita, int linea, int columna) {
        super(linea, columna);
        this.nombre        = nombre;
        this.tipo          = tipo;
        this.inicializador = inicializador;
        this.esExplicita   = esExplicita;
    }

    @Override
    public Object execute(Entorno entorno) {
        if (entorno.existeEnActual(nombre)) {
            throw new ErrorSemanticoException(
                "La variable '" + nombre + "' ya fue declarada en este ámbito.",
                linea, columna
            );
        }

        String tipoFinal;
        Object valorFinal;

        if (esExplicita) {
            tipoFinal = tipo.getNombre();

            if (inicializador != null) {
                Object valorExpr = inicializador.getValue(entorno);
                valorFinal = convertirYValidar(valorExpr, tipoFinal);
            } else {
                // INICIALIZACIÓN AUTOMÁTICA
                if (tipoFinal.startsWith("[]")) {
                    valorFinal = new ArrayList<Object>();
                } else if (!isPrimitivo(tipoFinal)) {
                    valorFinal = new HashMap<String, Object>();
                } else {
                    valorFinal = tipo.valorPorDefecto(); 
                }
            }
        } else {
            // Caso := (Declaración Corta)
            Object valorExpr = (inicializador != null) ? inicializador.getValue(entorno) : null;
            tipoFinal  = inferirTipo(valorExpr);
            valorFinal = valorExpr;
        }

        entorno.declarar(nombre, tipoFinal, valorFinal, linea, columna);
        return null;
    }

    private Object convertirYValidar(Object valor, String tipoDeclarado) {
        if (valor == null) return null; 
        String tipoValor = inferirTipo(valor);

        if (tipoDeclarado.equals("float64") && tipoValor.equals("int")) {
            return ((Integer) valor).doubleValue();
        }
        if (tipoDeclarado.equals(tipoValor)) return valor;
        if (tipoDeclarado.equals("rune") && tipoValor.equals("int")) return valor;
        if (tipoDeclarado.equals("int")  && tipoValor.equals("rune")) return valor;

        throw new ErrorSemanticoException(
            "No se puede asignar un valor de tipo '" + tipoValor +
            "' a la variable '" + nombre + "' de tipo '" + tipoDeclarado + "'.",
            linea, columna
        );
    }

    private String inferirTipo(Object valor) {
        if (valor instanceof Integer) return "int";
        if (valor instanceof Double)  return "float64";
        if (valor instanceof String)  return "string";
        if (valor instanceof Boolean) return "bool";
        if (valor instanceof Character) return "rune";
        if (valor instanceof ArrayList || valor instanceof java.util.List) return "[]"; 
        if (valor instanceof HashMap) return "struct";
        return "nil";  
    }
    
    private boolean isPrimitivo(String tipoStr) {
        return tipoStr.equals("int") || tipoStr.equals("float64") || 
               tipoStr.equals("string") || tipoStr.equals("bool") || 
               tipoStr.equals("rune") || tipoStr.equals("nil");
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append(esExplicita ? "DeclaracionVar\n" : "DeclaracionCorta\n");
        sb.append(indent(nivel + 1)).append("ID: ").append(nombre).append("\n");
        if (tipo != null) {
            sb.append(indent(nivel + 1)).append("Tipo: ").append(tipo.getNombre()).append("\n");
        }
        if (inicializador != null) {
            sb.append(inicializador.toAST(nivel + 1));
        }
        return sb.toString();
    }
}