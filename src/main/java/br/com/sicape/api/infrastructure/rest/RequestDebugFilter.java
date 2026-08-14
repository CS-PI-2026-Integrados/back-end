package br.com.sicape.api.infrastructure.rest;

import java.io.IOException;
import java.util.Collections;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("===== REQUEST =====");
        System.out.println(request.getMethod() + " " + request.getRequestURI());

        Collections.list(request.getHeaderNames())
            .forEach(name ->
                System.out.println(name + ": " + request.getHeader(name))
            );

        Collections.list(request.getParameterNames())
            .forEach(name ->
                System.out.println(name + "=" + request.getParameter(name))
            );

        System.out.println("===================");

        filterChain.doFilter(request, response);
    }
}