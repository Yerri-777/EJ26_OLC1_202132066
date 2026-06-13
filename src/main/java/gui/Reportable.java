package gui;


public interface Reportable {

    
     //@return String con el reporte en formato texto plano
     
    String generarReporte();

    
     // @return String con el reporte en formato HTML
     
    String generarReporteHTML();

    
     // @return Número de elementos reportados
     
    int totalElementos();

    
     // Limpia los datos del reporte
     
    void reset();
}