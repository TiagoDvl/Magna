# Magna — Revisão técnica pré-1.1

Data: 2026-09-15. Base: `main` em `a3dbef3`.

Objetivo deste documento: servir de mapa para as sessões de implementação da 1.1. Cada item tem arquivo e linha para que uma sessão isolada consiga agir sem re-explorar o projeto. Os itens estão ordenados por severidade dentro de cada seção, e no final há uma proposta de sequência de trabalho em blocos independentes.

---

## 1. Resumo executivo

O projeto está bem acima da média para um app solo: camadas claras, tudo atrás de interface, dispatcher injetável, SQLDelight como fonte de verdade e a UI já separada em `Screen` (stateless) + `ViewModel`. A base é boa. Os problemas são de três tipos:

1. **Bugs de dados que já afetam usuários** — despesas duplicadas a cada visita, ordenação de meses errada, filtro de partidos ignorando a legislatura, DTOs que quebram a tela inteira quando a API manda um `null`.
2. **Dívida estrutural que vai cobrar juros na 1.1** — sem migração de banco (qualquer mudança de schema crasha quem já tem a 1.0 instalada), coroutines disparadas em escopos que nunca cancelam, quatro variantes caseiras do padrão "loading/erro/conteúdo", zero testes reais e um job de CI que passa sem rodar nada.
3. **Instrumentação inexistente** — Firebase Analytics e Crashlytics estão declarados no Gradle, mas nenhuma linha de código os usa. Crashes chegam (o plugin cuida disso); uso, não.

A recomendação é: **antes da feature nova**, fazer o bloco 0 (baseline de migração + CI), o bloco 1 (analytics) e o bloco 2 (despesas). Os demais podem intercalar com a feature.

---

## 2. Banco de dados (SQLDelight)

### 2.1 [ALTO] Não existe migração; qualquer mudança de schema quebra a atualização

- `composeApp/build.gradle.kts` liga `verifyMigrations = true` e aponta `schemaOutputDirectory` para `src/commonMain/sqldelight/migrations`, mas a pasta não existe e não há nenhum `.sqm`.
- Os drivers (`DatabaseDriverFactory.android.kt`, `.ios.kt`, `.jvm.kt`) usam `MagnaDatabase.Schema` na versão 1. Usuários da 1.0 já têm um `magna.db` versão 1 no aparelho.
- Consequência: a primeira alteração em qualquer `.sq` (que a correção de despesas e provavelmente a feature nova vão exigir) faz o app crashar na abertura para quem atualiza, com `no such column`.

**Status: resolvido.** O baseline `composeApp/src/commonMain/sqldelight/migrations/1.db` existe e está commitado, com `user_version = 1` — igual ao `MagnaDatabase.Schema.version` gerado, que é o que os usuários da 1.0 têm no aparelho. Os 9 `CREATE TABLE` foram conferidos um a um contra os `.sq` (normalizando espaços): batem exatamente.

A partir daqui, cada mudança de schema precisa de um `migrations/N.sqm` (`ALTER TABLE ...`) e o `verifyMigrations` passa a proteger de verdade.

> **Cuidado ao regerar no Windows.** `./gradlew :composeApp:generateCommonMainMagnaDatabaseSchema` falha nesta máquina com `UnsatisfiedLinkError: 'void org.sqlite.core.NativeDB._open_utf8'`. A causa não é o projeto: a task roda num worker isolado do Gradle que, no Windows, sobe sem `TMP`/`TEMP`, então `java.io.tmpdir` cai em `C:\WINDOWS` e o sqlite-jdbc não consegue extrair a `.dll` (`AccessDeniedException`). Verificado que o daemon tem tmpdir correto e que nem `-D` nem `org.gradle.jvmargs` propagam para o worker. O baseline atual foi gerado aplicando os mesmos `CREATE TABLE` via SQLite direto, o que produz um `sqlite_master` idêntico. `verifySqlDelightMigration` roda no CI (Linux), onde o problema não existe — é lá que a verificação vale.

### 2.2 [ALTO] Despesas duplicam a cada abertura do deputado

- `DeputadoExpense.sq:19` — `insertExpense` é um `INSERT` simples numa tabela com `expenseId AUTOINCREMENT` e sem `UNIQUE`.
- `DeputadosRepository.getDeputadoExpenses` (linhas 111-139) chama a API e insere toda vez que a tela abre.
- Resultado: 15 linhas novas por visita, e a lista cresce indefinidamente. Quem abriu o mesmo deputado cinco vezes vê 75 cards.

**Status: corrigido.** A tabela foi refeita com chave natural `PRIMARY KEY (deputadoId, legislaturaId, codDocumento, parcela)` e o insert virou `ON CONFLICT ... DO UPDATE`. Revisitar o deputado agora atualiza as linhas em vez de acrescentar uma segunda cópia.

`parcela` entrou na chave de propósito: um documento reembolsado em partes volta com uma linha por parcela, e chavear só por `codDocumento` faria as parcelas colapsarem numa só, perdendo dado em silêncio.

### 2.3 [MÉDIO] Ordenação de despesas por mês está errada

- `DeputadoExpense.sq:44` e `:50` — `ORDER BY year DESC, month DESC` sobre colunas `TEXT`. `"9" > "10"` lexicograficamente, então outubro/novembro/dezembro aparecem antes de fevereiro a setembro.
- Causa: `DeputadoExpenseMapper.kt:42` grava `ano.toString()` e `mes.toString()`.

**Status: corrigido.** `year` e `month` são `INTEGER`. A ordenação passou a ser numérica.

