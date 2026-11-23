# Correção incremental do multiplayer existente

## Regra de preservação
A implementação deve partir da versão atual da `main` (versão 1.2) e preservar criação de sala, entrada por código, seleção de instrumento/dificuldade, confirmação PRONTO e fluxo de início que já funcionam. Não substituir o multiplayer por uma implementação paralela ou por uma demo.

## Diagnóstico confirmado na base atual
- `MultiplayerCloudRepository.createRoom()` grava na sala `trackId` e `trackTitle`, mas não transfere o arquivo de áudio escolhido pelo anfitrião.
- `GameViewModel.joinMultiplayerRoom()` tenta localizar `room.trackId` na biblioteca local do convidado. Se não encontrar, mantém a música local selecionada, o que permite anfitrião e convidado iniciarem músicas diferentes.
- `LiveStageGameplayScreen` usa `viewModel.selectedTrack` e desenha uma única pista principal. A tela também observa participantes, mas isso não cria uma pista de gameplay independente para cada jogador.
- O modelo de sala atual não contém estado de sincronização de reprodução (versão/ID de sessão, instante futuro de início, posição/pausa e revisão de seleção).
- As regras atuais de participantes não incluem violão acústico tradicional na lista de instrumentos permitidos; a extensão deve ser validada junto com charts e animações, sem quebrar salas existentes.
- Ainda não existe no fluxo atual uma modalidade de Strike consensual por sala; não confundir com Star Power individual, que já é outro recurso.

## Frente A — Mesma música para todos
1. Ao criar ou alterar a seleção, o anfitrião publica um identificador estável da gravação, título, duração, origem/tipo e revisão da seleção.
2. Convidados recebem a alteração em tempo real e confirmam que possuem exatamente a mesma gravação antes de ficarem PRONTOS.
3. Para faixas internas do jogo, resolver pelo mesmo ID de catálogo e verificar disponibilidade local.
4. Para áudio importado pelo anfitrião, implementar entrega temporária e autenticada aos membros da sala, com acesso limitado à sessão e sem salvar permanentemente na biblioteca do convidado. Não enviar apenas o caminho local/URI do Android: esse caminho não existe no aparelho do outro jogador.
5. Se a faixa não puder ser transferida/validada, bloquear o início e informar claramente qual participante não recebeu a música; nunca iniciar silenciosamente com faixas diferentes.
6. O anfitrião inicia por agendamento futuro comum. Todos carregam/preparam o áudio e charts, respondem READY; a sala define `startAtServerMs` com margem de preparação. Usar relógio sincronizado/estimativa de offset, compensação de latência e correção de drift.
7. Sincronizar eventos de pausa, retomada, reinício e encerramento, além da seleção da música. Reentrada/reconexão deve recuperar estado atual da sessão.
8. Manter custo zero: solução de transferência deve usar recursos já disponíveis ou alternativa gratuita validada, sem assumir storage/serviços pagos.

## Frente B — Duas pistas simultâneas
- Em multiplayer, apresentar pistas independentes lado a lado, uma por jogador (ou por participante conforme modo cooperativo), com nome, instrumento, dificuldade, chart, notas, combo, pontuação e estado de conexão próprios.
- Cada pista deve consumir o chart do instrumento/dificuldade daquele participante; não reutilizar o chart do anfitrião para todos.
- Preservar a pista única atual no modo solo.
- Adaptar layout a celulares estreitos: divisão legível, escala responsiva e sem sobreposição de controles; avaliar orientação e rolagem somente se necessário, sem esconder notas/placar.
- Estado de cada jogador deve ser autoritativo para seu próprio score; interpolar/exibir atualizações dos outros jogadores sem permitir que um jogador escreva a pontuação do outro.

## Frente C — Strike / Sem Strike
- Configuração escolhida na sala antes do início.
- Todos os participantes precisam concordar explicitamente com Strike.
- Se qualquer participante recusar ou não confirmar, a sala fica em Sem Strike.
- Mostrar a modalidade final a todos antes do início; bloquear divergências de configuração.
- Strike é um sistema competitivo de interferência com catálogo, cooldowns, limites, balanceamento e sincronização autoritativa. Não é o Star Power atual.
- No modo Sem Strike, desativar todos os efeitos de interferência sem desativar Star Power individual.

## Instrumentos e compatibilidade
Cada jogador escolhe livremente guitarra, violão acústico tradicional, baixo, bateria ou vocal. Instrumentos repetidos são permitidos. Cada participante recebe chart e animações adequados ao instrumento e dificuldade selecionados. Atualizar validação Firestore, enum/modelo, seleção, charts e animações de forma compatível.

## Testes obrigatórios
- Dois aparelhos: anfitrião e convidado recebem e executam a mesma faixa interna.
- Dois aparelhos: áudio importado pelo anfitrião chega ao convidado de forma temporária; nenhum começa se falhar a transferência.
- Impedir início com ID/revisão de música divergente.
- Testar relógios com diferença, carregamento lento, pausa/retomada, reconexão e drift.
- Duas pistas independentes: notas, instrumentos, dificuldades e placares não se misturam.
- Testar dois jogadores com mesmo instrumento e instrumentos diferentes.
- Strike só inicia se todos concordarem; um único não mantém Sem Strike.
- Regressão: criação/entrada de sala, PRONTO, início e saída/transferência de anfitrião continuam funcionando.
- Regras Firestore/emulador, testes unitários e build Android.
- Teste real em dois aparelhos antes de declarar funcional.

Nenhuma das três frentes é considerada implementada apenas por este documento. O aceite exige testes reais em dois dispositivos.
