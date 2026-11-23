# Escopo — Identidade de gravações e playlists

## Objetivo
Impedir duplicações acidentais de uma mesma gravação sem impedir que o jogador mantenha versões realmente diferentes da mesma música.

## Regras da biblioteca
- A mesma gravação não pode ser cadastrada duas vezes na biblioteca.
- A identificação não pode depender exclusivamente do título ou do artista.
- Hash criptográfico do arquivo (SHA-256) pode bloquear cópias binariamente idênticas.
- Arquivos com bytes diferentes não devem ser considerados automaticamente gravações diferentes: quando viável, avaliar uma impressão digital acústica/perceptual gratuita e de código aberto.
- A identificação perceptual deve ser conservadora. Se não houver confiança suficiente para distinguir cópia, recodificação e versão alternativa, não excluir nem sobrescrever automaticamente uma faixa existente.
- Não substituir a faixa original quando uma importação for reconhecida como duplicata.

## Regras das playlists
- A mesma gravação não pode aparecer mais de uma vez dentro da mesma playlist.
- A mesma gravação pode ser incluída em playlists diferentes.
- Versões distintas da mesma música podem coexistir na mesma playlist.
- Ao detectar uma duplicata dentro da playlist, preservar a lista e sua ordem, sem inserir uma segunda entrada.
- Mensagem obrigatória para duplicata na playlist: "Você já tem essa música na sua playlist. Gostaria de escolher outra?"

## Distinção entre versões
Devem ser aceitas gravações realmente diferentes, incluindo versões originais, feitas por fãs, ao vivo, acústicas, remasterizadas ou outras interpretações distintas. Título semelhante, duração próxima ou artista igual, isoladamente, não provam que duas faixas sejam a mesma gravação.

## Validação obrigatória
Criar testes para:
1. rejeitar uma cópia byte a byte idêntica na biblioteca;
2. permitir arquivos com títulos iguais quando forem gravações diferentes;
3. permitir versões distintas na mesma playlist;
4. rejeitar a mesma gravação repetida na mesma playlist;
5. permitir a mesma gravação em playlists diferentes;
6. garantir que uma rejeição não altere a ordem nem apague a faixa já cadastrada.

## Limites da implementação atual
A proteção inicial por SHA-256 detecta apenas arquivos binariamente idênticos. Ela não reconhece, por si só, uma mesma gravação recodificada em outro formato ou com metadados diferentes. A identificação acústica e o sistema de playlists ainda precisam ser implementados e testados.