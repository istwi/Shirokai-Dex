# Wiki de espécies — alpha.6

A wiki consulta o registro real de espécies **implementadas** e a Pokédex oficial do próprio jogador. Está disponível em cada coleção pública ou autorizada, incluindo coleções JSON e a coleção Shiny cujo progresso de captura continua N/D.

## Abrir e pesquisar

```text
/dex national wiki
/dex legendary wiki
/dex shiny wiki
/dex <id_da_colecao> wiki
/dex national wiki pikachu
/dex <id_da_colecao> wiki namespace:especie
```

A página de uma coleção também possui o botão **Abrir wiki de espécies**. A pesquisa aceita até 128 caracteres e compara, sem diferenciar maiúsculas/minúsculas, trechos do nome canônico fornecido pelo Cobblemon e do identificador completo. Não consulta comandos externos nem interpreta formatação como ação. O botão de pesquisa mostra o comando no chat; esta primeira versão não possui campo de texto dentro da GUI.

## Navegar

A wiki usa inventário vanilla de quatro linhas (36 slots), com até 18 espécies por página e controles separados. Seletor e progresso das coleções mantêm suas três linhas. Clique numa espécie para abrir seus detalhes. Os controles permitem:

| Controle | Ação |
|---|---|
| Página anterior/próxima | Navegar mantendo pesquisa, filtro e ordenação |
| Filtro | Alternar Todas → Vistas → Obtidas → Não vistas → Shiny vistas |
| Ordem | Alternar nome canônico e identificador; IDs desempatam nomes iguais |
| Tipo | Abrir seletor paginado de tipos reais da coleção |
| Categoria (label) | Abrir seletor paginado de labels reais da coleção |
| Pesquisar | Fechar a GUI e mostrar o comando de pesquisa |
| Limpar pesquisa e filtros | Voltar à lista completa da coleção |
| Voltar à coleção | Abrir a página de progresso da mesma coleção |
| Fechar | Fechar o inventário |

O título informa página/total de páginas e quantidade de resultados. Uma lista vazia mostra uma mensagem explícita. Alterar filtro/ordem reinicia a paginação; voltar dos detalhes preserva a consulta.

## Informações e critérios

Lista e detalhes mostram nome canônico, ID, estado **vista**, **obtida** e **shiny vista**. Os detalhes mostram o total de formas elegíveis e seus tipos. **Ver formas** abre todas as formas elegíveis em páginas de 18 itens, cada uma com nome, tipos, labels e seus próprios estados oficiais. Não existe mais o corte em 32 linhas: a última forma continua acessível por paginação. A forma padrão é incluída mesmo quando o registro não fornece formas alternativas. Voltar aos detalhes e à lista preserva pesquisa, ordenação e filtros.

Os critérios JSON da coleção são aplicados antes da pesquisa e dos filtros. Em coleções filtradas por forma/tipo/label, e ao selecionar um tipo ou label adicional na wiki, somente as formas elegíveis entram na exibição e nos estados agregados: obter uma forma não elegível não registra outra forma. Nacional sem filtro preserva o conhecimento agregado oficial. Espécies válidas mas não implementadas não entram na lista, seguindo o denominador do progresso.

**Obtida não significa captura selvagem legítima. Shiny vista não comprova captura shiny.** O estado de captura shiny verificada permanece indisponível em todas as páginas; nenhum histórico é reconstruído. A wiki apenas lê os dados oficiais, sem conceder entradas de Pokédex.

## Tipos e categorias da consulta

Os seletores são gerados dos dados reais das formas aceitas pelos critérios da coleção. Não há lista fixa de tipos/espécies ou classificações inventadas. Categoria aqui significa uma **label canônica** informada pelo registro, como `legendary` ou `mythical` quando presentes; ainda não é o sistema completo de categorias nomeadas/condições avançadas/histórico.

Os seletores mostram até 18 opções por página e incluem **Sem filtro**. As opções vêm da coleção inteira, independentemente da pesquisa e do estado/segundo filtro atual; assim uma combinação sem resultado pode ser alterada. Não mostram opções exclusivas de formas excluídas pelos critérios da coleção. A pesquisa é preservada ao entrar/sair dos seletores, aplicar um filtro e trocar a ordenação.

Tipo + label + critérios JSON precisam combinar **na mesma forma**. Uma espécie com forma normal lendária e variante de água mítica não satisfaz a combinação água + lendária. Obter a forma normal não torna a variante de água vista/obtida. Sem filtro de forma/tipo/label, a consulta continua respeitando o agregado oficial da espécie.

Esses controles afetam somente a consulta da wiki, sem alterar o JSON, progresso, denominador da coleção ou placeholders. Resultados vazios são explícitos. Após reload, callbacks revalidam os critérios e a permissão; páginas de formas/opções são ajustadas ao tamanho atual. Uma opção antiga que deixou de existir pode produzir lista vazia até limpar o filtro, sem exibir dados excluídos. Nenhum registro de espécies é recarregado a quente.

## Aparecimento e limites

Nos detalhes, **Ver regras conhecidas de spawn** abre páginas de declarações do pool mundial carregado do Cobblemon. São mostrados IDs de bioma, tags `#namespace:tag`, dimensões e intervalos de ticks Minecraft que existam nas condições suportadas. Alternativas e exclusões ficam separadas por regra; uma lista longa continua nas páginas seguintes.

É um resumo **parcial por espécie**, não uma previsão de aparecimento. A forma declarada é preservada quando existe; aspectos/propriedades personalizadas não são convertidos em formas e a aplicação à forma selecionada não é inferida. Tags não são expandidas em biomas. Ticks não são convertidos em horas reais. Composições de condições são sinalizadas, sem avaliação. Pesos, clima, luz, altura, regras globais, outros tipos de spawn/spawners e extensões de terceiros não são interpretados integralmente.

**Nenhuma regra simples conhecida no pool consultado** significa que a consulta funcionou, mas não encontrou uma declaração suportada para a espécie. **Informação de aparecimento indisponível** significa que a fonte não pôde ser consultada. Nenhum dos resultados prova ausência universal de spawn natural.

O snapshot é preparado no servidor após iniciar e após o evento de conclusão bem-sucedida de reload de datapacks; falha deixa a fonte indisponível. O Dex não provoca reload do registro de espécies. Para trocar fixtures/espécies nos testes, reinicie o servidor. Alterações programáticas por outros mods fora desses eventos podem exigir reinício.

As mensagens da interface seguem o idioma global pt_br/en_us. Nomes de espécies, formas e tipos são os valores canônicos do registro nesta versão, não traduções personalizadas por cliente. Ícones são livros vanilla, sem modelo 3D nem novo mod cliente do Dex.

Toda navegação com dados reconsulta a definição/permissão atual. Revogação de acesso ou reload não permite usar um botão antigo para abrir espécie que deixou de ser elegível. O snapshot compartilhado com o progresso tem cache de até cinco segundos e invalidação pelos eventos oficiais já utilizados. Não há varredura global por tick, escrita de progresso ou consulta SQLite por página.

[Validação da alpha.6](validation/phase2-spawns.json): testes Java e protocolo em servidor de desenvolvimento. Aparência em cliente Minecraft real, carga, cobertura adicional de spawn, categorias avançadas e personalização completa dos layouts continuam pendentes. Esta é uma wiki inicial; não conclui toda a Fase 2.
