package com.tuanistack.cranhua.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryAlmacenamientoImagenes implements AlmacenamientoImagenes {

  static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
  static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

  private static final String CARPETA_FOTOS = "cranhua/fotos";
  private static final String CARPETA_BANNERS = "cranhua/banners";

  private final Cloudinary cloudinary;
  private final CloudinaryProperties propiedades;

  public CloudinaryAlmacenamientoImagenes(Cloudinary cloudinary, CloudinaryProperties propiedades) {
    this.cloudinary = cloudinary;
    this.propiedades = propiedades;
  }

  @Override
  public String subirFoto(MultipartFile archivo) {
    return subir(archivo, CARPETA_FOTOS);
  }

  @Override
  public String subirBanner(MultipartFile archivo) {
    return subir(archivo, CARPETA_BANNERS);
  }

  private String subir(MultipartFile archivo, String carpeta) {
    validar(archivo);
    asegurarCredenciales();

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> resultado =
          cloudinary
              .uploader()
              .upload(
                  archivo.getBytes(),
                  ObjectUtils.asMap("folder", carpeta, "resource_type", "image"));
      Object url = resultado.get("secure_url");
      if (url == null || !StringUtils.hasText(url.toString())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Cloudinary no devolvió una URL válida.");
      }
      return url.toString();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No se pudo leer el archivo de imagen.", e);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "No se pudo subir la imagen a Cloudinary.", e);
    }
  }

  private void validar(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen es obligatoria.");
    }

    String contentType = archivo.getContentType();
    if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WebP.");
    }

    if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La imagen no puede superar los 5 MB.");
    }
  }

  private void asegurarCredenciales() {
    if (!StringUtils.hasText(propiedades.cloudName())
        || !StringUtils.hasText(propiedades.apiKey())
        || !StringUtils.hasText(propiedades.apiSecret())) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Cloudinary no está configurado. Revisá CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y"
              + " CLOUDINARY_API_SECRET.");
    }
  }
}
