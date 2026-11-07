# Changelog

## 0.1.0-alpha.1 — fundação experimental

- Implementados lifecycle Fabric, configuração validada com backup/recuperação e SQLite com migração transacional em executor dedicado.
- Adicionados `/dex`, `/dex status`, menu inicial somente de leitura e consulta da Pokédex oficial/registro dinâmico Cobblemon.
- Reload administrativo protegido por API LuckPerms opcional; ausência nega ação sem impedir startup.
- Adicionados mensagens pt_br/en_us, helper de proxy/cache, workflow Java 21 e documentação da fundação.
- Resolvidos requisitos do dev runtime: JDK completo, metadata Kotlin remapeada e ICU no servidor dedicado.
- Validação: 20 casos JUnit sem falhas e 10 checks de protocolo em servidor real, repetidos após reinício/recuperação e com JAR instalado fora do Loom. Evidência em `docs/validation/phase1-foundation.json`.
- Ainda sem coleções completas, recompensas, capturas/competições ou release estável. Testes positivos de LuckPerms, cliente visual e datapacks adicionais continuam pendentes.

## Não lançado — preparação inicial

- Adicionada checklist permanente das fases do prompt mestre, vinculada a AGENTS, README e verificação de continuidade.
- Corrigido checksum da distribuição Gradle 8.14.3 após comparação com o endpoint oficial; download por proxy funcionou, mas execução e build continuam pendentes.

- Preservado o prompt mestre em `docs/PROJECT_SPECIFICATION.md`.
- Documentada auditoria estática das APIs reais e limitações, incluindo progresso shiny e entrega de chaves do Shirokai Crates.
- Adicionado esqueleto Gradle 8.14.3/Loom 1.11.8, com alvo Java 21, Minecraft 1.21.1 e Cobblemon 1.8.3. Ainda sem build validado ou mod inicializável.
- Preparado protocolo de continuidade entre instâncias Codex, com estado e próxima tarefa documentados.
- Adicionados checker documental e workflow de continuidade; validado localmente inclusive com falhas simuladas de documentos/links. Pipeline Java permanece pendente.

Sem release, sem JAR e sem funcionalidades entregues até esta etapa.
