# Itens salvos e componentes — alpha.2 local

Salva uma cópia do item da mão principal, incluindo quantidade, nome, lore, encantamentos, dano e componentes registrados. Usa o codec oficial Minecraft 1.21.1; não remove o original. Payload guarda um protótipo com count=1 e quantidade separada. Referências/quantidades/componentes inválidos são recusados.

Permissão exclusiva `shirokaidex.admin.items`, via LuckPerms, para jogador online:

```text
/dex admin item save <id>
/dex admin item list [página]
/dex admin item give <id> <jogador_online>
/dex admin item delete <id> confirm
```

save cria/substitui o modelo; list pagina 18 IDs ordenados. give abre confirmação com prévia e destinatário; revalida permissão no clique e usa o journal/inventário já existentes. Cada confirmação administrativa é uma concessão distinta, marcada ADMIN_ITEM, sem fingir desbloqueio de Pokédex. A operação registra o administrador iniciador e o UUID destinatário. Resultado incerto deve ser revisado pelo UUID informado, sem repetir give como tentativa de retomada.

## Configuração de recompensas

```json
"items": [
  {"type": "SAVED_ITEM", "saved_item": "presente"},
  {"type": "SAVED_ITEM", "saved_item": "presente", "count": 2},
  {"type": "ITEM", "id": "minecraft:paper", "count": 64}
]
```

type é opcional e inferido; quando informado precisa corresponder ao conteúdo. SAVED_ITEM usa a quantidade original salvo override explícito. CUSTOM_ITEM usa id/count/payload_snbt com a representação SNBT **completa do ItemStack protótipo**, incluindo id e componentes, não apenas um patch. A opção mais simples é salvar um item preparado no jogo e referenciá-lo. Configuração inline também passa pelo codec/registro real na thread do servidor, antes de aplicar o candidato. Tipos ainda não implementados não são aceitos.

Na leitura da configuração, a referência vira um snapshot imutável de item/quantidade/componentes. Editar/excluir o modelo não muda a configuração ativa nem planos de resgate reservados. Reload bem-sucedido resolve referências vigentes novamente; novas regras usam o conteúdo atualizado. Não reutilizar IDs de recompensa para prêmios diferentes; uma operação antiga nunca troca seu plano silenciosamente.

O backup de recuperação substitui referências por CUSTOM_ITEM e payload resolvido. Assim, apagar um modelo não inutiliza o último backup válido. Um reload com referência ausente é rejeitado integralmente; no reinício, esse backup é validado novamente e pode recuperar a configuração, preservando o arquivo original. Componentes de mod removido continuam indisponíveis: backup não substitui dependências reais.

## Persistência e limites

Migração 5: saved_items e saved_item_audit. Salvar/excluir e auditoria são transacionais. Histórico preserva conteúdo anterior/novo, ator e horário; planos existentes conservam seu próprio payload. SHA-256 detecta divergência de conteúdo em modelos. Leitura corrompida é recusada; substituição/exclusão explícita pode reparar o cadastro, guardando os bytes anteriores na auditoria.

Limites: 256 modelos ativos; ID `[a-z][a-z0-9_]{0,47}`; 16 KiB UTF-8 por payload; 64 KiB de payloads por recompensa; plano até 256 KiB. Configuração original permanece limitada a 64 KiB; backup expandido até 512 KiB. Candidato cujo backup exceda o limite não é publicado.

Componentes customizados não se misturam a pilhas comuns incompatíveis. Quantidades acima da pilha real são distribuídas somente se houver espaço integral. Prévia conserva lore original e acrescenta quantidade sem alterar o payload entregue. Salvamento/readback do inventário e tratamento de resultado incerto seguem [regras de resgates](REWARDS.md).

## Revisão administrativa

`/dex admin reward list [página]` mostra até dez operações PENDING/PROCESSING/NEEDS_REVIEW por página, com a permissão separada `shirokaidex.admin.rewards`. Concluídas/canceladas ficam fora dessa fila; estado completo continua acessível por UUID. Cada página reflete o estado atual. Consultas e resgates também informam o UUID da operação quando disponível, sem criar operação apenas para informar seu ID.

[Testes e limites](TESTING.md), [evidência](validation/phase3-saved-items.json). Testados componentes Minecraft listados acima e dados personalizados; não certifica todos os componentes de mods, cliente visual, carga ou instalação do JAR fora do Loom.

## Compatibilidade de chaves físicas — alpha.9

Na branch `handoff/crates-checkpoint`, uma chave real emitida pelo Crates 0.2.1 foi salva com `/dex admin item save native_key` e usada como SAVED_ITEM. Componentes são preservados e o checkpoint nativo deve concluir antes do ACK. A autenticidade é conferida pela rotina `Tokens.matches` do provedor, sem ler ou recriar seu segredo. O cenário usa a chave `shiny1` e o mesmo servidor/segredo; não certifica migração entre servidores, outras versões ou todos os componentes de mods. **Isso não implementa o tipo CRATE_KEY dedicado.** [Passagem/testes](CRATES_CHECKPOINT_HANDOFF.md).
