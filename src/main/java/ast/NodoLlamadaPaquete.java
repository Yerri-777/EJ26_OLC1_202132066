package ast;

import entorno.Entorno;
import excepciones.ErrorSemanticoException;
import java.util.List;


public class NodoLlamadaPaquete extends NodoExpresion {

    private final String paquete;
    private final String funcion;
    private final List<NodoExpresion> argumentos;

    public NodoLlamadaPaquete(String paquete, String funcion, List<NodoExpresion> argumentos, int linea, int columna) {
        super(linea, columna);
        this.paquete = paquete;
        this.funcion = funcion;
        this.argumentos = argumentos;
    }

    @Override
    public Object getValue(Entorno entorno) {
        String firma = paquete + "." + funcion;

        switch (firma) {
            case "fmt.Println":
                return ejecutarFmtPrintln(entorno);
            case "strconv.Atoi":
                return ejecutarStrconvAtoi(entorno);
            case "strconv.ParseFloat":
                return ejecutarStrconvParseFloat(entorno);
            case "reflect.TypeOf":
                return ejecutarReflectTypeOf(entorno);
            default:
                throw new ErrorSemanticoException(
                    "La función '" + firma + "' no está definida o soportada.", 
                    linea, columna
                );
        }
    }



    private Object ejecutarFmtPrintln(Entorno entorno) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argumentos.size(); i++) {
            Object valor = argumentos.get(i).getValue(entorno);
            sb.append(valor == null ? "nil" : valor.toString());
            if (i < argumentos.size() - 1) sb.append(" ");
        }
        System.out.println(sb.toString());
        return null; 
    }

    private Object ejecutarStrconvAtoi(Entorno entorno) {
        validarArgumentos(paquete + "." + funcion, 1);
        Object arg = argumentos.get(0).getValue(entorno);
        
        if (!(arg instanceof String)) {
            throw new ErrorSemanticoException("strconv.Atoi requiere un argumento string.", linea, columna);
        }
        
        try {
            return Integer.parseInt((String) arg);
        } catch (NumberFormatException e) {
            throw new ErrorSemanticoException("No se pudo convertir '" + arg + "' a int.", linea, columna);
        }
    }

    private Object ejecutarStrconvParseFloat(Entorno entorno) {
        validarArgumentos(paquete + "." + funcion, 1);
        Object arg = argumentos.get(0).getValue(entorno);
        
        if (!(arg instanceof String)) {
            throw new ErrorSemanticoException("strconv.ParseFloat requiere un argumento string.", linea, columna);
        }
        
        try {
            return Double.parseDouble((String) arg);
        } catch (NumberFormatException e) {
            throw new ErrorSemanticoException("No se pudo convertir '" + arg + "' a float64.", linea, columna);
        }
    }

    private Object ejecutarReflectTypeOf(Entorno entorno) {
        validarArgumentos(paquete + "." + funcion, 1);
        Object arg = argumentos.get(0).getValue(entorno);
        
        if (arg instanceof Integer) return "int";
        if (arg instanceof Double)  return "float64";
        if (arg instanceof String)  return "string";
        if (arg instanceof Boolean) return "bool";
        if (arg == null)            return "nil";
        
        return "desconocido";
    }

    private void validarArgumentos(String nombreFirma, int cantidadEsperada) {
        if (argumentos.size() != cantidadEsperada) {
            throw new ErrorSemanticoException(
                "La función " + nombreFirma + " espera " + cantidadEsperada + " argumento(s), " +
                "se recibieron " + argumentos.size() + ".", 
                linea, columna
            );
        }
    }

    @Override
    public String toAST(int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(nivel)).append("LlamadaPaquete: ").append(paquete).append(".").append(funcion).append("\n");
        for (NodoExpresion a : argumentos) {
            sb.append(a.toAST(nivel + 1));
        }
        return sb.toString();
    }
}