package tokens;



 //Organización:
 // LITERALES      : valores concretos del lenguaje
 //TIPOS          : palabras reservadas de tipos
 // PALABRAS_RES   : palabras reservadas de control y estructura
 // OPERADORES     : aritméticos, lógicos, comparación, asignación
 // DELIMITADORES  : puntuación y agrupación
 // ESPECIALES     : EOF, ERROR
 
public enum TipoToken {

   
    // LITERALES
   
    LIT_ENTERO,        // 42, 0, 100
    LIT_FLOTANTE,      // 3.14, 0.001, 1.0
    LIT_STRING,        // "hola mundo"
    LIT_RUNE,          // 'A', '\n'
    LIT_BOOLEANO,      // true, false
    LIT_NIL,           // nil

  
    // TIPOS PRIMITIVOS 
 
    TIPO_INT,          // int
    TIPO_FLOAT64,      // float64
    TIPO_STRING,       // string
    TIPO_BOOL,         // bool
    TIPO_RUNE,         // rune

   
    // PALABRAS RESERVADAS — control de flujo

    RES_IF,            // if
    RES_ELSE,          // else
    RES_FOR,           // for
    RES_SWITCH,        // switch  
    RES_CASE,          // case    
    RES_DEFAULT,       // default 
    RES_BREAK,         // break
    RES_CONTINUE,      // continue
    RES_RETURN,        // return
    RES_RANGE,         // range   

    
    // PALABRAS RESERVADAS — declaración
  
    RES_VAR,           // var
    RES_FUNC,          // func
    RES_STRUCT,        // struct  


    // PALABRAS RESERVADAS — funciones embebidas
 
    RES_FMT_PRINTLN,         // fmt.Println
    RES_STRCONV_ATOI,        // strconv.Atoi
    RES_STRCONV_PARSEFLOAT,  // strconv.ParseFloat
    RES_REFLECT_TYPEOF,      // reflect.TypeOf


    // IDENTIFICADOR

    IDENTIFICADOR,    


    // OPERADORES ARITMÉTICOS

    OP_SUMA,           // +
    OP_RESTA,          // -
    OP_MULT,           // *
    OP_DIV,            // /
    OP_MOD,            // %


    // OPERADORES DE ASIGNACIÓN

    OP_ASIGNACION,         // =
    OP_ASIGN_CORTO,        // :=
    OP_ASIGN_SUMA,         // +=
    OP_ASIGN_RESTA,        // -=

   
    // OPERADORES DE COMPARACIÓN
   
    OP_IGUAL,          // ==
    OP_DIFERENTE,      // !=
    OP_MENOR,          // <
    OP_MAYOR,          // >
    OP_MENOR_IGUAL,    // <=
    OP_MAYOR_IGUAL,    // >=

    // ─────────────────────────────────────────────
    // OPERADORES LÓGICOS
    // ─────────────────────────────────────────────
    OP_AND,            // &&
    OP_OR,             // ||
    OP_NOT,            // !

    
    // INCREMENTO / DECREMENTO 

    OP_INCREMENTO,     // ++
    OP_DECREMENTO,     // --

   
    // DELIMITADORES Y PUNTUACIÓN
 
    PAREN_ABRE,        // (
    PAREN_CIERRA,      // )
    LLAVE_ABRE,        // {
    LLAVE_CIERRA,      // }
    CORCHETE_ABRE,     // [
    CORCHETE_CIERRA,   // ]
    PUNTO_COMA,        // ;
    DOS_PUNTOS,        // :
    COMA,              // ,
    PUNTO,             // .

  
    // ESPECIALES

    EOF,               // fin de archivo
    ERROR              // carácter no reconocido (error léxico)
}