### 2.4 [MÉDIO] Valor monetário gravado como string formatada

- `DeputadoExpenseMapper.kt:47` grava `"R$ $valorDocumento"` (vira `R$ 8000.0`, sem formatação pt-BR).
- Impede somar, ordenar por valor ou trocar a formatação sem migração.

**Status: corrigido.** `documentValue` é `REAL` e o domínio carrega `Double`. A formatação virou `Double.toBrlString()` em `util/CurrencyFormat.kt`, chamada na tela: `8000.0` vira `R$ 8.000,00`. Escrita à mão porque não existe formatador de número multiplataforma. O mesmo tratamento foi dado à data: a coluna guarda o que a API mandou e a formatação acontece no mapper, então mudar o formato de exibição não exige migração.

### 2.5 [BAIXO] Chaves estrangeiras são decorativas

- Todas as tabelas declaram `FOREIGN KEY`, mas nenhum driver liga `PRAGMA foreign_keys = ON`. SQLite ignora FKs por padrão.
- Hoje isso até ajuda: `DeputadoDetails.legislaturaId` referencia `Legislatura`, que nunca é populada. Ligar o pragma agora quebraria inserts.
- Depois do bloco 5 não existe mais nem o código que *poderia* popular a tabela: `LegislaturaRepository` e `LegislaturaDao` foram removidos. A tabela ficou de propósito, para não exigir migração, mas agora é comprovadamente órfã.

Decisão a tomar: remover as FKs (honesto) ou popular `Legislatura` no sync e ligar o pragma. Recomendação: remover.

### 2.6 [BAIXO] Coluna `Proposicao.codTipo` guarda a sigla

- `ProposicoesRepository.kt:83` tem o `TODO` original. A coluna se chama `codTipo` mas recebe `siglaTipoEntity.sigla`, e `ProposicaoDao.getProposicoes` filtra por ela com o nome do enum `ProposicaoType`.
- Funciona, mas engana quem lê. Renomear para `siglaTipo` na mesma migração dos itens acima.

### 2.7 [BAIXO] `LIMIT 5` hardcoded no SQL

- `Proposicao.sq:14` e `:17`. O limite pertence a quem chama, não à query.

### 2.8 [BAIXO] Busca da Home é sensível a acento

- `Deputado.sq:41` usa `LIKE`. `"Jose"` não encontra `"José"`. A tela de busca completa (`DeputadosSearchState.kt`) já usa `normalizeForSearch()` em memória, então os dois caminhos se comportam diferente.
- Opção simples: coluna `name_search` normalizada gravada no insert. Opção mais simples ainda: a Home filtrar em memória como a outra tela.

---

## 3. Rede (Ktor, APIs, DTOs)

### 3.1 [ALTO] DTOs estritos derrubam a tela inteira com um `null`

A API da Câmara devolve `null` e omite campos com frequência. Com `kotlinx.serialization`, campo não-nulo sem default = exceção = `catch` genérico = estado de erro na tela toda.

Casos mais expostos:

- `DeputadoByIdDto.kt:23,35,41,47` — `cpf`, `dataNascimento`, `ufNascimento`, `escolaridade` não-nulos. Deputado sem `escolaridade` cadastrada = detalhes nunca carregam.
- `DespesaDto.kt:13,21,22` — `dataDocumento`, `numRessarcimento`, `codLote` não-nulos. Uma despesa sem data = nenhuma despesa aparece.
- `DeputadoDto` — `urlFoto` e `siglaUf` não-nulos.

**Status: corrigido.** Nos DTOs expostos só o `id` continua obrigatório; o resto ganhou default ou virou nulável. Campos que o app nunca lê foram removidos da declaração — `ignoreUnknownKeys` os descarta, e campo que não existe não quebra parse. Saíram 6 campos do `DespesaDto`, 5 do `DeputadoByIdDto` e o `UltimoStatusDto` ficou só com o gabinete. `links` ganhou default em todas as 17 respostas.

**Correção do diagnóstico original:** eu tinha escrito `explicitNulls = false`, que está errado — essa opção afeta a *escrita* de JSON, não a leitura. A chave certa é `coerceInputValues = true`, que converte um `null` explícito no default do campo. Sem ela, um `null` falha mesmo em campo que tem default.

A configuração do parser saiu para `ApiJson.kt` e é compartilhada com os testes de propósito: teste que monta o próprio parser tolerante não prova nada sobre o que o app faz com o payload real. `ApiParsingTest` usa payloads no formato da API com null, campo ausente e campo novo desconhecido.

### 3.2 [ALTO] Sem timeout nem retry

**Status: corrigido.** `HttpTimeout` com 30s de request e socket, 10s de conexão. `HttpRequestRetry` com `retryOnServerErrors(maxRetries = 2)` e `exponentialDelay()`.

Timeout **não** é retentado, de propósito: três tentativas de 30s deixariam a pessoa um minuto e meio no spinner antes de receber o erro. Erro de servidor retenta; request travada falha rápido.

O retry fica antes do `HttpResponseValidator`, então o `api_error` é reportado uma vez por requisição lógica, não uma por tentativa.

### 3.3 [MÉDIO] `getPartidos` ignora a legislatura

**Status: corrigido.** `PartidosApi.getPartidos` passou a enviar o `idLegislatura` que já recebia, no lugar do `dataInicio=2025-01-01` hardcoded.

### 3.4 [MÉDIO] `getDeputadoExpenses` ignora o ano e só pega a primeira página

