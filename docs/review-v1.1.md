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

### 1.1 Escopo de plataformas — decidido em 2026-09-19

A 1.1 é uma release **Android**. iOS e Desktop saem da lista de preocupações: nenhum dos dois tem release, nenhum dos dois tem plano, e nenhum item deste documento deve ser adiado, redesenhado ou enfraquecido por causa deles.

Isso **não** significa desmontar o Compose Multiplataforma. A arquitetura continua como está — `commonMain` segue sendo onde o código mora, `expect/actual` continua servindo driver de banco e data. O que muda é o status: ser multiplataforma deixa de ser requisito. Quando uma decisão for boa para Android e chata para os outros alvos, ela é tomada pelo Android, e o alvo que quebrar fica quebrado até alguém se importar.

Duas ressalvas concretas, porque "largar iOS e Desktop" tem um limite técnico:

- **O alvo `jvm()` não pode sair do Gradle.** O job de teste da CI roda `./gradlew :composeApp:jvmTest` (`.github/workflows/android-release.yml:27`), e é ele que executa `commonTest`. O que está descontinuado é o *app* desktop (`composeApp/src/jvmMain/kotlin/com/tick/magna/main.kt`, alvo `:composeApp:run`), não o alvo de compilação JVM. Tirar `jvm()` do `composeApp/build.gradle.kts` derruba a suíte inteira.
- **`iosArm64()`, `iosSimulatorArm64()` e `iosMain` ficam onde estão.** Remover dá trabalho, não dá ganho nenhum na Play Store e fecha a porta à toa. Eles simplesmente não são compilados nem verificados, e podem quebrar sem que isso bloqueie nada.

Consequência direta no item 16.4: dos dois pendentes de verificação, só o **build de release com R8 continua obrigatório** — é o binário que vai para a loja. iOS deixa de ser pendência e passa a ser não-objetivo declarado.

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

**Play Console:** ao publicar a 1.1, o formulário Data Safety precisa ser atualizado. Isso é o último bloco do plano — ver seção 15.

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

### 8.5 Eventos de navegação (terceira fatia) — FEITO

As cinco fontes de `deputado_opened` entraram no mesmo commit, que era a condição combinada: `source` parcial engana mais que ausência de dado.

| Evento | Origem | Onde dispara |
| --- | --- | --- |
| `deputado_opened(home_search)` | resultado da busca da Home | `HomeViewModel`, via `HomeAction.SearchResultOpened` |
| `deputado_opened(recent)` | carrossel de recentes | `RecentDeputadosViewModel.onDeputadoOpened` |
| `deputado_opened(search)` | tela de busca com filtros | `DeputadosSearchViewModel.onDeputadoOpened` |
| `deputado_opened(autores)` | lista de autores da proposição | `ProposicaoDetailsViewModel.onAutorOpened` |
| `deputado_opened(membros)` | membros do partido | `PartidoDetailsViewModel.onMemberOpened` |
| `partido_opened(home_section)` | seção de partidos da Home | `PartidosComponentViewModel.onPartidoOpened` |
| `partido_opened(list)` | lista completa de partidos | `PartidosListViewModel.onPartidoOpened` |
| `proposicao_opened` | seção de proposições da Home | `RecentProposicoesViewModel.onProposicaoOpened` |
| `expense_opened(has_document)` | abertura da sheet de despesa | `DeputadoDetailsViewModel.onExpenseOpened` |
| `external_link_opened(kind)` | 4 destinos externos | ver abaixo |

Decisões:

- **O evento sai do ViewModel, não do composable.** Todos os pontos de origem já tinham um ViewModel; o `koinInject<AnalyticsInterface>()` dentro da árvore de composição quebraria os `@Preview`, que renderizam justamente os composables privados de conteúdo. As telas passam a lambda para baixo como já faziam com navegação.
- **`source` não virou argumento de rota.** Seria mais robusto (sobrevive a morte de processo), mas põe uma preocupação de analytics dentro do modelo de navegação e faz a mesma tela com duas origens virar duas entradas distintas na back stack.
- **`RecentDeputadosComponent` trocou `onNavigate: (Any) -> Unit` por `onDeputadoClick` e `onSearchClick`.** A assinatura pública não mudou; o que sumiu foi precisar de um `is DeputadoDetailsArgs` dentro de uma lambda para saber o que tinha sido clicado.
- **`LinkKind.DEPUTADO_WEBSITE` foi removido do catálogo.** `DeputadoDetails.urlWebsite` existe no domínio e no mapper, mas nenhuma tela renderiza. Um valor de dimensão que nada consegue emitir é pior que valor nenhum: o relatório parece completo. Sobraram `expense_document`, `proposicao_full_text`, `deputado_social` e `partido_website`, e um teste fixa esse conjunto — igual ao que já existia para `Source`.
- **`legislatura_id` sai no fim do sync**, em `SyncUserInformationUseCase.reportLegislatura()`, depois do passo de configuração para que a primeira execução reporte a linha que acabou de escrever. `UserRepositoryInterface` ganhou `getLegislaturaId()`. É user property, não parâmetro: particiona todo o relatório pela legislatura a que o dado local pertence, que é o que vai distinguir sessão em dado fresco de sessão presa na legislatura velha quando a próxima começar.

**Bug encontrado no caminho:** `ProposicaoAutores.kt` tinha `private const val AUTORES_INITIAL_COUNT` declarado duas vezes no mesmo arquivo — redeclaração, erro de compilação. Entrou na extração do bloco 6 e só apareceu agora porque nada compilou desde então.

**Teste de grafo do Koin: não feito, de propósito.** Exigiria `koin-test`, um source set `jvmTest` novo e `Module.verify()` com `extraTypes` para os ViewModels injetados por parâmetro — tudo isso sem poder rodar nada para conferir. E o risco que ele cobre é pequeno aqui: no DSL `module { X(get(), get()) }` a aridade errada é erro de compilação, não de runtime; sobra só o caso de tipo não registrado, e `AnalyticsInterface` já está no `loggingModule`. Fica para a passada de verificação em lote.

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
| 1 | Analytics + `CrashlyticsAntilog` + gate de logs em release + eventos de navegação com `source` (8.5) — **FEITO** | `AnalyticsInterface`, `platformModule`, `App.kt`, `MagnaApplication.kt`, ViewModels (`processAction`) | 0 |
| 2 | ~~Despesas: schema, `1.sqm`, upsert, `Error` state, formatação pt-BR, parâmetro `ano`~~ **FEITO** | `DeputadoExpense.sq`, `1.sqm`, `2.db`, mapper, DAO, repositório, `DeputadosApi.kt`, tela | 0 |
| 3 | ~~`HttpTimeout` + `HttpRequestRetry`; DTOs nuláveis com testes de JSON real; `getPartidos` com `idLegislatura`~~ **FEITO** | `HttpClientFactory.kt`, `ApiJson.kt`, `dto/*.kt`, `response/*.kt`, `PartidosApi.kt` | 0 |
| 4 | ~~Remover `factory<CoroutineScope>`; repositórios sem `launch`; `Resource<T>` unificado~~ **FEITO** | `Resource.kt`, `Modules.kt`, `repository/**` | 2, 3 |
| 5 | ~~Decisão e remoção de código morto (Votações do deputado, Eventos, Legislatura)~~ **FEITO** | ver 4.5 | nenhum |
| 6 | ~~Split de telas, `contentDescription`, strings, splash/dark window, `dataExtractionRules`~~ **FEITO** | `features/**/*Screen.kt`, `ui/component/chart/`, `strings.xml`, manifest | nenhum |
| 7 | Revisão do modelo de dados e das APIs; `documentation/api-map.md`. Ver seção 10 | leitura de `source/remote/api/**`, `repository/**`, `sqldelight/**`; nenhum código de feature | nenhum |
| 8 | Legislatura selecionável e persistida; escopo de dados por legislatura. Ver seção 11 | `User.sq`, `UserDao.kt`, `LegislaturasApi`, repositórios, Home, `AnalyticsEvent.kt` | 0, 1, 2, 7 |
| 9 | Comissões com conteúdo: composição, presidência, relator da votação, curadoria por atividade. Ver seção 12 | `OrgaosApi.kt`, `VotacoesApi.kt`, `VotacaoDetailDto`, `OrgaosRepository.kt`, `Orgao.sq`, telas de comissão | 7, 8 |
| 10 | Votos do deputado: arquivo anual do ano corrente sob permissão, validade explícita, incremental pela API. Ver seção 13 | tabela nova de voto + migração, parser de CSV, download com progresso, `VotacoesApi.kt`, tela do deputado | 7, 8 |
| 11 | Passada de design em todas as telas a partir de uma tela de referência; `documentation/design-system.md`. Ver seção 14 | `ui/core/theme/**`, `ui/component/**`, `features/**/*Screen.kt`, `App.kt` | 10 |
| 12 | **Último bloco, já com tudo pronto para publicar:** Data Safety, política de privacidade, `whatsnew`, bump de versão. Ver seção 15 | Play Console, `distribution/whatsnew/`, `androidApp/build.gradle.kts` | todos |

