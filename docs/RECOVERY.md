# Recuperação supervisionada — alpha.11

`/dex admin reward resume <operação> <justificativa>` abre uma prévia com os componentes do plano persistido, destinatário e justificativa. Exige jogador com `shirokaidex.admin.rewards`, destinatário online e uma justificativa de 8 a 2048 caracteres; confirmar na GUI é uma segunda ação. O prêmio é entregue ao destinatário, nunca ao administrador que está revisando.

## Critério seguro de retomada

Somente operações PENDING ou NEEDS_REVIEW com **zero registros em reward_steps** podem ser retomadas. Um PROCESSING interrompido antes da primeira intenção passa a NEEDS_REVIEW no reinício e pode ser retomado explicitamente. Configuração atual deve corresponder exatamente ao plano congelado, sem trocar componentes, quantidades, Pokémon preparados, moedas, contexto ou comando. Requisitos/permissões atuais do destinatário continuam obrigatórios; operações ADMIN_ITEM manuais não são retomadas por este fluxo.

Qualquer INTENT bloqueia: o efeito pode ter acontecido antes da falha. Um prefixo COMPLETED também bloqueia: itens intermediários podiam estar só em memória antes do salvamento final. Não inferir durabilidade nem repetir etapas a partir dessas marcas. Operações fechadas, planos alterados ou destinatário offline são recusados. Casos incertos continuam nos comandos existentes de inspeção/conclusão/cancelamento com evidência externa.

A autorização é revalidada na confirmação e em cada etapa, além da conferência transacional do SQLite imediatamente antes da retomada. O bloqueio ativo por destinatário impede duas entregas concorrentes. Não se cria outra operação, não se apagam etapas nem se substitui o plano: ID e payload originais são preservados. A auditoria registra RESUME_AUTHORIZED, UUID do operador e justificativa; uma nova falha continua exigindo revisão.

## Automatização dos testes

`scripts/integration_server.py` reaproveita server.jar original, classpath e dependências de um servidor descartável já preparado, instala o JAR de `build/server-mod`, cria um mundo novo e executa testes com parada/reinício normais. Não usa Loom nem classes de desenvolvimento no servidor. Não sobrescreve mundo existente. Só usa `/tmp`, loopback, perfil reservado, segredo RCON temporário e o aceite de EULA já concedido para estes testes.

```sh
JAVA_HOME=/caminho/jdk21 python3 scripts/integration_server.py \
  --base /tmp/servidor-preparado --suite recovery
```

Para repetir a regressão COMMAND, use `--suite commands`. Base deve conter `server.jar` original MC1.21.1, `classpath.txt` do perfil Fabric0.17.2 e JARs reais das dependências auditadas. O executor recusa o checksum incorreto do servidor e classpath de classes/Loom; segredos nunca são argumentos Java. Logs e `suite-result.json` permanecem no mundo descartável; RCON é desativado e o segredo removido em finally. Não publica ambiente, release ou main.

A suíte recovery inicializa o schema pelo próprio mod, encerra e só então cria estados sintéticos com `tests/integration/seed-recovery.py`. Não fabrica entrega: operações sem etapas, INTENT incerto, prefixo reconhecido e plano alterado são fixtures declaradas. Depois testa inventário/permissões/plano reais e reinicia para conferir persistência.

## Evidências e limites

[Relatório](validation/phase3-recovery.json): 290 JUnit (oito novos), nove checks de recuperação e nove de regressão COMMAND, em servidor empacotado. Componentes, destinatário, concorrência unitária, revogação, reload, desconexão, falhas simuladas e reinício cobertos. Retomada de operações parcialmente executadas, recibos externos, crash durante efeito, carga/cliente gráfico e todos os provedores de mods não estão certificados. Não há transação distribuída entre SQLite e inventário/APIs externas.
