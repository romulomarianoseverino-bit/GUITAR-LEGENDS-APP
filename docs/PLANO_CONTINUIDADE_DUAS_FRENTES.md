# Plano de continuidade — duas frentes do Guitar Legends

Este documento organiza o trabalho em duas frentes paralelas. Ele é um quadro de acompanhamento: itens descritos como pendentes ou em validação não devem ser considerados implementados até passarem por testes reais.

## Frente A — Evolução do sistema de personalização

### Estúdio de personagens 3D
- Criar personagens 3D reais, com malha, anatomia coerente, materiais, cabelo, roupas e rig utilizável; não substituir por imagens 2D, filtros ou camadas planas.
- Personalização modular de corpo/proporções, cabeça/rosto, cabelo, roupas, calçados, acessórios, cores, materiais, estampas e detalhes.
- Combinar peças de diferentes conjuntos com encaixe correto e sem clipping.
- Preview 360 graus, zoom e atualização visual imediata.
- Salvar, reabrir e editar sem perder configurações.
- Manter os estilos oficiais: “3D Estilizado”, “3D Cartoon” e “3D Anime”.
- A partir de imagem de referência, sugerir o estilo, permitir escolha manual e gerar personagem 3D editável e animável.
- Manter consistência entre estúdio, salas multiplayer e gameplay.
- Incluir cosméticos desbloqueáveis por moedas e suporte a skins personalizadas.

### Animações musicais
- Preview em repouso e com guitarra, violão acústico, baixo, bateria e vocal.
- Animações coerentes com a função musical durante as partidas.
- Biblioteca variada de poses e movimentos próprios do jogo musical: vitória, derrota, comemoração, entrada e saída de palco, saudação, agradecimento, espera, reações, incentivo e interação entre integrantes.
- Transições naturais entre repouso, execução, reações e resultados.
- Animações adaptadas aos três estilos visuais. Não incluir sistema de combate.

### Pipeline e validação
- Continuar a validação do pipeline gratuito Blender + MPFB2 e dos pacotes de recursos, verificando licença de cada ativo.
- Gerar e inspecionar o modelo, o rig, as animações, o GLB e o relatório antes de declarar o protótipo válido.
- Validar desempenho em Android e manter custo financeiro zero.

## Frente B — Correções e funcionalidades pendentes

### Conta e apelido
- O login/autenticação já funciona; o problema identificado é o salvamento/reserva do apelido, que retorna “Not Found”.
- Investigar a existência do banco Firestore customizado e a publicação das regras nele; obter logs completos para confirmar a causa.
- Corrigir e testar disponibilidade, reserva, troca e prevenção de apelidos duplicados sem afetar contas existentes.

### Multiplayer
- Validar o multiplayer ponta a ponta: criação/entrada em sala, seleção de instrumentos, sincronização, início, pontuação, encerramento e reconexão.
- Implementar/validar duas pistas independentes lado a lado na partida multiplayer.
- Oferecer escolha individual entre guitarra, violão acústico tradicional, baixo, bateria e vocal; permitir instrumentos repetidos entre participantes, com charts e animações correspondentes.
- Sincronizar a música importada pelo anfitrião com os convidados durante a sessão, sem salvá-la permanentemente nos dispositivos dos convidados.
- Validar a transferência automática da liderança quando o anfitrião sair antes do início, mantendo a sala aberta quando houver outro participante.
- Rever e testar a política de saída do anfitrião durante uma partida ativa.
- Validar os modos “Com Strike” e “Sem Strike”; Strike só pode ser usado quando todos concordarem, caso contrário a sala usa Sem Strike.

### Biblioteca musical e playlists
- Completar a prevenção de duplicatas na biblioteca e nas playlists, distinguindo a mesma gravação de versões realmente diferentes (ao vivo, acústica, remaster, versões de fãs etc.).
- Não usar apenas título/artista/metadados como prova absoluta de identidade; evitar falsos positivos.
- Na biblioteca, não criar outra entrada nem sobrescrever a gravação original quando for a mesma gravação.
- Na mesma playlist, impedir a repetição da mesma gravação/versão; permitir a mesma música em playlists diferentes e versões diferentes na mesma playlist.
- Usar a mensagem: “Você já tem essa música na sua playlist. Gostaria de escolher outra?”
- Implementar playlists ilimitadas, nomes e ordem livres, adicionar/remover/reordenar músicas e persistência.

### Fluxo de músicas e gameplay
- Permitir escolher a próxima música após/durante a partida, com confirmação antes de pular e opção de manter ou trocar instrumento/dificuldade.
- Disponibilizar charts Easy, Medium, Hard e Expert por instrumento/dificuldade.
- Melhorar visual e feedback do medidor de aprovação e do multiplicador, não apenas trocar cores ou rótulos. A referência visual enviada anteriormente deve ser solicitada novamente quando essa etapa for iniciada.

### Menu de pausa e áudio
- Manter a ordem exata: 1. Continuar; 2. Reiniciar música; 3. Controle de volume; 4. Próxima música; 5. Sair.
- Incluir “Próxima música” também na tela de conclusão.
- Controles de volume para cinco canais: geral, música, efeitos de gameplay, interface/menus e ambiente/plateia/palco.
- Cada canal deve ter slider e campo numérico sincronizados nos dois sentidos, aplicar alterações imediatamente e aceitar 0–100.

## Regras de execução e aceite
- Trabalhar nas duas frentes sem abandonar as correções enquanto a personalização avança.
- Manter mudanças isoladas em branches próprias, criadas a partir da versão atual correta; não substituir a versão atual por uma demonstração antiga.
- Não declarar um item concluído só porque o código foi escrito ou o APK compilou. Exigir testes relevantes, incluindo testes reais no Android e multiplayer quando aplicável.
- Priorizar ferramentas gratuitas e de código aberto. Nenhum custo pago sem decisão explícita e comprovação de receita líquida sustentável.
- Atualizar este quadro conforme cada item for efetivamente implementado, testado e validado.
