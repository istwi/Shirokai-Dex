# Changelog

## Publicação v0.4.0-alpha.1

- Pré-release no commit78dfe72 publicada pelo workflow42432155833 após 304 JUnit, upload/download e checksum. Quatro anexos conferidos independentemente; JAR coincide com o binário nativo testado. Main/releases anteriores/ambiente preservados. [Evidência](docs/validation/release-captures-alpha1.json).

## 0.4.0-alpha.1 — captura/estatísticas iniciais

- Correlação nativa pré-hit/captura com origem PlayerSpawner/pool mundial; sem prova fica UNKNOWN. Eventos não são reconstruídos da Pokédex ou de concessões por comando.
- Schema6, dedup transacional por Pokébola/Pokémon, snapshots/versionamento de classificação e contadores permanentes/espécies/categorias; CLI próprio e períodos com fuso configurável.
- 304 JUnit e 18 checks de captura/recuperação/reinícios. Captura positiva foi nativa, replay não duplicou, criatura administrativa não pontuou e causa perdida após reinício não foi presumida legítima.
- Fase 4 parcial: proveniência persistente, outros fluxos, histórico/revisão/recordes/Shiny% ainda pendentes. Sem release/main/ambiente publicados.

## 0.3.0-alpha.11 — recuperação segura e executor de testes

- Retomada administrativa com plano/ID preservados, motivo, prévia/segunda confirmação e auditoria. Só aceita operações sem qualquer etapa; INTENT/prefixo/fechadas/alteradas continuam bloqueados. Permissões atuais de operador/destinatário e contexto revalidados.
- Executor de servidor empacotado reutiliza assets auditados, cria mundo novo, semeia fixtures somente parado, testa/reinicia/limpa RCON. 290 JUnit e 18 checks reais de recuperação/COMMAND com reinícios.
- Próximo essencial: origem de captura e estatísticas. Sem nova release/main/ambiente publicados.

## 0.3.0-alpha.10 — COMMAND local

- Política opt-in de templates/contextos exatos, parâmetros restritos ao destinatário e uma ação isolada por marco. PLAYER preserva permissões; CONSOLE exige autorização explícita no arquivo.
- Comando preparado congelado no plano antes da intenção, parsing/precondições revalidados e aviso na prévia/confirmação. Dispatch sempre exige NEEDS_REVIEW; retorno não comprova persistência externa. Sem retry automático.
- 282 JUnit e nove checks de servidor/reinício, incluindo erro, revogação, rollback e resolução supervisionada sem replay. Recibos automáticos e planos mistos pendentes.
- Envio autorizado na branch de desenvolvimento; sem release/publicação de ambiente.

## 0.3.0-alpha.9 — validação local de checkpoint Crates

- Checkpoint Crates0.2.1 aguarda future e readback antes do ACK de itens; fallback vanilla somente na ausência do provedor. Provedor carregado unhealthy/indisponível bloqueia efeito.
- Runner em quatro modos: chave real via SAVED_ITEM, autenticidade nativa, atraso controlado, itens, falha de escrita, revisão/anti-repetição, fallback e reinícios. Sem CRATE_KEY dedicado nem alteração de outros mods.
- 274 JUnit e evidências de servidor empacotado; regressões nos relatórios. Probe Pokémon agora tem task de remapeamento separada com input dev distinto da saída.
- Envio do bloco validado autorizado somente na branch handoff/crates-checkpoint. Nenhuma nova release/publicação de ambiente.

## 0.3.0-alpha.4 — trabalho local não enviado

- POKEMON com atributos reais validados, UUID/payload congelados, espaço de equipe/PC e confirmação de gravação NBT; falhas bloqueadas para revisão.
- Primeira passagem visual: central /dex, resumo do treinador, paletas/gradiente/slots JSON, grade de recompensas e retorno /menu como jogador. Sistemas indisponíveis não simulam funcionalidades.
- 214 JUnit + 30 checks de servidor nos dois blocos. [Pokémon](docs/validation/phase3-pokemon.json), [layout](docs/validation/layout-home.json).
- Prioridade seguinte: padronizar demais telas antes de outros sistemas. Sem commit/push/release/ambiente publicado.

## 0.3.0-alpha.2 — trabalho local não enviado

