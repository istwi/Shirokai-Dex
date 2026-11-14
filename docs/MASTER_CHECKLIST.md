# Prompt mestre — checklist de execução

## Validação complementar de perfis — 2026-11-14

Suíte nativa concluída: nove checks e um de reinício aprovados, três inicializações/encerramentos normais e segredo RCON removido. [Evidência](validation/phase6-profiles.json), incluindo falha inicial e repetição corrigida. Perfis offline, privacidade pública, Pokédex oficial, conquistas/favoritos e ausência de concessões materiais indevidas verificados por protocolo. Próximo: placeholders avançados/menus/notificações e demais pendências; cliente gráfico/carga não certificados. Esta atualização substitui as menções anteriores de repetição pendente.

Lista operacional do [prompt mestre integral](PROJECT_SPECIFICATION.md). A especificação original permanece preservada; esta checklist acompanha sua execução.

## Como atualizar (obrigatório para todo Codex)

- `[ ]` = pendente ou parcial; não riscar trabalho incompleto.
- `[x] ~~Item~~` = concluído, com evidência indicada. Inspeção de fonte, compilação e teste em servidor são resultados diferentes.
- A cada criação, alteração, correção ou remoção, atualizar o item correspondente e registrar arquivos, testes, limitações e commit na tabela de entregas. Se for trabalho parcial, manter `[ ]` e explicar o avanço.
- Se uma mudança invalidar uma conclusão anterior, reabrir o item e justificar. Nunca apagar requisitos para aparentar conclusão.
- Conferir os critérios da seção 34 do prompt antes de concluir funcionalidades. Esta lista resume as fases; os detalhes das seções 1–38 continuam obrigatórios.

## Ponto de retomada

**Retomada:** captura/estatísticas iniciais 0.4.0-alpha.1, base `26c4472`: 304 JUnit e 18 checks nativos/reinícios. [Evidência](validation/phase4-captures.json). Captura natural validada no escopo PlayerSpawner/pool mundial; origem perdida após reload fica UNKNOWN. Fase 4 parcial: proveniência contínua/batalha/pesca/outros fluxos/recordes/histórico/revisão e Shiny% ainda pendentes. Commits/push contínuos na branch; main/release/ambiente separados.

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
- [ ] Shiny. Progresso verificado por espécie/forma ligado ao ledger na alpha.5; sem snapshot confirmado fica N/D. Visto/obtido não contam. Demais recursos avançados continuam parciais.
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
- [ ] Impactor. Integração CURRENCY com API5.3.5/JSON nativo implementada/testada; serviços/backends/versões adicionais não certificados. [Guia](CURRENCY_REWARDS.md).
- [x] ~~Itens salvos: save/list/give/delete, componentes preservados e snapshots independentes.~~ Migração 5, permissão, auditoria/limites, backup e reinício comprovados. [Guia](SAVED_ITEMS.md).
- [ ] Resgate manual. Itens simples com prévia/segunda confirmação, espaço integral e idempotência validados; tipos avançados pendentes.
- [ ] Persistência segura. Journal/migração 3, intenção/etapas/auditoria, concorrência e falhas simuladas testados; não há transação distribuída com inventário.
- [ ] Recuperação de operações. PROCESSING → NEEDS_REVIEW no reinício, revisão/cancelamento/conclusão explícita auditados; retomada de etapas ainda pendente.

- [x] ~~Entregar itens simples por marco com prévia, confirmação separada e journal antes dos efeitos.~~ Trabalho local: testes de servidor/reinício e falhas simuladas; [guia](REWARDS.md), [evidência](validation/phase3-items.json). Não conclui todos os tipos de recompensa.

- [x] ~~Persistir o primeiro desbloqueio observado de marcos e preservá-lo após mudanças de progresso/reinício.~~ Migração 4; permissão atual obrigatória. [Evidência](validation/phase3-unlocks.json).

## FASE 4 — Capturas e estatísticas

- [x] ~~Eventos nativos de captura validados no Cobblemon1.8.3.~~ Escopos PlayerSpawner, PvE simples e pesca auditada; demais origens ficam UNKNOWN, sem inferência. Relatórios alpha.1–5.
- [x] ~~Deduplicação persistente de observações nativas.~~ Chave Pokébola/Pokémon e consumo único de prova; replay/reinício não reclassificam nem duplicam.
- [x] ~~Totais permanentes de ações, shiny/labels/espécie/categoria e períodos atuais.~~ Transações e snapshots/UTC/fuso/semana ISO testados; recordes e retenção permanecem itens próprios abertos.
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