Blocos 0, 1 e 2 são os únicos que eu não deixaria para depois da feature: 0 porque sem ele a 1.1 crasha na atualização, 1 porque sem ele a 1.1 sai sem dado nenhum, e 2 porque é o bug visível para quem usa hoje.

O bloco 7 é novo e não entrega feature nenhuma: ele mapeia o que cada endpoint da Câmara aceita e o que o app guarda de cada um. Ele vem antes do 8 porque a legislatura selecionável depende justamente desse mapa — e porque é onde as simplificações de modelagem aparecem, antes de virarem mais uma camada por cima.

O bloco 9 é a única feature de produto da 1.1 que não é a legislatura. Ele existe porque o mapa do bloco 7 mostrou que a tela de comissão tem conteúdo disponível e não usado — inclusive dado que o app já baixa e descarta. Depende do 8 porque composição de comissão é escopada por legislatura.

O bloco 10 é a feature que o app sempre quis e nunca conseguiu: os votos de cada deputado. O mapa do bloco 7 mostrou por que a tentativa anterior não tinha como dar certo, e onde o dado realmente está.

O bloco 11 vem depois do 10 de propósito: as telas novas — seletor de legislatura, comissões e votos — têm que entrar na mesma passada de design, senão nascem fora do padrão que o bloco acabou de estabelecer.

O bloco 12 é o único que tem que ser literalmente o último: ele descreve o app como ele ficou, então só faz sentido quando o resto parou de mudar — incluindo a aparência, de onde saem as capturas da loja.

---

## 10. Bloco 7 — revisão do modelo de dados e das APIs

Este bloco não entrega feature. Ele existe porque o bloco 8 (legislatura selecionável) depende de uma pergunta que hoje ninguém no projeto sabe responder de cabeça: **quais endpoints da Câmara aceitam `idLegislatura`, quais só aceitam faixa de datas, e o que o app está guardando de cada um.**

O diagnóstico que originou o bloco é honesto e vale registrar: partes do app foram modeladas de forma mais complicada do que precisavam, porque as relações entre entidades da API são difíceis de manter na cabeça no meio de tantos ids e formas de query diferentes. A resposta para isso não é mais um refactor às cegas — é escrever o mapa uma vez e passar a decidir olhando para ele.

**A premissa da 1.0 sobre a API está revista.** Não se trata de dados não confiáveis para trás; a API entrega. O que precisa ser tratado é **conexão** — a experiência de quem está offline, com rede ruim ou no meio de um sync. Isso muda o item 11.2 e muda o desenho de estados do bloco 11.

**Aviso que vale para tudo abaixo e para o mapa inteiro:** a API não se comporta igual em todas as legislaturas, e muda com o tempo sem aviso. O que está medido aqui é retrato da legislatura 57 em 2026-09-19. **Antes de começar a trabalhar em qualquer domínio do app, rode alguns exemplos dos endpoints daquele domínio, na legislatura em que o trabalho vai acontecer.** A seção 0 do mapa explica o método e o porquê. Quando a medição não bater, corrija o mapa e anote a legislatura, em vez de contornar no código.

### 10.1 A varredura — FEITA

Concluída em 2026-09-19 contra `https://dadosabertos.camara.leg.br/api/v2`, com requisição real em todos os quinze endpoints que o app usa. O resultado está em **[`documentation/api-map.md`](../documentation/api-map.md)** e não é repetido aqui.

O que ele responde, e que o plano assumia sem saber:

- **`/proposicoes`, `/orgaos`, `/orgaos/{id}/membros` e `/votacoes` recusam `idLegislatura` com HTTP 400.** O 400 nomeia o parâmetro recusado no campo `instance`, o que torna a verificação barata de repetir.
- **`/legislaturas?itens=100` devolve as 57 legislaturas com `dataInicio` e `dataFim` numa página só.** As datas saem daí; nada precisa ser hardcoded.
- **`/votacoes` recusa intervalo maior que 3 meses** (`A diferença entre as datas não pode ser maior que 3 meses`). Uma legislatura inteira custa 16 janelas.
- **`codTipoOrgao` e faixa de datas não convivem em `/orgaos`**: juntos devolvem zero itens. Comissão por legislatura tem que sair de `/orgaos/{id}/membros`, onde cada registro traz o seu `idLegislatura`.
- **`ano` em despesas é ignorado sem `idLegislatura` junto** — devolve zero em vez de erro.

E três achados que viram trabalho, detalhados no item 4 do mapa:

- **[ALTO]** `getPartidoMembros` não manda `itens` e o padrão do endpoint é 15: a tela de partido mostra 15 de ~90 membros, **em produção, na legislatura atual**;
- ~~**[ALTO]** `getDeputados` não pagina. A 57 cabe numa página (879 de 1000), a **55 tem 1138** e a segunda página seria descartada em silêncio — o bug que a troca de legislatura destravaria~~ **CORRIGIDO**, seguindo o link `rel="next"`;
- **[MÉDIO]** `/orgaos?codTipoOrgao=2` devolve **30** comissões permanentes; `MagnaComissaoPermanente` fixa seis.

O custo por tela também foi medido: proposições recentes **31 requisições**, detalhe da comissão **21**, detalhe do partido **17**.

### 10.2 A resposta para "de onde vêm as datas sem hardcode"

Vêm da própria API. `GET /legislaturas` devolve exatamente isto:

```json
{"id":57,"dataInicio":"2023-02-01","dataFim":"2027-01-31"}
{"id":56,"dataInicio":"2019-02-01","dataFim":"2023-01-31"}
{"id":55,"dataInicio":"2015-02-01","dataFim":"2019-01-31"}
```

São 19 páginas a 3 itens por página — a lista inteira de legislaturas cabe numa chamada só com `itens` alto.

E a tabela `Legislatura` no banco **já tem as colunas certas**: `id TEXT PRIMARY KEY`, `startDate TEXT NOT NULL`, `endDate TEXT NOT NULL`. Ela nunca foi populada porque o bloco 5 removeu a API que a alimentava, mas o formato já estava desenhado para isto.

Ou seja, a regra fica:

1. sincronizar `/legislaturas` uma vez e guardar na tabela `Legislatura`;
2. endpoint que aceita `idLegislatura` → mandar o id;
3. endpoint que não aceita → ler `startDate`/`endDate` da legislatura escolhida e mandar como `dataInicio`/`dataFim`.

**Nenhuma data hardcoded, em lugar nenhum.** Isso também elimina o `CURRENT_YEAR = 2026` fixo do item 4.6 como padrão para o resto do app.

### 10.3 O entregável: uma tabela de referência

**Escrito:** [`documentation/api-map.md`](../documentation/api-map.md). Uma linha por endpoint que o app usa, com quatro colunas:

- **endpoint e parâmetros aceitos** (verificados, não presumidos);
- **quem chama no app** (arquivo e função);
- **o que vai para o banco** (tabela, e se é escopada por legislatura);
- **quantas requisições custa** uma abertura de tela.

A última coluna é a que dói: o item 3.6 já registrou explosão de requests nas proposições, e o bloco 5 removeu um `getDeputadoVotacoes` que fazia 21 requisições para responder uma pergunta. Ter o custo escrito ao lado da chamada é o que impede o próximo desses de nascer.

### 10.4 Simplificações que provavelmente saem daqui

Não é lista fechada — é onde procurar primeiro, com base no que já apareceu:

- **Comissões permanentes.** `OrgaosApi.getComissoesPermanentes()` chama `/orgaos?codTipoOrgao=2`, que já devolve todas as comissões permanentes. Mesmo assim, `MagnaComissaoPermanente` (em `data/repository/orgaos/params/`) fixa seis `idOrgao` na mão. Ou a curadoria das seis é decisão de produto e deve virar configuração explícita, ou ela é acidente e o app deveria mostrar o que a API devolve. Hoje são as duas coisas ao mesmo tempo.
- **`Orgao` e `Proposicao` sem `legislaturaId`.** Ver item 11.3. A decisão de como escopá-los sai deste bloco, não do seguinte.
- **`Proposicao.codTipo` guarda a sigla** (item 2.6), e existe uma tabela `SiglaTipo` separada. Uma das duas coisas está sobrando.
- **Chaves estrangeiras decorativas** (item 2.5). Com o mapa na mão dá para decidir quais relações são reais e quais só parecem.
- **`VotacoesApi.getVotacoesFromOrgao`** fixa `itens=20` e `ordenarPor=idProposicaoObjeto` sem que nada explique por quê.
- **`LIMIT 5` hardcoded no SQL** (item 2.7) — mesma família de problema, do lado do banco.

