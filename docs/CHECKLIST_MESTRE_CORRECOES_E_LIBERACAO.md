# Checklist mestre — Guitar Legends

Este documento reúne o escopo obrigatório de correções, implementação e validação. Nenhum item pode ser considerado concluído apenas por documentação, compilação ou abertura do aplicativo. Cada item exige evidência de código e teste adequado.

## 1. Preservação da versão-base
- [ ] Preservar a interface e navegação funcionais da versão 1.2.
- [ ] Preservar botões, menus, autenticação e recursos já funcionais.
- [ ] Impedir regressões visuais, retorno de telas antigas, botões quadrados ou saldos fictícios/default de 450 moedas.
- [ ] Comparar telas e fluxos da base com a versão candidata.
- [ ] Confirmar que atualização por instalação sobre a versão anterior preserva dados e executa migrações sem apagar progresso.
- [ ] Não integrar branches divergentes sem revisar conflitos e diferenças.

## 2. Conta, perfil e progresso
- [ ] Preservar login/autenticação funcional; não reescrever sem evidência de defeito.
- [ ] Corrigir validação, disponibilidade e salvamento de apelido.
- [ ] Preservar modo visitante e navegação sem login.
- [ ] Sincronizar progresso, moedas, conquistas e personalização.
- [ ] Mesclar progresso de visitante na conta sem duplicar recompensas.
- [ ] Sair da conta sem forçar autenticação novamente nem apagar dados indevidos.
- [ ] Histórico de partidas: salvar detalhes de cada partida para usuário autenticado, persistir pendências offline, sincronizar ao reconectar, restaurar após atualização, usar identificadores idempotentes e evitar duplicatas.
- [ ] Definir e validar paginação/retenção do histórico sem perda silenciosa de registros.

## 3. Biblioteca e áudio
- [ ] Importar MP3, WAV e OGG para armazenamento privado persistente.
- [ ] Preservar arquivos e metadados após reinício e atualização.
- [ ] Rejeitar duplicata exata, mas permitir gravações diferentes com mesmo título.
- [ ] Validar arquivos corrompidos e conteúdo que não seja música.
- [ ] Proteger recursos internos necessários contra exclusão.
- [ ] Playlists com nomes personalizados, organização, edição e ordenação.
- [ ] Tons curtos internos são opções provisórias, não músicas completas: exibir como orientação inicial quando não houver músicas próprias e ocultar da lista principal quando houver importações.
- [ ] Permitir ocultar/remover esses tons da lista sem apagar recursos essenciais do aplicativo.
- [ ] Permitir excluir músicas importadas pelo usuário com confirmação adequada.

## 4. Gameplay, pistas e instrumentos
- [ ] Guitarra, violão acústico, baixo, bateria e vocal.
- [ ] Dificuldades Fácil, Médio, Difícil e Expert com charts adequados por instrumento.
- [ ] Pistas com leitura, perspectiva e resposta visual aprimoradas, inspiradas nos Guitar Hero clássicos de PS2.
- [ ] Validar notas, acertos, erros, combos, multiplicadores, precisão e pontuação.
- [ ] Validar músicas importadas e charts gerados.
- [ ] Indicadores de desempenho e multiplicadores com apresentação visual clara.
- [ ] Notas sustentadas médias, longas e muito longas: pontuação progressiva apenas durante sustentação válida, interrupção ao soltar, sem duplicação por pausa/retomada/atraso.
- [ ] Efeitos luminosos, ondulações e partículas sincronizados à sustentação, legíveis e com intensidade adequada às notas longas.
- [ ] No celular, sustentar pelo toque/botão correspondente, sem exigir alavanca.
- [ ] No controle, sustentar pelo botão correspondente e oferecer comando adicional opcional configurável (botão/eixo compatível, como R1/L2 quando suportado) para modular o efeito; nunca obrigatório.
- [ ] Efeito adicional não altera indevidamente chart, precisão ou pontuação.
- [ ] Modulação sonora somente se tecnicamente compatível e sem comprometer a música.
- [ ] Testar controle com e sem eixo analógico compatível, deadzone/calibração e desempenho.

## 5. Pontuação, carreira e recompensas
- [ ] Auditar fórmula de moedas e reconciliar com regra acordada: 20 moedas base e 50 por desempenho de 100%, sem alterar sem validação.
- [ ] Auditar distribuição de recompensas e histórico.
- [ ] Recompensa diária sem reivindicação duplicada.
- [ ] Preservar compras, desbloqueios, moedas e progresso após atualização e sincronização.
- [ ] Nunca usar saldo fixo fictício como padrão universal.

