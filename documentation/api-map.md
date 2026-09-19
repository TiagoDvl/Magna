# Mapa da API da Câmara — o que o Magna usa

Entregável do **bloco 7** do plano da 1.1 (`docs/review-v1.1.md`, seção 10).

Verificado contra `https://dadosabertos.camara.leg.br/api/v2` em **2026-09-19**. Tudo aqui foi medido com requisição real, não lido de documentação — a coluna de custo e os limites de intervalo só aparecem assim.

O objetivo é responder, sem abrir o navegador: *para mostrar X na legislatura Y, quais chamadas são feitas, com quais parâmetros, e o que fica no banco depois.*

---

## 0. Leia isto antes de confiar em qualquer linha abaixo

**Este documento é um retrato, não um contrato.**

A API da Câmara **não se comporta igual em todas as legislaturas**. O mesmo endpoint, com os mesmos parâmetros, pode devolver campo que existe numa legislatura e é nulo em outra, paginar diferente, mudar de volume em ordens de grandeza, ou simplesmente não ter o dado. Boa parte do que está aqui foi medido na **legislatura 57**, a atual, e o pouco que foi conferido em legislaturas anteriores já mostrou diferença — a paginação de `/deputados`, por exemplo, só quebra da 55 para trás, e a composição de comissão muda de forma conforme a janela de datas pedida.

Some-se a isso que a própria API muda com o tempo, sem aviso e sem versionamento visível.

Então a regra de trabalho é:

> **Ao começar a mexer em qualquer domínio do app, rode você mesmo alguns exemplos dos endpoints daquele domínio, na legislatura em que o trabalho vai acontecer, antes de confiar no que está escrito aqui.**

Não é desconfiança do documento: é que a pergunta "isso vale para a legislatura X?" só tem uma resposta honesta, e ela custa três requisições. A seção 8 tem o método — em resumo, o campo `instance` do erro 400 nomeia o parâmetro recusado, e o link `rel="last"` dá o volume sem baixar nada.

Quando uma medição aqui não bater com a realidade, **corrija esta linha e anote a legislatura**, em vez de contornar no código. Retrato desatualizado que ninguém corrige vira armadilha, e este documento existe justamente para acabar com uma delas.

Data e escopo de cada medição estão junto do número. Onde não houver legislatura indicada, leia "legislatura 57, em 2026-09-19".

---

## 1. Tabela mestre

| Endpoint | Parâmetros usados hoje | Quem chama | Vai para o banco | Custo por abertura |
|---|---|---|---|---|
| `GET /deputados` | `idLegislatura` | `DeputadosRepository.kt:152` | `Deputado` (com `legislaturaId`) | 1 |
| `GET /deputados/{id}` | — | `DeputadosRepository`, `PartidosRepository.fetchBios` | `DeputadoDetails` (com `legislaturaId`), `DeputadoBio` (**sem**) | 1 por deputado ainda não guardado |
| `GET /deputados/{id}/despesas` | `idLegislatura`, `ano`, `ordem`, `ordenarPor`, `itens=100` | `DeputadosRepository.kt:140` | `DeputadoExpense` (com `legislaturaId`) | 1 |
| `GET /partidos` | `idLegislatura`, `itens=100` | `PartidosRepository.kt:45` | `Partido` (com `legislaturaId`) | 1 |
| `GET /partidos/{id}` | — | `PartidosRepository.kt:82` | não persiste | 1 |
| `GET /partidos/{id}/membros` | `idLegislatura`, `itens=100`, `pagina` | `PartidosRepository.fetchRoster` | não persiste | 1 por página |
| `GET /proposicoes` | `dataApresentacaoInicio`, `dataApresentacaoFim`, `ordem=desc`, `siglaTipo?` | `ProposicoesRepository` | `Proposicao` (com `legislaturaId`) | 1 |
| `GET /proposicoes/{id}` | — | `ProposicoesRepository.kt:71,111` | `Proposicao` | 1 por proposição |
| `GET /proposicoes/{id}/autores` | — | `ProposicoesRepository.kt:88,112` | campo de texto em `Proposicao` | 1 por proposição |
| `GET /proposicoes/{id}/votacoes` | — | declarado, sem uso hoje | — | — |
| `GET /referencias/proposicoes/siglaTipo` | — | `ProposicoesRepository.kt:34` | `SiglaTipo` | 1 |
| `GET /orgaos` | `codTipoOrgao=2` | `OrgaosRepository.kt:29` | `Orgao` (**sem** `legislaturaId`) | 1 |
| `GET /votacoes` | `idOrgao`, `ordenarPor`, `itens=20` | `OrgaosRepository.kt:58` | não persiste | 1 |
| `GET /votacoes/{id}` | — | `OrgaosRepository.kt:62` | não persiste | 1 por votação |
| `GET /legislaturas` | — | **nenhum** (removido no bloco 5) | `Legislatura` (tabela vazia) | — |