### 10.5 Conexão: o que o mapa precisa responder

Como a preocupação real não é a qualidade do dado e sim a rede, o mapa tem que deixar explícito, por chamada:

- **o dado tem cache local?** Se a resposta vai para uma tabela, a tela funciona offline depois do primeiro sync. Se a chamada é direta para a UI sem passar pelo banco, a tela quebra sem rede — e essas são as que precisam de estado de erro decente;
- **a chamada é do sync inicial ou sob demanda?** O `SyncUserInformationUseCase` já orquestra o primeiro; o resto é caso a caso;
- **o que a tela mostra enquanto não tem resposta**, e o que mostra quando a resposta nunca vem.

Isso alimenta direto o bloco 11: os estados de carregando, vazio, offline e erro só podem ser padronizados depois que estiver escrito quais telas podem cair em cada um.

### 10.6 Escopo — e o limite dele

Este bloco é **leitura, medição e documento**, mais as simplificações que forem óbvias e baratas depois do mapa pronto. Refactor grande de schema não entra aqui: ele vira item do bloco 8 ou fica para a 1.2, com o mapa justificando a decisão.

O sinal de que o bloco acabou é conseguir responder, sem abrir o navegador: "para mostrar X na legislatura Y, quais chamadas são feitas, com quais parâmetros, e o que fica no banco depois".

---

## 11. Bloco 8 — legislatura selecionável

A ideia original do Magna era ser um histórico da API da Câmara, com a `legislaturaId` como eixo do tempo. A 1.0 resolveu prendendo todo mundo na 57 (a atual), e o bloco 5 removeu o que sobrou da tentativa anterior.

O que travou o eixo na prática **não foi a qualidade dos dados** — a API entrega o histórico. Foi a dificuldade de manter na cabeça quais endpoints aceitam legislatura, quais só aceitam faixa de datas, e como as entidades se ligam. É exatamente isso que o bloco 7 resolve, e é por isso que ele vem antes deste.

Com o mapa pronto, este bloco devolve o eixo: a pessoa escolhe a legislatura, a escolha é persistida, e cada tela consulta o recorte certo. O que precisa ser tratado com cuidado aqui é **conexão**, não confiabilidade do dado: trocar de legislatura dispara sync, e sync depende de rede.

### 11.1 O que já existe — e é mais do que parece

A base de dados já está pronta para isso:

- `User.sq` tem a coluna `legislaturaId TEXT` e a query `updateUserLegislatura`.
- `UserDaoInterface.setUserLegislatura(legislaturaId)` existe e está implementado em `UserDao.kt:28`. **Ninguém chama.** Uma varredura por `setUserLegislatura` encontra só a declaração e a implementação.
- `UserDao.setupInitialUser()` (`UserDao.kt:16`) grava `User(0, "57")` na mão, com um comentário que já previa este bloco.
- As leituras dos repositórios **já reagem** ao usuário: `DeputadosRepository.kt:79,107,115,132` e `PartidosRepository.kt:73` usam `flatMapLatest` em cima de `userDao.getUser()`. Trocar a legislatura no banco já faria essas telas re-consultarem sozinhas.

Ou seja, boa parte do encanamento está feita. O que falta é a torneira, a lista de opções, e o que acontece com a água que já estava no copo.

### 11.2 De onde vem a lista de legislaturas

Da API, sem hardcode — o bloco 7 já responde isso na seção 10.2: `GET /legislaturas` devolve `id`, `dataInicio` e `dataFim` de cada uma, e a tabela `Legislatura` no banco já tem exatamente essas três colunas. Ela nunca foi populada porque o bloco 5 removeu a API que a alimentava; restaurar uma `LegislaturasApi` mínima é o caminho, e as datas que voltam com ela são o que faz os endpoints sem `idLegislatura` funcionarem.

Como a lista inteira cabe numa chamada só, ela pode ser sincronizada junto do sync inicial e ficar no banco. O seletor lê do banco, não da rede — quem está offline continua conseguindo trocar de legislatura para um recorte já baixado.

O que **não** vale fazer: filtrar a lista por "faixa confiável". A API entrega o histórico; se um recorte antigo vier vazio, isso é estado de tela (item 11.4), não motivo para esconder a opção.

### 11.3 [ALTO] O que é escopado por legislatura — e o que não é

Este é o ponto que decide se o bloco 8 funciona ou só parece funcionar. Das tabelas do banco:

| Tabela | Tem `legislaturaId` | Ao trocar de legislatura |
|---|---|---|
| `Deputado` | sim | re-consulta certo |
| `DeputadoDetails` | sim | re-consulta certo |
| `DeputadoExpense` | sim | re-consulta certo |
| `Partido` | sim | re-consulta certo |
| `Proposicao` | **não** | **mostra o dado da legislatura anterior** |
| `Orgao` | **não** | idem |
| `SiglaTipo` | não | tudo bem — é tabela de referência, não muda |

E não é só o banco: `ProposicoesApi.getProposicoes` (`ProposicoesApi.kt:19`) manda só `ordem=desc` e um `siglaTipo` opcional. As "proposições recentes" da Home são as mais recentes da Casa, ponto — trocar para a 55 continuaria mostrando proposição de 2026.

Aqui está a razão técnica, e ela foi verificada contra a API de produção (seção 10.1): **`/proposicoes` e `/orgaos` devolvem HTTP 400 se receberem `idLegislatura`.** Não é omissão do app; esses endpoints simplesmente não têm esse parâmetro. A janela deles é `dataInicio`/`dataFim`, e `/proposicoes` responde 200 com as duas.

O caminho, então, é um só — não há trade-off real:

1. **Escopar por data de apresentação.** ~~Mandar `startDate`/`endDate` como `dataInicio`/`dataFim`~~ — **esses dois filtram tramitação, não apresentação** (item 3.7 do mapa). Os certos são `dataApresentacaoInicio`/`dataApresentacaoFim`, e como o teto de intervalo é de 3 meses, uma legislatura inteira não cabe numa chamada: a janela fica no fim do mandato, que é onde moram as proposições mais novas dele. As datas vêm da API (item 10.2 do bloco 7), então nada é hardcoded.
2. **Adicionar `legislaturaId` a `Proposicao` e `Orgao`** para que o cache local seja escopado igual ao resto. É migração nova (`2.sqm`) — ver o item 16.3 antes, porque migração é justamente o que não compila no Windows hoje.

Se o item 2 for grande demais para a 1.1, o recuo aceitável é esconder as seções de proposições e comissões fora da legislatura atual até que o cache esteja escopado. O que **não** é aceitável é deixar como está: dado da legislatura errada apresentado como se fosse do recorte escolhido é pior do que seção ausente.

### 11.4 Re-sync ao trocar

As leituras reagem, os syncs não. `DeputadosRepository.kt:149` e `PartidosRepository.kt:168` têm o mesmo `private suspend fun legislaturaId(): String? = userDao.getUser().first()?.legislaturaId` — leitura de uma amostra só, usada pelos caminhos de `sync*`. Depois da troca, a tela nova fica vazia até alguém disparar um sync.

O que precisa acontecer na troca, em ordem:

1. gravar o novo `legislaturaId` (`setUserLegislatura`);
2. disparar o sync do recorte novo, reaproveitando `SyncUserInformationUseCase`, que já orquestra os passos em paralelo e já reporta `sync_step_failed` / `sync_finished`;
3. mostrar estado de carregamento enquanto isso, porque o banco vai responder vazio antes de responder certo. O estado de "vazio" e o de "ainda não sincronizado" **não podem ser a mesma tela** — é o mesmo erro do item 5.1.

Não apagar os dados da legislatura anterior. Como tudo é escopado por `legislaturaId`, voltar para a 57 é instantâneo se o dado ficou lá. O crescimento do banco é irrelevante nessa escala.

**E o sync vai falhar.** Trocar de legislatura é a única ação do app que depende de rede para produzir resultado visível, então ela é a que mais expõe conexão ruim. Três casos, e nenhum pode cair no estado genérico de erro:

- **sem rede na troca:** a escolha já foi gravada, mas o recorte novo está vazio. Ou a troca é revertida, ou a tela diz que o recorte está incompleto e oferece tentar de novo. Gravar e ficar em branco sem explicação é o pior dos três;
- **recorte já baixado antes:** deve funcionar offline, sem sync nenhum. É o argumento mais forte para não apagar o dado antigo;
- **sync parcial:** `SyncUserInformationUseCase` roda os passos em paralelo e alguns podem falhar sozinhos. A tela precisa saber a diferença entre "essa seção falhou" e "essa legislatura não tem isso".

