# SQLite — migração 1

Caminho atual: `config/shirokaidex/data/shirokaidex.sqlite`, relativo ao servidor. O diretório de configuração deve permanecer persistente. Não coloque este arquivo no Git.

Uma conexão é confinada a um executor próprio. Acesso SQL usa statements parametrizados; nenhum objeto Minecraft é enviado ao worker. WAL, foreign keys, busy timeout de 5 segundos e synchronous FULL são habilitados.

A migração `V001__foundation.sql` cria:

- `schema_migrations(version, applied_at)`;
- `players(uuid, last_known_name, first_join_at, last_seen_at)`;
- `player_settings(player_uuid, language)` (estrutura preparada; preferências individuais ainda não expostas).

O `PRAGMA user_version` é `1`. Migração e histórico são gravados na mesma transação; falha reverte alterações. Versões futuras são recusadas sem downgrade. Login atualiza nome/último acesso preservando UUID e primeiro acesso. A Pokédex oficial não é copiada para esse banco.

Testes verificaram 100 atualizações enfileiradas para o mesmo UUID, reabertura, rollback em conflito de esquema, recusa de versão futura e rejeição após fechamento. No servidor real, reinício preservou os registros e uma única migração, com `integrity_check=ok`.

Para backup manual seguro nesta fase, desligue o servidor normalmente e copie todo o diretório `data` (incluindo arquivos WAL/SHM se existirem). Não copie somente o `.sqlite` de um servidor ativo. Comandos de backup/restauração e demais tabelas do prompt ainda não foram implementados.

## Migração 2 — versões de categorias (local)

Tabela `category_rule_versions(fingerprint, format_version, rules_json, first_observed_at)`: snapshots canônicos SHA-256, inserção idempotente e conteúdo antigo preservado. Migração incremental transacional; banco com versão futura é recusado sem downgrade. Sem vínculo com capturas/competições ainda. [Semântica e limites](CATEGORIES.md#versões-persistidas-das-regras-trabalho-local).

## Migração 3 — journal de resgates (local)

reward_operations (único jogador/recompensa, plano imutável e estado), reward_steps (intenção/resultado por índice) e reward_audit (ator/ação/razão/horário). Operações e auditoria usam transação no mesmo worker. Recuperação de PROCESSING para NEEDS_REVIEW é transacional; efeitos externos nunca são repetidos automaticamente. [Regras](REWARDS.md).

## Migração 4 — desbloqueios permanentes (local)

reward_unlocks guarda a primeira evidência de desbloqueio por UUID/ID, plano observado, quantidade/total e horário. Lotes são transacionais; repetição não modifica a primeira observação. Sem alteração de estatísticas oficiais ou eventos históricos.

## Migração 5 — itens salvos (local)

saved_items contém modelo/protótipo SNBT, quantidade e SHA-256; saved_item_audit preserva conteúdo anterior/novo, ator e horário. Mutação e auditoria em transação; corrupção impede uso mas pode ser reparada explicitamente com preservação da evidência. Referências resolvidas ficam copiadas nos planos/backup. [Limites](SAVED_ITEMS.md). Bancos novos não são compatíveis com JARs antigos que só suportam schemas anteriores.

## Etapas CURRENCY

Sem alteração de schema: reward_operations.plan_json inclui opcionalmente currencies, enquanto reward_steps continua genérico. Ordem fixa: itens, Pokémon e moedas; intenção precede depósito, e ack exige sucesso da transação/save/leitura nativa. JSONs históricos sem currencies não mudam. Não existe ID idempotente externo fornecido pelo chamador; PROCESSING/efeitos incertos continuam em revisão, sem retry automático.

## Migração 6 — capturas observadas e agregados permanentes

`capture_events` tem chave única derivada de Pokébola+Pokémon, snapshots/UTC/local/origem e fingerprint de categorias; `capture_totals` e `capture_counts` preservam contadores permanentes independentes dos detalhes. Inserção e incrementos só ocorrem uma vez na mesma transação; UNKNOWN não incrementa os legítimos. `capture_counts` separa SPECIES/CATEGORY. Índice por jogador/origem/data suporta períodos. Não existe poda automática; retenção futura deve preservar contadores de calendário antes de remover detalhes.

Migração5→6 preserva os planos de recompensa e inicia sem capturas reconstruídas, testado com banco antigo. JARs que só suportam schema5 não devem abrir um banco já migrado. [Guia](CAPTURES.md), [evidência](validation/phase4-captures.json).
