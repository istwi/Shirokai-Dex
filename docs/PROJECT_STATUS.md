# Estado do projeto

Atualizado em 2026-11-07. GitHub: `istwi/Shirokai-Dex`. Apenas uma instância Codex por vez.

## Fase atual

Sistema de continuidade configurado e enviado ao GitHub; Fase 0 com auditoria estática inicial registrada. Fase 1 possui apenas esqueleto de build, não funcionalidade implementada. A prioridade solicitada pelo usuário foi configurar a continuidade **antes** das funcionalidades.

## Concluído

- Prompt mestre preservado integralmente em `PROJECT_SPECIFICATION.md`.
- Protocolo obrigatório de entrada, sincronização segura, documentação, commits, push e saída em `../AGENTS.md`.
- Estado e próxima tarefa registrados no Git; não dependem desta conversa.
- Revisões exatas de Cobblemon 1.8.3, Impactor 5.3.5 e submódulo API consultadas. APIs candidatas e limitações registradas em `API_COMPATIBILITY.md`.
- Investigado o Shirokai Crates 0.2.1, commit `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, indicado pelo usuário. Entrega por console não é suportada pelo comando consultado.
- Esqueleto Gradle com versões principais fixadas; bootstrap oficial Gradle 8.14.3 teve checksum conferido. Isso não comprova build.

## Em desenvolvimento

- Auditoria de comportamento e compatibilidade binária das integrações.
- Resolução do Gradle no ambiente cloud e preparação da Fase 1.

## Pendente

- Resolver Gradle/plugins/dependências por proxy com TLS e truststore corretos.
- Inicializador Fabric, configurações e reload validado, SQLite/migrações, logs, `/dex`, menu inicial e adaptador básico Cobblemon.
- Testes automatizados, inicialização em servidor Fabric real, pipeline Java e artefato instalável.
- Todas as funcionalidades das Fases 2–8. Não há recompensas, capturas persistentes, estatísticas, rankings ou competições implementados.
- Definir licença do código próprio antes de distribuição geral.

## Testes e verificações executados

| Verificação | Resultado |
|---|---|
| Inspeção do checkout e instruções pré-existentes | Checkout inicialmente vazio; não havia HEAD, fontes, AGENTS ou testes |
| `git ls-remote origin` | Leitura autorizada; remoto sem refs antes dos primeiros commits |
| `git fetch origin` com remoto vazio | Falhou: `couldn't find remote ref HEAD`; não é evidência de credencial ausente |
| Java local | OpenJDK 21.0.13 |
| Tags e fontes das integrações | Obtidas/inspecionadas; sem testes funcionais |
| POM Cobblemon oficial | HTTP 200; commit do POM igual à tag auditada |
| Wrapper Gradle oficial | SHA-256 confere com checksum oficial; bootstrap anterior copiado da árvore Cobblemon foi substituído pela versão oficial 8.14.3 |
| Prompt mestre salvo | Comparação byte a byte com TXT original aprovada; SHA-256 `321319bd16a08a72746457460892732b4936f3edf9ca905b1f0671e74eb517b3` |
| `python3 scripts/check_continuity.py` | Aprovado: documentos, seções e links locais |
| Cenários negativos do checker em diretório temporário | Detectou documento ausente, seções ausentes e link quebrado; 3 cenários aprovados |
| `bash -n gradlew` | Sintaxe aprovada; não é execução do Gradle |
| Whitespace Git | CRLF do batch normalizado com `.gitattributes`; validação staged aprovada |
| `./gradlew --no-daemon help` | Falhou: diretório `/home/agent/.gradle` não gravável |
| Mesmo comando com `GRADLE_USER_HOME=/workspace/.cache/shirokai-gradle` | Falhou: `UnknownHostException: services.gradle.org`; Java não utiliza automaticamente o proxy herdado |
| Build, testes unitários/integração e servidor Fabric | Não executados com sucesso; não há testes do mod ou código de inicialização ainda |

O checker de continuidade e a verificação de links/arquivos são checks de documentação; não validam o mod. O resultado do workflow GitHub só pode ser declarado depois de observado.

Pipeline `.github/workflows/continuity.yml` preparado com permissão somente de leitura: verifica documentação e exige atualizar estado/próximos passos quando o diff contém alterações técnicas. Não faz push, publicação, build Java ou release automaticamente. A revisão de exatidão dos documentos continua obrigatória para o Codex.

## Erros e limitações conhecidos

- Configuração local do Gradle/proxy pendente, sem tentativa de desabilitar TLS ou checksums.
- A Pokédex oficial não comprova captura shiny pelo cruzamento de shiny visto com espécie obtida; não implementar essa inferência.
- Eventos de captura não fornecem ID próprio universal nem autenticação da origem contra outros mods. Deduplicação por Pokémon + Poké Ball é proposta a testar.
- Crates: API pública idempotente para entrega automática ainda não verificada; não usar `givekey` como console e não conceder permissões administrativas ao jogador destinatário.
- Versões efetivamente instaladas de Crates, LuckPerms, SkiesGUIs e TAB não confirmadas; fontes candidatas não equivalem ao conjunto instalado.
- Fontes temporárias em `/tmp/shirokai-*-audit` são auxiliares descartáveis; podem não existir em outra sessão. URLs e commits estão persistidos no documento de compatibilidade.

## Última alteração relevante e sincronização

Base técnica: commit `9eaae42946e5e11a3e0d5e0b9c82fd495a8e1afc` — `chore: preserve project specification and initial API audit`.

Entrega de continuidade: `72304ea4a58ff166d19cfc92413e4e36e20e9285` — `docs: establish mandatory Codex handoff and continuity checks`.

Sincronização efetivamente verificada em 2026-11-07: `git push -u origin main` criou `main`; `git ls-remote origin refs/heads/main` retornou exatamente `72304ea4a58ff166d19cfc92413e4e36e20e9285`, igual ao HEAD local, e a árvore estava limpa. Nenhum force push foi usado. Esta atualização documental registra essa confirmação; confirme também seu commit no remoto ao iniciar a próxima sessão. Publicação de ambiente e release não foram executadas.
