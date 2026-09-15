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

Ação obrigatória **antes de tocar em qualquer `.sq`**:

```bash
./gradlew :composeApp:generateCommonMainMagnaDatabaseSchema
```

Isso gera `migrations/1.db` (o baseline). Commitar. A partir daí, cada mudança de schema vem com um `migrations/1.sqm` (`ALTER TABLE ...`) e o `verifyMigrations` passa a proteger de verdade.

### 2.2 [ALTO] Despesas duplicam a cada abertura do deputado

- `DeputadoExpense.sq:19` — `insertExpense` é um `INSERT` simples numa tabela com `expenseId AUTOINCREMENT` e sem `UNIQUE`.
- `DeputadosRepository.getDeputadoExpenses` (linhas 111-139) chama a API e insere toda vez que a tela abre.
- Resultado: 15 linhas novas por visita, e a lista cresce indefinidamente. Quem abriu o mesmo deputado cinco vezes vê 75 cards.

Correção: gravar `codDocumento` (o `DespesaDto` já traz) e criar chave natural `UNIQUE(deputadoId, codDocumento, ano, mes)` com `INSERT ... ON CONFLICT DO UPDATE`, ou apagar as despesas do deputado dentro da mesma transação antes de reinserir. Exige migração (ver 2.1).

### 2.3 [MÉDIO] Ordenação de despesas por mês está errada

- `DeputadoExpense.sq:44` e `:50` — `ORDER BY year DESC, month DESC` sobre colunas `TEXT`. `"9" > "10"` lexicograficamente, então outubro/novembro/dezembro aparecem antes de fevereiro a setembro.
- Causa: `DeputadoExpenseMapper.kt:42` grava `ano.toString()` e `mes.toString()`.

Correção: colunas `INTEGER`. Exige migração.

### 2.4 [MÉDIO] Valor monetário gravado como string formatada

- `DeputadoExpenseMapper.kt:47` grava `"R$ $valorDocumento"` (vira `R$ 8000.0`, sem formatação pt-BR).
- Impede somar, ordenar por valor ou trocar a formatação sem migração.

Correção: coluna `REAL`, formatação na UI com separador de milhar e vírgula decimal. Exige migração.

### 2.5 [BAIXO] Chaves estrangeiras são decorativas

- Todas as tabelas declaram `FOREIGN KEY`, mas nenhum driver liga `PRAGMA foreign_keys = ON`. SQLite ignora FKs por padrão.
- Hoje isso até ajuda: `DeputadoDetails.legislaturaId` referencia `Legislatura`, que nunca é populada (a `LegislaturaRepository` não é usada em lugar nenhum). Ligar o pragma agora quebraria inserts.

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

Correção: tornar nulável com default tudo que o app não precisa obrigatoriamente, e configurar `explicitNulls = false` no `Json`. Vale um teste unitário por DTO com JSON real da API contendo nulls.

### 3.2 [ALTO] Sem timeout nem retry

- `HttpClientFactory.kt` não instala `HttpTimeout`. A API da Câmara é lenta e cai com regularidade; sem timeout a request pendura e o usuário fica olhando um spinner eterno (o diálogo de sync inicial, por exemplo, não tem saída).
- Sugestão: `HttpTimeout` com `requestTimeoutMillis = 30_000`, `connectTimeoutMillis = 10_000`; `HttpRequestRetry` com `retryOnServerErrors(maxRetries = 2)` e backoff exponencial.

### 3.3 [MÉDIO] `getPartidos` ignora a legislatura

- `PartidosApi.kt:15` — recebe `idLegislatura` e não usa; manda `dataInicio=2025-01-01` fixo. O parâmetro certo é `idLegislatura`.

### 3.4 [MÉDIO] `getDeputadoExpenses` ignora o ano e só pega a primeira página

