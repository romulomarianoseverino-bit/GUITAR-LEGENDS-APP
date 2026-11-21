# Configuração Firebase no GitHub Actions

O aplicativo Android usa o identificador de pacote `com.aistudio.applet.kpfcic`.

## Configuração necessária

1. No Firebase Console, crie ou selecione o projeto correto do Guitar Legends.
2. Registre um aplicativo Android com o identificador de pacote acima.
3. Baixe o arquivo `google-services.json` desse aplicativo Android.
4. No GitHub, abra **Settings > Secrets and variables > Actions** do repositório.
5. Crie um segredo de repositório chamado `GOOGLE_SERVICES_JSON_BASE64`.
6. Converta o conteúdo do arquivo JSON para Base64 localmente e salve o resultado nesse segredo. Não publique o JSON nem o conteúdo do segredo em issues, commits ou mensagens públicas.

## O que o fluxo faz

- Se o segredo existir, decodifica o JSON durante a execução, valida se ele contém o pacote Android esperado e grava temporariamente `app/google-services.json` no executor do GitHub Actions.
- Se o segredo não existir, ainda permite compilar um APK de depuração, mas informa que login Google/Firebase não estará configurado.
- O arquivo JSON não é incluído no commit nem no artefato como arquivo separado.

## Etapas ainda necessárias no Firebase

- Ativar Google como provedor em Authentication.
- Cadastrar as impressões SHA-1 e SHA-256 da chave de assinatura usada no APK distribuído.
- Conferir regras e índices do Firestore e validar o fluxo com o projeto correto.

**Importante:** o segredo de configuração não cria um projeto Firebase, não ativa o provedor Google e não configura regras do Firestore. Esses passos precisam ser concluídos no Console. O APK debug gerado pelo fluxo não deve ser tratado como versão de lançamento.