Vale um TTL por legislatura em vez do sync global sem TTL do item 4.4 — mas isso é ganho, não requisito do bloco.

### 11.5 Onde a escolha fica na UI

Não existe tela de configurações hoje. Duas opções:

- **Seletor no topo da Home**, do lado do título. Deixa o eixo do tempo visível, que é a ideia original do produto. Mexe no `MagnaLargeTopBar`.
- **Tela de ajustes nova**, com a legislatura como primeiro item. Mais fácil de crescer depois (tema, cache, sobre), e mais escondido.

**Recomendação: as duas, em ordem.** Seletor na Home para a 1.1, porque é a feature da release e esconder a feature da release num menu é desperdício. A tela de ajustes vem quando houver um segundo ajuste.

Se o seletor entrar na Home, ele entra também no bloco 11 (design) — é componente novo numa tela que vai ser revisada de qualquer jeito.

### 11.6 Instrumentação

`SyncUserInformationUseCase.kt:82` já manda `USER_PROPERTY_LEGISLATURA` como user property. Isso continua valendo, mas passa a mudar em runtime — reenviar a property **depois** da troca, senão o relatório atribui a sessão inteira à legislatura antiga.

Adicionar ao catálogo de `AnalyticsEvent.kt`:

- `legislatura_changed` com `from` e `to`. É o evento que responde se alguém usa a feature — a pergunta que justifica o bloco existir.
- `api_error` já cobre a falha de `/legislaturas` pelo `HttpResponseValidator`, sem trabalho extra.

Manter a regra que o `AnalyticsEventTest` garante: nada de texto livre. `legislaturaId` é numérico e controlado, então passa.

### 11.7 Testes

O bloco 8 é o primeiro que mexe em estado global do app, então vale cobrir:

- trocar a legislatura e verificar que `getDeputados`/`getPartidos` re-emitem com o recorte novo — o `flatMapLatest` já deveria garantir, e o teste é para não perder isso num refactor;
- `setUserLegislatura` seguido de leitura devolve o valor gravado. Atenção ao `updateUserLegislatura: UPDATE User SET legislaturaId = ? WHERE id = 0` do `User.sq`: o `WHERE id = 0` só funciona porque `setupInitialUser` insere com id `0` explícito. Funciona, é frágil, e um teste é mais barato que descobrir isso em produção;
- o estado de "sincronizando após troca" não é confundido com "vazio".

---

## 12. Bloco 9 — comissões com conteúdo

A tela de comissão é a menos interessante do app, e o bloco 7 mediu por quê (mapa, seção 5): as vinte votações que ela mostra dizem uma de duas frases, todas aprovadas, e o cabeçalho lê sempre `20 total · 20 aprovadas · 0 rejeitadas`.

Este bloco não inventa dado. **Tudo o que ele propõe já existe na API, e parte já está sendo baixada e descartada.** Cada afirmação abaixo foi verificada com requisição real em 2026-09-19, na CCJC (`idOrgao=2003`), que é a comissão com mais votações.

Ele vem depois do bloco 8 porque composição de comissão é escopada por legislatura, e antes do 10 porque as telas novas precisam entrar na passada de design.

### 12.1 O que a tela mostra hoje

`ComissaoPermanenteDetailScreen.kt` monta uma grade de cartões com data, a `descricao` da votação, uma tarja verde ou vermelha e a ementa da proposição afetada. O repositório (`OrgaosRepository.kt:56-90`) busca vinte votações e o detalhe de cada uma — 21 requisições.

Três coisas tornam isso pobre, e nenhuma é culpa do layout:

- **a `descricao` é carimbo processual**, não conteúdo: nas vinte medidas, só "Aprovado o Parecer." e "Aprovada a Redação Final.";
- **a tarja é sempre verde**: as vinte foram aprovadas, e isso não é amostra azarada — parecer em comissão é aprovado na esmagadora maioria das vezes;
- **as vinte não são as mais recentes**: `ordenarPor=idProposicaoObjeto` ordena por id de proposição, e o repositório reordena por data depois, o que faz um recorte arbitrário de 410 parecer "as últimas".

A ementa, que é a única parte com substância, já é exibida — `OrgaosRepository.kt:79` mapeia `proposicoesAfetadas.map { it.ementa }`.

### 12.2 Composição: a API organiza diferente do que parece

Confirmado por medição, porque a intuição de que "as comissões se organizavam de outro jeito" estava certa.

`GET /orgaos/{id}/membros` **não devolve uma lista de membros.** Devolve **um registro por passagem** — uma linha por (deputado, título, período), cada uma com `dataInicio`, `dataFim`, `idLegislatura`, `titulo`, `codTitulo`, `siglaPartido`, `siglaUf` e `urlFoto`.

O comportamento muda conforme os parâmetros, e essa é a parte que engana:

| Chamada | O que volta | CCJC |
|---|---|---|
| sem parâmetros | **só a composição vigente** (todos com `dataFim` nulo) | 130 registros, 2 páginas com `itens=100` |
| com `dataInicio`/`dataFim` | **o histórico de passagens** naquela janela | legislatura 57 → 902 registros, 10 páginas |

A composição vigente da CCJC, medida:

| Título | `codTitulo` | Quantidade |
|---|---|---|
| Presidente | 1 | 1 |
| 1º / 2º / 3º Vice-Presidente | 2 / 3 / 4 | 1 cada |
| Titular | 101 | 60 |
| Suplente | 102 | 66 |

Dois detalhes que mudam o desenho:

- **a composição é renovada todo ano.** Todos os 130 registros vigentes começam em 2026 — a sessão legislativa atual. "Quem está na comissão" é uma foto anual, não do quadriênio;
- **o histórico precisa ser reduzido.** Os 902 registros da legislatura 57 correspondem a **293 deputados distintos**. Qualquer tela de "quem passou pela comissão" tem que agrupar por deputado, senão mostra a mesma pessoa várias vezes.

E o achado que sozinho já justifica a tela: **a presidência tem mandato e rotaciona.** A CCJC teve quatro presidentes na legislatura 57, cada um com período:

| Presidente | Período |
|---|---|
| Rui Falcão | 2023-03-15 → 2024-03-06 |
| Caroline de Toni | 2024-03-06 → 2025-03-18 |
| Paulo Azi | 2025-03-19 → 2026-02-09 |
| Leur Lomanto Júnior | 2026-02-10 → atual |

Isso é uma linha do tempo pronta, com nomes que o app já tem nas telas de deputado, e liga comissão a deputado e a partido — as três entidades que hoje vivem separadas.

### 12.3 A votação: o que já está pago e é descartado

`GET /votacoes/{id}` devolve mais do que `VotacaoDetailDto` mapeia. Os campos ignorados hoje:

| Campo | Conteúdo real medido | Serve para |
|---|---|---|
| `ultimaApresentacaoProposicao` | `Parecer do Relator, Dep. Orlando Silva (PCdoB-SP), pela constitucionalidade, juridicidade e técnica legislativa.` | dizer **quem relatou e o que defendeu** |
| `siglaTipo` / `numero` / `ano` de `proposicoesAfetadas` | `PDL 476/2024` | rotular a proposição em vez de só mostrar a ementa solta |
| `objetosPossiveis` | 1 a 3 itens por votação | o que estava em pauta |
| `efeitosRegistrados` | vazio na amostra | efeito na tramitação — verificar em outras comissões |

**Ressalva importante, para não prometer o que a API não dá:** `ultimaApresentacaoProposicao.descricao` é **texto livre**. O nome do relator e o partido vêm embutidos na frase, não em campos separados. Mostrar a frase inteira é de graça; transformar "Dep. Orlando Silva (PCdoB-SP)" em link para a tela do deputado exige casar o texto com a lista de deputados que o app já tem no banco — factível, e sujeito a falhar em nome com grafia diferente. Começar exibindo a frase e tratar o link como melhoria depois.

Segunda ressalva, honesta: as votações recentes da CCJC são quase todas `PDL` de renovação de concessão de rádio e TV. Mostrar o rótulo e o relator torna isso **visível**, não emocionante. A tela fica honesta; o conteúdo é o que o Congresso produziu.

### 12.4 As seis comissões

A curadoria é decisão de produto — comissões de nome reconhecível e com votação suficiente para a tela não ficar vazia. Ela continua válida como intenção, mas está congelada como constante em `data/repository/orgaos/params/` e envelheceu: das seis, **CCTI (16 votações) e CAPADR (7) hoje estão atrás de oito comissões que o app não mostra**, entre elas CPOVOS (45) e CCULT (38). Números completos no item 4.3 do mapa.

