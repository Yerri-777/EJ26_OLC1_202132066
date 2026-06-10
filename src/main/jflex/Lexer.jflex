package lexer;

import java_cup.runtime.Symbol;
import tokens.Token;
import tokens.TipoToken;
import errores.ErrorManager;
import java.util.ArrayList;
import java.util.List;

%%

/* ══════════════════════════════════════════════════════════════════════
   OPCIONES DE JFLEX
   ══════════════════════════════════════════════════════════════════════ */
%class  Lexer
%public
%unicode
%cup
%line
%column
%char

/* ══════════════════════════════════════════════════════════════════════
   CÓDIGO JAVA INCRUSTADO EN LA CLASE Lexer
   ══════════════════════════════════════════════════════════════════════ */
%{
    /** Lista de tokens generados. Usada para el reporte de tabla de tokens. */
    private List<Token> listaTokens = new ArrayList<>();

    /**
     * Crea un Symbol para CUP y además registra el token en la lista
     * para el reporte.  Línea y columna son base-1 (yyline+1, yycolumn+1).
     */
    private Symbol token(int tipo, TipoToken tipoToken) {
        Token t = new Token(tipoToken, yytext(), yyline + 1, yycolumn + 1);
        listaTokens.add(t);
        return new Symbol(tipo, yyline + 1, yycolumn + 1, t);
    }

    /**
     * Registra un error léxico sin crear un Symbol válido.
     * Permite al parser continuar buscando más errores.
     */
    private void errorLexico(String lexema) {
        ErrorManager.getInstance().agregarLexico(
            "El símbolo \"" + lexema + "\" no es reconocido por el lenguaje.",
            yyline + 1,
            yycolumn + 1
        );
    }

    /** Acceso externo a la lista de tokens para el reporte. */
    public List<Token> getListaTokens() {
        return listaTokens;
    }
%}

/* ══════════════════════════════════════════════════════════════════════
   MACROS / DEFINICIONES REUTILIZABLES
   ══════════════════════════════════════════════════════════════════════ */

/* Espacios en blanco: espacio, tab, retorno de carro, nueva línea */
Espacio        = [ \t\r\n]+

/* Dígitos y letras */
Digito         = [0-9]
Letra          = [a-zA-Z]
LetraOGuion    = [a-zA-Z_]

/* Identificador: empieza con letra o guión bajo, sigue con alfanumérico o guión */
Identificador  = {LetraOGuion}({LetraOGuion}|{Digito})*

/* Literales numéricos */
Entero         = {Digito}+
Flotante       = {Digito}+"."{Digito}+

/* Literales string: comillas dobles, admite secuencias de escape */
StringLit      = \"([^\"\\]|\\.)*\"

/* Literal rune: comilla simple, un carácter o secuencia de escape */
RuneLit        = \'([^\'\\]|\\[nrt\\'\"])\'

/* Comentarios */
ComLinea       = "//"[^\n]*
ComBloque      = "/*"([^*]|\*[^/])*"*/"

%%

/* ══════════════════════════════════════════════════════════════════════
   REGLAS LÉXICAS
   Orden: más específico primero. JFlex aplica la regla más larga que
   haga match (maximal munch); en caso de empate, la primera regla.
   ══════════════════════════════════════════════════════════════════════ */

/* ── Espacios y comentarios (ignorados) ──────────────────────────── */
{Espacio}          { /* ignorar */ }
{ComLinea}         { /* ignorar comentario de línea */ }
{ComBloque}        { /* ignorar comentario de bloque */ }

/* ── Funciones embebidas con punto (ANTES que identificadores) ───── */
"fmt.Println"           { return token(sym.FMT_PRINTLN,        TipoToken.RES_FMT_PRINTLN);        }
"strconv.Atoi"          { return token(sym.STRCONV_ATOI,       TipoToken.RES_STRCONV_ATOI);       }
"strconv.ParseFloat"    { return token(sym.STRCONV_PARSEFLOAT, TipoToken.RES_STRCONV_PARSEFLOAT); }
"reflect.TypeOf"        { return token(sym.REFLECT_TYPEOF,     TipoToken.RES_REFLECT_TYPEOF);     }

/* ── Palabras reservadas (ANTES que identificadores) ──────────────── */
"var"       { return token(sym.VAR,      TipoToken.RES_VAR);      }
"func"      { return token(sym.FUNC,     TipoToken.RES_FUNC);     }
"if"        { return token(sym.IF,       TipoToken.RES_IF);       }
"else"      { return token(sym.ELSE,     TipoToken.RES_ELSE);     }
"for"       { return token(sym.FOR,      TipoToken.RES_FOR);      }
"switch"    { return token(sym.SWITCH,   TipoToken.RES_SWITCH);   }
"case"      { return token(sym.CASE,     TipoToken.RES_CASE);     }
"default"   { return token(sym.DEFAULT,  TipoToken.RES_DEFAULT);  }
"break"     { return token(sym.BREAK,    TipoToken.RES_BREAK);    }
"continue"  { return token(sym.CONTINUE, TipoToken.RES_CONTINUE); }
"return"    { return token(sym.RETURN,   TipoToken.RES_RETURN);   }
"range"     { return token(sym.RANGE,    TipoToken.RES_RANGE);    }
"struct"    { return token(sym.STRUCT,   TipoToken.RES_STRUCT);   }

