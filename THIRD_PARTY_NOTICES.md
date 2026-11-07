# Componentes de terceiros

Os scripts `gradlew` e `gradlew.bat` e o bootstrap `gradle/wrapper/gradle-wrapper.jar` pertencem ao Gradle e são distribuídos sob Apache License 2.0. Os scripts preservam o aviso de licença em seus cabeçalhos. Consulte [a licença oficial](https://github.com/gradle/gradle/blob/v8.14.3/LICENSE).

O JAR do Wrapper foi obtido do repositório oficial Gradle, tag `v8.14.3`; SHA-256 verificado com `https://services.gradle.org/distributions/gradle-8.14.3-wrapper.jar.sha256`:

`7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`

A distribuição Gradle 8.14.3 tem checksum fixado em `gradle-wrapper.properties`; download, verificação e execução foram concluídos na validação da fundação. O checksum inicialmente transcrito foi corrigido com nova consulta ao endpoint oficial, sem desabilitar verificação.

O JAR do mod inclui `org.xerial:sqlite-jdbc:3.47.1.0` como dependência aninhada, preservando os arquivos de licença do artefato upstream. Demais mods e suas bibliotecas são dependências externas; consulte suas próprias licenças. Código das integrações foi consultado para auditoria, sem ser incorporado ao projeto.

As fontes das integrações foram consultadas para auditoria fora do checkout; não foram incorporadas ao projeto. O documento de compatibilidade contém as revisões utilizadas. A licença do código próprio do projeto ainda não foi escolhida; não presumir autorização de redistribuição geral.