- `DeputadosApi.kt:25-28` — o parâmetro `year` não é enviado. Sem `itens`, a API devolve 15 registros. O `LinkDto` com `rel=next` é desserializado em todas as responses e nunca lido.
- Decisão de produto: ou mostra "últimas 15 despesas" explicitamente na UI, ou implementa paginação (`itens=100` + seguir `next`) por ano.

### 3.5 [MÉDIO] Chamadas sequenciais onde deveriam ser paralelas

- `OrgaosRepository.kt:58` — dentro de um `.map`, para cada uma das 20 votações faz `getVotacaoDetail` em série. São 21 requests em fila. Trocar por `async`/`awaitAll` com `Semaphore` como já é feito em `PartidosRepository`.

### 3.6 [MÉDIO] Explosão de requests nas proposições

- `ProposicoesRepository.observeRecentProposicoes` — para cada troca de filtro: 1 request de lista + 2 por proposição (detalhe + autores) = 31 requests. Roda toda vez que a Home abre e cada vez que o chip muda, sem TTL. Ver 4.1 sobre cancelamento.

### 3.7 [BAIXO] Logging de rede em release

- `HttpClientFactory.kt:26` instala `Logging` em nível `INFO` incondicionalmente. Junto com `Napier.base(DebugAntilog())` em `MagnaApplication.kt:17` e Koin em `Level.DEBUG` (`:20-21`), o release faz log de tudo. Custo de CPU e ruído no Logcat de produção. Ver seção 8 para o que fazer no lugar.

---

## 4. Repositórios e concorrência

### 4.1 [ALTO] Escopos de coroutine que nunca cancelam

- `Modules.kt:125` — `factory<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }`. Cada repositório (singleton) recebe seu próprio escopo eterno e faz `coroutineScope.launch { api... }` de dentro de funções que devolvem `Flow`.
- Efeito: sair da tela não cancela nada. Abrir e fechar detalhes de partido dispara o fetch de ~70 deputados (semáforo de 10) que continua rodando. Trocar o chip de proposições três vezes rápido enfileira ~93 requests. Nenhum erro aparece, só bateria e dados.
- Padrão recomendado: repositório não lança coroutines. Expõe `Flow` construído com `flow { }` / `channelFlow { }` onde o fetch acontece dentro do próprio flow (cancelável por quem coleta), ou expõe `suspend fun refresh()` separado de `fun observe(): Flow`. O `viewModelScope` passa a ser o único dono de ciclo de vida. Remover o `factory<CoroutineScope>` inteiro.

### 4.2 [MÉDIO] Quatro implementações do mesmo padrão "loading / erro / conteúdo"

- `DeputadoDetailsResult` (sealed), `PartidoDetailsResult` (flags booleanas), `ProposicaoDetailsResult` (flags), `RecentProposicoesResult` (flags). Cada uma com um `MutableStateFlow` de sinal criado por chamada e combinado à mão.
- Isso é o principal alvo de simplificação. Um `sealed interface Resource<T> { Loading; Error(cause); Content(data, isRefreshing) }` e um helper `networkBoundResource(query = dao.observe(), fetch = api.get(), save = dao.insert())` substituem ~150 linhas e ficam testáveis.

### 4.3 [MÉDIO] `suspend fun` devolvendo `Flow`

- `DeputadosRepositoryInterface`, `PartidosRepositoryInterface` e `LegislaturaRepositoryInterface` têm métodos `suspend fun x(): Flow<T>`. O `suspend` existe só para ler `legislaturaId` antes de montar o flow. `getDeputadoExpenses` já mostra o jeito certo: `userDao.getUser().flatMapLatest { ... }`. Padronizar.

### 4.4 [MÉDIO] Sync inicial não tem TTL

- `SyncUserInformationUseCase` só sincroniza quando alguma tabela está vazia. Depois disso, `Deputado` só é atualizado quando a tela de busca abre (`getDeputados()` refaz o fetch dos 513 toda vez). `Partido` e `Orgao` nunca mais.
- `User` no domínio tem `lastSync`, mas a tabela `User.sq` não tem a coluna. Adicionar `lastSyncAt INTEGER` (mesma migração) e um TTL de, por exemplo, 24h.