/* ── Tipos primitivos (palabras reservadas de tipo) ──────────────── */
"int"       { return token(sym.TIPO_INT,     TipoToken.TIPO_INT);     }
"float64"   { return token(sym.TIPO_FLOAT64, TipoToken.TIPO_FLOAT64); }
"string"    { return token(sym.TIPO_STRING,  TipoToken.TIPO_STRING);  }
"bool"      { return token(sym.TIPO_BOOL,    TipoToken.TIPO_BOOL);    }
"rune"      { return token(sym.TIPO_RUNE,    TipoToken.TIPO_RUNE);    }

/* ── Literales booleanos y nil ───────────────────────────────────── */
"true"      { return token(sym.BOOLEANO, TipoToken.LIT_BOOLEANO); }
"false"     { return token(sym.BOOLEANO, TipoToken.LIT_BOOLEANO); }
"nil"       { return token(sym.NIL,      TipoToken.LIT_NIL);      }

/* ── Literales numéricos ─────────────────────────────────────────── */
/* Flotante ANTES que entero para capturar "3.14" completo */
{Flotante}  { return token(sym.FLOTANTE, TipoToken.LIT_FLOTANTE); }
{Entero}    { return token(sym.ENTERO,   TipoToken.LIT_ENTERO);   }

/* ── Literales string y rune ─────────────────────────────────────── */
{StringLit} { return token(sym.STRING,   TipoToken.LIT_STRING);   }
{RuneLit}   { return token(sym.RUNE_LIT, TipoToken.LIT_RUNE);     }

/* ── Identificadores ─────────────────────────────────────────────── */
{Identificador} { return token(sym.ID, TipoToken.IDENTIFICADOR); }

/* ── Operadores de dos caracteres (ANTES que operadores simples) ─── */
":="        { return token(sym.ASIGN_CORTO,   TipoToken.OP_ASIGN_CORTO);   }
"+="        { return token(sym.ASIGN_SUMA,    TipoToken.OP_ASIGN_SUMA);    }
"-="        { return token(sym.ASIGN_RESTA,   TipoToken.OP_ASIGN_RESTA);   }
"=="        { return token(sym.IGUAL,         TipoToken.OP_IGUAL);         }
"!="        { return token(sym.DIFERENTE,     TipoToken.OP_DIFERENTE);     }
"<="        { return token(sym.MENOR_IGUAL,   TipoToken.OP_MENOR_IGUAL);   }
">="        { return token(sym.MAYOR_IGUAL,   TipoToken.OP_MAYOR_IGUAL);   }
"&&"        { return token(sym.AND,           TipoToken.OP_AND);           }
"||"        { return token(sym.OR,            TipoToken.OP_OR);            }
"++"        { return token(sym.INCREMENTO,    TipoToken.OP_INCREMENTO);    }
"--"        { return token(sym.DECREMENTO,    TipoToken.OP_DECREMENTO);    }

/* ── Operadores de un carácter ───────────────────────────────────── */
"+"         { return token(sym.SUMA,          TipoToken.OP_SUMA);          }
"-"         { return token(sym.RESTA,         TipoToken.OP_RESTA);         }
"*"         { return token(sym.MULT,          TipoToken.OP_MULT);          }
"/"         { return token(sym.DIV,           TipoToken.OP_DIV);           }
"%"         { return token(sym.MOD,           TipoToken.OP_MOD);           }
"="         { return token(sym.ASIGNACION,    TipoToken.OP_ASIGNACION);    }
"<"         { return token(sym.MENOR,         TipoToken.OP_MENOR);         }
">"         { return token(sym.MAYOR,         TipoToken.OP_MAYOR);         }
"!"         { return token(sym.NOT,           TipoToken.OP_NOT);           }

/* ── Delimitadores ───────────────────────────────────────────────── */
"("         { return token(sym.PAREN_ABRE,     TipoToken.PAREN_ABRE);     }
")"         { return token(sym.PAREN_CIERRA,   TipoToken.PAREN_CIERRA);   }
"{"         { return token(sym.LLAVE_ABRE,     TipoToken.LLAVE_ABRE);     }
"}"         { return token(sym.LLAVE_CIERRA,   TipoToken.LLAVE_CIERRA);   }
"["         { return token(sym.CORCHETE_ABRE,  TipoToken.CORCHETE_ABRE);  }
"]"         { return token(sym.CORCHETE_CIERRA,TipoToken.CORCHETE_CIERRA);}
";"         { return token(sym.PUNTO_COMA,     TipoToken.PUNTO_COMA);     }
":"         { return token(sym.DOS_PUNTOS,     TipoToken.DOS_PUNTOS);     }
","         { return token(sym.COMA,           TipoToken.COMA);           }
"."         { return token(sym.PUNTO,          TipoToken.PUNTO);          }

/* ── Cualquier otro carácter → ERROR LÉXICO (no detiene el análisis) */
[^]         {
                errorLexico(yytext());
            }