- `DeputadosApi.kt:25-28` — o parâmetro `year` não é enviado. Sem `itens`, a API devolve 15 registros. O `LinkDto` com `rel=next` é desserializado em todas as responses e nunca lido.
**Status: parcialmente corrigido.** `ano` e `ordenarPor` agora são enviados, e `itens=100` substitui a página padrão de 15. Seguir o `rel=next` para quem estourar 100 documentos no ano continua em aberto.

### 3.5 [MÉDIO] Chamadas sequenciais onde deveriam ser paralelas

**Status: corrigido** junto com o bloco 4. Os detalhes das votações saem em paralelo com `async`/`awaitAll`. A ordenação também mudou: antes formatava a data para `dd/MM/yyyy` e depois reparseava essa string para ordenar; agora ordena pelo timestamp ISO cru, que já é cronológico como texto.

### 3.6 [MÉDIO] Explosão de requests nas proposições

- `ProposicoesRepository.observeRecentProposicoes` — para cada troca de filtro: 1 request de lista + 2 por proposição (detalhe + autores) = 31 requests. Roda toda vez que a Home abre e cada vez que o chip muda, sem TTL. Ver 4.1 sobre cancelamento.

### 3.7 [BAIXO] Logging de rede em release

- `HttpClientFactory.kt:26` instala `Logging` em nível `INFO` incondicionalmente. Junto com `Napier.base(DebugAntilog())` em `MagnaApplication.kt:17` e Koin em `Level.DEBUG` (`:20-21`), o release faz log de tudo. Custo de CPU e ruído no Logcat de produção. Ver seção 8 para o que fazer no lugar.

---

## 4. Repositórios e concorrência

### 4.1 [ALTO] Escopos de coroutine que nunca cancelam

**Status: resolvido.** O `factory<CoroutineScope>` saiu do `Modules.kt` e nenhum repositório recebe escopo. O único `CoroutineScope` que sobrou no projeto é o `rememberCoroutineScope()` do Compose, que é o certo.

Cada flow agora constrói o próprio trabalho de rede dentro de si, então ele é filho de quem coleta e morre junto. Sair da tela cancela.

Onde isso mais pesava: abrir um partido buscava a bancada e depois fazia uma requisição por membro, ~70 nos partidos grandes. Nada disso parava ao fechar a tela.

### 4.2 [MÉDIO] Quatro implementações do mesmo padrão "loading / erro / conteúdo"

**Status: unificado.** `data/repository/Resource.kt` tem um `Resource<T>` (`Loading` / `Error` / `Content(data, isRefreshing)`) e três construtores:

- `cachedRecord` — registro único vindo do cache, com refresh.
- `cachedList` — lista, onde vazio após refresh bem-sucedido é resposta válida, não falha.
- `networkResource` — só rede, para as telas sem cache atrás.

Os três cancelam junto com quem coleta e **relançam `CancellationException`** em vez de registrá-la como erro, que é o engano clássico de quem usa `runCatching` em coroutine.

Saíram `DeputadoDetailsResult`, `DeputadoExpensesResult`, `PartidoDetailsResult`, `ProposicaoDetailsResult` e `RecentProposicoesResult`.

Duas coisas ficaram de fora de propósito: `getComissaoPermanenteVotacoes` segue devolvendo `Result`, porque é uma leitura one-shot já governada por quem chama, e `getDeputados()` segue devolvendo `Flow<List<Deputado>>` porque nunca teve estado de erro para modelar.

O `PartidoDetailsResult`, que era um data class com quatro flags, virou dois flows que o ViewModel combina. A carga em duas fases da bancada continua igual na tela: emite os nomes primeiro e depois a mesma lista preenchida, usando o `isRefreshing` que o `Resource` já carrega.

### 4.3 [MÉDIO] `suspend fun` devolvendo `Flow`

**Status: corrigido.** Construir um `Flow` nunca suspende; o `suspend` só obrigava o chamador a estar numa coroutine à toa. Saiu dos repositórios e dos DAOs de deputado. Quem precisava do `legislaturaId` antes de montar o flow agora usa `flatMapLatest` sobre o usuário, que é o que o `getDeputadoExpenses` já fazia certo.

### 4.4 [MÉDIO] Sync inicial não tem TTL

- `SyncUserInformationUseCase` só sincroniza quando alguma tabela está vazia. Depois disso, `Deputado` só é atualizado quando a tela de busca abre (`getDeputados()` refaz o fetch dos 513 toda vez). `Partido` e `Orgao` nunca mais.
- `User` no domínio tem `lastSync`, mas a tabela `User.sq` não tem a coluna. Adicionar `lastSyncAt INTEGER` (mesma migração) e um TTL de, por exemplo, 24h.

### 4.5 [MÉDIO] Código morto ou meio-vivo

**Status: removido.** Saíram 38 arquivos e cerca de 1.100 linhas. Tudo foi conferido por varredura antes: as únicas referências que existiam eram auto-referência — registro no Koin ou o próprio código morto se citando.

**Votações do deputado.** A tela ficou órfã quando a feature saiu em `6b21146`: a rota continuava registrada, o ViewModel no Koin, mas o único caminho até ela era um `FloatingActionButton` comentado. Saíram tela, state, ViewModel, rota, o FAB, e em cascata `getDeputadoVotacoes` (que fazia 21 requisições para achar o voto de um deputado nas 20 votações mais recentes da Casa), `getRecentVotacoes`, `getVotacaoVotos`, `VotoItemDto`, `DeputadoVotacao` e seus DTOs. A `VotacoesApi` continua, com as duas chamadas que as comissões usam.