### 4.5 [MÉDIO] Código morto ou meio-vivo

Depois do commit `6b21146` (remoção de Votações), sobrou:

- `features/deputados/votacoes/*` (tela, state, ViewModel, rota registrada em `App.kt`, FAB comentado em `DeputadoDetailsScreen.kt:526-538`).
- `DeputadosRepository.getDeputadoVotacoes` — 21 requests para achar o voto de um deputado nas 20 últimas votações da Casa inteira; só acha votos nominais.
- `EventosApi`, `EventosRepository`, `EventosRepositoryInterface` e seus DTOs — registrados no Koin, nunca injetados em ninguém.
- `LegislaturaApi`, `LegislaturaRepository`, `LegislaturaDao` — nunca chamados. As strings `welcome_*` em `strings.xml` sugerem uma tela de escolha de legislatura que não existe.
- `DespesaDto.toDomain()` em `DeputadoExpenseMapper.kt` — sem uso.
- `ExpenseRow` em `DeputadoDetailsScreen.kt:969` — sem uso.
- `User.kt` (domínio) — sem uso.

Decidir por item: apagar ou terminar. Manter meio-vivo custa em cada refactor.

### 4.6 [BAIXO] Miudezas

- `PartidosRepository.kt:129` — `?: "57"` hardcoded como fallback de legislatura.
- `PartidoDetailsViewModel.kt:27` — `CURRENT_YEAR = 2026` fixo; `currentYear()` já existe em `util/DateUtils.kt`.
- `ProposicoesRepository.kt:25,29` — `siglaTipoDao` e `siglatipoDao` são a mesma dependência injetada duas vezes.
- `UserRepository.userDao` é `val` público.
- `EventosApi` recebe `AppLoggerInterface`; nenhuma outra API recebe.
- `DeputadoDetailsMapper` classifica redes por `contains("twitter")`; links `x.com` são descartados.
- `MagnaComissaoPermanente` (enum de 6 comissões) mora em `data/repository/orgaos/params` — é configuração de produto, não de dados.
- `Legislatura` no domínio é `class`, não `data class`.
- Os `*Mock` em `data/domain/*.kt` (~800 linhas) vão para o APK porque os `@Preview` que os usam estão em `commonMain`. Aceitável, mas vale saber.

---

## 5. ViewModels e estado

### 5.1 [MÉDIO] Deputado sem despesas fica em "carregando" para sempre

- `DeputadoDetailsViewModel.kt:49` — `expensesResult.isEmpty() -> ExpensesState.Loading`. Lista vazia e erro de API produzem o mesmo estado; `ExpensesState.Error` nunca é emitido. Resolvido de graça ao adotar o `Resource<T>` de 4.2.

### 5.2 [BAIXO] `SharingStarted.Lazily`

- `ComissoesPermanentesViewModel` e `RecentProposicoesViewModel` usam `Lazily`; o flow do banco continua coletado depois que a UI some. `WhileSubscribed(5_000)` é o padrão para Compose.

---

## 6. UI (Compose)

### 6.1 [MÉDIO] Acessibilidade: ícones clicáveis sem descrição

- 20 ocorrências de `contentDescription = null`, incluindo voltar, fechar e buscar (`MagnaHomeScreen.kt`, `MagnaMediumTopBar.kt`, `MagnaLargeTopBar.kt`, `DeputadoDetailsScreen.kt`). Com TalkBack, o usuário não consegue voltar. Imagens decorativas podem ficar `null`; controles não.

### 6.2 [MÉDIO] Arquivos de tela grandes demais

