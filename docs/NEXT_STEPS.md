# Próximos passos

## Última tarefa realizada

Retomada autorizada após a pausa. Base sincronizada: `41e0b7b3266af4ff2d2b17c2c352b779f68949f7` (coleções JSON). Fundação e coleções já existiam; não foram reimplementadas. Entrega atual identificada pelo assunto `test: validate contextual permissions and protected inventory menus`.

Build atual aprovado com **69 JUnit**, zero falhas/erros/skips. Novo runner de contextos/menus: **8 checks aprovados**, repetidos com novo perfil descartável em servidor de desenvolvimento real, LuckPerms Fabric 5.4.140. Grant/reload por dimensão, retorno/revogação, descarte, double-click, drag, clone criativo/clique externo e integridade de inventário/progresso verificados. [Evidência](validation/phase2-contexts.json), [reprodução](TESTING.md).

Init script opt-in `scripts/server_proxy.init.gradle` repassa proxy/truststore do Gradle à JVM filha de `runServer`; corrigiu falha de download LuckPerms por proxy ausente sem relaxar TLS. LuckPerms usa `world=overworld`, sem namespace, e comandos RCON assíncronos precisam de espaçamento/validação observável. Nenhuma alteração no mod nem mudança de dependências foi necessária. Servidor encerrado, mundos salvos, SQLite íntegro, configuração local anterior restaurada e segredo temporário removido.

Nenhuma release ou configuração de ambiente foi publicada. A pré-release v0.1.0-alpha.1 permanece imutável; o workflow publish-alpha.yml é específico dela. Conferir SHA/push desta entrega com Git; o assunto identifica o commit sem autorreferência circular.

## Próxima tarefa exata

1. Execute o protocolo de AGENTS, sincronize sem descarte e leia checklist/estado/auditoria. Confira o código e não use o antigo resumo “somente esqueleto” como estado atual.
2. Continuar a Fase 2 com a wiki navegável por espécie, pesquisa/filtros/ordenação/paginação e evolução das categorias. Reutilizar coleções JSON, filtros, modelos e percentuais existentes; auditar os métodos de nome/formas/tipos/dados de spawn antes da integração. Informação de spawn desconhecida deve permanecer explicitamente indisponível.
3. Quando houver cliente Minecraft/Cobblemon real: validar aparência e gestos na interface. Os pacotes já testados não certificam renderização. Contextos LuckPerms adicionais/herança e desempenho continuam complementares; não refazer grants globais e dimensão já verificados.
4. Reutilizar a fixture namespaced e os testes de totais entre reinícios para expandir datapacks/formas. O registro Cobblemon não é seguro para hot reload; não chamar reload de espécies para forçar o teste.
5. Não tratar OWNED como captura competitiva nem inferir captura shiny a partir de shiny visto. Capturas, recompensas/economia e competições continuam fora desta entrega.
6. Atualizar checklist, estado, próximos passos, auditoria e changelog; executar testes aplicáveis, commit/push autorizado e confirmação remota. Não publicar ambiente nem release sem autorização.

## Arquivos importantes

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
