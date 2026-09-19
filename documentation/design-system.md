# Sistema de design do Magna

Extraído da Home, que é a tela de referência escolhida em 2026-09-19 — ela toca quase todas as telas secundárias, então é onde a identidade já existia de fato.

Este documento é para ser aplicado, não descrito. A regra do bloco 11: **decidir antes de editar**, porque decidir durante a edição é exatamente como o app chegou ao estado que este bloco conserta.

---

## 1. Áreas

O app cobre seis partes da Câmara. Cada uma tem **ícone** e **acento**, em `ui/core/theme/MagnaArea.kt`.

| Área | Ícone | Acento |
|---|---|---|
| Deputados | `Groups` | `primary` (verde) |
| Partidos | `Flag` | `tertiary` (azul) |
| Proposições | `Description` | `secondary` (dourado) |
| Comissões | `AccountBalance` | teal `#2F6F68` / `#8FCFC4` |
| Votações | `HowToVote` | bronze `#8A5A33` / `#E5B78F` |
| Legislatura | `CalendarMonth` | `onSurfaceVariant` — sem cor própria |

**O ícone é o marcador, não a cor.** Seis ícones são inequívocos; seis cores não são. Num app sobre política a cor é lida como filiação, queira-se ou não — a própria paleta diz isso nos comentários dela: verde, dourado e azul, tirados da bandeira e descritos como não-partidários.

Daí três regras:

1. Quatro das seis áreas **reusam um papel que o tema já tinha**. Só duas cores foram inventadas, e as duas ficam longe de qualquer paleta partidária.
2. O acento nunca é preenchimento atrás de um partido ou de um deputado. Ele é ícone, título de seção e no máximo um filete.
3. A legislatura não tem cor. Ela é a moldura do resto, não mais uma coisa para olhar.

## 2. Espaçamento

`ui/core/theme/Dimensions.kt`, acessado por `LocalDimensions.current`. **Nenhum `.dp` cru em `features/` ou `ui/`.** Se o valor não existe na escala, ou ele vira token ou ele vira o token vizinho — o que não pode é ficar solto. Foi assim que apareceram os 31 de hoje.

Padding de borda de tela: `grid16`.

## 3. Tipografia por função

Um papel por função, o mesmo em todas as telas.

| Função | Papel |
|---|---|
| Título de seção | `titleLarge`, **Bold**, na cor da área — via `MagnaSectionHeader` |
| Título de tela | `MagnaMediumTopBar` |
| Nome de pessoa em lista | `bodyMedium` SemiBold |
| Rótulo de metadado | `labelSmall`, `onSurfaceVariant` |
| Corpo | `bodyMedium` |
| Nota de rodapé / ressalva | `labelSmall`, `onSurfaceVariant` |

`titleLarge.copy(color = primary, fontWeight = Bold)` estava escrito à mão em quatro componentes e `titleMedium` fazia o mesmo trabalho em outros três. **Expressão repetida é token que ainda não foi nomeado** — virou `MagnaSectionHeader`.

## 4. Cantos

`ui/core/theme/Shapes.kt`, agora passado ao `MaterialTheme`. Antes o tema passava só `colorScheme` e `typography`, então todo raio era ou o padrão do Material ou um `RoundedCornerShape` escrito na tela.

| Papel | Raio | Uso |
|---|---|---|
| `extraSmall` | 6 dp | chip, tag, selo |
| `small` | 10 dp | bloco dentro de card |
| `medium` | 16 dp | o card padrão |
| `large` | 20 dp | campo de busca, sheet |
| `extraLarge` | 28 dp | superfície grande |

Escala curta de propósito: raio que precisa ser consultado é raio que vai ser chutado.

## 5. Estados

Quatro, compartilhados, e ninguém escreve o seu:

| Estado | Componente |
|---|---|
| Carregando | `LoadingComponent` |
| Vazio | `EmptyComponent` (título obrigatório, descrição opcional) |
| Erro | `SomethingWentWrongComponent` |
| Offline | ainda não existe — o cache resolve quase tudo, mas a primeira visita sem rede não |

**Vazio não é erro e não é carregando.** Comissão sem votação, deputado sem voto nominal e legislatura sem dado baixado são fatos, e cada um já custou um giro infinito neste app.

## 6. Sem emoji

Removidos dos 13 pontos onde estavam. Eles renderizam diferente em cada aparelho, o leitor de tela anuncia o nome do emoji em voz alta, e "prédio, porta, telefone, envelope" não é o que um gabinete é. Ícone com rótulo diz a mesma coisa e diz igual em todo lugar.

---

## O que ainda falta neste bloco

- `MagnaScreen` — Scaffold + top bar + padding + comportamento de scroll padrão
- `MagnaSection` — regra de separação entre seções
- Comportamento de scroll: **zero** `scrollBehavior` ou `nestedScroll` no app hoje
- Transições de navegação: o `NavHost` usa o padrão, e não há nenhuma
- `MagnaLargeTopBar` está morto (0 usos). Ou a Home ganha top bar, ou ele sai
- `navigateBack` (23) contra `onBack` (11) — escolher um
- `background` e `surface` são a mesma cor no claro **e** no escuro, o que apaga a distinção entre fundo e card
- Aplicar o inventário do §14.2, tela por tela

## O teste de que o bloco funcionou

A próxima feature deve ser montável **sem escrever um `.dp`, sem escolher um papel tipográfico e sem desenhar um estado de erro.** Se ainda precisar, o sistema não ficou pronto — ficou documentado.
