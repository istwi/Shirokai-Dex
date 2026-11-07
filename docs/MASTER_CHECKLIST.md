# Prompt mestre — checklist de execução

Lista operacional do [prompt mestre integral](PROJECT_SPECIFICATION.md). A especificação original permanece preservada; esta checklist acompanha sua execução.

## Como atualizar (obrigatório para todo Codex)

- `[ ]` = pendente ou parcial; não riscar trabalho incompleto.
- `[x] ~~Item~~` = concluído, com evidência indicada. Inspeção de fonte, compilação e teste em servidor são resultados diferentes.
- A cada criação, alteração, correção ou remoção, atualizar o item correspondente e registrar arquivos, testes, limitações e commit na tabela de entregas. Se for trabalho parcial, manter `[ ]` e explicar o avanço.
- Se uma mudança invalidar uma conclusão anterior, reabrir o item e justificar. Nunca apagar requisitos para aparentar conclusão.
- Conferir os critérios da seção 34 do prompt antes de concluir funcionalidades. Esta lista resume as fases; os detalhes das seções 1–38 continuam obrigatórios.

## Ponto de retomada

**Prioridade local atual: layout geral**, por pedido do usuário após o bloco Pokémon. Recompensas POKEMON no escopo NBT auditadas; central e grade novas, paletas/slots/gradiente e navegação testados. 214 JUnit e 30 checks reais destes blocos. [Pokémon](validation/phase3-pokemon.json), [layout](validation/layout-home.json). Não conclui todo o mod/layout; sem commit/push até autorização.

## Preparação e continuidade

- [x] ~~Preservar o prompt mestre integral no GitHub.~~ Evidência: [especificação](PROJECT_SPECIFICATION.md), commit `9eaae42`.
- [x] ~~Registrar auditoria estática inicial com fontes e limitações.~~ Evidência: [compatibilidade](API_COMPATIBILITY.md), commit `9eaae42`; não equivale à validação funcional da Fase 0.
- [x] ~~Configurar passagem obrigatória entre Codex.~~ Evidência: [AGENTS.md](../AGENTS.md), [estado](PROJECT_STATUS.md), [próximos passos](NEXT_STEPS.md), commit `72304ea`.
- [x] ~~Confirmar commit e envio inicial ao GitHub.~~ Evidência: commit remoto `ec8f1e9`, confirmado por leitura Git.
- [x] ~~Concluir configuração e resolução do build Gradle.~~ Evidência: `build` aprovado com JDK 21, proxy herdado e checksums ativos; [instalação](INSTALLATION.md).

## FASE 0 — Auditoria técnica

- [ ] Investigar as APIs reais.
- [ ] Verificar dependências.
- [ ] Confirmar eventos de captura.
- [x] ~~Confirmar acesso à Pokédex.~~ Consulta básica executada em servidor real, sem escrita na Pokédex oficial; variantes shiny permanecem limitadas conforme auditoria.
- [ ] Confirmar registro dinâmico de espécies.
- [ ] Confirmar criação de Pokémon.
- [ ] Confirmar integração econômica.
- [x] ~~Criar `API_COMPATIBILITY.md`.~~ Documento criado e atualizado com evidências e testes pendentes.
- [ ] Identificar bloqueios técnicos.

## FASE 1 — Fundação

- [x] ~~Estrutura Gradle.~~ Build e JAR remapeado aprovados.
- [x] ~~Inicialização Fabric.~~ Servidor dev e JAR isolado iniciaram com Cobblemon 1.8.3.
- [x] ~~Sistema de configuração.~~ Parsing estrito, reload seguro e recuperação testados; reload positivo/global e por dimensão verificado nos relatórios de coleções/contextos.
- [x] ~~SQLite.~~ Driver incluído e carregado no JAR instalado; executor dedicado.
- [x] ~~Migrações.~~ Transação, rollback, versão futura e reabertura testados.
- [x] ~~Logs.~~ Readiness, recuperação e falhas registradas no servidor.
- [x] ~~Comando `/dex`.~~ Menu e `/dex status` testados via cliente de protocolo.
- [x] ~~Menu inicial.~~ 10 verificações de smoke com itens somente de leitura; pacotes de drag/double-click/descarte/clone testados no complemento de contextos; cliente visual pendente.
- [x] ~~Integração básica com Cobblemon.~~ Leitura real de registros oficiais e 905 espécies implementadas; datapacks externos e demais integrações pendentes.

