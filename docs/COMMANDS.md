# Comandos disponíveis — alpha experimental

| Comando | Comportamento |
|---|---|
| `/dex` | Abre seletor de coleções somente de leitura |
| `/dex national`, `/dex legendary`, `/dex shiny` | Abrem páginas individuais com progresso/% ou estado indisponível explicado |
| `/dex national status`, `/dex legendary status`, `/dex shiny status` | Resumo de quantidade/total e % por coleção, ou N/D sem evidência |
| `/dex <id> wiki [pesquisa]` | Wiki paginada da coleção; pesquisa por nome/ID, seletores de tipo/label e todas as formas em páginas. [Guia](WIKI.md) |
| `/dex <id> placeholder <métrica>` | Prévia dos cinco placeholders básicos pelo parser da API opcional; [sintaxe e limites](PLACEHOLDERS.md) |
| `/dex status` | Consulta espécies vistas/obtidas na Pokédex oficial e total de espécies implementadas |
| `/dex admin reload` | Recarrega a configuração validada, exigindo LuckPerms |

`/dex` e `/dex status` exigem jogador e fundação pronta; são públicos nesta fase. Espécies obtidas não significam capturas selvagens. O menu mostra as coleções iniciais; Nacional/Lendária têm contagem e %, enquanto Shiny está indisponível para capturas verificadas. Coleções JSON e wiki estão disponíveis; recompensas continuam pendentes. Fechar: botão de barreira ou tecla de inventário. Não há GUI administrativa.

O registro recusa um `/dex` já presente durante seu callback e registra o conflito; não sobrescreve essa raiz. Colisões introduzidas por mods que registrem comandos posteriormente ainda precisam de teste com o conjunto do servidor.

Não existem comandos de premiação, pausa de competição, concessão de Pokédex ou manipulação de capturas nesta alpha.

Detalhes de cálculo, atualização e limitações: [Pokédex e porcentagem](POKEDEX.md).

Desde a alpha.2: `/dex <id>` e `/dex <id> status` também atendem coleções JSON criadas pelo administrador. Sugestões dinâmicas e seletor respeitam as permissões atuais; páginas do seletor são automáticas. [Coleções](COLLECTIONS.md).

## Recompensas de itens (desenvolvimento local)

`/dex rewards` e `/dex rewards <id>`: lista/prévia, com resgate por segunda confirmação na GUI. Administração: `/dex admin reward status <uuid>`, `/dex admin reward cancel <uuid> confirm <justificativa>` e `/dex admin reward complete <uuid> confirm <evidência>`. [Regras e limites](REWARDS.md).

## Itens salvos (local alpha.2)

`/dex admin item save <id>`, `list [página]`, `give <id> <jogador>` e `delete <id> confirm`. Give requer confirmação separada e registro da operação. `/dex admin reward list [página]` lista a fila de revisão. [Guia](SAVED_ITEMS.md).

## Central e grade de recompensas

`/dex` agora abre a central; Pokédex abre o seletor, e Recompensas abre a grade paginada. Comandos diretos `/dex <coleção>` e `/dex rewards <id>` permanecem. Não foram adicionados aliases que mascarem IDs de coleções existentes. O botão externo VOLTAR AO MENU executa `/menu` como o jogador quando registrado. [Navegação e limites](LAYOUT.md).

## Retomada administrativa sem etapas

`/dex admin reward resume <operação> <justificativa>` — permissão `shirokaidex.admin.rewards`; abre confirmação protegida para entregar ao destinatário online usando o plano original. Somente PENDING/NEEDS_REVIEW sem etapas; justificativa 8..2048 caracteres. [Escopo e bloqueios](RECOVERY.md).

## Estatísticas próprias de captura

`/dex profile captures`, `/dex profile captures species <namespace:espécie>` e `/dex profile captures category <id>` consultam somente o jogador atual. Capturas verificadas são distintas de registros da Pokédex e de origem desconhecida. Perfil gráfico ainda não implementado. [Guia](CAPTURES.md).
