# Próximos passos

## Última tarefa realizada

Início Fase 2 com pedido explícito de %: Nacional/Lendária exibem quantidade/total e percentual em menu/status; seletor e páginas com retorno interno; Shiny N/D sem inventar captura histórica. Cache por UUID invalidado por evento oficial. 32 JUnit, regressão de 11 checks, progresso de 6 checks, reconexão/lore e remoção de datapack aprovados, inclusive JAR instalado fora do Loom. [Evidências](validation/phase2-percentages.json).

Base remota anterior `93185ae`; localizar esta entrega pelo assunto `feat: add verified Pokedex completion percentages`. Versão do build: `0.2.0-alpha.1+mc1.21.1-cobblemon1.8.3`. Não sobrescrever a release anterior `v0.1.0-alpha.1`: publicada e verificada, alvo c11b366. O workflow publish-alpha.yml é específico da release antiga e não serve para publicar esta versão.

## Próxima tarefa exata

1. Execute o protocolo de AGENTS: sincronize sem descarte, leia checklist/estado e confira o código.
2. Feche testes complementares: auditar/instalar LuckPerms Fabric compatível e testar permissão positiva/contextos e `/dex admin reload` válido/inválido. Ausência do provedor já nega a ação; não usar OP como substituto.
3. Testar arraste, double-click/descarte e aparência no cliente Minecraft/Cobblemon real; o smoke não valida renderização.
4. Continuar a Fase 2 a partir do cálculo/menus já implementados em `pokedex`, `cobblemon` e `menus`; não reimplementar percentuais. Adicionar modelos de coleções/categorias JSON validados e consultas de leitura. Não tratar OWNED como captura competitiva nem inferir captura shiny de shiny visto. Documentar política para dados que não podem ser reconstruídos.
5. Reutilizar a fixture namespaced e os testes de totais entre reinícios já existentes; expandir para datapacks reais/casos de formas conforme os novos critérios, sem recriar a validação concluída. Cobblemon declara espécies inseguras para hot reload; não forçar reload do registro oficial.
6. Atualizar checklist/estado/próximos passos/auditoria/changelog; testes, commit, push autorizado e confirmação remota. Não publicar ambiente/release estável sem autorização.

## Arquivos importantes

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

Não presumir caminhos/caches/processos em outra máquina. `runServer` requer terminal interativo e configuração descartável com EULA aceita pelo responsável; veja TESTING. Diretórios ignorados e `/tmp` não são fontes de continuidade.

Crates indicado: `https://github.com/istwi/Shirokai-Crates`, revisão `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, versão declarada 0.2.1. Impactor 5.3.5 auditado estaticamente; nenhuma integração econômica foi implementada. Não inventar métodos/coordenadas nem comandos console de chaves.

## Testes pendentes e cuidados

- Capturas, recompensas, economia, competições, datapacks reais adicionais e carga ainda não validados funcionalmente. Duas espécies de fixture namespaced foram testadas; não generalizar para todo ecossistema. Percentual shiny deve permanecer N/D até haver fonte de captura verificável.
- Não modificar a Pokédex oficial nem chamar concessões para fabricar progresso.
- Não prometer atomicidade SQLite/inventário/APIs externas.
- Não depender de processos vivos; servidores de teste devem ser encerrados e dados locais ficam fora do Git.
- Preservar inscrição, cancelamento definitivo por edição, calendário, faixas exclusivas e estatísticas permanentes independentes.
- Conferir Actions pelo commit; existência do workflow não prova artifact publicado.
