package br.com.sicape.api.application.group.dto.request;

public record CreateGroupRequest (
    String name,
    String description
) {}