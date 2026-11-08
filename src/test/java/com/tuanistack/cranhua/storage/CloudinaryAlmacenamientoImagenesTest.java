package com.tuanistack.cranhua.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class CloudinaryAlmacenamientoImagenesTest {

  private Cloudinary cloudinary;
  private Uploader uploader;
  private CloudinaryAlmacenamientoImagenes almacenamiento;

  @BeforeEach
  void setUp() {
    cloudinary = mock(Cloudinary.class);
    uploader = mock(Uploader.class);
    when(cloudinary.uploader()).thenReturn(uploader);
    almacenamiento =
        new CloudinaryAlmacenamientoImagenes(
            cloudinary, new CloudinaryProperties("demo", "key", "secret"));
  }

  @Test
  void subirFotoDevuelveSecureUrlYUsaCarpetaFotos() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[] {1, 2, 3});
    when(uploader.upload(any(byte[].class), anyMap()))
        .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/foto.jpg"));

    String url = almacenamiento.subirFoto(archivo);

    assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/foto.jpg");
    verify(uploader).upload(any(byte[].class), anyMap());
  }

  @Test
  void subirBannerDevuelveSecureUrl() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "banner.png", "image/png", new byte[] {4, 5, 6});
    when(uploader.upload(any(byte[].class), anyMap()))
        .thenReturn(
            Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/banner.png"));

    String url = almacenamiento.subirBanner(archivo);

    assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/banner.png");
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/pdf", "image/gif", "text/plain"})
  void mimeInvalidoDa400YNoSube(String contentType) throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "archivo.bin", contentType, new byte[] {1});

    assertThatThrownBy(() -> almacenamiento.subirFoto(archivo))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @Test
  void archivoVacioDa400() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

    assertThatThrownBy(() -> almacenamiento.subirFoto(archivo))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void contentTypeNuloOVacioDa400(String contentType) throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "foto.jpg", contentType, new byte[] {1});

    assertThatThrownBy(() -> almacenamiento.subirBanner(archivo))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @Test
  void archivoMayorA5MbDa400() throws Exception {
    byte[] grande = new byte[(int) CloudinaryAlmacenamientoImagenes.TAMANO_MAXIMO_BYTES + 1];
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "grande.webp", "image/webp", grande);

    assertThatThrownBy(() -> almacenamiento.subirFoto(archivo))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException status = (ResponseStatusException) ex;
          assertThat(status.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(status.getReason()).contains("5 MB");
        });

    verify(uploader, never()).upload(any(byte[].class), anyMap());
  }

  @Test
  void aceptaJpegPngWebpSinImportarMayusculas() throws Exception {
    when(uploader.upload(any(byte[].class), anyMap()))
        .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/ok.jpg"));

    for (String tipo : new String[] {"image/jpeg", "IMAGE/PNG", "Image/WebP"}) {
      MockMultipartFile archivo = new MockMultipartFile("archivo", "img", tipo, new byte[] {1, 2});
      assertThat(almacenamiento.subirFoto(archivo)).startsWith("https://");
    }
  }
}
