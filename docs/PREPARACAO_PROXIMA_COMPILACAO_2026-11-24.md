# Preparação para a próxima compilação de teste — 24/11/2026

## Objetivo e regra de liberação

Preparar uma versão de teste instalável para validação real no Android, sem gerar APK nesta etapa. A compilação só deve ser disparada depois de consolidar as alterações, executar os testes e verificar que o artefato corresponde à branch atual. Um APK que apenas compila/abre não significa que as funcionalidades estejam prontas.

Trabalhar a partir do estado atual de integração e reconciliar mudanças por commits/arquivos, sem substituir a interface e o gameplay atuais por uma demonstração antiga. Não integrar branches inteiras sem revisar conflitos e regressões. Não publicar na main sem validação.

## 1. Personagem 3D de palco — prioridade visual

### Base aprovada pelo usuário
Usar como base de continuidade o modelo mostrado em `Imagens/file_00000000afa8820e8132233143f51295.png` na main e o avatar GLB da linha `feat/luan-inspired-stage-character`. Não reiniciar a criação a partir de um modelo procedural antigo.

### Melhorias necessárias
- Aproximar com respeito a identidade visual pública de Luan Santana, mantendo um personagem original do Guitar Legends, sem prometer cópia fotográfica.
- Melhorar rosto, formato geral, olhos, sobrancelhas, nariz, boca, mandíbula e proporções; não considerar apenas a troca de materiais como melhoria facial.
- Cabelo castanho-escuro, curto, texturizado, com volume e corte de palco coerente.
- Refazer/harmonizar o figurino: alfaiataria de palco contemporânea, preto/grafite, calçado escuro e detalhes discretos; eliminar aparência genérica de roupa de escritório, combinação azul/marrom destoante e elementos sertanejos caricatos.
- Conferir se os materiais realmente estão separados para paletó, camisa, calça, gravata/detalhes e calçado; não aplicar uma cor única ao corpo inteiro por erro de nomes de materiais.
- Produzir renders frontal, 3/4, perfil, traseiro e close do rosto; verificar também escala, malha, rig e deformações.
- Exportar GLB e validar que abre, mantém materiais, malhas e armature. Confirmar animação/rig antes de integrar ao jogo.
- Não afirmar semelhança ou aprovação antes de comparar os renders com a referência.

### Estado já conhecido
A branch `feat/luan-stage-avatar-refinement` tem uma primeira tentativa de ajuste de paleta e cabelo. A geração estava em andamento na run 45118722934. Essa tentativa não comprova ainda melhoria facial ou roupa final. A linha anterior de pipeline Blender/MPFB2 também tinha runs em andamento; verificar antes de disparar novas gerações.

## 2. Nickname, autenticação e perfil
- Validar disponibilidade e reserva de nickname com normalização case-insensitive e transação atômica.
- Impedir que duas contas reservem o mesmo nickname; permitir o próprio usuário manter o nickname atual.
- Preservar e restaurar perfil, moedas, progresso, estatísticas e configurações ao relogar.
- Verificar login/autenticação de ponta a ponta e regras Firestore na base real.
- Diagnóstico de produção ainda pendente: os testes do emulador passaram, mas não confirmam que o banco Firestore customizado exista e tenha as regras publicadas. Não trocar silenciosamente para banco default nem arriscar dados existentes. Obter logs completos se falhar no aparelho.

## 3. Multiplayer e salas
- Testar criação, entrada, código de sala, lotação, prontidão, desconexão e reconexão.
- Host sai antes do início: transferir liderança para o próximo participante online, mantendo sala WAITING e preservando os loadouts.
- Host sai durante partida: validar política explícita e consistente; não confundir com a transferência pré-partida.
- Cada participante escolhe livremente guitarra, violão acústico tradicional, baixo, bateria ou vocal; duplicatas de instrumentos são permitidas.
- Confirmar charts e animações corretos por instrumento e dificuldade.
- Música importada pelo anfitrião deve ser transmitida/sincronizada para convidados durante a sessão, sem salvar permanentemente no aparelho convidado.
- Implementar duas pistas independentes lado a lado durante gameplay multiplayer, com notas, instrumento, pontuação e estado individuais.
- Testar sincronização de início, pausa, progresso, placar e fim de partida sob latência/desconexão.

## 4. Partidas Com Strike e Sem Strike
- Implementar escolha do modo na criação/configuração da sala.
- Sem Strike: nenhuma habilidade de ataque/perturbação fica disponível.
- Com Strike: catálogo de poderes que interferem na experiência do adversário, com regras claras, cooldowns, balanceamento e feedback visual.
- A ativação de Com Strike só é permitida se todos os participantes concordarem. Se um participante não aceitar, configurar a sala como Sem Strike.
- Estado e efeitos devem ser autoritativos/sincronizados para todos, sem confiar apenas no cliente.
- Não confundir Strike com Star Power individual já existente.

