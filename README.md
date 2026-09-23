# Magna

Magna é um app open source para acompanhar a Câmara dos Deputados: quem são os deputados, como
votaram, quanto gastaram, o que propuseram, em que partido e em que comissão estão. Tudo sai dos
[dados abertos da Câmara](https://dadosabertos.camara.leg.br/), sem login e sem servidor próprio.

**[Disponível no Google Play](https://play.google.com/store/apps/details?id=com.tick.magna)**

Feito com Kotlin Multiplatform e Compose Multiplatform. O código compila para Android, iOS e
desktop (JVM), mas só a versão Android está publicada.

---

## O que o app faz

- **Deputados**: busca por nome com filtros por UF, região, partido, comissão e em exercício.
  O perfil mostra gabinete (dá para ligar, mandar e-mail ou abrir no mapa), redes sociais,
  despesas do ano e **como a pessoa votou**, no plenário e nas comissões.
- **Votações**: o resultado de cada votação nominal e o voto de cada deputado. Funciona offline
  depois da primeira carga.
- **Partidos**: a lista na ordem que você escolher, o hemiciclo com as bancadas, a cor de cada
  partido e a composição (gênero, idade, estado de origem).
- **Comissões permanentes**: as 30, ordenadas por quanto votam. Cada uma mostra as votações com o
  parecer do relator, quem compõe a comissão hoje e quem já a presidiu na legislatura.
- **Proposições**: as mais recentes, filtradas por tipo (Constituição, leis, atos legislativos,
  tramitação), com autores, situação, relator, votações e a tramitação dia a dia.
- **Qualquer legislatura**: dá para trocar de mandato e navegar pelas legislaturas anteriores.
  Uma legislatura encerrada é baixada uma única vez e depois abre sem internet.
- **Santinho**: anote os números dos seus candidatos para levar na hora de votar. Os números
  ficam criptografados só no aparelho, não são enviados a lugar nenhum nem cruzados com
  candidato nenhum, e ver ou salvar pede sua digital ou PIN. Por enquanto só no Android.

## Privacidade

O app não tem conta, não tem login e não tem servidor. O analytics (Firebase, só no Android)
registra quais telas foram abertas, nunca os ids de quem você consultou, e não registra nada
sobre o Santinho, nem que você abriu a tela.
[Política de privacidade](https://tiagodavila.com/magna/privacy-policy.html).

## Stack

| Camada | Tecnologia |
|---|---|
| UI | [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/), navegação type-safe, shared element transitions |
| Rede | [Ktor](https://ktor.io/) |
| Banco local | [SQLDelight](https://sqldelight.github.io/sqldelight/), com migrations verificadas |
| Injeção de dependência | [Koin](https://insert-koin.io/) |
| Imagens | [Coil 3](https://coil-kt.github.io/coil/) |
| Logs e crashes | Napier e Firebase Crashlytics (Android) |

## Rodando

```bash
# Desktop (JVM), o jeito mais rápido de ver o app
./gradlew :composeApp:run

# Testes unitários
./gradlew testDebugUnitTest

# APK de debug
./gradlew :androidApp:assembleDebug
```

O build de release precisa do keystore (`magna-keystore.jks`) e das variáveis `KEYSTORE_PASSWORD`,
`KEY_ALIAS` e `KEY_PASSWORD`. Para testar o R8 sem o keystore, existe o build type `minified`,
assinado com a chave de debug. O `google-services.json` não está no repositório: sem ele, o build
Android que depende do Firebase não compila.

## Documentação

A documentação do projeto está em [`docs/`](docs/) e foi escrita para quem vai mexer no código,
gente ou agente:

- [`docs/architecture.md`](docs/architecture.md): módulos, pipeline de dados, navegação,
  ViewModels, DI e o pipeline de release
- [`docs/domain.md`](docs/domain.md): objetos de domínio e as regras de mapping até a tela
- [`docs/features/`](docs/features/README.md): um documento por feature, com arquivos, fluxo de
  dados, regras fáceis de quebrar e testes
- [`docs/api/`](docs/api/README.md): tudo o que medimos da API da Câmara (endpoints, parâmetros,
  nullability, armadilhas de modelagem e um glossário)

Instruções para agentes de código estão em [`CLAUDE.md`](CLAUDE.md).

## Publicação

- `android-release.yml` (manual): roda os testes e manda um APK para o Firebase App Distribution.
- `playstore-upload.yml` (em qualquer tag): roda os testes e publica o AAB **em produção** na Play
  Store. **Criar uma tag é publicar para todo mundo.**

## Contribuindo

Contribuições são bem-vindas. Para mudanças maiores, abra uma issue antes para a gente conversar.

1. Faça um fork e crie uma branch
2. Leia o documento da feature em `docs/features/` antes de mexer nela
3. Rode `./gradlew testDebugUnitTest`
4. Abra um Pull Request

## Licença

[MIT](./LICENSE)