Duas mudanças, e a segunda depende da primeira:

1. **mover o critério para onde ele é decisão**, fora de `data/`, com o porquê escrito junto (nome reconhecível + atividade). Isso resolve o item 4.6 do plano de quebra;
2. **ordenar por atividade em vez de fixar ids** — o app já baixa as 30 de `/orgaos?codTipoOrgao=2` e pode rankear. Mantém a intenção e para de precisar de revisão manual a cada legislatura.

**Pré-requisito do item 2, e não é opcional:** `ComissaoPermanenteDetailScreen.kt:73` usa `if (state.votacoes.isEmpty())` para decidir mostrar o `LoadingComponent`. Não existe estado de vazio, e **CASP tem zero votações**. Além disso, `OrgaosRepository.kt:81` filtra votações sem `proposicoesAfetadas`, então uma comissão pode esvaziar depois do filtro mesmo tendo votações. Nos dois casos a tela gira para sempre. O componente de estado vazio do bloco 11 é o que destrava abrir a curadoria.

### 12.5 Custo e cache

Hoje a tela gasta 21 requisições e **não guarda nada** — `OrgaosRepository.getComissaoPermanenteVotacoes` devolve direto para a UI, sem passar pelo banco. Sem rede, a tela não abre nem depois de já ter aberto uma vez.

O que este bloco acrescenta, medido:

| Adição | Requisições | Observação |
|---|---|---|
| composição vigente | 2 | `itens=100`, 130 registros |
| histórico de passagens da legislatura | 10 | 902 registros, agrupar por deputado |
| linha do tempo de presidentes | 0 extra | sai do histórico acima |

A composição vigente é barata e vale a pena; o histórico só se a tela de fato o usar. **Nada disso deveria ser feito sem cache**: composição de comissão muda uma vez por ano, e é o tipo de dado que pertence ao banco. As tabelas precisam de `legislaturaId`, o que conecta este bloco ao item 11.3 e a uma migração — ver o item 16.3 antes, porque migração é o que não compila no Windows hoje.

Aproveitar para resolver as 21 requisições: buscar votações por data com paginação honesta em vez de `ordenarPor=idProposicaoObjeto&itens=20`, e considerar se o detalhe de cada votação precisa ser buscado na lista ou só quando a pessoa abre uma.

### 12.6 O que este bloco não resolve

- **Votação nominal por deputado não existe aqui.** O bloco 5 removeu `getDeputadoVotacoes` porque custava 21 requisições para responder uma pergunta. Nada neste bloco o traz de volta.
- **`/orgaos/{id}/eventos` devolveu vazio** para a CCJC sem parâmetros de data. Antes de contar com reuniões como conteúdo, verificar com janela de datas.
- **`efeitosRegistrados` veio vazio** nas três votações inspecionadas. Pode ser da CCJC, pode ser geral — verificar antes de desenhar em cima.

---

## 13. Bloco 10 — votos do deputado

A tela de detalhe do deputado sempre quis mostrar como ele votou, e a tentativa anterior foi abandonada. O bloco 5 removeu o `getDeputadoVotacoes`, que fazia 21 requisições para procurar o voto de um deputado nas vinte votações mais recentes da Casa.

**Não foi falta de jeito: era a direção da busca.** Medido em 2026: a Câmara registrou **7.360 votações no ano, e só 152 têm voto nominal** — 2%. Buscar deputado a deputado nas votações recentes é garimpar num palheiro em que quase nada é agulha, e por isso a tela vinha quase sempre vazia.

Este bloco inverte: sincroniza **votação → votos** e deixa o índice local responder "votos do deputado X". Tudo medido em 2026-09-19.

### 13.1 A API não tem a relação invertida

Verificado endpoint por endpoint (o swagger é renderizado por JavaScript e não pôde ser lido; o que existe abaixo veio de requisição real):

| Chamada | Resultado |
|---|---|
| `GET /deputados/{id}/votos` | **405** — não existe |
| `GET /deputados/{id}/votacoes` | **405** — não existe |
| `GET /votacoes/{id}/votos` | **200** — é o único caminho |

`GET /votacoes/{id}/votos` devolve `tipoVoto`, `dataRegistroVoto` e o deputado inteiro (id, nome, partido, UF, foto). Numa votação de plenário medida vieram 62 votos.

**Armadilha que provavelmente matou a tentativa anterior: esse endpoint recusa `itens` com HTTP 400.** Quem tenta paginar leva erro ou lista vazia e conclui que não há dado. Sem parâmetro nenhum ele devolve tudo de uma vez, sem paginação — o `links` só traz `self`.

### 13.2 Os arquivos anuais, e por que eles são o caminho do histórico

Fora da API REST, o portal publica arquivo por ano:

```
https://dadosabertos.camara.leg.br/arquivos/votacoesVotos/csv/votacoesVotos-{ano}.csv
```

Colunas: `idVotacao;uriVotacao;dataHoraVoto;voto;deputado_id;deputado_nome;deputado_siglaPartido;deputado_uriPartido;deputado_siglaUf;deputado_idLegislatura;deputado_urlFoto`. É exatamente a relação que falta na API.

Medido em 2026 (até setembro):

| | |
|---|---|
| Linhas de voto | 51.832 |
| Votações nominais | 152 |
| Deputados distintos | 566 |
| Votos por deputado | mediana **102**, máximo 145 |
| Tipos de voto | Sim, Não, Abstenção, Obstrução, Artigo 17 |
| Dias do ano com votação nominal | **44** |

**Os arquivos não são estáticos: são regerados toda madrugada.** Conferido pelos headers — o arquivo de 2026 e o de 2025 tinham ambos `Last-Modified` da manhã do mesmo dia. Consequências:

- **o dado atrasa até ~24h**: votação da tarde só entra no arquivo da madrugada seguinte;
- **não há delta**: mudou, baixa tudo de novo — e muda todo dia, então `If-None-Match` quase nunca devolve 304;
- **`Range` não ajuda**: o servidor aceita (`accept-ranges: bytes`), mas o arquivo não está em ordem cronológica — a primeira linha do de 2026 é uma votação de junho. Não dá para baixar só o fim;
- **não há compressão**: pedir `Accept-Encoding: gzip` devolve os mesmos bytes. O tamanho anunciado é o tamanho real da transferência.

### 13.3 Tamanho — o que decide a regra de produto

| Ano | CSV | JSON |
|---|---|---|
| 2023 | 40,8 MB | 75,4 MB |
| 2024 | 36,9 MB | 68,2 MB |
| 2025 | 55,5 MB | 102,6 MB |
| 2026 (até setembro) | **16,4 MB** | 30,4 MB |
| **Legislatura 57 inteira** | **~150 MB** | ~277 MB |

Baixar uma legislatura completa está fora de questão num celular. **Usar CSV e não JSON** — mesma informação, quase metade do peso.

O que vai para o banco é muito menor que o download. O arquivo repete nome, URI, partido e URL da foto em toda linha; guardando só `idVotacao`, `deputadoId`, `voto` e a data, as 51.832 linhas de 2026 ficam na casa de **2 a 3 MB** de tabela. O custo é a transferência, não o armazenamento.

### 13.4 As regras de produto

Decididas com o peso na mão:

1. **Só o ano corrente.** O download é oferecido apenas para o ano em curso, e portanto só quando a legislatura selecionada é a atual. Legislatura antiga mostra um estado explicando que o histórico de votos não está disponível ali — não uma tela vazia sem motivo. *(Interpretação da regra: "ano corrente", não "legislatura corrente inteira". A legislatura 57 cobre cinco arquivos anuais e ~150 MB; o ano corrente é um arquivo e 16 MB.)*
2. **Pedir permissão antes, com o peso na tela.** Nunca baixar por conta própria, nem no sync inicial. A tela diz quantos megabytes são antes de qualquer transferência, e o download só começa depois do toque. O tamanho não é chute: `HEAD` no arquivo devolve `content-length` antes de baixar um byte.
3. **Validade explícita.** Depois de baixado, a tela declara **"dados completos até \<data\>"**, usando o `Last-Modified` do arquivo que foi baixado — não a hora do download.
4. **Desatualizado com botão de atualizar.** Passada essa data, o estado vira "desatualizado" e aparece o botão. **Um `HEAD` diz de graça se existe snapshot mais novo** — compara `Last-Modified`/`ETag` sem baixar nada. O botão só aparece se de fato houver o que baixar.

Isso resolve a parte desconfortável: o usuário decide, sabe o preço e sabe o que tem na mão.

