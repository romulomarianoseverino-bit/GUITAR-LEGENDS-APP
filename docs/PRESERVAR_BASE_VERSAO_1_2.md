# Preservação obrigatória da versão 1.2 — Guitar Legends

## Base de trabalho
- Repositório: `romulomarianoseverino-bit/GUITAR-LEGENDS-APP`
- Base: branch `main`, commit `a93f6c8f994cf0456d0be25efe6aa9deb90ce967`.
- A configuração Android da base declara `versionName = "1.2"` e `versionCode = 2`.
- Esta branch foi criada diretamente da `main`; não usar como base as branches antigas de preparação/integracão que divergem da versão atual.

## Regra principal
A versão 1.2 é a referência funcional e visual a preservar. As correções devem ser incrementais, sem reconstruir telas, trocar o tema, regredir componentes para botões antigos, remover funções existentes ou substituir o fluxo atual de autenticação.

## Interface
- Preservar o padrão visual atual dos botões, cartões, bordas, cores, espaçamentos e navegação.
- Não substituir os componentes atuais por botões genéricos/antigos do Material.
- Antes de alterar qualquer tela, comparar com a implementação da `main` e limitar o diff ao problema específico.
- Uma compilação não é aprovada se apresentar regressão visual evidente ou remover ação existente.

## Login e conta
- Preservar o fluxo de autenticação já existente na versão 1.2.
- Não introduzir login obrigatório, bloqueio na tela inicial ou troca de provedor/configuração sem diagnóstico.
- Validar Firebase Auth, Credential Manager, configuração do aplicativo e leitura do perfil Firestore ponta a ponta.
- Em falha de rede, manter o modo visitante acessível e não apagar o progresso local.

## Moedas e estrelas
- A barra superior atual exibe moedas e estrelas de carreira por meio de `TopStatusBar(coins, stars)`.
- `PlayerStats` e `UserAccountCloud` na base atual têm valores iniciais de referência: 450 moedas e 6 estrelas desbloqueadas.
- O indicador de estrelas corresponde ao total de estrelas de carreira (`unlockedStars`), não a uma coleção de seis botões independentes.
- Não zerar moedas, estrelas, recordes, desbloqueios, cosméticos, personagem ou configurações durante login, sincronização, migração ou atualização.
- Nunca substituir um perfil existente por valores padrão quando a consulta remota falhar ou vier incompleta.
- Resolver conflitos de sincronização de forma não destrutiva e com testes para impedir regressão de saldo/progresso.

## Persistência e reinstalação
- O progresso visitante local é gravado em `filesDir/local_player_progress.json`; dados somente locais podem ser apagados pelo Android quando o app é desinstalado.
- Para sobreviver à desinstalação, o progresso precisa estar sincronizado com a conta na nuvem antes da remoção do app. Após reinstalar, autenticar com a mesma conta e restaurar o perfil remoto antes de mostrar valores iniciais.
- Não prometer preservação de progresso visitante que nunca foi sincronizado.
- Preservar `applicationId`, assinatura compatível e versionamento de atualização; não gerar uma variante que force desinstalação ou pareça uma instalação nova.
- Antes de qualquer migração, criar testes com perfil já existente e verificar que saldo e estrelas não diminuem.

## Critérios de aceite antes de gerar APK
1. Comparação visual das telas principais com a versão 1.2, sem retorno aos botões antigos.
2. Login e restauração de perfil testados de ponta a ponta.
3. Perfil com saldo e estrelas existentes continua igual após reiniciar e após reinstalar com a mesma conta.
4. Modo visitante continua disponível e informa corretamente a limitação de dados locais após desinstalação.
5. Testes de migração/sincronização comprovam que falha de rede ou perfil ausente não zera dados.
6. APK usa identificador, assinatura e versionamento compatíveis com atualização.
7. Relatar separadamente compilação, testes automatizados, teste em emulador e teste real no aparelho.

Nenhuma implementação funcional é declarada concluída por este documento. Ele fixa a base e os critérios para as próximas correções.
