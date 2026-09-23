# Glossary

Portuguese terms as they appear in the API, the code and the UI. The code keeps the Câmara's
own words wherever it can, so these names are the ones you will be searching for.

## The institution

| Term | Meaning |
|---|---|
| **Câmara dos Deputados** | Lower house of the Brazilian Congress, 513 seats. The API's owner. |
| **Senado** | Upper house. Not covered by this API. `PDS` and `PLS` are siglas that come from it. |
| **Legislatura** | A four-year term of the Câmara, 1 Feb to 31 Jan. The 57th runs 2023-02-01 to 2027-01-31. `id` is its number. |
| **Sessão legislativa** | One legislative year inside a legislatura. Committee compositions are renewed every year. |
| **Recesso** | Recess, in January and July. Votação counts over those months read as a quiet committee. |
| **Plenário** | The full chamber, as opposed to a committee. |
| **Mesa** | A body's board. In a committee: the president and the 1st, 2nd and 3rd vice-presidents (`CargoComissao.MESA`). |
| **Bancada** | A party's group of deputados in the house. |
| **Líder** | A party's leader in the house (`status.lider`). |
| **Gabinete** | A deputado's office, identified by building (`predio`, an annex number) and room (`sala`). |
| **Anexo** | A building of the Câmara (Anexo 3, Anexo 4). |

## People

| Term | Meaning |
|---|---|
| **Deputado / deputada** | A member of the Câmara. |
| **Titular / suplente** | Holder and substitute. Used both for a seat in the house and for a seat on a committee (`codTitulo` 101 / 102). |
| **Em exercício** | Actually sitting on a given date, as opposed to having been elected or having sat at some point in the term (`emExercicio`). |
| **Posse** | Swearing-in. `totalPosse` is how many were sworn in for a party, `totalMembros` how many are there today. |
| **Relator(a)** | Rapporteur: the deputado who writes the opinion (**parecer**) on a proposition in a committee. |
| **Autor / assinatura** | Author and signature. The first signature is the proponent. The author can be an órgão or the Executive, not only a deputado. |
| **UF** | Unidade Federativa: one of the 26 states or the DF. Two-letter sigla (`SP`, `DF`). |
| **Região** | The five regions (Norte, Nordeste, Centro-Oeste, Sudeste, Sul), derived from UF in the app. |

## Bodies

| Term | Meaning |
|---|---|
| **Órgão** | Any body of the house: committees, the plenary, the Mesa… `/orgaos`. |
| **Comissão permanente** | Standing committee, `codTipoOrgao=2`. There are 30. |
| **codTitulo** | A member's title in an órgão: `1` president, `2`/`3`/`4` vice-presidents, `101` titular, `102` suplente. Sort by this, and use `titulo` only for display. |
| Committee siglas | `CCJC` Constitution and Justice, `CFT` Finance and Taxation, `CSAUDE` Health, `CE` Education, `CPD` People with Disabilities, `CAPADR` Agriculture, `CCTI` Science and Technology, `CASP` Public Administration, `CSSF` Social Security and Family, `CCOM` Communication, `CSPCCO` Public Security, `CTUR` Tourism, `CDU` Urban Development… |
| **Evento** | A session or meeting of an órgão (`/orgaos/{id}/eventos`). `Reunião Deliberativa` is a meeting that votes. `audiência pública` is a public hearing. |

## Propositions and their passage

