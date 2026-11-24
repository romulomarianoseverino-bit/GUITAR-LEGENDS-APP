# Atualização isolada de nickname — referência Guitar Legends 1.2

## Base protegida
- Commit de origem: a3077637f7870a0c2c9c655ff9414e3cd57842ad
- APK de referência: release referencia-apk-2026-11-24
- Esta branch foi criada diretamente do commit da referência, sem mesclar a main ou branches divergentes.

## Escopo único
Corrigir o salvamento/reenvio do nickname quando o mesmo usuário já possui a reserva no Firestore.

## Diagnóstico do código
As regras Firestore da referência tratam a reserva em nicknames como criação, não como atualização livre. O fluxo anterior executava tx.set mesmo quando o documento de reserva já existia. Ao salvar novamente o próprio nickname, isso podia ser interpretado como atualização e ser negado pelas regras.

## Alteração aplicada
- Ler a reserva antiga antes de qualquer gravação na transação.
- Criar a reserva nova somente quando ainda não existir.
- Não sobrescrever uma reserva já pertencente ao mesmo usuário.
- Excluir a reserva anterior somente quando ela existir e pertencer ao UID autenticado.
- Manter a escrita do documento do usuário e a mesclagem de progresso.

## Teste novo
claimingTheSameNicknameAgain_doesNotAttemptToUpdateImmutableReservation:
1. cria uma conta e reserva um nickname;
2. envia novamente o mesmo nickname;
3. confirma sucesso e verifica que a reserva continua vinculada ao mesmo UID.

## Validação independente
Workflow: .github/workflows/validate-nickname-v1.2.yml
Executa apenas AccountCloudRepositoryRuleTest com Firebase Auth e Firestore Emulator.

## Fora do escopo
- Não altera autenticação/login.
- Não altera multiplayer, salas, interface, pistas, pontuação, biblioteca ou playlists.
- Não altera firestore.rules.
- Não gera nem publica APK.
- Não integra esta branch à main.

## Critérios de aceite
- Workflow específico concluído com sucesso.
- Testes existentes de conta continuam passando.
- Teste de reaplicar o mesmo nickname passa.
- Revisão confirma que nenhuma outra funcionalidade foi alterada.
- Depois disso, validação de produção Firestore e do aplicativo em aparelho real continua necessária antes de distribuir atualização.