### Custo real por tela

O número acima é por chamada. O que a tela gasta é a soma, e três delas fazem leque:

| Tela | Requisições | Conta |
|---|---|---|
| Proposições recentes | **31** | 1 lista + 15 × (detalhe + autores) — `ProposicoesRepository.kt:104-113` |
| Detalhe da comissão | **21** | 1 lista de votações + 20 detalhes — `OrgaosRepository.kt:58-63` |
| Detalhe do partido, 1ª visita | **146** no PL | 1 partido + 2 páginas de membros + 143 × `deputados/{id}`, limitado por `Semaphore` |
| Detalhe do partido, revisita | **3** | 1 partido + 2 páginas de membros; a biografia sai de `DeputadoBio` |
| Lista de deputados | 1 | — |
| Despesas do deputado | 1 | — |

---

## 2. Escopo por legislatura — quem aceita o quê

Esta é a pergunta que o bloco 8 depende, e a resposta não é uniforme.

| Endpoint | `idLegislatura` | `dataInicio`/`dataFim` | Como escopar |
|---|---|---|---|
| `/deputados` | **sim** | sim | `idLegislatura` |
| `/deputados/{id}/despesas` | **sim** | — (usa `ano`) | `idLegislatura` + `ano` |
| `/partidos` | **sim** | — | `idLegislatura` |
| `/partidos/{id}/membros` | **sim** | — | `idLegislatura` |
| `/proposicoes` | **HTTP 400** | sim, mas filtram **tramitação** — ver item 3.7 | `dataApresentacaoInicio`/`dataApresentacaoFim`, janela de 3 meses |
| `/orgaos` | **HTTP 400** | sim, mas ver item 3.3 | não dá pelo endpoint |
| `/orgaos/{id}/membros` | **HTTP 400** | sim | cada registro traz `idLegislatura`; filtrar no cliente |
| `/votacoes` | **HTTP 400** | sim, **máximo 3 meses** | ver item 3.2 |

O 400 vem no formato `{"status":400,"instance":"idLegislatura","detail":"Parâmetro(s) inválido(s)."}` — o campo `instance` nomeia o parâmetro recusado, o que torna a verificação barata de repetir.

### As datas saem da própria API

`GET /legislaturas?itens=100` devolve **as 57 legislaturas numa página só**:

```json
{"id":57,"dataInicio":"2023-02-01","dataFim":"2027-01-31"}
{"id":56,"dataInicio":"2019-02-01","dataFim":"2023-01-31"}
{"id":55,"dataInicio":"2015-02-01","dataFim":"2019-01-31"}
```

A tabela `Legislatura` no banco já tem `id`, `startDate` e `endDate`. **Nenhuma data precisa ser hardcoded em lugar nenhum**, e isso também resolve o `CURRENT_YEAR = 2026` fixo de `PartidoDetailsViewModel.kt:27`.

---

## 3. Pegadinhas da API

As que custaram uma requisição para descobrir e custariam um bug para redescobrir.

### 3.1 `ano` sozinho não funciona em despesas

`GET /deputados/{id}/despesas?ano=2026` devolve **zero itens**. O mesmo pedido com `idLegislatura=57&ano=2026` devolve os dados. O parâmetro `ano` só é considerado quando acompanhado de `idLegislatura`.

Medido em três deputados da 57: com `idLegislatura`, 100 itens para 2024, 2025 e 2026; sem, zero em todos os anos. (Um dos três não tem despesa em 2025/2026 — mandato encerrado, não falha da API.)

### 3.2 `/votacoes` recusa intervalo maior que 3 meses

Mensagem literal: `A diferença entre as datas não pode ser maior que 3 meses`.

Consequência direta: **não existe "votações da legislatura" em uma chamada.** Um quadriênio precisa de 16 janelas de 3 meses. Qualquer feature de votações escopada por legislatura precisa assumir esse custo ou mudar de recorte.

### 3.3 `codTipoOrgao` e as datas não convivem

