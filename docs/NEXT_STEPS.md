# Próximos passos

## Última tarefa realizada

Wiki inicial implementada na alpha `0.2.0-alpha.3+mc1.21.1-cobblemon1.8.3`, baseada em `5ad1b60ddd5c6ce7353e8ce6942b54064e852128`. Entrega identificada pelo assunto `feat: add searchable paginated species wiki`. Nenhuma fundação ou coleção existente foi reimplementada.

Comandos `/dex <id> wiki [pesquisa]` e botão na página da coleção; pesquisa por nome canônico/ID, paginação de 18 espécies, filtros de estados, ordenação e detalhes. Permissões/critério atual revalidados em callbacks, inclusive após reload. Spawn e captura shiny verificada explicitamente indisponíveis. [Guia e limites](WIKI.md).

78 JUnit (nove novos), zero falhas/erros/skips. Oito checks de wiki, nove de regressão de coleções com LuckPerms e 12 de fundação sem LuckPerms, em servidor dev real. [Evidência](validation/phase2-wiki.json). Uma assertion inicial supôs que só a fixture seria lendária/normal; Regigigas real também é elegível. Teste corrigido para consultar o namespace da fixture, sem alterar critérios do mod.

Servidores encerrados, mundos salvos e SQLite íntegro; configurações/mod anteriores restaurados e senha RCON removida. Artefato em `build/libs/shirokai-dex-0.2.0-alpha.3+mc1.21.1-cobblemon1.8.3.jar`. Não houve publicação de ambiente nem release. Pré-release v0.1.0-alpha.1 permanece imutável. Confirmar SHA e remoto pelo Git.

## Próxima tarefa exata

1. Executar protocolo AGENTS e sincronizar sem descarte. Não reiniciar fundação, coleções ou wiki já implementadas.
2. Continuar Fase 2 pelos **placeholders básicos**: conferir artefato/API real da Text Placeholder API 2.4.2+1.21 e remapeamento no ambiente Mojang; validar registro/expansão no servidor. Expor progresso/total por coleção sem consulta pesada e com semântica explícita para indisponível/permissões. Não sobrescrever alias já registrado; TAB e alias legado exigem testes próprios.
3. Evoluções posteriores da wiki: auditar dados reais de spawn, filtros adicionais por tipo/categoria e consulta de todas as formas além do limite de 32; nomes localizados por cliente se houver API adequada. Manter informação desconhecida distinta de ausência confirmada de spawn natural. Não inventar biomas/horários.
4. Com cliente Minecraft/Cobblemon real disponível, conferir aparência/gestos. Nova alpha fora do Loom e carga continuam pendentes; não reutilizar evidência antiga como certificação do JAR novo.
5. Reutilizar fixtures/testes existentes de datapacks, percentuais, coleções e wiki. `OWNED` não equivale a captura competitiva; shiny visto não comprova captura shiny; não forçar hot reload do registro Cobblemon.
6. Atualizar checklist/estado/auditoria/changelog, executar testes aplicáveis e commit/push autorizado com confirmação remota. Não publicar ambiente nem release sem autorização.

## Arquivos importantes

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
