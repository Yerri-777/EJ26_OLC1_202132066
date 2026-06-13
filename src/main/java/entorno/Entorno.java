package entorno;

import ast.NodoFuncion;
import ast.NodoParametro;
import ast.NodoTipo;
import ast.NodoBloque;
import errores.ErrorManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
 // Entorno — Tabla de símbolos encadenada y gestor de funciones.
 
public class Entorno {

    // Centinela para no encontrado
    public static final Object NO_ENCONTRADO = new Object();

    // Estado del entorno 
    private final Map<String, Simbolo> tabla;
    private final Entorno padre;
    private final String nombreAmbito;

    //Solo en el entorno global 
    private final Map<String, NodoFuncion> funciones;
    private final StringBuilder salidaConsola;
    private final List<Simbolo> historicoSimbolos;

    // Constructores
    // Contructor para entorno global
    public Entorno() {
        this.tabla = new HashMap<>();
        this.padre = null;
        this.nombreAmbito = "Global";
        this.funciones = new HashMap<>();
        this.salidaConsola = new StringBuilder();
        this.historicoSimbolos = new ArrayList<>();
    }

    // Constructor para entorno hijo (bloque independiente, if, for)
    public Entorno(Entorno padre) {
        this.tabla = new HashMap<>();
        this.padre = padre;
        this.nombreAmbito = padre != null ? padre.getNombreAmbito() + "->Bloque" : "Bloque";
        this.funciones = null;
        this.salidaConsola = null;
        this.historicoSimbolos = null;
    }

    // Constructor para entorno de función
    public Entorno(Entorno padre, String nombreFuncion) {
        this.tabla = new HashMap<>();
        this.padre = padre;
        this.nombreAmbito = nombreFuncion != null ? nombreFuncion : "Funcion_Desconocida";
        this.funciones = null;
        this.salidaConsola = null;
        this.historicoSimbolos = null;
    }

    // ─── Getters simples ──────────────────────────────────────────────────────
    public String getNombreAmbito() { return nombreAmbito; }
    public Entorno getPadre() { return padre; }

  
    // VARIABLES


    public void declarar(String nombre, String tipo, Object valor) {
        declarar(nombre, tipo, valor, 0, 0); 
    }

    public void declarar(String nombre, String tipo, Object valor, int linea, int col) {
        if (tabla.containsKey(nombre)) {
            ErrorManager.getInstance().agregarSemantico(
                "La variable '" + nombre + "' ya fue declarada en el ámbito '" + nombreAmbito + "'.", linea, col
            );
            return; 
        }
        
        Simbolo s = new Simbolo(
            nombre, Simbolo.TipoSimbolo.VARIABLE, tipo,
            nombreAmbito, linea, col, valor
        );
        tabla.put(nombre, s);
        agregarHistorico(s);
    }

    public Object obtener(String nombre) {
        if (tabla.containsKey(nombre)) return tabla.get(nombre).getValor();
        if (padre != null) return padre.obtener(nombre);
        return NO_ENCONTRADO;
    }

    public String obtenerTipo(String nombre) {
        if (tabla.containsKey(nombre)) return tabla.get(nombre).getTipoDato();
        if (padre != null) return padre.obtenerTipo(nombre);
        return "desconocido"; 
    }

    public void asignar(String nombre, Object valor) {
        if (tabla.containsKey(nombre)) {
            tabla.get(nombre).setValor(valor);
            return;
        }
        if (padre != null) {
            padre.asignar(nombre, valor);
        }
    }

    public boolean existeEnActual(String nombre) {
        return tabla.containsKey(nombre);
    }

    public void imprimirVariables() {
        if (this.tabla.isEmpty()) {
            System.out.println("   [" + nombreAmbito + "] (Sin variables locales activas)");
        } else {
            for (Map.Entry<String, Simbolo> entrada : this.tabla.entrySet()) {
                Simbolo sim = entrada.getValue();
                System.out.println("   [" + nombreAmbito + "] " + entrada.getKey() + " = " 
                    + (sim.getValor() != null ? sim.getValor() : "nil") 
                    + " (" + sim.getTipoDato() + ")");
            }
        }
        if (this.padre != null) {
            this.padre.imprimirVariables();
        }
    }

 
    // FUNCIONES 

    
    public void declararFuncion(String nombre, NodoFuncion funcion) {
        Entorno global = getGlobal();
        if (global.funciones.containsKey(nombre)) {
            ErrorManager.getInstance().agregarSemantico(
                "La función '" + nombre + "' ya está declarada.", 
                funcion.getLinea(), funcion.getColumna()
            );
            return;
        }
        
        if (global.tabla.containsKey(nombre)) {
            ErrorManager.getInstance().agregarSemantico(
                "Ya existe una variable con el nombre '" + nombre + "'. No se puede declarar la función.", 
                funcion.getLinea(), funcion.getColumna()
            );
            return;
        }
        
        global.funciones.put(nombre, funcion);
        
        String tipoRetorno = funcion.getTipoRetorno() != null ? funcion.getTipoRetorno().getNombre() : "void";
        Simbolo s = new Simbolo(
            nombre, Simbolo.TipoSimbolo.FUNCION, tipoRetorno,
            "Global", funcion.getLinea(), funcion.getColumna(), null
        );
        global.agregarHistorico(s);
    }

    public void registrarFuncionNativa(String nombre, String tipoRetorno) {
        Entorno global = getGlobal();
        if (global.tabla.containsKey(nombre)) return;

        Simbolo s = new Simbolo(
            nombre, 
            Simbolo.TipoSimbolo.FUNCION, 
            tipoRetorno,
            "Global", 
            0, 0, 
            null
        );
        global.tabla.put(nombre, s);
        global.agregarHistorico(s);

        // Registro en el mapa de funciones
        global.funciones.put(nombre, new NodoFuncionNativa(nombre));
    }

    public NodoFuncion buscarFuncion(String nombre) {
        return getGlobal().funciones.get(nombre);
    }

    // IMPLEMENTACIÓN DE NODO NATIVO 
    public static class NodoFuncionNativa extends NodoFuncion {
        public NodoFuncionNativa(String nombre) {
            super(nombre, new ArrayList<NodoParametro>(), null, null, 0, 0);
        }

        @Override
        public Object execute(Entorno entorno) {
            return null;
        }

        @Override
        public Object ejecutarCon(Entorno entornoLlamada, List<Object> valores) {
            return null;
        }
    }


    // SALIDA Y UTILIDADES

    
    public void agregarSalida(String texto) {
        if (texto != null) {
            Entorno global = getGlobal();
            if (global.salidaConsola != null) {
                global.salidaConsola.append(texto);
            }
        }
    }

    public String getSalidaConsola() {
        Entorno global = getGlobal();
        return (global.salidaConsola != null) ? global.salidaConsola.toString() : "";
    }

    private void agregarHistorico(Simbolo s) {
        Entorno global = getGlobal();
        if (global.historicoSimbolos != null) {
            global.historicoSimbolos.add(s);
        }
    }

    public List<Simbolo> getHistoricoSimbolos() {
        Entorno global = getGlobal();
        return (global.historicoSimbolos != null) ? global.historicoSimbolos : new ArrayList<>();
    }

    private Entorno getGlobal() {
        Entorno e = this;
        while (e.padre != null) {
            e = e.padre;
        }
        return e;
    }
}