## FASE 2 — Pokédex

- [ ] Pokédex Nacional. Parcial: leitura oficial, quantidade/total e % em menu/chat; JSON e wiki inicial validados; recursos avançados ainda pendentes.
- [ ] Shiny. Página explícita N/D; capturas shiny não são inferidas de dados vistos/obtidos.
- [ ] Lendária. Parcial: labels de formas reais, quantidade/total e %; filtros JSON iniciais validados; recursos avançados pendentes.
- [ ] Categorias. Regras nomeadas/condições all/any/none e filtros reais implementados; versão persistida implementada localmente; histórico de eventos e recálculo auditado pendentes.
- [ ] Progresso oficial. Leitura, percentuais, wiki e placeholders básicos verificados; captura shiny e demais requisitos completos permanecem parciais.
- [ ] Datapacks. Fixture com duas espécies namespaced e remoção entre reinícios testados; ecossistema real e demais regras ainda pendentes.
- [ ] Wiki. Parcial: pesquisa, filtros de estados, ordenação, paginação e detalhes implementados/testados; seletores por tipo/label e todas as formas já paginados; resumo parcial de spawn validado; cobertura adicional, categorias avançadas e cliente visual pendentes.
  - [x] ~~Exibir resumos parciais auditados do pool mundial: biomas/tags, dimensões, ticks, alternativas/exclusões e indisponibilidade distinta de nenhuma regra simples conhecida.~~ Alpha.6, [evidência](validation/phase2-spawns.json); aplicação por forma e spawn efetivo não inferidos.
- [ ] Menus. Seletor, progresso e wiki protegidos disponíveis; layouts JSON avançados e validação visual pendentes.
- [x] ~~Placeholders básicos.~~ Cinco métricas por coleção com Text Placeholder API opcional; parser, permissões, eventos/reload, contextos e conflitos verificados no servidor. [Guia](PLACEHOLDERS.md), [evidência](validation/phase2-placeholders.json). TAB/alias legado e placeholders completos permanecem na Fase 7.

- [x] ~~Exibir % e quantidade/total nas coleções com dados confiáveis.~~ Nacional/Lendária em menu e status; duas casas, sem 100% antecipado; vazio/sem evidência = N/D. [Regra](POKEDEX.md).
- [x] ~~Validar denominadores dinâmicos e não duplicar espécies/formas.~~ 12 testes de cálculo, fixture real, aquisição repetida e remoção entre reinícios.
- [x] ~~Adicionar seletor e páginas das três coleções iniciais.~~ Navegação e lore transmitida testadas; Shiny não numérica até evidência.

- [x] ~~Configurar coleções por JSON com metadados, filtros, modo e permissão.~~ Validação de referências reais, 1–64 coleções e compatibilidade com config antigo; [regras](COLLECTIONS.md).
- [x] ~~Validar reload atômico e permissões com LuckPerms real.~~ Grant/revoke global, botão antigo, negação, rollback e recuperação após reinício testados; cenário de dimensão e revogação contextual verificados no complemento; outros contextos/herança pendentes.
- [x] ~~Paginar seletor de coleções configuradas.~~ Fixture com 31 coleções; navegação de ida/volta e ocultação/revalidação verificadas.

- [x] ~~Validar permissões por dimensão e pacotes adicionais de inventário.~~ Oito checks em servidor dev real, com repetição em perfil novo; não equivale a cliente visual/carga. [Evidência](validation/phase2-contexts.json).

- [x] ~~Entregar wiki inicial navegável por coleção.~~ Comandos/botão, pesquisa nome/ID, filtros, ordenação, paginação e detalhes com revalidação; 78 JUnit e 8 checks específicos em servidor real. [Guia](WIKI.md), [evidência](validation/phase2-wiki.json).

- [x] ~~Adicionar filtros por tipo/label e páginas sem corte de formas na wiki.~~ 95 JUnit e 8 checks novos + 8 regressões reais, com 41 formas e revalidação de inventário/permissão/reload. [Guia](WIKI.md), [evidência](validation/phase2-wiki-facets.json).

