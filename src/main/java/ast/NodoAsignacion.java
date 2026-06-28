package ast;

import entorno.Entorno;
import entorno.Simbolo; // Asegúrate de importar tu clase Simbolo
import excepciones.ErrorSemanticoException; // Asegúrate de importar tu clase de error

public class NodoAsignacion extends NodoSentencia {

    private final Nodo target;
    private final NodoExpresion expresion;

    public NodoAsignacion(Nodo target, NodoExpresion expresion, int linea, int columna) {
        super(linea, columna);
        this.target = target;
        this.expresion = expresion;
    }

    @Override
    public Object execute(Entorno entorno) throws ErrorSemanticoException {
        // 1. Obtenemos el valor de la expresión
        Object nuevoValor = expresion.getValue(entorno);

        if (nuevoValor != null) {
            // 2. VALIDACIÓN SEMÁNTICA: Solo si es un identificador, validamos el tipo
            if (target instanceof NodoIdentificador) {
                String nombreVar = ((NodoIdentificador) target).getNombre();
                Simbolo simbolo = entorno.buscar(nombreVar);

                if (simbolo != null) {
                    String tipoDeclarado = simbolo.getTipoDato(); // <--- Usa el método que ya existe en tu clase Simbolo
                    
                    // Verificamos compatibilidad
                    if (!esTipoCompatible(tipoDeclarado, nuevoValor)) {
                        throw new ErrorSemanticoException(
                            "Error semántico: No se puede asignar '" + nuevoValor.getClass().getSimpleName() + 
                            "' a una variable declarada como '" + tipoDeclarado + "'", 
                            this.linea, this.columna
                        );
                    }
                }
            }

            // 3. Si pasó la validación, procedemos con la lógica original
            target.assign(entorno, nuevoValor);
        }
        return null;
    }

    // Método auxiliar para verificar compatibilidad de tipos
    private boolean esTipoCompatible(String tipoDeclarado, Object valor) {
        if (tipoDeclarado.equalsIgnoreCase("int")) return valor instanceof Integer;
        if (tipoDeclarado.equalsIgnoreCase("string")) return valor instanceof String;
        if (tipoDeclarado.equalsIgnoreCase("float")) return valor instanceof Double || valor instanceof Float;
        if (tipoDeclarado.equalsIgnoreCase("bool")) return valor instanceof Boolean;
        return true; // Por defecto dejamos pasar si no logramos determinar el tipo
    }

  @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("Asignacion\n");
    // Agregamos el símbolo del operador de asignación
    sb.append(indent(nivel + 1)).append("Operador: =\n");
    if (target != null) sb.append(target.toAST(nivel + 1));
    if (expresion != null) sb.append(expresion.toAST(nivel + 1));
    return sb.toString();
}}