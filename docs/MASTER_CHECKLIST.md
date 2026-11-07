# Prompt mestre — checklist de execução

Lista operacional do [prompt mestre integral](PROJECT_SPECIFICATION.md). A especificação original permanece preservada; esta checklist acompanha sua execução.

## Como atualizar (obrigatório para todo Codex)

- `[ ]` = pendente ou parcial; não riscar trabalho incompleto.
- `[x] ~~Item~~` = concluído, com evidência indicada. Inspeção de fonte, compilação e teste em servidor são resultados diferentes.
- A cada criação, alteração, correção ou remoção, atualizar o item correspondente e registrar arquivos, testes, limitações e commit na tabela de entregas. Se for trabalho parcial, manter `[ ]` e explicar o avanço.
- Se uma mudança invalidar uma conclusão anterior, reabrir o item e justificar. Nunca apagar requisitos para aparentar conclusão.
- Conferir os critérios da seção 34 do prompt antes de concluir funcionalidades. Esta lista resume as fases; os detalhes das seções 1–38 continuam obrigatórios.

## Ponto de retomada

**Fase 0: auditoria estática inicial feita; validação funcional pendente. Fase 1: esqueleto Gradle parcial.** Próxima ação: repetir o comando Gradle via proxy com o checksum já corrigido na fonte oficial; depois implementar a fundação. Não há mod inicializável ou JAR instalável.

## Preparação e continuidade

- [x] ~~Preservar o prompt mestre integral no GitHub.~~ Evidência: [especificação](PROJECT_SPECIFICATION.md), commit `9eaae42`.
- [x] ~~Registrar auditoria estática inicial com fontes e limitações.~~ Evidência: [compatibilidade](API_COMPATIBILITY.md), commit `9eaae42`; não equivale à validação funcional da Fase 0.
- [x] ~~Configurar passagem obrigatória entre Codex.~~ Evidência: [AGENTS.md](../AGENTS.md), [estado](PROJECT_STATUS.md), [próximos passos](NEXT_STEPS.md), commit `72304ea`.
- [x] ~~Confirmar commit e envio inicial ao GitHub.~~ Evidência: commit remoto `ec8f1e9`, confirmado por leitura Git.
- [ ] Concluir configuração e resolução do build Gradle. Parcial: download via proxy funcionou; checksum incorreto foi corrigido consultando o valor oficial. Repetição do Wrapper e resolução completa pendentes.

## FASE 0 — Auditoria técnica

- [ ] Investigar as APIs reais.
- [ ] Verificar dependências.
- [ ] Confirmar eventos de captura.
- [ ] Confirmar acesso à Pokédex.
- [ ] Confirmar registro dinâmico de espécies.
- [ ] Confirmar criação de Pokémon.
- [ ] Confirmar integração econômica.
- [ ] Criar `API_COMPATIBILITY.md`.
- [ ] Identificar bloqueios técnicos.

## FASE 1 — Fundação

- [ ] Estrutura Gradle.
- [ ] Inicialização Fabric.
- [ ] Sistema de configuração.
- [ ] SQLite.
- [ ] Migrações.
- [ ] Logs.
- [ ] Comando `/dex`.
- [ ] Menu inicial.
- [ ] Integração básica com Cobblemon.

## FASE 2 — Pokédex

- [ ] Pokédex Nacional.
- [ ] Shiny.
- [ ] Lendária.
- [ ] Categorias.
- [ ] Progresso oficial.
- [ ] Datapacks.
- [ ] Wiki.
- [ ] Menus.
- [ ] Placeholders básicos.

## FASE 3 — Recompensas

- [ ] Marcos de progresso.
- [ ] Recompensas.
- [ ] Itens personalizados.
- [ ] Pokémon.
- [ ] Impactor.
- [ ] Itens salvos.
- [ ] Resgate manual.
- [ ] Persistência segura.
- [ ] Recuperação de operações.

## FASE 4 — Capturas e estatísticas

- [ ] Eventos de captura validados.
- [ ] Deduplicação.
- [ ] Estatísticas permanentes.
- [ ] Categorias.
- [ ] Histórico.
- [ ] Retenção.
- [ ] Recordes.
- [ ] Rankings permanentes.

## FASE 5 — Competições

- [ ] Calendários.
- [ ] Inscrições.
- [ ] Cancelamentos.
- [ ] Pontuação.
- [ ] Proteções.
- [ ] Revisões.
- [ ] Finalização.
- [ ] Premiações.
- [ ] Histórico.

## FASE 6 — Conquistas e perfis

- [ ] Medalhas.
- [ ] Requisitos.
- [ ] Conquistas secretas.
- [ ] Favoritas.
- [ ] Perfis.
- [ ] Privacidade.
- [ ] Estatísticas públicas.

## FASE 7 — Personalização e integrações

- [ ] Menus JSON avançados.
- [ ] Condições dos botões.
- [ ] Integração SkiesGUIs.
- [ ] LuckPerms.
- [ ] Placeholders completos.
- [ ] Idiomas.
- [ ] Notificações.
- [ ] Ferramentas administrativas.

## FASE 8 — Validação final

- [ ] Testes automatizados.
- [ ] Testes de integração.
- [ ] Testes de reinicialização.
- [ ] Testes de configurações inválidas.
- [ ] Testes de recompensas.
- [ ] Testes de desempenho.
- [ ] Verificação das dependências.
- [ ] Build final.
- [ ] Documentação.
- [ ] Release.

## Entregas e evidências

| Entrega | Resultado e evidência | Referência |
|---|---|---|
| Auditoria inicial e build base | Fontes reais auditadas; sem testes funcionais; sem mod implementado | `9eaae42` |
| Continuidade | Documentos e checker validados, incluindo cenários negativos; envio confirmado | `72304ea`, `ec8f1e9` |
| Checklist permanente | Adicionada esta lista e exigida atualização por AGENTS e checker; documentação do ponto de retomada atualizada | Commit com assunto `docs: track master prompt progress and correct official Gradle checksum` |
| Gradle via proxy | Download chegou a 100%; Wrapper rejeitou checksum salvo. Não houve execução do Gradle | Tentativa posterior a `ec8f1e9`; checksum corrigido na entrega seguinte, nova execução pendente |

Ao registrar a própria entrega, use o assunto do commit em preparação e substitua pela referência exata na atualização seguinte; não crie commits infinitos apenas para gravar o hash do próprio documento.