- [x] ~~Categorias nomeadas reutilizáveis em coleções/wiki com composição JSON e validação de grafo.~~ Alpha.7; [schema e limites](CATEGORIES.md), [testes](validation/phase2-categories.json). Não conclui histórico/recálculo.

- [x] ~~Preservar versões canônicas das regras de categorias no SQLite.~~ Trabalho local sem commit; migração incremental, SHA-256, snapshots antigos e falhas de persistência/integridade testados. [Evidência](validation/phase2-category-versions.json). Não conclui histórico de capturas nem SNAPSHOT/LIVE.

## FASE 3 — Recompensas

- [x] ~~Marcos por quantidade/percentual exato com desbloqueio persistente.~~ Contagens atuais observadas, evidência preservada, reinício e permissão atual testados; [evidência](validation/phase3-unlocks.json).
- [ ] Recompensas.
- [x] ~~CUSTOM_ITEM com codec oficial e componentes Minecraft auditados.~~ Nome/lore/encantamento/dano/custom_data testados; componentes adicionais de mods exigem validação própria. [Evidência](validation/phase3-saved-items.json).
- [x] ~~Recompensas POKEMON com atributos reais, snapshot e equipe/PC nativos.~~ Escopo NBT e intervalo 1..60s; formatos/provedores adicionais não suportados. Falha/reinício/anti-repetição testados. [Evidência](validation/phase3-pokemon.json).
- [ ] Impactor.
- [x] ~~Itens salvos: save/list/give/delete, componentes preservados e snapshots independentes.~~ Migração 5, permissão, auditoria/limites, backup e reinício comprovados. [Guia](SAVED_ITEMS.md).
- [ ] Resgate manual. Itens simples com prévia/segunda confirmação, espaço integral e idempotência validados; tipos avançados pendentes.
- [ ] Persistência segura. Journal/migração 3, intenção/etapas/auditoria, concorrência e falhas simuladas testados; não há transação distribuída com inventário.
- [ ] Recuperação de operações. PROCESSING → NEEDS_REVIEW no reinício, revisão/cancelamento/conclusão explícita auditados; retomada de etapas ainda pendente.

- [x] ~~Entregar itens simples por marco com prévia, confirmação separada e journal antes dos efeitos.~~ Trabalho local: testes de servidor/reinício e falhas simuladas; [guia](REWARDS.md), [evidência](validation/phase3-items.json). Não conclui todos os tipos de recompensa.

- [x] ~~Persistir o primeiro desbloqueio observado de marcos e preservá-lo após mudanças de progresso/reinício.~~ Migração 4; permissão atual obrigatória. [Evidência](validation/phase3-unlocks.json).

## FASE 4 — Capturas e estatísticas

- [ ] Eventos de captura validados.
- [ ] Deduplicação.
- [ ] Estatísticas permanentes.
- [ ] Categorias. Regras nomeadas/condições all/any/none e filtros reais implementados; versão persistida implementada localmente; histórico de eventos e recálculo auditado pendentes.
- [ ] Histórico.
- [ ] Retenção.
- [ ] Recordes.
- [ ] Rankings permanentes.

## FASE 5 — Competições

- [ ] Calendários.
- [ ] Inscrições.
- [ ] Cancelamentos.
- [ ] Pontuação.
- [ ] Proteções.
- [ ] Revisões.
- [ ] Finalização.
- [ ] Premiações.
- [ ] Histórico.

## FASE 6 — Conquistas e perfis

- [ ] Medalhas.
- [ ] Requisitos.
- [ ] Conquistas secretas.
- [ ] Favoritas.
- [ ] Perfis.
- [ ] Privacidade.
- [ ] Estatísticas públicas.

## FASE 7 — Personalização e integrações

- [ ] Menus JSON avançados. Central com rows/palette/slots validada; layouts completos de todos os submenus ainda pendentes.
- [ ] Condições dos botões.
- [ ] Integração SkiesGUIs.
- [ ] LuckPerms.
- [ ] Placeholders completos.
- [ ] Idiomas.
- [ ] Notificações.
- [ ] Ferramentas administrativas.

