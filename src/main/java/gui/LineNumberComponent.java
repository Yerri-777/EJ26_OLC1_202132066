package gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class LineNumberComponent extends JComponent {
    private final JTextArea textArea;

    public LineNumberComponent(JTextArea textArea) {
        this.textArea = textArea;
        setPreferredSize(new Dimension(40, 0));
        // Se actualiza cuando cambia el documento
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        // Se actualiza cuando cambia el scroll
        textArea.addCaretListener(e -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.GRAY);
        
        FontMetrics fontMetrics = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = fontMetrics.getHeight();
        int fontAscent = fontMetrics.getAscent();

        // Calcular la primera y última línea visible para no dibujar de más
        int startLine = textArea.viewToModel2D(new Point(0, 0));
        int endLine = textArea.viewToModel2D(new Point(0, getHeight()));

        try {
            for (int i = 0; i < textArea.getLineCount(); i++) {
                Rectangle2D rect = textArea.modelToView2D(textArea.getLineStartOffset(i));
                
                if (rect != null) {
                    // CORRECCIÓN: Se agrega casteo a (int) y se remueve getViewport()
                    int y = (int) rect.getY() + fontAscent;
                    
                    if (y > 0 && y < getHeight() + fontAscent) {
                        g.drawString(String.valueOf(i + 1), 5, y);
                    }
                }
            }
        } catch (Exception e) {}
    }
}