<p align="center">
  <img src="fastlane/metadata/android/pt-BR/images/featureGraphic.png" alt="Onio" width="640">
</p>

# Onio

> **Aqui você NÃO compra.**

Onio é um app Android que parece uma loja online completa, mas **nunca cobra nada**. Você navega, coloca
produtos no carrinho, preenche um cartão e "paga". Pouco tempo depois o pedido é cancelado automaticamente,
e a aba **Economia** mostra quanto dinheiro ficou no seu bolso.

Ele foi pensado para quem tem **oniomania** (compra compulsiva): dá para sentir a emoção de comprar, sem o
arrependimento na fatura.

> **Transparência:** o Onio é uma simulação. Nenhuma compra real é feita, nenhum pagamento é processado e os
> dados do cartão nunca são salvos nem enviados para lugar nenhum (só a bandeira e os 4 últimos dígitos ficam
> no pedido, no próprio aparelho).

## Como funciona

1. **Onboarding** (só na primeira abertura): três slides, a pergunta *"Em quanto tempo você costuma se
   arrepender das suas compras?"* (presets ou um valor livre de 1 minuto a 7 dias; padrão: 1 hora) e
   *"Como gostaria de ser chamado?"*.
2. **Loja:** home com banners, categorias e ofertas do dia, busca, detalhe do produto, carrinho e checkout com
   validação de cartão (Luhn, validade, CVV) e prévia do cartão ao vivo.
3. **Pedido:** fica "em processamento" com contagem regressiva. Ao vencer o prazo (um `WorkManager` garante isso
   mesmo com o app fechado, e o app confere de novo ao abrir), ele vira **cancelado**.
4. **Economia:** soma tudo o que você não gastou, quantos pedidos foram cancelados e quantos itens não foram
   comprados.

## Recursos

- **10 idiomas:** inglês, chinês, hindi, espanhol, francês, árabe, bengali, português (BR), russo e urdu, com
  RTL, plurais corretos e moeda/formatação por idioma (USD, CNY, INR, EUR, SAR, BDT, BRL, RUB, PKR).
  O idioma e a moeda podem ser trocados em *Configurações*.
- Layout adaptativo (celular, tablet, dobrável) com `NavigationSuiteScaffold` e lista/detalhe em Pedidos.
- Tema claro/escuro, esqueletos de carregamento, selo de desconto, "adicionar rápido" nos cards e contador no
  carrinho.
- O nome informado no onboarding personaliza a Home, o checkout, a Economia e o pedido cancelado.

## Arquitetura

Modularização por features no Gradle, com submódulos `bridge`/`impl`, **MVI** e Compose.

```
:app                        Composition Root: Hilt, navegação (Navigation 3, uma pilha por aba)
:core:model                 Domínio em Kotlin puro (sem Android/Compose)
:core:ui                    Design System, componentes compartilhados, formatação de moeda
:core:network               DummyJSON via Retrofit; DTOs ficam internos, só o domínio sai
:core:data                  Room, DataStore, WorkManager e implementações dos repositórios
:feature:<nome>:bridge      Só contratos públicos: chaves de navegação (NavKey)
:feature:<nome>:impl        UI (Compose), ViewModel e UiModels da feature
```

Features: `onboarding`, `home`, `search`, `product`, `cart`, `checkout`, `orders`, `savings`, `settings`.

Regras que o projeto segue:

- Nenhuma feature depende do `:impl` de outra; só o `:app` enxerga todos os `:impl`.
- Fluxo de dados `DTO/Entity → Domain → UiModel` somente por funções de extensão de mapeamento explícitas.
- Cada tela tem `UiState` imutável (com `effect`), `UiIntent` (incluindo `ConsumeEffect`) e `UiEffect`;
  todo `viewModelScope.launch` trata erro e o reflete no estado.
- UI coleta estado com `collectAsStateWithLifecycle()`, usa `ImmutableList` e referências de método
  (`onIntent = viewModel::onIntent`).
- Plugins de convenção em `build-logic/` e versões centralizadas em `gradle/libs.versions.toml`.

## Stack

Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Material 3 + Adaptive · Navigation 3 · Hilt · Room · DataStore ·
WorkManager · Retrofit + kotlinx.serialization · Coil 3 · AGP 9.4 · Gradle 9.6 · `minSdk 24`, `targetSdk 37`.

## Como rodar

Requisitos: **JDK 17+** e Android SDK com a plataforma 37 (o `local.properties` é gerado pelo Android Studio).

```sh
./gradlew assembleDebug          # gera o APK de debug
./gradlew installDebug           # instala no dispositivo/emulador conectado
./gradlew testDebugUnitTest      # testes unitários
./gradlew :app:lintDebug         # lint (traduções faltando são erro de build)
```

O catálogo vem da [DummyJSON](https://dummyjson.com) (sem chave) e precisa de internet. Dica de teste: nas
*Configurações* (ou no onboarding) escolha **1 min** para ver o cancelamento acontecer rápido.

## Internacionalização

Nenhum texto fica fixo no código: cada módulo tem `res/values/strings.xml` (inglês) e uma pasta por idioma
(`values-zh`, `values-hi`, `values-es`, `values-fr`, `values-ar`, `values-bn`, `values-pt`, `values-ru`,
`values-ur`). O lint trata `MissingTranslation` como **erro**, então um texto novo só compila se existir nos
10 idiomas. Valores de moeda usam `NumberFormat` do idioma, com taxas de câmbio fixas e embutidas
(`ExchangeRates`), já que nenhum dinheiro real circula.

## Ficha da Play Store

Os recursos da loja ficam em `fastlane/metadata/android/<locale>/` (título, descrições, ícone 512 e *feature
graphic* 1024×500) para os 10 idiomas, gerados por `tools/store-assets/generate_store_assets.py`
(precisa de `rsvg-convert`, ImageMagick e fontes Noto). Veja [`fastlane/README.md`](fastlane/README.md).
As artes não trazem preço, "grátis", "promoção" nem chamadas para ação, conforme a política da Play.

## Preços reais

A DummyJSON é só um catálogo de exemplo. Preços reais do varejo brasileiro foram avaliados via
[Data Market](https://datamarket.cnovatech.com.br/); a integração exige um backend com cache (a chave de API
não pode ir no APK) e está no backlog. A fonte de produtos é isolada em `ProductRemoteSource`
(`:core:network`), então trocar de provedor não afeta o resto do app.

## Roadmap

O planejamento fica no [quadro do projeto](https://github.com/users/tonimadev/projects/3) e nas
[issues](https://github.com/tonimadev/onio/issues): preços reais, capturas de tela da Play, revisão nativa das
traduções, notificação de cancelamento, favoritos/filtros, mais testes e build de release assinado.

## Licença

Ainda não definida.
