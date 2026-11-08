# Próximos passos

**Estado atual:** captura/estatísticas iniciais 0.4.0-alpha.1 sobre `26c4472`, branch `handoff/crates-checkpoint`, autorização contínua. Main preservada `c89cc1b`. [Guia](CAPTURES.md), [evidência](validation/phase4-captures.json). Referência: `feat: track verified native captures and permanent statistics`. Sem release/merge/ambiente.

## Publicação em andamento

Pedido explícito de release: `v0.4.0-alpha.1`, alvo `78dfe72`. Workflow `.github/workflows/release-captures-alpha.yml` roda somente no commit de preparação com assunto exato e fonte fixa. Não reexecutar cegamente se draft/release já existir; inspecionar primeiro. Confirmar run, tag, assets, checksums e proveniência antes de atualizar status para publicada. Primeiro run 42431952113 falhou por YAML antes de iniciar; condição corrigida, sem draft criado. Após publicação, continuar proveniência persistente conforme próxima tarefa abaixo. Main/ambiente preservados.

## Última tarefa realizada

Captura nativa comprovada no escopo PlayerSpawner/pool mundial, correlação pré-hit/pós-captura e identidade Pokébola/Pokémon/jogador. Schema6, dedup persistente, snapshots de categorias/regras e agregados permanentes; resumo próprio `/dex profile captures` e consultas por espécie/categoria. Períodos respeitam statistics_timezone (UTC padrão). 304 JUnit e 18 checks nativos/reinício, incluindo regressão de recuperação no schema6.

Caso positivo usou Pokébola/motor reais em criaturas realmente geradas pelo pool; probe não atribui causa nem emite a primeira captura artificialmente. Caso administrativo sem SpawnCause ficou UNKNOWN, e replay não duplicou. Proveniência é perdida no reload da entidade: criatura natural preservada após reinício ficou UNKNOWN, conforme limite explícito. Shiny% antiga segue N/D; estatística shiny nova é separada. Testes encerram/salvam servidores, conferem SQLite e removem segredo RCON automaticamente.

COMMAND enviado em `73e5d78`, recuperação/executor em `26c4472`. Não refazer blocos já validados. CRATE_KEY dedicado continua sem contrato público suficiente no Crates0.2.1.

## Pendência de distribuição

Alpha.5 foi publicada como artifact no Actions run 42400989186, com conferência de hash após upload. A limitação abaixo refere-se a anexos de release, não a esse artifact. Alpha.6/alpha.7/alpha.8 foram sincronizadas na main; a evidência do artifact alpha.8 está na passagem Crates.

O upload do JAR/sources/SHA256SUMS recebeu HTTP 401 Bad credentials em uploads.github.com, embora API e push estejam autorizados. A release informa ausência de JAR anexado. Corrigir o acesso do ambiente ao host de uploads e enviar os arquivos já validados de build/libs antes de recomendar instalação. Nunca solicitar token em chat.

## Próxima tarefa exata

1. Protocolo Git/AGENTS, preservar trabalho e confirmar envio. Autorização contínua somente na branch; main/release/ambiente separados. Blocos maiores, testes relevantes e documentação objetiva.
2. Prioridade funcional: **proveniência persistente e demais fluxos nativos de captura**, antes de rankings/competições. Auditar eventos de spawn/ownership e persistir somente prova observada suficiente; perda de SpawnCause em unload/reinício não pode ser tratada como origem válida por suposição. Definir como impedir reutilização de prova após captura/transferência/recompensa. Não fabricar histórico anterior à instalação.
3. Auditar captura em batalha/pesca/extensões: pre-hit pode não cobrir todos os caminhos. Manter UNKNOWN separado e validar cada origem pelo motor real, sem usar evento artificial como primeira prova positiva.
4. Após ampliar evidência, ligar ledger de capturas shiny à coleção Shiny com denominador/forma atuais, preservando diferença entre visto/obtido/capturado. Stats shiny já existe; não reimplementar ledger/agregados. Recordes/histórico/admin/rate review/retention seguem parciais. Retenção precisa preservar períodos antes de apagar detalhes.
5. Reutilizar `scripts/integration_server.py --suite captures|recovery|commands`, base com assets oficiais. Captures requer build/remapCaptureProbeJar, instala probe/fixture só em /tmp. Schema6: testes de recuperação/seeder já adaptados. Não instalar probes em servidores de usuários.
6. Atualizar checklist/status/next/evidência, commitar/push na branch e confirmar SHA. Não publicar releases, main ou ambiente automaticamente.

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