- [x] ~~Central Shirokai, grade de recompensas e navegação inicial com paletas/gradiente e slots configuráveis.~~ Protocolo, bloqueios, /menu como jogador, ausência de integrações e regressão testados; cliente visual pendente. [Evidência](validation/layout-home.json).

## FASE 8 — Validação final

- [ ] Testes automatizados.
- [ ] Testes de integração.
- [ ] Testes de reinicialização.
- [ ] Testes de configurações inválidas.
- [ ] Testes de recompensas.
- [ ] Testes de desempenho.
- [ ] Verificação das dependências.
- [ ] Build final.
- [ ] Documentação.
- [ ] Release.

## Distribuição da fundação

- [x] ~~Confirmar builds Java 21 e continuidade no GitHub Actions.~~ Commits 9cab47d e c11b366: success.
- [x] ~~Publicar pré-release v0.1.0-alpha.1 com JAR, fontes, checksums e evidências.~~ [Release publicada](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.1.0-alpha.1); workflow `42358003410` aprovado, 5 anexos confirmados, downloads e SHA-256 conferidos. Isto não conclui a release final da Fase 8.

- [x] ~~Publicar pré-release v0.2.0-alpha.3 com JAR e evidências verificados.~~ Workflow `42365493365`, 78 JUnit, cinco anexos baixados/checksums conferidos e tag no código `47817bf`. [Release](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3), [evidência](validation/release-alpha3.json). Não conclui release estável da Fase 8.

## Entregas e evidências

| Entrega | Resultado e evidência | Referência |
|---|---|---|
| Auditoria inicial e build base | Fontes reais auditadas; sem testes funcionais; sem mod implementado | `9eaae42` |
| Continuidade | Documentos e checker validados, incluindo cenários negativos; envio confirmado | `72304ea`, `ec8f1e9` |
| Checklist permanente | Adicionada esta lista e exigida atualização por AGENTS e checker; documentação do ponto de retomada atualizada | Commit com assunto `docs: track master prompt progress and correct official Gradle checksum` |
| Gradle via proxy | Resolvido: checksum oficial corrigido, JDK completo instalado/verificado, build aprovado | [Instalação](INSTALLATION.md) e [testes](TESTING.md) |
| Fundação alpha | Inicialização, config, SQLite/migrações, menu, comandos, consulta oficial, helper de build e CI; 20 casos Java + 10 checks de protocolo, com reinício e JAR instalado. [Arquivos e testes](PROJECT_STATUS.md) | `9cab47d7d4dcd6c95793cc57e4b1b60d87b49b45`, confirmado no remoto |
| Passagem da fundação | Registrada referência exata da entrega funcional e notices atualizados; sem mudança de código | Commit `docs: record synchronized foundation validation` |
| Pré-release alpha.1 | Publicada pelo Actions após 20 testes; 5 anexos, downloads/checksums e commit da tag conferidos | `v0.1.0-alpha.1`, alvo `c11b366`; [evidência](validation/release-alpha1.json) |
| Porcentagens — início Fase 2 | Nacional/Lendária, cache invalidado por evento, páginas/lore/status, Shiny N/D; 32 JUnit e testes de protocolo/datapack | Commit `feat: add verified Pokedex completion percentages`, [evidência](validation/phase2-percentages.json) |
| Coleções JSON | Metadados/filtros/modos, validação, paginação e permissões; 69 JUnit, 11 regressões, 9 checks + 2 de reinício. Pausado por pedido do usuário | Commit `feat: add validated JSON Pokedex collections and permission checks`, [evidência](validation/phase2-collections.json) |
| Contextos e menus protegidos | `scripts/server_proxy.init.gradle`, fixture/runner de contextos, 69 JUnit e 8 checks repetidos em servidor dev. Proxy/truststore repassados à JVM filha; sem mudança funcional no mod. Cliente visual/carga pendentes | Commit `test: validate contextual permissions and protected inventory menus`, base `41e0b7b`; [evidência](validation/phase2-contexts.json) |
| Wiki inicial | `wiki/Wiki.java`, adaptador/core/menu/localização, `WikiTest`, runner wiki e smoke público; alpha.3, 78 JUnit + 8 wiki + 9 coleções + 12 fundação. Spawn/cliente visual/carga/JAR fora do Loom pendentes | Commit `feat: add searchable paginated species wiki`, base `5ad1b60`; [evidência](validation/phase2-wiki.json) |
| Release alpha.3 | Workflow `42365493365` aprovado, 78 JUnit, 5 anexos baixados e checksums/proveniência/tag verificados; pré-release publicada, sem alterar a anterior | Tag `v0.2.0-alpha.3`, alvo `47817bf`; [evidência](validation/release-alpha3.json) |
| Placeholders básicos | Pacote placeholders, lifecycle/preview, compileOnly opcional, nove testes Java novos, runner/probe; 87 JUnit e 8+5+13 checks reais, preservação de provedores e cleanup PASS. TAB/alias positivo/carga pendentes | Commit `feat: add optional collection progress placeholders`, base `05a7577`; [evidência](validation/phase2-placeholders.json) |
| Filtros e formas da wiki | Wiki/GUI/localização, oito testes Java novos, fixture/runner; alpha.5, 95 JUnit + 8 novos + 8 regressões. Todas as 41 formas de teste acessíveis, mesmo escopo de forma e proteção de inventário/reload. Spawn/categorias completas/cliente visual/carga pendentes | Commit `feat: add wiki type filters and complete form pagination`, base `ef99403`; [evidência](validation/phase2-wiki-facets.json) |
| Resumos de spawn | Pacote spawning, wiki/core/menus/localização, sete testes Java, fixture/runner e guias. Alpha.6: 102 JUnit, 7 spawn + 2 remoção + 8 regressão; namespaces, exclusões, ticks reais, permissões e leitura sem mutação. Cobertura universal/cliente visual/carga pendentes | Commit `feat: show audited world spawn rule summaries in wiki`, base `a4ecd4f`; [evidência](validation/phase2-spawns.json) |