## 5. Biblioteca, duplicatas e playlists
- SHA-256 deve bloquear cópias binariamente idênticas sem apagar nem sobrescrever a faixa já cadastrada.
- Corrigir o teste atual de importação com nomes iguais: nome/metadata iguais não provam gravação igual.
- Planejar fingerprint acústico/perceptual gratuito e open-source com limiar conservador; não bloquear automaticamente versões diferentes.
- Não usar título/artista/duração como identificador absoluto.
- Criar playlists ilimitadas, nome livre, persistência, adicionar/remover/reordenar.
- A mesma gravação não pode repetir na mesma playlist; pode aparecer em playlists diferentes.
- Versões realmente diferentes (ao vivo, acústica, remaster, interpretação de fã etc.) podem coexistir na mesma playlist.
- Mensagem obrigatória: “Você já tem essa música na sua playlist. Gostaria de escolher outra?”
- Testes para duplicata exata, mesmo título com áudio diferente, recodificação, versões diferentes, playlists distintas e preservação da ordem.

## 6. Fluxo de músicas e charts
- Após/durante partida, permitir próxima música com confirmação para pular.
- Permitir manter ou trocar instrumento e dificuldade.
- Charts Easy/Medium/Hard/Expert próprios por instrumento; validar geração/seleção e comportamento real.
- Botão Próxima música na conclusão e no menu de pausa.

## 7. Menu de pausa e volumes
A ordem final deve ser:
1. Continuar
2. Reiniciar música
3. Controle de volume
4. Próxima música
5. Sair

- Layout completo sem rolagem problemática em telas Android menores.
- Cinco canais: geral, música, efeitos de gameplay, interface/menus, ambiente/plateia/palco.
- Cada canal com slider e campo numérico 0–100; sincronização bidirecional e aplicação imediata.

## 8. Gameplay, palco e apresentação
- Melhorar visual e feedback do medidor de aprovação e multiplicador (mudança estrutural/visual real, não só rótulos ou cores).
- Plateia com movimento e reações dinâmicas a música, acertos e erros; não manter imagem estática.
- Cantores e músicos com animações próprias e coerentes com função/instrumento.
- Personagem se move no estúdio de personalização e permite visualizar o modelo em 360 graus.
- Transições suaves entre repouso, execução, celebração, vitória/derrota, saudação, agradecimento e reações.
- Personalização deve persistir e aparecer corretamente no multiplayer e na partida.

## 9. Estúdio de personagens
Manter os três estilos oficiais: “3D Estilizado”, “3D Cartoon” e “3D Anime”.
- Criar/editar personagem 3D real, com malha, materiais, cabelo, roupas e rig; não apenas imagem, filtro ou camadas planas.
- Modularidade e compatibilidade entre peças; evitar clipping.
- Foto de referência sugere estilo, permite escolha manual e gera personagem editável/animável.
- Preview 360°, zoom, salvamento e reabertura sem perda.
- Cosméticos desbloqueáveis por moedas e skins personalizadas.
- Preview e animações específicas para guitarra, violão, baixo, bateria e vocal.

## 10. Critérios de aceite para liberar APK de teste
- Build debug APK concluído na branch consolidada e SHA registrado.
- Testes unitários, instrumentados e Firestore Emulator relevantes passando; falhas conhecidas listadas e corrigidas ou explicitamente bloqueadoras.
- Instalação/abertura no Android confirmada.
- Testes reais documentados para login/nickname, sala multiplayer, seleção de instrumentos duplicados, transferência de host pré-partida, sincronização de música, playlists/duplicatas, modos Strike, menu de pausa/volumes e gameplay.
- Verificar duas pistas lado a lado, plateia animada e persistência da personalização.
- Não declarar funcionalidade pronta com base apenas em compilação ou teste de unidade.
- Não distribuir como versão final enquanto autenticação, multiplayer, personalização essencial e pistas/validação de gameplay não estiverem aprovados ponta a ponta.

## 11. Restrições
- Custo financeiro zero: usar ferramentas gratuitas/open-source e GitHub Actions; verificar licenças e custos indiretos.
- Não iniciar workflows redundantes enquanto houver execuções relevantes em andamento.
- Não gerar APK até a etapa de validação solicitada pelo usuário.
- Preservar o jogo atual e trabalhar em branches isoladas; só integrar após revisão e testes.

## Evidências de partida
- Integração de referência: `integration/next-apk-validation` (divergente da main; não compilar sem reconciliar).
- Nickname: `fix/nickname-not-found` e run 45115735017 (workflow concluído com sucesso; validação de produção pendente).
- Transferência de host: `fix/multiplayer-host-transfer` e run 45115738355 (workflow concluído com sucesso; teste real multiplayer ainda pendente).
- Duplicatas: `fix/catalog-track-duplicate-detection` (SHA-256 inicial; build/testes e playlists pendentes).
- Personagem 3D: `feat/luan-stage-avatar-refinement`, run 45118722934 (verificar resultado antes de aceitar).
- Falha geral anterior: run 45096047664; revisar logs e corrigir o caso de importação com nomes iguais antes de nova validação.
