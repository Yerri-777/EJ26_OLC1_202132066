package compilador;

import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TextAreaOutputStream extends OutputStream {
    private JTextArea textArea;

    public TextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        // Redirige los bytes al área de texto de forma segura para Swing
        SwingUtilities.invokeLater(() -> {
            textArea.append(String.valueOf((char)b));
        });
    }
}