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

### Marcador de área

A cor sozinha não separa seis áreas: seis acentos dentro de uma paleta institucional não são seis matizes distinguíveis, e quem não separa os verdes dos teais fica sem nada. Uma forma é lida antes de uma cor ser comparada, e sobrevive em escala de cinza.

| área | marcador |
|---|---|
| Deputados | faixa diagonal no canto superior direito |
| Proposições | faixa vertical de 4dp, recuada 4dp da borda esquerda |
| Votações | linha tracejada acima da borda inferior |
| Partidos, Comissões | ainda não definidos — não desenham nada |

Uma marca inventada para preencher um slot é uma marca que ninguém aprende. As duas em aberto ficam sem marcador até serem decididas.

Cada uma ocupa uma **aresta diferente**, não um tratamento diferente da mesma: um tracejado ao lado de uma faixa sólida seriam duas marcas para comparar, e três lugares para olhar se leem antes disso.

**O tracejado de votações ainda não desenha em lugar nenhum**, porque votações não tem cards na Home — é uma tela-folha, alcançada pelo voto de um deputado ou pela proposição. A forma está definida e testada; falta onde pô-la.

**As duas são faixas, não bordas.** Nenhuma encosta na aresta que acompanha: sobra sempre uma tira da cor do próprio card do lado de fora dela. Uma marca que corre até a borda é lida como a borda — moldura, divisor, estado de seleção. Com a superfície ainda aparecendo por fora, a faixa é lida como algo posto sobre o card, que é o que ela é.

O recuo é para o lado, não para as pontas. A diagonal para antes da quina; a vertical das proposições foi empurrada 4dp para dentro e **mantém a altura inteira**. Cortá-la em cima e embaixo transformou-a num fragmento flutuando no card — o que tira o ar de moldura é a tira de superfície por fora, não o encurtamento.

Na diagonal isso são quatro pontos em vez de três: a ponta do triângulo é cortada fora. O recuo se mede contra a quina, não contra a aresta — `shapes.medium` arredonda em 12dp e o ponto do arco mais próximo da quina está a uns 5dp na diagonal, então recuo abaixo de dez o clip come inteiro e a faixa volta a parecer canto cheio. Hoje são 14dp de recuo e 10dp de espessura medida na aresta, que o corte diagonal transforma nos ~7dp que o olho mede.

**Fica atrás do conteúdo, não sobre ele.** No card de deputado a faixa passa por trás da foto. Isso obriga o modificador a ser aplicado no contêiner de dentro do `Card` e antes do padding dele: o `Card` pinta o fundo dentro da própria cadeia de modificadores, então o que for desenhado a partir do modificador do `Card` fica coberto por ele.

**Onde aparece:** nos itens da Home, que é onde as áreas se misturam e um card precisa dizer de qual delas é.

**Onde não aparece:** nas linhas de lista dentro da própria feature. Numa lista em que toda linha é da mesma área, o marcador não diz nada e se repete quarenta e cinco vezes.

Esteve no topbar da feature e saiu: a barra desenha de borda a borda, então o canto superior direito dela fica sob a barra de status e a faixa caiu em cima do ícone de bateria. Levar a identidade para dentro da feature é um problema separado — a barra já toma a cor da área.

### A cor de votações

Verde de deputados levado ao dourado de proposições — que é o que um voto é: as pessoas agindo sobre um projeto. Era um bronze, inventado para ser um matiz quente e não para significar algo.

Matiz 85: 75° do verde de deputados e 39° do dourado de proposições, então é lido como cor própria e não como filha de nenhum dos dois. `#547722` no claro (4.69:1 num card) e `#B4D289` no escuro (8.82:1).

### Cor que não é área: o voto

Uma exceção, e é a única. `Sim` e `Não` num card de voto são separados por cor além da palavra, no par que o painel do próprio plenário usa — verde e vermelho. A palavra continua sendo o sinal principal, então a tag lê sem visão de cor nenhuma; a cor é para quem varre a lista em vez de ler linha a linha.

O que ela **não** diz: se a votação passou. Isso é um fato separado e não é desenhado contra a pessoa. Também não é filiação — é o voto daquele dia, numa matéria, e está escrito ao lado.

