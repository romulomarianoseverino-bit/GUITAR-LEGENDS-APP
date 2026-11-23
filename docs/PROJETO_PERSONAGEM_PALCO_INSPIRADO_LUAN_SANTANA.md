# Projeto de personagem de palco inspirado na estética de Luan Santana

## Objetivo

Criar um avatar masculino 3D original para o Guitar Legends, inspirado em referências públicas da imagem artística de Luan Santana. O resultado deve ser uma interpretação estilística para o jogo, não uma cópia de fotografias, figurinos exclusivos, logotipos ou marcas pessoais. O personagem precisa continuar editável, rigável e compatível com os três estilos oficiais do estúdio: 3D Estilizado, 3D Cartoon e 3D Anime.

## Pesquisa visual e direção artística

A pesquisa pública consultada mostra uma identidade que mudou ao longo da carreira:
- Cabelo castanho-escuro, com textura e volume no topo, penteado para trás ou levemente para o lado; evitar tons claros/avermelhados e o cabelo baixo/chapado do protótipo anterior.
- Rosto oval com mandíbula mais definida, sobrancelhas escuras, olhos escuros e nariz de linhas suaves. O modelo deve ter anatomia facial completa e expressão natural, sem exagerar traços.
- A aparência pública alterna entre barba curta/bigode discretos e rosto mais limpo; isso deve ser uma opção cosmética separada.
- O vestuário de palco atual deve seguir uma linha contemporânea e elegante, com alfaiataria, camisas e jaquetas modernas, calças escuras e detalhes discretos. Os registros de 2025 mostram trocas de figurino e diferentes fases visuais; não reduzir a referência a um uniforme único.
- Nos figurinos de palco aparecem aplicações metálicas, bordados, brilho, ombros destacados e peças de impacto visual.
- A fase Luan City usa referências pop/futuristas e cenografia urbana; não deve ser reduzida ao estereótipo sertanejo de chapéu, bota e fivela.

## Referências consultadas

- CNN Brasil, 22/08/2024: figurinos e processo de criação visual de Luan Santana.
- Gshow: variações de cabelo e mudanças de visual ao longo da carreira.
- Portal Leo Dias, 16/08/2024: evolução do topete, roupas e fases visuais.
- Entrevista sobre Luan City 2.0, 16/10/2023: influências pop, K-pop e cyberpunk.
- Gshow, 13/03/2021: alfaiataria, jaquetas de couro e peças de linho.
- Registro Histórico oficial, gravado em outubro de 2025 e publicado em 2026: referência de fase de palco mais recente.

## Correção exigida após a primeira prévia

A primeira prévia não correspondeu à referência solicitada: cabelo/cor inadequados e elementos visuais com leitura de vaqueiro. A próxima geração deve remover integralmente acessórios pendurados na perna, retirar botas de vaqueiro e não usar chapéu ou fivela grande. Deve usar cabelo castanho-escuro texturizado e uma base de alfaiataria de palco contemporânea. O resultado precisa ser inspecionado visualmente antes de ser aprovado; trocar o nome do asset ou concluir o GLB não prova semelhança.

## Construção 3D obrigatória

1. **Cabeça e rosto**
   - Malha facial contínua com olhos, pálpebras, sobrancelhas, nariz, lábios, orelhas e mandíbula definidos.
   - Silhueta oval, mandíbula moderadamente marcada e proporções naturais.
   - Materiais de pele com variação sutil; evitar aparência de plástico.
   - Expressões faciais e rig facial compatíveis com animações de canto.

2. **Cabelo**
   - Base castanho-escura, volume real no topo e laterais mais controladas.
   - Mechas ou cartões de cabelo com direção e silhueta legíveis; não usar uma esfera lisa como cabelo.
   - Opções: topete volumoso, penteado lateral texturizado e corte curto.
   - Cabelo preso ao rig da cabeça e testado em rotação/animação.

3. **Figurino**
   - Look-base de palco: conjunto de alfaiataria contemporânea em tons escuros, com camisa/parte superior ajustada e calçado urbano discreto.
   - Variante de impacto: jaqueta preta com detalhes metálicos/dourados ou prateados, sem reproduzir figurino exclusivo.
   - Materiais independentes por peça: tecido fosco, couro sintético, metal e detalhes brilhantes.
   - Acessórios opcionais discretos: brinco, corrente e anéis. Proibidos elementos de vaqueiro como chapéu, fivela grande, bota de montaria e acessórios pendurados na perna.
   - Todas as peças devem ajustar ao mesmo rig e não atravessar o corpo.

4. **Estilos do jogo**
   - Produzir a mesma identidade nos estilos 3D Estilizado, 3D Cartoon e 3D Anime.
   - Preservar a leitura do rosto, cabelo e figurino em cada estilo, sem transformar um estilo em filtro 2D.

5. **Animação e integração**
   - Rig corporal completo e compatível com guitarra, violão acústico, baixo, bateria e vocal.
   - Expressões de canto, piscadas, sorriso, fala e reações.
   - Prévia 360°, zoom e troca de figurino em tempo real.
   - Exportação GLB com materiais, armature e animações; depois validar no renderizador Android.
   - Salvar e reabrir o personagem sem perder as escolhas.

## Critérios de aprovação

- Prévia frontal, lateral, traseira e 3/4 mostrando o corpo inteiro.
- Close-up do rosto e do cabelo com iluminação neutra.
- Teste de deformação em ombros, cotovelos, mãos, quadril e joelhos.
- Teste de clipping de cada peça e acessório.
- GLB reaberto em visualizador independente, sem cubos ou objetos estranhos.
- Relatório registra malhas, materiais, armature, shape keys e animações.
- Não aprovar com base apenas em compilação ou exportação do arquivo.
- Não substituir o estúdio atual por uma demonstração isolada.