- ITEM/CUSTOM_ITEM/SAVED_ITEM, componentes oficiais, save/list/give/delete com confirmação, checksum e auditoria transacional.
- Modelos são resolvidos em snapshots; edição/exclusão não altera planos e backup recupera componentes sem depender do modelo removido.
- Revisão paginada e identificação de operações; permissões atuais verificadas nos callbacks.
- 179 JUnit + 10 checks de servidor aprovados. [Guia](docs/SAVED_ITEMS.md), [evidência](docs/validation/phase3-saved-items.json). Sem commit/push/publicação; tipos avançados restantes pendentes.

## Trabalho local não enviado — recompensas de itens

- Marcos JSON por quantidade/percentual, prévia paginada e confirmação de resgate.
- Espaço integral no inventário, journal persistido antes dos efeitos, etapas/auditoria e bloqueio de duplicação.
- Interrupções bloqueadas para revisão; comandos administrativos com permissão própria, confirmação literal e evidência/justificativa. Sem reenvio automático.
- [Guia](docs/REWARDS.md), [testes](docs/validation/phase3-items.json). Desbloqueio permanente implementado na migração 4; tipos avançados pendentes. Sem commit/push/publicação.

## Trabalho local não enviado — versões de categorias

- JSON canônico/SHA-256 e migração SQLite 2 para preservar regras anteriores; leitura verifica hash.
- Startup/reload rejeitam publicação se a persistência falhar. Snapshots representam regras observadas, sem inventar capturas/ativações históricas.
- Sem commit/push/publicação por pedido do usuário. [Validação](docs/validation/phase2-category-versions.json).

## 0.2.0-alpha.7 — categorias nomeadas (desenvolvimento)

- Categorias JSON reutilizáveis com critérios reais e composição all/any/none, associadas a coleções e wiki na mesma forma; espécies contadas uma vez.
- Validação de ciclos, referências, duplicados e dados do registro; reload/backup continuam transacionais. Permissões por coleção e configuração antiga preservadas.
- Histórico/recálculo e versão persistida de regras continuam pendentes. [Guia](docs/CATEGORIES.md), [validação](docs/validation/phase2-categories.json). Sem publicação de release ou ambiente.

## 0.2.0-alpha.6 — declarações de spawn na wiki (desenvolvimento)

- Resumo paginado e somente de leitura do pool mundial: IDs/tags de bioma, dimensões e intervalos de ticks; alternativas e exclusões separadas. IDs de espécies preservam namespaces.
- Forma declarada preservada sem inferir aplicação por aspectos; condições compostas sinalizadas, sem avaliação. Fonte indisponível distinta de nenhuma regra simples conhecida, sem prometer spawn natural.
- Snapshot no lifecycle/reload, com permissões/critérios atuais em cada navegação. 102 JUnit e 17 checks de protocolo em servidor dev, incluindo remoção da fixture entre reinícios e regressão da wiki.
- Cobertura adicional, cliente visual/carga e instalação desta alpha fora do Loom pendentes. Nenhuma nova release ou configuração de ambiente publicada.

## 0.2.0-alpha.5 — filtros e formas da wiki (desenvolvimento)

- Seletores paginados de tipos/labels reais da coleção, combinados na mesma forma com critérios JSON e filtros de estado. Pesquisa e ordenação são preservadas.
- Todas as formas acessíveis em páginas de 18, sem corte de 32 linhas; estados, tipos e labels por forma, com retorno aos detalhes/lista. Wiki usa quatro linhas; seletor/progresso permanecem com três.
- Permissões e critérios atuais revalidados em seletores e páginas de formas; reload ajusta páginas e impede dados obsoletos. Inventário/progresso oficial permanecem somente de leitura.
- Oito novos testes Java e fixture com 40 variantes para validar mais de 32 formas em servidor real. Spawn, cliente visual, categorias avançadas e carga continuam pendentes. Nenhuma nova release ou configuração de ambiente publicada.

## 0.2.0-alpha.4 — placeholders básicos (desenvolvimento)

- Integração opcional com Text Placeholder API 2.4.2+1.21: percent, registered, seen, total e remaining por coleção; prévia `/dex <id> placeholder <métrica>`.
- Expansão lê snapshots preparados fora do handler, sem consulta SQLite, com permissões atuais, revisão de configuração e N/D explícito para indisponível/contextos inadequados.
- Registro não substitui provedores existentes; limpeza preserva handlers substituídos por outro mod. Alias legado não registrado; TAB ainda pendente.
- 87 JUnit, oito checks de integração, cinco de probe real e 13 de fundação sem APIs opcionais, mais limpeza real no shutdown. Dependência e probe não são incluídos no JAR distribuído.
- Nenhuma nova release nem configuração de ambiente publicada; alpha.3 permanece imutável.

