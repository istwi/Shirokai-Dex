# Recompensas COMMAND — alpha.10 local

Escopo inicial: **um comando isolado por recompensa, com revisão obrigatória**. Não pode ser misturado com itens, Pokémon, moedas ou outros comandos. Execução ocorre uma vez após intenção persistida; seu retorno não comprova persistência externa. A operação fica em NEEDS_REVIEW, sem retry automático. Itens/Pokémon/moedas continuam usando seus adaptadores próprios.

## Configurar explicitamente

Sem `command_policy`, a execução fica desativada. Cada par contexto/template precisa de autorização exata; não há autorização genérica por prefixo. Exemplo para um objetivo scoreboard previamente criado pelo administrador:

```json
{
  "schema_version": 1,
  "language": "pt_br",
  "menu_title": "Shirokai Dex",
  "command_policy": {
    "enabled": true,
    "allowed": [{
      "type": "COMMAND", "context": "CONSOLE",
      "command": "scoreboard players add {player} reward_points 1",
      "acknowledgement": "MANUAL_REVIEW"
    }]
  },
  "rewards": [{
    "id": "command_points", "collection": "national", "minimum_registered": 1,
    "commands": [{
      "type": "COMMAND", "context": "CONSOLE",
      "command": "scoreboard players add {player} reward_points 1",
      "acknowledgement": "MANUAL_REVIEW"
    }]
  }]
}
```

PLAYER usa a fonte do premiado, preservando suas permissões nativas. CONSOLE usa a fonte do servidor; é uma escolha privilegiada explícita do operador no arquivo, não uma promoção do jogador nem fallback automático. Configuração é confiada ao administrador; handlers/aliases de terceiros não são certificados por esta política.

Templates de até 512 caracteres devem conter `{player}` ou `{uuid}`. Apenas esses dois parâmetros são substituídos, pelo nome Minecraft validado e UUID do próprio destinatário. Não há argumento livre vindo do chat, shell ou expansão de Placeholder API. Barras iniciais, controles/newlines e parâmetros desconhecidos são rejeitados. Raízes `execute`, `function`, `schedule`, `return`, `op`, `deop`, `lp`, `luckperms`, `stop`, `reload`, `dex`, `shirokaidex`, `crates` e `shirokaicrates` são recusadas, inclusive com namespace. Isso não é uma inspeção universal de aliases ou do código de outros mods; autorize somente templates cujo efeito foi auditado.

Máximo de 32 autorizações distintas e uma ação COMMAND por recompensa. Não configurar segredo em template. `snapshot` é informação interna do plano, rejeitada no JSON público. Política desativada pode manter definições visíveis; confirmar não executa nem reserva efeito. Política habilitada com recompensa não autorizada rejeita o reload inteiro, preservando last-good.

## Entrega e revisão

Prévia e segunda confirmação exibem template, contexto e aviso de revisão. Após confirmar, o comando com destinatário resolvido é congelado no plano SQLite antes de registrar intenção. Pré-condições, política, configuração, jogador e permissões de parsing são revalidados na thread do servidor imediatamente antes do dispatcher.

Comando ausente ou permissão PLAYER insuficiente recusa antes de reservar operação. Após tentativa de dispatch, mesmo callback/retorno aparentemente bem-sucedido ou erro de execução não confirma entrega persistida: a etapa permanece INTENT e a operação passa a NEEDS_REVIEW, com motivo específico de comando. Nada é reexecutado automaticamente, inclusive após reinício. Interrupção entre intenção/execução/revisão também exige inspeção externa.

Use `/dex admin reward status <operação>` e a resolução administrativa existente, com permissão própria, confirmação literal e evidência real. Completar/cancelar não executa o comando. Não marcar COMPLETED sem conferir o sistema afetado; não usar COMMAND para contornar o [bloqueio de CRATE_KEY dedicado](CRATE_KEY_CONTRACT.md).

## Validação e limites

[Relatório](validation/phase3-commands.json): testes Java e servidor empacotado fora do Loom, com scoreboard nativo e comando PLAYER. Cobertura de intenção/plano congelado, permissões, erro runtime, revogação por política/reload, anti-repetição, reinício e resolução administrativa. Não certifica comandos de outros mods, durabilidade de todos os efeitos, crash/perda de energia, carga ou cliente visual. Recibos automáticos e planos mistos continuam pendentes. Nenhuma nova release/configuração de ambiente publicada; entrega alpha.10 na branch de desenvolvimento sobre `f714c4b`, assunto `feat: add explicitly authorized command reward dispatch`.
