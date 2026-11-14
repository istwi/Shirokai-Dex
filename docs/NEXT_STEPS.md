# Próximos passos

## Publicação dev.1 confirmada

[Pré-release com JAR](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.6.0-dev.1) publicada pelo [Actions aprovado](https://github.com/istwi/Shirokai-Dex/actions/runs/43585022162), fonte 9714b99. Quatro anexos baixados e manifesto SHA256 conferido localmente. Binário publicado idêntico ao JAR validado nativamente. [Evidência](validation/release-development-0.6.0-dev.1.json). Código/perfis/validação complementar enviados na branch handoff/crates-checkpoint; main e ambiente preservados. Pendências funcionais continuam abertas.

## Validação complementar de perfis — 2026-11-14

Suíte nativa concluída: nove checks e um de reinício aprovados, três inicializações/encerramentos normais e segredo RCON removido. [Evidência](validation/phase6-profiles.json), incluindo falha inicial e repetição corrigida. Perfis offline, privacidade pública, Pokédex oficial, conquistas/favoritos e ausência de concessões materiais indevidas verificados por protocolo. Próximo: placeholders avançados/menus/notificações e demais pendências; cliente gráfico/carga não certificados. Esta atualização substitui as menções anteriores de repetição pendente.

## Passagem 0.6.0-dev.1 — estado vigente

Última tarefa: preparar snapshot publicável na branch handoff/crates-checkpoint por solicitação explícita do responsável. Build e 382 JUnit aprovados; schema14/perfis/conquistas estão ativos (as notas históricas de preparação abaixo estão superadas).

Próxima tarefa exata: concluir scripts/integration_server.py --base /tmp/shirokai-packaged-base-v2 --suite profiles --root /tmp/shirokai-profiles-native-v2; investigar qualquer falha sem enfraquecer expectativas. A primeira execução falhou em clique de janela obsoleta; correção de token da tela e estabilização do cliente aplicadas. Registrar resultado antes de considerar Fase6 validada nativamente.

Depois: placeholders avançados com cache limitado e privacidade, integração real da API opcional/alias legado, menus JSON para novas telas, notificações, preferências de idioma, recálculo administrativo de categorias com prévia/motivo/auditoria, carga e validação final. CRATE_KEY dedicado e versões externas não auditadas permanecem sem suporte comprovado. Não certificar cliente gráfico ou modpack de produção a partir de bots.

Arquivos centrais: core/ShirokaiDex.java, database/Database.java, profiles/, achievements/, menus/, placeholders/, V014, Profile*Test/AchievementTest e tests/integration/profiles.cjs. Preservar migrações e fixtures legadas; nenhuma alteração na Pokédex oficial. Publicação desta versão não autoriza alterar main/configuração do ambiente.

## Trabalho ativo — continuidade sem parar

363 JUnit e blocos nativos 15+6+9 aprovados; ver relatórios phase5*. Complemento de histórico/auditoria tem duas novas classes de cenário em CompetitionTest ainda aguardando próxima execução integral após última edição. Perfil/privacidade/Fase6 em preparação: StatisticsPrivacy/ProfileProgress/CapturedForms/V014 existem no fonte, mas schema14 e lifecycle ainda não ativados. Próximo: requisito/conquistas persistentes, snapshots oficiais de perfil e privacidade aplicada a ranking/perfil/placeholders. API org.sqlite.Collation.create/ xCompare conferida por javap no sqlite-jdbc3.47.1.0 para comparação racional exata (sem índice persistente com collation customizada). Preservar todos os arquivos locais; não publicar e não encerrar a cada fase.


## Trabalho ativo — competições

Correção de SNAPSHOT shiny em CompetitionStore.matches, teste shinyCaptureCanEvaluateFrozenCategory..., build em /tmp/shirokai-competition-shiny-fix.log. Próxima ação: após build, repetir scripts/integration_server.py --base /tmp/shirokai-packaged-base-v2 --suite captures --competitions --root /tmp/shirokai-competition-native-v2. Primeira tentativa falhou na classificação shiny; mundo descartável encerrado e senha removida. Fonte completa/evidência de falha em build/integration-captures.json (preservar em relatório final). Depois ativar/testar prêmios e resgate: PrizePolicy/RewardSnapshot/CompetitionAwards/V013 existem no fonte, mas schema13/modelo de política ainda não integrados. Store já implementa adjust/revert; falta exposição admin. Retenção protege edições pendentes/auditorias.


## Continuação contínua solicitada — 2026-11-14

O responsável pediu executar todas as fases sem encerrar a cada bloco, com testes normais e sequência otimizada. Sem publicação. Recordes/rankings de capturas validados com 344 JUnit/7 checks nativos. Em desenvolvimento: calendário/inscrição/cancelamento/pontuação/revisão de competições, CompetitionDefinition e V012 ainda não integrados ao runtime. Preservar schema11 e registros permanentes; integrar holds de histórico antes de ativar pontos competitivos.


## Continuação local — recibos e retenção (2026-11-14)

Base remota continua 88f923e; histórico e novo bloco permanecem locais. Schema10 com capture_receipts, migração/backfill sem inventar eventos, histórico verificado opcional e cleanup limitado. Build/JAR e 337 JUnit aprovados; migração/limpeza/reinício nativos aprovados ([evidência](validation/phase4-retention.json)). Próximo: recordes e rankings permanentes com data de atingimento/UUID para desempate, sem inventar data histórica indisponível. Antes de competições/revisão de capturas, adicionar proteção contra limpeza dos detalhes necessários; ver RETENTION.md. Não publicar nada nesta retomada.


## Retomada local — 2026-11-14

Base real 88f923e, branch handoff/crates-checkpoint. Novo histórico administrativo `/dex admin captures <player_uuid> [page]`, permissão shirokaidex.admin.captures; guia CAPTURE_HISTORY.md. Build/JAR e 328 JUnit aprovados; seis checks do comando/ACL em servidor Loom passaram ([evidência](validation/phase4-history.json)); fora do Loom pendente. Próximo bloco: implementar projeções permanentes de períodos/recordes/rankings e deduplicação independente ANTES de retenção: summary hoje consulta capture_events e event_key é sua barreira contra replay. Não apagar eventos sem preservar ambos. Alterações desta retomada locais.


**Estado atual de desenvolvimento:** alpha.5, coleção Shiny ligada à captura verificada; release pública alpha.1 preservada. Próximo: histórico, recordes/rankings e retenção. Histórico: captura/estatísticas iniciais 0.4.0-alpha.1 sobre `26c4472`, branch `handoff/crates-checkpoint`, autorização contínua. Main preservada `c89cc1b`. [Guia](CAPTURES.md), [evidência](validation/phase4-captures.json). Referência: `feat: track verified native captures and permanent statistics`. Sem release/merge/ambiente.

## Publicação concluída

[v0.4.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.4.0-alpha.1) publicada e verificada, tag/source `78dfe72`. Workflow [42432155833](https://github.com/istwi/Shirokai-Dex/actions/runs/42432155833) passou e quatro anexos foram baixados/conferidos também neste ambiente. JAR coincide com o binário nativo validado; 304 JUnit/18 checks documentados. [Evidência](validation/release-captures-alpha1.json). Não alterar essa release/workflow de source fixo para distribuir desenvolvimento posterior. Proveniência persistente já avançou na alpha.2; próximo bloco audita demais fluxos nativos; main/ambiente não publicados.

## Última tarefa realizada

Alpha.5: coleção Shiny verificada, schema9 e cache/projeção permanente; 326 JUnit, suítes nativas no [relatório](validation/phase4-shiny.json). Entrega `feat: derive shiny collection progress from verified captures`, base b359a12. Próxima tarefa é histórico/rankings/recordes/retenção. Release somente após toda a Fase4, conforme esclarecimento do responsável.

### Histórico alpha.4

Alpha.4: pesca nativa, fonte persistente e schema8; 317 JUnit, suite fishing e regressões registradas no [relatório](validation/phase4-fishing.json). Entrega `feat: track verified native fishing capture provenance`, base d967c98. CaptureAccess/NativeSpawnProof/NativeProvenance/V008/Database, quatro testes novos, probe/fixture/runner fishing e seeder/executor. Sem nova release. Próxima instância começa a ponte de capturas shiny, preservando prova/denominador e permissões.

### Histórico alpha.3

Alpha.3: captura PvE nativa/LIVE_NATIVE, replay e reinício aprovados; 313 JUnit e dez checks nativos. Auditoria estática da pesca identificou causa/spawner próprios, sem ampliar origem aceita. Entrega `test: validate native battle capture provenance`, base3f4adaf; [evidência](validation/phase4-battle.json). Arquivos novos/alterados: CaptureProbe, captures.cjs, build.gradle e documentação. Próxima instância deve continuar a pesca, sem refazer batalha/ledger. Sem nova release.

### Histórico alpha.2

Alpha.2: proveniência persistente, nove JUnit novos (313 totais), captura real após reinício e regressão de recuperação no schema7. Entrega `feat: preserve observed native spawn provenance across reloads`, base a082358. [Evidência](validation/phase4-provenance.json). Sem nova release.

### Histórico alpha.1

Captura nativa comprovada no escopo PlayerSpawner/pool mundial, correlação pré-hit/pós-captura e identidade Pokébola/Pokémon/jogador. Schema6, dedup persistente, snapshots de categorias/regras e agregados permanentes; resumo próprio `/dex profile captures` e consultas por espécie/categoria. Períodos respeitam statistics_timezone (UTC padrão). 304 JUnit e 18 checks nativos/reinício, incluindo regressão de recuperação no schema6.

Caso positivo usou Pokébola/motor reais em criaturas realmente geradas pelo pool; probe não atribui causa nem emite a primeira captura artificialmente. Caso administrativo sem SpawnCause ficou UNKNOWN, e replay não duplicou. Proveniência é perdida no reload da entidade: criatura natural preservada após reinício ficou UNKNOWN, conforme limite explícito. Shiny% antiga segue N/D; estatística shiny nova é separada. Testes encerram/salvam servidores, conferem SQLite e removem segredo RCON automaticamente.

COMMAND enviado em `73e5d78`, recuperação/executor em `26c4472`. Não refazer blocos já validados. CRATE_KEY dedicado continua sem contrato público suficiente no Crates0.2.1.

## Histórico de distribuição

O bloqueio antigo de uploads da alpha.4 permaneceu limitado àquela tentativa. A release0.4.0-alpha.1 foi publicada pelo Actions com anexos e validada independentemente; não permanece bloqueada. Download de artifacts do Actions via host de armazenamento continua bloqueado no Cloud, mas download dos anexos da release pública funcionou. Não solicitar credencial em chat nem alterar ambiente automaticamente.

## Próxima tarefa exata

1. Protocolo Git/AGENTS, preservar trabalho e confirmar envio. Autorização contínua somente na branch; main/release/ambiente separados. Blocos maiores, testes relevantes e documentação objetiva.
2. Proveniência persistente concluída no escopo PlayerSpawner/reload na alpha.2: não reimplementar. Consultar [evidência](validation/phase4-provenance.json), NativeProvenance/CaptureObservation/V007 e nove testes novos. Provas consumidas não reabrem; eventos anteriores não são reclassificados.
3. Captura PvE/PlayerSpawner e pesca nativa validadas no escopo dos relatórios alpha.3/alpha.4. Não reimplementar. Pesca usa singleton/causa/vara/jogador observados, native_source e schema8; [guia](FISHING.md). Outros pools/iscas/varas, batalhas duplas e extensões continuam pendentes. UNKNOWN nunca vira origem legítima por suposição.
4. Próxima tarefa funcional: histórico paginado próprio/administrativo com permissões, rankings permanentes e recordes; depois retenção que preserve agregados/períodos/provas consumidas. A ponte Shiny já foi implementada: não reimplementar. Consultar [guia](SHINY_CAPTURES.md), CapturedShinyForm/ProgressCalculator, projeção V009/CaptureLedger/Database e cache assíncrono em CobblemonAccess/core. Capturas repetidas são ações múltiplas, mas uma espécie na coleção. Nunca inferir prova de visto/obtido.
5. Reutilizar `scripts/integration_server.py --suite captures|fishing|recovery|commands`, base com assets oficiais. Captures requer build/remapCaptureProbeJar, instala probe/fixture só em /tmp. Schema9: testes de recuperação/seeder já adaptados. Não instalar probes em servidores de usuários.
6. Atualizar checklist/status/next/evidência, commitar/push na branch e confirmar SHA. Usuário esclareceu: publicar release somente após toda a Fase4 (histórico/retenção/recordes/rankings incluídos). Main e ambiente não autorizados.

## Arquivos importantes

- Capturas: pacote captures (Record/Tracker/Access/Ledger), V006, Database, timezone em Settings/ConfigManager, eventos e comandos em ShirokaiDex. CaptureTest, captureProbe separado/remapeado, fixture/runner captures e executor.

- Alpha.11: RewardJournal.resumable/authorizeResume, Database.authorizeRewardResume, RewardDelivery.resume, previewRewardResume/deliverReward no core, MainMenu.confirmRecovery, RewardRecoveryTest; fixture/runner/seeder recovery e `scripts/integration_server.py`.

- COMMAND alpha.10: CommandReward/CommandPolicy/CommandRewardAdapter, CommandRewardsCodec/Settings, RewardPlan.definition/reuse/commands, RewardJournal.steps/RewardDelivery.ManualReview, core/menus/traduções; CommandRewardsTest e fixture/runner commands em dois modos. Nenhuma migração SQLite nova.

- Checkpoint: PlayerSaveCoordinator/ItemRewardAdapter, `src/checkpointProbe`, `tests/integration/crates-checkpoint.cjs` (quatro modos), manifestos/fixture crates-checkpoint. Remapeamento Pokémon de regressão em build.gradle; ver relatório e passagem atualizados.

- Moedas: CurrencyReward, CurrencyRewardAdapter/Port, ImpactorCurrencyPort, CurrencyPersistence; RewardPlan/RewardsCodec/RewardJournal.steps, core/menus/localização. CurrencyRewardsTest/CurrencyPersistenceTest, fixture e runner currency.cjs (normal/without-impactor/after-restart), manifesto oficial runtime-mods.json. Guia CURRENCY_REWARDS.md.

- Aparência: `MenuAppearance`, `MenuNavigation.Layout`, `MenuStyle.appearance`, `MenuAppearanceTest` e CollectionValidator (snapshot de ícones). Runner/fixture `menu-appearance`, seeder com `--fixture menu-appearance`; exporta metadados de teste, não dados de usuários.
- Nova navegação: `menus/layout/MenuNavigation`, `config/MenuNavigationCodec`, `Settings.menus`, `MenuStyle.navigation`, `MenuNavigationTest`; fixture/runner `submenus`, seeder reservado para servidor descartável parado.
- Layout: `HomeMenu`, `menus/layout/HomeLayout`, `HomeLayoutCodec`, `MainMenu.show`/tamanhos e callbacks em ShirokaiDex; HomeLayoutTest, runner home-layout.cjs, fixture layout e probe separado layoutProbe. Helpers antigos de teste agora entram no seletor clicando na central.
- Pokémon: `PokemonReward`, `PokemonRewardsCodec`, `PokemonPayload`, `PokemonRewardAdapter`, RewardPlan.steps/definition/reuse e journal compatível. `pokemonProbe` só de testes; nunca incluir no JAR. Leitura assíncrona de NBT não escreve stores nem consulta registries fora da thread do servidor.

- `SavedItem`, `ItemPayload`, `RewardPlan.Item.data`, `RewardsCodec`, `ConfigManager.resolvedBackup`, migração V005 e APIs salvas no Database. Core agenda validação de payloads na thread do servidor; shutdown cancela esperas. `SavedItems*Test`, runner `saved-items.cjs` (normal/after-restart/review-list) e fixture saved-items.

- Pacote `rewards`, `RewardsCodec`, migrações V003/V004, core/MainMenu/idiomas, `Reward*Test`, runners `rewards.cjs`/`reward-unlocks.cjs` e fixtures de recompensas. Versão local 0.3.0-alpha.1; 159 JUnit e 25 checks de servidor. Adaptador relê inventário NBT porque save vanilla suprime exceções; falha real foi testada e ficou em NEEDS_REVIEW após reinício.

- `categories/CategoryCatalog`, `config/CategoriesCodec`, `CollectionDefinition.CategorySelection`, Settings/ConfigManager/CollectionValidator; `CategoryCatalogTest`, `CategoryIntegrationTest`, runner `tests/integration/categories.cjs` e fixture `tests/fixtures/categories/config.json`. Categoria é regra sem ACL própria; acesso pertence à coleção.

- `spawning/SpawnAccess.java`, `SpawnCatalog.java`, `SpawnCatalogTest`, lifecycle em `ShirokaiDex`, `MainMenu.spawnItem`, `tests/integration/spawns.cjs` e `tests/fixtures/spawning`: entrega alpha.6.

- `Wiki.java` (Request/Facet/Slice), `MainMenu.openWiki`/`formItem`, `WikiFacetsTest`, `tests/integration/wiki-facets.cjs` e `tests/fixtures/wiki-facets`: entrega atual.

- Pacote `placeholders`, `PlaceholderTest`, `TextPlaceholderAccess`, lifecycle em `ShirokaiDex`, runner `tests/integration/placeholders.cjs`, fixture `tests/fixtures/placeholders/runtime.json` e probe separado em `src/placeholderProbe`: entrega atual. [Sintaxe e limites](PLACEHOLDERS.md).

- `wiki/Wiki.java`, `WikiTest`, `MainMenu.openWiki`, `CobblemonAccess.wiki`, comandos em `ShirokaiDex`, `tests/integration/wiki.cjs` e [guia](WIKI.md): entrega atual.

- `scripts/server_proxy.init.gradle`, `tests/integration/contexts.cjs`, `tests/fixtures/contexts/config.json` e [evidência de contextos](validation/phase2-contexts.json): complemento de validação mais recente.

- [Coleções JSON](COLLECTIONS.md), CollectionDefinition, CollectionsCodec, CollectionValidator e os testes CollectionsConfigTest/ConfiguredProgressTest; runtime em tests/integration/collections.cjs e fixture tests/fixtures/collections.


- [Porcentagens e coleções](POKEDEX.md), `src/main/java/br/com/shirokaidex/pokedex`, `DexProgressTest`, `tests/integration/progress.cjs` e `tests/fixtures/datapacks/dex-progress`: trabalho atual. A forma padrão precisa ser incluída mesmo quando Species.forms está vazio.


- [Checklist](MASTER_CHECKLIST.md), [prompt integral](PROJECT_SPECIFICATION.md), [auditoria](API_COMPATIBILITY.md), [estado](PROJECT_STATUS.md).
- `src/main/java/br/com/shirokaidex/core/ShirokaiDex.java`: lifecycle, comandos e integração assíncrona.
- Pacotes `config`, `database`, `cobblemon`, `menus`, `permissions`, `localization`: fundação existente.
- Recursos `database/V001__foundation.sql`, traduções e `fabric.mod.json`.
- `src/test`, `tests/integration/foundation.cjs`, `scripts/gradle_cloud.py`: testes e helper.
- [Instalação](INSTALLATION.md), [testes](TESTING.md), [configuração](CONFIGURATION.md), [banco](DATABASE.md), [diagnóstico](TROUBLESHOOTING.md).

## Dependências e problemas ainda não resolvidos

Minecraft 1.21.1; JDK 21; Fabric Loader 0.17.2; Fabric API 0.116.6+1.21.1; Cobblemon 1.8.3+1.21.1; Fabric Kotlin 1.13.6+kotlin.2.2.20; Gradle 8.14.3; Loom 1.11.8; plugin Kotlin 2.2.20; ICU dev 71.1; SQLite JDBC 3.47.1.0; LuckPerms API compileOnly 5.4; JUnit 5.11.4.

Na máquina atual, JDK verificado em `/workspace/.tools/jdk-21.0.13+9`. Build usado:

```sh
JAVA_HOME=/workspace/.tools/jdk-21.0.13+9 python3 scripts/gradle_cloud.py -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon build
```

Não presumir caminhos/caches/processos em outra máquina. Para `runServer` cloud, acrescente `--init-script scripts/server_proxy.init.gradle` ao comando acima, trocando `build` por `runServer`; a JVM filha precisa do proxy para dependências LuckPerms. `runServer` requer terminal interativo e configuração descartável com EULA aceita pelo responsável; veja TESTING. Diretórios ignorados e `/tmp` não são fontes de continuidade.

Crates indicado: `https://github.com/istwi/Shirokai-Crates`, revisão `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, versão declarada 0.2.1. Impactor 5.3.5/JSON nativo foi integrado e testado na alpha.8; demais backends não certificados. Não inventar métodos/coordenadas nem comandos console de chaves.

## Testes pendentes e cuidados

- Fixture atual tem 3 espécies de teste (2 implementadas e 1 não implementada). Runner de coleções exige perfil descartável novo; MC_COLLECTIONS_PLAYER permite nome diferente. RCON usa arquivo de senha temporário fora do Git e somente loopback. Não reutilizar dados de jogadores reais nem registrar credenciais.


- Capturas, competições, datapacks reais adicionais e carga ainda não validados funcionalmente; recompensas/economia apenas nos escopos dos relatórios. Duas espécies de fixture namespaced foram testadas; não generalizar para todo ecossistema. Percentual shiny deve permanecer N/D até haver fonte de captura verificável.
- Não modificar a Pokédex oficial nem chamar concessões para fabricar progresso.
- Não prometer atomicidade SQLite/inventário/APIs externas.
- Não depender de processos vivos; servidores de teste devem ser encerrados e dados locais ficam fora do Git.
- Preservar inscrição, cancelamento definitivo por edição, calendário, faixas exclusivas e estatísticas permanentes independentes.
- Conferir Actions pelo commit; existência do workflow não prova artifact publicado.

## Publicação da alpha.3 confirmada

Usuário autorizou publicar o JAR. [Pré-release v0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) publicada, não draft e experimental, com tag confirmada no commit `47817bff13cdc3464ad926791233801a01354b54`. Workflow [42365493365](https://github.com/istwi/Shirokai-Dex/actions/runs/42365493365) aprovado e 78 JUnit executados no build de publicação.

Cinco anexos baixados da release e conferidos: JAR instalável, fontes, `phase2-wiki.json`, `BUILD_PROVENANCE.json` e `SHA256SUMS.txt`. Checksums GitHub e manifesto verificados; versão interna do JAR, proveniência e alvo da tag confirmados. [Evidência](validation/release-alpha3.json). O relatório de servidor dev mantém seu hash local separado dos binários de CI. Limites de cliente visual/carga/JAR fora do Loom continuam explícitos. A release antiga permaneceu inalterada; nenhuma configuração de ambiente foi publicada.