**Eventos.** `EventosApi`, `EventosRepository`, três DTOs, três respostas e o domínio `Pauta`. Estava registrado no Koin e nunca foi injetado em ninguém.

**Legislatura.** API, repositório, DAO, mapper, DTOs, respostas e as strings `welcome_*`. O usuário segue fixo na legislatura 57, que é o que já acontecia — `UserDao.setupInitialUser()` sempre gravou `"57"` na mão.

A **tabela `Legislatura` continua no banco**, de propósito: removê-la exigiria migração e mexer nas chaves estrangeiras de cinco tabelas, que é a decisão em aberto do item 2.5. Como nunca foi populada, mantê-la não custa nada.

**Miudezas:** `ExpenseRow` (composable sem uso), `domain/User.kt`, `orgaosMock`, `mockedLegislaturas`, seis drawables órfãos e quatro strings que ninguém resolvia.

### 4.6 [BAIXO] Miudezas

- ~~`PartidosRepository.kt:129` — `?: "57"` hardcoded como fallback de legislatura.~~ **Removido no bloco 4.**
- `PartidoDetailsViewModel.kt:27` — `CURRENT_YEAR = 2026` fixo; `currentYear()` já existe em `util/DateUtils.kt`.
- ~~`ProposicoesRepository.kt:25,29` — `siglaTipoDao` e `siglatipoDao` são a mesma dependência injetada duas vezes.~~ **Removido no bloco 4**, junto com a `VotacoesApi` que o repositório recebia sem usar.
- `UserRepository.userDao` é `val` público.
- `EventosApi` recebe `AppLoggerInterface`; nenhuma outra API recebe.
- `DeputadoDetailsMapper` classifica redes por `contains("twitter")`; links `x.com` são descartados.
- `MagnaComissaoPermanente` (enum de 6 comissões) mora em `data/repository/orgaos/params` — é configuração de produto, não de dados.
- `Legislatura` no domínio é `class`, não `data class`.
- Os `*Mock` em `data/domain/*.kt` (~800 linhas) vão para o APK porque os `@Preview` que os usam estão em `commonMain`. Aceitável, mas vale saber.

---

## 5. ViewModels e estado

### 5.1 [MÉDIO] Deputado sem despesas fica em "carregando" para sempre

**Status: corrigido.** O repositório passou a devolver `DeputadoExpensesResult` (`Fetching` / `Error` / `Success`), e `ExpensesState` ganhou `Empty`. A tela renderiza texto para vazio e para erro, em vez do `-> Unit` silencioso de antes.

Regra de precedência escolhida: cache não vazio ganha de requisição falhada. Mostrar a despesa da semana passada é melhor que mostrar erro porque a API da Câmara caiu agora. Erro só aparece quando não há nada em cache.

### 5.2 [BAIXO] `SharingStarted.Lazily`

- `ComissoesPermanentesViewModel` e `RecentProposicoesViewModel` usam `Lazily`; o flow do banco continua coletado depois que a UI some. `WhileSubscribed(5_000)` é o padrão para Compose.

---

## 6. UI (Compose)

### 6.1 [MÉDIO] Acessibilidade: ícones clicáveis sem descrição

**Status: corrigido.** Voltar, buscar, limpar e fechar eram ícones sem descrição: o TalkBack anunciava botão sem rótulo e não havia como saber qual voltava.

Os ícones decorativos mantiveram `null`, que é exatamente para isso que `null` serve. O que precisava de rótulo era o controle.

As duas top bars passaram a receber a descrição como parâmetro, com default "Voltar", que é o uso das seis telas. Nenhum call site mudou.

### 6.2 [MÉDIO] Arquivos de tela grandes demais

**Status: reduzido.**

| Arquivo | Antes | Depois |
|---|---|---|
| `PartidoDetailsScreen.kt` | 793 | 629 |
| `DeputadoDetailsScreen.kt` | 652 | 489 |
| `ProposicaoDetailsScreen.kt` | 630 | 367 |

Os gráficos saíram para `ui/component/chart/PartidoCharts.kt` — não têm nada de partido, são componentes de desenho. A sheet de despesa virou `DeputadoExpenseSheet.kt` e a lista de autores virou `ProposicaoAutores.kt`.

Na tela de proposição achei mais código morto que escapou do bloco 5: o `VotacaoCard` (97 linhas) não tinha call site, o `ProposicaoVotacoesState` nunca era produzido pelo ViewModel, e o parâmetro `votacoesTitle` era passado pela árvore inteira sem nunca ser usado no corpo. Saíram os três, mais a string que os alimentava.

### 6.3 [BAIXO] Home

**Status: corrigido.**

- O texto da busca vivia num `remember`, não num saveable: girar o aparelho perdia a consulta. Agora usa `rememberTextFieldState()`.
- O clique na lupa fazia `append("")`, que é literalmente nada. Agora abre a busca.
- O `BottomSheetScaffold` tinha corpo vazio e `sheetPeekHeight = 0.dp`. Era um `Scaffold` fantasiado e virou um.
- Enquanto o sync não terminava, a tela não renderizava nada: cold start ficava em branco até o diálogo aparecer. Agora mostra indicador de progresso.

### 6.4 [BAIXO] Strings

**Status: corrigido.** `cancel` virou "Cancelar", `loading` e `retry` saíram no bloco 5 por não terem uso, e o `contentDescription = "Clear"` em inglês virou recurso. "Mostrar menos" e "+ N autores" saíram do código para o `strings.xml`.