## Limites e licenças

- Usar fotos públicas apenas como referência visual de pesquisa; não embutir fotos protegidas no APK nem copiar figurinos, logotipos ou marcas.
- Priorizar assets CC0 e ferramentas gratuitas. Registrar atribuição de qualquer asset CC-BY antes de redistribuir.
- Este documento é direção artística e critérios de validação; não declara que o modelo final já está pronto.


## Revisão da referência visual enviada em 23/11/2026

A imagem `Imagens/file_00000000afa8820e8132233143f51295.png` na `main` foi aberta e confirmada como a base visual indicada pelo usuário. Ela mostra um avatar humano 3D de corpo inteiro, com terno cinza-escuro de aparência corporativa, gravata listrada azul, sapatos marrons, cabelo curto penteado para trás e pose neutra com os braços afastados. Esta é a base de continuidade; não substituir por um personagem de outra versão nem reiniciar a criação do zero.

### Problemas observados na base
- O conjunto parece roupa social de escritório, não figurino de apresentação musical contemporânea.
- Gravata azul listrada e sapatos marrons quebram a unidade visual escura de palco.
- O cabelo está claro e muito assentado, sem o volume/textura escura desejada.
- O rosto ainda é genérico e não tem semelhança visual suficiente com a referência artística de Luan Santana.
- A pose é apenas neutra; o modelo precisa continuar sendo preparado para rig e animações musicais.

### Próxima revisão técnica
- Preservar a malha humana, escala, rig e continuidade do personagem atual.
- Testar cabelo curto texturizado castanho-escuro e conferir a cor no render final, não apenas no material.
- Harmonizar o figurino para palco em preto/grafite, com calçado preto e detalhes metálicos discretos; evitar aparência de uniforme corporativo e evitar elementos sertanejos caricatos.
- Ajustar rosto e cabelo com referência visual pública de forma respeitosa, mantendo um avatar original do jogo em vez de uma cópia fotográfica.
- Gerar vistas frontal, 3/4, lateral e traseira, além de close do rosto; validar o GLB e a deformação do rig.
- A primeira execução desta revisão é uma tentativa técnica de paleta e cabelo. Não declarar semelhança facial, figurino final ou aprovação antes de inspecionar as novas imagens e o GLB.


## Pesquisa complementar de cabelo — 23/11/2026

A revisão das referências públicas confirma que não existe um único corte fixo; o cabelo de Luan Santana mudou bastante entre fases. Não se deve tratar um asset curto genérico penteado para trás como correspondência suficiente.

### Referências editoriais consultadas
- R7, 06/06/2026 — notícia sobre o corte e a barba mais curtos, com retorno visual à fase do início da carreira: https://entretenimento.r7.com/prisma/fabiola-reipert/video/luan-santana-muda-visual-e-surpreende-fas-o-inicio-da-carreira-06062026/
- Gshow, 07/01/2019 — galeria com transformações, incluindo cabelo curto no início da carreira, fases mais longas e topete: https://gshow.globo.com/cultura-pop/famosos/noticia/luan-santana-exibe-novo-corte-de-cabelo.ghtml
- Gshow, 16/02/2021 — corte mais curto com topete e detalhe de cor no topo: https://gshow.globo.com/cultura-pop/famosos/noticia/luan-santana-muda-visual-para-marcar-nova-fase-na-carreira.ghtml
- UOL, 13/08/2014 — referência técnica do penteado pompadour: escovado para cima e para trás, sem espetar os fios: https://www.uol.com.br/universa/album/2014/08/13/sertanejos-revelam-o-que-fazem-para-manter-o-topete-nas-alturas.htm
- Capricho, 01/10/2017 — retrospectiva visual com topetes, franja, cabelo longo e penteados presos: https://capricho.abril.com.br/famosos/os-cortes-de-cabelo-mais-iconicos-do-luan-santana/

### Requisitos técnicos para corrigir o cabelo 3D
- Não aceitar apenas trocar o arquivo .mhclo: conferir a silhueta real no render frontal, 3/4, perfil e traseiro.
- Construir/ajustar volume superior com mechas agrupadas e direção coerente; criar transição gradual para laterais e nuca, linha frontal e costeletas naturais.
- Evitar aparência de capacete, bloco liso, cabelo excessivamente baixo/chapado, topete espetado genérico ou fios apontando aleatoriamente.
- Definir cor castanho-escura natural com variação sutil de tons; evitar reflexo ruivo/loiro exagerado, salvo se a fase escolhida justificar.
- O cabelo deve acompanhar a cabeça no rig, manter a silhueta em rotação e não atravessar testa, orelhas ou pescoço.
- Registrar no relatório o asset/malha escolhida, materiais, contagem de malhas e vistas renderizadas para comparação.
- Preservar o rosto e a malha-base já aprovados parcialmente; concentrar esta revisão no cabelo, sem regredir o rosto.

### Decisão de referência pendente
Como as fontes mostram cortes distintos, a escolha final da fase precisa ser confirmada pelo usuário antes de fixar a silhueta. A geração mais recente noticiada em junho de 2026 remete ao visual curto do começo da carreira; o pompadour de 2014 é outra opção histórica, com topo mais alto e penteado para trás. Não misturar essas duas referências em um penteado híbrido sem aprovação.