- `PartidoDetailsScreen.kt` 793 linhas (inclui `GenderChart`, `HorizontalBarChart`, `MemberRow`), `DeputadoDetailsScreen.kt` 670, `ProposicaoDetailsScreen.kt` 615, `DeputadoVotacoesScreen.kt` 491. O próprio `CLAUDE.md` pede split. Gráficos vão para `ui/component/chart/`, sheet de despesa para arquivo próprio.

### 6.3 [BAIXO] Home

- `MagnaHomeScreen.kt:182` — `remember { mutableStateOf(TextFieldState()) }` em vez de `rememberTextFieldState()`; texto some em rotação.
- `:213` — clique no ícone de busca faz `append("")`, um no-op.
- `:262` — `BottomSheetScaffold` com `sheetContent = {}` e `sheetPeekHeight = 0.dp`; é um `Scaffold` comum disfarçado.
- Enquanto `syncState` é `Initial`, a tela é branca (nada renderiza fora de `Done`).

### 6.4 [BAIXO] Strings

- `loading`, `cancel`, `retry` em inglês dentro do `strings.xml` pt-BR.
- `VotoFilter` (`DeputadoVotacoesState.kt`) tem labels em português hardcoded no enum.

### 6.5 [BAIXO] Tema e janela

- `AndroidManifest.xml:14` — `Theme.Material.Light.NoActionBar`. Sem `windowBackground` escuro, o cold start em dark mode dá um flash branco antes do Compose. Migrar para `Theme.SplashScreen` (core-splashscreen) resolve os dois.
- `MagnaTheme.kt` — `CompositionLocalProvider(LocalDimensions provides LocalDimensions.current)` é um no-op.

---

## 7. Build, CI e release

### 7.1 [ALTO] O job de testes do CI não roda teste nenhum

- `.github/workflows/android-release.yml:24` — `./gradlew testDebugUnitTest`. Verificado com `gradlew tasks --all`: essa task existe só em `:androidApp` (que não tem testes). Os testes de `commonTest` rodam via `:composeApp:jvmTest` (ou `:composeApp:allTests`). O job fica verde sem executar nada.
- Correção: `./gradlew :composeApp:jvmTest :androidApp:testDebugUnitTest`.

### 7.2 [ALTO] Um teste, e é `assertEquals(3, 1 + 2)`

- `commonTest/.../ComposeAppCommonTest.kt` é o único teste. A arquitetura é toda testável (interfaces + `DispatcherInterface`), então o custo de começar é baixo.
- Setup sugerido (só `commonTest` + `jvmTest`, sem Android): `kotlin-test`, `kotlinx-coroutines-test`, `turbine`, e `sqlite-driver` (`JdbcSqliteDriver.IN_MEMORY`) para testar DAOs e queries de verdade. Fakes à mão para APIs (sem MockK — não roda em commonTest).
- Ordem de valor: mappers de data e valor → DTOs com JSON real contendo nulls → `SyncUserInformationUseCase` → filtros de `DeputadosSearchViewModel` → DAOs com migração (o próprio SQLDelight testa migração com o `.db` baseline).

### 7.3 [MÉDIO] `-Xskip-prerelease-check`

- `androidApp/build.gradle.kts:57`. Isso silencia um erro real: alguma dependência foi compilada com Kotlin mais novo que o 2.2.20 do projeto (suspeita: `androidx.compose.bom 2025.12.00`). Descobrir qual e alinhar a versão; a flag é uma bomba-relógio.

### 7.4 [MÉDIO] `whatsnew` da Play Store anuncia feature removida

- `distribution/whatsnew/whatsnew-pt-BR:6` — "Histórico de votações de cada deputado, com filtros por tipo de voto". Removido em `6b21146`. Corrigir na 1.1.

### 7.5 [BAIXO] `dataExtractionRules` com schema errado

- `AndroidManifest.xml:9` aponta `dataExtractionRules` para `backup_rules.xml`, cujo root é `<full-backup-content>`. Para API 31+ o formato é `<data-extraction-rules><cloud-backup><exclude .../></cloud-backup></data-extraction-rules>`. Hoje a exclusão do banco provavelmente não vale no Android 12+.