Sobraram interpolações de contagem (`"${state.partidos.size} partidos"`, `"${ufMembers.size} dep."`). São de baixo valor e mexer nelas é churn; ficam para quem for fazer localização de verdade.

### 6.5 [BAIXO] Tema e janela

**Status: corrigido.** A janela de launch é desenhada pelo sistema antes do Compose subir, usando `android:windowBackground`. Como o app herdava o tema claro da plataforma, cold start em modo escuro piscava branco. Agora existe `Theme.Magna` com `values` e `values-night`, espelhando `backgroundLight` e `backgroundDark` do `Colors.kt`.

O `CompositionLocalProvider` do `MagnaTheme` provia `LocalDimensions` com `LocalDimensions.current`, ou seja, o valor que já era. Era no-op e saiu.

---

## 7. Build, CI e release

### 7.1 [ALTO] O job de testes do CI não roda teste nenhum

**Status: corrigido.** `./gradlew testDebugUnitTest` existe só em `:androidApp`, que não tem nenhum teste — o job ficava verde sem executar nada. Confirmado ao rodar: `:androidApp:testDebugUnitTest` reporta `NO-SOURCE`.

O workflow agora roda `./gradlew :composeApp:jvmTest :androidApp:testDebugUnitTest`, mais um passo `:composeApp:verifySqlDelightMigration` antes dos testes. O glob de artefato (`**/build/test-results/**/*.xml`) já cobre a saída do `jvmTest`, não precisou mudar.

### 7.2 [ALTO] Um teste, e é `assertEquals(3, 1 + 2)`

**Status: começado.** O placeholder `ComposeAppCommonTest.kt` saiu e entraram 21 testes em `commonTest`, todos verdes no `:composeApp:jvmTest`:

- `DeputadoExpenseMapperTest` (9) — formatação de data, `toLocal`/`toDomain`, e três testes que fixam por escrito os bugs de 2.3, 2.4 e 3.1 (o de 3.1 prova com `assertFailsWith` que uma data sem hora explode o mapper).
- `DeputadoDetailsMapperTest` (6) — parsing de redes sociais, incluindo o descarte de `x.com` descrito em 4.6.
- `StringUtilsTest` (6) — `normalizeForSearch`, acentuação e maiúsculas.

Os três testes que documentam bug passam de propósito: eles afirmam o comportamento atual e vão falhar quando o bloco 2 mudar o schema, forçando uma atualização consciente.

Próximos passos, sem dependências novas ainda: DTOs com JSON real contendo nulls. Depois, adicionando `kotlinx-coroutines-test` + `turbine` + `sqlite-driver` (`JdbcSqliteDriver.IN_MEMORY`): `SyncUserInformationUseCase`, filtros de `DeputadosSearchViewModel` e DAOs. Fakes à mão para as APIs (MockK não roda em `commonTest`).

### 7.3 [MÉDIO] `-Xskip-prerelease-check`

**Diagnóstico corrigido.** A suspeita de dependência estava errada. A saída do compilador diz: `Following manually enabled features will force generation of pre-release binaries: ExplicitBackingFields`. Ou seja, é o próprio projeto: `composeApp/build.gradle.kts` liga `-XXLanguage:+ExplicitBackingFields`, o que marca os binários do `:composeApp` como pre-release, e por isso o `:androidApp` precisa do `-Xskip-prerelease-check` (linha 57) para consumi-los.

E a flag não é usada: `grep` por `field =` em property não acha nenhuma ocorrência em `composeApp/src` nem `androidApp/src`.

Correção: remover as duas flags (`-XXLanguage:+ExplicitBackingFields` do `composeApp` e `-Xskip-prerelease-check` do `androidApp`) e compilar Android + iOS + JVM para confirmar. Some a flag insegura e o warning de build junto.

### 7.4 [MÉDIO] `whatsnew` da Play Store anuncia feature removida

- `distribution/whatsnew/whatsnew-pt-BR:6` — "Histórico de votações de cada deputado, com filtros por tipo de voto". Removido em `6b21146`. Corrigir na 1.1.

### 7.5 [BAIXO] `dataExtractionRules` com schema errado

**Status: corrigido.** `dataExtractionRules` apontava para o arquivo com root `<full-backup-content>`, que o Android 12+ não aceita nesse atributo, então a exclusão do banco provavelmente não valia em aparelho novo. Agora tem arquivo próprio no schema certo, com `cloud-backup` e `device-transfer`. O `fullBackupContent` continua apontando para o antigo, que é o que o Android 11 e abaixo leem.

### 7.6 [BAIXO] ProGuard

- `-keep class app.cash.sqldelight.** { *; }` e `-keep @kotlinx.serialization.Serializable class com.tick.magna.** { *; }` são mais largos que o necessário. Funciona; só infla o APK.

---

## 8. Instrumentação (analytics + crash)

### 8.1 Situação original

- `androidApp/build.gradle.kts` declarava `firebase-analytics` e `firebase-crashlytics` e aplicava os plugins. `grep -ri firebase` no código Kotlin: zero resultados.
- Crashlytics funcionava sem código (captura crash, sobe mapping via plugin). Mas nenhum log do Napier chegava lá, então um crash vinha sem breadcrumbs.
- Analytics coletava `screen_view` automático por Activity. O app tem uma Activity. Ou seja: sabia-se que o app abriu e quanto tempo ficou aberto. Nada mais.

**Status: infraestrutura no ar.** Ver 8.4 para o que já reporta e o que falta.

