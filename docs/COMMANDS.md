# Comandos disponíveis — alpha de fundação

| Comando | Comportamento |
|---|---|
| `/dex` | Abre menu inicial de inventário somente de leitura |
| `/dex status` | Consulta espécies vistas/obtidas na Pokédex oficial e total de espécies implementadas |
| `/dex admin reload` | Recarrega a configuração validada, exigindo LuckPerms |

`/dex` e `/dex status` exigem jogador e fundação pronta; são públicos nesta fase. Espécies obtidas não significam capturas selvagens. O menu mostra um resumo, sem coleções/recompensas ainda. Fechar: botão de barreira ou tecla de inventário. Não há GUI administrativa.

O registro recusa um `/dex` já presente durante seu callback e registra o conflito; não sobrescreve essa raiz. Colisões introduzidas por mods que registrem comandos posteriormente ainda precisam de teste com o conjunto do servidor.

Não existem comandos de premiação, pausa de competição, concessão de Pokédex ou manipulação de capturas nesta alpha.
