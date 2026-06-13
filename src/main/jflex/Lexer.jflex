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
        if (tipo == sym.ID || tipo == sym.ENTERO || tipo == sym.FLOTANTE ||
            tipo == sym.STRING || tipo == sym.RUNE_LIT || tipo == sym.BOOLEANO ||
            tipo == sym.NIL || tipo == sym.BREAK || tipo == sym.CONTINUE ||
            tipo == sym.RETURN || tipo == sym.INCREMENTO || tipo == sym.DECREMENTO ||
            tipo == sym.PAREN_CIERRA || tipo == sym.LLAVE_CIERRA) {
            requierePuntoComa = true;
        } else {
            requierePuntoComa = false;
        }

        return new Symbol(tipo, yyline + 1, yycolumn + 1, t);
    }

    private void errorLexico(String lexema) {
        ErrorManager.getInstance().agregarLexico(
            "El símbolo \"" + lexema + "\" no es reconocido.",
            yyline + 1, yycolumn + 1
        );
    }

    public List<Token> getListaTokens() { return listaTokens; }
%}

/* MACROS */
/* Separamos los saltos de línea de los espacios normales para el ASI */
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

/* INYECCIÓN AUTOMÁTICA DE PUNTO Y COMA */
{SaltoLinea} {
    if (requierePuntoComa) {
        requierePuntoComa = false; // Apagamos bandera para evitar ciclos
        // Creamos un token ficticio para enviarlo al parser
        Token t = new Token(TipoToken.PUNTO_COMA, "ASI_;", yyline, yycolumn + 1);
        listaTokens.add(t);
        return new Symbol(sym.PUNTO_COMA, yyline + 1, yycolumn + 1, t);
    }
}

{Espacio}       { /* ignorar */ }
{ComLinea}      { /* ignorar, la bandera ASI no se altera aquí */ }
{ComBloque}     { /* ignorar */ }

/* Palabras reservadas */
"var"           { return token(sym.VAR,      TipoToken.RES_VAR);      }
"func"          { return token(sym.FUNC,     TipoToken.RES_FUNC);     }
"if"            { return token(sym.IF,       TipoToken.RES_IF);       }
"else"          { return token(sym.ELSE,     TipoToken.RES_ELSE);     }
"for"           { return token(sym.FOR,      TipoToken.RES_FOR);      }
"break"         { return token(sym.BREAK,    TipoToken.RES_BREAK);    }
"continue"      { return token(sym.CONTINUE, TipoToken.RES_CONTINUE);}
"return"        { return token(sym.RETURN,   TipoToken.RES_RETURN);   }

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

{Flotante}      { return token(sym.FLOTANTE, TipoToken.LIT_FLOTANTE); }
{Entero}        { return token(sym.ENTERO,   TipoToken.LIT_ENTERO);   }
{StringLit}     { return token(sym.STRING,   TipoToken.LIT_STRING);   }
{RuneLit}       { return token(sym.RUNE_LIT, TipoToken.LIT_RUNE);     }

/* Identificadores */
{Identificador} { return token(sym.ID, TipoToken.IDENTIFICADOR); }

/* Operadores y otros */
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
";"  { return token(sym.PUNTO_COMA,   TipoToken.PUNTO_COMA);    }
","  { return token(sym.COMA,         TipoToken.COMA);          }
"."  { return token(sym.PUNTO,        TipoToken.PUNTO);         }

/* Fallback de error */
[^] { errorLexico(yytext()); }

/* Manejo explícito de EOF para inyectar un último ';' si es necesario y evitar cuelgues */
<<EOF>> { 
    if (requierePuntoComa) {
        requierePuntoComa = false;
        Token t = new Token(TipoToken.PUNTO_COMA, "ASI_EOF", yyline + 1, yycolumn + 1);
        listaTokens.add(t);
        return new Symbol(sym.PUNTO_COMA, yyline + 1, yycolumn + 1, t);
    }
    return new Symbol(sym.EOF); 
}