- `/orgaos?codTipoOrgao=2&itens=100` → **30 comissões permanentes**, uma página.
- `/orgaos?dataInicio=2019-02-01&dataFim=2023-01-31` → 200, mas devolve órgãos de todos os tipos (84 páginas).
- `/orgaos?codTipoOrgao=2&dataInicio=...&dataFim=...` → 200 com **zero itens**.

Ou seja: dá para filtrar por tipo, dá para filtrar por data, e as duas juntas não devolvem nada. **Escopar comissões por legislatura via `/orgaos` não funciona.** O caminho é `/orgaos/{id}/membros`, onde cada registro traz `idLegislatura`, `dataInicio` e `dataFim` próprios.

### 3.4 Paginação: cada endpoint tem um padrão diferente

| Endpoint | `itens` padrão | Exemplo real |
|---|---|---|
| `/deputados` | **1000** | legislatura 57 → 879 numa página; legislatura 55 → 1138 em **duas** |
| `/partidos/{id}/membros` | **15** | partido 36844 na 57 → 6 páginas |
| `/proposicoes` | **15** | `ordem=desc` sem filtro → 1311 páginas |
| `/orgaos` | 15 | com `itens=100`, as 30 permanentes cabem numa página |
| `/orgaos/{id}/membros` | 15 | CCJ (2003) → ~130 registros |
| `/votacoes` | 15 | CCJ com `itens=20` → 21 páginas |

O link `rel="last"` de cada resposta carrega o número da última página. É a forma barata de medir custo sem baixar tudo.

### 3.5 `Accept-Charset` derruba tudo com 403

Já registrado no plano (seção 16.1) e corrigido em `3897905`, mas pertence a este documento: o gateway responde **403 em qualquer requisição** que carregue o header `Accept-Charset`, com qualquer valor. O Ktor instala `HttpPlainText` por padrão e o carimba sozinho; a remoção tem que acontecer no send pipeline.

### 3.6 Voto nominal: a relação só existe num sentido, e `itens` a esconde

`GET /deputados/{id}/votos` e `GET /deputados/{id}/votacoes` devolvem **405**. Não existe caminho deputado → votos. O único acesso é `GET /votacoes/{id}/votos`.

E esse endpoint **recusa `itens` com HTTP 400**. Sem parâmetro nenhum ele devolve todos os votos de uma vez, sem paginação (`links` só traz `self`). Quem tenta paginar leva erro ou lista vazia e conclui que não há voto nominal registrado — é a armadilha mais cara do mapa inteiro.

Vale saber a proporção antes de desenhar em cima: em 2026, de **7.360 votações, só 152 têm voto nominal** (2%), concentradas em 44 dias do ano. Detalhes e a estratégia de sincronização estão no bloco 10 do plano (seção 13).

### 3.7 `dataInicio`/`dataFim` em `/proposicoes` não filtram por apresentação

Esta corrige uma afirmação errada das primeiras versões deste documento e do plano.

`/proposicoes?dataInicio=2018-11-01&dataFim=2019-01-31` responde 200, e é fácil concluir que filtra proposições apresentadas naquele intervalo. **Não filtra.** Pedindo a mesma janela com `ordem=asc`, as primeiras respostas são:

| id | `dataApresentacao` |
|---|---|
| 14016 | 1998-03-04 |
| 14244 | 1999-03-02 |
| 14257 | 1991-06-05 |

Ou seja: o par `dataInicio`/`dataFim` filtra por **tramitação** — proposições que *se moveram* naquele período, independentemente de quando foram apresentadas. Para uma janela de legislatura isso é a pergunta errada.

**Os parâmetros certos existem e se chamam `dataApresentacaoInicio` e `dataApresentacaoFim`.** A mesma janela com eles devolve o que se esperava: 2018-12-10, 2018-12-11, 2018-12-12.

Três detalhes que vêm junto:

- **o teto de intervalo vale aqui também.** Três e quatro meses passaram; seis e doze devolveram `A diferença entre as datas não pode ser maior que 3 meses`. Uma legislatura inteira não é uma janela que dê para pedir, então "as proposições do mandato" não existe numa chamada;
- **`ordenarPor=dataApresentacao` é recusado com 400.** O `ordem=desc` que o app manda ordena por **id**, que é o padrão. Ids crescem junto com a apresentação de perto o bastante para pegar uma fatia recente, mas não é a mesma coisa e não deve ser tratado como se fosse;
- **`ano` não é o ano de apresentação.** É o ano do número da proposição: `ano=2018` devolve itens com `dataApresentacao` em 2019-05-16. Serve para achar "PL 1234/2018", não para recortar um período.

