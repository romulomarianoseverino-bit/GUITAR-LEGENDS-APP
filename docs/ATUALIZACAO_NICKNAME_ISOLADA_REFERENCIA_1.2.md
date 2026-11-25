# Linha experimental APP-2 — baseline

## Origem preservada
- Repositório: GUITAR-LEGENDS-APP-2
- Commit de origem: 5f09853a20a75b1e5b363ce7b9a5109be54f49ab
- Branch experimental: exp/app2-baseline-apk-2026-11-24
- A branch foi criada a partir da main do APP-2, sem alterar a main.
- A referência funcional 1.2 continua sendo a compilação nº 37 do repositório anterior; este baseline não é declarado equivalente a ela.

## Preparação mínima para o primeiro APK
- Inclui a configuração Firebase Android do projeto `guitar-legends-d7708`, cujo pacote Android é `com.aistudio.applet.kpfcic`.
- Preserva o identificador de banco Firestore já configurado no APP-2.
- O fluxo experimental executa os testes automatizados com Auth e Firestore Emulator e tenta gerar um APK assinado com a chave permanente.
- O fluxo verifica o certificado SHA-256 do APK e interrompe a publicação do artefato se a assinatura não corresponder à chave usada pela referência 1.2.

## Limites desta etapa
- APK experimental, não release.
- A compilação não comprova login real, playlists, sincronização nem multiplayer em aparelhos.
- A configuração OAuth precisa corresponder ao certificado permanente; a validação manual em aparelho continua obrigatória.
- Nenhum recurso de gameplay foi migrado nesta etapa.
- Nenhuma alteração foi feita na main do APP-2 ou no repositório anterior.
