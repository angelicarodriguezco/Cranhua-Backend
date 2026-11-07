package com.tuanistack.cranhua.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CorreoRecuperacion {

  private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacion.class);

  private final JavaMailSender mailSender;
  private final String remitente;

  public CorreoRecuperacion(
      JavaMailSender mailSender, @Value("${app.correo.remitente}") String remitente) {
    this.mailSender = mailSender;
    this.remitente = remitente;
  }

  // Asíncrono para que el tiempo de respuesta no delate si el correo tiene cuenta
  @Async
  public void enviarCodigo(String correo, String codigo) {
    SimpleMailMessage mensaje = new SimpleMailMessage();
    mensaje.setFrom(remitente);
    mensaje.setTo(correo);
    mensaje.setSubject("Código para restablecer tu contraseña");
    mensaje.setText(
        """
        Tu código para restablecer la contraseña de Cranhua es: %s

        Vence en 10 minutos. Si no lo pediste, ignorá este correo.
        """
            .formatted(codigo));
    try {
      mailSender.send(mensaje);
    } catch (MailException e) {
      log.error("No se pudo enviar el código de recuperación", e);
    }
  }
}