### 7.6 [BAIXO] ProGuard

- `-keep class app.cash.sqldelight.** { *; }` e `-keep @kotlinx.serialization.Serializable class com.tick.magna.** { *; }` são mais largos que o necessário. Funciona; só infla o APK.

---

## 8. Instrumentação (analytics + crash)

### 8.1 Situação atual

- `androidApp/build.gradle.kts` declara `firebase-analytics` e `firebase-crashlytics` e aplica os plugins. `grep -ri firebase` no código Kotlin: zero resultados.
- Crashlytics funciona sem código (captura crash, sobe mapping via plugin). Mas nenhum log do Napier chega lá, então um crash vem sem breadcrumbs.
- Analytics coleta `screen_view` automático por Activity. O app tem uma Activity. Ou seja: hoje se sabe que o app abriu e quanto tempo ficou aberto. Nada mais.

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

**Play Console:** ao publicar a 1.1, atualizar o formulário Data Safety para declarar "App interactions" e "Crash logs" como coletados. Sem PII, sem compartilhamento com terceiros além do Firebase.

### 8.3 Perguntas que essa instrumentação responde

- Qual seção da Home recebe cliques (recentes, proposições, comissões, partidos).
- Alguém abre despesas e o documento PDF.
- Busca da Home vs. tela de busca com filtros.
- Taxa de falha do sync inicial e tempo médio (mede a API da Câmara, não o app).
- Sessões por semana dos usuários constantes, e em qual tela eles passam mais tempo.

---

## 9. Sequência sugerida para a 1.1

Cada bloco cabe numa sessão isolada e foi pensado para não conflitar com o outro em arquivos.

| Bloco | Escopo | Arquivos principais | Pré-requisito |
|---|---|---|---|
| 0 | Baseline de migração (`1.db`), CI rodando `:composeApp:jvmTest`, primeiro teste real (mapper de data) | `build.gradle.kts`, `.github/workflows/*.yml`, `commonTest` | nenhum |
| 1 | Analytics + `CrashlyticsAntilog` + gate de logs em release | `AnalyticsInterface`, `platformModule`, `App.kt`, `MagnaApplication.kt`, ViewModels (`processAction`) | 0 |
| 2 | Despesas: schema (`INTEGER`/`REAL`/`codDocumento UNIQUE`), `1.sqm`, upsert, `Error` state, formatação pt-BR na UI, parâmetro `ano` | `DeputadoExpense.sq`, `DeputadoExpenseMapper.kt`, `DeputadoExpenseDao.kt`, `DeputadosApi.kt`, `DeputadoDetailsViewModel.kt` | 0 |
| 3 | `HttpTimeout` + `HttpRequestRetry`; DTOs nuláveis com testes de JSON real; `getPartidos` com `idLegislatura` | `HttpClientFactory.kt`, `dto/*.kt`, `PartidosApi.kt` | 0 |
| 4 | Remover `factory<CoroutineScope>`; repositórios sem `launch`; `Resource<T>` unificado. Fazer um repositório por PR, começando por `Deputados` | `Modules.kt`, `repository/**` | 2, 3 |
| 5 | Decisão e remoção de código morto (Votações do deputado, Eventos, Legislatura) | ver 4.5 | nenhum |
| 6 | Split de telas, `contentDescription`, strings, splash/dark window, `whatsnew`, `dataExtractionRules` | `features/**/*Screen.kt`, `strings.xml`, manifest | nenhum |
| 7 | Feature nova da 1.1 | — | 0, 1, 2 |

Blocos 0, 1 e 2 são os únicos que eu não deixaria para depois da feature: 0 porque sem ele a 1.1 crasha na atualização, 1 porque sem ele a 1.1 sai sem dado nenhum, e 2 porque é o bug visível para quem usa hoje.
