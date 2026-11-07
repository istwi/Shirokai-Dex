# Instalação — alpha experimental

Alvo: Minecraft 1.21.1, servidor dedicado Fabric Loader 0.17.2+, Java **21**, Fabric API 0.116.6+1.21.1, Cobblemon **1.8.3+1.21.1** e Fabric Language Kotlin 1.13.6+kotlin.2.2.20. Confira as versões exatas antes de instalar. Não use esta alpha no servidor de produção sem os testes de staging.

Gere o artefato com JDK 21 completo:

```sh
./gradlew --no-daemon build
```

Em nuvem com proxy herdado:

```sh
export JAVA_HOME=/caminho/do/jdk-21
python3 scripts/gradle_cloud.py --no-daemon build
```

O helper deriva HTTP/HTTPS proxy das variáveis existentes e usa cache gravável em `/workspace/.cache/shirokai-gradle` quando não há `GRADLE_USER_HOME` definido. Não salva credenciais nem altera configuração global. Caso o JDK local não use a CA do ambiente, use a truststore fornecida pelo sistema; na máquina validada foi necessário acrescentar `-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts`. Não desabilite TLS.

Para iniciar `runServer` na nuvem, acrescente `--init-script scripts/server_proxy.init.gradle`: ele repassa as propriedades de proxy/truststore ao processo Java filho. Veja o comando e os critérios de readiness em [testes](TESTING.md). O proxy do Gradle sozinho não basta para os downloads de dependências feitos pelo LuckPerms em runtime.

JDK local usado na validação: Temurin 21.0.13+9, tarball oficial `OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz`, obtido da release `jdk-21.0.13+9` do repositório `adoptium/temurin21-binaries`. SHA-256 verificado com o arquivo oficial: `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`. Não presumir que `/workspace/.tools/jdk-21.0.13+9` exista em outras máquinas.

Arquivo de mod: `build/libs/shirokai-dex-0.2.0-alpha.6+mc1.21.1-cobblemon1.8.3.jar`. O arquivo `-sources.jar` é código-fonte e **não deve ser instalado**. O JDBC SQLite é incluído no JAR; não instalar outro SQLite mod por conta desta dependência. LuckPerms é opcional na inicialização, mas obrigatório para o reload administrativo.

Instale o JAR principal na pasta `mods` de um servidor de testes com as dependências. Faça backup antes de iniciar. Este Dex não exige mod próprio no cliente; o Cobblemon e outros mods do servidor continuam tendo seus próprios requisitos de cliente.

O servidor de desenvolvimento Loom e o JAR distribuído fora do Loom foram validados. O servidor isolado de distribuição usou o perfil oficial `https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.17.2/fabric-loader-0.17.2.json`, Minecraft original e JARs de release das dependências, com checksums conferidos. Foi iniciado por `net.fabricmc.loader.impl.launch.knot.KnotServer` com o classpath desse perfil e `-Dfabric.gameJarPath=server.jar`, sem classes/fontes do projeto no classpath. O JAR do Dex ficou somente em `mods`; o smoke passou. Essa preparação alternativa foi usada porque o Fabric Installer não conseguiu consultar seus endpoints de metadata neste ambiente. Não houve alteração de allowlist.

Evidência e checksum do JAR estão em [validação da Fase 1](validation/phase1-foundation.json). Não há release estável. O workflow Java gera artifact identificado pelo commit se passar; confira sua execução no GitHub, sem presumir resultado.

A versão de desenvolvimento 0.2.0-alpha.1 adiciona [porcentagens e páginas de coleção](POKEDEX.md), com [validação própria](validation/phase2-percentages.json). A pré-release pública 0.1.0-alpha.1 permanece disponível separadamente e não contém essas novidades. Nenhum layout JSON completo ou captura shiny verificável foi adicionado nesta etapa.

A alpha de desenvolvimento 0.2.0-alpha.2 acrescenta [coleções JSON](COLLECTIONS.md), validação semântica, permissões e paginação; [evidência atual](validation/phase2-collections.json). LuckPerms Fabric 5.4.140 foi testado com grant/revoke global e reload; continua opcional para coleções públicas.

A alpha.3 de desenvolvimento acrescenta a [wiki de espécies](WIKI.md), com protocolo validado no servidor de desenvolvimento e [evidência própria](validation/phase2-wiki.json). O JAR desta versão ainda não foi novamente validado instalado fora do Loom; as evidências antigas dessa modalidade pertencem às alphas anteriores. Nenhuma release nova foi publicada.

Alpha.4: [placeholders básicos](PLACEHOLDERS.md), mediante instalação separada da Text Placeholder API 2.4.2+1.21. Essa dependência é opcional e não está aninhada no JAR do Dex; o build obtém o artefato oficial pelo GitHub. A pré-release publicada alpha.3 permanece separada e imutável. Nenhuma nova release é publicada automaticamente pelo build.

Alpha.5: seletores dinâmicos de tipo/label e navegação de todas as formas da wiki, em inventário vanilla de quatro linhas. Os critérios da coleção e os estados por forma continuam sendo revalidados, sem alteração de dependências ou do formato de configuração. [Guia](WIKI.md).

## Correção alpha.5: escolher o arquivo instalável

Instale somente `shirokai-dex-0.3.0-alpha.5+mc1.21.1-cobblemon1.8.3.jar` em `mods`, substituindo os arquivos antigos do Shirokai Dex. Não altere os demais mods. `-sources.jar` é material para desenvolvedores, não um mod. Na alpha.4 ele continha descritor Fabric sem a classe compilada, causando ClassNotFoundException quando instalado; o problema foi reproduzido em servidor real. Alpha.5 remove esse descritor das fontes e o artefato de instalação do Actions contém apenas o binário e SHA256SUMS.
