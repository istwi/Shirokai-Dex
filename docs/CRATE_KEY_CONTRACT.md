# CRATE_KEY — viabilidade no Crates 0.2.1

Auditoria estática posterior à validação de checkpoint `f714c4b`. Fonte oficial `05b3995c4d4466ef17c6171ad07d4f0eab512db9`; release SHA-256 `49a82904ef3fa971c535191657a592b9ee5affad5577913f1bb537ca1274adfd`. Métodos públicos conferidos com `javap -public` no JAR original, além da fonte. Não houve mudança no Crates nem leitura do seu segredo.

## Resultado

**UNSUPPORTED no contrato automático auditado desta versão.** Isso se refere à emissão dedicada de chaves por um novo tipo de recompensa; chaves reais previamente salvas via SAVED_ITEM continuam no escopo validado. Não é conclusão de impossibilidade para versões/contratos futuros.

| Caminho existente | Evidência | Consequência para o Dex |
|---|---|---|
| `CrateService.give(admin,target,id,count,fragment)` | Retorna `void`; exige jogador com `shirokaicrates.give`, saúde e espaço; faz inserção e checkpoint assíncrono privado | Não oferece plano preparado/recibo de persistência. Não elevar permissão do destinatário, fabricar administrador nem inferir sucesso de retorno/comando |
| `Tokens.create` / `Tokens.matches` | Métodos públicos, mas a instância `tokens` é campo privado de CrateService, sem getter público. Construtor recebe caminho e lê/cria segredo | Não acessar campo privado em produção nem construir outra instância lendo segredo. A reflexão usada no probe continua restrita aos testes |
| `checkpoints().save(player,true)` | Future público comprovado nos 26 checks da alpha.9 | Resolve a gravação coordenada, mas não fornece uma API pública de emissão/autenticação de chave |
| Chave emitida pelo próprio Crates + SAVED_ITEM | Componentes congelados, autenticidade conferida nativamente, checkpoint/readback/reinícios testados | Alternativa disponível para o mesmo servidor/segredo. Não chamar essa alternativa de tipo CRATE_KEY dedicado |

## Contrato necessário para reconsiderar

Uma integração futura precisa de acesso público e versionado à preparação de uma chave autêntica sem expor segredo, com validação explícita da crate/tipo/quantidade e sem conceder permissão administrativa ao premiado. O Dex deve poder congelar o payload antes da intenção e entregá-lo pelo adaptador coordenado já auditado. Outra opção seria uma entrega nativa com identificador persistente da operação e recibo que confirme o checkpoint; ela exigiria auditoria/testes próprios de repetição, reinício e resultado incerto.

Esses requisitos descrevem um contrato desejado, **não métodos existentes**. Nenhuma alteração no outro mod está autorizada nesta tarefa. Manter CRATE_KEY desabilitado; não implementar um alias que silenciosamente trate uma chave declarada como item vanilla nem fabricar assinatura.

## Continuidade

Checkpoint/SAVED_ITEM validado e enviado somente em `handoff/crates-checkpoint`, commit `f714c4b`; main preservada em `c89cc1b`. Esta auditoria posterior é local. Antes de implementar COMMAND, definir no Dex política explícita de comandos autorizados, contexto de execução, confirmação/journal e comprovação de resultado: comando que agenda efeito assíncrono não produz confirmação durável apenas pelo retorno do dispatcher. Não usar COMMAND para contornar o bloqueio de CRATE_KEY.
