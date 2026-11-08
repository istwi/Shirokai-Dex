# Progresso Shiny por captura verificada — alpha.5

`/dex shiny` e `/dex shiny status` agora mostram quantidade/total e percentual de espécies shiny capturadas com origem comprovada. Receber por comando/troca/recompensa, ver um shiny ou ter registro de propriedade não cria prova. `SHINY_SEEN` continua consultando a evidência oficial de observação, separado de `SHINY_CAPTURED`.

## Contagem e persistência

Schema9 mantém captured_shiny_forms por UUID do jogador + espécie namespaced + forma normalizada. A projeção é escrita na mesma transação do evento/agregados, somente para captura verificada e shiny; replay não cria progresso. Primeira migração usa exclusivamente capture_events já persistidos com origin legítima e shiny=1. Não importa histórico anterior à instalação e não modifica a Pokédex oficial.

Uma espécie conta uma vez, ainda que existam várias ações/formas capturadas. Estatística de ações continua contando todas as capturas distintas. O denominador considera espécies implementadas atualmente; só conta forma atualmente existente e elegível pelos filtros da coleção/categorias. Espécie/forma removida não infla o percentual; reintrodução pode restaurar elegibilidade da prova persistente. Mudar regras não altera snapshots históricos de estatísticas.

A projeção independe dos detalhes de histórico: um teste remove os detalhes e comprova persistência do conjunto. Isso prepara retenção, mas nenhum comando de limpeza foi implementado neste bloco. Backup antes de migrar; não fazer downgrade para schema8/6.

## Carregamento e integrações

Leitura SQLite ocorre no executor do banco. A thread do servidor recebe somente conjunto imutável de IDs/nomes; registries e Pokédex são consultados na thread correta. Um jogador por tick pode solicitar sua primeira leitura, com intervalo de tentativa em falha. Captura confirmada agenda atualização da mesma projeção; login/reload carregam novamente. Desconexão impede publicação para sessão antiga. Sem snapshot confirmado fica N/D, distinto de zero conhecido. Falha posterior pode preservar último snapshot válido e subestimar progresso até recuperação.

Status, menus, placeholders e elegibilidade de recompensas reutilizam o cálculo e ACL existentes. Atualização invalida cache e observação de marcos; não entrega recompensa automaticamente. Permissão continua revalidada por coleção e no resgate. Wiki mantém seus filtros explícitos vistos/obtidos/shiny vistos, sem converter obtenção em captura.

## Validação e limites

326 JUnit: seis testes de cálculo (disponibilidade, visto/obtido, espécies repetidas/formas, filtros e mudanças no registro) e três de persistência/migração8→9/isolamento e independência de detalhes. [Capturas/pesca nativas e regressões](validation/phase4-shiny.json). As suítes comprovam aquisição administrativa sem progresso, captura real com progresso, repetição da mesma espécie e reinício. Runners antigos de porcentagem foram adaptados à fonte conhecida zero; não foram todos repetidos.

Outras varas/pools/modpacks, carga, falhas de disco e cliente gráfico continuam limitados pelos relatórios anteriores. Histórico paginado/recordes/rankings/retenção e restante da Fase4 seguem pendentes. Não declarar o mod completo nem Fase4 encerrada.