- [ ] Menus JSON avançados. Central e 11 telas existentes têm navegação/tamanho/título/paleta/aparência de controles validados em alpha.6/7; modelos reutilizáveis, conteúdo/condições genéricas, telas de fases futuras e cliente gráfico pendentes.
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
- [ ] Validar aparência em cliente gráfico. Navegação nativa/estados dos submenus já foram exercitados no bloco alpha.6; cliente visual continua pendente.

| Preparação da pré-release alpha.4 | Snapshot das alterações locais, 218 JUnit/build, JAR sem probes e limitações explícitas; sem certificação visual/carga | Tag `v0.3.0-alpha.4`, commit de preparação da release |

### Correção de inicialização alpha.5

- [x] ~~Reproduzir erro de entrypoint ao instalar fontes e confirmar causa.~~ Fontes alpha.4 reproduzem ClassNotFoundException; usuário confirmou instalação acidental do sources.jar.
- [x] ~~Proteger empacotamento e separar artefato instalável das fontes.~~ `verifyModJar` valida bytecode final; Actions usa `build/server-mod`. Ver `validation/entrypoint-alpha5.json`.

| Correção de inicialização alpha.5 | build.gradle, workflow, documentação/evidência; fontes não são mod e distribuição valida bytecode final | Commit `fix: prevent source archives from loading as Fabric mods` |

- [x] ~~Validar inicialização alpha.5 fora do Loom.~~ Fabric 0.17.2 / MC1.21.1 / Java21 / Cobblemon1.8.3: Done + Dex ready, SQLite íntegro; fontes novas ignoradas. Evidência em `validation/entrypoint-alpha5.json`. Não certifica modpack/carga/cliente gráfico.

### Navegação configurável dos submenus — alpha.6 local

- [x] ~~Validar navegação JSON, conflitos, defaults e recuperação de configuração.~~ 234 JUnit totais, incluindo 16 novos casos, e 7 checks em servidor empacotado. [Evidência](validation/layout-navigation.json).
- [x] ~~Exercitar estilo/navegação dos submenus e estados reais via protocolo.~~ Confirmação e entrega de item personalizado, sem repetição, filtros/formas/spawn e reload incluídos.
- [ ] Revisar aparência em cliente gráfico e completar modelos reutilizáveis/conteúdo/condições. Tamanhos/títulos/paletas/ícones de navegação foram validados no bloco alpha.7; isso não equivale ao sistema completo de menus.

| Navegação de submenus (local) | MenuNavigation/codec/Settings/MenuStyle/MainMenu/HomeMenu, 16 casos Java novos e fixture/runner/seeder de servidor; 234 JUnit + 7 checks | Sem commit, base `7defc0c`; [evidência](validation/layout-navigation.json) |

### Apresentação configurável — alpha.7 local

- [x] ~~Validar linhas, títulos, paletas e aparência dos controles existentes.~~ 254 JUnit totais, 20 casos novos e 6 checks em servidor empacotado; [evidência](validation/menu-appearance.json).
- [x] ~~Preservar estado real, ações bloqueadas e componentes de prêmios sob templates.~~ Ausência de claim preservada, entrega real única e reload de ícone inexistente rejeitado.
- [ ] Modelos reutilizáveis, condições genéricas, posicionamento de conteúdo e cliente gráfico. O escopo implementado é só dos menus atuais, sem telas fictícias para fases futuras.

| Aparência dos menus (local) | MenuAppearance, Layout/codec/renderer/validador, 20 casos novos, fixture/runner; 254 JUnit + 6 checks nativos; runners legados adaptados com sintaxe validada | Sem commit, base `7defc0c`; [evidência](validation/menu-appearance.json) |

### CURRENCY — alpha.8 local

- [x] ~~Entregar moedas registradas pela API Impactor5.3.5 com journal e leitura nativa.~~ 274 JUnit totais, 20 casos novos e 8 checks reais de servidor/ausência/reinício; [evidência](validation/phase3-currency.json).
- [x] ~~Detectar save nativo suprimido e impedir repetição automática após falha/reinício.~~ Erro real injetado, saldo em arquivo comparado e operação persistida em NEEDS_REVIEW.
- [ ] Outros backends/serviços/versões, carga, transações concorrentes amplas e retomada supervisionada. Sem promessa de atomicidade distribuída.

