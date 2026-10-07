# API de status dos apenados

Use `PUT /api/convicted/{uuid}/status/{status}` para ativar ou inativar um apenado da comarca autenticada.
Os parâmetros são enviados exclusivamente na URL. Não há corpo de requisição nem DTO específico.

```http
PUT /api/convicted/00000000-0000-0000-0000-000000000001/status/INACTIVE
Authorization: Bearer <token>
```

Para reativar, use a mesma rota com `ACTIVE`. A resposta é `200 OK` com o `ConvictedResponse` existente,
incluindo o status atualizado. Repetir o status atual não altera o registro nem a auditoria.

- `409 Conflict`: tentativa de inativar um apenado vinculado a processo ativo.
- `422 Unprocessable Entity`: status inválido na URL.
- `404 Not Found`: apenado inexistente ou pertencente a outra comarca.

A inativação registra a data e o usuário responsável; a reativação limpa esses campos.
Dados, foto e vínculos são preservados. Apenados inativos continuam acessíveis na listagem,
no GET de detalhes e no GET de foto, mas alterações de dados, processos e foto retornam `409 Conflict`.
Reative o apenado antes de editá-lo.

O antigo `DELETE /api/convicted/{uuid}` foi removido e retorna `405 Method Not Allowed`.
Clientes que utilizavam DELETE para inativação devem migrar para o PUT de status.
