package gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;


  //Muestra en tiempo real:
   // La salida del programa (fmt.Println)
   // Mensajes del compilador ([INFO], [OK], [WARN], [ERROR])
   // Resumen de errores

public class ConsolaPanel extends JPanel {

    private final JTextPane areaConsola;
    private final JButton btnLimpiar;

    public ConsolaPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));

        
        // Header
     
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

      
        // Área de consola
    
        areaConsola = new JTextPane();
        areaConsola.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaConsola.setEditable(false);
        areaConsola.setBackground(new Color(12, 12, 12));
        areaConsola.setCaretColor(Color.WHITE);
        areaConsola.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(areaConsola);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // API Pública
 


    public void escribir(String texto) {

        SwingUtilities.invokeLater(() -> {

            try {

                StyledDocument doc = areaConsola.getStyledDocument();

                Style estilo = areaConsola.addStyle("estiloConsola", null);

                // Color por defecto
                Color color = new Color(204, 204, 204);

                if (texto.contains("[ERROR]") || texto.contains("[CRITICAL]")) {
                    color = new Color(255, 85, 85); // Rojo
                } else if (texto.contains("[WARN]")) {
                    color = new Color(255, 255, 85); // Amarillo
                } else if (texto.contains("[OK]")) {
                    color = new Color(85, 255, 85); // Verde
                } else if (texto.contains("[INFO]") || texto.contains("Ejecutando")) {
                    color = new Color(85, 170, 255); // Azul claro
                }

                StyleConstants.setForeground(estilo, color);

                doc.insertString(doc.getLength(), texto, estilo);

                // Auto-scroll
                areaConsola.setCaretPosition(doc.getLength());

            } catch (BadLocationException e) {
                e.printStackTrace();
            }

        });
    }

    
      //Limpia completamente la consola.
     
    public void limpiar() {
        SwingUtilities.invokeLater(() -> areaConsola.setText(""));
    }
}