package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

/**
 * EditorTab — Panel de un archivo abierto en el IDE.
 * * Contiene:
 * - JTextArea con el código fuente
 * - Columna de números de línea sincronizada
 * - Tracking de modificaciones automático con feedback en el título del Tab
 */
public class EditorTab extends JPanel {

    // ─── Estado del archivo ────────────────────────────────────────────────────
    private String nombre;
    private File   archivo;
    private boolean modificado = false;

    // ─── Componentes de UI ─────────────────────────────────────────────────────
    protected final JTextArea  areaTexto;
    private LineNumberPanel numerosLinea;

    // ──────────────────────────────────────────────────────────────────────────
    public EditorTab(String nombre, File archivo) {
        this.nombre  = nombre;
        this.archivo = archivo;

        setLayout(new BorderLayout());

        // ── Área de texto ──────────────────────────────────────────────────
        areaTexto = new JTextArea();
        areaTexto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaTexto.setTabSize(4);
        areaTexto.setLineWrap(false);
        areaTexto.setBackground(new Color(30, 30, 30));
        areaTexto.setForeground(new Color(212, 212, 212));
        areaTexto.setCaretColor(Color.WHITE);
        areaTexto.setSelectionColor(new Color(38, 79, 120));

        // Escuchar cambios para marcar el archivo como modificado
        areaTexto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { marcarModificado(); }
            @Override public void removeUpdate(DocumentEvent e)  { marcarModificado(); }
            @Override public void changedUpdate(DocumentEvent e) { marcarModificado(); }
        });

        // ── Números de línea ───────────────────────────────────────────────
        numerosLinea = new LineNumberPanel(areaTexto);
        
        // Repintar los números de línea cuando el cursor se mueva
        areaTexto.addCaretListener(e -> numerosLinea.repaint());

        // ── ScrollPane ─────────────────────────────────────────────────────
        JScrollPane scroll = new JScrollPane(areaTexto);
        scroll.setRowHeaderView(numerosLinea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    // ─── API pública ───────────────────────────────────────────────────────────

    public String  getNombre()               { return nombre;   }
    public File    getArchivo()              { return archivo;  }
    public String  getContenido()            { return areaTexto.getText(); }
    public boolean tieneModificaciones()     { return modificado; }

    public void setContenido(String texto) {
        areaTexto.setText(texto);
        areaTexto.setCaretPosition(0);
        modificado = false;
        actualizarTituloEnTab(); // Limpiar el asterisco
    }

    public void setArchivo(File archivo) {
        this.archivo = archivo;
        this.nombre  = archivo.getName();
        actualizarTituloEnTab();
    }

    public void marcarGuardado() {
        modificado = false;
        actualizarTituloEnTab();
    }

    private void marcarModificado() {
        if (!modificado) {
            modificado = true;
            actualizarTituloEnTab();
        }
    }

    /**
     * Notifica a la pestaña padre que debe actualizar el título con asterisco.
     * Busca dinámicamente el JTabbedPane contenedor.
     */
    private void actualizarTituloEnTab() {
        Component t = this.getParent();
        while (t != null && !(t instanceof JTabbedPane)) {
            t = t.getParent();
        }
        if (t instanceof JTabbedPane tabbedPane) {
            int idx = tabbedPane.indexOfComponent(this);
            if (idx != -1) {
                String titulo = nombre + (modificado ? " *" : "");
                tabbedPane.setTitleAt(idx, titulo);
            }
        }
    }

    // ─── Panel interno de números de línea ─────────────────────────────────────

    private static class LineNumberPanel extends JPanel {
        private final JTextArea textArea;
        private static final int ANCHO = 45;

        LineNumberPanel(JTextArea textArea) {
            this.textArea = textArea;
            setPreferredSize(new Dimension(ANCHO, 0));
            setBackground(new Color(37, 37, 38));

            textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e)  { repaint(); }
                @Override public void removeUpdate(DocumentEvent e)  { repaint(); }
                @Override public void changedUpdate(DocumentEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            g2.setFont(textArea.getFont());
            g2.setColor(new Color(133, 133, 133));

            int alturaLinea = fm.getHeight();
            int totalLineas = textArea.getLineCount();

            Rectangle clip = g2.getClipBounds();
            int inicioLinea = clip.y / alturaLinea;
            int finLinea    = Math.min(totalLineas, (clip.y + clip.height) / alturaLinea + 1);

            int lineaActual = 0;
            try {
                int caretPos = textArea.getCaretPosition();
                lineaActual  = textArea.getLineOfOffset(caretPos) + 1;
            } catch (Exception ignored) {}

            for (int i = inicioLinea; i < finLinea; i++) {
                int linea = i + 1;
                int y = i * alturaLinea + fm.getAscent() + textArea.getInsets().top;

                if (linea == lineaActual) {
                    g2.setColor(new Color(200, 200, 200)); 
                } else {
                    g2.setColor(new Color(133, 133, 133));
                }

                String numStr = String.valueOf(linea);
                int x = ANCHO - fm.stringWidth(numStr) - 6;
                g2.drawString(numStr, x, y);
            }
        }
    }
}