| Categorias nomeadas | Catálogo/codec, configuração transacional, vínculo em coleções, progresso/wiki, testes Java e runner de servidor; histórico/recálculo/versão persistida pendentes | Commit `feat: add reusable named category rules for collections`, base `5ac56f2`; [evidência](validation/phase2-categories.json) |

| Versões de categorias (local) | CategoryRules, migração 2, Database/core e testes de migração/reabertura/integridade/falha; nenhum commit/push por instrução do usuário | Sem commit, base `593d43f`; [evidência](validation/phase2-category-versions.json) |

| Recompensas simples e desbloqueios (local) | RewardPlan/Journal/Delivery/ItemRewardAdapter/Unlock, codecs, core/menu/idiomas, migrações 3–4, testes Java e protocolo | Sem commit por pedido do usuário; [itens](validation/phase3-items.json), [desbloqueios](validation/phase3-unlocks.json) |

| Itens salvos/componentes alpha.2 (local) | SavedItem/ItemPayload, tipos e backup resolvido, migração 5/auditoria, comandos/menu e revisão paginada; 179 JUnit + 10 checks reais | Sem commit; base `593d43f`; [evidência](validation/phase3-saved-items.json) |

| POKEMON e primeira passagem visual (local) | Snapshots/validação/adapter NBT, journal misto, HomeMenu/HomeLayout/codec, callbacks/idiomas e probes isolados; 214 JUnit + 8 Pokémon + 22 layout/regressão | Sem commit, base `593d43f`; [Pokémon](validation/phase3-pokemon.json), [layout](validation/layout-home.json) |

Ao registrar a própria entrega, use o assunto do commit em preparação e substitua pela referência exata na atualização seguinte; não crie commits infinitos apenas para gravar o hash do próprio documento.

### Segunda passagem visual local

- [x] ~~Testar cálculo da barra, bloqueio de resgate por estado/plano alterado e isolamento da consulta SQLite.~~ Evidência: `validation/layout-submenus.json`.
- [ ] Validar em servidor/cliente o estilo compartilhado dos submenus e estados de resgate. Código aplicado; testes automatizados não certificam aparência nem navegação nativa.

| Preparação da pré-release alpha.4 | Snapshot das alterações locais, 218 JUnit/build, JAR sem probes e limitações explícitas; sem certificação visual/carga | Tag `v0.3.0-alpha.4`, commit de preparação da release |
