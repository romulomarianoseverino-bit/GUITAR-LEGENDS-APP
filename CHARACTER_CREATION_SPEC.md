# Padrão oficial de estilos e criação de personagens

Este documento define a nomenclatura e os requisitos oficiais para os estilos visuais de personagens do Guitar Legends.

## Os três estilos oficiais

Os nomes devem ser usados exatamente assim em documentação, interface, modelos, dados, testes e materiais do projeto:

1. **3D Estilizado** — personagem tridimensional com acabamento artístico próprio, anatomia e materiais mais naturais e detalhados, sem exigir fotorrealismo.
2. **3D Cartoon** — personagem tridimensional com linguagem cartunesca, formas, proporções e expressões intencionalmente estilizadas.
3. **3D Anime** — personagem tridimensional com linguagem visual inspirada em anime, incluindo tratamento característico de olhos, cabelo, expressões e figurino.

Não usar “realista 3D” como nome de uma quarta categoria nem substituir “3D Estilizado” por “3D realista”.

## Criação de personagem a partir de imagem

O fluxo planejado deve:

1. Permitir selecionar ou enviar uma imagem de referência.
2. Analisar a imagem e sugerir uma das três categorias oficiais: **3D Estilizado**, **3D Cartoon** ou **3D Anime**.
3. Mostrar a categoria detectada e permitir que o jogador a altere manualmente antes de gerar.
4. Gerar um modelo tridimensional utilizável no jogo — não apenas uma imagem ou textura com aparência 3D.
5. Preparar o modelo para rigging/esqueleto, expressões e animações compatíveis com as ações do palco e do instrumento.
6. Abrir o resultado no editor para ajustes de aparência, roupas, cabelo e acessórios.
7. Salvar o personagem e seus dados na conta, com sincronização e recuperação entre dispositivos.

## Requisitos de qualidade e validação

- A silhueta, o rosto, o cabelo, o figurino e os materiais devem corresponder ao estilo selecionado.
- A detecção automática deve ser uma sugestão editável, nunca uma escolha irreversível.
- O fluxo deve informar claramente quando a imagem não permitir uma classificação confiável.
- Modelos gerados precisam passar por validação técnica antes de serem disponibilizados: integridade da malha, escala, materiais, rigging, expressões, animações e desempenho no Android.
- Uma imagem 2D, um avatar estático ou uma prévia animada de um modelo existente não contam como geração de personagem 3D por imagem.
- Não declarar o recurso concluído até que envio, detecção, geração, edição, salvamento e sincronização tenham sido testados de ponta a ponta.

## Situação

Este arquivo registra o escopo e a nomenclatura aprovados. Ele não significa que a geração por imagem, a detecção automática ou os três estilos já estejam implementados no aplicativo.
