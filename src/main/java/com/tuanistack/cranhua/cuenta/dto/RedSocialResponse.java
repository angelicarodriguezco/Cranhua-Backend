package com.tuanistack.cranhua.cuenta.dto;

import com.tuanistack.cranhua.cuenta.RedSocial;
import com.tuanistack.cranhua.cuenta.TipoRedSocial;

public record RedSocialResponse(TipoRedSocial tipo, String valor) {

  public static RedSocialResponse de(RedSocial red) {
    return new RedSocialResponse(red.getTipo(), red.getValor());
  }
}