Vale como lembrete do item 0: um 200 não quer dizer que o parâmetro faz o que o nome sugere. A verificação que pega isso é pedir a mesma janela com `ordem=asc` e olhar o campo que deveria estar filtrado.

---

## 4. O que o mapa revelou como trabalho

### 4.0 [ALTO] `legislaturaId` era coluna e não chave — CORRIGIDO

Não é pegadinha da API; é do banco, mas é o que a medição da API escancarou, então fica registrado aqui.

`Deputado`, `Partido` e `DeputadoDetails` tinham `PRIMARY KEY` só no id, com `legislaturaId` como coluna comum. Como o upsert sobrescrevia a coluna, cada reeleito era **uma linha** que pertencia à legislatura sincronizada por último.

Medido em 2026-09-19 percorrendo `/deputados` página a página:

| | 57 | 56 | em comum |
|---|---|---|---|
| deputados | 648 | 613 | **332** |

Trocar 57 → 56 tirava 332 pessoas da 57. Voltar encontrava 316 e não re-sincronizava, porque a lista não estava vazia — estava pela metade, que é o estado que nada no app sabia distinguir de "já baixei tudo". Os 27 partidos existem em toda legislatura, mesmo defeito.

Corrigido na migração `4.sqm`, que recria as três com chave `(id, legislaturaId)` e move `last_seen` para `DeputadoLastSeen`.

### 4.1 [ALTO] Membros do partido são truncados em 15 — CORRIGIDO

`PartidosApi.getPartidoMembros` (`PartidosApi.kt:24`) não manda `itens`, e o padrão do endpoint é 15. O partido 36844 na legislatura 57 tem **6 páginas**; a tela mostra a primeira e não indica que há mais.

Isso não é problema futuro: **está em produção hoje, na legislatura atual.**

Medido em 2026-09-19, com `itens=100` em todos os 27 partidos da 57:

| | membros na 57 |
|---|---|
| PL | **145** (2 páginas) |
| UNIÃO | **mais de 100** (2 páginas) |
| REPUBLICANOS | 84 |
| PSD | 81 |
| PT | 80 |

Dois pontos que a medição fechou:

- **`itens` tem teto de 100.** Pedir `itens=200` devolve 100 e um `rel=next`. Então `itens=100` não é atalho suficiente: paginar é obrigatório.
- **Os números passam do total de cadeiras** porque o endpoint devolve todo mundo que passou pelo partido durante a legislatura, não a bancada de hoje.
- **O endpoint repete pessoas, e não é artefato de paginação.** A página 1 do PL na 57 devolve **100 linhas com 99 deputados**; a página 2 devolve 45 linhas com 44. Quem sai do partido e volta dentro da legislatura ganha uma linha por período. Sem deduplicar por id, o PL tem 145 linhas para 143 pessoas — e os gráficos contam linhas.

Interseção entre partidos, medida na 57, porque é o que decide se guardar biografia compartilha trabalho entre telas:

| par | em comum |
|---|---|
| PL ∩ UNIÃO | 11 |
| PL ∩ PP | 7 |
| PL ∩ PT | 0 |

Existe, e vem de quem trocou de partido no meio do mandato — mas é pequena. Guardar biografia se paga pela **revisita ao mesmo partido**, não pela partilha entre partidos.

O leque de `deputados/{id}` continua sendo o problema caro: os campos que a tela de partido usa nos gráficos — `siglaSexo`, `dataNascimento`, `ufNascimento`, `municipioNascimento` — **não vêm na listagem de membros**, só no detalhe individual. Paginar sem resolver isso troca 16 requisições por 146 no PL.

Dois caminhos, e o segundo é novo:

1. **Persistir a biografia.** Os quatro campos não mudam e não dependem de legislatura, então cabem numa tabela própria preenchida sob demanda. Primeira abertura do PL custa 145 requisições, as seguintes custam zero, e o custo é pago uma vez por pessoa em vez de uma vez por tela.
2. **`arquivos/deputados/csv/deputados.csv`** (ver seção 6.1): **1,3 MB, 7889 linhas, todos os deputados da história**, com exatamente esses quatro campos. Um download substitui o leque inteiro, em qualquer legislatura. Cai nas regras de download do bloco 10 do plano — pedir permissão avisando o peso, marcar validade —, então não é decisão do item 4.1.

