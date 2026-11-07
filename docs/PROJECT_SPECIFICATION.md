# PROMPT MESTRE — SHIROKAI DEX

## 1. Objetivo do projeto

Desenvolva um mod completo chamado **Shirokai Dex**, destinado ao servidor Minecraft Shirokai.

O mod será um sistema avançado de Pokédex, recompensas, conquistas, estatísticas de capturas, rankings permanentes e competições de treinadores, com interfaces personalizáveis por JSON.

O projeto deve ser desenvolvido em fases, com arquitetura modular, testes automatizados, documentação e geração de arquivos `.jar` instaláveis.

**Não implemente tudo de uma vez.** Primeiro investigue as dependências e APIs reais. Depois implemente cada fase, validando seu funcionamento antes de avançar.

O objetivo é produzir um mod funcional, seguro, configurável, otimizado e adequado a um servidor multiplayer.

---

# 2. Ambiente obrigatório

| Componente | Versão |
|---|---|
| Minecraft | 1.21.1 |
| Mod Loader | Fabric |
| Java | 21 |
| Cobblemon | 1.8.3 |
| Impactor API | 5.3.5 |
| Banco de dados | SQLite |
| Build | Gradle |
| Repositório | GitHub |
| Integração de permissões | LuckPerms |
| Integração de menus externos | SkiesGUIs |
| Placeholders | Fabric Text Placeholder API |
| Scoreboard | TAB, por meio dos placeholders |
| Crates | Shirokai Crates, mediante validação da integração |

O mod deverá funcionar prioritariamente no servidor dedicado Fabric, sem exigir instalação no cliente para suas funcionalidades essenciais.

Dependências opcionais não podem impedir o servidor de iniciar quando estiverem ausentes, exceto quando alguma funcionalidade explicitamente obrigatória depender delas.

---

# 3. REGRA MAIS IMPORTANTE — AUDITORIA REAL DAS APIs

Antes de implementar qualquer funcionalidade, investigue as versões exatas das dependências.

**É proibido inventar classes, métodos, eventos, campos ou comportamentos das APIs.**

Verifique:

1. Código-fonte, documentação e artefatos reais do Cobblemon 1.8.3.
2. Eventos relacionados à captura de Pokémon.
3. Como identificar capturas legítimas e suas origens.
4. Como consultar a Pokédex oficial de um jogador.
5. Como consultar o registro dinâmico de espécies.
6. Como identificar espécies, formas, tipos, shinies, lendários e outras categorias.
7. Como criar e entregar Pokémon com atributos personalizados.
8. Como consultar e modificar moedas pela Impactor API 5.3.5.
9. Como consultar permissões pelo LuckPerms.
10. Como registrar placeholders na versão compatível da Fabric Text Placeholder API.
11. Como construir menus de inventário server-side.
12. Como entregar chaves físicas do Shirokai Crates, se a integração estiver disponível.

Para cada integração, documente:

- Dependência e versão verificadas.
- Classes e métodos utilizados.
- Origem da documentação ou do código consultado.
- Restrições encontradas.
- Testes realizados.
- Status: `VERIFIED`, `PENDING_TEST` ou `UNSUPPORTED`.

Não confunda compilação com validação funcional.

Caso determinada funcionalidade não seja suportada pela API, apresente uma alternativa tecnicamente correta. Não crie implementações fictícias.

**Entregável obrigatório da primeira fase:** `docs/API_COMPATIBILITY.md`.

---

# 4. Arquitetura geral

Organize o projeto em módulos ou pacotes com responsabilidades separadas.

Estrutura conceitual:

```text
shirokai-dex/
├── src/main/java/
│   └── .../shirokaidex/
│       ├── core/
│       ├── config/
│       ├── database/
│       ├── cobblemon/
│       ├── pokedex/
│       ├── categories/
│       ├── captures/
│       ├── statistics/
│       ├── rankings/
│       ├── competitions/
│       ├── rewards/
│       ├── achievements/
│       ├── menus/
│       ├── profiles/
│       ├── commands/
│       ├── permissions/
│       ├── placeholders/
│       ├── notifications/
│       ├── localization/
│       └── integrations/
├── src/main/resources/
├── src/test/
├── docs/
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

A estrutura pode ser adaptada, desde que preserve a separação de responsabilidades.

Requisitos arquiteturais:

- Processamento orientado a eventos.
- Não executar varreduras globais de jogadores a cada tick.
- Cache para consultas frequentes.
- Operações SQLite fora da thread principal quando apropriado.
- Operações com entidades e APIs do Minecraft na thread correta.
- Persistência transacional.
- Migrações de banco versionadas.
- Identificação de jogadores por UUID.
- Tratamento explícito de erros.
- Logs úteis e configuráveis.
- Nenhuma dependência de dados históricos do antigo DexRewards.

---

# 5. Pokédex personalizadas

Implementar múltiplas Pokédex independentes.

Pokédex iniciais:

- Nacional.
- Shiny.
- Lendária.

Permitir criar outras Pokédex por JSON.

Cada Pokédex deve possuir:

- ID exclusivo.
- Nome e descrição.
- Ícone.
- Critérios de inclusão de espécies.
- Modo de progresso.
- Recompensas.
- Menus.
- Permissões opcionais.
- Placeholders.
- Configurações de exibição.

Modos de progresso suportados:

- Espécies capturadas.
- Espécies vistas.
- Espécies shiny capturadas.
- Espécies shiny vistas.
- Formas registradas, quando os dados oficiais permitirem.

Padrões:

- Nacional: espécies capturadas.
- Shiny: espécies shiny capturadas.
- Lendária: lendários capturados.

Uma mesma espécie pode contribuir para várias Pokédex.

## 5.1. Integração com a Pokédex oficial

**Não modificar a Pokédex oficial do Cobblemon.**

O Shirokai Dex deve consultar os dados oficiais do jogador e calcular o progresso das suas próprias coleções.

Não utilizar comandos para conceder artificialmente registros de Pokédex.

Atualizar o progresso quando ocorrerem eventos relevantes e sincronizar quando necessário, como no login.

## 5.2. Espécies de datapacks

O servidor utiliza conteúdos adicionais, incluindo o ecossistema Complete Cobblemon Collection:

https://github.com/Complete-Cobblemon-Collection-Team

Não criar uma lista fixa de espécies no código.

Consultar o registro real de espécies disponibilizado pelo Cobblemon, incluindo espécies adicionais carregadas por datapacks.

O total de cada Pokédex deverá ser dinâmico.

Quando uma atualização adicionar ou remover espécies:

- Recalcular os totais disponíveis.
- Preservar recompensas já desbloqueadas.
- Não retirar recompensas já resgatadas.
- Evitar duplicação de desbloqueios.
- Registrar alterações relevantes para diagnóstico.

---

# 6. Wiki de Pokémon

Cada Pokédex deverá possuir uma Wiki navegável.

Informações desejadas:

- Nome.
- Espécie.
- Formas disponíveis.
- Tipos.
- Status de visto/capturado.
- Status shiny.
- Biomas de aparecimento.
- Dimensões.
- Horários.
- Outras condições de spawn quando disponíveis.

Não inventar informações de spawn.

Quando os dados não estiverem disponíveis, exibir uma mensagem equivalente a:

`Informação de aparecimento indisponível.`

Distinguir Pokémon sem spawn natural conhecido daqueles cujas informações não puderam ser consultadas.

Adicionar:

- Pesquisa.
- Filtros.
- Ordenação.
- Paginação.
- Navegação por categoria.

Usar ícones ou representações compatíveis com menus server-side.

Não exigir renderização 3D personalizada no cliente.

---

# 7. Sistema de categorias

Implementar categorias híbridas e extensíveis.

Categorias possíveis:

- Lendário.
- Mítico.
- Shiny.
- Ultra Beast.
- Paradox.
- Tipos elementais.
- Espécies específicas.
- Categorias personalizadas.

A identificação automática deve utilizar dados reais do Cobblemon.

Permitir por JSON:

- Incluir espécies.
- Excluir espécies.
- Filtrar por tipo.
- Filtrar por forma.
- Criar categorias.
- Combinar condições.

Validar identificadores.

Quando uma classificação não puder ser determinada automaticamente, exigir configuração explícita em vez de presumir informações.

## 7.1. Mudanças nas categorias

Preservar as classificações históricas por padrão.

Permitir recálculo administrativo de estatísticas quando houver dados suficientes, com:

- Prévia.
- Justificativa.
- Confirmação.
- Auditoria.
- Relatório de dados indisponíveis.

Nunca inventar eventos históricos.

Não modificar resultados oficiais de competições automaticamente.

## 7.2. Categorias durante competições

Cada competição possui:

`category_update_mode: SNAPSHOT | LIVE`

Padrão: `SNAPSHOT`.

- `SNAPSHOT`: congela as regras no início da edição.
- `LIVE`: aplica novas regras às próximas capturas, sem alterar automaticamente pontos anteriores.

Persistir a versão das regras utilizada.

---

# 8. Sistema de recompensas

Implementar recompensas configuráveis individualmente por Pokédex.

Critérios de desbloqueio:

- Quantidade de registros.
- Porcentagem de conclusão.

Cada marco pode conceder múltiplas recompensas.

Tipos suportados:

- `ITEM`
- `CUSTOM_ITEM`
- `SAVED_ITEM`
- `POKEMON`
- `COMMAND`
- `CURRENCY`
- `CRATE_KEY`, quando tecnicamente viável

Permitir recompensas como:

- Itens vanilla.
- Itens de mods.
- Itens com componentes personalizados do Minecraft 1.21.1.
- Pokémon.
- Moedas Impactor.
- Comandos autorizados.
- Chaves físicas do Shirokai Crates.

## 8.1. Pokémon como recompensa

A configuração poderá definir:

- Espécie.
- Nível.
- Shiny.
- IVs.
- EVs.
- Nature.
- Ability.
- Gender.
- Form.
- Pokéball.
- Moves.
- Held item.

Validar os atributos com a API real do Cobblemon 1.8.3.

Quando um atributo for omitido, utilizar um padrão válido do Cobblemon.

Não gerar Pokémon inválidos.

## 8.2. Moedas

Integrar com Impactor API 5.3.5.

Moedas iniciais:

```text
impactor:dollars
impactor:tokens
```

Exibição:

```text
PokéDollars: $
PokéTokens: ✦
```

Não converter automaticamente uma moeda na outra.

## 8.3. Itens salvos

Criar comandos administrativos equivalentes a:

```text
/dex admin item save <id>
/dex admin item list
/dex admin item give <id> <player>
/dex admin item delete <id>
```

Preservar corretamente os componentes dos itens.

## 8.4. Resgate

As recompensas devem ser:

- Desbloqueadas automaticamente.
- Resgatadas manualmente.
- Entregues apenas uma vez por jogador e identificador de recompensa.

Suportar:

- Prévia direta.
- GUI de prévia.
- Confirmação antes do resgate.

Verificar inventário e capacidade de armazenamento de Pokémon antes da entrega.

---

# 9. SEGURANÇA CRÍTICA DAS RECOMPENSAS

Este sistema exige atenção máxima.

Implementar um registro persistente de operações com identificadores exclusivos.

Estados previstos:

```text
PENDING
PROCESSING
NEEDS_REVIEW
COMPLETED
CANCELLED
```

Persistir:

- UUID do jogador.
- ID da recompensa.
- ID da operação.
- Etapas executadas.
- Estado atual.
- Data e hora.
- Erros.
- Evidências disponíveis.

Requisitos:

1. Impedir resgates simultâneos duplicados.
2. Impedir resgate repetido após reinicialização.
3. Verificar pré-condições antes da entrega.
4. Registrar a intenção da operação antes de efeitos externos.
5. Detectar operações interrompidas.
6. Não repetir automaticamente entregas externas de resultado incerto.
7. Permitir revisão administrativa.
8. Permitir retomada ou cancelamento supervisionado.
9. Manter auditoria.

**Não afirmar que existe transação atômica entre SQLite, inventário do Minecraft, Cobblemon, Impactor e execução de comandos se isso não for tecnicamente garantido.**

Quando não for possível garantir entrega exatamente uma vez, utilizar mecanismos de idempotência onde disponíveis e recuperação supervisionada nos demais casos.

Criar testes de falhas simuladas em diferentes etapas da entrega.

---

# 10. Estatísticas permanentes de capturas

Registrar capturas legítimas independentemente de o jogador estar inscrito em competições.

Estatísticas:

- Capturas totais.
- Capturas shiny.
- Capturas lendárias.
- Capturas míticas.
- Capturas por espécie.
- Capturas por categoria.
- Capturas diárias.
- Capturas semanais.
- Capturas mensais.
- Recordes pessoais.

Não confundir capturas totais com espécies diferentes registradas na Pokédex.

Capturar 10 Pikachu deve contar como 10 capturas permanentes, mas não como 10 espécies diferentes.

Os contadores começam a funcionar após a instalação do mod.

Não inventar capturas anteriores.

---

# 11. VALIDAÇÃO CRÍTICA DOS EVENTOS DE CAPTURA

Investigue como o Cobblemon 1.8.3 informa a captura de Pokémon.

O sistema deve tentar reconhecer:

- Capturas selvagens legítimas.
- Capturas de shinies.
- Capturas de lendários.
- Capturas de eventos autorizados.

Não contabilizar como capturas competitivas:

- Trocas.
- Ovos chocados.
- Pokémon criados por comandos administrativos.
- Pokémon concedidos como recompensa.
- Transferências entre armazenamento e equipe.

Se a API não fornecer informações suficientes para determinar a origem, não classificar a captura como legítima por suposição.

Documentar a limitação e implementar a melhor estratégia comprovadamente compatível.

## 11.1. Proteção contra duplicação

Evitar processar o mesmo evento duas vezes.

Registrar identificadores confiáveis quando disponíveis.

Não usar somente a espécie e o horário como prova de duplicação, pois capturas distintas podem ocorrer próximas.

Uma captura pode pontuar em múltiplas categorias elegíveis, mas apenas uma vez em cada ranking correspondente.

## 11.2. Antiexploit

Implementar:

- Validação de origem.
- Detecção de duplicações.
- Auditoria de padrões suspeitos.
- Limites opcionais por período.
- Revisão administrativa.

Taxas altas de captura, isoladamente, não comprovam abuso.

Modos por competição:

```text
LOG_ONLY
HOLD_FOR_REVIEW
```

Padrão: `LOG_ONLY`.

Capturas comprovadamente inválidas não pontuam.

---

# 12. Histórico detalhado de capturas

Armazenar registros individuais no SQLite.

Campos relevantes:

- Identificador do evento.
- UUID do jogador.
- Espécie.
- Forma.
- Shiny.
- Categorias aplicáveis.
- Origem validada.
- Data e hora.
- Competições relacionadas.
- Status da pontuação.

Retenção padrão:

```json
{
  "capture_history": {
    "enabled": true,
    "retention_days": 90,
    "cleanup": {
      "enabled": true,
      "batch_size": 1000
    }
  }
}
```

Limpeza automática em lotes.

Preservar:

- Estatísticas agregadas permanentes.
- Resultados oficiais.
- Auditorias obrigatórias.
- Registros de revisões pendentes.
- Dados necessários à recuperação de operações.

**O histórico individual será exclusivo para administradores.**

Jogadores comuns não terão uma GUI de histórico de cada captura.

---

# 13. Rankings permanentes

Implementar dois sistemas distintos.

## 13.1. Ranking de conclusão da Pokédex

Baseado no progresso oficial de cada coleção.

Exemplos:

- Nacional.
- Shiny.
- Lendária.
- Pokédex personalizadas.

## 13.2. Rankings permanentes de capturas

Baseados nas estatísticas acumuladas desde a instalação do mod.

Exemplos:

- TOP capturas totais.
- TOP shiny.
- TOP lendário.
- TOP por espécie.
- TOP por categoria.

Não exigem inscrição.

Não reiniciam semanalmente ou mensalmente.

Não concedem recompensas automaticamente.

Permitir criação de rankings por JSON.

## 13.3. Desempates

Ordenar por:

1. Maior pontuação.
2. Data e hora mais antiga em que o jogador atingiu a pontuação atual.
3. UUID como critério final determinístico.

Utilizar SQLite e cache para consultas eficientes.

---

# 14. Sistema de competições

Implementar competições configuráveis por JSON.

Tipos:

- Semanais.
- Mensais.
- Especiais.
- Períodos personalizados.

Categorias:

- Maior quantidade de capturas.
- Maior quantidade de shinies capturados.
- Maior quantidade de lendários capturados.
- Capturas por espécie.
- Categorias personalizadas.

**Regra essencial: competições contam capturas legítimas, não novas entradas da Pokédex.**

Capturas repetidas da mesma espécie podem pontuar.

## 14.1. Inscrição obrigatória

O jogador deve se inscrever pelo menu `/dex top`.

A inscrição pode ocorrer em qualquer momento durante uma edição ativa.

Somente capturas realizadas após a inscrição contam para aquela edição.

Não contabilizar capturas anteriores retroativamente.

Persistir inscrição, horário e edição.

## 14.2. Cancelamento da inscrição

Permitir que o jogador cancele sua participação durante a competição.

Exigir confirmação.

Consequências:

- Remover o jogador da classificação competitiva daquela edição.
- Impedir novas pontuações naquela edição.
- Torná-lo inelegível para premiações.
- Impedir reinscrição na mesma edição.
- Permitir inscrição normal na próxima edição.

Manter registro de auditoria.

## 14.3. Calendário

Configuração individual por competição.

Timezone padrão:

```text
America/Sao_Paulo
```

Padrões:

- Semanal: segunda-feira, 00:00.
- Mensal: primeiro dia do mês, 00:00.

Persistir identificadores únicos de cada edição.

Usar instantes e fuso horário de forma consistente, inclusive em reinicializações e mudanças de horário.

## 14.4. Reinicializações e manutenção

As competições seguem automaticamente o calendário.

Não implementar comandos administrativos para:

- Pausar.
- Retomar.
- Estender.
- Cancelar manualmente uma edição.

Se o servidor ficar offline, o prazo continua transcorrendo.

Ao reiniciar:

- Recuperar edições ativas.
- Encerrar edições cujo prazo terminou.
- Preservar inscrições e pontuações.
- Não inventar capturas ocorridas enquanto o servidor estava offline.
- Não gerar recompensas duplicadas.

---

# 15. Classificações das competições

Cada competição pode configurar visibilidade:

```text
PUBLIC
PARTIAL
HIDDEN
```

Padrão: `PUBLIC`.

Exibir conforme configuração:

- Nome do jogador.
- Posição.
- Pontuação.
- Tempo restante.
- Estado da competição.
- Inscrição.
- Prêmios disponíveis.
- Resultados anteriores.

Respeitar configurações de privacidade.

---

# 16. Premiações das competições

Recompensas opcionais.

Padrão: desativadas.

Faixas:

- TOP 1.
- TOP 3.
- TOP 10.

**As faixas são exclusivas, sem acumulação.**

Exemplo:

- 1º lugar recebe somente o pacote TOP 1.
- 2º e 3º recebem o pacote TOP 3.
- 4º ao 10º recebem o pacote TOP 10.

Não somar os pacotes.

Permitir requisitos de elegibilidade:

- Quantidade mínima de participantes.
- Quantidade mínima de capturas.
- Critérios por posição.
- Outros requisitos configurados.

Esses requisitos afetam a premiação, não a posição pública do jogador.

## 16.1. Redistribuição

Configurável individualmente por ranking.

Padrão: desativada.

Quando habilitada, permitir transferir um pacote não atribuído ao próximo jogador elegível, sem alterar as posições públicas.

Não redistribuir uma recompensa já atribuída apenas porque o vencedor não a resgatou.

Cada jogador pode receber no máximo um pacote de premiação por edição.

## 16.2. Validade

Cada ranking configura a validade das recompensas.

Opções:

- Permanente.
- Expiração após determinado número de dias.

O prazo começa quando a recompensa é oficialmente registrada.

Jogadores offline também estão sujeitos à expiração configurada.

O resgate é manual.

---

# 17. Revisão e finalização das competições

Cada competição terá período de revisão configurável.

Padrão:

```json
{
  "result_review": {
    "enabled": true,
    "duration_hours": 24,
    "auto_finalize": true,
    "hold_affected_rewards": true,
    "announce_only_final_results": true
  }
}
```

Fluxo:

1. Competição encerra.
2. Classificação provisória é registrada.
3. Inicia o período de revisão.
4. Administradores podem investigar pontuações.
5. Após o prazo, resultados sem pendências são oficializados.
6. Pendências que possam alterar vencedores impedem a premiação afetada.
7. Após a resolução, os resultados correspondentes são finalizados.
8. Recompensas são disponibilizadas.
9. Anúncios oficiais são enviados, conforme configuração.

Não permitir duplicação de finalização após reinicialização.

## 17.1. Revisão exclusiva para administradores

**Não implementar sistema de contestação pelos jogadores.**

Administradores poderão:

- Consultar capturas.
- Inspecionar eventos.
- Aprovar ou rejeitar pontos retidos.
- Invalidar pontos competitivos.
- Ajustar pontuações.
- Reverter correções.
- Consultar histórico de alterações.

Toda correção exige justificativa e auditoria.

## 17.2. Correções competitivas

**Decisão obrigatória do projeto: corrigir apenas a competição.**

Quando um administrador invalidar uma captura na pontuação de uma edição:

- Corrigir a pontuação daquela competição.
- Não diminuir estatísticas permanentes.
- Não alterar rankings permanentes de capturas.
- Não modificar a Pokédex oficial.

Essa regra não impede o sistema separado de recálculo administrativo de categorias, que deve ter suas próprias validações e limites.

---

# 18. Histórico de competições

Criar `/dex history`.

Permitir visualizar:

- Competições encerradas.
- Vencedores.
- Pontuações.
- Períodos.
- Categorias.
- Premiações.
- Medalhas e resultados relevantes.

Suportar filtros por competição, categoria e período.

Preservar os resultados oficiais no SQLite.

Não reescrever automaticamente edições históricas quando um JSON for alterado.

---

# 19. Sistema de conquistas e medalhas

Implementar conquistas personalizáveis por JSON.

Requisitos possíveis:

- Capturas.
- Progresso da Pokédex.
- Porcentagem de conclusão.
- Espécies específicas.
- Shinies.
- Formas.
- Combinação de Pokédex.
- Condições AND/OR.
- Dependências entre conquistas.

Permitir conquistas secretas.

Cada medalha poderá definir:

- Nome.
- Descrição.
- Ícone.
- Requisitos.
- Visibilidade.
- Dicas.
- Notificação.
- Política de desbloqueio.

Políticas:

```text
PERMANENT
RECALCULATE
```

Padrão: `PERMANENT`.

Conquistas são **medalhas visuais, sem recompensas materiais**.

## 19.1. Medalhas favoritas

Cada jogador poderá destacar até **3 medalhas desbloqueadas** em seu perfil.

Configuração pela GUI.

As demais medalhas continuam disponíveis na página de conquistas.

## 19.2. Retroatividade

Configurar por medalha.

Permitir desbloqueio retroativo apenas quando existirem dados confiáveis suficientes.

Não inventar capturas antigas.

---

# 20. Perfis dos treinadores

Implementar:

```text
/dex profile
/dex profile <player>
```

Informações:

- Nome e UUID.
- Progresso das Pokédex.
- Posições em rankings.
- Capturas permanentes.
- Shinies.
- Lendários.
- Recordes.
- Competições vencidas.
- Recompensas resgatadas.
- Conquistas.
- Três medalhas favoritas.

Suportar jogadores offline com dados persistidos.

Permitir abrir perfis por cabeças de jogadores nos rankings.

---

# 21. Privacidade personalizável

Cada jogador poderá configurar a visibilidade de:

- Capturas totais.
- Capturas shiny.
- Capturas lendárias.
- Capturas por espécie.
- Capturas por categoria.
- Recordes pessoais.

Integrar ao `/dex settings`.

Permitir padrões por JSON.

Exemplo:

```json
{
  "statistics_privacy": {
    "enabled": true,
    "defaults": {
      "total_captures": true,
      "shiny_captures": true,
      "legendary_captures": true,
      "species_captures": false,
      "category_captures": true,
      "personal_records": true
    },
    "allow_player_override": true
  }
}
```

Salvar preferências por UUID.

Aplicar a privacidade aos perfis públicos, rankings e placeholders públicos, conforme o tipo de informação.

Manter acesso administrativo autorizado para auditoria.

O sistema já prevê opção de ocultar o jogador dos rankings públicos. Jogadores que optarem por essa exclusão não serão elegíveis às recompensas competitivas enquanto ela estiver ativa, preservando recompensas já conquistadas.

---

# 22. Sistema completo de menus JSON

Criar menus server-side, personalizáveis sem modificar o `.jar`.

Configurações:

- Título.
- Tamanho.
- Slots.
- Itens.
- Nomes.
- Lore.
- Gradientes.
- Bordas.
- Ícones.
- Placeholders.
- Paginação.
- Pesquisa.
- Ordenação.
- Ações.
- Condições.
- Modelos reutilizáveis.
- Elementos dinâmicos.

Menus previstos:

- Seletor de Pokédex.
- Pokédex individual.
- Wiki.
- Recompensas.
- Prévia de recompensas.
- Rankings.
- Competições.
- Histórico.
- Conquistas.
- Perfil.
- Configurações.

Permitir layouts fixos e posicionamento automático de itens dinâmicos.

Validar conflitos de slots e tamanhos inválidos.

## 22.1. Padrão visual do Shirokai

Usar como padrão:

- Títulos com gradientes.
- Nomes destacados com `✦`.
- Lore curta e organizada.
- Paletas temáticas por menu.
- Perfil do treinador.
- Botões de navegação consistentes.

Perfil do treinador deve suportar:

```text
%luckperms:prefix%
%dexrewards:player_dex_percent%
%player:objective gyms%
%cobbledollars:balance%
```

Os placeholders externos devem ser utilizados apenas quando suas integrações estiverem disponíveis.

O placeholder legado do DexRewards deverá ter compatibilidade quando possível, sem conflitos com outro provedor.

## 22.2. Botão de retorno

Manter o padrão:

```json
{
  "item": "minecraft:arrow",
  "name": "<yellow><bold>VOLTAR AO MENU</bold></yellow>",
  "lore": [
    "<gray>Retornar ao menu principal.",
    "",
    "<yellow>➜ Clique para voltar!"
  ]
}
```

A ação executará `/menu` como jogador.

Respeitar os formatos de ação próprios de cada sistema, sem presumir que o JSON do SkiesGUIs seja diretamente compatível com o formato interno do Shirokai Dex.

## 22.3. Botões bloqueados

**Configuração individual por botão.**

Modos:

```text
HIDE
LOCKED
REPLACE
```

Permitir requisitos baseados em:

- LuckPerms.
- Progresso da Pokédex.
- Conquistas.
- Condições personalizadas autorizadas.

Um botão poderá:

- Desaparecer.
- Permanecer visível e bloqueado.
- Ser substituído por outro item.

Revalidar os requisitos no servidor ao executar a ação.

Não confiar somente na aparência da GUI para segurança.

Ferramentas administrativas devem continuar protegidas e ocultas dos jogadores comuns.

---

# 23. Integração com SkiesGUIs

A integração será híbrida e configurável.

O Shirokai Dex controlará seus próprios menus.

O SkiesGUIs poderá abrir suas funcionalidades por comandos públicos.

Exemplos:

```text
/dex
/dex national
/dex shiny
/dex legendary
/dex rewards
/dex top
/dex achievements
/dex profile
/dex settings
/dex history
```

Permitir abrir diretamente essas páginas a partir do menu principal do Shirokai, warps, NPCs ou outros menus.

Não depender de APIs internas não verificadas do SkiesGUIs.

Aliases adicionais poderão ser avaliados como configuração opcional, sem substituir ou comprometer os comandos oficiais.

Detectar conflitos de registro de comandos e não sobrescrever silenciosamente comandos de outros mods.

---

# 24. Placeholders

Integrar com a Fabric Text Placeholder API compatível.

Criar placeholders para:

- Progresso de cada Pokédex.
- Quantidade capturada.
- Quantidade vista.
- Total de espécies.
- Espécies restantes.
- Recompensas disponíveis.
- Recompensas resgatadas.
- Posição em rankings.
- Capturas totais.
- Capturas shiny.
- Capturas lendárias.
- Estatísticas semanais.
- Estatísticas mensais.
- Medalhas.
- Vitórias em competições.

Sintaxe conceitual:

```text
%shirokaidex:percent/national%
%shirokaidex:caught/national%
%shirokaidex:total/national%
%shirokaidex:stats/total_captures%
%shirokaidex:stats/shiny_captures%
%shirokaidex:stats/legendary_captures%
```

Verificar a sintaxe e o comportamento reais da API.

Permitir placeholders personalizados por JSON quando tecnicamente viável.

Compatibilidade legada desejada:

```text
%dexrewards:player_dex_percent%
```

Registrar esse alias apenas se não houver conflito com outro mod.

Não modificar ou sequestrar registros de outros provedores.

---

# 25. Notificações

Implementar notificações configuráveis.

Canais:

- Chat.
- Actionbar.
- Title.
- Sons.

Eventos:

- Progresso.
- Recompensa desbloqueada.
- Recompensa resgatada.
- Medalha conquistada.
- Inscrição em competição.
- Encerramento de competição.
- Resultados oficiais.
- Premiações disponíveis.

Permitir:

- Mensagens por JSON.
- Notificações individuais.
- Anúncios globais.
- Preferências do jogador.
- Controle de duplicações.

Anúncios globais devem ser desativados por padrão.

Persistir preferências.

---

# 26. Idiomas

Suportar inicialmente:

```text
pt_br
en_us
```

Detectar o idioma do cliente quando disponível.

Permitir substituição individual:

```text
/dex language
```

Armazenar traduções em arquivos separados.

Não deixar mensagens importantes fixas diretamente no código Java.

---

# 27. Administração

Criar comandos administrativos completos, sem necessidade de GUI administrativa.

Funcionalidades:

- Recarregar configurações.
- Validar JSONs.
- Consultar diagnósticos.
- Inspecionar jogadores.
- Consultar histórico de capturas.
- Inspecionar competições.
- Corrigir pontuações.
- Revisar capturas suspeitas.
- Reverter correções.
- Gerenciar itens salvos.
- Consultar operações de recompensa.
- Recuperar entregas pendentes.
- Exportar dados.
- Importar dados.
- Criar backups.
- Restaurar backups.
- Consultar logs de auditoria.

Exemplos conceituais:

```text
/dex admin reload
/dex admin validate
/dex admin debug
/dex admin captures <player>
/dex admin competition inspect <competition> <edition> <player>
/dex admin competition adjust <competition> <edition> <player> <points> <reason>
/dex admin competition review <id>
/dex admin reward inspect <operation_id>
/dex admin reward recover <operation_id>
/dex admin backup
```

Os comandos definitivos devem possuir sintaxe consistente, sugestões e mensagens de erro úteis.

Toda operação sensível exige permissão LuckPerms.

Operações destrutivas exigem confirmação.

Correções exigem justificativa.

Registrar UUID do administrador, horário e alterações realizadas.

Não incluir comandos administrativos para pausar, retomar, estender ou cancelar edições competitivas.

---

# 28. CONFIGURAÇÕES JSON — SEGURANÇA OBRIGATÓRIA

Todas as configurações devem ser validadas antes da aplicação.

Verificar:

- Sintaxe JSON.
- Campos obrigatórios.
- Tipos.
- IDs duplicados.
- Referências inexistentes.
- Espécies inválidas.
- Itens inválidos.
- Slots inválidos.
- Conflitos de layout.
- Permissões.
- Dependências entre conquistas.
- Ciclos de referências.
- Recompensas incompatíveis.
- Categorias inexistentes.
- Calendários inválidos.
- Ações não autorizadas.

## 28.1. Reload transacional

Ao executar reload:

1. Ler os arquivos.
2. Criar uma configuração candidata.
3. Validar tudo.
4. Verificar referências cruzadas.
5. Informar erros encontrados.
6. Aplicar somente quando a configuração estiver válida.

Se ocorrer erro:

- Manter a última configuração válida.
- Não aplicar alterações parcialmente.
- Não encerrar o servidor.
- Informar o arquivo e o campo problemático.

Implementar backups de configurações.

Permitir recuperação de uma configuração válida.

---

# 29. SQLite e persistência

Utilizar SQLite com esquema versionado.

Entidades conceituais:

```text
players
player_settings
dex_progress_cache
reward_unlocks
reward_claims
reward_operations
capture_events
capture_statistics
capture_category_statistics
personal_records
rankings
competition_definitions
competition_editions
competition_enrollments
competition_scores
competition_capture_scores
competition_reviews
competition_results
competition_prizes
achievements
player_achievements
favorite_medals
saved_items
audit_log
```

A estrutura pode ser reorganizada conforme a arquitetura escolhida.

Requisitos:

- Índices adequados.
- Migrações versionadas.
- Transações.
- Integridade referencial.
- Persistência de estados intermediários.
- Backups consistentes.
- Consultas paginadas.
- Evitar consultas SQLite pesadas na thread principal.
- Evitar perda de dados em reinicializações.

Não armazenar informações que possam ser consultadas com segurança diretamente na Pokédex oficial sem necessidade, salvo caches e snapshots justificados.

---

# 30. Desempenho

O mod será utilizado em servidor multiplayer.

Requisitos:

- Evitar processamento por tick quando eventos forem suficientes.
- Evitar consultas completas ao banco em cada abertura de menu.
- Usar cache com invalidação correta.
- Paginar históricos e rankings.
- Processar limpeza do banco em lotes.
- Evitar carregar todas as capturas na memória.
- Evitar serialização excessiva.
- Não bloquear a thread principal com operações de banco ou disco.
- Limitar consultas e atualizações caras.
- Liberar recursos corretamente no encerramento do servidor.

Preparar testes para dezenas de jogadores simultâneos e volumes grandes de dados.

Não afirmar capacidade para determinado número de jogadores sem testes.

---

# 31. Testes obrigatórios

Criar testes automatizados sempre que possível.

## 31.1. Cobblemon

Testar:

- Captura selvagem.
- Captura shiny.
- Captura lendária.
- Capturas repetidas.
- Trocas.
- Ovos.
- Pokémon concedidos por comandos.
- Pokémon concedidos por recompensas.
- Espécies adicionais por datapacks.
- Mudanças no registro de espécies.
- Progresso da Pokédex oficial.

## 31.2. Recompensas

Testar:

- Primeiro resgate.
- Tentativa de segundo resgate.
- Dois resgates simultâneos.
- Inventário cheio.
- Falta de espaço para Pokémon.
- Falha da Impactor API.
- Falha na execução de comando.
- Reinicialização durante entrega.
- Operação interrompida.
- Recuperação administrativa.

## 31.3. Competições

Testar:

- Início automático.
- Inscrição.
- Inscrição tardia.
- Capturas anteriores à inscrição.
- Cancelamento definitivo da inscrição.
- Capturas simultâneas.
- Pontuação por categoria.
- Mudanças de regras `SNAPSHOT` e `LIVE`.
- Encerramento automático.
- Revisão de 24 horas.
- Correção de pontos.
- Premiações por faixa exclusiva.
- Redistribuição opcional.
- Expiração de recompensas.
- Reinicialização antes e depois do encerramento.
- Servidor offline durante o encerramento.
- Ausência de premiações duplicadas.

## 31.4. Configurações

Testar:

- JSON malformado.
- ID duplicado.
- Item inexistente.
- Espécie inexistente.
- Slot inválido.
- Referência circular.
- Reload inválido.
- Preservação da última configuração válida.

## 31.5. Segurança

Testar:

- Jogador sem permissão.
- Jogador tentando executar ação de botão bloqueado.
- Comandos administrativos não autorizados.
- Placeholders maliciosos em comandos.
- Tentativas de resgate duplicado.
- Dados inconsistentes.
- Ações administrativas sem confirmação.

## 31.6. Testes de integração

Distinguir claramente:

- Testes unitários.
- Testes automatizados de integração.
- Testes em servidor Fabric real.
- Testes manuais pendentes.

Nunca declarar que um teste foi realizado sem evidência.

---

# 32. GitHub Actions, builds e releases

Configurar GitHub Actions.

Pipeline:

1. Preparar Java 21.
2. Resolver dependências.
3. Compilar o projeto.
4. Executar testes automatizados.
5. Verificar falhas.
6. Gerar o `.jar`.
7. Publicar o artifact do workflow.

Criar releases versionadas conforme o fluxo do projeto.

Para cada alteração validada e integrada ao fluxo de distribuição, disponibilizar uma versão ou artifact identificável, com changelog e commit correspondente.

Não publicar uma release como estável se os testes obrigatórios falharem.

Os arquivos devem indicar claramente:

- Versão do mod.
- Versão do Minecraft.
- Versão do Cobblemon.
- Versão do Java.
- Estado de estabilidade.

Documentar qual `.jar` deve ser instalado no servidor.

---

# 33. Desenvolvimento obrigatório em fases

## FASE 0 — Auditoria técnica

Antes de desenvolver funcionalidades complexas:

- Investigar as APIs reais.
- Verificar dependências.
- Confirmar eventos de captura.
- Confirmar acesso à Pokédex.
- Confirmar registro dinâmico de espécies.
- Confirmar criação de Pokémon.
- Confirmar integração econômica.
- Criar `API_COMPATIBILITY.md`.
- Identificar bloqueios técnicos.

**Não avançar com integrações críticas baseadas em suposições.**

## FASE 1 — Fundação

Implementar:

- Estrutura Gradle.
- Inicialização Fabric.
- Sistema de configuração.
- SQLite.
- Migrações.
- Logs.
- Comando `/dex`.
- Menu inicial.
- Integração básica com Cobblemon.

Validar compilação e inicialização.

## FASE 2 — Pokédex

Implementar:

- Pokédex Nacional.
- Shiny.
- Lendária.
- Categorias.
- Progresso oficial.
- Datapacks.
- Wiki.
- Menus.
- Placeholders básicos.

Testar com dados reais.

## FASE 3 — Recompensas

Implementar:

- Marcos de progresso.
- Recompensas.
- Itens personalizados.
- Pokémon.
- Impactor.
- Itens salvos.
- Resgate manual.
- Persistência segura.
- Recuperação de operações.

Não avançar sem testar duplicações e interrupções.

## FASE 4 — Capturas e estatísticas

Implementar:

- Eventos de captura validados.
- Deduplicação.
- Estatísticas permanentes.
- Categorias.
- Histórico.
- Retenção.
- Recordes.
- Rankings permanentes.

## FASE 5 — Competições

Implementar:

- Calendários.
- Inscrições.
- Cancelamentos.
- Pontuação.
- Proteções.
- Revisões.
- Finalização.
- Premiações.
- Histórico.

Testar reinicializações e encerramentos.

## FASE 6 — Conquistas e perfis

Implementar:

- Medalhas.
- Requisitos.
- Conquistas secretas.
- Favoritas.
- Perfis.
- Privacidade.
- Estatísticas públicas.

## FASE 7 — Personalização e integrações

Implementar:

- Menus JSON avançados.
- Condições dos botões.
- Integração SkiesGUIs.
- LuckPerms.
- Placeholders completos.
- Idiomas.
- Notificações.
- Ferramentas administrativas.

## FASE 8 — Validação final

Executar:

- Testes automatizados.
- Testes de integração.
- Testes de reinicialização.
- Testes de configurações inválidas.
- Testes de recompensas.
- Testes de desempenho.
- Verificação das dependências.
- Build final.
- Documentação.
- Release.

---

# 34. Critérios de aceitação

Uma funcionalidade só pode ser marcada como concluída quando:

1. O código estiver implementado.
2. Compilar com as versões corretas.
3. Possuir tratamento de erros.
4. Possuir persistência quando necessária.
5. Possuir validação de permissões.
6. Não comprometer outros sistemas.
7. Passar pelos testes automatizados aplicáveis.
8. Ter testes de integração realizados ou explicitamente pendentes.
9. Estar documentada.
10. Não depender de APIs fictícias.

Ao final de cada fase, fornecer:

- Resumo do que foi implementado.
- Arquivos alterados.
- Dependências utilizadas.
- Testes executados e resultados.
- Funcionalidades verificadas.
- Limitações.
- Pendências.
- Link ou identificação do artifact `.jar`, quando disponível.
- Próximos passos.

Não utilizar expressões como “100% funcional” ou “totalmente testado” sem evidências suficientes.

---

# 35. Documentação obrigatória

Criar:

```text
README.md
docs/INSTALLATION.md
docs/CONFIGURATION.md
docs/COMMANDS.md
docs/PERMISSIONS.md
docs/PLACEHOLDERS.md
docs/API_COMPATIBILITY.md
docs/DATABASE.md
docs/REWARDS.md
docs/COMPETITIONS.md
docs/TESTING.md
docs/TROUBLESHOOTING.md
```

Documentar os JSONs com exemplos completos.

Incluir instruções para:

- Instalar o mod.
- Configurar as dependências.
- Criar Pokédex.
- Criar recompensas.
- Criar competições.
- Configurar categorias.
- Personalizar menus.
- Configurar LuckPerms.
- Integrar SkiesGUIs.
- Consultar logs.
- Criar backups.
- Recuperar operações pendentes.

---

# 36. Referência ao DexRewards

O Shirokai Dex poderá utilizar o DexRewards como referência conceitual:

https://github.com/Unariginal/DexRewards

Entretanto:

- Não copiar código sem verificar a licença.
- Não depender do DexRewards instalado.
- Não modificar dados do DexRewards.
- Não assumir compatibilidade de APIs sem verificação.

O Shirokai Dex deverá ser um mod independente.

---

# 37. Regras finais do projeto

Estas regras têm prioridade durante todo o desenvolvimento:

**REGRA 1 — Compatibilidade real**

Utilizar somente APIs e comportamentos comprovados.

**REGRA 2 — Integridade da Pokédex**

Nunca alterar indevidamente os registros oficiais do Cobblemon.

**REGRA 3 — Capturas corretas**

Separar progresso de Pokédex, estatísticas permanentes e pontos de competições.

**REGRA 4 — Recompensas seguras**

Evitar duplicações e tratar falhas de entrega com persistência e recuperação.

**REGRA 5 — Persistência**

Nenhuma reinicialização normal deve apagar competições, inscrições, pontuações, conquistas ou recompensas.

**REGRA 6 — Configurações seguras**

JSONs inválidos não devem derrubar o servidor nem substituir configurações válidas.

**REGRA 7 — Desempenho**

Priorizar eventos, cache e operações eficientes.

**REGRA 8 — Segurança**

Validar permissões e requisitos no servidor.

**REGRA 9 — Transparência**

Informar limitações reais, testes pendentes e integrações não verificadas.

**REGRA 10 — Desenvolvimento incremental**

Não implementar todos os sistemas simultaneamente.

**REGRA 11 — Respeitar as decisões do projeto**

Não alterar automaticamente decisões funcionais já estabelecidas, especialmente inscrição obrigatória, cancelamento definitivo de participação, faixas exclusivas de premiação, revisão administrativa, estatísticas permanentes independentes e calendário automático.

**REGRA 12 — Entrega utilizável**

Produzir código-fonte, configurações, documentação, testes e builds reais, não apenas exemplos ou pseudocódigo.

---

# 38. INSTRUÇÃO PARA COMEÇAR AGORA

Comece pela **FASE 0 — Auditoria técnica e compatibilidade real das APIs**.

Primeiro:

1. Inspecione o repositório atual e identifique o estado do projeto.
2. Confirme Minecraft 1.21.1, Fabric, Java 21 e Cobblemon 1.8.3.
3. Investigue os eventos reais de captura do Cobblemon.
4. Verifique a API da Pokédex oficial.
5. Verifique como obter espécies registradas por datapacks.
6. Investigue a criação e entrega de Pokémon.
7. Verifique a Impactor API 5.3.5.
8. Verifique as demais integrações.
9. Documente os resultados em `docs/API_COMPATIBILITY.md`.
10. Apresente os riscos, incompatibilidades e alternativas reais.

Depois, prepare a estrutura inicial do projeto e avance para a FASE 1.

**Não pule a auditoria técnica. Não invente APIs. Não declare integrações como funcionais sem testes.**

O objetivo final é entregar o **Shirokai Dex**, um mod completo, estável, seguro, otimizado e personalizável para o servidor Shirokai.