# Magna

Magna é um app open source para acompanhar a atividade dos deputados federais brasileiros. Consulte informações sobre deputados, partidos, comissões, proposições e despesas — tudo a partir dos dados públicos da [API da Câmara dos Deputados](https://dadosabertos.camara.leg.br/).

A estrutura está pensada para **Android**, **iOS** e **Desktop (JVM)**, construído com Kotlin Multiplatform e Compose Multiplatform. Mas está somente disponível em produção em Android.

---

## Funcionalidades

- Listagem e busca de deputados federais por nome e partido
- Detalhes do deputado: dados pessoais, redes sociais e informações de contato
- Histórico de despesas por deputado
- Proposições legislativas por tipo
- Comissões e órgãos da Câmara
- Dados persistidos localmente com suporte offline

---

## Stack

| Camada | Tecnologia |
|---|---|
| UI | [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) |
| Networking | [Ktor](https://ktor.io/) |
| Banco de dados local | [SQLDelight](https://cashapp.github.io/sqldelight/) |
| Injeção de dependência | [Koin](https://insert-koin.io/) |
| Imagens | [Coil3](https://coil-kt.github.io/coil/) |
| Analytics | Firebase (Android) |

---

## Dados

Todas as informações são provenientes da [API de Dados Abertos da Câmara dos Deputados](https://dadosabertos.camara.leg.br/swagger/api.html), uma API pública e gratuita mantida pela própria Câmara. Nenhuma autenticação é necessária.

---

## Contribuindo

Contribuições são bem-vindas toda a intenção do projeto é que seja colaborativa. Para mudanças maiores, abra uma issue primeiro para discutir o que você gostaria de mudar.

1. Fork o repositório
2. Crie uma branch (`git checkout -b feature/minha-feature`)
3. Commit suas mudanças (`git commit -m 'Add minha feature'`)
4. Push para a branch (`git push origin feature/minha-feature`)
5. Abra um Pull Request

---

## Licença

[MIT](./LICENSE)
