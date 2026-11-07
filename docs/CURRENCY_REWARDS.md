# Recompensas monetárias — Impactor 5.3.5

Escopo local alpha.8: CURRENCY em marcos, combinado com itens/componentes/Pokémon ou isoladamente. Integração opcional; o Dex não instala, configura, cria moedas nem modifica outros mods no servidor de usuários.

```json
{
  "id": "national_bonus",
  "collection": "national",
  "minimum_registered": 10,
  "currencies": [
    { "type": "CURRENCY", "currency": "impactor:dollars", "amount": 125.50 },
    { "currency": "impactor:tokens", "amount": 3 }
  ]
}
```

O tipo é opcional, mas quando fornecido precisa ser CURRENCY. `currency` exige ID namespaced registrado no serviço ativo; `impactor:dollars` e `impactor:tokens` têm rótulos PokéDollars `$` e PokéTokens `✦`. Outros IDs exibem seu nome canônico. Nunca converte valores ou troca moeda ausente pela principal. O parser valida ID/quantia; a existência e precisão reais são verificadas a cada resgate, pois o provedor é opcional e seu registro pode mudar. Carregar o JSON não comprova que a moeda existe.

Quantia JSON numérica positiva, até 10^15, no máximo oito casas, sem expoente/string; `BigDecimal` normaliza valores equivalentes sem usar double na lógica do Dex. A precisão configurada na moeda também é obrigatória: tokens com zero casas não aceitam 0.5. Até oito entradas monetárias e 32 etapas totais, contando itens e Pokémon. A ordem persistida é itens → Pokémon → moedas.

## Confirmação e persistência

Prévia e segunda confirmação mostram valores exatos. Todas as moedas restantes são verificadas antes do primeiro efeito, junto do inventário/armazenamento e permissões existentes. Ausência de integração/moeda ou precisão incompatível impede a entrega inteira no preflight inicial. Carregamento/criação de conta ocorre somente depois da intenção persistida.

A API real é `EconomyService.instance().currencies().currency(Key)`, `account(Currency, UUID)`, `Account.deposit(BigDecimal)`, resultado DEPOSIT/SUCCESS e `EconomyService.save(Account)`. O adaptador verifica conta/dono/moeda/quantia, sessão/configuração/permissão atual e gravação antes de confirmar a etapa. Cada moeda recebe intenção/ack próprios no journal. Plano monetário preservado não muda retroativamente; resgate encerrado não é reenviado.

A auditoria encontrou dois riscos na implementação nativa 5.3.5:

- deposit retorna após iniciar um save assíncrono; sucesso da transação não prova gravação.
- ConfigurateProvider.save captura exceções de gravação e imprime o erro, permitindo que o future público aparente sucesso. Também serializa saldo por doubleValue.

Por isso este escopo aceita apenas serviço nativo e backend JSON auditados. Antes de creditar, verifica que o saldo previsto sobrevive à conversão utilizada pelo provedor. Após aguardar save, lê o arquivo nativo em executor separado e compara o saldo decimal exato; uma janela curta admite escrita concorrente transitória, mas nunca repete o depósito. Não escreve esse arquivo.

O serviço público não expõe o backend. Uma checagem reflexiva **somente leitura**, isolada e presa a 5.3.5, confere os campos reais `EconomyStorage.implementation`, `ConfigurateProvider.loader` e `root`, além das classes nativas/JsonLoader. Fontes oficiais e o JAR distribuído foram inspecionados. Se essa identificação falhar, rejeita o preflight antes de efeitos. Não usa um caminho presumido para certificar SQL/custom providers.

Contas virtuais, serviço customizado, JSON não nativo, YAML/HOCON/SQL e versões não auditadas ficam indisponíveis neste bloco. Falta de precisão e mudanças concorrentes podem exigir revisão conservadora. O future de persistência e o carregamento de conta têm limites; um carregamento tardio não dispara depósito após timeout.

## Falhas e revisão

Sem idempotência externa por UUID fornecido pelo Dex e sem transação distribuída entre SQLite/Impactor/inventário. Após uma intenção, depósito rejeitado, exceção, timeout ou saldo gravado divergente bloqueiam a operação em NEEDS_REVIEW. Pode haver crédito/itens já efetivados. Não faz compensação nem tenta de novo automaticamente. O código de operação continua disponível na prévia e administração existente; revisar evidências externas antes de concluir/cancelar. PROCESSING interrompido continua recuperado para revisão no startup.

O log de transações do provedor não é uma prova garantida: a implementação auditada desabilita a gravação correspondente. O journal do Dex registra moeda/quantia e confirmação de leitura, sem inventar ID de transação externo.

Testes e limitações verificadas estão em `validation/phase3-currency.json`. Cliente gráfico, carga, provedores adicionais e retomada supervisionada continuam fora deste escopo.
