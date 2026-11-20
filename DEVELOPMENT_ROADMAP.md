# Roteiro de evolução — Guitar Legends: Edição Lendária

Este roteiro consolida decisões aprovadas e organiza o desenvolvimento para que correções, evolução de qualidade e novas implementações avancem em paralelo. A existência deste documento não significa que os itens estejam implementados.

## Regra de desenvolvimento

Cada ciclo deve entregar pelo menos um avanço funcional ou uma implementação verificável, além das correções necessárias. Evitar ciclos longos dedicados apenas a polimento pequeno. Agrupar alterações relacionadas, testar proporcionalmente e registrar claramente o que está implementado, pendente e validado.

## Frentes permanentes

### A. Corrigir e consolidar o que já existe
- [ ] Login Google: corrigir e validar autenticação real de ponta a ponta.
- [ ] Multiplayer: criação/entrada em salas, presença, seleção livre de instrumentos repetidos e sincronização real de música, notas, andamento e pontuação.
- [ ] Controles físicos: validar Bluetooth/USB em aparelhos reais, gameplay, acordes, remapeamento, menus, pausa e Star Power.
- [ ] Áudio: controles independentes de volume geral, música e efeitos; persistência das preferências.
- [ ] Recompensas e progressão: validar cálculo no cliente/servidor e preservação dos saldos e limites existentes.
- [ ] Compatibilidade, desempenho, estabilidade e recuperação de estado.

### B. Elevar o nível do jogo existente
- [ ] Gameplay completa: pista, notas, botões, HUD, palco, câmera, iluminação e efeitos coerentes entre si.
- [ ] Instrumentos com modelos e acabamento mais convincentes.
- [ ] Animações dos músicos coerentes com instrumento, música e desempenho.
- [ ] Plateia com reações visuais ao andamento e aos acertos/erros.
- [ ] Menus e telas com uma identidade visual unificada, inspirada na apresentação dos jogos musicais de console da era PS2, mas com arte original.
- [ ] Tela de pausa com controles de áudio e navegação clara.

### C. Implementar funcionalidades novas
- [ ] **Personalização 3D real**: substituir a representação provisória em Canvas por modelos 3D carregados por um pipeline apropriado, com malhas, materiais, esqueleto e animações.
- [ ] Editor de personagem com rotação de 360°, zoom, seleção de roupas, cabelo, acessórios e visualização imediata.
- [ ] Três estilos oficiais: **3D Estilizado**, **3D Cartoon** e **3D Anime**.
- [ ] Criação baseada em imagem: sugerir um dos três estilos, permitir correção manual e produzir um modelo 3D utilizável, rigável e animável. Uma imagem 2D ou avatar estático não conta como geração 3D.
- [ ] Instrumentos selecionáveis: guitarra, violão acústico tradicional, baixo, bateria e vocal; seleção repetida permitida no multiplayer.
- [ ] Abertura cinematográfica original, com personagens reais do próprio sistema, animação, palco, iluminação, logotipo e opção de pular.
- [ ] Nova tela inicial com banda/personagens do jogo e menu integrado ao cenário.
- [ ] Novo ícone com músicos e instrumentos, sem prometer qualidade que o aplicativo ainda não entrega.
- [ ] Progressão, cosméticos e desbloqueios integrados às regras de economia existentes.

## Direção artística e coerência

A referência é a atmosfera, composição, energia e apresentação dos Guitar Hero da era PS2 — não a cópia de personagens, logotipos, telas, músicas ou ativos protegidos. O Guitar Legends deve ter identidade própria.

O ícone, a abertura, a tela inicial, a personalização e as partidas devem usar o mesmo universo visual. Não usar arte promocional de personagem ultra-realista enquanto o jogo ainda apresenta apenas formas provisórias de Canvas.

## Marcos de entrega

1. **Base de personagem 3D funcional**: escolher e integrar o runtime de renderização 3D adequado ao Android; carregar um modelo de teste com materiais e câmera orbitável; validar desempenho em aparelho Android.
2. **Primeiro personagem completo**: modelo original, esqueleto, materiais, animações básicas e integração ao editor.
3. **Editor e integração**: personalização, persistência e reutilização do mesmo modelo no palco/gameplay.
4. **Apresentação do jogo**: abertura, tela inicial e ícone coerentes com os personagens efetivamente disponíveis.
5. **Consolidação de lançamento**: login, multiplayer, gameplay, personalização essencial e compatibilidade testados de ponta a ponta.

## Critério de conclusão

Um item só passa para **Concluído** quando estiver implementado e testado no contexto real correspondente. Compilar, abrir uma tela, exibir uma prévia Canvas ou produzir uma imagem estática não prova que um sistema 3D, login, multiplayer ou animação esteja concluído.

## Situação técnica identificada nesta revisão

- Existe um arquivo de especificação de criação de personagens, mas ele descreve requisitos e não comprova a implementação.
- O renderizador chamado `Character3DRenderer.kt` usa Compose Canvas e projeção matemática; portanto, é uma representação 2D com aparência de profundidade, não uma malha 3D real.
- A tela de personalização chama visualizadores interativos existentes, mas isso, por si só, não comprova um pipeline 3D com modelos, esqueleto e animação.
- A abertura cinematográfica, a nova tela inicial e o ícone com músicos devem permanecer pendentes até serem implementados com ativos coerentes do jogo.
- Não marcar nenhum desses recursos como concluído sem testes e evidências correspondentes.

## Atualização contínua

Ao terminar cada ciclo, atualizar este roteiro com: arquivos alterados, comportamento implementado, testes executados, resultados, limitações conhecidas e próximo avanço funcional. Não remover itens pendentes apenas porque uma nova conversa ou correção começou.
