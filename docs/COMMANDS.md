# Comandos disponíveis — alpha experimental

| Comando | Comportamento |
|---|---|
| `/dex` | Abre seletor de coleções somente de leitura |
| `/dex national`, `/dex legendary`, `/dex shiny` | Abrem páginas individuais com progresso/% ou estado indisponível explicado |
| `/dex national status`, `/dex legendary status`, `/dex shiny status` | Resumo de quantidade/total e % por coleção, ou N/D sem evidência |
| `/dex <id> wiki [pesquisa]` | Wiki paginada da coleção; pesquisa por nome canônico/ID, filtros e detalhes. [Guia](WIKI.md) |
| `/dex status` | Consulta espécies vistas/obtidas na Pokédex oficial e total de espécies implementadas |
| `/dex admin reload` | Recarrega a configuração validada, exigindo LuckPerms |

`/dex` e `/dex status` exigem jogador e fundação pronta; são públicos nesta fase. Espécies obtidas não significam capturas selvagens. O menu mostra as coleções iniciais; Nacional/Lendária têm contagem e %, enquanto Shiny está indisponível para capturas verificadas. Coleções JSON e wiki estão disponíveis; recompensas continuam pendentes. Fechar: botão de barreira ou tecla de inventário. Não há GUI administrativa.

O registro recusa um `/dex` já presente durante seu callback e registra o conflito; não sobrescreve essa raiz. Colisões introduzidas por mods que registrem comandos posteriormente ainda precisam de teste com o conjunto do servidor.

Não existem comandos de premiação, pausa de competição, concessão de Pokédex ou manipulação de capturas nesta alpha.

Detalhes de cálculo, atualização e limitações: [Pokédex e porcentagem](POKEDEX.md).

Desde a alpha.2: `/dex <id>` e `/dex <id> status` também atendem coleções JSON criadas pelo administrador. Sugestões dinâmicas e seletor respeitam as permissões atuais; páginas do seletor são automáticas. [Coleções](COLLECTIONS.md).