**Feito o caminho 1.** `getPartidoMembros` manda `itens=100` e segue `links[rel=next]` até acabar, com teto de 10 páginas para o caso da API mudar de ideia. A biografia vai para `DeputadoBio` (migração `5.sqm`), tabela **sem `legislaturaId`**, porque nada ali muda entre mandatos. O leque passou a pedir só quem ainda não está guardado.

Custo antes e depois, no PL da 57:

| | antes | primeira visita | revisita |
|---|---|---|---|
| roster | 1 requisição, 15 de 143 membros | 2 requisições, 143 membros | 2 |
| biografia | 15 | 143 | **0** |

O caminho 2 continua valendo e agora é barato de encaixar: ele só preenche a mesma tabela por outra porta.

### 4.2 [ALTO] A segunda página de deputados some em legislaturas antigas — CORRIGIDO

`getDeputados` não mandava `itens` nem paginava. O padrão de `/deputados` é 1000, então a 57 (879 deputados) cabia numa página e o problema não aparecia. **A 55 tem 1138 e ocupa duas páginas** — o app gravaria 1000 e perderia 138 sem erro nenhum.

Era o bug que o bloco 8 destravaria no dia em que a troca de legislatura entrasse no ar, e foi o que aconteceu: o seletor tornou a 55 alcançável em dois toques.

**Corrigido** seguindo o link `rel="next"` em vez de contar itens — os endpoints não concordam num tamanho de página padrão, então comparar o tamanho com o que foi pedido erra para algum deles. A coleta acumula tudo antes de gravar, para que uma legislatura entre inteira ou não entre: gravar a primeira página e reportar falha deixaria o mesmo estado que o bug original. Há um teto de 10 páginas, que existe só para o caso de um `next` que nunca some.

### 4.3 [MÉDIO] As seis comissões fixas contra as 30 que a API devolve

`/orgaos?codTipoOrgao=2` devolve **30 comissões permanentes**. `MagnaComissaoPermanente` (`data/repository/orgaos/params/`) fixa seis `idOrgao` na mão.

**A curadoria é decisão de produto, e é deliberada:** as seis foram escolhidas por serem reconhecíveis pelo nome e por serem, à época, as que tinham mais votações — o objetivo era não encher a tela de comissão vazia. O problema não é a decisão; é ela estar codificada como se fosse dado, dentro de `data/`, sem o critério escrito em lugar nenhum (ver também o item 4.6 do plano).

O critério foi medido em 2026-09-19, contando votações por órgão via `/votacoes?idOrgao={id}&itens=1` e lendo o total no link `rel="last"`:

| Comissão | Votações | Está no app? |
|---|---|---|
| CCJC — Constituição e Justiça | **410** | sim |
| CCOM — Comunicação | **271** | sim |
| CSPCCO — Segurança Pública | **83** | sim |
| CPOVOS — Amazônia e Povos Originários | 45 | não |
| CSAUDE — Saúde | 42 | sim |
| CCULT — Cultura | 38 | não |
| CVT — Viação e Transportes | 37 | não |
| CE — Educação | 35 | não |
| CPD — Pessoas com Deficiência | 33 | não |
| CFT — Finanças e Tributação | 32 | não |
| CREDN — Relações Exteriores | 32 | não |
| CCTI — Ciência, Tecnologia e Inovação | 16 | **sim** |
| CAPADR — Agricultura | 7 | **sim** |
| … | … | … |
| CASP — Administração e Serviço Público | **0** | não |

A intuição original acertou o topo: as três primeiras do app são as três primeiras da lista. As duas últimas envelheceram — **CCTI (16) e CAPADR (7) hoje estão atrás de oito comissões que o app não mostra**, entre elas CPOVOS (45) e CCULT (38).

E o medo que motivou a curadoria é real e mensurável: CASP tem **zero** votações, e várias outras ficam abaixo de dez.

A conclusão não é "mostrar as 30". É que **o critério pode ser dado em vez de constante**: ordenar por atividade e cortar por limiar mantém a intenção de produto e para de congelar um retrato de 2023 dentro do código.

(Contagem de toda a série disponível, não só da legislatura 57 — é o que explica a distância da CCJC.)

### 4.3.1 [ALTO] Comissão sem votação carrega para sempre — CORRIGIDO

`ComissaoPermanenteDetailScreen.kt:73` usava `if (state.votacoes.isEmpty())` para decidir mostrar `LoadingComponent`. Não existia estado de lista vazia: uma comissão sem votação nenhuma ficava girando indefinidamente.

