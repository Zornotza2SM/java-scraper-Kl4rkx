package com.digi;

import java.io.FileWriter;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Scraper {

    public static void main(String[] args) {

        // ----------------------------------------------------
        // FASE 1: Recolección y Acceso al Dato No Estructurado
        // ----------------------------------------------------
        // 1. Crear el StringBuilder para construir el contenido del CSV
        StringBuilder csvData = new StringBuilder();
        csvData.append("Nombre;Descripcion;Precio_Euros;URL_Producto;URL_Imagen\n"); // Cabecera del CSV

        try {
            // Carga la página (sin el fragmento # ya que Jsoup no procesa JavaScript)
            String url = "https://tienda.productostipicosregionales.es/12-gastronomia";
            
            System.out.println("Conectando al sitio web...");
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://www.google.com")
                    .timeout(30000)  // 30 segundos de timeout
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            System.out.println("Página cargada exitosamente");

            // 2. Seleccionar todos los contenedores de producto dentro del <ul> especificado
            // Primero intentamos con el selector más específico
            Elements productos = doc.select("ul.product_list.grid.row li.ajax_block_product");

            // Si no encuentra productos, intentamos con un selector más general
            if (productos.isEmpty()) {
                System.out.println("No se encontraron productos con el selector específico, intentando selector alternativo...");
                productos = doc.select("li.ajax_block_product");
            }

            if (productos.isEmpty()) {
                System.out.println("Intentando otro selector...");
                productos = doc.select(".product-container");
            }

            System.out.println("Productos encontrados: " + productos.size());
            
            // Debug: Mostrar información del título de la página
            if (productos.isEmpty()) {
                System.out.println("\n--- DEBUG ---");
                System.out.println("Título de la página: " + doc.title());
                System.out.println("Longitud del HTML: " + doc.html().length() + " caracteres");
                System.out.println("--- Fin del debug ---\n");
            }

            // ----------------------------------------------------
            // FASE 2: Preprocesamiento (Extracción y Limpieza)
            // ----------------------------------------------------
            for (Element producto : productos) {

                // Extraer el nombre del producto
                Element nombreElement = producto.selectFirst("h5[itemprop=name] a.product-name");
                String nombre = nombreElement != null ? nombreElement.text().trim() : "N/A";

                // Extraer la descripción del producto
                Element descripcionElement = producto.selectFirst("p.product-desc[itemprop=description]");
                String descripcion = descripcionElement != null ? descripcionElement.text().trim() : "N/A";

                // Extraer el precio
                Element precioElement = producto.selectFirst("span.price.product-price");
                String precioTexto = precioElement != null ? precioElement.text().trim() : "0";

                // Extraer la URL del producto
                Element urlElement = producto.selectFirst("a.product-name");
                String urlProducto = urlElement != null ? urlElement.attr("href") : "N/A";

                // Extraer la URL de la imagen
                Element imagenElement = producto.selectFirst("div.product-image-container img.img-responsive");
                String urlImagen = imagenElement != null ? imagenElement.attr("src") : "N/A";

                // ----------------------------------------------------
                // Limpieza del Dato
                // ----------------------------------------------------
                // 1. Limpiar el precio: eliminar símbolo € y espacios, reemplazar coma por punto
                String precioLimpio = precioTexto
                        .replace("€", "")
                        .replace(" ", "")
                        .replace(",", ".")
                        .trim();

                // 2. Limpiar la descripción: eliminar saltos de línea y tabulaciones
                descripcion = descripcion.replace("\n", " ").replace("\r", " ").replace(";", ",");

                // 3. Limpiar el nombre: reemplazar punto y coma por coma (para no romper el CSV)
                nombre = nombre.replace(";", ",");

                // 3. Estructurar el dato: Añadir al StringBuilder en formato CSV
                csvData.append(nombre).append(";")
                       .append(descripcion).append(";")
                       .append(precioLimpio).append(";")
                       .append(urlProducto).append(";")
                       .append(urlImagen).append("\n");

                // Mostrar progreso
                System.out.println("Procesado: " + nombre + " - " + precioLimpio + "€");
            }

            // ----------------------------------------------------
            // FASE 3: Guardar el archivo (Resultado Estructurado)
            // ----------------------------------------------------
            System.out.println("\nExtracción completada. Guardando datos en productos_limpios.csv");
            FileWriter writer = new FileWriter("productos_limpios.csv");
            writer.write(csvData.toString());
            writer.close();
            System.out.println("Proceso finalizado con éxito. Revisa el archivo productos_limpios.csv");

        } catch (IOException e) {
            System.err.println("Error al acceder a la página o al escribir el CSV.");
            e.printStackTrace();
        }
    }
}
