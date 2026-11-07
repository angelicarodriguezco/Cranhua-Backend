package com.tuanistack.cranhua.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificadorGoogleIdToken implements VerificadorGoogle {

  private final GoogleIdTokenVerifier verifier;

  // Revisa firma, emisor de Google, vencimiento y que el token sea para nuestro client id
  public VerificadorGoogleIdToken(@Value("${app.google.client-id}") String clientId) {
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(clientId))
            .build();
  }

  @Override
  public Optional<DatosGoogle> verificar(String credencial) {
    try {
      GoogleIdToken token = verifier.verify(credencial);
      if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
        return Optional.empty();
      }
      GoogleIdToken.Payload datos = token.getPayload();
      return Optional.of(
          new DatosGoogle(
              datos.getSubject(),
              datos.getEmail(),
              (String) datos.get("name"),
              (String) datos.get("picture")));
    } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