**Eram três caminhos para o mesmo spinner eterno, não um.** Além da lista vazia:

- **falha de rede**: o ViewModel gravava `isError = true` e **nenhum ramo da tela lia esse campo**;
- **órgão não encontrado**: o `init` fazia `return@launch` sem tocar no estado. É alcançável de verdade — a lista de comissões é filtrada por legislatura desde o bloco 8, então uma comissão criada depois do fim do mandato selecionado não está nela.

Corrigido com `VotacoesState` (`Loading` / `Empty` / `Error` / `Content`), no formato que o `CLAUDE.md` define para estado assíncrono, montado pela função pura `votacoesStateFor(Result<List<Votacao>>)`. `Content` carrega os próprios totais de aprovadas e rejeitadas, que antes eram calculados ao lado do ramo — num lugar onde lista vazia nunca chegava.

Nasceu daí o **`EmptyComponent`**, que o app não tinha. A ausência dele é o que empurrava telas para o `if (lista.vazia) carregando`, e o plano já previa que o bloco 9 precisaria dele (seção 14 do plano).

Continua valendo o aviso original: **qualquer mexida na curadoria do item 4.3 dependia disto** — CASP tem zero votações.

### 4.4 [MÉDIO] 31 requisições para 15 proposições

`refreshProposicoes` busca a lista e dispara detalhe + autores por item (`ProposicoesRepository.kt:104-113`). São 31 chamadas para montar uma seção da Home. O `supervisorScope` já evita que uma falha derrube as outras, e o bloco 4 já arrumou o cancelamento — o que sobra é a quantidade.

Vale checar se `/proposicoes` sozinho já traz o suficiente para a lista, deixando detalhe e autores para quando a pessoa abre a proposição.

### 4.5 [MÉDIO] `/votacoes` com `itens=20` e `ordenarPor` sem motivo escrito

`VotacoesApi.getVotacoesFromOrgao` (`VotacoesApi.kt:13`) fixa `itens=20` e `ordenarPor=idProposicaoObjeto`. São 21 páginas disponíveis para a CCJ. O `20` é o que produz as 21 requisições da tela de comissão, e nada explica a escolha — nem o número, nem a ordenação por id de proposição num lugar onde a tela mostra por data.

### 4.6 [BAIXO] `/proposicoes/{id}/votacoes` existe e não é usado

Declarado em `ProposicoesApiInterface`, sem chamador. Ou vira feature, ou sai junto do próximo lote de código morto.

### 4.7 [MÉDIO] Não existe tamanho de bancada que sirva para legislatura antiga — CORRIGIDO

Achado ao implementar favoritos de partido, porque ordenar exige um tamanho.

Três medições, todas em 2026-09-19:

- **`/partidos` não devolve tamanho nenhum.** O item da lista tem só `id`, `nome`, `sigla`, `uri`. E vem em ordem alfabética por sigla.
- **`/partidos/{id}` devolve `status.totalMembros`**, mas só da legislatura corrente: `status.idLegislatura` volta `57` sempre, e **`/partidos/{id}?idLegislatura=56` responde HTTP 400**. Para o PT: `totalMembros: "65"` (bancada de hoje) e `totalPosse: "68"` (eleitos).
- **Contar da tabela `Deputado` local custa zero requisição e funciona em qualquer legislatura.** PL 116, PT 79, UNIÃO 66, PSD 63, PP 62. É outro número: conta quem ocupou cadeira pelo partido durante a legislatura, suplente incluído, então fica acima da bancada do dia.

O que estava acontecendo no app: os dois lugares que mostram a lista faziam `sortedByDescending { it.totalMembros }`, e `syncPartidos` grava essa coluna como `null`. Ordenar por null não reordena nada, então sobrava a ordem de inserção — que é a ordem da API — **que é alfabética**. O carrossel da Home pegava os 8 primeiros e mostrava AVANTE, CIDADANIA, DC onde queria mostrar os maiores. O chip também nunca exibia número, pelo mesmo motivo.

Resolvido com a contagem local, feita em SQL junto da leitura (`getPartidos` em `Partido.sq`), que ordena favoritos primeiro, depois por tamanho, e desempata pela sigla. O rótulo passou a ser "deputados" e não "membros", porque é o que o número é. O header do detalhe continua mostrando "membros" com o `totalMembros` oficial — dois números diferentes, dois rótulos diferentes.

