# Retenção de capturas — desenvolvimento local

A partir do schema10, os detalhes administrativos podem expirar sem apagar a proteção contra replay ou os contadores por período. Fazer backup antes de migrar; não abrir um banco schema10 com binários anteriores.

```json
{
  "capture_history": {
    "enabled": true,
    "retention_days": 90,
    "cleanup": {
      "enabled": true,
      "batch_size": 1000
    }
  }
}
```

Defaults são os valores acima, inclusive para configs antigas. `retention_days`: 1–36500; `batch_size`: 1–10000. Campos desconhecidos/duplicados e tipos incorretos são rejeitados. Reload aplica a nova política às próximas operações; lote já enfileirado termina com a política que o agendou.

`capture_history.enabled=false` deixa de armazenar novos detalhes de capturas verificadas. Não desliga estatísticas, deduplicação, prova de origem ou Shiny. Eventos UNKNOWN continuam detalhados para auditoria, pois sua origem não foi comprovada. Esse campo não apaga detalhes antigos imediatamente; a política de limpeza continua independente.

## Recibos permanentes

`capture_receipts` guarda chave do evento (identidades Pokébola/Pokémon), UUID do jogador, instante em milissegundos e indicador de origem verificada. O recibo, o histórico opcional, os contadores e a projeção Shiny são gravados na mesma transação SQLite. Não existe transação distribuída com o motor do jogo.

A migração9→10 copia apenas eventos presentes. Não inventa capturas, reconstrói eventos já perdidos ou altera os totais antigos. Um replay consulta o recibo antes de consumir provas de origem e não pode promover um evento UNKNOWN antigo a verificado. Recibos não expiram.

Hoje/semana/mês e contagem de origem não comprovada consultam os recibos indexados, inclusive após remover detalhes. Isso preserva a semântica do fuso configurado, mudanças de fuso e exclusão de instantes futuros. São recibos mínimos por evento, não agregados diários: ainda há crescimento permanente proporcional ao número de capturas. Planejamento de capacidade/carga continua pendente.

## Limpeza automática

A primeira limpeza ocorre após readiness do Dex; depois, no máximo um lote por minuto, sem acumular lotes simultâneos. SQLite roda no worker, sem acessar o banco na thread do servidor. Falha faz rollback e permite nova tentativa no intervalo seguinte. Reinício retoma a política automaticamente.

Somente detalhes verificados estritamente anteriores a `agora - retention_days × 24 horas` são elegíveis. O recibo deve existir e concordar com jogador, instante e verificação. O lote é transacional, limitado e ordenado por data/chave. UNKNOWN e registros sem recibo consistente são preservados. Não há VACUUM automático nem promessa de redução imediata do arquivo SQLite.

A limpeza não remove recibos, agregados, provas de spawn, projeções Shiny, regras de categoria, operações/revisões de recompensas ou auditorias. Competições/revisão de capturas ainda não foram implementadas: antes de acrescentá-las, integrar suas retenções obrigatórias/holds à elegibilidade da limpeza. Não ativar tabelas novas dependentes de capture_events sem essa integração.

## Validação

Cinco testes CaptureReceiptsTest: remoção/reinício/replay, fuso e períodos, origem desconhecida, migração9→10/reabertura, rollback de escrita e de migração. Quatro CaptureRetentionTest: lote/cutoff/auditoria, limpeza desativada/recibo inconsistente, histórico desativado sem perder progresso, parser estrito/defaults.

Cenário nativo reproduzível: com servidor descartável parado, mundo `capture-retention-validation` e banco schema9/10 vazio, executar `python3 tests/integration/retention.py seed <raiz>`. O script cria cinco eventos explicitamente sintéticos e configura retenção de 90 dias. Iniciar o servidor, aguardar readiness e a limpeza e executar `python3 tests/integration/retention.py verify <raiz>`. Esperado: três detalhes expirados removidos, um UNKNOWN e um recente mantidos, cinco recibos e quatro capturas permanentes intactos. Não executar em dados de usuários. Esses eventos não comprovam capturas no jogo; verificam migração, agendamento e persistência da limpeza.

Resultado: 337 JUnit sem falhas/erros/skips; migração9→10, limpeza automática e reinício em servidor Loom aprovados. Três detalhes expirados removidos; cinco recibos e quatro capturas preservados. [Evidência](validation/phase4-retention.json). Servidores encerrados normalmente, segredo removido, RCON desativado e SQLite íntegro.
