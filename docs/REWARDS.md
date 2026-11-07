# Recompensas — desenvolvimento local

Escopo atual: itens vanilla/de mods e componentes/itens salvos, marcos por registros ou percentual, prévia paginada, confirmação separada e registro persistente de operações. CUSTOM_ITEM e SAVED_ITEM foram acrescentados no trabalho local alpha.2; [schema e comandos](SAVED_ITEMS.md). POKEMON foi acrescentado no [escopo NBT auditado](POKEMON_REWARDS.md). [CURRENCY](CURRENCY_REWARDS.md) está disponível no backend auditado. [COMMAND](COMMAND_REWARDS.md) permite uma ação isolada autorizada com revisão obrigatória na alpha.10 local; CRATE_KEY dedicado permanece indisponível. Não configurar tipos não implementados.

```json
"rewards": [
  {
    "id": "first_registration",
    "collection": "national",
    "minimum_registered": 1,
    "items": [{"id": "minecraft:diamond", "count": 2}]
  },
  {
    "id": "half_national",
    "collection": "national",
    "minimum_percent": 50,
    "items": [{"id": "minecraft:paper", "count": 64}]
  }
]
```

Adicionar ao config.json existente. Campo opcional, padrão vazio, até 128 recompensas e 32 entradas por recompensa; limite global permanece 64 KiB. IDs estáveis `[a-z][a-z0-9_]{0,47}`. Exatamente um requisito positivo: minimum_registered inteiro, ou minimum_percent numérico >0 e <=100 com no máximo duas casas. Percentual compara frações exatas, sem usar o valor arredondado da interface. Progresso vazio/indisponível não desbloqueia. Quantidades 1..2304 por entrada; inventário real e tamanho máximo real das pilhas limitam a entrega. IDs de itens/coleções precisam existir; air é recusado. Coleções controlam acesso por sua permissão atual.

`/dex admin reward list [página]` lista operações não finalizadas, dez por página.

`/dex rewards` lista IDs visíveis; `/dex rewards <id>` abre a prévia, com quantidades totais e paginação de todas as entradas. Botão resgatar abre confirmação separada. Nenhum efeito ocorre só por consultar. Clique de confirmação revalida requisitos, permissão e plano atual; alterações via reload invalidam botões antigos. Espaço é simulado no inventário principal (36 slots), combinando somente pilhas de item/componentes iguais. Não usar armadura/offhand como espaço e não largar excedente no chão.

## Persistência e resultado incerto

Migração SQLite 3: reward_operations, reward_steps e reward_audit. UUID de operação e unicidade jogador/recompensa; plano preservado não é substituído pelo reload. Não reutilizar IDs para recompensas diferentes. Cancelamento é definitivo para esse jogador/ID.

PENDING → PROCESSING → COMPLETED; falha/interrupção em processamento vai para NEEDS_REVIEW, nunca para nova entrega automática. Intenção e auditoria são gravadas em transação antes de cada efeito; confirmação da etapa vem depois. Recuperação no startup transforma PROCESSING em NEEDS_REVIEW e mantém etapas/evidências. Todas as transições e decisões administrativas são auditadas. Pré-condição inicial recusada não reserva operação.

A entrega altera o inventário na thread do servidor e aguarda o checkpoint nativo quando Crates 0.2.1 está carregado (ou solicita saveAll vanilla sem Crates) e relê o arquivo NBT do jogador, conferindo os 36 slots e componentes antes da confirmação da última etapa. **SQLite e arquivos de jogador não formam uma transação atômica**: falha/crash pode deixar resultado incerto, inclusive no salvamento; verificar inventário, etapas e evidências. O salvamento vanilla suprime algumas exceções; a leitura posterior detecta arquivo ausente/inventário divergente e bloqueia a operação para revisão. Leitura NBT é limitada a 16 MiB; arquivos maiores também exigem revisão. Isso não garante fsync/atomicidade em perda de energia. No fallback, saveAll salva jogadores e pode ter custo em servidores grandes; carga/desempenho ainda não avaliados. Não prometer entrega exatamente uma vez em todos os modos de falha.

## Revisão administrativa

Permissão própria `shirokaidex.admin.rewards`, via LuckPerms; não exige também a permissão de reload. Somente jogador autorizado.

```text
/dex admin reward status <uuid_operacao>
/dex admin reward cancel <uuid_operacao> confirm <justificativa>
/dex admin reward complete <uuid_operacao> confirm <evidencia>
```

status mostra jogador/recompensa/estado/plano preservado. cancel aceita PENDING ou NEEDS_REVIEW. complete aceita somente NEEDS_REVIEW e reconhece evidência externa: **não entrega nem reenvia itens**. Ambos exigem texto não vazio e registram UUID do administrador, razão e horário. Não declarar complete sem conferir a entrega. Retomada supervisionada de etapas restantes ainda não existe; não repetir entregas incertas por comandos externos.

Ainda pendentes: comandos, moedas e chaves, economia/chaves/Pokémon, busca/paginação da auditoria e retomada supervisionada. [Testes](TESTING.md).

## Desbloqueio permanente (migração 4)

Avaliador em round robin considera no máximo um jogador por tick, com intervalo de cinco segundos por jogador e sem fila ilimitada. Eventos oficiais de Pokédex e reload invalidam o intervalo de observação. Critérios são avaliados na thread do servidor; somente dados imutáveis seguem para SQLite. Recompensa só fica resgatável depois da confirmação persistida, podendo aparecer bloqueada por alguns instantes ao conectar/desbloquear.

reward_unlocks preserva a primeira evidência por UUID/ID: plano observado, registros, total e horário. Observações repetidas não substituem a evidência. Desbloqueios já registrados sobrevivem ao logout, reinício e mudanças posteriores de progresso/requisito. Permissão atual continua obrigatória. ID identifica a mesma recompensa permanentemente; mudar ID cria outra recompensa. Conteúdo configurado vigente é usado ao iniciar resgate; operação já reservada conserva seu plano e bloqueia substituição silenciosa. Use novos IDs para recompensas diferentes.

Não reconstruímos marcos transitórios que nunca foram observados; dados atuais da Pokédex podem desbloquear marcos ao instalar o mod, sem inferir capturas históricas. Falhas de persistência não liberam novos resgates; nova observação é tentada depois.

## CURRENCY — alpha.8 local

Entradas `currencies` podem acompanhar itens/Pokémon ou formar uma recompensa isolada. API real Impactor5.3.5 com backend JSON nativo: sem moeda principal como fallback, sem arredondamento implícito e com verificação de gravação por leitura. Veja [guia de moedas](CURRENCY_REWARDS.md). Planos antigos continuam serializados sem campo currencies quando vazio; journal conta etapas monetárias adicionais sem nova migração SQL.

## Checkpoint opcional — alpha.9 na branch de validação

`PlayerSaveCoordinator` usa os métodos públicos do Crates 0.2.1 auditado, verifica saúde/barreira e aguarda o future antes do readback da última etapa de itens. Sem Crates, mantém fallback vanilla. Provedor carregado desconhecido, indisponível ou unhealthy bloqueia itens, sem fallback silencioso. Timeout de 30s afeta somente a espera dependente; não cancela a gravação/barreira nativa. Falha após intenção mantém NEEDS_REVIEW, sem repetição automática. [Escopo e evidência](CRATES_CHECKPOINT_HANDOFF.md).
