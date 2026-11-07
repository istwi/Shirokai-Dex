# Shirokai Dex

Fundação experimental de mod server-side para Minecraft 1.21.1, Fabric, Java 21 e Cobblemon 1.8.3.

Na versão de desenvolvimento 0.2.0-alpha.4, Nacional e Lendária mostram **quantidade/total e porcentagem** em menu e status. Shiny é N/D enquanto não houver prova confiável de captura. [Regras e comandos](docs/POKEDEX.md).

Disponível: `/dex`, resumo somente de leitura da Pokédex oficial, menu inicial protegido, configuração validada com recuperação e SQLite com migração versionada. Coleções com filtros/modos/permissões por JSON e seletor paginado estão disponíveis: veja [configuração das coleções](docs/COLLECTIONS.md). A [wiki de espécies](docs/WIKI.md) permite pesquisa, filtros, ordenação, paginação e detalhes por coleção. [Placeholders básicos](docs/PLACEHOLDERS.md) expõem progresso, vistas, total e restantes pela Text Placeholder API opcional. Recompensas, capturas, rankings e competições continuam pendentes.

Validação da alpha.4: **87 testes JUnit**; **8 checks de placeholders**, **5 de parser/contextos/conflitos** e **13 de fundação sem APIs opcionais** em servidor de desenvolvimento real. [Testes e limites](docs/TESTING.md). TAB, cliente visual, carga e instalação desta alpha fora do Loom continuam pendentes.

[Baixar a pré-release 0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) — JAR, fontes, checksums e relatório de validação.

[Baixar a pré-release anterior 0.1.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.1.0-alpha.1) — JAR, fontes, checksums e relatório de validação.

## Instalar e desenvolver

Com JDK 21 completo: `./gradlew --no-daemon build`.

Em cloud com proxy herdado: `python3 scripts/gradle_cloud.py --no-daemon build`, usando a truststore do ambiente quando necessária. Não desabilite TLS/checksums. Artefato: `build/libs/shirokai-dex-0.2.0-alpha.4+mc1.21.1-cobblemon1.8.3.jar`; não instalar `-sources.jar`.

Leia [instalação](docs/INSTALLATION.md), [configuração](docs/CONFIGURATION.md), [comandos](docs/COMMANDS.md), [permissões](docs/PERMISSIONS.md), [banco](docs/DATABASE.md) e [diagnóstico](docs/TROUBLESHOOTING.md). O workflow [Java 21](.github/workflows/build.yml) gera artifact identificado pelo commit quando passa; confira o resultado no GitHub.

## Continuidade obrigatória

Antes de trabalhar, leia [AGENTS.md](AGENTS.md), [checklist](docs/MASTER_CHECKLIST.md), [estado](docs/PROJECT_STATUS.md), [próximos passos](docs/NEXT_STEPS.md) e [auditoria](docs/API_COMPATIBILITY.md). O [prompt mestre integral](docs/PROJECT_SPECIFICATION.md) permanece preservado.

Apenas um Codex por vez. A cada entrega, atualize a checklist com itens comprovados riscados, estado, próximos passos e evidências. Faça commit/push autorizado e confirme sincronização. Execute `python3 scripts/check_continuity.py`; o checker não substitui revisão do código ou testes do mod.
