# Compilação Android

A compilação de verificação do aplicativo é executada pelo GitHub Actions por meio do fluxo `.github/workflows/android-apk.yml`.

O fluxo executa os testes unitários e gera uma versão APK de depuração. Quando a execução termina com sucesso, o arquivo fica disponível na seção **Actions**, dentro dos artefatos da execução correspondente.

Este APK é destinado a testes. Uma versão de distribuição/atualização exige assinatura de lançamento consistente com a chave já utilizada para instalar versões anteriores.