`Abstenção`, `Obstrução` e `Artigo 17` são neutros. Nenhum deles é um voto, e dar cor a cada um seriam cinco cores para a única pergunta que se faz olhando a tag. O mapeamento fica em `tomDoVoto`, que a contagem sob a aba também usa — a soma e a tag não podem discordar sobre o que foi um voto.

## 2. Espaçamento

`ui/core/theme/Dimensions.kt`, acessado por `LocalDimensions.current`. **Nenhum `.dp` cru em `features/` ou `ui/`.** Se o valor não existe na escala, ou ele vira token ou ele vira o token vizinho — o que não pode é ficar solto. Foi assim que apareceram os 31 de hoje.

Padding de borda de tela: `grid16`.

## 3. Tipografia por função

Um papel por função, o mesmo em todas as telas.

| Função | Papel |
|---|---|
| Título de seção | `titleLarge`, **Bold**, na cor da área — via `MagnaSectionHeader` |
| Título de tela | `MagnaMediumTopBar` — **Light**, `onSurface` |
| Nome de pessoa em lista | `bodyMedium` SemiBold |
| Rótulo de metadado | `labelSmall`, `onSurfaceVariant` |
| Corpo | `bodyMedium` |
| Nota de rodapé / ressalva | `labelSmall`, `onSurfaceVariant` |
| Legenda de bloco | `labelSmall` |
| Valor principal de um bloco | `bodyMedium` SemiBold |
| Valor secundário de um bloco | `bodySmall` |

Os três últimos são o cartão de contato do detalhe de deputado, e existem porque quatro linhas do mesmo tamanho é a forma do registro impressa em vez da do leitor. Uma legenda diz de que é o bloco, um valor carrega e os outros acompanham.

### Barra de topo não é cabeçalho de seção

A barra diz **onde você está**; a seção diz **de que área** ela é. Eram a mesma coisa em dois tamanhos: título de tela em `headlineLarge` **Bold** na cor `primary`, título de seção em `titleLarge` **Bold** na cor da área — e `MagnaArea.DEPUTADOS.accent` é `primary`. Mesmo verde, mesmo peso, 32sp contra 22sp.

A separação usa quatro eixos e nenhuma fonte nova:

| | peso | cor | ícone |
|---|---|---|---|
| Barra de topo | Light | `onSurface` | nenhum |
| Cabeçalho de seção | Bold | cor da área | o da área |

**Uma família só.** Magna Sans Display já ocupa 785 KB em seis pesos, e o app renderizava Bold nos dois papéis — ExtraLight, Light e SemiBold estavam praticamente parados. Uma segunda família aproximadamente dobraria esse peso para comprar uma distinção que peso e cor dão de graça. Uma segunda voz tipográfica se justifica quando existem duas vozes de verdade, produto e texto editorial; este app não tem texto longo, e o título da Home é um dado — `57ª legislatura` — não uma marca.

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

## 5. Fundo, superfície e elevação

`background` é o creme em que o app se apoia; `surface` é o que fica **em cima** dele. Eram o mesmo valor, no claro e no escuro — então card não tinha de que se destacar, e a elevação de todo `Card` do app estava em zero. Os dois fatos eram o mesmo fato.

| | Claro | Escuro |
|---|---|---|
| `background` | `#FFFCF4` | `#1A1C1E` |
| `surface` | `#FFFFFF` | `#212427` |

Elevação em `ui/core/theme/Elevation.kt`, via `magnaCardElevation()`:

| Papel | Valor |
|---|---|
| Card | **1 dp** |
| Pressionado | 3 dp |

Um dp lê como sombra, não como painel flutuante. A tela é cheia de card; mais que isso vira pilha de azulejo.

**Fundo de tela usa `background`, nunca `surface`.** Duas telas pintavam o próprio fundo com `surface`, o que era invisível enquanto as duas cores eram iguais e virou um bloco branco no meio do creme assim que deixaram de ser. Quem usa `surface` é o que se sobrepõe: sheet, card, diálogo.

### Superfície é da mesma temperatura que o fundo

A família `surfaceContainer` era azul-acinzentada — medido, hue **216 a 220** — contra um `background` creme de hue **44**. Cento e setenta e dois graus de distância: todo card do app era um card frio sobre uma página quente, e o contraste entre os dois é de apenas **1.10**, então não havia luminosidade separando-os, só temperatura brigando.