## 0.2.0-alpha.3 — wiki inicial (pré-release)

- Wiki por coleção via botão e `/dex <id> wiki [pesquisa]`, com 18 espécies por página, ordenação por nome/ID e filtros vistas/obtidas/não vistas/shiny vistas.
- Detalhes com nomes canônicos, IDs, formas/tipos e estado oficial; spawn e captura shiny verificada explicitamente indisponíveis.
- Reutiliza critérios/permissões e snapshot do progresso; ações antigas revalidam acesso e configuração após reload.
- Nove testes Java novos (78 no total), integração da wiki e regressão das coleções em servidor real; limites documentados em `docs/WIKI.md`.
- Pré-release v0.2.0-alpha.3 publicada por autorização do usuário: 78 testes no CI, JAR/fontes/checksums/proveniência/relatório anexados e downloads verificados. Sem publicação de ambiente.

## Não lançado — validação complementar da alpha.2

- Runner/fixture de contexto por dimensão: LuckPerms `world=overworld`, reload autorizado/negado, retorno e revogação com menu aberto, sem OP.
- Pacotes de descarte, double-click, drag esquerdo/direito, clone criativo e clique externo com cursor forjado; contagens de inventário e progresso oficial preservados.
- Init script opt-in para repassar proxy/truststore do Gradle à JVM de `runServer`, corrigindo download de dependências LuckPerms no fluxo cloud.
- Build/69 JUnit e 8 checks de protocolo aprovados, estes repetidos com novo perfil em servidor dev real. Sem mudança funcional no mod, versão de dependências ou publicação de release/ambiente. Cliente visual e carga continuam pendentes.

## 0.2.0-alpha.2 — coleções JSON

- Metadados, ícones, modos OWNED/SEEN/SHINY_SEEN e filtros combinados por coleção; SHINY_CAPTURED permanece indisponível sem evidência.
- Validação semântica com snapshot real do registro e aplicação/backup atômicos; compatibilidade com config antigo e recuperação validadas.
- Comandos dinâmicos, seletor paginado e permissões revalidadas inclusive em GUI antiga.
- 69 JUnit e integração com LuckPerms 5.4.140: 11 regressões, 9 checks de coleções e 2 de reinício aprovados.
- Trabalho salvo e pausado por solicitação do usuário; sem nova release.


## 0.2.0-alpha.1 — porcentagens iniciais (desenvolvimento)

- Nacional e Lendária com quantidade/total e % no menu/status; duas casas sem falso 100%, total vazio N/D.
- Seletor e páginas das três coleções; Shiny explicitamente N/D até evidência de captura confiável.
- Cache por UUID invalidado por evento oficial e lifecycle, sem polling global por tick.
- Classificação por formas reais, incluindo forma padrão quando a lista de alternativas é vazia; nacional preserva agregado oficial.
- 32 JUnit, regressão de 11 checks e progresso de 6 checks aprovados; datapack namespaced, remoção entre reinícios e JAR fora do Loom testados.
- Fase 2 ainda parcial: coleções JSON, wiki, placeholders e captura shiny histórica pendentes. Release pública anterior não foi alterada.


## 0.1.0-alpha.1 — fundação experimental

- Publicada a pré-release `v0.1.0-alpha.1`, alvo `c11b366`, por workflow aprovado com 20 testes e 5 anexos. Downloads, checksums e proveniência conferidos; upload direto recusado foi resolvido pelo Actions.

- Implementados lifecycle Fabric, configuração validada com backup/recuperação e SQLite com migração transacional em executor dedicado.
- Adicionados `/dex`, `/dex status`, menu inicial somente de leitura e consulta da Pokédex oficial/registro dinâmico Cobblemon.
- Reload administrativo protegido por API LuckPerms opcional; ausência nega ação sem impedir startup.
- Adicionados mensagens pt_br/en_us, helper de proxy/cache, workflow Java 21 e documentação da fundação.
- Resolvidos requisitos do dev runtime: JDK completo, metadata Kotlin remapeada e ICU no servidor dedicado.
- Validação: 20 casos JUnit sem falhas e 10 checks de protocolo em servidor real, repetidos após reinício/recuperação e com JAR instalado fora do Loom. Evidência em `docs/validation/phase1-foundation.json`.
- Ainda sem coleções completas, recompensas, capturas/competições ou release estável. Testes positivos de LuckPerms, cliente visual e datapacks adicionais continuam pendentes.

