package gui;

import javax.swing.*;
import java.awt.*;

/**
 * ConsolaPanel — Área de consola del IDE GoLite.
 *
 * Muestra en tiempo real:
 *  - La salida del programa (fmt.Println)
 *  - Mensajes del compilador ([INFO], [OK], [WARN], [ERROR])
 *  - Resumen de errores
 *
 * El PDF especifica que solo se califica si la salida aparece en la consola.
 */
public class ConsolaPanel extends JPanel {

    private final JTextArea areaConsola;
    private final JButton   btnLimpiar;

    public ConsolaPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));

        // ── Header de la consola ───────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        header.setBackground(new Color(37, 37, 38));

        JLabel lblConsola = new JLabel("Consola");
        lblConsola.setForeground(new Color(212, 212, 212));
        lblConsola.setFont(lblConsola.getFont().deriveFont(Font.BOLD));

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setFont(btnLimpiar.getFont().deriveFont(11f));
        btnLimpiar.addActionListener(e -> limpiar());

        header.add(lblConsola);
        header.add(Box.createHorizontalStrut(10));
        header.add(btnLimpiar);

        // ── Área de texto ──────────────────────────────────────────────────
        areaConsola = new JTextArea();
        areaConsola.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaConsola.setEditable(false);
        areaConsola.setBackground(new Color(12, 12, 12));
        areaConsola.setForeground(new Color(204, 204, 204));
        areaConsola.setCaretColor(Color.WHITE);
        areaConsola.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(areaConsola);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header,  BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
    }

    // ─── API pública ───────────────────────────────────────────────────────────

    /**
     * Agrega texto a la consola. Hilo-seguro (puede llamarse desde SwingWorker).
     */
    public void escribir(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaConsola.append(texto);
            // Auto-scroll al final
            areaConsola.setCaretPosition(areaConsola.getDocument().getLength());
    
        });
    }

    /**
     * Limpia toda la consola.
     */
    public void limpiar() {
        areaConsola.setText("");
    }
}