| CURRENCY (local) | Modelo/codec/planos/journal/adapter opcional/readback/menus, 20 casos novos e runner em três modos; 274 JUnit + 8 checks | Sem commit, base `7defc0c`; [evidência](validation/phase3-currency.json) |

| Sincronização autorizada alpha.6/7/8 | Snapshot de menus e CURRENCY, evidências de 274 JUnit e testes nativos, documentos de continuidade atualizados | Commit `feat: add configurable menu presentation and Impactor currency rewards`, base `7defc0c`; sem nova release |

### WIP separado — checkpoint Crates

- [x] ~~Conferir fonte/HEAD e binário oficial Crates0.2.1 para salvamento coordenado.~~ Hash/métodos reais auditados; isso não prova entrega integrada.
- [x] ~~Testar coordenação assíncrona/itens/chaves via SAVED_ITEM em servidor empacotado e reinício.~~ Writer atrasado, autenticidade nativa, componentes, falha real/NEEDS_REVIEW/anti-repetição, fallback sem Crates e reinícios; [evidência](validation/phase3-crates-checkpoint.json). Não conclui tipo CRATE_KEY dedicado, outras versões/modpacks ou toda a Fase 3.

| Passagem WIP Crates | PlayerSaveCoordinator, ItemRewardAdapter, probe/fixture e instruções exatas; build/274 JUnit, sem teste nativo novo | Branch handoff/crates-checkpoint; [passagem](CRATES_CHECKPOINT_HANDOFF.md) |

| Validação local checkpoint Crates | Runner em quatro modos, remapeamento de probe Pokémon, regressões e documentação objetiva. 274 JUnit e 26 checks fora do Loom (8 checkpoint + 10 itens + 8 Pokémon); SQLite íntegro e cleanup concluído. Planos/componentes e main preservados | Commit `test: validate native Crates checkpoints and delivery regressions`;  base `6b97093`, branch `handoff/crates-checkpoint`; [evidência](validation/phase3-crates-checkpoint.json) |

### Auditoria posterior — emissão CRATE_KEY

- [x] ~~Avaliar contrato público de emissão dedicada no Crates0.2.1 com fonte e binário oficiais.~~ [Resultado estático](CRATE_KEY_CONTRACT.md): UNSUPPORTED no contrato automático auditado; não equivale à implementação/teste de CRATE_KEY.
- [ ] CRATE_KEY dedicado. Requer contrato público suficiente sem acesso ao segredo/campo privado, elevação de permissão ou sucesso inferido de comando; SAVED_ITEM nativo permanece alternativa validada.

| Viabilidade CRATE_KEY0.2.1 | Matriz de API pública/limites/requisitos e continuidade; fonte e javap, sem mudança de runtime ou novo teste funcional | Auditoria local posterior ao commit enviado `f714c4b`; [contrato](CRATE_KEY_CONTRACT.md) |

### COMMAND — alpha.10 local

- [x] ~~Entregar dispatch COMMAND isolado com política exata, plano preparado, confirmação e revisão obrigatória, sem retry.~~ 282 JUnit e nove checks nativos/reinício; [guia](COMMAND_REWARDS.md), [evidência](validation/phase3-commands.json). Isso não comprova persistência de comandos externos nem conclui COMMAND completo.
- [ ] Recibos automáticos/planos mistos COMMAND e recuperação supervisionada completa. Nenhum INTENT incerto pode ser reenviado automaticamente.

| COMMAND local | CommandReward/Policy/Adapter, codec/Settings, planos/journal/core/menus/traduções, oito testes Java, fixture/runner e guia. 282 JUnit, 7+2 checks; sem nova migração SQLite | Commit `feat: add explicitly authorized command reward dispatch`; base `f714c4b`, alpha.10; [evidência](validation/phase3-commands.json) |

### Recuperação segura e testes reutilizáveis — alpha.11