## Não lançado — preparação inicial

- Adicionada checklist permanente das fases do prompt mestre, vinculada a AGENTS, README e verificação de continuidade.
- Corrigido checksum da distribuição Gradle 8.14.3 após comparação com o endpoint oficial; download por proxy funcionou, mas execução e build continuam pendentes.

- Preservado o prompt mestre em `docs/PROJECT_SPECIFICATION.md`.
- Documentada auditoria estática das APIs reais e limitações, incluindo progresso shiny e entrega de chaves do Shirokai Crates.
- Adicionado esqueleto Gradle 8.14.3/Loom 1.11.8, com alvo Java 21, Minecraft 1.21.1 e Cobblemon 1.8.3. Ainda sem build validado ou mod inicializável.
- Preparado protocolo de continuidade entre instâncias Codex, com estado e próxima tarefa documentados.
- Adicionados checker documental e workflow de continuidade; validado localmente inclusive com falhas simuladas de documentos/links. Pipeline Java permanece pendente.

Sem release, sem JAR e sem funcionalidades entregues até esta etapa.

## Local — segunda passagem de layout

- Estilo comum para submenus, barra de progresso exata, resumo e confirmação destacada.
- Grade/prévia exibem estado persistido do resgate; operações encerradas ou incertas não oferecem confirmação.
- Consulta assíncrona protegida contra telas/configurações/permissões desatualizadas; componentes dos prêmios preservados.
- Sem publicação. Repetição em servidor e cliente pendente.

## 0.3.0-alpha.5 — correção da distribuição

- Impede que fontes Java sejam descobertas como mod Fabric: remove descritor do sources.jar.
- Separa artefato instalável no Actions e inclui SHA256SUMS; não distribui fontes no mesmo ZIP.
- Verifica versão, entrypoint compilado Java 21, ModInitializer/onInitialize, namespace remapeado e dependências no JAR final antes da distribuição.
- Reproduzida ClassNotFoundException ao instalar fontes alpha.4; o usuário confirmou ter instalado esse arquivo. Sem alterações em outros mods ou sistemas de jogo.

## Local — 0.3.0-alpha.6

- Navegação JSON e rodapé opcionais em 11 telas, com movimentação conjunta de ícones/callbacks e suporte a trocas de posições.
- Validação estrita de conflitos, limites, duplicatas e proteção da área de prêmios; compatibilidade com configurações antigas.
- Fixture e testes de protocolo para submenus/estados reais, preservação de componentes, confirmação e reload.
- Sem commit, push ou publicação desta revisão.

## Local — 0.3.0-alpha.7

- Tamanho de 3/4 até 6 linhas, título com dados reais, paleta e aparência de botões por tela.
- Ícones registrados, nomes que preservam ação/estado e lore adicional, sem alterar payloads nem habilitar controles ausentes.
- Validação estrita de templates/limites/registro e reload transacional, com configurações antigas preservadas.
- Runners de recompensas adaptados ao botão oculto após operação encerrada; verificações anteriores de inventário/stores preservadas.
- Mudanças alpha.6/alpha.7 permanecem locais, sem publicação.

## Local — 0.3.0-alpha.8

- Recompensas CURRENCY isoladas ou combinadas com itens/Pokémon, valores BigDecimal e moeda exata registrada, sem conversão/fallback.
- Integração opcional Impactor5.3.5 com JSON nativo auditado: valida conta/precisão/resultado, aguarda save e confirma arquivo antes do ack.
- Detecta erro de persistência que o provedor oculta; incerteza fica em NEEDS_REVIEW, sem retry automático.
- Prévia/segunda confirmação monetárias, planos históricos compatíveis e limites estritos.
- Dependência API compileOnly 5.3.5, sem empacotar/instalar Impactor ou alterar suas configurações no servidor do usuário.
- Alterações continuam locais; nenhuma publicação.

## Origem do WIP alpha.9

Passagem `6b97093` continha coordenação/probe compilados, sem validação funcional. Retomada atual descrita no topo.
