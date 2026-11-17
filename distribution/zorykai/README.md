# Zorykai Dex — versão atual dev.7

[Baixar JAR dev.7](dev7/zorykai-dex-0.7.0-dev.7+mc1.21.1-cobblemon1.8.3.jar) · [Instruções](dev7/LEIA-ME.txt) · [Validação](dev7/VALIDACAO.json)

`/dex` sem argumentos executa `skiesguis open pokedex` como o próprio jogador. Requer SkiesGUIs com o menu `pokedex` configurado e as permissões do provedor. Subcomandos e placeholders preservados; apresentação Zorykai mantida no JAR. Build/verifyModJar e427 JUnit aprovados. Abertura real no SkiesGUIs ainda não validada nesta instância.

O código-fonte inclui o encaminhamento do comando e a versão dev.7. A marca Zorykai continua aplicada somente na cópia do JAR; o build direto das fontes mantém os textos originais Shirokai. Não instalar dois JARs do mesmo mod.

## Distribuição anterior

# Zorykai Dex 0.7.0-dev.6

JAR com alteração exclusivamente visual de Shirokai para Zorykai, baseado em `fix/latest-menu-command`, commit `32c4d3167ce57627f0c7cd3a04ac47fafa8e07b7`.

[Baixar JAR](zorykai-dex-0.7.0-dev.6+mc1.21.1-cobblemon1.8.3.jar) · [Instruções](LEIA-ME.txt) · [Validação](VALIDACAO.json)

O prefixo ✦ ZORYKAI usa branco/negrito; os degradês dos 11 títulos originais foram preservados. Placeholders, comandos, permissões, IDs, namespaces, pacotes, classes e instruções permanecem intactos. O código-fonte original não foi editado; reconstruí-lo produz o JAR original Shirokai. Esta distribuição contém o JAR editado e a auditoria das diferenças.

Build original: 427 JUnit aprovados. Artefato alterado: integridade ZIP, auditoria estrutural e componentes reais Minecraft aprovados. Servidor dedicado e cliente gráfico não executados nesta alteração. Configurações externas existentes podem sobrepor os padrões visuais incorporados.

Para verificar a integridade, execute nesta pasta:

```sh
sha256sum --check zorykai-dex-0.7.0-dev.6+mc1.21.1-cobblemon1.8.3.sha256
```