- [x] ~~Retomar explicitamente PENDING/NEEDS_REVIEW comprovadamente sem etapas, com prévia/confirmação/motivo/auditoria e revalidações atuais.~~ ID/plano preservados; oito JUnit novos, permissões/desconexão/reload e inventário real/reinício. [Guia](RECOVERY.md).
- [x] ~~Automatizar boot, fixture offline, suíte e reinício/cleanup no servidor empacotado reutilizando dependências auditadas.~~ Suites recovery e commands, 18 checks e logs/save/segredo conferidos. Não é teste de todos os tipos/modpacks.
- [ ] Retomada parcial de etapas com resultado externo incerto. INTENT e prefixos reconhecidos não são reenviados; sem recibos/prova suficiente continuam em revisão.

| Recuperação e executor | RewardJournal/Database/Delivery/core/GUI/traduções, oito testes Java, fixture/runner/seeder e executor. 290 JUnit e 9+9 checks empacotados/reinícios. Sem migração nova nem alteração em outros mods | Commit `feat: add safe supervised reward recovery and reusable server suites`, base `73e5d78`; [evidência](validation/phase3-recovery.json) |

### Captura e estatísticas iniciais — 0.4.0-alpha.1

- [x] ~~Validar captura pelo motor nativo e correlacionar evidência prévia de origem no PlayerSpawner/pool mundial.~~ Pokébola física/animação, sem evento artificial de primeira captura; origem ausente fica UNKNOWN. [Guia](CAPTURES.md).
- [x] ~~Deduplicar observações e manter totais/shiny/labels/espécies/categorias observadas de novas capturas verificadas.~~ Transação/schema6, snapshots históricos, migração5→6, replay e reinício; 304 JUnit e 18 checks nativos/regressão.
- [x] ~~Consultar estatísticas próprias e períodos com timezone/semana ISO.~~ CLI, UTC padrão, timezone válido, DST/fronteiras/calendário e total distinto de espécies testados.
- [ ] Cobertura completa de captura. Proveniência persistente no escopo PlayerSpawner/reload validada na alpha.2; batalhas/pesca/outros spawners requerem teste próprio. Nenhuma captura antiga inferida.
- [ ] Histórico/admin/revisão/limites/recordes/retention e ligação do ledger à coleção Shiny. Permanentes e categorias preservados; não concluir toda a Fase 4.

| Capturas/estatísticas | CaptureRecord/Tracker/Access/Ledger, V006/Database, timezone/CLI/core/traduções; 14 JUnit novos, probe/fixture/runner e executor. 304 JUnit e 9+9 checks; origem perdida permanece desconhecida | Commit `feat: track verified native captures and permanent statistics`, base `26c4472`; [evidência](validation/phase4-captures.json) |

### Publicação autorizada — v0.4.0-alpha.1

- [x] ~~Publicar pré-release experimental v0.4.0-alpha.1 e conferir anexos/tag/proveniência.~~ Workflow42432155833 aprovado; quatro assets baixados e hashes/entrypoint/tag conferidos. Binário publicado igual ao nativo testado. [Evidência](validation/release-captures-alpha1.json).

| Preparação release0.4 | Notas, empacotador e workflow novo de fonte fixa 78dfe72; antigos não alterados. Build remoto da fonte já aprovado; publicação ainda depende do run/anexos | Commit `release: prepare v0.4.0-alpha.1`; sem merge/main/ambiente |

Correção de preparação: condição do workflow convertida em escalar YAML após run42431952113 rejeitado antes de jobs; source/tag permanecem os mesmos e nenhum draft foi criado.

| Release0.4 publicada | Tag78dfe72, workflow42432155833 aprovado, quatro anexos baixados/conferidos, hash do JAR igual ao teste nativo; sem main/ambiente | Commit `docs: record verified captures alpha release`; [evidência](validation/release-captures-alpha1.json) |

### Proveniência persistente — 0.4.0-alpha.2

- [x] ~~Persistir prova nativa realmente observada e reconhecer a mesma entidade após reinício com correlação pré-hit válida.~~ Captura física real sem SpawnCause após reload, schema7; [evidência](validation/phase4-provenance.json).
- [x] ~~Consumir prova uma vez e impedir reabertura/reclassificação retroativa.~~ Nove JUnit novos para identidades, ausência de prova/pré-hit, consumo, replay e migração6→7; 313 JUnit totais.
- [ ] Certificar batalha/pesca/outros spawners, carga e retenção. Fase4 continua aberta.