### 8.2 Proposta

Manter Firebase (já está no projeto, DebugView é ótimo para validar) e isolar atrás de interface para não vazar Android no `commonMain` e poder trocar por Aptabase/PostHog no futuro se iOS/desktop virarem reais.

**Em `commonMain`:**

```kotlin
interface AnalyticsInterface {
    fun track(event: AnalyticsEvent)
    fun setUserProperty(key: String, value: String?)
}

sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {
    data class ScreenView(val screen: String) : AnalyticsEvent("screen_view", mapOf("screen_name" to screen))
    data object SyncStarted : AnalyticsEvent("sync_started")
    data class SyncFinished(val success: Boolean, val durationMs: Long) : AnalyticsEvent("sync_finished", mapOf("success" to success, "duration_ms" to durationMs))
    data class DeputadoOpened(val source: String) : AnalyticsEvent("deputado_opened", mapOf("source" to source))
    data class SearchPerformed(val queryLength: Int, val results: Int, val filters: String) : AnalyticsEvent("search_performed", ...)
    data class ExpenseOpened(val hasDocument: Boolean) : AnalyticsEvent("expense_opened", ...)
    data class ExternalLinkOpened(val kind: String) : AnalyticsEvent("external_link_opened", ...)
    data class ProposicaoFilterChanged(val tipo: String) : AnalyticsEvent("proposicao_filter_changed", ...)
    data class ProposicaoOpened(val tipo: String, val source: String) : AnalyticsEvent("proposicao_opened", ...)
    data class PartidoOpened(val source: String) : AnalyticsEvent("partido_opened", ...)
    data class PartidoChartSelected(val chart: String) : AnalyticsEvent("partido_chart_selected", ...)
    data class ComissaoOpened(val id: String) : AnalyticsEvent("comissao_opened", ...)
    data class ApiError(val endpoint: String, val status: Int?) : AnalyticsEvent("api_error", ...)
}
```

Regras: nomes `snake_case`, até 25 parâmetros, nunca texto livre (o termo de busca não vai; só o tamanho e a contagem de resultados). O app não tem login e não deve mandar nenhum identificador de pessoa. `legislatura_id` vira user property.

**Por plataforma (`platformModule`):**

- Android: `FirebaseAnalyticsTracker` chamando `Firebase.analytics.logEvent(name, bundle)`.
- iOS e JVM: `LogAnalyticsTracker` que só faz `Napier.d`. Isso mantém o `commonMain` compilando e permite testar com um `FakeAnalytics` que grava numa lista.

**Screen view em um lugar só:** em `App.kt`, um `LaunchedEffect(navController)` coletando `navController.currentBackStackEntryFlow` e mandando `ScreenView(route.simpleName)`. Nenhuma tela precisa saber de analytics. Desligar o automático do Firebase no manifest (`firebase_analytics_automatic_screen_reporting_enabled = false`) para não contar `MainActivity` em dobro.

**Eventos de ação:** disparados no `ViewModel` (não na UI), nos `processAction` que já existem. Os pontos: `HomeViewModel.trySync` (start/finished com duração), `DeputadosSearchViewModel.handleFilter`, `RecentProposicoesViewModel.updateFilter`, `PartidoDetailsViewModel.processAction`, e as navegações (`deputado_opened` com `source` = `recent | search | home_search | autores | membros`). O `source` é o dado mais valioso: diz qual seção da Home está sendo usada.

**Crashlytics com contexto:** um `CrashlyticsAntilog` em `androidApp` (só release) que faz `Napier.e` → `recordException(throwable)` e `w/i` → `log(message)` (vira breadcrumb). Em debug continua `DebugAntilog`. Isso resolve também o item 3.7: o Ktor `Logging` e o Koin `Level.DEBUG` ficam atrás de `BuildConfig.DEBUG`.

**Erros de API como sinal:** `api_error(endpoint, status)` é o evento que vai responder "a API da Câmara está derrubando meus usuários?". Com `HttpTimeout` (3.2), timeout vira `status = null`.

**Validação:** `adb shell setprop debug.firebase.analytics.app com.tick.magna` e olhar o DebugView no console. Eventos custom aparecem em relatórios em até 24h.

**Play Console:** ao publicar a 1.1, o formulário Data Safety precisa ser atualizado. Isso é o último bloco do plano — ver seção 10.

### 8.3 Perguntas que essa instrumentação responde

- Qual seção da Home recebe cliques (recentes, proposições, comissões, partidos).
- Alguém abre despesas e o documento PDF.
- Busca da Home vs. tela de busca com filtros.
- Taxa de falha do sync inicial e tempo médio (mede a API da Câmara, não o app).
- Sessões por semana dos usuários constantes, e em qual tela eles passam mais tempo.

### 8.4 O que já está implementado

Camada compartilhada, em `composeApp/src/commonMain/.../data/analytics/`:

- `AnalyticsInterface` com `track(event)` e `setUserProperty(key, value)`.
- `AnalyticsEvent`, o catálogo inteiro dos 13 eventos como `sealed class`. As regras do Firebase (nome snake_case, limites de 40 e 100 caracteres, prefixos reservados) são verificadas por teste, não por convenção.
- `LogAnalytics`, o tracker padrão que só escreve no log. É o que iOS e desktop usam.
- `toScreenName()`, que reduz a rota type-safe a um nome curto e estável.

Android, em `androidApp`:

