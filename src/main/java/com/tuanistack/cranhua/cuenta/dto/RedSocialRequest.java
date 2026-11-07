package com.tuanistack.cranhua.cuenta.dto;

import com.tuanistack.cranhua.cuenta.TipoRedSocial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedSocialRequest(
    @NotNull(message = "Indicá el tipo de red social.") TipoRedSocial tipo,
    @NotBlank(message = "Indicá el valor de la red social.")
        @Size(
            max = 200,
            message = "El valor de la red social no puede tener más de 200 caracteres.")
        String valor) {}
