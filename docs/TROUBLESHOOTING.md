# Diagnóstico

## Gradle sem acesso à rede

`UnknownHostException` no cloud pode indicar que a JVM não usou o proxy herdado. Use `scripts/gradle_cloud.py` e uma truststore fornecida pelo ambiente, sem desativar TLS. Defina cache gravável e JDK 21 completo; `java -version` sozinho não prova que `javac` está instalado.

Checksum da distribuição deve ser consultado no endpoint oficial Gradle e ter 64 caracteres hexadecimais. O erro inicial de transcrição foi corrigido; não trocar pelo hash de um download desconhecido nem remover a verificação.

Se o build passa, mas LuckPerms falha no `runServer` com `Unable to load dependency`/`UnknownHostException`, confira a JVM filha: os `-D` do Gradle não são herdados automaticamente. Use `--init-script scripts/server_proxy.init.gradle` junto ao helper cloud e à truststore, conforme [testes](TESTING.md). Esse ajuste foi validado no download real das dependências e no startup; não desabilite verificação TLS/checksums.

## Cobblemon falha em `net.minecraft.class_2960`

No ambiente Mojang mappings de desenvolvimento, o Loom precisa remapear os metadados Kotlin usados por reflexão. O build aplica `org.jetbrains.kotlin.jvm` 2.2.20 para habilitar o serviço de metadados do Loom 1.11.8, mesmo sendo um mod Java. Se vier de um cache anterior ao ajuste, invalide somente os artefatos gerados de remapeamento do Cobblemon e tente novamente; não modifique JAR oficial nem fontes.

## Showdown não encontra ICU

O runtime de desenvolvimento dedicado precisa de `minecraftServerLibraries 'com.ibm.icu:icu4j:71.1'`, como o próprio build Fabric do Cobblemon 1.8.3. Sem isso, pode falhar com `NoClassDefFoundError: com/ibm/icu/text/DateFormat`. O runtime distribuído do Cobblemon inclui essa biblioteca; não adicionar uma versão aleatória de ICU ao servidor.

## Dex indisponível

Procure o erro de inicialização do Dex. JSON inválido sem backup válido ou falha na migração deixa somente o Dex indisponível. Corrija a causa; não apague o banco para esconder o erro. Versões de banco futuras são recusadas sem downgrade.

## Reload não aparece

O comando administrativo requer LuckPerms e permissão positiva. OP não substitui essa autorização. O menu e `/dex status` continuam disponíveis sem o provedor opcional. Não conceda permissões administrativas gerais para contornar um teste.

Em permissões por dimensão, confira `lp user <uuid> info`: LuckPerms Fabric 5.4.140 reportou `world=overworld`, sem namespace. Comandos RCON do LuckPerms são assíncronos; uma resposta vazia não comprova aplicação, e `Another command is being executed` indica que o próximo comando não concluiu. Espacie as operações e valide o comportamento como jogador.

## Mensagens do próprio Cobblemon

No primeiro login de usuários novos, o Cobblemon auditado registrou mensagens `.old File ... corrupt or missing`; não havia dados históricos desses usuários e o Dex não escreveu esses arquivos. O startup também registra avisos de data fixers/mixins opcionais. Diagnostique cada mensagem pela origem; não classifique tudo como erro do Dex nem silencie falhas de inicialização reais.
