# Estado do projeto

Atualizado em 2026-11-07. Repositório `istwi/Shirokai-Dex`, branch `main`; apenas um Codex por vez.

## Fase atual

Primeira entrega da **fundação experimental da Fase 1**, validada no escopo abaixo. Auditoria da Fase 0 é incremental: consulta básica comprovada; captura, criação/entrega de Pokémon e economia não estão funcionalmente verificadas. Não é mod completo nem release estável.

## Concluído

- Build Gradle 8.14.3/Loom 1.11.8 com checksum oficial, helper de proxy/cache e JDK 21 completo verificado. Plugin Kotlin 2.2.20 habilita metadata remapping; ICU 71.1 compõe o runtime dev dedicado.
- Inicializador Fabric com lifecycle/readiness/logs e falhas isoladas do Dex; ausência de LuckPerms não impede iniciar.
- Configuração imutável estrita, reload assíncrono, backup validado atômico e recuperação sem sobrescrever JSON inválido.
- SQLite em worker dedicado; migração transacional, WAL/foreign keys, cadastro por UUID e fechamento.
- `/dex`, `/dex status`, menu inicial de 27 slots somente de leitura e consulta oficial Cobblemon. Espécies enumeradas dinamicamente; nenhum registro oficial alterado.
- `/dex admin reload` protegido por LuckPerms sem fallback OP/console; negação sem provedor comprovada, permissão positiva pendente.
- Mensagens em recursos separados pt_br/en_us, com idioma global configurável nesta fase.
- Workflow Java 21 com build/testes e upload de JAR/sources/JUnit condicionado ao sucesso. Checklist e continuidade mantidas.

## Em desenvolvimento

- Testes complementares: LuckPerms positivo/contextos e interações/aparência em cliente Minecraft real.
- Preparação incremental da Fase 2, respeitando limitações reais de shiny e de espécies/datapacks.

## Pendente

- Fases 2–8: Pokédex completas, categorias/datapacks/wiki, recompensas, capturas/estatísticas, rankings, competições, conquistas/perfis e integrações avançadas.
- Permissão positiva e reload administrativo de ponta a ponta com LuckPerms instalado, demais gestos de menu e testes de carga.
- Layouts JSON completos e documentação das funcionalidades ainda não implementadas.
- Licença do código próprio e release estável. Nenhuma configuração de ambiente foi alterada/publicada nesta entrega.

## Testes e verificações executados

Evidência persistida: [relatório da fundação](validation/phase1-foundation.json). Testes em `src/test` e `tests/integration`.

| Verificação | Resultado |
|---|---|
| Gradle help/build com JDK 21, proxy e truststore | Aprovados com checksums ativos |
| JUnit configuração | 16 casos aprovados; 0 falhas/erros/skips |
| JUnit SQLite | 4 casos aprovados; inclui 100 atualizações para um UUID, rollback e reabertura |
| Fabric/Cobblemon dev | Inicializou e Showdown iniciou; 1025 espécies carregadas, 905 implementadas no resumo |
| Cliente de protocolo real | 10 checks: login, consulta, menu, quatro modos de clique, fechamento, não mutação/sem ícones indevidos e negação administrativa |
| Reinício com JSON inválido | Backup recuperado, original preservado, UUIDs/primeiro acesso intactos, uma migração e integrity_check=ok; smoke aprovado novamente |
| JAR instalado fora do Loom | Servidor isolado com releases originais e perfil oficial Maven; mesmos 10 checks aprovados, SQLite incluído carregou |
| GitHub Actions | Workflow preparado; conferir execução do commit antes de afirmar aprovação |

Servidores locais eram offline e limitados a loopback; não validam autenticação online ou capacidade multiplayer. Cliente de protocolo ignora pacotes customizados de metadados/receitas/advancements/árvore de comandos não usados nas assertions; não substitui cliente visual.

## Erros e limitações conhecidos

- Falhas iniciais resolvidas: cache não gravável, JVM sem proxy, checksum transcrito com 66 caracteres, runtime sem compilador, Kotlin metadata sem remapeamento e ICU ausente no dev server. Consulte [diagnóstico](TROUBLESHOOTING.md).
- Matcher de erro vanilla do smoke foi corrigido após observar `Incorrect argument for command`; a autorização não foi alterada para fazer o teste passar.
- Conexão prematura ao servidor empacotado recebeu ECONNREFUSED; execução aprovada ocorreu após readiness `Done`.
- Fabric Installer não concluiu consulta de metadata. Usado perfil estático oficial Maven e artefatos verificados para montar o servidor isolado, sem alterar allowlist/TLS.
- Cobblemon emitiu avisos de mixins opcionais/data fixers e arquivos `.old` de usuários novos, sem histórico prévio. Dex não escreve esses arquivos.
- Shiny visto + espécie obtida não comprovam captura shiny. Registros de espécies Cobblemon não são seguros para hot reload.
- Integração Crates por console não é suportada no contrato auditado. Nenhuma recompensa foi entregue/implementada.

## Última alteração relevante e sincronização

Base anterior enviada/confirmada: `8c1e7f365be918b866ac137c020e2337db14e721` (checklist/checksum). Esta entrega é identificada pelo assunto `feat: implement and validate Fabric foundation alpha`; inclui código, testes, documentos e evidência do JAR. Obter o SHA com Git, sem atualização circular do hash deste documento.

Entrega funcional enviada e confirmada no remoto: `9cab47d7d4dcd6c95793cc57e4b1b60d87b49b45`, com árvore local limpa. Workflows desse commit iniciaram; aprovação remota/artifact ainda dependem do resultado observado no GitHub. Atualização documental posterior apenas registra essa referência e corrige os notices; a próxima sessão deve conferir também esse commit documental e quaisquer posteriores.

## Publicação alpha solicitada

O usuário autorizou publicar a alpha. Builds e continuidade dos commits 9cab47d/c11b366 passaram no GitHub Actions. Criado rascunho v0.1.0-alpha.1 com alvo c11b366; upload direto recebeu HTTP 401 no host de anexos, embora API/Git funcionem. Workflow manual Publish foundation alpha foi preparado para compilar esse alvo exato, exigir 20 testes aprovados, anexar JAR/fontes/checksums/proveniência e só então publicar como pré-release. Não necessita chave pessoal. Conferir resultado antes de declarar publicado.
