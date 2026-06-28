package ast;

import entorno.Entorno;

public class NodoAsignacionCompuesta extends NodoSentencia {

    private final Nodo target;
    private final String operador; // "+=" o "-="
    private final NodoExpresion expresion;

    public NodoAsignacionCompuesta(Nodo target, String operador, NodoExpresion expresion, int linea, int columna) {
        super(linea, columna);
        this.target = target;
        this.operador = operador;
        this.expresion = expresion;
    }

    @Override
    public Object execute(Entorno entorno) {
        Object valorActual = target.getValue(entorno); // Obtiene el valor (int/float)
        Object operandoDer = expresion.getValue(entorno);
        
        if (valorActual != null && operandoDer != null) {
            // Realizar operación (puedes usar tu clase NodoBinario aquí si quieres)
            NodoBinario op = new NodoBinario(operador.substring(0,1), 
                                             new NodoLiteral(valorActual, "any", linea, columna), 
                                             expresion, linea, columna);
            Object resultado = op.getValue(entorno);
            
            if (resultado != null) {
                target.assign(entorno, resultado); // Se guarda de vuelta
            }
        }
        return null;
    }

 @Override
public String toAST(int nivel) {
    StringBuilder sb = new StringBuilder();
    sb.append(indent(nivel)).append("AsignacionCompuesta\n");
    // 'this.operador' es el símbolo (ej. +=, -=)
    sb.append(indent(nivel + 1)).append("Operador: ").append(this.operador).append("\n");
    if (target != null) sb.append(target.toAST(nivel + 1));
    if (expresion != null) sb.append(expresion.toAST(nivel + 1));
    return sb.toString();
} }