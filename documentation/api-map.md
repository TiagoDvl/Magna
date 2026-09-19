# Mapa da API da Câmara — o que o Magna usa

Entregável do **bloco 7** do plano da 1.1 (`docs/review-v1.1.md`, seção 10).

Verificado contra `https://dadosabertos.camara.leg.br/api/v2` em **2026-09-19**. Tudo aqui foi medido com requisição real, não lido de documentação — a coluna de custo e os limites de intervalo só aparecem assim.

O objetivo é responder, sem abrir o navegador: *para mostrar X na legislatura Y, quais chamadas são feitas, com quais parâmetros, e o que fica no banco depois.*

---

## 1. Tabela mestre

| Endpoint | Parâmetros usados hoje | Quem chama | Vai para o banco | Custo por abertura |
|---|---|---|---|---|
| `GET /deputados` | `idLegislatura` | `DeputadosRepository.kt:152` | `Deputado` (com `legislaturaId`) | 1 |
| `GET /deputados/{id}` | — | `DeputadosRepository.kt:123`, `PartidosRepository.kt:150` | `DeputadoDetails` (com `legislaturaId`) | 1 por deputado |
| `GET /deputados/{id}/despesas` | `idLegislatura`, `ano`, `ordem`, `ordenarPor`, `itens=100` | `DeputadosRepository.kt:140` | `DeputadoExpense` (com `legislaturaId`) | 1 |
| `GET /partidos` | `idLegislatura`, `itens=100` | `PartidosRepository.kt:45` | `Partido` (com `legislaturaId`) | 1 |
| `GET /partidos/{id}` | — | `PartidosRepository.kt:82` | não persiste | 1 |
| `GET /partidos/{id}/membros` | `idLegislatura` | `PartidosRepository.kt:117` | não persiste | 1 |
| `GET /proposicoes` | `ordem=desc`, `siglaTipo?` | `ProposicoesRepository.kt:104` | `Proposicao` (**sem** `legislaturaId`) | 1 |
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
| Detalhe do partido | **17** | 1 partido + 1 membros + 15 × `deputados/{id}`, limitado por `Semaphore` — `PartidosRepository.kt:82,117,144` |
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
| `/proposicoes` | **HTTP 400** | **sim** | faixa de datas da legislatura |
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

Já registrado no plano (seção 14.1) e corrigido em `3897905`, mas pertence a este documento: o gateway responde **403 em qualquer requisição** que carregue o header `Accept-Charset`, com qualquer valor. O Ktor instala `HttpPlainText` por padrão e o carimba sozinho; a remoção tem que acontecer no send pipeline.

---

## 4. O que o mapa revelou como trabalho

### 4.1 [ALTO] Membros do partido são truncados em 15

`PartidosApi.getPartidoMembros` (`PartidosApi.kt:24`) não manda `itens`, e o padrão do endpoint é 15. O partido 36844 na legislatura 57 tem **6 páginas**; a tela mostra a primeira e não indica que há mais.

Isso não é problema futuro: **está em produção hoje, na legislatura atual.** Mandar `itens=100` como `getPartidos` já faz resolve a maioria dos casos; o correto é paginar.

Efeito colateral bom: o leque de 15 `deputados/{id}` da tela de detalhe existe porque só 15 membros chegam. Resolver a paginação sem resolver o leque troca 17 requisições por ~90.

### 4.2 [ALTO] A segunda página de deputados some em legislaturas antigas

`getDeputados` não manda `itens` nem pagina. O padrão de `/deputados` é 1000, então a 57 (879 deputados) cabe numa página e o problema não aparece hoje. **A 55 tem 1138 e ocupa duas páginas** — o app gravaria 1000 e perderia 138 sem erro nenhum.

É o bug que o bloco 8 destravaria no dia em que a troca de legislatura entrasse no ar.

### 4.3 [MÉDIO] As seis comissões fixas contra as 30 que a API devolve

`/orgaos?codTipoOrgao=2` devolve **30 comissões permanentes**. `MagnaComissaoPermanente` (`data/repository/orgaos/params/`) fixa seis `idOrgao` na mão, com um comentário que diz de onde vieram.

Se a curadoria é decisão de produto, ela deveria estar declarada como tal (e fora de `data/`, ver item 4.6 do plano). Se é acidente de implementação, o app está escondendo 24 comissões. Hoje o código não deixa claro qual das duas.

### 4.4 [MÉDIO] 31 requisições para 15 proposições

`refreshProposicoes` busca a lista e dispara detalhe + autores por item (`ProposicoesRepository.kt:104-113`). São 31 chamadas para montar uma seção da Home. O `supervisorScope` já evita que uma falha derrube as outras, e o bloco 4 já arrumou o cancelamento — o que sobra é a quantidade.

Vale checar se `/proposicoes` sozinho já traz o suficiente para a lista, deixando detalhe e autores para quando a pessoa abre a proposição.

### 4.5 [MÉDIO] `/votacoes` com `itens=20` e `ordenarPor` sem motivo escrito

`VotacoesApi.getVotacoesFromOrgao` (`VotacoesApi.kt:13`) fixa `itens=20` e `ordenarPor=idProposicaoObjeto`. São 21 páginas disponíveis para a CCJ. O `20` é o que produz as 21 requisições da tela de comissão, e nada explica a escolha — nem o número, nem a ordenação por id de proposição num lugar onde a tela mostra por data.

### 4.6 [BAIXO] `/proposicoes/{id}/votacoes` existe e não é usado

Declarado em `ProposicoesApiInterface`, sem chamador. Ou vira feature, ou sai junto do próximo lote de código morto.

---

## 5. O que este documento ainda não cobre

- **Ordenação padrão de cada endpoint.** Só foi verificada onde o app passa `ordenarPor` explicitamente.
- **Comportamento de `/orgaos/{id}/membros` com datas.** Com faixa de datas ele devolve *mais* registros do que sem, o que sugere que o filtro seleciona vínculos históricos em vez de restringir. Precisa ser entendido antes de virar base do escopo de comissões.
- **Limites de intervalo nos outros endpoints.** Só `/votacoes` foi testado até o erro; `/proposicoes` aceitou um mês e um ano sem reclamar, mas o teto não foi procurado.
- **`/legislaturas/{id}`** individual, que provavelmente evita baixar as 57.

## 6. Como refazer esta verificação

O método que produziu a tabela, para quando a API mudar:

1. mandar o parâmetro suspeito e olhar o `instance` do 400 — ele nomeia o recusado;
2. ler `links[rel=last]` para saber quantas páginas existem sem baixar nada;
3. comparar a mesma chamada com e sem o parâmetro, porque alguns (como `ano`) são silenciosamente ignorados em vez de recusados.

O terceiro é o que pega os piores: recusa aparece no log, item faltando não aparece em lugar nenhum.
