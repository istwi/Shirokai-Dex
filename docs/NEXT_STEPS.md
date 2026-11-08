# Próximos passos

**Estado atual:** alpha.11, recuperação segura e testes automatizados; base enviada `73e5d78`, branch `handoff/crates-checkpoint`, main preservada `c89cc1b`. Autorização contínua de commits/push na branch. [Guia](RECOVERY.md), [evidência](validation/phase3-recovery.json). Referência da entrega: `feat: add safe supervised reward recovery and reusable server suites`. Sem release/publicação de ambiente.

## Última tarefa realizada

Recuperação administrativa apenas de operações sem etapas, com prévia/confirmacão, motivo e auditoria. Plano/ID preservados, permissões atuais de operador/destinatário, desconexão/reload e concorrência seguros no escopo demonstrado. 290 JUnit e 18 checks de servidor (9 recuperação + 9 regressão COMMAND, com reinícios). INTENT/prefixo confirmado não são reenviados. Schema continua 5.

Executor `scripts/integration_server.py` e fixture/runner/seeder de recuperação reutilizam os assets de um servidor empacotado, criam mundos novos e fazem boot/teste/reinício/cleanup. Não dependem de processos vivos. COMMAND alpha.10 já foi enviado em `73e5d78`; não refazer. Checkpoint alpha.9/SAVED_ITEM permanece validado; CRATE_KEY dedicado continua sem contrato público suficiente no Crates0.2.1.

## Pendência de distribuição

Alpha.5 foi publicada como artifact no Actions run 42400989186, com conferência de hash após upload. A limitação abaixo refere-se a anexos de release, não a esse artifact. Alpha.6/alpha.7/alpha.8 foram sincronizadas na main; a evidência do artifact alpha.8 está na passagem Crates.

O upload do JAR/sources/SHA256SUMS recebeu HTTP 401 Bad credentials em uploads.github.com, embora API e push estejam autorizados. A release informa ausência de JAR anexado. Corrigir o acesso do ambiente ao host de uploads e enviar os arquivos já validados de build/libs antes de recomendar instalação. Nunca solicitar token em chat.

## Próxima tarefa exata

1. Conferir Git/status/remotos, preservar mudanças e usar autorização contínua somente na branch de desenvolvimento. Main/release/ambiente continuam sob autorização separada. Trabalhar em blocos maiores e reduzir documentação repetida.
2. Próximo bloco funcional: **captura legítima e estatísticas permanentes**, seções 10–12 do prompt. Auditar origem, deduplicação por IDs confiáveis e datas/timezone configurável antes de pontuação. Não inferir captura de obtenção, shiny visto, troca, ovo ou prêmio.
3. APIs candidatas reais: `PokemonCapturedEvent.getPokemon/getPlayer/getPokeBallEntity`; `EmptyPokeBallEntity.getCapturingPokemon`; `PokemonEntity.getSpawnCause`; `PokemonSpawnAction` atribui a causa ao criar entidade. Confirmar ordenação/validade no evento e persistência através de unload/reinício. Origem ausente/desconhecida não vira captura legítima por suposição. Validar captura nativa positiva, não só emissão artificial de evento/probe.
4. Usar executor `scripts/integration_server.py` para repetir suítes. Base precisa de server.jar/profile/dependências originais; caminhos locais são caches, não fonte de continuidade. Em outra máquina seguir INSTALLATION/TESTING. Preservar TLS/checksums/proxy.
5. Recuperação parcial com INTENT/prefixo e recibos automáticos continuam abertos; manter bloqueio/revisão. COMMAND isolado e recuperação sem etapas já estão implementados: não refazer. Recibos/integrações opcionais/acabamento visual podem ficar para depois dos essenciais.
6. Atualizar checklist/status/next, evidência objetiva e testes relevantes, commitar/push na branch e confirmar SHA remoto. Não publicar releases/main/ambiente automaticamente.

## Arquivos importantes

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
