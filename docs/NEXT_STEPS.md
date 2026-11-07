# Próximos passos

**Entrega solicitada:** pré-release `v0.3.0-alpha.4`, incluindo commit/push do snapshot atual. Referência de continuidade: essa tag. Após esta entrega, desenvolvimento futuro permanece local até nova autorização. Configuração de ambiente não deve ser publicada.

## Última tarefa realizada

Passagem local de estilo compartilhado nos submenus de coleção, wiki, formas, spawn, prévia e confirmação: paleta, títulos, cartões, rodapé e barra de progresso. Prévia/grade consultam o journal assincronamente para mostrar estado real; confirmação fica oculta em operações concluídas/em processamento/revisão/canceladas. Payloads de itens de recompensa são preservados. Ver `validation/layout-submenus.json`. Snapshot incluído na preparação da pré-release `v0.3.0-alpha.4`.

## Próxima tarefa exata

1. Conferir a tag `v0.3.0-alpha.4` e o estado remoto antes de continuar. Preservar alterações futuras localmente; nova publicação requer autorização.
2. **Prioridade explícita atual: layout geral antes de outros sistemas.** Validar em servidor dev a harmonização aplicada: páginas de progresso, wiki/detalhes/formas/spawn e prévia/confirmação, estados reais e consulta assíncrona. Ajustar runners antigos que ainda esperam botão de resgate depois de concluído; preservar a verificação de não duplicação. Conferir itens personalizados intactos. Manter sistemas ainda não implementados visivelmente indisponíveis, sem dados fictícios.
3. A central/grade e configuração `home` já existem: não refazer. /dex é o hub; seletor é acessado por botão, sem novos aliases que mascarem IDs personalizados. Framework de layouts JSON para todos os menus e aparência em cliente real seguem parciais. Conferir renderização/legibilidade em cliente quando disponível.
4. Não iniciar agora moeda/Impactor, Crates, COMMAND, capturas/competições; usuário pediu retomar sistemas após revisão visual. Pokémon já funciona no escopo descrito em POKEMON_REWARDS.md; formatos/provedores adicionais ficam pendentes, não repetir essa implementação.
5. Documentação objetiva, testes relevantes e blocos maiores; atualizar checklist e provas localmente. A autorização atual cobre apenas esta pré-release, sem configuração de ambiente.

## Arquivos importantes

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

Crates indicado: `https://github.com/istwi/Shirokai-Crates`, revisão `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, versão declarada 0.2.1. Impactor 5.3.5 auditado estaticamente; nenhuma integração econômica foi implementada. Não inventar métodos/coordenadas nem comandos console de chaves.

## Testes pendentes e cuidados

- Fixture atual tem 3 espécies de teste (2 implementadas e 1 não implementada). Runner de coleções exige perfil descartável novo; MC_COLLECTIONS_PLAYER permite nome diferente. RCON usa arquivo de senha temporário fora do Git e somente loopback. Não reutilizar dados de jogadores reais nem registrar credenciais.


- Capturas, recompensas, economia, competições, datapacks reais adicionais e carga ainda não validados funcionalmente. Duas espécies de fixture namespaced foram testadas; não generalizar para todo ecossistema. Percentual shiny deve permanecer N/D até haver fonte de captura verificável.
- Não modificar a Pokédex oficial nem chamar concessões para fabricar progresso.
- Não prometer atomicidade SQLite/inventário/APIs externas.
- Não depender de processos vivos; servidores de teste devem ser encerrados e dados locais ficam fora do Git.
- Preservar inscrição, cancelamento definitivo por edição, calendário, faixas exclusivas e estatísticas permanentes independentes.
- Conferir Actions pelo commit; existência do workflow não prova artifact publicado.

## Publicação da alpha.3 confirmada

Usuário autorizou publicar o JAR. [Pré-release v0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) publicada, não draft e experimental, com tag confirmada no commit `47817bff13cdc3464ad926791233801a01354b54`. Workflow [42365493365](https://github.com/istwi/Shirokai-Dex/actions/runs/42365493365) aprovado e 78 JUnit executados no build de publicação.

Cinco anexos baixados da release e conferidos: JAR instalável, fontes, `phase2-wiki.json`, `BUILD_PROVENANCE.json` e `SHA256SUMS.txt`. Checksums GitHub e manifesto verificados; versão interna do JAR, proveniência e alvo da tag confirmados. [Evidência](validation/release-alpha3.json). O relatório de servidor dev mantém seu hash local separado dos binários de CI. Limites de cliente visual/carga/JAR fora do Loom continuam explícitos. A release antiga permaneceu inalterada; nenhuma configuração de ambiente foi publicada.
