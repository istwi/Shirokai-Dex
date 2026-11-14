# Recordes e rankings permanentes — local

`/dex top` abre os rankings de capturas; `/dex top <id> [page]` abre uma classificação. `/dex records` consulta seus melhores períodos DAY/WEEK/MONTH no statistics_timezone configurado. Não requer inscrição, não reinicia periodicamente e não concede prêmios.

Configuração opcional `rankings`: até64 objetos com `id`, `name`, `metric`, `key` e `permission`. Métricas TOTAL/SHINY/LEGENDARY/MYTHICAL não aceitam key; SPECIES exige ID namespaced e CATEGORY exige ID de categoria. IDs únicos, parser estrito. Padrão: captures/TOTAL, shiny/SHINY e legendary/LEGENDARY. Ausência de dados não inventa entradas; jogadores offline usam último nome persistido ou UUID.

Schema11 mantém score e reached_at, incrementados na transação da captura verificada; UNKNOWN e replay não pontuam. Ordem: score desc, data de atingimento asc, UUID asc. Migração copia totais/counters existentes e recupera datas somente quando há evidência completa. TOTAL pode usar recibos completos; demais métricas exigem todos os detalhes daquela contagem. Data desconhecida aparece N/D e ordena após datas conhecidas, depois por UUID; é uma limitação histórica explícita, não uma data fabricada. Nova captura registra o atingimento do novo total.

Cache de páginas no worker: até128 entradas/5s, invalidado por captura ou alteração de visibilidade. Paginação de10, dados imutáveis e permissão revalidada ao abrir/paginar. Menus usam o bloqueio de inventário existente. Nomes podem refletir o cache por até5s. Menus já abertos são snapshots e não apagam informações já vistas ao mudar privacidade.

`/dex settings rankings hidden|visible` persiste exclusão dos rankings por UUID. Não apaga pontuação. Privacidade detalhada por tipo de estatística, padrões JSON e perfis completos ainda serão integrados na Fase6.

Recordes usam buckets permanentes por jogador/fuso/período. A primeira consulta a um fuso agrega os recibos em memória no worker e grava buckets em lote; capturas seguintes atualizam apenas fusos já inicializados. Não há varredura por tick. Limpeza de detalhes não altera buckets nem recibos. Períodos empatados escolhem o início mais antigo. A cobertura depende dos recibos disponíveis; eventos anteriores à instalação ou previamente perdidos não são reconstruídos.

Validação:344 JUnit e7 checks de servidor Loom, [evidência](validation/phase4-rankings.json). Fixture sintética valida migração/UI/ACL/privacidade/recordes, não captura no jogo. Coleções/Pokédex têm fonte distinta e ainda não entram nesses rankings. Competições são um sistema separado.
