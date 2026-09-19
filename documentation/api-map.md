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

Já registrado no plano (seção 16.1) e corrigido em `3897905`, mas pertence a este documento: o gateway responde **403 em qualquer requisição** que carregue o header `Accept-Charset`, com qualquer valor. O Ktor instala `HttpPlainText` por padrão e o carimba sozinho; a remoção tem que acontecer no send pipeline.

### 3.6 Voto nominal: a relação só existe num sentido, e `itens` a esconde

`GET /deputados/{id}/votos` e `GET /deputados/{id}/votacoes` devolvem **405**. Não existe caminho deputado → votos. O único acesso é `GET /votacoes/{id}/votos`.

E esse endpoint **recusa `itens` com HTTP 400**. Sem parâmetro nenhum ele devolve todos os votos de uma vez, sem paginação (`links` só traz `self`). Quem tenta paginar leva erro ou lista vazia e conclui que não há voto nominal registrado — é a armadilha mais cara do mapa inteiro.

Vale saber a proporção antes de desenhar em cima: em 2026, de **7.360 votações, só 152 têm voto nominal** (2%), concentradas em 44 dias do ano. Detalhes e a estratégia de sincronização estão no bloco 10 do plano (seção 13).

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

### 4.3.1 [ALTO] Comissão sem votação carrega para sempre

`ComissaoPermanenteDetailScreen.kt:73` usa `if (state.votacoes.isEmpty())` para decidir mostrar `LoadingComponent`. Não existe estado de lista vazia: uma comissão sem votação nenhuma fica girando indefinidamente.

Hoje isso não aparece porque as seis fixas todas têm votação. **Qualquer mexida na curadoria expõe o bug na hora** — CASP tem zero. É o mesmo erro do item 5.1 do plano, e o componente de estado vazio que o bloco 9 vai criar é o que resolve.

### 4.4 [MÉDIO] 31 requisições para 15 proposições

`refreshProposicoes` busca a lista e dispara detalhe + autores por item (`ProposicoesRepository.kt:104-113`). São 31 chamadas para montar uma seção da Home. O `supervisorScope` já evita que uma falha derrube as outras, e o bloco 4 já arrumou o cancelamento — o que sobra é a quantidade.

Vale checar se `/proposicoes` sozinho já traz o suficiente para a lista, deixando detalhe e autores para quando a pessoa abre a proposição.

### 4.5 [MÉDIO] `/votacoes` com `itens=20` e `ordenarPor` sem motivo escrito

`VotacoesApi.getVotacoesFromOrgao` (`VotacoesApi.kt:13`) fixa `itens=20` e `ordenarPor=idProposicaoObjeto`. São 21 páginas disponíveis para a CCJ. O `20` é o que produz as 21 requisições da tela de comissão, e nada explica a escolha — nem o número, nem a ordenação por id de proposição num lugar onde a tela mostra por data.

### 4.6 [BAIXO] `/proposicoes/{id}/votacoes` existe e não é usado

Declarado em `ProposicoesApiInterface`, sem chamador. Ou vira feature, ou sai junto do próximo lote de código morto.

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

---

## 7. O que este documento ainda não cobre

- **Ordenação padrão de cada endpoint.** Só foi verificada onde o app passa `ordenarPor` explicitamente.
- **Comportamento de `/orgaos/{id}/membros` com datas.** Com faixa de datas ele devolve *mais* registros do que sem, o que sugere que o filtro seleciona vínculos históricos em vez de restringir. Precisa ser entendido antes de virar base do escopo de comissões.
- **Limites de intervalo nos outros endpoints.** Só `/votacoes` foi testado até o erro; `/proposicoes` aceitou um mês e um ano sem reclamar, mas o teto não foi procurado.
- **`/legislaturas/{id}`** individual, que provavelmente evita baixar as 57.

## 8. Como refazer esta verificação

O método que produziu a tabela, para quando a API mudar:

1. mandar o parâmetro suspeito e olhar o `instance` do 400 — ele nomeia o recusado;
2. ler `links[rel=last]` para saber quantas páginas existem sem baixar nada;
3. comparar a mesma chamada com e sem o parâmetro, porque alguns (como `ano`) são silenciosamente ignorados em vez de recusados.

O terceiro é o que pega os piores: recusa aparece no log, item faltando não aparece em lugar nenhum.