### 13.5 Manter atualizado sem baixar de novo — segunda etapa, opcional

O item 4 acima já entrega uma feature honesta. Mas dá para mover a data de validade para frente sem transferir 16 MB, porque o incremental pela API é **muito** barato — desde que se saiba quais votações têm voto nominal sem chamar `/votos` nas 7.360.

O sinal está na `descricao`, que a API já devolve na listagem: votação nominal traz a contagem embutida no texto (`Aprovado o Requerimento de Urgência (Art. 155 do RICD). Sim: …`). Testado contra as 152 nominais de 2026:

| Filtro | Acerta | Falso-positivo | Perde |
|---|---|---|---|
| `descricao` contém `"Sim:"` ou `"Resultado:"` | **147 / 152** | **2** | 5 |
| `descricao` contém só `"Sim:"` | 122 / 152 | 2 | 30 |
| órgão é `PLEN` | 123 / 152 | 1.233 | 29 |

97% de cobertura com dois falsos positivos, usando campo que já vem de graça. O sync incremental fica:

1. `/votacoes` com `dataInicio`/`dataFim` desde o último sync — 1 chamada, respeitando o **teto de 3 meses** do item 3.2 do mapa;
2. filtrar por `"Sim:"` / `"Resultado:"`;
3. `/votos` só nas que passaram — **cerca de 3 por semana**.

E é raro de verdade: só 44 dias do ano tiveram votação nominal. A maior parte das semanas não tem nenhuma.

Os 3% que o filtro perde não somem para sempre: o botão de atualizar do item 13.4 rebaixa o arquivo e reconcilia. **O incremental mantém fresco, o arquivo mantém correto.**

*(Detalhe que explica o filtro: o arquivo `votacoes-{ano}.csv` tem as colunas `votosSim`/`votosNao`/`votosOutros`, e elas são discriminador perfeito — 151 das 152 nominais preenchidas, zero das 7.208 simbólicas. A API não devolve esse campo em lugar nenhum, nem na listagem nem no detalhe. O `"Sim:"` na descrição é o mesmo dado vazando pelo texto.)*

### 13.6 O que precisa existir no código

- **tabela nova** para o voto, com `idVotacao`, `deputadoId`, `voto`, data e `legislaturaId` — migração, então ver o item **16.3** antes;
- **parser de CSV** em `commonMain`. É a primeira vez que o app lê algo que não é JSON da API; o arquivo usa `;` como separador e vem com BOM;
- **download com progresso e cancelamento**, que também é novo. Ktor dá o `ByteReadChannel`; o que falta é a tela e o cancelamento decente (o bloco 4 já ensinou a tratar cancelamento como cancelamento);
- **escrita em lote no SQLDelight** — 52 mil linhas não entram uma a uma;
- **`/votacoes/{id}/votos` sem `itens`**, com um teste que trave isso, porque é o erro que a próxima pessoa vai repetir;
- **estados**: nunca baixado, baixando, completo até X, desatualizado, sem rede. São cinco, e o componente de estado vazio do bloco 11 é pré-requisito.

### 13.7 O que este bloco não promete

- **Legislatura antiga não tem a feature.** É consequência direta da regra do ano corrente, e a tela precisa dizer isso, não fingir que não há votos.
- **Voto nominal é minoria.** Mesmo com tudo sincronizado, a lista de um deputado tem ordem de **100 votos por ano**, não milhares. Isso é o que a Câmara registra nominalmente — o resto é simbólico e não tem nome atrelado.
- **Nada de atribuir voto simbólico a deputado.** Se a votação não é nominal, não existe registro individual. A tela precisa ser explícita quanto a isso, senão sugere ausência onde houve presença.
- **O atraso de ~24h continua** para quem só usa o arquivo. Só o incremental do item 13.5 encosta no tempo real, e mesmo ele depende de a Câmara publicar.

---

## 14. Bloco 11 — passada de design

O app foi montado tela a tela, à mão, ao longo do tempo. Isso funciona para chegar até aqui e cobra o preço exatamente agora: a 1.1 vai adicionar uma feature nova a um conjunto de telas que nunca foi olhado como conjunto.

O objetivo deste bloco não é redesenhar o Magna. É o contrário: **o app já tem identidade visual — ela só existe em uma tela.** O trabalho é escolher essa tela, extrair dela o sistema que ela já usa sem ter escrito, e aplicar esse sistema nas outras.

E tem um segundo objetivo, que é o que faz este bloco valer mais do que uma faxina: sair dele com **um sistema no qual a próxima feature se encaixa sem decisão de design nova**. Hoje, cada feature nova custa uma rodada de escolhas de padding, papel tipográfico e raio de canto. Depois deste bloco, não deveria custar.

Ele vem depois do bloco 10 (as telas de comissão e de votos entram na mesma passada, e a do seletor de legislatura também) e antes da publicação (as capturas da loja saem daqui).

O bloco 6 já fez a parte estrutural — quebrou os arquivos grandes, pôs `contentDescription`, moveu strings. Este é o passo seguinte, e é de aparência, não de arquitetura.

### 14.1 A tela de referência

**Primeira decisão do bloco, e ela é de gosto, não técnica: qual tela é a referência.** Nada abaixo começa antes disso, porque "unificar" sem um alvo vira média de tudo — e média de decisões inconsistentes dá um resultado pior do que a melhor delas.

Os candidatos, pelo que cada um oferece como fonte:

- **Home** (`MagnaHomeScreen.kt`) — é onde mora a maior variedade de componentes (busca, listas horizontais, seções, diálogo de sync) e é a primeira tela que qualquer pessoa vê. É a favorita se a identidade que você gosta está no arranjo das seções;
- **Detalhe do deputado** (`DeputadoDetailsScreen.kt`) — cabeçalho com avatar, blocos de metadado, sheet de despesas. É a favorita se a identidade está na forma como a informação densa é apresentada;
- **Detalhe do partido** (`PartidoDetailsScreen.kt`) — é a única com gráficos, e portanto a única que já teve que decidir cor de série e escala.

Escolhida a tela, o passo seguinte é **extrair**, não descrever: abrir o arquivo e anotar, valor por valor, qual padding de borda ela usa, qual papel tipográfico ela dá a cada função de texto, qual raio de canto, como separa seção, como trata estado vazio. Essa anotação é o sistema. As seções seguintes são as categorias que ela precisa cobrir, e o item 14.10 é como transformar isso em algo que a próxima feature herde de graça.

### 14.2 O inventário

Todas as telas e componentes de tela que precisam passar pela revisão:

| Tela | Arquivo |
|---|---|
| Home | `features/home/MagnaHomeScreen.kt` |
| Busca de deputados | `features/deputados/search/DeputadosSearchScreen.kt` |
| Detalhe do deputado | `features/deputados/details/DeputadoDetailsScreen.kt` |
| Sheet de despesas | `features/deputados/details/DeputadoExpenseSheet.kt` |
| Lista de partidos | `features/partidos/list/PartidosListScreen.kt` |
| Detalhe do partido | `features/partidos/details/PartidoDetailsScreen.kt` |
| Detalhe da proposição | `features/proposicoes/details/ProposicaoDetailsScreen.kt` |
| Autores da proposição | `features/proposicoes/details/ProposicaoAutores.kt` |
| Detalhe da comissão | `features/comissoes/permanentes/detail/ComissaoPermanenteDetailScreen.kt` |
| Componentes da Home | `deputados/recent/RecentDeputadosComponent.kt`, `partidos/component/PartidosComponent.kt`, `proposicoes/component/RecentProposicoesComponent.kt`, `comissoes/permanentes/component/ComissoesPermanentesComponent.kt` |
| Compartilhados | `ui/component/LoadingComponent.kt`, `ui/component/SomethingWentWrongComponent.kt`, `ui/component/chart/PartidoCharts.kt`, `ui/core/avatar/Avatar.kt`, `ui/core/image/MagnaImage.kt` |
| Seletor de legislatura | novo, vindo do bloco 8 |

### 14.3 Spacing — a régua existe, metade do app a ignora

`ui/core/theme/Dimensions.kt` define a escala (`grid0` a `grid40`) e a expõe por `LocalDimensions`. A adoção é parcial: **53 usos de `LocalDimensions` contra 58 valores `.dp` crus** espalhados por 13 arquivos de `features/` e `ui/`. Os piores: `ui/component/chart/PartidoCharts.kt` (9), `deputados/recent/RecentDeputadosComponent.kt` (8), `partidos/details/PartidoDetailsScreen.kt` (5).

O que fazer:

