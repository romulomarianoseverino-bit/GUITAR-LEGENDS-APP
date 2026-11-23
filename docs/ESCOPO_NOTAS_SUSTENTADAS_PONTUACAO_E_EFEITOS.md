# Escopo obrigatório — notas sustentadas, pontuação e efeitos

Este requisito faz parte da implementação e das correções obrigatórias do Guitar Legends. Não é recurso opcional para uma atualização futura.

## Comportamento de jogo
- Suportar notas de sustentação médias, longas e muito longas em todos os instrumentos/modos em que o chart permita sustains.
- Conceder pontuação progressiva somente enquanto a nota válida estiver sendo mantida pelo jogador.
- Interromper a pontuação adicional quando o jogador soltar o comando antes do fim da sustentação.
- Calcular os pontos de forma determinística, respeitando duração efetivamente sustentada, multiplicadores e limites temporais; impedir pontuação duplicada em pausas, retomadas, atraso de quadros ou sincronização multiplayer.
- Preservar a pontuação inicial do acerto, combo, precisão, janela de acerto, detecção de erro e resultado final.

## Controles
- No celular, a sustentação e os efeitos básicos devem funcionar apenas mantendo pressionado o botão/toque correspondente à nota; não exigir alavanca física.
- No controle, a sustentação básica também funciona segurando o botão correspondente.
- Permitir, como opção configurável, associar um botão ou eixo compatível do controle (por exemplo R1 ou L2, conforme o dispositivo) para modificar/intensificar o efeito visual. A configuração adicional nunca pode ser obrigatória.
- Tratar controles sem eixo analógico compatível com segurança, com calibração/deadzone quando necessário; não presumir que todos os controles mapeiam os mesmos eixos.

## Efeitos visuais e áudio
- Exibir brilho, ondulações e partículas sincronizados à nota sustentada.
- A intensidade e/ou duração visual deve acompanhar a sustentação, com apresentação perceptivelmente adequada também nas notas muito longas.
- O efeito adicional do controle, quando configurado, modifica a apresentação sem alterar indevidamente o chart ou a precisão.
- Qualquer modulação sonora é complementar e só será incluída se compatível com o mecanismo de áudio e sem comprometer a música.

## Critérios obrigatórios de validação
- Testar notas médias, longas e muito longas, incluindo sustentar até o fim e soltar antes do fim.
- Conferir pontuação progressiva, multiplicadores, pausas/retomadas e prevenção de pontos duplicados.
- Testar no celular por toque e em controle físico, incluindo controle sem alavanca analógica compatível.
- Verificar legibilidade das notas, desempenho de quadros e ausência de regressões no gameplay.
- A implementação só pode ser considerada concluída após testes automatizados pertinentes e validação prática nos dispositivos disponíveis. Compilar ou desenhar efeitos na pista, isoladamente, não constitui aprovação.