| Proveniência alpha.2 | NativeSpawnProof/Observation/Provenance, CaptureAccess/Tracker/Ledger, core/Database/V007; nove JUnit novos, runner/probe por versão e seeder schema7. Captura/reinício nativos; sem nova release/main/ambiente | Commit `feat: preserve observed native spawn provenance across reloads`; base `a082358`; [evidência](validation/phase4-provenance.json) |

### Captura PvE — 0.4.0-alpha.3

- [x] ~~Validar captura física em batalha PvE nativa no escopo PlayerSpawner/pool mundial.~~ BattleBuilder.pve real, Master Ball real, LIVE_NATIVE, replay sem duplicação e reinício; [evidência](validation/phase4-battle.json).
- [x] ~~Auditar contrato estático de origem da pesca no Cobblemon1.8.3.~~ FishingSpawnCause/BasicSpawner identificados; isso não certifica captura por pesca.
- [ ] Validar pesca pelo motor real, batalhas duplas e extensões; Shiny/histórico/recordes e demais Fase4 continuam abertos.

| Captura PvE alpha.3 | CaptureProbe/teste de captura ampliados, versão e documentação; 313 JUnit, dez checks nativos incluindo batalha/reinício. Sem mudança na política de origem/schema7 | Commit `test: validate native battle capture provenance`; base `3f4adaf`; [evidência](validation/phase4-battle.json) |

### Pesca nativa — alpha.4

- [x] ~~Validar origem e captura por pesca com vara/anzol/mordida/recolhimento nativos.~~ Fonte FISHING persistida, captura física, replay/reinício; [evidência](validation/phase4-fishing.json).
- [x] ~~Preservar fonte original e provas consumidas na migração7→8.~~ Quatro JUnit novos; 317 totais aprovados.
- [ ] Certificar outras varas/iscas/pools/modpacks, carga/retenção e demais Fase4; Shiny continua pendente.

| Pesca alpha.4 | CaptureAccess/NativeSpawnProof/NativeProvenance/Database/V008, quatro testes Java, CaptureProbe e fixture/runner fishing, executor/seeder e documentação; build e testes no relatório | Commit `feat: track verified native fishing capture provenance`; base `d967c98`; [evidência](validation/phase4-fishing.json) |

### Coleção Shiny — alpha.5

- [x] ~~Ligar SHINY_CAPTURED a capturas legítimas persistidas, com espécie/forma/denominador atuais.~~ Cache assíncrono, projeção permanente/schema9, repetição não aumenta espécies; [guia](SHINY_CAPTURES.md).
- [x] ~~Distinguir fonte indisponível de zero conhecido e preservar prova no reinício/migração e fora do histórico detalhado.~~ Nove JUnit novos/326 totais e servidor conforme [evidência](validation/phase4-shiny.json).
- [ ] Histórico/recordes/rankings/retenção e demais requisitos da Fase4; não marcar fase concluída.

| Shiny alpha.5 | CapturedShinyForm/ProgressCalculator, CaptureLedger/Database/V009, CobblemonAccess/core; nove testes novos, runners e documentos afetados. Fonte oficial não alterada | Commit `feat: derive shiny collection progress from verified captures`; base `b359a12`; [evidência](validation/phase4-shiny.json) |

### Retomada — histórico administrativo local (2026-11-14)

- [ ] Histórico completo. Consulta administrativa paginada implementada localmente; 328 JUnit e seis checks de ACL/comando em servidor Loom aprovados; [evidência](validation/phase4-history.json). Retenção/filtros/auditoria avançada pendentes.

| Entrega | Resultado e evidência | Referência |
|---|---|---|
| Histórico administrativo local | CaptureHistory/Database/core/idiomas, CaptureHistoryTest e CAPTURE_HISTORY.md; 328 JUnit/6 checks nativos aprovados; [evidência](validation/phase4-history.json) | Sem commit, base 88f923e |

- [x] ~~Consultar histórico administrativo paginado por UUID com permissão dedicada e revalidação.~~ Dois testes SQLite novos e seis checks de comando/ACL em Loom, [evidência](validation/phase4-history.json). Não conclui histórico completo/retenção.

### Recibos permanentes e retenção local (2026-11-14)

