package com.tuanistack.cranhua.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoImagenes {

  String subirFoto(MultipartFile archivo);

  String subirBanner(MultipartFile archivo);
}
