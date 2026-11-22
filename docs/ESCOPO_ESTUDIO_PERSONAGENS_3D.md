# Escopo — Estúdio de criação de personagens 3D

## Referência de design
Usar como inspiração conceitual o nível de liberdade e a montagem modular do criador de personagens de Mortal Kombat: Armageddon: o jogador combina partes e elementos visuais, ajusta características e constrói um personagem próprio. A referência é de fluxo e profundidade de personalização, não de reprodução do jogo.

Não copiar modelos, roupas, texturas, interface, código, nomes de opções ou outros materiais protegidos de Mortal Kombat. O Guitar Legends deve ter identidade visual e conteúdo originais.

## Estrutura do criador
- Organizar a criação em categorias claras: corpo e proporções, cabeça e rosto, cabelo, roupas, calçados, acessórios e materiais/cores.
- Permitir combinar peças de diferentes conjuntos, com encaixe correto e sem partes atravessando o corpo.
- Oferecer ajustes graduais e neutros de características visuais e proporções, sem impor um padrão corporal ideal.
- Permitir personalizar cores, materiais, estampas e detalhes de roupas e acessórios, respeitando compatibilidade entre peças.
- Oferecer conjuntos iniciais e itens cosméticos desbloqueáveis por moedas obtidas no jogo, além de suporte a skins personalizadas conforme o escopo do projeto.

## Requisitos 3D obrigatórios
- O resultado precisa ser um personagem 3D real, com malha, anatomia coerente, materiais, cabelo/roupas e esqueleto (rig) utilizável; não basta uma imagem 2D, filtro ou montagem de camadas planas.
- As peças modulares devem ser compatíveis com o mesmo rig e com pontos de encaixe definidos, para permitir troca de partes sem quebrar a deformação ou a animação.
- O jogador deve poder girar e inspecionar o personagem em 360 graus, aproximar a câmera e visualizar o resultado durante a edição.
- Alterações devem atualizar o modelo em tempo real ou com resposta visual imediata.
- Salvar, reabrir e editar personagens sem perder peças, cores, materiais ou ajustes.

## Integração com Guitar Legends
- Manter os três estilos oficiais: “3D Estilizado”, “3D Cartoon” e “3D Anime”.
- A criação por imagem de referência deve sugerir o estilo correspondente, permitir escolha manual e produzir um modelo 3D editável e animável.
- Disponibilizar prévias de animação para postura em repouso e para os instrumentos do jogo.
- Durante as partidas, o personagem deve executar animações coerentes com o instrumento selecionado: guitarra, violão acústico, baixo, bateria ou vocal.
- As mesmas personalizações devem aparecer de forma consistente no estúdio, nas salas multiplayer e durante a partida.



## Biblioteca de poses e animações musicais
A referência a movimentos de ataque e defesa de jogos de luta pode inspirar apenas a variedade, a expressividade e a qualidade das poses. Não implementar combate, golpes, ataques, defesas ou movimentos de lutador.

Criar uma biblioteca original de animações apropriadas a um jogo musical, incluindo:
- Poses de vitória, comemoração e celebração após boas apresentações.
- Poses de derrota, frustração leve ou desapontamento após resultados ruins, sem exageros caricatos obrigatórios.
- Entrada no palco, apresentação ao público, saudações, agradecimentos e saída do palco.
- Movimentos de espera e repouso, reações, gestos de incentivo e interações entre integrantes da banda.
- Animações específicas e sincronizadas para guitarra, violão acústico, baixo, bateria e vocal.
- Transições suaves entre repouso, execução musical, reações e poses de resultado.

As animações devem combinar com o estilo visual escolhido (“3D Estilizado”, “3D Cartoon” ou “3D Anime”), respeitar o rig do personagem e poder ser reutilizadas nas telas de personalização, resultados, salas multiplayer e partidas quando fizer sentido. A seleção deve priorizar contexto musical e desempenho da partida; nenhuma animação de combate deve ser adicionada.

## Critérios de validação
1. Trocar peças de diferentes categorias sem falhas de encaixe ou deformações graves.
2. Girar o personagem em 360 graus e inspecionar frente, laterais e costas.
3. Testar rig e animações com cada função/instrumento suportado.
4. Salvar, fechar e reabrir o personagem preservando a configuração.
5. Confirmar que os três estilos oficiais geram personagens 3D reais e utilizáveis.
6. Validar desempenho em aparelho Android dentro do orçamento de memória e processamento do projeto.

## Restrições de custo e licenças
Priorizar ferramentas gratuitas e de código aberto, sem serviços pagos, créditos ou assinaturas. Verificar e registrar a licença de cada ferramenta e recurso artístico usado. A referência a Mortal Kombat: Armageddon não autoriza copiar ou redistribuir seus ativos.
