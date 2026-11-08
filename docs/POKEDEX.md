# Pokédex e porcentagem — início da Fase 2

O percentual foi solicitado explicitamente pelo usuário e já consta no prompt mestre. Esta etapa adiciona consulta por coleção e porcentagem; não conclui todos os sistemas da Fase 2.

A alpha.3 acrescenta a [wiki de espécies](WIKI.md), com pesquisa, filtros, ordenação, paginação e detalhes somente de leitura em cada coleção.

## Como aparece

- `/dex`: seletor com Nacional, Shiny e Lendária; cada ícone mostra seu estado de progresso.
- `/dex national` e `/dex legendary`: página da coleção com quantidade registrada/total, vistas e porcentagem. A seta retorna ao seletor do Dex.
- `/dex national status` e `/dex legendary status`: resumo no chat, por exemplo `Nacional: 5/10 — 50,00%.`
- `/dex status`: resumo oficial anterior, agora também com percentual Nacional.
- `/dex shiny` e `/dex shiny status`: quantidade/percentual de espécies shiny capturadas com prova; N/D enquanto fonte confiável não carregou (alpha.5).

O botão de retorno desta página volta ao **seletor interno**. Integração com `/menu` externo e personalização completa de botões JSON continuam para a fase correspondente.

## Cálculo

`registradas × 100 ÷ total de espécies elegíveis implementadas no registro atual`.

São exibidas duas casas decimais (vírgula em pt_br, ponto em en_us), truncadas para baixo. Assim, 100000/100001 exibe 99,99%, sem anunciar conclusão antes da última espécie. 0/total positivo é 0,00%. Registro vazio é N/D, não 100%. Dados sem evidência também são N/D e não são convertidos em zero.

Cada espécie conta uma vez, mesmo com vários exemplares ou formas. IDs com namespace são preservados, sem lista fixa de espécies.

## Fontes oficiais

Nacional usa o maior conhecimento oficial da espécie (`SEEN`/`OWNED`), sem perder seu agregado só porque uma forma antiga não aparece no catálogo atual. **Obtido na Pokédex oficial não equivale a captura selvagem competitiva.**

Lendária consulta labels das formas reais, incluindo a forma padrão mesmo quando `Species.forms` está vazio. Para registrar a espécie nessa coleção, é necessário conhecimento `OWNED` de uma forma classificada como lendária. Não basta ter uma forma comum de uma espécie que também tenha outra forma lendária.

Shiny permanece sem percentual numérico porque a API auditada registra shiny visto separadamente do maior estado de obtenção: possuir um normal e ver um shiny não comprova capturar um shiny. Nem mesmo encontrar um shiny obtido via comando no armazenamento comprova captura selvagem. Uma fonte histórica verificável será implementada/auditada na fase de capturas; nenhuma contagem retroativa foi inventada aqui.

## Atualizações e desempenho

Snapshots imutáveis por UUID, consultados somente na thread do servidor, com cache de até 5 segundos. O evento oficial `POKEDEX_DATA_CHANGED_POST`, login, logout, reload do Dex e lifecycle invalidam o cache. Percentual é atualizado na próxima consulta/reabertura do menu, sem varredura global por tick. Nenhum registro oficial é modificado pelo Dex.

O total acompanha o registro real após startup. Alterações de datapack de espécies exigem reinício: Cobblemon 1.8.3 não declara seguro hot reload dessas espécies. Isso é diferente do reload de configuração do Dex.

## Limites desta etapa

[Coleções por JSON](COLLECTIONS.md) e filtros iniciais foram adicionados na alpha.2. Critérios avançados/categorias históricas, dados de spawn e demais evoluções da wiki, integração TAB e recompensas continuam pendentes. Os [placeholders básicos da Text Placeholder API](PLACEHOLDERS.md) estão disponíveis na alpha.4. Pesquisa/paginação por espécie já estão disponíveis na alpha.3. Nacional/Lendária são as coleções iniciais de progresso, não a Fase 2 completa. O cálculo puro e o adaptador Cobblemon estão separados para permitir essa evolução.

## Estatísticas de captura verificadas separadas

A versão 0.4.0-alpha.1 acrescenta [ledger/contadores próprios de capturas](CAPTURES.md), incluindo shiny comprovado no escopo auditado. A coleção Shiny/percentual descrita acima ainda não foi ligada a esse ledger e permanece N/D. Não confundir contador de novas capturas shiny com o denominador/percentual de conclusão.

## Atualização alpha.5

A ligação antes pendente foi implementada: [Shiny por capturas verificadas](SHINY_CAPTURES.md). Os limites N/D descritos para versões anteriores são históricos; visto/obtido continuam insuficientes para comprovar captura.
