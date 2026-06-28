package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;

public class EditorTab extends JPanel {

    // ─── Estado del archivo ───
    private String nombre;
    private File   archivo;
    private boolean modificado = false;

    // ─── Componentes de UI ───
    private final JTextArea areaTexto;
    private final LineNumberPanel numerosLinea;

    public EditorTab(String nombre, File archivo) {
        this.nombre  = nombre;
        this.archivo = archivo;

        setLayout(new BorderLayout());

        // Configuración del Área de texto 
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

        // Panel lateral para los números de línea
        numerosLinea = new LineNumberPanel(areaTexto);

        // Configuración del ScrollPane
        JScrollPane scroll = new JScrollPane(areaTexto);
        scroll.setRowHeaderView(numerosLinea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    // ─── API pública ───────────────────────────────────────────────────────────

    public String  getNombre()              { return nombre; }
    public File    getArchivo()             { return archivo; }
    public String  getContenido()           { return areaTexto.getText(); }
    public boolean tieneModificaciones()    { return modificado; }

    public void setContenido(String texto) {
        areaTexto.setText(texto);
        areaTexto.setCaretPosition(0);
        modificado = false;
    }

    public void setArchivo(File archivo) {
        this.archivo = archivo;
        this.nombre  = archivo.getName();
    }

    public void marcarGuardado() {
        modificado = false;
    }

    private void marcarModificado() {
        if (!modificado) {
            modificado = true;
        }
    }

    // ─── Panel interno de números de línea consolidado ─────────────────────────

    private static class LineNumberPanel extends JPanel {

        private final JTextArea textArea;
        private static final int ANCHO = 45;

        LineNumberPanel(JTextArea textArea) {
            this.textArea = textArea;
            setBackground(new Color(37, 37, 38));

            // Se actualiza el dibujo y tamaño cuando cambia el texto del documento
            textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e)  { actualizar(); }
                @Override public void removeUpdate(DocumentEvent e)  { actualizar(); }
                @Override public void changedUpdate(DocumentEvent e) { actualizar(); }
            });

            // Se actualiza cuando cambia el scroll o se mueve el cursor (Para repintar la línea actual)
            textArea.addCaretListener(e -> repaint());
        }

        // Le avisa al JScrollPane que el tamaño cambió
        private void actualizar() {
            revalidate();
            repaint();
        }

        // El panel de números siempre tendrá el mismo alto que el área de texto
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(ANCHO, textArea.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            // Antialiasing para suavizar el texto
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            g2.setFont(textArea.getFont());
            int fontAscent = fm.getAscent();

            // Identificar el número de línea actual donde está el cursor
            int lineaActual = 0;
            try {
                int caretPos = textArea.getCaretPosition();
                lineaActual  = textArea.getLineOfOffset(caretPos) + 1;
            } catch (Exception ignored) {}

            // Obtener el área visible para no procesar código que no se ve
            Rectangle clip = g.getClipBounds();
            if (clip == null) return;

            try {
                // Calcular solo la primera y última línea visibles
                int startOffset = textArea.viewToModel2D(new java.awt.geom.Point2D.Double(0, clip.y));
                int endOffset = textArea.viewToModel2D(new java.awt.geom.Point2D.Double(0, clip.y + clip.height));

                int startLine = textArea.getLineOfOffset(startOffset);
                int endLine = textArea.getLineOfOffset(endOffset);

                // Dibujar únicamente las líneas que están en pantalla
                for (int i = startLine; i <= endLine; i++) {
                    Rectangle2D rect = textArea.modelToView2D(textArea.getLineStartOffset(i));
                    
                    if (rect != null) {
                        int y = (int) rect.getY() + fontAscent;
                        int linea = i + 1;
                        
                        // Resaltar el color si es la línea en la que se encuentra el cursor
                        if (linea == lineaActual) {
                            g2.setColor(new Color(200, 200, 200));  
                        } else {
                            g2.setColor(new Color(133, 133, 133));
                        }

                        // Alinear el texto hacia la derecha
                        String numStr = String.valueOf(linea);
                        int x = ANCHO - fm.stringWidth(numStr) - 6;
                        g2.drawString(numStr, x, y);
                    }
                }
            } catch (Exception e) {
                // Silenciamos las excepciones gráficas internas si el componente aún no está renderizado
            }
        }
    }
}