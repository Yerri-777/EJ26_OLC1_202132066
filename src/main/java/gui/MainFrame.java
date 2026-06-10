package gui;

import compilador.Compiler;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

/**
 * MainFrame — Ventana principal del IDE GoLite.
 *
 * Estructura visual (según boceto del PDF):
 * ┌─────────────────────────────────────────────────────┐
 * │  [Archivo]  [Herramientas]  [Reportes]  [▶ Ejecutar]│  ← Menú + toolbar
 * ├─────────────────────────────────────────────────────┤
 * │  [main.glt ×]  [otro.glt ×]                         │  ← Tabs editor
 * │                                                     │
 * │    Editor de código con número de líneas            │
 * │                                                     │
 * ├─────────────────────────────────────────────────────┤
 * │  [Consola]                                          │  ← Tab consola
 * │  > Ejecutando...                                    │
 * └─────────────────────────────────────────────────────┘
 */
public class MainFrame extends JFrame {

    // ─── Componentes principales ───────────────────────────────────────────────
    private JTabbedPane  tabbedEditor;   // Tabs con archivos abiertos
    private ConsolaPanel consolaPanel;   // Panel inferior de consola
    private JSplitPane   splitPane;      // Divide editor y consola

    // ─── Contador para nuevos archivos ─────────────────────────────────────────
    private int contadorNuevo = 1;

    // ──────────────────────────────────────────────────────────────────────────
    public MainFrame() {
        super("GoLite IDE");
        configurarVentana();
        construirUI();
        abrirNuevoArchivo(); // Empieza con un archivo vacío
        setVisible(true);
    }

    // ─── Configuración base de la ventana ─────────────────────────────────────

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // Icono de la aplicación (si existe en resources)
        try {
            // setIconImage(ImageIO.read(getClass().getResource("/icon.png")));
        } catch (Exception ignored) {}

