# Correção do sistema de apelido e implementação de playlists — Guitar Legends

## Status e preservação
- Branch de acompanhamento: `fix/preserve-v1.2-baseline`.
- Esta especificação registra requisitos e critérios de aceite; não declara implementação funcional.
- Preservar a interface, os botões, o login e os fluxos existentes da versão 1.2.
- **Login/autenticação: funcionando.** Não é uma correção pendente. Não reimplementar nem alterar o provedor sem diagnóstico.
- **Apelido: correção pendente.** É um problema separado do login e não pode impedir o acesso ao aplicativo.

## Frente A — Sistema de apelido
1. Permitir definir e editar o apelido do jogador.
2. Validar campos vazios, espaços excedentes, tamanho e caracteres aceitos, com mensagens claras.
3. Salvar o apelido de forma persistente e associá-lo à conta correta.
4. Carregar e exibir o apelido correto no perfil, nas salas e durante o multiplayer.
5. Sincronizar alterações entre dispositivos e após sair/entrar novamente.
6. Tratar indisponibilidade de rede sem apagar o apelido salvo nem bloquear o login.
7. Verificar colisões/duplicidade de apelidos e informar o jogador sem sobrescrever dados de outra conta.
8. Criar testes para conta nova, conta existente, edição, reconexão, falha de rede e exibição em sala.

## Frente B — Playlists personalizadas
1. Permitir criar várias playlists, sem limite artificial imposto pela interface.
2. Permitir definir e editar o nome de cada playlist.
3. Adicionar músicas à playlist e remover músicas individualmente.
4. Reordenar livremente as músicas, inclusive por controles simples adequados ao celular.
5. Editar playlists existentes sem perder músicas ou sua ordem.
6. Preservar playlists após fechar/reabrir o aplicativo e sincronizar com a conta quando houver suporte de nuvem.
7. Não confundir músicas com o mesmo título: gravações/versões diferentes devem poder coexistir.
8. Detectar duplicidade da mesma gravação dentro da mesma playlist usando identificador estável e, quando necessário, verificação do conteúdo de áudio (hash), sem depender apenas do título.
9. Ao tentar adicionar uma gravação que já existe na playlist, exibir exatamente: **“Você já tem essa música na sua playlist. Gostaria de escolher outra?”**
10. Não adicionar a duplicata após o aviso; permitir ao jogador escolher outra música.
11. Testar criação, renomeação, adição, remoção, reordenação, persistência, sincronização e detecção de duplicatas/versões diferentes.

## Critérios de aceite
- Login continua funcionando sem alteração desnecessária.
- Apelido pode ser criado/editado, salvo, recuperado e exibido corretamente no perfil e no multiplayer.
- Playlists podem ser criadas e editadas livremente, com ordem persistente.
- A mesma gravação não é adicionada duas vezes à mesma playlist.
- Versões diferentes da música não são bloqueadas apenas por compartilharem o título.
- A mensagem de duplicidade corresponde exatamente ao texto especificado.
- Testes automatizados e testes reais em aparelhos distintos são relatados separadamente.
- Nenhum APK é considerado válido apenas por compilar; validar as funções e preservar a versão 1.2.

## Estado real no momento deste registro
- Login: funcionando, conforme correção confirmada pelo usuário.
- Apelido: pendente de diagnóstico/implementação/testes.
- Playlists: requisitos registrados; implementação e testes ainda pendentes.
- Este documento é especificação de trabalho, não evidência de que as funcionalidades já estejam prontas.


## Frente C — Multiplayer obrigatório (não esquecer no escopo geral)
O multiplayer é uma frente prioritária separada das correções de apelido e playlists. Seus requisitos completos e testes estão em [CORRECAO_MULTIPLAYER_MUSICA_DUAS_PISTAS_STRIKE.md](./CORRECAO_MULTIPLAYER_MUSICA_DUAS_PISTAS_STRIKE.md). Para evitar omissões, ficam destacados aqui os pontos que precisam ser implementados e validados:

1. **Sincronização da música:** anfitrião e convidados precisam tocar exatamente a mesma gravação, com início agendado em tempo comum, compensação de latência e correção de diferença de reprodução. Sincronizar pausa, retomada, reinício, encerramento e reconexão.
2. **Áudio importado:** quando o anfitrião usar uma música importada, disponibilizá-la temporariamente e com acesso restrito à sala para os convidados, sem adicioná-la permanentemente à biblioteca deles. Se a entrega/validação falhar, não iniciar a partida com músicas diferentes. Manter solução sem custo pago.
3. **Escolha da modalidade:** mostrar duas opções explícitas na sala: **Com Strike** e **Sem Strike**. Com Strike só pode ser confirmado se todos concordarem; se qualquer jogador recusar, não responder ou retirar a confirmação, prevalece Sem Strike. Exibir a decisão final para todos antes do início. Strike não é Star Power.
4. **Duas pistas simultâneas:** exibir uma pista independente para cada participante, lado a lado, no multiplayer. Cada pista usa o instrumento, dificuldade e chart daquele jogador, com notas, combo, pontuação e conexão separados. Não misturar charts nem placares. O modo solo continua com sua pista atual.
5. **Prontidão e início:** não iniciar até todos confirmarem a mesma música/versão e a configuração acordada, e até os aparelhos estarem preparados. O início deve ocorrer no mesmo instante sincronizado para todos.
6. **Instrumentos livres:** guitarra, violão acústico tradicional, baixo, bateria e vocal; permitir que participantes escolham o mesmo instrumento.
7. **Continuidade da sala:** se o anfitrião sair antes do início, transferir a liderança automaticamente e preservar a sala e as escolhas confirmadas.
8. **Validação real:** testar com pelo menos dois aparelhos, incluindo música interna e importada, as duas pistas, instrumentos iguais/diferentes, Com Strike/Sem Strike, pausa/retomada, reconexão e saída do anfitrião.

**Estado:** requisitos registrados; não declarar essas funções prontas até implementação, testes automatizados e validação real em dois dispositivos.
