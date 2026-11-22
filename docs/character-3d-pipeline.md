# Pipeline de personagem 3D — decisão técnica e critérios de validação

## Objetivo

Substituir gradualmente o personagem procedural 2.5D do estúdio por um modelo 3D humano real, sem remover o renderizador atual até que o novo modelo passe pelos testes visuais e funcionais no Android.

O primeiro personagem de referência será um músico de rock com jaqueta de couro, camiseta, jeans, botas e violão/guitarra. A referência orienta o figurino e a silhueta; não deve ser tratada como autorização para copiar a identidade de uma pessoa.

## Ferramentas escolhidas para o fluxo de criação

- Blender 4.2 ou superior.
- MPFB 2 (MakeHuman Plugin For Blender 2) para gerar e ajustar a base humana.
- Exportação de ativos em glTF 2.0 / GLB para o aplicativo Android.
- SceneView/Filament como caminho de renderização 3D já avaliado em uma tela de prova isolada.

O MPFB 2 declara exigir Blender 4.2 ou superior. O Blender e o MPFB são ferramentas de produção de conteúdo; não serão dependências instaladas pelo jogador nem executadas dentro do APK.

## Licenciamento e distribuição

A licença oficial do MPFB separa claramente código e ativos:

- O código-fonte do complemento MPFB é GPLv3.
- Os ativos gráficos incluídos pelo projeto — malha base, proxies, alvos, modificadores, texturas, roupas, rigs, poses e expressões — são declarados CC0 1.0.
- A licença do MPFB declara que os arquivos de saída (incluindo exportações) são tratados pelos autores como dados do usuário, sem reivindicação de direitos sobre eles.

Decisão para este projeto: usar o MPFB apenas como ferramenta externa de autoria e distribuir somente os modelos exportados, sem copiar ou incorporar código Python do complemento ao aplicativo. Antes de incluir qualquer roupa, textura, cabelo ou acessório, registrar a origem e conferir a licença específica do ativo; não presumir que um recurso externo ao pacote oficial seja CC0.

Referências oficiais:
- MPFB: https://github.com/makehumancommunity/mpfb2
- README e requisito de Blender: https://github.com/makehumancommunity/mpfb2/blob/master/README.md
- Licença geral, incluindo a distinção entre código GPLv3 e ativos CC0: https://github.com/makehumancommunity/mpfb2/blob/master/LICENSE.md
- Licença dos ativos: https://github.com/makehumancommunity/mpfb2/blob/master/LICENSE.ASSETS.md

## Especificação do primeiro modelo

1. Malha contínua com anatomia natural: cabeça, pescoço, ombros, tronco, quadril, braços, mãos, pernas e pés proporcionais.
2. Roupa vestida e modelada como peças próprias: jaqueta, camiseta, jeans e botas; evitar volumes sobrepostos que pareçam blocos.
3. Materiais PBR otimizados para celular, com texturas em resolução controlada.
4. Cabelo e detalhes faciais próprios do estilo escolhido.
5. Esqueleto humano com pesos de deformação revisados em ombros, cotovelos, punhos, quadril, joelhos e tornozelos.
6. Instrumento separado do corpo, com pontos de encaixe para mãos e postura de execução.
7. Exportação GLB com escala, orientação, materiais e rig conferidos.

## Três estilos oficiais

Os três estilos devem continuar com os nomes exatos:
- 3D Estilizado
- 3D Cartoon
- 3D Anime

A primeira base humana pode compartilhar o mesmo rig e a mesma estrutura de malha, mas cada estilo precisa de ajustes visíveis de proporção, rosto, cabelo, materiais e acabamento. Não basta trocar o rótulo ou aplicar apenas um filtro de cor.

## Animações mínimas para validar o rig

- Idle de palco.
- Movimento de cabeça e tronco sincronizado com a música.
- Postura e movimento de execução específicos para guitarra, violão acústico, baixo, bateria e vocal.
- Deformação sem rompimento ou torção anormal nas articulações principais.
- Prévia de rotação 360° no estúdio, sem perder o modelo de vista.

A plateia reativa e as animações completas da banda continuam sendo requisitos de produção, mas não serão consideradas concluídas só porque o primeiro avatar está rigado.

## Integração Android

A prova SceneView/Filament já existente é isolada e não substitui o estúdio atual. A integração de produção deve:

- carregar GLB a partir dos ativos internos do app;
- manter o renderizador atual como fallback até a validação;
- respeitar os três estilos e as opções de personalização;
- preservar salvamento e carregamento do perfil do personagem;
- não exigir conexão de internet nem serviço pago;
- manter o desempenho aceitável em aparelhos Android de entrada.

## Critérios obrigatórios antes de chamar de concluído

- [ ] GLB abre no SceneView/Filament sem erros.
- [ ] Modelo vestido e com proporções humanas aprovado em frente, perfil e costas.
- [ ] Texturas e materiais aparecem corretamente no Android.
- [ ] Rig deforma corretamente nas articulações.
- [ ] Animações de teste são reproduzidas.
- [ ] Rotação e zoom do estúdio continuam funcionando.
- [ ] Personalização persiste ao fechar e reabrir o app.
- [ ] Teste em aparelho Android real, incluindo desempenho e memória.
- [ ] Licenças e origem de todos os ativos usados estão documentadas.

Até cumprir esses critérios, não substituir o personagem de produção nem publicar um APK como versão melhorada do modelo 3D. Compilar o projeto, isoladamente, não valida a qualidade visual ou funcional.
