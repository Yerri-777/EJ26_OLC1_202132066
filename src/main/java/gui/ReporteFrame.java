package gui;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

/**
 * ReporteFrame — Ventana flotante para mostrar los reportes del compilador.
 *
 * Reutilizable: recibe un título y HTML ya generado.
 * Soporta los 4 reportes del PDF:
 *   - Reporte de Errores
 *   - Tabla de Tokens
 *   - Tabla de Símbolos (Fase 2)
 *   - Reporte de AST    (Fase 2)
 */
public class ReporteFrame extends JFrame {

    public ReporteFrame(String titulo, String html) {
        super(titulo);

        setSize(900, 600);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ── JEditorPane para renderizar HTML ───────────────────────────────
        JEditorPane visor = new JEditorPane();
        visor.setEditorKit(new HTMLEditorKit());
        visor.setEditable(false);
        visor.setText(html);
        visor.setCaretPosition(0);  // Scroll al inicio

        JScrollPane scroll = new JScrollPane(visor);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // ── Botón cerrar ───────────────────────────────────────────────────
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCerrar);

        setLayout(new BorderLayout());
        add(scroll,   BorderLayout.CENTER);
        add(botones,  BorderLayout.SOUTH);
    }
}