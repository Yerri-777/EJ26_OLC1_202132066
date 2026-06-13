# GoLite IDE 

**GoLite IDE** es un entorno de desarrollo integrado diseñado específicamente para el compilador del lenguaje GoLite. Incluye un editor de código funcional, una consola de salida interactiva y un generador de reportes automáticos (Tokens, Símbolos y AST).

---

## Requisitos del Sistema

Para garantizar una experiencia fluida y un rendimiento óptimo de la interfaz gráfica y el compilador, tu equipo debe cumplir con lo siguiente:

* **Java:** JDK 17 o superior instalado (Compilado nativamente en JDK 21).
* **Sistema Operativo:** Windows, Linux o macOS.
* **Memoria RAM:** 512 MB mínimo.
* **Resolución de Pantalla:** Mínimo 1024 × 600.

---

##  Iniciar la Aplicación

Puedes ejecutar el entorno de dos maneras, ya sea directamente desde consola o utilizando tu IDE de preferencia. Al iniciar, aparecerá la ventana principal con una pestaña vacía lista para escribir código.

### Opción 1: Ejecución Directa (Recomendado)
Abre tu terminal, navega a la carpeta donde se encuentra el ejecutable y lanza el archivo JAR generado por Maven:

```bash
java -jar target/GoLiteCompiler-1.0-SNAPSHOT-jar-with-dependencies.jar

Opción 2: Desde NetBeans IDE
Si estás desarrollando o testeando el código fuente directamente en NetBeans:

Compila el proyecto presionando F11 (Clean and Build).

Ejecuta el entorno presionando F6 (Run Project).