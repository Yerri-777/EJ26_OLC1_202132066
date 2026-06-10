package gui;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * ReporteFrame — Ventana flotante para mostrar los reportes del compilador.
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
        HTMLEditorKit kit = new HTMLEditorKit();
        visor.setEditorKit(kit);
        
        // Mejora visual: CSS básico para que las tablas y textos se vean bien
        StyleSheet style = kit.getStyleSheet();
        style.addRule("body { font-family: sans-serif; padding: 10px; }");
        style.addRule("table { border-collapse: collapse; width: 100%; }");
        style.addRule("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }");
        style.addRule("th { background-color: #f2f2f2; }");

        visor.setEditable(false);
        visor.setText(html);
        visor.setCaretPosition(0); 

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