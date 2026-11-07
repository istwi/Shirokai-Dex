# Próximos passos

## Última tarefa realizada

Categorias nomeadas alpha.7, base `5ac56f2`, commit `feat: add reusable named category rules for collections`. JSON `categories`, composição all/any/none e referência por coleção; wiki/progresso/reload compartilham o mesmo escopo de forma. [Guia](CATEGORIES.md). Evidências e resultados finais em PROJECT_STATUS e validation/phase2-categories.json.

## Próxima tarefa exata

1. Executar protocolo AGENTS e sincronizar sem descartar trabalho. Não refazer fundação, coleções, wiki, placeholders, spawn ou categorias nomeadas.
2. Completar a fundação de evolução das categorias (seção 7.1): definir versão/fingerprint persistido das regras e política de histórico antes de implementar capturas. Não inventar eventos, recálculo ou pontuação. Não implementar competições para simular SNAPSHOT/LIVE.
3. Testar recuperação/reinício e mudança de regras em dados reais descartáveis; histórico/recálculo administrativo continuam abertos até existirem dados suficientes e fluxo de prévia/justificativa/confirmação/auditoria.
4. Cobertura de spawn adicional, TAB/alias legado positivo, cliente visual, carga e instalação da nova alpha fora do Loom continuam pendentes.
5. Trabalhar em blocos coerentes maiores, com documentação objetiva e testes relevantes; não repetir a leitura integral dos mesmos documentos dentro da mesma tarefa sem mudança. Atualizar checklist/estado/API/changelog e evidências; commitar, enviar e confirmar remoto. Não publicar ambiente nem release sem autorização específica.

## Arquivos importantes

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