O resultado lia como "sem vida", e a causa não era o cinza ser apagado: era ele ser de outra família que a página.

A escala foi reafinada para hue 45, mantendo os passos de luminosidade:

| token | antes | agora |
|---|---|---|
| `surfaceContainerLow` | `#F4F6FA` | `#F9F8F6` |
| `surfaceContainer` | `#EEF2F8` | `#F5F3ED` |
| `surfaceContainerHigh` | `#E8EDF5` | `#F0EDE5` |
| `surfaceContainerHighest` | `#E2E8F2` | `#ECE8DD` |
| `surfaceVariant` | `#E2E2E8` | `#E5E3DC` |

O modo escuro não tinha o problema: fundo e containers já eram hue 210.

## 6. Andaime de tela

`MagnaScreen` — Scaffold, top bar, e o comportamento de scroll. Uma tela nova começa com a identidade pronta em vez de ser montada à mão outra vez.

```kotlin
MagnaScreen(
    title = stringResource(Res.string.titulo),
    navigateBack = navigateBack,
    belowTopBar = { /* aba, filtro */ },
) { paddingValues -> /* conteúdo */ }
```

- `navigateBack` nulo não desenha seta, para tela que não foi empilhada em nada.
- `belowTopBar` fica sob a barra e colapsa junto: é onde vive uma `PrimaryTabRow`.
- A barra usa **`enterAlwaysScrollBehavior`**, não `exitUntilCollapsed`: ela volta no primeiro scroll para cima, em vez de obrigar a subir uma lista inteira para saber onde se está.

O callback de voltar chama-se **`navigateBack`** em todo lugar. Eram 23 contra 11 `onBack`.

`MagnaLargeTopBar` foi apagado. Tinha zero usos fora dos próprios previews, e dar top bar à Home é decisão de desenho que ainda não foi tomada — manter os dois sem usar um era o pior dos três caminhos.

## 7. Estados

Quatro, compartilhados, e ninguém escreve o seu:

| Estado | Componente |
|---|---|
| Carregando | `LoadingComponent` |
| Vazio | `EmptyComponent` (título obrigatório, descrição opcional) |
| Erro | `SomethingWentWrongComponent` |
| Offline | ainda não existe — o cache resolve quase tudo, mas a primeira visita sem rede não |

**Vazio não é erro e não é carregando.** Comissão sem votação, deputado sem voto nominal e legislatura sem dado baixado são fatos, e cada um já custou um giro infinito neste app.

## 8. Sem emoji

Removidos dos 13 pontos onde estavam. Eles renderizam diferente em cada aparelho, o leitor de tela anuncia o nome do emoji em voz alta, e "prédio, porta, telefone, envelope" não é o que um gabinete é. Ícone com rótulo diz a mesma coisa e diz igual em todo lugar.

---

## O que ainda falta neste bloco

- `MagnaSection` — regra de separação entre seções
- Aplicar `MagnaScreen` nas telas que faltam: Home, busca, detalhe do deputado (usa `BottomSheetScaffold`) e detalhe do partido
- Transições de navegação: o `NavHost` usa o padrão, e não há nenhuma
- Aplicar o inventário do §14.2, tela por tela

## Regra de seção

Título por `MagnaSectionHeader`, com o ícone da área. Quando a seção tem um "Ver todas", o cabeçalho leva `Modifier.weight(1f)` no Row externo: sem isso um título longo empurra o botão para duas linhas.

**Seção nunca reserva altura.** A de proposições usava `Box(height = 380.dp)` para caber cinco cards; o filtro padrão tinha **uma** proposição na janela, então a Home abria com um card e 250 dp de nada. Altura fixa é aposta sobre quantos itens existem, e a Câmara não coopera: na mesma janela de três meses PEC teve 1, MPV 24, PLP 62 e PL 2074.

**Quando o número varia assim, ele vai para a tela.** A seção diz quantas existem contra as cinco que mostra. Lista de cinco sem número ao lado parece a mesma coisa sendo tudo o que há ou um vigésimo de um por cento.

## O teste de que o bloco funcionou

A próxima feature deve ser montável **sem escrever um `.dp`, sem escolher um papel tipográfico e sem desenhar um estado de erro.** Se ainda precisar, o sistema não ficou pronto — ficou documentado.
