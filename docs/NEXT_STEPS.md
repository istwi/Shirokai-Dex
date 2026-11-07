# Próximos passos

## Última tarefa realizada

Placeholders básicos implementados na alpha `0.2.0-alpha.4+mc1.21.1-cobblemon1.8.3`, base `05a75779a5944a59d41ea0392daa537899f3c759`. Entrega identificada pelo assunto `feat: add optional collection progress placeholders`.

Integração opcional Text Placeholder API 2.4.2+1.21: percent/registered/seen/total/remaining por coleção e prévia pelo parser real em `/dex <id> placeholder <métrica>`. Permissões atuais e N/D explícito; fila de preparo no servidor evita cálculo/SQLite dentro do handler. Registro/limpeza preservam outros provedores; alias legado não é registrado nesta entrega. [Guia](PLACEHOLDERS.md).

87 JUnit, zero falhas/erros/skips; oito checks normais, cinco de probe real e 13 de fundação sem APIs opcionais; limpeza do probe confirmou PASS. [Evidência](validation/phase2-placeholders.json). API original obtida do GitHub oficial por Ivy restrito ao módulo, pois Maven Nucleoid estava bloqueado. Nada foi embutido no JAR; TLS/checksums permaneceram ativos.

Servidores encerrados, mundos salvos, SQLite íntegro e mods/configurações anteriores restaurados; senha RCON removida. Artefato local `build/libs/shirokai-dex-0.2.0-alpha.4+mc1.21.1-cobblemon1.8.3.jar`. Nenhuma nova release ou publicação de ambiente. Alpha.3 publicada permanece imutável. Confirmar SHA/push pelo Git; os workflows antigos de release estão fixados nos commits antigos e não devem publicar esta versão.

## Próxima tarefa exata

1. Executar protocolo AGENTS, sincronizar sem descartar trabalho e consultar checklist/auditoria. Não refazer fundação, coleções, wiki inicial ou os cinco placeholders básicos.
2. Continuar Fase 2 evoluindo a **wiki**: filtros adicionais por tipo/categoria e navegação de formas para retirar a limitação de 32 linhas. Reutilizar snapshots e critérios atuais; dados de spawn exigem auditoria real antes de exibir biomas/horários/dimensões. Manter informação desconhecida distinta de ausência confirmada de spawn natural.
3. Integrações posteriores: alias legado positivo somente quando não houver provedor e com teste próprio; TAB exige distribuição/bridge Fabric compatível identificada e validação ponta a ponta. Não tratar a preservação de alias existente como implementação do alias do Dex.
4. Com cliente real disponível, validar aparência e gestos. Carga e alpha.4 instalada fora do Loom continuam pendentes. Não alegar suporte a consumidor assíncrono: por segurança callbacks fora da thread retornam N/D.
5. Preservar fixtures/testes por namespace, não inferir captura competitiva/estatística permanente de OWNED ou shiny visto, não forçar hot reload do registro Cobblemon e não introduzir placeholders fictícios para sistemas não implementados.
6. Atualizar checklist/estado/auditoria/changelog, validar, commit/push autorizado e confirmar remoto. Não publicar ambiente nem nova release sem autorização específica.

## Arquivos importantes

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
