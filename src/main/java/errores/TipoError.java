package errores;


public enum TipoError {
    LEXICO,     // Carácter no reconocido por el lexer
    SINTACTICO, // Estructura gramatical inválida
    SEMANTICO   // Incompatibilidad de tipos, variable no declarada, etc.
}