## 6. Personalização de personagens
- [ ] Manter os três estilos oficiais: 3D Estilizado, 3D Cartoon e 3D Anime.
- [ ] Criar e personalizar personagens individualmente.
- [ ] Geração baseada em imagem: detecção assistida do estilo, seleção manual e personagem 3D utilizável/animável.
- [ ] Refinar o modelo-base já existente inspirado na referência de Luan Santana; não reiniciar sem necessidade.
- [ ] Melhorar rosto, cabelo, roupa, proporções e semelhança dentro dos limites técnicos.
- [ ] Não fixar corte de cabelo ainda não escolhido pelo usuário.
- [ ] Prévia 3D animada e inspeção em múltiplos ângulos.
- [ ] Roupas, cabelos, acessórios e skins desbloqueáveis por moedas; permitir skins personalizadas.
- [ ] Validar rig, animações, carregamento, salvamento e integração real no aplicativo.

## 7. Instrumentos, microfone, palco e plateia
- [ ] Guitarra elétrica, violão acústico, baixo e bateria 3D com aparência próxima a instrumentos reais de palco.
- [ ] Microfone 3D realista para categoria vocal.
- [ ] Instrumentos e microfone integrados à personalização e às animações de apresentação.
- [ ] Animações coerentes com instrumento e música.
- [ ] Plateia efetivamente animada e reativa ao desempenho, acertos/erros e música; não aceitar plateia estática como concluída.
- [ ] Validar desempenho e sincronização de animações.

## 8. Multiplayer
- [ ] Preservar criação e entrada em salas já funcionais.
- [ ] Garantir mesma música/gravação para todos; bloquear partida se a gravação não corresponder.
- [ ] Transferência temporária e segura de áudio importado, sem adicionar permanentemente à biblioteca de convidados.
- [ ] Sincronizar início, pausa, retomada, reinício e encerramento.
- [ ] Compensar latência e deriva de relógio; reconexão.
- [ ] Duas pistas independentes lado a lado com instrumento, chart, dificuldade, notas, combo, pontuação e conexão individuais.
- [ ] Permitir instrumentos iguais entre participantes.
- [ ] Strike somente com concordância explícita de todos; caso contrário, Sem Strike.
- [ ] Transferir liderança automaticamente se anfitrião sair antes da partida.
- [ ] Testar com pelo menos dois dispositivos reais.

## 9. Pausa, controles e áudio
- [ ] Menu de pausa na ordem: Continuar; Reiniciar música; Controle de volume; Próxima música; Sair.
- [ ] Confirmar antes de pular música.
- [ ] Permitir manter/alterar instrumento e dificuldade conforme fluxo acordado.
- [ ] Cinco canais independentes: master, música, efeitos de gameplay, interface/menus e plateia/ambiente.
- [ ] Sliders e valores numéricos sincronizados de 0 a 100.

## 10. Configurações e desempenho
- [ ] Resoluções automática, 480p, 720p, 1080p e 4K conforme capacidade.
- [ ] Qualidade gráfica.
- [ ] 30/60/90/120 FPS somente quando suportados pelo dispositivo.
- [ ] Economia de energia.
- [ ] Pré-visualização e restauração das configurações.

## 11. Segurança, custo e distribuição
- [ ] Revisar regras de acesso da nuvem e propriedade dos dados por UID.
- [ ] Não expor dados privados ou arquivos de áudio.
- [ ] Manter custo financeiro zero; verificar licenças, cotas e custos indiretos antes de adotar serviços.
- [ ] Não introduzir hospedagem, serviço ou conteúdo pago sem receita sustentável e aprovação explícita.
- [ ] APK candidato deve derivar da base correta e conter todas as alterações aprovadas.
- [ ] Executar testes unitários/instrumentados relevantes, compilação limpa, testes de migração e instalação sobre versão anterior.
- [ ] Testar em aparelho real; multiplayer em dois aparelhos.
- [ ] Registrar versão, commit, origem da base, resultados e limitações.
- [ ] Não liberar como versão testável se apenas compilar ou abrir; não chamar de final sem validação ponta a ponta.

## Critério de status
Use apenas: **Não iniciado**, **Em implementação**, **Implementado sem validação**, **Testado parcialmente**, **Validado** ou **Bloqueado**. Todo status deve citar commit, teste ou evidência verificável. Itens não inspecionados permanecem não confirmados.
