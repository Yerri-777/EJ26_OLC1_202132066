package lexer;

import java_cup.runtime.Symbol;
import tokens.Token;
import tokens.TipoToken;
import errores.ErrorManager;
import java.util.ArrayList;
import java.util.List;
import parser.sym; 

%%

%class  Lexer
%public
%unicode
%cup
%line
%column
%char

%{
    private List<Token> listaTokens = new ArrayList<>();
    private boolean requierePuntoComa = false;

    private Symbol token(int tipo, TipoToken tipoToken) {
        Token t = new Token(tipoToken, yytext(), yyline + 1, yycolumn + 1);
        listaTokens.add(t);
        
        // Lógica ASI: Evaluamos si este token marca el posible final de una instrucción
        // Se inyecta punto y coma si el token es un ID, un literal o un cierre de bloque/función
        if (tipo == sym.ID || tipo == sym.ENTERO || tipo == sym.FLOTANTE ||
            tipo == sym.STRING || tipo == sym.RUNE_LIT || tipo == sym.BOOLEANO ||
            tipo == sym.NIL || tipo == sym.BREAK || tipo == sym.CONTINUE ||
            tipo == sym.RETURN || tipo == sym.INCREMENTO || tipo == sym.DECREMENTO ||
            tipo == sym.PAREN_CIERRA || tipo == sym.LLAVE_CIERRA) {
            requierePuntoComa = true;
        } else if (tipo == sym.PUNTO_COMA || tipo == sym.LLAVE_ABRE || tipo == sym.COMA) {
            // Si el token es explícito, no inyectamos otro
            requierePuntoComa = false;
        } else {
            requierePuntoComa = false;
        }

        return new Symbol(tipo, yyline + 1, yycolumn + 1, t);
    }

    private void errorLexico(String lexema) {
        // En lugar de detener, reportamos y continuamos
        ErrorManager.getInstance().agregarLexico(
            "El símbolo \"" + lexema + "\" no es reconocido.",
            yyline + 1, yycolumn + 1
        );
    }

    public List<Token> getListaTokens() { return listaTokens; }
%}

