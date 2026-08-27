package br.com.sicape.api.application.oauth;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.entity.User;

public record AuthContext(
    User user,
    JudicialDistrict district,
    Session session
) {}
