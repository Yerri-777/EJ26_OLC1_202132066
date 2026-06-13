package gui;

import javax.swing.*;


public abstract class ComponenteGUI extends JPanel {

    protected static final int ANCHO_DEFECTO  = 1200;
    protected static final int ALTO_DEFECTO   = 750;

   
    public ComponenteGUI() {
        setLayout(null);  
        inicializar();
    }

   
    private void inicializar() {
        configurar();
        construirUI();
    }

    
    protected abstract void configurar();

   
    protected abstract void construirUI();

  
    public abstract void refrescar();
}