# API de agenda do apenado

`GET /convicted/{uuid}/schedule?from=2026-10-01&to=2026-11-01` consulta a agenda na comarca da sessão. As datas civis seguem `America/Sao_Paulo`. O início é inclusivo e o fim é exclusivo, com intervalo de 1 a 31 dias.

| Resposta | Condição |
| --- | --- |
| 200 | Consulta concluída nas duas fontes. A lista fica vazia se não houver eventos no período. |
| 404 | O apenado não existe na comarca autenticada. |
| 422 | UUID ou intervalo inválido. |
| 503 | Uma ou ambas as fontes autoritativas ainda não estão disponíveis. |

```json
[
  {
    "id": "7acb87ab-9cd4-416e-af47-72cc4f119acb",
    "origin": "REFLECTION_GROUP",
    "date": "2026-10-22",
    "time": "14:00:00",
    "title": "Grupo reflexivo",
    "status": "SCHEDULED",
    "process_id": null
  },
  {
    "id": "ef39ed84-5c4c-45f8-8098-942028abaefb",
    "origin": "INDIVIDUAL_APPEARANCE",
    "date": "2026-10-22",
    "time": null,
    "title": "Comparecimento individual",
    "status": "SCHEDULED",
    "process_id": null
  }
]
```

O estado `CANCELLED` identifica um cancelamento e o front-end o exclui das marcações. Uma remarcação usa a data vigente da ocorrência. Os estados descrevem o agendamento, sem inferir presença ou falta.

O endpoint está preparado para receber implementações Spring de `ConvictedScheduleSource`. A fonte de grupos deve consultar ocorrências persistidas e aplicar a vigência do vínculo na data do encontro. A fonte de comparecimentos individuais deve consultar agendamentos persistidos. Cada fonte usa o apenado já validado na comarca e fornece referências processuais somente quando o domínio as possuir. As fontes e as regras de agendamento individual dependem das tarefas de domínio vinculadas à #69.