- [x] ~~Deduplicar capturas após remoção do histórico detalhado e preservar contagens por período/fuso.~~ Schema10, migração e replay/rollback/reinício testados.
- [x] ~~Reter detalhes de capturas com limpeza automática em lotes, preservando os dados permanentes e a auditoria atual.~~ Política/configuração, 337 JUnit e migração/limpeza/reinício nativos aprovados; [evidência](validation/phase4-retention.json). Preserva UNKNOWN/recibos/agregados; novas revisões/competições exigem integrar suas dependências antes de ativação.

| Entrega | Resultado e evidência | Referência |
|---|---|---|
| Recibos permanentes e retenção | CaptureLedger/NativeProvenance/Database/V010, Settings/codec/core, 9 novos testes e runner retention.py;337 JUnit/build e migração/scheduler/reinício nativos aprovados; [evidência](validation/phase4-retention.json) | Local sem commit, base 88f923e |

### Rankings e recordes locais

- [x] ~~Rankings permanentes de capturas configuráveis/paginados e recordes por período/fuso.~~344 JUnit/7 checks nativos; [evidência](validation/phase4-rankings.json). Limites de datas históricas, perfis/privacidade completa e rankings de Pokédex em RANKINGS.md.

| Entrega | Evidência | Referência |
|---|---|---|
| Recordes/rankings de capturas | V011, CaptureRankings/CaptureRecords, cache/codec/core/menu/testes e runner;344 JUnit/7 checks | Local, base88f923e |

### Competições em desenvolvimento contínuo

- [ ] Calendário, inscrição/cancelamento, pontuação e revisão. Schema12 e APIs/menus implementados,355 JUnit anteriores aprovados; teste nativo detectou regressão shiny/SNAPSHOT e está sendo repetido após correção. Não concluir por compilação.
- [ ] Premiação: política/serialização/store/V013 em preparação; ainda não ativados no runtime.

| Entrega | Evidência | Referência |
|---|---|---|
| Fundação competitiva em andamento | CompetitionDefinition/Json/Store, V012, codec/settings/Database/core/menu, testes/runner nativo e proteção de retenção; correção da falha shiny real em validação | Local, base88f923e |

### Fundação competitiva e premiações — validadas localmente

- [x] ~~Calendário/inscrição/cancelamento/pontos SNAPSHOT/LIVE com persistência e revisão.~~356 JUnit/15 checks nativos; erro shiny identificado, corrigido e repetido com sucesso. [Evidência](validation/phase5-foundation.json).
- [x] ~~Pacotes exclusivos TOP1/3/10, redistribuição opcional, expiração, atribuição oficial e resgate pelo journal.~~363 JUnit/6 checks nativos e9 de regressão. [Prêmios](validation/phase5-prizes.json). Backend nativo de material testado foi ITEM; demais adaptadores mantêm escopos anteriores.
- [ ] Histórico e ferramentas administrativas completos: filtros/paginação/consulta de pontos/auditoria implementados, testes complementares em andamento; anúncios/notificações e privacidade detalhada seguem em integração.

| Entrega | Evidência | Referência |
|---|---|---|
| Fundação competitiva | phase5-foundation.json, incluindo erro inicial e correção shiny real | Local, base88f923e |
| Premiações/reuso de entrega | V013/PrizePolicy/RewardSnapshot/Awards/menus/core,363 JUnit e6+9 checks nativos | Local, base88f923e |

### Snapshot 0.6.0-dev.1 — entrega solicitada

- [x] ~~Implementar perfis, privacidade detalhada, rankings de conclusão e conquistas persistentes.~~ Schema14, 382 JUnit/build aprovados; validação nativa completa ainda pendente, portanto Fase6 permanece aberta.
- [ ] Concluir validação nativa de perfis/conquistas, API opcional, menus avançados, notificações, recálculo administrativo e testes finais de carga/cliente.

| Entrega | Evidência | Referência |
|---|---|---|
| Snapshot fases4–6 e JAR experimental | validation/development-0.6.0-dev.1.json; relatórios phase4/phase5; testes e limitações no CHANGELOG | Commit de publicação `feat: publish 0.6.0-dev.1 development snapshot`, branch handoff/crates-checkpoint |

| Distribuição automatizada dev.1 | build.yml: build limpo, anexo CI, download e SHA256 antes da pré-release; contorna falha HTTP401 do upload local sem expor credenciais | Commit `release: distribute v0.6.0-dev.1 snapshot`; funcional e93699b |