- `FirebaseAnalyticsTracker` com `FirebaseAnalytics.getInstance(context)` e conversão de parâmetros para `Bundle`.
- `CrashlyticsAntilog`, que manda `Napier.w/i` para `crashlytics.log()` (breadcrumb) e `Napier.e` para `recordException()`. Ativo só em release; debug continua com `DebugAntilog`.
- `MagnaApplication` calcula `isDebug` por `ApplicationInfo.FLAG_DEBUGGABLE` (sem precisar de `BuildConfig`) e registra o módulo Firebase por cima do padrão.
- Manifest desliga `google_analytics_automatic_screen_reporting_enabled`, senão a `MainActivity` seria contada em paralelo ao rastreio real.

Gating de log (resolve o item 3.7): `AppBuildConfig(isDebug)` vem do `platformModule` de cada plataforma. Em release o Ktor `Logging` não é instalado, o `prettyPrint` do JSON sai, e o logger do Koin fica fora.

Eventos ligados: `screen_view` (automático, via back stack), `sync_started`, `sync_finished(success, duration_ms)`, `sync_step_failed(step)`, `api_error(endpoint, status)`, `content_empty(content)`, `search_performed`, `proposicao_filter_changed`, `comissao_opened(sigla)` e `partido_chart_selected`.

Três decisões que valem registro:

- **`api_error` sai do cliente Ktor, não dos repositórios.** Um `HttpResponseValidator` no `HttpClientFactory` cobre tudo de uma vez, inclusive as falhas que nunca produzem resposta (timeout, sem conexão) — justamente o caso que mais importa num app usado offline. A alternativa seria instrumentar quinze blocos `catch`.
- **`toEndpointName()` mascara id.** A URL crua (`/api/v2/deputados/204528/despesas`) diz qual político a pessoa estava lendo, e cada id viraria uma linha no relatório. Qualquer segmento com dígito vira `{id}`. Nenhum nome de endpoint da API da Câmara tem dígito, então a regra é segura e pega também id não-numérico como o `2358471-1` das votações. `EndpointNameTest` garante que nenhum dígito escapa.
- **`sync_step_failed` mudou comportamento de propósito.** O `if` antigo usava `&&`, que curto-circuita: se partidos falhasse, o use case emitia `Retry` sem esperar os outros três, deixando coroutines órfãs rodando. Agora os quatro são aguardados e o passo que falhou é nomeado.

A busca usa debounce de 1s só para o evento; a busca em si continua rodando a cada tecla. Digitar "tabata" reportava seis buscas, cinco das quais ninguém fez.

**Testes: 53 passando.**

### 8.5 O que falta (terceira fatia)

- **Eventos de navegação com `source`**: `deputado_opened`, `proposicao_opened`, `partido_opened`. Ficaram de fora de propósito: só valem se as quatro fontes de `deputado_opened` entrarem juntas. Se só uma reportasse, o relatório diria que todo mundo chega por ali. Dado parcial de `source` engana mais que ausência de dado.
- `expense_opened` e `external_link_opened` — o enum `LinkKind` já existe, falta chamar nas telas.
- `setUserProperty(legislatura_id)` no fim do sync.
- Um teste de grafo do Koin (`checkModules`) em `jvmTest`.

### 8.6 Configuração do Firebase, fora do código

Dois ajustes que **não são retroativos** e por isso valem antes de a 1.1 sair:

- **Retenção de dados**: o padrão do Analytics é 2 meses. Subir para 14.
- **Export para o BigQuery**: grátis, diário, e entrega o evento cru sem os limiares que o painel aplica a audiência pequena. Com poucos usuários, é a diferença entre ver os dados e olhar para um relatório vazio.

Com essa base de usuários, contagem de evento não vai ter significado estatístico. O que rende é sinal de presença: "alguém já abriu despesas?", "o sync falha, e em qual passo?". O catálogo foi desenhado para isso.

---

## 9. Sequência sugerida para a 1.1

Cada bloco cabe numa sessão isolada e foi pensado para não conflitar com o outro em arquivos.

| Bloco | Escopo | Arquivos principais | Pré-requisito |
|---|---|---|---|
| 0 | ~~Baseline de migração (`1.db`), CI rodando `:composeApp:jvmTest`, primeiro teste real~~ **FEITO** | `migrations/1.db`, `android-release.yml`, `commonTest` | nenhum |
| 1 | Analytics + `CrashlyticsAntilog` + gate de logs em release — **base pronta**, faltam os eventos de navegação (8.5) | `AnalyticsInterface`, `platformModule`, `App.kt`, `MagnaApplication.kt`, ViewModels (`processAction`) | 0 |
| 2 | ~~Despesas: schema, `1.sqm`, upsert, `Error` state, formatação pt-BR, parâmetro `ano`~~ **FEITO** | `DeputadoExpense.sq`, `1.sqm`, `2.db`, mapper, DAO, repositório, `DeputadosApi.kt`, tela | 0 |
| 3 | ~~`HttpTimeout` + `HttpRequestRetry`; DTOs nuláveis com testes de JSON real; `getPartidos` com `idLegislatura`~~ **FEITO** | `HttpClientFactory.kt`, `ApiJson.kt`, `dto/*.kt`, `response/*.kt`, `PartidosApi.kt` | 0 |
| 4 | ~~Remover `factory<CoroutineScope>`; repositórios sem `launch`; `Resource<T>` unificado~~ **FEITO** | `Resource.kt`, `Modules.kt`, `repository/**` | 2, 3 |
| 5 | ~~Decisão e remoção de código morto (Votações do deputado, Eventos, Legislatura)~~ **FEITO** | ver 4.5 | nenhum |
| 6 | ~~Split de telas, `contentDescription`, strings, splash/dark window, `dataExtractionRules`~~ **FEITO** | `features/**/*Screen.kt`, `ui/component/chart/`, `strings.xml`, manifest | nenhum |
| 7 | Feature nova da 1.1 | — | 0, 1, 2 |
| 8 | **Último bloco, já com tudo pronto para publicar:** Data Safety, política de privacidade, `whatsnew`, bump de versão. Ver seção 10 | Play Console, `distribution/whatsnew/`, `androidApp/build.gradle.kts` | todos |