- passar os `.dp` crus para tokens, e quando um valor não existir na escala, decidir: ou ele vira token, ou ele vira o token vizinho. O que não pode é continuar solto;
- revisar a própria escala. `grid20`, `grid28` e `grid36` existem e provavelmente aparecem uma vez cada — escala com furo demais não é escala, é lista de números com nome. Menos tokens, mais consistência;
- fixar um padding de borda de tela único e aplicar nas dez telas. Hoje cada uma escolheu o seu.
- fixar o espaçamento vertical entre seções da Home, que hoje é somatório de `Spacer` avulsos.

### 14.4 Cor e background

Esta é a parte mais saudável do tema. `Colors.kt` tem o conjunto M3 completo em claro e escuro, e **não há um único `Color(0x...)` hardcoded dentro de `features/`** — tudo passa por `MaterialTheme.colorScheme`. Não mexer no que está funcionando.

O que revisar:

- `backgroundLight` e `surfaceLight` são o mesmo `0xFFFFFCF4`. Isso apaga a distinção entre fundo e superfície, e é o motivo mais provável de cards e seções "sumirem" no claro. Decidir se é intencional (visual flat) ou se surface deve destacar;
- checar o mesmo par no escuro;
- padronizar como as seções se separam: por cor de superfície, por divider, ou por espaço. Hoje provavelmente convivem os três;
- `ui/core/shape/RoundedPentagonShape.kt` e os gráficos de `PartidoCharts.kt` são onde a cor mais foge do sistema — conferir se as cores de série dos gráficos saem do `colorScheme` ou de uma lista própria.

### 14.5 Tipografia

`Typography.kt` tem 144 linhas e `MagnaTheme.kt:96` aplica `magnaTypography()` no `MaterialTheme`. A parte de base está feita.

O que revisar é o uso: quais papéis (`headlineSmall`, `titleMedium`, `bodyLarge`…) cada tela usa para a mesma coisa. Título de tela, título de seção, nome de deputado, rótulo de metadado e valor monetário devem ter **um** papel cada, e o mesmo em todas as telas. Fazer a lista antes de editar, porque é aqui que a inconsistência "feita à mão" mais aparece e menos se percebe editando uma tela por vez.

### 14.6 Cantos e shape

`MagnaTheme.kt:94` chama `MaterialTheme(...)` passando `colorScheme` e `typography` — **e não passa `shapes`**. Ou seja, todo raio de canto do app é ou o padrão do Material 3, ou um `RoundedCornerShape` escrito à mão (7 ocorrências).

Definir um `Shapes` no tema e usá-lo. Decidir os raios de card, sheet, chip, avatar e imagem, e parar de escrever raio na tela.

### 14.7 Scroll, top bars e seções

Dois achados concretos:

- **`MagnaLargeTopBar` está morto.** As únicas referências a ele são a própria declaração e os dois `@Preview` do mesmo arquivo. Todas as seis telas de detalhe usam `MagnaMediumTopBar`, e a **Home não tem top bar nenhuma**. Ou a Home ganha a large top bar (que é onde o seletor de legislatura do item 11.5 caberia bem), ou o componente sai. Manter os dois sem usar um é o pior dos três.
- **Nenhum efeito de scroll existe no app.** Zero ocorrências de `scrollBehavior` ou `nestedScroll` no `commonMain` inteiro. As top bars são estáticas. Um `TopAppBarDefaults.enterAlwaysScrollBehavior()` (ou `exitUntilCollapsed` nas telas de detalhe) é barato e é provavelmente o item desta lista com maior diferença percebida por esforço.

Padronizar também o contêiner de rolagem: a Home usa `Column` + `verticalScroll` com um `LazyColumn` embutido na busca. Vale conferir tela a tela quem é `LazyColumn` e quem é `Column` rolável, e se o critério é o tamanho da lista ou o acaso.

### 14.8 Movimento e voltar

- **Nenhuma transição de navegação foi customizada.** O `NavHost` em `App.kt` usa o padrão. Definir `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` uma vez no `NavHost`, e não por rota.
- **Predictive back (Android 13+)** não está tratado. O app tem `minSdk` recente o bastante para valer o gesto funcionando de verdade em vez de um corte seco. É item de Android puro, e pelo item 1.1 isso agora é permitido sem culpa.
- **O app tem exatamente uma animação:** o `animateFloatAsState` que gira o chevron em `ProposicaoAutores.kt:91`. Não é para encher de movimento — é para que expandir/colapsar, carregar e trocar de estado se comportem igual nas dez telas em vez de só nessa.
- **Miudeza que denuncia o resto:** o callback de voltar se chama `navigateBack` em cinco telas e `onBack` em `PartidoDetailsScreen.kt:82`. Escolher um.

### 14.9 Estados: carregando, vazio e erro

O bloco 4 unificou `Resource<T>` na camada de dados, mas a **aparência** dos três estados nunca foi unificada. Existem `LoadingComponent` e `SomethingWentWrongComponent` compartilhados — conferir se todas as telas realmente os usam, ou se algumas têm o seu próprio `CircularProgressIndicator` no meio de um `Box`.

Faltando de propósito: **não existe um componente de estado vazio.** O bloco 8 torna isso obrigatório — trocar de legislatura vai produzir listas legitimamente vazias, e o item 11.4 depende de "vazio" e "sincronizando" serem visualmente distintos.

### 14.10 Como fechar o bloco — e como não perder o sistema depois

O risco deste bloco é virar refactor infinito. Duas amarras:

1. **Escrever as decisões antes de editar.** A extração do item 14.1 vira um documento curto — `documentation/design-system.md`: escala de espaçamento final, papéis de tipografia por função, raios, regra de seção, regra de scroll, os quatro estados. Aplicar depois. Decidir durante a edição é exatamente como o app chegou no estado atual.
2. **Percorrer o inventário do item 14.2 uma tela por vez, com a lista como checklist**, e não abrir exceção "só nesta tela". Exceção é o que se está consertando.

#### O que sobra depois — a parte que importa para a 1.2

Um documento sozinho não sobrevive a três features novas. O que sobrevive é código que torna o caminho certo mais fácil que o errado:

- **Tokens que cobrem tudo.** Se `Dimensions` não tem o valor que a tela precisa, a pessoa escreve `.dp` cru — e foi assim que apareceram os 58 de hoje. A escala final tem que ser suficiente, e `Shapes` tem que existir no tema (item 14.6). Token que falta é token que será contornado.
- **Componentes de tela, não só de widget.** Hoje `ui/component/` tem `LoadingComponent` e `SomethingWentWrongComponent`. Falta o andaime: um `MagnaScreen` (Scaffold + top bar + padding + scroll behavior padrão) e um `MagnaSection` (título + espaçamento + regra de separação). Com eles, uma feature nova começa com a identidade pronta em vez de recomeçar a decisão. Sem eles, cada tela nova é uma tela feita à mão de novo.
- **Um componente por estado.** Carregando, vazio, offline e erro, os quatro compartilhados e usados por todo mundo. O bloco 8 já exige o de vazio e o de offline; deixá-los genéricos desde o começo é o que evita a quinta variante caseira (o item 4.2 já contou quatro variantes do mesmo padrão na camada de dados — o mesmo não pode se repetir na UI).
- **Previews como vitrine.** Os `@Preview` já existem espalhados. Concentrar um arquivo de preview por componente do sistema dá um catálogo consultável sem precisar rodar o app — é o mais barato que existe nessa direção, e é o que faz alguém reusar em vez de reinventar.

O teste de que o bloco funcionou não é o app estar bonito. É: **a próxima feature deve ser montável sem escrever um `.dp`, sem escolher um papel tipográfico e sem desenhar um estado de erro.** Se ainda precisar, o sistema não ficou pronto — ficou documentado.

Ao terminar, rodar as telas em claro e escuro e em uma tela pequena. Como este é o último bloco antes da publicação, é também quando as capturas da Play Store devem ser tiradas — não antes.

---

## 15. Bloco 12 — publicação da 1.1

Este é o último bloco por construção: ele descreve o app como ele ficou, então qualquer item acima que ainda esteja em aberto invalida o que for preenchido aqui. Fazer só quando o código parar de mudar e a 1.1 estiver pronta para subir.

### 15.1 [BLOQUEANTE] Política de privacidade

`grep -ri "privac|lgpd"` no repositório inteiro: nenhuma ocorrência. A 1.0 quase certamente foi publicada declarando que não coleta dado nenhum, o que era verdade — o Firebase estava no Gradle mas nenhuma linha de código o usava.

Isso mudou. A partir da 1.1 o app coleta dados, e o Google Play **exige uma URL de política de privacidade** para qualquer app que colete dados. Sem ela a submissão é rejeitada, e essa é a falha mais provável de travar a release em cima da hora.

