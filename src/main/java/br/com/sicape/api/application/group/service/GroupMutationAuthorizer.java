package br.com.sicape.api.application.group.service;

import org.springframework.stereotype.Component;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;

@Component
public class GroupMutationAuthorizer {
    public void requireOperator(AuthContext auth) {
        if (auth == null || auth.user() == null) {
            throw new ForbiddenException("Apenas administradores e operadores podem alterar grupos reflexivos.");
        }
        UserRole role = auth.user().getRole();
        if (role != UserRole.ADMIN && role != UserRole.OPERATOR) {
            throw new ForbiddenException("Apenas administradores e operadores podem alterar grupos reflexivos.");
        }
    }
}