Blocos 0, 1 e 2 são os únicos que eu não deixaria para depois da feature: 0 porque sem ele a 1.1 crasha na atualização, 1 porque sem ele a 1.1 sai sem dado nenhum, e 2 porque é o bug visível para quem usa hoje.

O bloco 8 é o único que tem que ser literalmente o último: ele descreve o app como ele ficou, então só faz sentido quando o resto parou de mudar.

---

## 10. Bloco 8 — publicação da 1.1

Este é o último bloco por construção: ele descreve o app como ele ficou, então qualquer item acima que ainda esteja em aberto invalida o que for preenchido aqui. Fazer só quando o código parar de mudar e a 1.1 estiver pronta para subir.

### 10.1 [BLOQUEANTE] Política de privacidade

`grep -ri "privac|lgpd"` no repositório inteiro: nenhuma ocorrência. A 1.0 quase certamente foi publicada declarando que não coleta dado nenhum, o que era verdade — o Firebase estava no Gradle mas nenhuma linha de código o usava.

Isso mudou. A partir da 1.1 o app coleta dados, e o Google Play **exige uma URL de política de privacidade** para qualquer app que colete dados. Sem ela a submissão é rejeitada, e essa é a falha mais provável de travar a release em cima da hora.

O que a política precisa cobrir, dado o que o app realmente faz:

- quais dados são coletados (interações no app, dados de diagnóstico e identificador de instalação);
- para que servem (entender uso e corrigir falhas — nada de publicidade);
- quem processa (Google, via Firebase Analytics e Crashlytics);
- que não há login, que nenhum dado pessoal é enviado, e que termos de busca não saem do aparelho;
- contato para pedidos de exclusão, exigência prática da LGPD.

O texto pode ficar no próprio repositório, publicado por GitHub Pages, e a URL entra na ficha da Play Store. É o caminho mais barato e mantém a política versionada junto do código que ela descreve.

### 10.2 [BLOQUEANTE] Formulário Data Safety

A declaração precisa refletir Firebase Analytics **e** Crashlytics. A lista abaixo é um rascunho de trabalho, não a palavra final: **confira contra a página oficial do Firebase sobre Data Safety antes de enviar**, porque o que cada SDK coleta muda entre versões e o formulário é auditável.

| Tipo de dado | Categoria no formulário | Coletado | Compartilhado | Finalidade |
|---|---|---|---|---|
| Eventos e telas | App activity → App interactions | Sim | Não | Analytics |
| Logs de erro | App info and performance → Crash logs | Sim | Não | Analytics, Diagnóstico |
| Diagnóstico | App info and performance → Diagnostics | Sim | Não | Analytics, Diagnóstico |
| ID de instalação | Device or other IDs | Sim | Não | Analytics |

Pontos do formulário que costumam ser respondidos errado:

- **"Device or other IDs" é o item mais esquecido.** O Firebase Analytics gera um App Instance ID e o Crashlytics um identificador de instalação. Não é o usuário, mas o formulário conta como identificador e precisa ser declarado.
- **Coletado, não compartilhado.** O Google atua como processador em nome do desenvolvedor. "Shared" no vocabulário do Play significa repassar para um terceiro independente, o que não acontece aqui.
- **Nenhum dado é obrigatório.** Todos entram como opcionais para o uso do app.
- **Criptografado em trânsito:** sim, o Firebase usa HTTPS.
- **Exclusão a pedido:** tem que existir um caminho, e o e-mail da política de privacidade serve.
- Marcar que o app **não** coleta nome, e-mail, localização precisa, contatos, nem dado financeiro. É verdade e vale confirmar item a item, porque o padrão do formulário não é esse.

O catálogo em `AnalyticsEvent.kt` é a fonte de verdade para preencher isso: cada evento está lá com seus parâmetros, e `AnalyticsEventTest` garante que nenhum texto livre é enviado. Reler os dois antes de responder o formulário é mais rápido do que tentar lembrar.

### 10.3 Antes de subir

- **`whatsnew`** (item 7.4): `distribution/whatsnew/whatsnew-pt-BR` ainda anuncia "Histórico de votações de cada deputado, com filtros por tipo de voto", removido em `6b21146`. Reescrever para a 1.1.
- **Versão**: `androidApp/build.gradle.kts` está em `versionCode = 3`, `versionName = "1.0.1"`. Subir os dois.
- ~~**`dataExtractionRules`**~~ **corrigido no bloco 6.**
- **Validar os eventos em aparelho real** antes de confiar no relatório: `adb shell setprop debug.firebase.analytics.app com.tick.magna` e acompanhar o DebugView. Eventos custom levam até 24h para aparecer nos relatórios normais, então o DebugView é o único jeito de saber na hora se a instrumentação está certa.
- **Conferir que o release não loga**: com `AppBuildConfig`, um build de release não deve imprimir requisição do Ktor nem log do Koin. Vale um `adb logcat` rápido no APK assinado.