/* MACROS */
SaltoLinea     = \r|\n|\r\n
Espacio        = [ \t\f]+
Digito         = [0-9]
Letra          = [a-zA-Z_]
Identificador  = {Letra}({Letra}|{Digito})*
Entero         = {Digito}+
Flotante       = {Digito}+"."{Digito}+
StringLit      = \"([^\"\n\r\\]|\\.)*\"
RuneLit        = \'([^\'\n\r\\]|\\.)\'
ComLinea       = "//"[^\n]*
ComBloque      = "/*"([^*]|\*+[^/*])*\*+"/"

%%

/* REGLAS */

{SaltoLinea} {
    if (requierePuntoComa) {
        requierePuntoComa = false; 
        Token t = new Token(TipoToken.PUNTO_COMA, "ASI_;", yyline + 1, yycolumn + 1);
        listaTokens.add(t);
        return new Symbol(sym.PUNTO_COMA, yyline + 1, yycolumn + 1, t);
    }
}

{Espacio}       { /* ignorar */ }
{ComLinea}      { /* ignorar */ }
{ComBloque}     { /* ignorar */ }

/* Reservadas Fase 1 y 2 */
"package"       { return token(sym.TK_PACKAGE, TipoToken.RES_PACKAGE); }
"var"           { return token(sym.VAR,      TipoToken.RES_VAR);      }
"func"          { return token(sym.FUNC,     TipoToken.RES_FUNC);     }
"if"            { return token(sym.IF,       TipoToken.RES_IF);       }
"else"          { return token(sym.ELSE,     TipoToken.RES_ELSE);     }
"for"           { return token(sym.FOR,      TipoToken.RES_FOR);      }
"range"         { return token(sym.RANGE,    TipoToken.RES_RANGE);    }
"break"         { return token(sym.BREAK,    TipoToken.RES_BREAK);    }
"continue"      { return token(sym.CONTINUE, TipoToken.RES_CONTINUE);}
"return"        { return token(sym.RETURN,   TipoToken.RES_RETURN);   }
"switch"        { return token(sym.SWITCH,   TipoToken.RES_SWITCH);   }
"case"          { return token(sym.CASE,     TipoToken.RES_CASE);     }
"default"       { return token(sym.DEFAULT,  TipoToken.RES_DEFAULT);  }
"struct"        { return token(sym.STRUCT,   TipoToken.RES_STRUCT);   }
"type"          { return token(sym.TYPE,          TipoToken.RES_TYPE);         }

/* Tipos */
"int"           { return token(sym.TIPO_INT,     TipoToken.TIPO_INT);     }
"float64"       { return token(sym.TIPO_FLOAT64, TipoToken.TIPO_FLOAT64); }
"string"        { return token(sym.TIPO_STRING,  TipoToken.TIPO_STRING);  }
"bool"          { return token(sym.TIPO_BOOL,    TipoToken.TIPO_BOOL);    }
"rune"          { return token(sym.TIPO_RUNE,    TipoToken.TIPO_RUNE);    }

/* Literales */
"true"          { return token(sym.BOOLEANO, TipoToken.LIT_BOOLEANO); }
"false"         { return token(sym.BOOLEANO, TipoToken.LIT_BOOLEANO); }
"nil"           { return token(sym.NIL,      TipoToken.LIT_NIL);      }

/* Nativas de GoLite */
"fmt.Println"        { return token(sym.FMT_PRINTLN, TipoToken.RES_FMT_PRINTLN); }
"strconv.Atoi"       { return token(sym.STRCONV_ATOI, TipoToken.RES_STRCONV_ATOI); }
"strconv.ParseFloat" { return token(sym.STRCONV_PARSEFLOAT, TipoToken.RES_STRCONV_PARSEFLOAT); }
"reflect.TypeOf"     { return token(sym.REFLECT_TYPEOF, TipoToken.RES_REFLECT_TYPEOF); }
"append"             { return token(sym.APPEND, TipoToken.RES_APPEND); }
"len"                { return token(sym.LEN, TipoToken.RES_LEN); }
"strings.Join"       { return token(sym.STRINGS_JOIN, TipoToken.RES_STRINGS_JOIN); }

{Flotante}      { return token(sym.FLOTANTE, TipoToken.LIT_FLOTANTE); }
{Entero}        { return token(sym.ENTERO,   TipoToken.LIT_ENTERO);   }
{StringLit}     { return token(sym.STRING,   TipoToken.LIT_STRING);   }
{RuneLit}       { return token(sym.RUNE_LIT, TipoToken.LIT_RUNE);     }

{Identificador} { return token(sym.ID, TipoToken.IDENTIFICADOR); }

/* Operadores y Símbolos */
":=" { return token(sym.ASIGN_CORTO, TipoToken.OP_ASIGN_CORTO); }
"+=" { return token(sym.ASIGN_SUMA,  TipoToken.OP_ASIGN_SUMA);  }
"-=" { return token(sym.ASIGN_RESTA, TipoToken.OP_ASIGN_RESTA); }
"==" { return token(sym.IGUAL,       TipoToken.OP_IGUAL);       }
"!=" { return token(sym.DIFERENTE,   TipoToken.OP_DIFERENTE);   }
"<=" { return token(sym.MENOR_IGUAL, TipoToken.OP_MENOR_IGUAL); }
">=" { return token(sym.MAYOR_IGUAL, TipoToken.OP_MAYOR_IGUAL); }
"&&" { return token(sym.AND,         TipoToken.OP_AND);         }
"||" { return token(sym.OR,          TipoToken.OP_OR);          }
"++" { return token(sym.INCREMENTO,  TipoToken.OP_INCREMENTO);  }
"--" { return token(sym.DECREMENTO,  TipoToken.OP_DECREMENTO);  }
":"   { return token(sym.DOS_PUNTOS, TipoToken.DOS_PUNTOS); }
"+"  { return token(sym.SUMA,        TipoToken.OP_SUMA);        }
"-"  { return token(sym.RESTA,       TipoToken.OP_RESTA);       }
"*"  { return token(sym.MULT,        TipoToken.OP_MULT);        }
"/"  { return token(sym.DIV,         TipoToken.OP_DIV);         }
"%"  { return token(sym.MOD,         TipoToken.OP_MOD);         }
"="  { return token(sym.ASIGNACION,  TipoToken.OP_ASIGNACION);  }
"<"  { return token(sym.MENOR,       TipoToken.OP_MENOR);       }
">"  { return token(sym.MAYOR,       TipoToken.OP_MAYOR);       }
"!"  { return token(sym.NOT,         TipoToken.OP_NOT);         }
"("  { return token(sym.PAREN_ABRE,  TipoToken.PAREN_ABRE);     }
")"  { return token(sym.PAREN_CIERRA, TipoToken.PAREN_CIERRA);  }
"{"  { return token(sym.LLAVE_ABRE,   TipoToken.LLAVE_ABRE);    }
"}"  { return token(sym.LLAVE_CIERRA, TipoToken.LLAVE_CIERRA);  }
"["  { return token(sym.COR_ABRE,     TipoToken.COR_ABRE);      }
"]"  { return token(sym.COR_CIERRA,   TipoToken.COR_CIERRA);    }
";"  { return token(sym.PUNTO_COMA,   TipoToken.PUNTO_COMA);    }
","  { return token(sym.COMA,         TipoToken.COMA);          }
"."  { return token(sym.PUNTO,        TipoToken.PUNTO);         }

[^] { errorLexico(yytext()); }

<<EOF>> { 
    if (requierePuntoComa) {
        requierePuntoComa = false;
        Token t = new Token(TipoToken.PUNTO_COMA, "ASI_EOF", yyline + 1, yycolumn + 1);
        listaTokens.add(t);
        return new Symbol(sym.PUNTO_COMA, yyline + 1, yycolumn + 1, t);
    }
    return new Symbol(sym.EOF); 
}