# Próximos passos — passagem para o próximo Codex

## Última tarefa realizada

Atualização mais recente: criada a checklist fixa `MASTER_CHECKLIST.md`, exigida por AGENTS e pelo checker em alterações técnicas. O download do Gradle via proxy funcionou, mas o Wrapper rejeitou um checksum salvo com 66 caracteres. Corrigido para os 64 caracteres consultados no endpoint oficial, sem desabilitar verificação. A próxima tarefa técnica é repetir o comando com cache gravável e propriedades JVM de proxy; não houve execução bem-sucedida do Gradle ainda.

Preparado o protocolo de continuidade solicitado antes da implementação de funcionalidades. Prompt mestre preservado, auditoria estática inicial documentada e esqueleto Gradle criado. Nenhum inicializador Fabric ou sistema de negócio foi implementado. Não recomeçar o repositório.

Base técnica relevante: `9eaae42946e5e11a3e0d5e0b9c82fd495a8e1afc` (`chore: preserve project specification and initial API audit`). Entrega de continuidade: `72304ea4a58ff166d19cfc92413e4e36e20e9285` (`docs: establish mandatory Codex handoff and continuity checks`), enviada para `origin/main` e confirmada por leitura remota. A atualização documental posterior registra essa verificação. Verifique também os commits posteriores no remoto; não dependa apenas do documento.

## Próxima tarefa exata

1. Execute o protocolo de `../AGENTS.md`: inspecione mudanças/commits locais, fetch e sincronização sem descarte; leia estado e próximos passos e confira o código.
2. Execute `python3 scripts/check_continuity.py` para verificar a integridade documental existente.
3. Configure **localmente** o Gradle para usar cache gravável e proxy HTTP/HTTPS herdado. O Wrapper usa Gradle 8.14.3 e Java 21. O primeiro comando a fazer passar é `./gradlew --no-daemon help`. Java não usa automaticamente `HTTPS_PROXY`; derive as propriedades JVM de proxy das variáveis existentes e valide a truststore sem imprimir credenciais. Não fixe host de proxy específico da sessão em arquivo versionado. Preserve o checksum do Wrapper e da distribuição.
4. Investigue falhas de resolução reais, com versões fixadas em `build.gradle`. O Maven oficial Cobblemon verificado é `https://artefacts.cobblemon.com/releases/`; não é necessário depender de `maven.cobblemon.com` ou Modrinth para localizar o POM auditado. Remova repositórios sem uso quando a estratégia for definida; `maven.nucleoid.xyz` foi incluído como candidato para GUI/placeholder e ainda não foi necessário nem testado.
5. Conclua a auditoria necessária à fundação e implemente **somente a Fase 1**, conforme o prompt: inicialização Fabric server-side, configuração validada/reload seguro, SQLite/migrações, logs, `/dex`, menu inicial e integração básica somente de leitura com Cobblemon.
6. Adicione testes significativos para configuração inválida/preservação da última válida e migrações/reabertura do banco. Compile com versões corretas e inicialize servidor Fabric com Cobblemon 1.8.3 antes de declarar Fase 1 concluída. Documente testes manuais de GUI pendentes.
7. Atualize estado, próximos passos, compatibilidade e changelog; faça commits coerentes, push autorizado e confira o SHA remoto. Configure o pipeline Java e artifact ao existir build de fato. Não publique release estável nesta etapa.

## Arquivos importantes

- `MASTER_CHECKLIST.md`: checklist permanente do prompt mestre, com itens concluídos riscados e parciais abertos. Atualização obrigatória em toda entrega.

- `../AGENTS.md`: protocolo obrigatório para qualquer Codex.
- `PROJECT_STATUS.md`: estado e evidências atuais.
- `PROJECT_SPECIFICATION.md`: prompt mestre integral; contém todas as fases e decisões funcionais.
- `API_COMPATIBILITY.md`: versões, fontes, assinaturas e limitações reais.
- `../build.gradle`, `../settings.gradle`, `../gradle.properties`, `../gradle/wrapper/gradle-wrapper.properties`: esqueleto de build não validado.
- `../THIRD_PARTY_NOTICES.md`: origem e checksum do Wrapper.
- `../scripts/check_continuity.py` e `../.github/workflows/continuity.yml`: verificação documental; não substituem build/testes Java.

## Dependências e problemas ainda não resolvidos

Java 21; Minecraft 1.21.1; Fabric Loader 0.17.2; Fabric API 0.116.6+1.21.1; Cobblemon `com.cobblemon:fabric:1.8.3+1.21.1`; Fabric Language Kotlin 1.13.6+kotlin.2.2.20; Gradle 8.14.3; Loom 1.11.8. SQLite JDBC 3.47.1.0 e JUnit 5.11.4 constam do esqueleto, ainda sem resolução validada.

Impactor plugin 5.3.5 auditado com submódulo API fixado; não invente coordenadas de API baseadas apenas na versão do plugin. LuckPerms, Placeholder API e SGUI têm fontes candidatas documentadas, sem compatibilidade funcional verificada.

Shirokai Crates informado pelo usuário: `https://github.com/istwi/Shirokai-Crates`. Commit auditado `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, versão declarada 0.2.1. Versão instalada ainda não confirmada; isso não bloqueia a fundação. Não alterar o Crates como parte do Dex sem escopo/autorização apropriados.

## Testes pendentes e cuidados

- Nenhum teste do mod executou; não herdar alegação de “funcional” a partir da auditoria.
- Não somar shiny visto com normal obtido para inventar shiny capturado.
- Não usar eventos genéricos de armazenamento como capturas competitivas.
- Capturas e recompensas precisam de idempotência persistente e recuperação supervisionada; não prometer atomicidade entre SQLite e APIs externas.
- Preservar inscrições obrigatórias, cancelamento definitivo na edição, calendário automático, faixas exclusivas, revisão administrativa e estatísticas permanentes independentes.
- Não depender de arquivos em `/tmp`, processos ativos ou instruções desta conversa na próxima sessão.
- Não modificar/publicar configuração de ambiente; a publicação anterior foi feita pelo usuário.
- Se não houver acesso de escrita ao GitHub, manter commits locais e informar exatamente o que falta enviar; não usar force push.
