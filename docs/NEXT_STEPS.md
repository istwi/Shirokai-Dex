# Próximos passos

## Última tarefa realizada

Filtros e navegação completa de formas da wiki na alpha `0.2.0-alpha.5+mc1.21.1-cobblemon1.8.3`, base `ef99403dbc2c3463cd1e55e91abe2202046e8a89`. Entrega identificada por `feat: add wiki type filters and complete form pagination`.

Seletores paginados de tipo/label, combinados na mesma forma com critérios JSON e estados. Todas as formas em páginas de 18, sem corte de 32; consulta/filtros preservados. Menus de wiki com quatro linhas vanilla, demais menus permanecem com três. Permissões/definições atuais revalidadas a cada navegação. [Guia](WIKI.md).

95 JUnit (oito novos), zero falhas/erros/skips; oito checks novos e oito de regressão da wiki em servidor real. Fixture própria comprovou 41 formas, sem confundir progresso da padrão com variantes. Callback antigo após reload ajusta página/critério/idioma; inventário e progresso não são modificados pela navegação. [Evidência](validation/phase2-wiki-facets.json).

Servidores encerrados e mundos salvos; SQLite íntegro; propriedades anteriores restauradas e RCON temporário removido. JAR local alpha.5 em build/libs. Nenhuma nova release ou configuração de ambiente publicada. Alpha.3 e workflows de publicação antigos continuam imutáveis/fixados nos seus commits; não usá-los para publicar esta versão. Conferir SHA/push pelo Git.

## Próxima tarefa exata

1. Executar protocolo AGENTS e sincronizar sem descarte. Não refazer coleções, filtros/formas da wiki ou placeholders básicos.
2. Continuar Fase 2 pela **auditoria dos dados de spawn do Cobblemon 1.8.3**: localizar fonte/API real de condições e sua correspondência com espécie/forma. Implementar somente informações demonstradas no servidor, mantendo desconhecido distinto de ausência confirmada de spawn natural; considerar que outros spawners/datapacks podem não estar cobertos. Não inventar biomas, dimensões, horários ou suporte universal.
3. Categorias nomeadas/condições avançadas/histórico continuam parciais; os novos seletores são labels do registro, não essa implementação completa. Reutilizar critérios existentes.
4. Integrações posteriores: alias legado positivo sem conflito e com teste próprio; TAB requer distribuição/bridge compatível e validação própria. Chamadas assíncronas dos placeholders continuam retornando N/D.
5. Cliente visual, carga e nova alpha instalada fora do Loom seguem pendentes. Preservar fixtures separadas e reiniciar para trocar espécies; não fazer hot reload de registros oficiais nem tratar obtenção por comando como captura competitiva.
6. Atualizar checklist/estado/auditoria/changelog, validar e fazer commit/push autorizado com confirmação remota. Não publicar ambiente nem release nova sem autorização específica.

## Arquivos importantes

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