        // Look and Feel del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
    }

    // ─── Construcción de la interfaz ───────────────────────────────────────────

    private void construirUI() {
        setJMenuBar(crearMenuBar());

        // Panel superior: toolbar con botón Ejecutar
        JPanel toolbar = crearToolbar();

        // Área de tabs del editor
        tabbedEditor = new JTabbedPane();
        tabbedEditor.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Consola inferior
        consolaPanel = new ConsolaPanel();

        // SplitPane vertical: editor arriba, consola abajo
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedEditor, consolaPanel);
        splitPane.setResizeWeight(0.70);          // 70% editor, 30% consola
        splitPane.setDividerSize(5);
        splitPane.setOneTouchExpandable(true);

        // Layout principal
        setLayout(new BorderLayout());
        add(toolbar,    BorderLayout.NORTH);
        add(splitPane,  BorderLayout.CENTER);
    }

    // ─── Menú principal ────────────────────────────────────────────────────────

    private JMenuBar crearMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(crearMenuArchivo());
        menuBar.add(crearMenuHerramientas());
        menuBar.add(crearMenuReportes());
        menuBar.add(crearMenuAyuda());

        return menuBar;
    }

    private JMenu crearMenuArchivo() {
        JMenu menu = new JMenu("Archivo");
        menu.setMnemonic(KeyEvent.VK_A);

        JMenuItem itemNuevo  = new JMenuItem("Nuevo",  KeyEvent.VK_N);
        JMenuItem itemAbrir  = new JMenuItem("Abrir…", KeyEvent.VK_O);
        JMenuItem itemGuardar = new JMenuItem("Guardar", KeyEvent.VK_S);
        JMenuItem itemGuardarComo = new JMenuItem("Guardar como…");
        JMenuItem itemCerrar = new JMenuItem("Cerrar pestaña");
        JMenuItem itemSalir  = new JMenuItem("Salir");

        itemNuevo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        itemAbrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        itemGuardar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        itemNuevo.addActionListener(e  -> abrirNuevoArchivo());
        itemAbrir.addActionListener(e  -> abrirArchivo());
        itemGuardar.addActionListener(e -> guardarArchivo());
        itemGuardarComo.addActionListener(e -> guardarArchivoComo());
        itemCerrar.addActionListener(e -> cerrarPestanaActual());
        itemSalir.addActionListener(e  -> System.exit(0));

        menu.add(itemNuevo);
        menu.add(itemAbrir);
        menu.addSeparator();
        menu.add(itemGuardar);
        menu.add(itemGuardarComo);
        menu.addSeparator();
        menu.add(itemCerrar);
        menu.addSeparator();
        menu.add(itemSalir);

        return menu;
    }

    private JMenu crearMenuHerramientas() {
        JMenu menu = new JMenu("Herramientas");

        JMenuItem itemEjecutar = new JMenuItem("▶ Ejecutar");
        itemEjecutar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        itemEjecutar.addActionListener(e -> ejecutar());

        menu.add(itemEjecutar);
        return menu;
    }

    private JMenu crearMenuReportes() {
        JMenu menu = new JMenu("Reportes");

        JMenuItem itemErrores = new JMenuItem("Reporte de Errores");
        JMenuItem itemTokens  = new JMenuItem("Tabla de Tokens");
        // Preparados para Día 2/4:
        JMenuItem itemSimbolos = new JMenuItem("Tabla de Símbolos  (Fase 2)");
        JMenuItem itemAST      = new JMenuItem("Reporte AST        (Fase 2)");

        itemErrores.addActionListener(e -> mostrarReporteErrores());
        itemTokens.addActionListener(e  -> mostrarReporteTokens());
        itemSimbolos.setEnabled(false);
        itemAST.setEnabled(false);

        menu.add(itemErrores);
        menu.add(itemTokens);
        menu.addSeparator();
        menu.add(itemSimbolos);
        menu.add(itemAST);

        return menu;
    }

    private JMenu crearMenuAyuda() {
        JMenu menu = new JMenu("Ayuda");
        JMenuItem itemSobre = new JMenuItem("Sobre GoLite IDE");
        itemSobre.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "GoLite IDE\nOrganización de Lenguajes y Compiladores 1\nUSAC — Escuela de Vacaciones Junio 2026",
            "Sobre GoLite IDE", JOptionPane.INFORMATION_MESSAGE));
        menu.add(itemSobre);
        return menu;
    }

    // ─── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel crearToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        JButton btnEjecutar = new JButton("▶ Ejecutar");
        btnEjecutar.setBackground(new Color(37, 139, 90));
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFocusPainted(false);
        btnEjecutar.setFont(btnEjecutar.getFont().deriveFont(Font.BOLD));
        btnEjecutar.addActionListener(e -> ejecutar());

        toolbar.add(btnEjecutar);
        return toolbar;
    }

    // ─── Acciones de Archivo ───────────────────────────────────────────────────

    /** Abre una nueva pestaña con editor vacío. */
    public void abrirNuevoArchivo() {
        String nombre = "nuevo" + contadorNuevo++ + ".glt";
        EditorTab tab = new EditorTab(nombre, null);
        agregarTab(tab, nombre);
    }

    /** Abre el diálogo para cargar un archivo .glt del disco. */
    private void abrirArchivo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Archivos GoLite (*.glt)", "glt"));
        fc.setDialogTitle("Abrir archivo GoLite");

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try {
                String contenido = Files.readString(archivo.toPath());
                EditorTab tab = new EditorTab(archivo.getName(), archivo);
                tab.setContenido(contenido);
                agregarTab(tab, archivo.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el archivo:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Guarda el archivo de la pestaña activa. */
    private void guardarArchivo() {
        EditorTab tab = getTabActivo();
        if (tab == null) return;

        if (tab.getArchivo() == null) {
            guardarArchivoComo();
        } else {
            try {
                Files.writeString(tab.getArchivo().toPath(), tab.getContenido());
                tab.marcarGuardado();
                actualizarTituloTab();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "No se pudo guardar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Guarda como nuevo archivo .glt. */
    private void guardarArchivoComo() {
        EditorTab tab = getTabActivo();
        if (tab == null) return;

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Archivos GoLite (*.glt)", "glt"));
        fc.setDialogTitle("Guardar como");
        fc.setSelectedFile(new File(tab.getNombre()));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            // Asegurar extensión .glt
            if (!archivo.getName().endsWith(".glt")) {
                archivo = new File(archivo.getAbsolutePath() + ".glt");
            }
            try {
                Files.writeString(archivo.toPath(), tab.getContenido());
                tab.setArchivo(archivo);
                tab.marcarGuardado();
                tabbedEditor.setTitleAt(tabbedEditor.getSelectedIndex(), archivo.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "No se pudo guardar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Cierra la pestaña activa (con confirmación si hay cambios sin guardar). */
    private void cerrarPestanaActual() {
        int idx = tabbedEditor.getSelectedIndex();
        if (idx == -1) return;

        EditorTab tab = (EditorTab) tabbedEditor.getComponentAt(idx);
        if (tab.tieneModificaciones()) {
            int resp = JOptionPane.showConfirmDialog(this,
                "El archivo tiene cambios sin guardar.\n¿Deseas guardar antes de cerrar?",
                "Cerrar pestaña",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (resp == JOptionPane.YES_OPTION)    guardarArchivo();
            else if (resp == JOptionPane.CANCEL_OPTION) return;
        }
        tabbedEditor.removeTabAt(idx);
    }

    // ─── Ejecución ─────────────────────────────────────────────────────────────

    /**
     * Botón ▶ Ejecutar: lanza el compilador sobre el contenido del editor activo.
     */
    private void ejecutar() {
        EditorTab tab = getTabActivo();
        if (tab == null) {
            consolaPanel.escribir("[ERROR] No hay archivo activo.\n");
            return;
        }

        String codigo = tab.getContenido();
        if (codigo.isBlank()) {
            consolaPanel.escribir("[WARN] El editor está vacío.\n");
            return;
        }

        consolaPanel.limpiar();
        consolaPanel.escribir("> Ejecutando " + tab.getNombre() + " ...\n");

        // Ejecutar en hilo separado para no bloquear la GUI
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return Compiler.getInstance().compilar(codigo);
            }

            @Override
            protected void done() {
                try {
                    consolaPanel.escribir(get());
                } catch (Exception ex) {
                    consolaPanel.escribir("[ERROR] Fallo inesperado: " + ex.getMessage() + "\n");
                }
            }
        };
        worker.execute();
    }

    // ─── Reportes ──────────────────────────────────────────────────────────────

    private void mostrarReporteErrores() {
        String html = Compiler.getInstance().getReporteErroresHTML();
        ReporteFrame reporte = new ReporteFrame("Reporte de Errores", html);
        reporte.setVisible(true);
    }

    private void mostrarReporteTokens() {
        String html = Compiler.getInstance().getReporteTokensHTML();
        ReporteFrame reporte = new ReporteFrame("Tabla de Tokens", html);
        reporte.setVisible(true);
    }

    // ─── Utilidades internas ───────────────────────────────────────────────────

    private void agregarTab(EditorTab tab, String titulo) {
        tabbedEditor.addTab(titulo, tab);
        tabbedEditor.setSelectedIndex(tabbedEditor.getTabCount() - 1);

        // Se conserva el DocumentListener dinámico para vigilar modificaciones en tiempo real
        tab.areaTexto.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { actualizarTituloTab(tab); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { actualizarTituloTab(tab); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarTituloTab(tab); }
        });
    }

    private EditorTab getTabActivo() {
        int idx = tabbedEditor.getSelectedIndex();
        if (idx == -1) return null;
        return (EditorTab) tabbedEditor.getComponentAt(idx);
    }

    private void actualizarTituloTab(EditorTab tab) {
        int idx = tabbedEditor.indexOfComponent(tab);
        if (idx == -1) return;

        String titulo = tab.getNombre();
        if (tab.tieneModificaciones()) {
            titulo = titulo + " *";  // Se añade el asterisco si hay cambios pendientes
        }
        tabbedEditor.setTitleAt(idx, titulo);
    }

    private void actualizarTituloTab() {
        int idx = tabbedEditor.getSelectedIndex();
        if (idx == -1) return;
        EditorTab tab = (EditorTab) tabbedEditor.getComponentAt(idx);
        actualizarTituloTab(tab);
    }
}