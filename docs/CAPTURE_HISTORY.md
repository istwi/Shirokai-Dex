# Histórico administrativo de capturas

Consulta somente de leitura: `/dex admin captures <player_uuid> [page]`.

Exige jogador conectado com `shirokaidex.admin.captures` no LuckPerms; console e OP sem a permissão não recebem acesso. A autorização e a sessão são revalidadas após a consulta assíncrona. UUIDs permitem consultar jogadores offline sem resolver nomes nem expor sugestões públicas.

Páginas de dez registros, da captura mais recente à mais antiga; empates usam a chave persistente do evento. Há indicação de próxima página. São exibidos instante UTC, espécie, forma, shiny, origem, fonte nativa, dimensão e chave do evento. Origem UNKNOWN permanece explicitamente não comprovada: aparecer no histórico não concede estatísticas ou progresso.

A leitura ocorre no worker SQLite e não altera Pokédex, eventos, totais, projeção Shiny ou recompensas. Paginação por offset consulta o estado atual: capturas novas entre comandos podem deslocar páginas; não é uma exportação com snapshot congelado. Limite de página: 100001.

O bloco posterior de [retenção](RETENTION.md) introduz schema10, recibos permanentes e limpeza em lotes. Filtros avançados, recálculo histórico, recordes e rankings continuam pendentes. Consultas mostram apenas detalhes ainda retidos; a expiração não apaga totais nem recibos.

Validação: 328 JUnit sem falhas/erros/skips e seis checks nativos de comando/ACL em Loom aprovados. [Evidência](validation/phase4-history.json). Novo comando em distribuição fora do Loom, cliente visual e carga pendentes.