| Term | Meaning |
|---|---|
| **Proposição** | Any formal proposal filed in the house: bills, amendments, requests, opinions… |
| **siglaTipo** | The type's abbreviation. There are 544. Listed by `/referencias/proposicoes/siglaTipo`. |
| `PEC` | Proposta de Emenda à Constituição: constitutional amendment (bucket CONSTITUICAO). |
| `PL`, `PLP` | Projeto de Lei (ordinary law), Projeto de Lei Complementar (complementary law). |
| `MPV`, `PLV` | Medida Provisória: an executive decree with force of law from the day it is published. It becomes a PLV (Projeto de Lei de Conversão) when converted. |
| `PLN`, `PLC` | `PLN`: a bill of Congress as a whole, mostly budget. `PLC`: how the Senate numbers a bill that came from the Câmara. Both count as LEI in the app. |
| `PDL`, `PDC`, `PDS` | Projeto de Decreto Legislativo: Congress acting on its own authority (for example, suspending an executive decree). Bucket ATO_LEGISLATIVO. |
| `PRC`, `PRN` | Projeto de Resolução: the Câmara's or Congress's internal rules. |
| `REQ`, `MSC`, `INC`, `RIC`, … | Requests, messages from the Executive, suggestions, information requests. They land in TRAMITACAO (procedural). |
| **Ementa** | The one-line official summary of a proposition. `ementaDetalhada` is a longer version, when there is one. |
| **Inteiro teor** | The full text (`urlInteiroTeor`, a PDF). |
| **Apresentação** | Filing. `dataApresentacao` is the filing date. The parameters that filter by it are `dataApresentacaoInicio`/`Fim`. |
| **Tramitação** | A proposition's passage through the house: each step (despacho, opinion, vote…) is a tramitação. Note that the generic `dataInicio`/`dataFim` on `/proposicoes` filter by this, not by filing. |
| **Situação** | Current state (`descricaoSituacao`, for example "Aguardando Parecer"). Different from `descricaoTramitacao`, which is the stage. |
| **Despacho** | An order routing a proposition (for example, "to the CCJC and CFT"). |
| **Regime** | Processing regime: `Urgência` or `Ordinário`, with the rule article. `.` when unset. |
| **Apreciação** | Who decides it: the plenary, or the committees conclusively. `Indefinida` when undecided. |
| **Parecer** | A committee opinion, written by the relator. |
| **Redação final** | The final wording, voted after the merits. |
| **Tema** | Subject classification, from a vocabulary of about 25. It arrives months after filing. |
| **Keywords** | Indexing terms, as one comma-separated string. |

## Votes

| Term | Meaning |
|---|---|
| **Votação** | A vote event: one decision taken in an órgão at one moment. Its id looks like `2645346-18`. |
| **Votação nominal** | A roll-call vote, where each deputado's vote is recorded. About 2% of votações. |
| **Votação simbólica** | A vote by show of hands, with no individual record. The other 98%. |
| **Voto** | One deputado's vote in a nominal votação: `tipoVoto` is `Sim`, `Não`, `Abstenção`, `Obstrução`, `Artigo 17` (the presiding deputado)… and can be null. |
| **Aprovação** | `aprovacao = 1` when the votação carried. |
| **Proposições afetadas** | The propositions a votação acted on (`proposicoesAfetadas`). |
| **Proposição objeto** | The main proposition of a votação (`uriProposicaoObjeto`). |
| **Última apresentação** | `ultimaApresentacaoProposicao`: the latest opinion presented, and the text with substance on a committee vote. |
| **Efeitos registrados** | Effects of a vote on the passage. Empty in every sample. |

## Money

| Term | Meaning |
|---|---|
| **Despesa** | An expense reimbursed through the parliamentary quota (CEAP). `/deputados/{id}/despesas`. |
| **Documento / parcela** | The receipt, and an instalment of it. `codDocumento` + `parcela` is the natural key. |
| **Fornecedor** | The supplier, identified by `cnpjCpfFornecedor`. |
| **CNPJ / CPF** | Company and individual tax ids. |

## App-only terms

Words the app coined in Portuguese and uses in code:

| Term | Meaning |
|---|---|
| **Santinho** | Literally the small card a campaign hands out with a candidate's number. In the app, the private note of numbers to take to the booth. |
| **Urna** | The electronic voting machine (`CargoDaUrna`: the offices it asks for, in order). |
| **Janela** | A date window for a request (`AtividadeWindow`, `ProposicaoWindow`, `mandateWindows`). |
| **Varredura / sweep** | Reading every votação in a window to find the nominal ones. |
| **Data de referência** | The day used to ask who was seated in a term (today, or the term's last day). |
| **Bucket** | One of the four groups of proposition types (`ProposicaoBucket`). |
| **Área** | One of the app's visual identities per domain (`MagnaArea`). |
