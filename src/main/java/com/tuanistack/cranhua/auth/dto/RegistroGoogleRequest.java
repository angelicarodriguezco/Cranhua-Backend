package com.tuanistack.cranhua.auth.dto;

import com.tuanistack.cranhua.auth.RolesValidos;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RegistroGoogleRequest(
    @NotBlank(message = "Falta la credencial de Google.") String credencial,
    @RolesValidos List<String> roles) {}
