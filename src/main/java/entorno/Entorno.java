
package entorno;

import ast.NodoFuncion;
import ast.NodoParametro;
import ast.NodoStruct;
import errores.ErrorManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entorno {

    // Centinela para no encontrado
    public static final Object NO_ENCONTRADO = new Object();

    // Estado del entorno 
    private final Map<String, Simbolo> tabla;
    private final Entorno padre;
    private final String nombreAmbito;

    // Solo en el entorno global 
    private final Map<String, NodoFuncion> funciones;
    private final Map<String, NativeFunction> funcionesNativas; // NUEVO: Para soporte de interfaz NativeFunction
    private final Map<String, NodoStruct> structs;
    private final StringBuilder salidaConsola;
    private final List<Simbolo> historicoSimbolos;

    // ────────────────────────── Constructores ──────────────────────────

    // Constructor para entorno global
    public Entorno() {
        this.tabla = new HashMap<>();
        this.padre = null;
        this.nombreAmbito = "Global";
        this.funciones = new HashMap<>();
        this.funcionesNativas = new HashMap<>(); // Inicializado
        this.structs = new HashMap<>();
        this.salidaConsola = new StringBuilder();
        this.historicoSimbolos = new ArrayList<>();
        
    // --- REGISTRO DE FUNCIONES NATIVAS ---
        
        // 1. fmt.Println
        registrarFuncionNativa("fmt.Println", (args) -> {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    Object val = args.get(i);
                    sb.append(val != null ? val.toString() : "nil");
                    if (i < args.size() - 1) sb.append(" ");
                }
            }
            sb.append("\n");
            this.agregarSalida(sb.toString());
            return null;
        });

        // 2. strconv.Atoi
        registrarFuncionNativa("strconv.Atoi", (args) -> {
            try { return Integer.parseInt(args.get(0).toString()); } 
            catch (Exception e) { return 0; }
        });

        // 3. strconv.ParseFloat
        registrarFuncionNativa("strconv.ParseFloat", (args) -> {
            try { return Double.parseDouble(args.get(0).toString()); } 
            catch (Exception e) { return 0.0; }
        });

        // 4. reflect.TypeOf
        registrarFuncionNativa("reflect.TypeOf", (args) -> {
            if (args == null || args.isEmpty()) return "nil";
            Object val = args.get(0);
            if (val instanceof Integer) return "int";
            if (val instanceof Double) return "float64";
            if (val instanceof String) return "string";
            if (val instanceof Boolean) return "bool";
            if (val instanceof List) return "slice";
            return "unknown";
        });

        // 5. slices.Index
        registrarFuncionNativa("slices.Index", (args) -> {
            if (args.size() == 2 && args.get(0) instanceof List) {
                List<?> lista = (List<?>) args.get(0);
                return lista.indexOf(args.get(1));
            }
            return -1;
        });

        // 6. strings.Join
        registrarFuncionNativa("strings.Join", (args) -> {
            if (args.size() == 2 && args.get(0) instanceof List) {
                List<?> lista = (List<?>) args.get(0);
                String sep = args.get(1).toString();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lista.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(lista.get(i).toString());
                }
                return sb.toString();
            }
            return "";
        });
    
    
    
    
    }

    
    
    
    
    // Constructor para entorno hijo (bloque independiente, if, for)
    public Entorno(Entorno padre) {
        this.tabla = new HashMap<>();
        this.padre = padre;
        this.nombreAmbito = padre != null ? padre.getNombreAmbito() + "->Bloque" : "Bloque";
        this.funciones = null;
        this.funcionesNativas = null;
        this.structs = null;
        this.salidaConsola = null;
        this.historicoSimbolos = null;
    }

    // Constructor para entorno de función
    public Entorno(Entorno padre, String nombreFuncion) {
        this.tabla = new HashMap<>();
        this.padre = padre;
        this.nombreAmbito = nombreFuncion != null ? nombreFuncion : "Funcion_Desconocida";
        this.funciones = null;
        this.funcionesNativas = null;
        this.structs = null;
        this.salidaConsola = null;
        this.historicoSimbolos = null;
    }

    // ─── Getters simples ──────────────────────────────────────────────────────
    public String getNombreAmbito() { return nombreAmbito; }
    public Entorno getPadre() { return padre; }

    // ────────────────────────── VARIABLES ──────────────────────────

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

    // --- MÉTODO AGREGADO PARA VALIDACIÓN SEMÁNTICA ---
    public Simbolo buscar(String nombre) {
        if (tabla.containsKey(nombre)) return tabla.get(nombre);
        if (padre != null) return padre.buscar(nombre);
        return null;
    }

    public String obtenerTipo(String nombre) {
        if (tabla.containsKey(nombre)) return tabla.get(nombre).getTipoDato();
        if (padre != null) return padre.obtenerTipo(nombre);
        return "desconocido"; 
    }

   public void asignar(String nombre, Object valor) {
        if (tabla.containsKey(nombre)) {
            // --- LOG DE DIAGNÓSTICO ---
            if (nombre.equals("p1")) {
                System.out.println("DEBUG: [Entorno] Asignando a 'p1'. Valor: " + 
                    (valor != null ? valor.getClass().getSimpleName() + " -> " + valor.toString() : "null"));
            }
            // ---------------------------
            
            tabla.get(nombre).setValor(valor);
            return;
        }
        
        if (padre != null) {
            padre.asignar(nombre, valor);
        } else {
            // Opcional: Si no existe y no hay padre, podrías querer lanzarlo aquí
            System.err.println("Advertencia: Intentando asignar a variable no declarada: " + nombre);
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

   // ────────────────────────── FUNCIONES AST ──────────────────────────

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

    public NodoFuncion buscarFuncion(String nombre) {
        return getGlobal().funciones.get(nombre);
    }

    // ────────────────────────── FUNCIONES NATIVAS (DOBLE SOPORTE) ──────────────────────────

    // 1. Soporte para la interfaz NativeFunction (Ejecución vía script/intérprete)
    public void registrarFuncionNativa(String nombre, NativeFunction funcion) {
        getGlobal().funcionesNativas.put(nombre, funcion);
    }

    public boolean esNativa(String nombre) {
        return getGlobal().funcionesNativas.containsKey(nombre);
    }

  /**
     * Ejecuta funciones nativas. 
     * Intercepta 'fmt.Println' para asegurar la escritura en el buffer de salida.
     */
   public Object ejecutarNativa(String nombre, List<Object> args, Entorno contexto) {
        // 1. Intercepción para fmt.Println (Manejo de salida)
        if ("fmt.Println".equals(nombre)) {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    Object val = args.get(i);
                    sb.append(val != null ? val.toString() : "nil");
                    if (i < args.size() - 1) {
                        sb.append(" ");
                    }
                }
            }
            sb.append("\n");
            
            // Usamos el contexto pasado por parámetro para asegurar que 
            // la salida llegue al búfer correcto
            contexto.agregarSalida(sb.toString());
            return null;
        }

        // 2. Delegación para el resto de funciones nativas
        Entorno global = getGlobal();
        if (global.funcionesNativas != null && global.funcionesNativas.containsKey(nombre)) {
            NativeFunction func = global.funcionesNativas.get(nombre);
            // Si tus otras funciones nativas no necesitan el contexto, 
            // solo pasamos los args como antes.
            return func.call(args);
        }
        
        return null;
    }

    @FunctionalInterface
    public interface NativeFunction {
        Object call(List<Object> args);
    }
    
    // 2. Soporte para NodoFuncionNativa (Ejecución vía AST robusto)
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

        // Registro en el mapa de funciones con nuestra implementación robusta
        global.funciones.put(nombre, new NodoFuncionNativa(nombre));
    }

    // IMPLEMENTACIÓN ROBUSTA DE NODOS NATIVOS 
    public static class NodoFuncionNativa extends NodoFuncion {
        public NodoFuncionNativa(String nombre) {
            super(nombre, new ArrayList<NodoParametro>(), null, null, 0, 0);
        }

        @Override
        public Object execute(Entorno entorno) {
            return null; // Las nativas se invocan con ejecutarCon()
        }

        @Override
        public Object ejecutarCon(Entorno entornoLlamada, List<Object> valores) {
            String nom = this.getNombre();
            try {
                switch (nom) {
                    // --- INTEGRACIÓN DE SALIDA ---
                    case "fmt.Println":
                        StringBuilder sb = new StringBuilder();
                        if (valores != null) {
                            for (int i = 0; i < valores.size(); i++) {
                                Object val = valores.get(i);
                                sb.append(val != null ? val.toString() : "nil");
                                if (i < valores.size() - 1) sb.append(" ");
                            }
                        }
                        sb.append("\n");
                        entornoLlamada.agregarSalida(sb.toString());
                        return null;

                    // --- LÓGICA EXISTENTE ---
                    case "strconv.Atoi":
                        if (valores.size() == 1) {
                            return Integer.parseInt(valores.get(0).toString());
                        }
                        break;
                    case "strconv.ParseFloat":
                        if (valores.size() == 1) {
                            return Double.parseDouble(valores.get(0).toString());
                        }
                        break;
                    case "reflect.TypeOf":
                        if (valores.size() == 1) {
                            Object val = valores.get(0);
                            if (val instanceof Integer) return "int";
                            if (val instanceof Double) return "float64";
                            if (val instanceof String) return "string";
                            if (val instanceof Boolean) return "bool";
                            if (val instanceof Character) return "rune";
                            if (val instanceof List) return "slice";
                            if (val instanceof HashMap) return "struct";
                        }
                        return "desconocido";
                    case "slices.Index":
                        if (valores.size() == 2 && valores.get(0) instanceof List) {
                            List<?> lista = (List<?>) valores.get(0);
                            return lista.indexOf(valores.get(1));
                        }
                        return -1;
                    case "strings.Join":
                        if (valores.size() == 2 && valores.get(0) instanceof List) {
                            List<?> lista = (List<?>) valores.get(0);
                            String separador = valores.get(1).toString();
                            StringBuilder joinSb = new StringBuilder();
                            for (int i = 0; i < lista.size(); i++) {
                                if (i > 0) joinSb.append(separador);
                                joinSb.append(lista.get(i).toString());
                            }
                            return joinSb.toString();
                        }
                        return "";
                }
            } catch (Exception e) {
                // Control de fallos nativos
                if (nom.equals("strconv.Atoi")) return 0;
                if (nom.equals("strconv.ParseFloat")) return 0.0;
            }
            return null;
        }
    }

    // ────────────────────────── STRUCTS ──────────────────────────

    public void registrarStruct(String nombre, NodoStruct struct) {
        Entorno global = getGlobal();
        if (global.structs == null) {
            return; 
        }

        if (global.structs.containsKey(nombre)) {
            ErrorManager.getInstance().agregarSemantico(
                "El struct '" + nombre + "' ya ha sido definido.", 
                struct.getLinea(), struct.getColumna()
            );
            return;
        }
        global.structs.put(nombre, struct);
    }

    public NodoStruct buscarStruct(String nombre) {
        Entorno global = getGlobal();
        if (global.structs == null) return null;
        return global.structs.get(nombre);
    }

    // ────────────────────────── SALIDA Y UTILIDADES ──────────────────────────

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