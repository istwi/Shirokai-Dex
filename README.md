# Shirokai Dex

Mod planejado para Minecraft 1.21.1, Fabric, Java 21 e Cobblemon 1.8.3, desenvolvido incrementalmente para o servidor Shirokai.

**Estado atual:** auditoria estática inicial e sistema de continuidade. Há um esqueleto Gradle, mas ainda não há inicializador Fabric, funcionalidades implementadas ou JAR instalável. Nenhuma integração é declarada funcional.

Acompanhe o que foi concluído e onde continuar na [checklist do prompt mestre](docs/MASTER_CHECKLIST.md). Todo Codex deve atualizá-la a cada entrega, marcando e riscando somente itens verificados.

Antes de trabalhar, leia [AGENTS.md](AGENTS.md), [estado do projeto](docs/PROJECT_STATUS.md) e [próximos passos](docs/NEXT_STEPS.md). O [prompt mestre](docs/PROJECT_SPECIFICATION.md) foi preservado integralmente. Consulte as [evidências das APIs](docs/API_COMPATIBILITY.md) antes de implementar integrações.

## Desenvolvimento

Java 21 é obrigatório. Gradle Wrapper 8.14.3 tem checksum de distribuição fixado. O build fixa as versões principais, mas a resolução completa e o build ainda não foram validados. O primeiro passo técnico é corrigir o acesso do Gradle ao proxy, preservando TLS e certificados, e executar `./gradlew --no-daemon help`.

Em ambientes onde o diretório pessoal não é gravável, defina `GRADLE_USER_HOME` para um cache gravável fora do checkout, por exemplo `/workspace/.cache/shirokai-gradle`. O Java/Gradle não usa automaticamente `HTTPS_PROXY`; configure o proxy a partir das variáveis herdadas sem salvar credenciais ou endereços específicos da sessão no repositório.

Não instale este checkout como um mod pronto. A Fase 1 precisa implementar e validar inicialização, configuração, SQLite/migrações, comando e menu inicial. O pipeline Java, os testes e a distribuição serão adicionados com essa implementação.

## Continuidade

GitHub é a fonte oficial entre sessões. Apenas um Codex trabalha por vez. Commits devem incluir código e documentos de progresso correspondentes. Antes de encerrar, valide, faça commit, tente push autorizado e confirme o resultado; se bloqueado, informe explicitamente o que permanece somente local. Não publique releases ou configurações de ambiente sem autorização específica.

Execute `python3 scripts/check_continuity.py` para verificar documentos, seções e links locais. O workflow de continuidade também exige atualizar estado e próximos passos em alterações técnicas. Ele não substitui os testes do mod nem garante que o texto corresponde ao código: essa conferência é obrigatória no protocolo do Codex.