---

## 5. Comissões: por que a tela é sem graça, medido

A tela de detalhe da comissão é a menos interessante do app, e o motivo não é de design — é de dado. Medido na CCJC (`idOrgao=2003`), que é a comissão com mais votações de todas, em 2026-09-19.

### 5.1 As vinte votações dizem duas frases

Buscando exatamente como o app busca (`ordenarPor=idProposicaoObjeto`, `itens=20`) e lendo o detalhe das vinte:

- **todas as vinte** têm `descricao` igual a `"Aprovado o Parecer."` ou `"Aprovada a Redação Final."`;
- **todas as vinte** têm `aprovacao = 1`;
- todas têm exatamente uma `proposicoesAfetadas`.

A tela mostra, então, vinte cartões com uma de duas frases, todos com a mesma tarja verde, e um cabeçalho que diz `20 total · 20 aprovadas · 0 rejeitadas`. O contador é sempre o mesmo número. Não há o que comparar, ordenar ou notar — a informação não varia.

Isso não é ruído de amostra: votação de parecer em comissão é aprovada na esmagadora maioria das vezes. O dado é assim.

### 5.2 As vinte não são as mais recentes

`ordenarPor=idProposicaoObjeto` ordena por id de proposição, não por data. A página 1 dessa ordenação devolve votações de julho a setembro de 2026 — um recorte arbitrário de um total de **410**. O repositório depois ordena o que recebeu por `dataHoraRegistro` decrescente, o que faz o resultado *parecer* "as mais recentes" sem ser.

Sem `ordenarPor`, as mesmas vinte vêm todas de um único dia (2026-09-01). Nenhuma das duas opções é "as vinte últimas votações da comissão" de forma honesta, e o cabeçalho de contagem descreve esse recorte como se fosse o todo.

### 5.3 O que a API tem e a tela ignora

O detalhe da votação (`/votacoes/{id}`) devolve bem mais do que o app lê. `VotacaoDetailDto` mapeia cinco campos; a resposta traz:

| Campo | O que é | Usado? |
|---|---|---|
| `ultimaApresentacaoProposicao` | **o parecer em si**: nome do relator, partido, UF e o texto do voto | **não** |
| `proposicoesAfetadas` | a proposição votada | sim |
| `objetosPossiveis` | o que estava em pauta naquela votação | não |
| `efeitosRegistrados` | efeito da votação sobre a tramitação | não |
| `idEvento` / `uriEvento` | a reunião onde aconteceu | mapeado, não exibido |

`ultimaApresentacaoProposicao.descricao` é o campo que contém a frase de verdade — quem relatou e o que defendeu — enquanto `descricao` é o carimbo processual. **O app já paga as 21 requisições e joga fora justamente a parte que teria conteúdo.**

E há um recurso inteiro sem uso: `GET /orgaos/{id}/membros`. Ele não devolve uma lista de membros — devolve **um registro por passagem**, uma linha por (deputado, título, período), com `dataInicio`, `dataFim`, `idLegislatura`, `titulo`, `codTitulo`, `siglaPartido`, `siglaUf` e `urlFoto`.

E ele tem dois modos, o que é fácil de interpretar errado:

| Chamada | O que volta | CCJC medida |
|---|---|---|
| sem parâmetros | **só a composição vigente** (todos com `dataFim` nulo) | 130 registros: 1 Presidente, 3 Vice-Presidentes, 60 Titulares, 66 Suplentes |
| com `dataInicio`/`dataFim` | **o histórico de passagens** da janela | legislatura 57 → 902 registros, 293 deputados distintos |

Duas consequências: a composição vigente é **anual**, não do quadriênio — os 130 registros atuais começam todos em 2026; e o histórico precisa ser agrupado por deputado antes de virar tela, senão repete a mesma pessoa.

A presidência rotaciona e tem mandato. A CCJC teve quatro presidentes na legislatura 57: Rui Falcão (2023-03-15 → 2024-03-06), Caroline de Toni (→ 2025-03-18), Paulo Azi (→ 2026-02-09) e Leur Lomanto Júnior (atual). É uma linha do tempo pronta, com nomes que o app já tem.

`GET /orgaos/{id}` também não é chamado — o app acha o órgão na lista que já baixou. Ele traz `dataInstalacao`, `sala`, `urlWebsite` e as datas de funcionamento.

### 5.4 Direções que essa medição sustenta

Não é decisão tomada, é o que o dado permite:

- **mostrar o parecer, não o carimbo** — ler `ultimaApresentacaoProposicao` e exibir relator e voto no lugar de "Aprovado o Parecer";
- **mostrar quem compõe a comissão** — `/orgaos/{id}/membros`, com presidente em destaque e ligação para as telas de deputado e partido;
- **escolher as votações por data**, com paginação honesta, em vez de uma página arbitrária apresentada como recente;
- **curadoria por atividade em vez de constante** (item 4.3), lembrando que isso exige o estado vazio do item 4.3.1.

---

## 6. Fora da API: os arquivos anuais

O portal publica arquivos por ano em `dadosabertos.camara.leg.br/arquivos/{recurso}/{formato}/{recurso}-{ano}.{formato}`, e eles têm dado que a API não expõe.

- **`votacoesVotos-{ano}.csv`** é a relação voto → deputado que falta na API: `idVotacao;dataHoraVoto;voto;deputado_id;…`. 2026 até setembro tem 51.832 linhas e 16,4 MB; a legislatura 57 inteira daria ~150 MB em CSV e ~277 MB em JSON.
- **`votacoes-{ano}.csv`** traz `votosSim`/`votosNao`/`votosOutros`, colunas que **a API não devolve nem na listagem nem no detalhe**.

Três propriedades que mudam como usá-los:

- **são regerados toda madrugada**, não são estáticos — o arquivo de 2025 e o de 2026 tinham o mesmo `Last-Modified` da manhã do dia da verificação. O dado atrasa até ~24h;
- **não há compressão**: `Accept-Encoding: gzip` devolve os mesmos bytes, então o `content-length` é a transferência real. Um `HEAD` diz o peso antes de baixar;
- **`Range` é aceito mas inútil**: o arquivo não está em ordem cronológica, então não dá para baixar só o que é novo.

Usar CSV e não JSON: mesma informação, quase metade do peso.

### 6.1 `deputados.csv` — pequeno, sem ano, e cobre um buraco do item 4.1

Nem todo arquivo é anual. `arquivos/deputados/csv/deputados.csv` não tem `-{ano}` no nome e é a lista completa: **1,3 MB, 7889 linhas, todos os deputados da história da Câmara.** Colunas, verificadas em 2026-09-19:

```
uri;nome;idLegislaturaInicial;idLegislaturaFinal;nomeCivil;cpf;siglaSexo;
urlRedeSocial;urlWebsite;dataNascimento;dataFalecimento;ufNascimento;municipioNascimento
```

Não há `id` em coluna própria — o id sai do fim do `uri`.

Por que importa: `siglaSexo`, `dataNascimento`, `ufNascimento` e `municipioNascimento` são exatamente os quatro campos que a tela de partido busca hoje com uma requisição por membro, e que **não existem em nenhuma listagem da API**. Um download de 1,3 MB substitui até 145 requisições por partido, em toda legislatura, e o conteúdo praticamente não muda: onde e quando alguém nasceu é fato fixo.

O JSON equivalente tem 3,6 MB — 2,8× o CSV, a maior diferença medida entre os dois formatos aqui.

Isso é bem menor que os arquivos de votação, mas continua sendo download, então segue as mesmas regras de produto: pedir permissão, avisar o peso, marcar validade.

---

## 7. O que este documento ainda não cobre

- **Ordenação padrão de cada endpoint.** Só foi verificada onde o app passa `ordenarPor` explicitamente.
- **Comportamento de `/orgaos/{id}/membros` com datas.** Com faixa de datas ele devolve *mais* registros do que sem, o que sugere que o filtro seleciona vínculos históricos em vez de restringir. Precisa ser entendido antes de virar base do escopo de comissões.
- **Limites de intervalo nos demais endpoints.** `/votacoes` e `/proposicoes` foram testados até o erro e os dois recusam mais de três meses. Os outros não foram.
- **`/legislaturas/{id}`** individual, que provavelmente evita baixar as 57.

## 8. Como refazer esta verificação

O método que produziu a tabela, para quando a API mudar:

1. mandar o parâmetro suspeito e olhar o `instance` do 400 — ele nomeia o recusado;
2. ler `links[rel=last]` para saber quantas páginas existem sem baixar nada;
3. comparar a mesma chamada com e sem o parâmetro, porque alguns (como `ano`) são silenciosamente ignorados em vez de recusados.

O terceiro é o que pega os piores: recusa aparece no log, item faltando não aparece em lugar nenhum.