O que a política precisa cobrir, dado o que o app realmente faz:

- quais dados são coletados (interações no app, dados de diagnóstico e identificador de instalação);
- para que servem (entender uso e corrigir falhas — nada de publicidade);
- quem processa (Google, via Firebase Analytics e Crashlytics);
- que não há login, que nenhum dado pessoal é enviado, e que termos de busca não saem do aparelho;
- contato para pedidos de exclusão, exigência prática da LGPD.

O texto pode ficar no próprio repositório, publicado por GitHub Pages, e a URL entra na ficha da Play Store. É o caminho mais barato e mantém a política versionada junto do código que ela descreve.

### 15.2 [BLOQUEANTE] Formulário Data Safety

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

### 15.3 Antes de subir

- **`whatsnew`** (item 7.4): `distribution/whatsnew/whatsnew-pt-BR` ainda anuncia "Histórico de votações de cada deputado, com filtros por tipo de voto", removido em `6b21146`. Reescrever para a 1.1.
- **Versão**: `androidApp/build.gradle.kts` está em `versionCode = 3`, `versionName = "1.0.1"`. Subir os dois.
- ~~**`dataExtractionRules`**~~ **corrigido no bloco 6.**
- **Validar os eventos em aparelho real** antes de confiar no relatório: `adb shell setprop debug.firebase.analytics.app com.tick.magna` e acompanhar o DebugView. Eventos custom levam até 24h para aparecer nos relatórios normais, então o DebugView é o único jeito de saber na hora se a instrumentação está certa.
- **Conferir que o release não loga**: com `AppBuildConfig`, um build de release não deve imprimir requisição do Ktor nem log do Koin. Vale um `adb logcat` rápido no APK assinado.

---

## 16. Achados da verificação em aparelho

Primeira compilação depois dos blocos 0 a 6. Os seis blocos passaram inteiros: **0 erros de Kotlin** no alvo JVM e no `:androidApp`, **73 testes, 0 falhas**. O único erro de compilação do lote foi um `AUTORES_INITIAL_COUNT` declarado duas vezes, deixado pela extração de arquivos do bloco 6 e corrigido no bloco 1.

O que apareceu de verdade foi o que só um aparelho mostra.

### 16.1 O gateway da Câmara recusa `Accept-Charset` — CORRIGIDO

Todas as requisições do app voltavam **403** com uma página HTML dizendo que o sistema de segurança bloqueou a operação. Não era query malformada: o endpoint sem nenhum parâmetro (`/referencias/proposicoes/siglaTipo`) também caiu.

O gatilho é o header `Accept-Charset`, **em qualquer valor**:

| Requisição | Resposta |
| --- | --- |
| sem o header | 200 |
| `Accept-Charset: UTF-8` | 403 |
| `Accept-Charset: utf-8` | 403 |
| `Accept-Charset: *` | 403 |
| `Accept-Charset: ISO-8859-1` | 403 |
| header vazio | 403 |

O Ktor instala `HttpPlainText` por padrão e ele carimba esse header em toda requisição. Ou seja, o app estava **completamente quebrado em produção**, independente do refactor.

A remoção precisa acontecer no **send pipeline**. O `HttpPlainText` adiciona o header na fase `Render` do request pipeline, então tirar pelo `defaultRequest` não tem efeito nenhum: aquilo roda antes e o plugin recoloca depois.

Vale registrar o que isso diz sobre o bloco 1: o `HttpResponseValidator` mandou `api_error(endpoint, 403)` para o Firebase em todas as chamadas. Se a instrumentação existisse antes, o bloqueio teria aparecido no painel em vez de num logcat.

### 16.2 Cancelamento tratado como falha — CORRIGIDO

`CancellationException: Flow was aborted, no more elements needed` aparecia como **erro** no log. O `Resource.kt` (bloco 4) relança cancelamento corretamente, mas nove `catch` genéricos fora dele não: os quatro `sync*` que devolvem `Boolean`, o `channelFlow` de `getDeputados`, o fetch por membro de `getPartidoMembros` e o `try` externo do `SyncUserInformationUseCase`.

Duas consequências, e as duas corrompem justamente o que os blocos 1 e 4 construíram:

- **Crashlytics mentiria.** `CrashlyticsAntilog` manda `Napier.e` para `recordException()`. Cada cold start gravaria um não-fatal falso.
- **O analytics mentiria.** Sair da Home no meio do sync cancela as quatro `async` e reportaria `sync_step_failed` nos quatro mais `sync_finished(success=false)` — o painel diria que a API da Câmara vive caindo quando o usuário apenas saiu da tela.

Verificado no aparelho: abrir e sair em um segundo não produz mais nenhum passo reportado como falha.

### 16.3 O build não roda no Windows — CORRIGIDO

`generateCommonMainMagnaDatabaseInterface` falhava antes de chegar no Kotlin. A mensagem do SQLDelight (`Failed to compile 1.sqm:482: DeputadoExpense`) era embrulho; embaixo estava:

```
java.nio.file.AccessDeniedException: C:\WINDOWS\sqlite-3.49.1.0-...-sqlitejdbc.dll.lck
Caused by: java.lang.UnsatisfiedLinkError: 'void org.sqlite.core.NativeDB._open_utf8(byte[], int)'
```

O SQLDelight abre os `.db` com sqlite-jdbc para validar as migrações, e faz isso num **worker process** que o Gradle lança com ambiente raspado — sem `TMP`, `TEMP` nem `USERPROFILE`. O JVM cai no fallback `C:\WINDOWS` e a biblioteca nativa não pode ser extraída ali. Valia desde `eb2e2fd`, quando o `1.sqm` entrou.

**O que a investigação acrescentou ao diagnóstico original:**

- o daemon do Gradle tem `java.io.tmpdir` correto, sondado por init script. Quem está errado é só o worker;
- `org.gradle.jvmargs` com `-Dorg.sqlite.tmpdir` **não chega no worker** — testado, falha idêntica. Junta-se ao `JAVA_TOOL_OPTIONS` que já havia sido descartado;
- o `SqlDelightWorkerTask` usa `processIsolation` e monta as `forkOptions` por conta própria, passando só o classpath. **Não há knob externo** para injetar argumento de JVM no worker;
- **a conexão sqlite é aberta só pela verificação, não pela geração.** Com `verifyMigrations = false` o `generateCommonMainMagnaDatabaseInterface` passa com os `.db` e o `1.sqm` intactos.

**A correção aplicada** é esse último ponto: `verifyMigrations` passa a ser `!isWindows` em `composeApp/build.gradle.kts`. A CI roda em Linux, então a verificação continua acontecendo antes de qualquer coisa ser publicada; no Windows ela é pulada e o build anda.

Junto vai uma segunda linha: a task avulsa `verifyCommonMainMagnaDatabaseMigration` **ignora a flag** e abre a mesma conexão, então quebrava mesmo com ela desligada. Ela é desabilitada no Windows, porque nada depende dela (`check` não a inclui) e deixar uma task que quebra ao ser chamada pelo nome é armadilha.

Verificado depois da mudança, com `1.sqm` e os dois `.db` no lugar: `:composeApp:jvmTest` e `:androidApp:testDebugUnitTest` passam, **74 testes, 0 falhas**.

**O que isso custa, explicitamente:** uma migração escrita no Windows não é validada localmente — o erro aparece na CI, não na máquina de quem escreveu. É trabalho a mais no ciclo, e é muito melhor que o estado anterior, onde o projeto simplesmente não compilava. O destravamento temporário que existia antes (mutilar o `1.sqm`) **não é mais necessário e não deve ser usado** — ele produzia APK com migração errada.

**Correção estrutural, se um dia a validação local fizer falta:** `deriveSchemaFromMigrations = true` com um `0.sqm` carregando o schema original. As migrações viram a fonte da verdade e nada precisa abrir `.db`. Continua sendo mudança grande: os `CREATE TABLE` sairiam dos `.sq`, que passariam a conter apenas queries.

### 16.4 O que continua sem verificação

Os alvos iOS e o build de release com R8. Nenhum dos dois foi compilado ainda.

Pela decisão de escopo do item 1.1, os dois deixam de ter o mesmo peso:

- **Release com R8 — obrigatório antes da 1.1.** É o binário que vai para a loja, e nunca foi gerado. Um `minifyEnabled` que come uma classe de DTO ou uma regra de serialização só aparece no APK assinado, depois que a CI passou verde.
- **iOS — não-objetivo.** Continua sem compilar e assim fica. Não bloqueia nada.
- **App desktop (`:composeApp:run`) — não-objetivo.** O alvo `jvm()` continua no build porque a suíte de testes depende dele, mas o app em si não é verificado nem distribuído.
