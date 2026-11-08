# Shirokai Dex

Fundação experimental de mod server-side para Minecraft 1.21.1, Fabric, Java 21 e Cobblemon 1.8.3.

Na versão de desenvolvimento da branch `handoff/crates-checkpoint`, 0.4.0-alpha.1, Nacional e Lendária mostram **quantidade/total e porcentagem** em menu e status. Shiny é N/D enquanto não houver prova confiável de captura. [Regras e comandos](docs/POKEDEX.md).

Disponível: `/dex`, resumo somente de leitura da Pokédex oficial, menu inicial protegido, configuração validada com recuperação e SQLite com migração versionada. Coleções com filtros/modos/permissões por JSON e seletor paginado estão disponíveis: veja [configuração das coleções](docs/COLLECTIONS.md). A [wiki de espécies](docs/WIKI.md) permite pesquisa, filtros por estado/tipo/label, ordenação e páginas de espécies e de todas as formas. [Placeholders básicos](docs/PLACEHOLDERS.md) expõem progresso, vistas, total e restantes pela Text Placeholder API opcional. [Recompensas](docs/REWARDS.md) têm marcos, desbloqueio permanente, prévia/segunda confirmação e journal de resgates. [Itens salvos/componentes](docs/SAVED_ITEMS.md) incluem save/list/give/delete, snapshots imutáveis e backup independente. Captura/estatísticas têm escopo inicial próprio; cobertura completa, rankings e competições continuam pendentes.

Validação 0.4.0-alpha.1: **304 JUnit e 18 checks de servidor/reinício**, [relatório](docs/validation/phase4-captures.json). [Capturas/estatísticas](docs/CAPTURES.md) distinguem ações legítimas comprovadas, origem desconhecida e espécies distintas; dedup e snapshots são persistentes. Captura natural no escopo auditado foi validada pelo motor real. Proveniência após reload e outros fluxos ainda são parciais. [Recuperação segura](docs/RECOVERY.md) e COMMAND seguem disponíveis no escopo documentado.

`/dex` abre a [central Shirokai](docs/LAYOUT.md), com resumo real, paletas, gradiente e atalhos. Recompensas incluem [Pokémon no escopo NBT auditado](docs/POKEMON_REWARDS.md). Checkpoint Crates0.2.1/SAVED_ITEM foi validado na alpha.9. [COMMAND isolado](docs/COMMAND_REWARDS.md) exige política explícita e revisão após dispatch na alpha.10 local. CRATE_KEY dedicado, cobertura completa de captura e competições seguem pendentes.

**Commits/push autorizados continuamente na branch de desenvolvimento; main preservada.** Nenhuma nova release foi publicada nesta validação; os links abaixo são versões anteriores.

[Baixar a pré-release 0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) — JAR, fontes, checksums e relatório de validação.

[Baixar a pré-release anterior 0.1.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.1.0-alpha.1) — JAR, fontes, checksums e relatório de validação.

## Instalar e desenvolver

Com JDK 21 completo: `./gradlew --no-daemon build`.

Em cloud com proxy herdado: `python3 scripts/gradle_cloud.py --no-daemon build`, usando a truststore do ambiente quando necessária. Não desabilite TLS/checksums. Distribuição: execute também `./gradlew serverDistribution`. Artefato: `build/server-mod/shirokai-dex-0.4.0-alpha.1+mc1.21.1-cobblemon1.8.3.jar`; não instalar `-sources.jar`.

Leia [instalação](docs/INSTALLATION.md), [configuração](docs/CONFIGURATION.md), [comandos](docs/COMMANDS.md), [permissões](docs/PERMISSIONS.md), [banco](docs/DATABASE.md) e [diagnóstico](docs/TROUBLESHOOTING.md). O workflow [Java 21](.github/workflows/build.yml) gera artifact identificado pelo commit quando passa; confira o resultado no GitHub.

## Continuidade obrigatória

Antes de trabalhar, leia [AGENTS.md](AGENTS.md), [checklist](docs/MASTER_CHECKLIST.md), [estado](docs/PROJECT_STATUS.md), [próximos passos](docs/NEXT_STEPS.md) e [auditoria](docs/API_COMPATIBILITY.md). O [prompt mestre integral](docs/PROJECT_SPECIFICATION.md) permanece preservado.

Apenas um Codex por vez. A cada entrega, atualize a checklist com itens comprovados riscados, estado, próximos passos e evidências. Faça commit/push autorizado e confirme sincronização. Execute `python3 scripts/check_continuity.py`; o checker não substitui revisão do código ou testes do mod.

Recompensas monetárias locais: [Impactor/CURRENCY](docs/CURRENCY_REWARDS.md), no escopo JSON nativo auditado.
