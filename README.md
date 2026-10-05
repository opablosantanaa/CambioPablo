# 💱 Câmbio Pablo

Aplicativo mobile desenvolvido com **Android Studio** e **Java** para conversão de moedas em real brasileiro, consulta de cotações e acompanhamento do histórico de conversões, com integração à **AwesomeAPI**.

O frontend do projeto foi desenvolvido com auxílio de inteligência artificial, utilizando técnicas de engenharia de prompt e supervisão humana para orientar, revisar e validar a implementação.

---

## 📲 Instalação

O aplicativo está disponível para celulares com **Android 6.0 ou superior**.

### Baixar o aplicativo

**[📥 Baixar cambioPablo.apk](https://github.com/opablosantanaa/CambioPablo/blob/main/apk/cambioPablo.apk)**

1. Baixe o APK pelo link acima no celular.
2. Abra o arquivo `cambioPablo.apk`.
3. Se solicitado pelo Android, autorize **Instalar apps desconhecidos** para o navegador ou gerenciador de arquivos utilizado.
4. Confirme a instalação e abra o **Câmbio Pablo**.

Não é necessário instalar Android Studio ou conectar o celular a um computador. A conexão com a internet é necessária para consultar as cotações.

---

## 🚀 Funcionalidades

- **Conversão de Moedas**: conversão de dólar americano, euro, libra esterlina, iene japonês e bitcoin para reais.
- **Consulta de Cotações**: lista de valores por unidade, com atualização manual e seleção de uma moeda para conversão.
- **Entrada de Valores**: máscara monetária com duas casas decimais, deslocando os dígitos conforme a digitação: `0,01 → 0,10 → 1,05`.
- **Histórico Local**: armazenamento das últimas 20 conversões no aparelho, com moeda de origem, valor convertido, resultado e horário da consulta.
- **Exclusão do Histórico**: remoção dos registros mediante confirmação.
- **Interface**: tema escuro com paleta verde e dourada, cartões sólidos e fundo animado DarkVeil.
- **Navegação entre Abas**: seletor em formato de cápsula, com ícones de fácil visualização e nome exibido somente na aba ativa. A seleção usa expansão e retorno elástico inspirados em JellyRadio.
- **Seleção de Moeda**: menu junto ao seletor, com destaque deslizante inspirado em GlideSelect. Permite escolher por toque, arraste ou teclado.
- **Tratamento de Erros**: validação de valores e mensagens para falhas de conexão ou cotações indisponíveis.

---

## 🎨 Interface e Animações

A interface foi implementada com **Java, layouts XML e componentes nativos do Android**. Os efeitos JellyRadio, GlideSelect e DarkVeil foram adaptados para essa estrutura nativa.

### Paleta de cores

| Cor | Valor | Aplicação |
|---|---|---|
| Fundo | `#0B1416` | Base escura da interface |
| Principal | `#00665E` | Botões de ação e aba selecionada |
| Destaque | `#D6B840` | Indicadores de foco e identificação da moeda de destino |

As cores estão centralizadas em `app/src/main/res/values/colors.xml`. Os cartões utilizam superfícies sólidas em tons derivados do verde, preservando a legibilidade sobre o fundo animado.

### Navegação e escolha de moeda

- **Abas**: Converter, Cotações e Histórico permanecem no rodapé. Cada aba tem um ícone vetorial de 28 dp; ao selecioná-la, o espaço se expande e o nome aparece ao lado do ícone. O nome também está disponível para leitores de tela e no toque prolongado.
- **Moedas**: o menu apresenta o código e o nome de cada moeda. O destaque acompanha o toque, o arraste e a navegação por teclado; a escolha é confirmada ao clicar ou soltar sobre uma opção. Tocar fora fecha o menu.

### Fundo DarkVeil

O fundo usa **OpenGL ES 2.0 e shaders GLSL** para criar formas suaves em movimento, remapeadas para a paleta do aplicativo. A renderização ocorre em uma thread separada da interface, com cadência desejada de aproximadamente 30 quadros por segundo e resolução reduzida para limitar o custo gráfico.

O efeito é pausado quando o aplicativo deixa de estar ativo ou sua janela não está visível. Com as animações do Android desativadas, ou em economia de bateria, o fundo fica estático. Os efeitos de seleção também respeitam a configuração de animações do sistema.

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Versão | Descrição |
|---|---|---|
| Java | 11 | Compatibilidade do código-fonte configurada no módulo |
| JDK | 25 | Versão configurada para o daemon do Gradle |
| Android SDK | 37 | SDK de compilação e versão de destino |
| Android Gradle Plugin | 9.4.1 | Compilação do aplicativo Android |
| Gradle Wrapper | 9.6.0 | Execução das tarefas de build |
| AppCompat | 1.6.1 | Componentes e compatibilidade da interface |
| Material Components | 1.10.0 | Componentes, diálogos e tema |
| Retrofit | 2.9.0 | Cliente HTTP para consulta de cotações |
| Gson Converter | 2.9.0 | Conversão das respostas JSON |
| OpenGL ES | 2.0 | Fundo procedural animado com shader GLSL |
| SharedPreferences + JSON | — | Persistência do histórico local |
| JUnit | 4.13.2 | Testes unitários |
| Espresso | 3.7.0 | Infraestrutura de testes instrumentados |

---

## 📋 Pré-requisitos

Para executar o código-fonte e contribuir com o projeto:

- **Android Studio** compatível com o Android Gradle Plugin utilizado.
- **JDK 25**, conforme `gradle/gradle-daemon-jvm.properties`.
- **Android SDK 37**, Platform Tools e ferramentas de compilação instalados pelo SDK Manager.
- Conexão com a internet para baixar dependências e consultar cotações.
- Emulador ou aparelho com **Android 6.0 ou superior** (`minSdk = 23`).

Para utilizar o aplicativo, basta um aparelho Android compatível e o APK instalado. Não é necessário ter Android Studio, Java ou um computador para realizar a instalação pelo próprio celular.

---

## ⚙️ Configuração

### 1. Abrir no Android Studio

1. Baixe o projeto e abra sua pasta raiz, que contém `settings.gradle.kts`.
2. Aguarde a sincronização do Gradle.
3. Instale os componentes solicitados pelo SDK Manager, se necessário.
4. Confira a configuração do JDK usado pelo Gradle e a disponibilidade do JDK 25.

O arquivo `local.properties` contém o caminho do Android SDK da máquina. Ele é local e não deve ser compartilhado como configuração universal do projeto.

### 2. Integração com a API

O endereço base está definido em `RetrofitClient.java`:

```text
https://economia.awesomeapi.com.br/
```

O código atual não utiliza token, arquivo `.env` ou servidor próprio. A permissão de acesso à internet está declarada no `AndroidManifest.xml`.

---

## ▶️ Executar o Código-fonte

1. Abra e sincronize o projeto.
2. Selecione o módulo `app`.
3. Escolha um emulador ou um aparelho conectado com depuração USB habilitada.
4. Clique em **Run**.

A atividade inicial é `com.opablosantanaa.cambiopablo.MainActivity`.

---


## 📁 Estrutura do Projeto

```text
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── assets/
│   │   ├── darkveil.vert               # Posição dos vértices do fundo
│   │   └── darkveil.frag               # Padrão procedural e mistura de cores
│   ├── java/com/opablosantanaa/cambiopablo/
│   │   ├── MainActivity.java            # Navegação, conversão, cotações e histórico
│   │   ├── AmountInputFormatter.java    # Formatação da entrada monetária
│   │   ├── api/
│   │   │   ├── ApiService.java          # Definição das requisições
│   │   │   ├── Currency.java            # Modelo da resposta de cotação
│   │   │   └── RetrofitClient.java      # Configuração do cliente HTTP
│   │   └── ui/
│   │       ├── JellyTabsLayout.java     # Seleção e expansão elástica das abas
│   │       ├── JellyTabView.java        # Ícone e nome da aba ativa
│   │       ├── DarkVeilView.java        # Camada visual do fundo
│   │       ├── DarkVeilRenderer.java    # Shader nativo e ciclo de renderização
│   │       ├── GlideCurrencyPicker.java # Menu de moedas com destaque deslizante
│   │       └── MotionPolicy.java        # Preferência de animações do Android
│   └── res/
│       ├── layout/                     # Telas e botão do histórico vazio
│       ├── drawable/                   # Fundos, ícones e estados visuais
│       └── values/                     # Textos, cores e estilos
├── src/test/                           # Testes unitários
├── src/androidTest/                    # Testes instrumentados
└── build.gradle.kts                    # Configuração do módulo Android

apk/
└── cambioPablo.apk                      # Aplicativo disponível para instalação

gradle/                                 # Wrapper e catálogo de dependências
third_party/react-bits/                  # Atribuição e licença dos efeitos adaptados
settings.gradle.kts                     # Configuração dos módulos
```

---

## 🔌 Consulta de Cotações

A aplicação consulta o endpoint `GET /json/last/{pairs}` da AwesomeAPI.

| Consulta | Endpoint |
|---|---|
| Dólar para real | `/json/last/USD-BRL` |
| Euro para real | `/json/last/EUR-BRL` |
| Libra para real | `/json/last/GBP-BRL` |
| Iene para real | `/json/last/JPY-BRL` |
| Bitcoin para real | `/json/last/BTC-BRL` |
| Lista completa | `/json/last/USD-BRL,EUR-BRL,GBP-BRL,JPY-BRL,BTC-BRL` |

O resultado utiliza o campo `bid` recebido da API:

```text
valor em reais = valor informado × cotação de compra
```

O cálculo usa `BigDecimal`, com arredondamento `HALF_UP` e duas casas decimais no resultado.

---

## 🧪 Testes

Execute os testes unitários com:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Para executar os testes instrumentados, mantenha um emulador ou aparelho conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Para verificar recursos, compatibilidade e problemas apontados pelo Android Lint:

```powershell
.\gradlew.bat :app:lintDebug
```

Os testes de `AmountInputFormatterTest` verificam a sequência de digitação, remoção de dígitos, valores colados e formatação de quantias maiores. Há uma divergência nos casos de zero: os testes esperam campo vazio, enquanto o formatador atual retorna `0,00`. Esses casos precisam ser alinhados ao comportamento desejado antes de considerar toda a suíte aprovada.

---

## 🔧 Observações Importantes

1. **Cotações indicativas**: o resultado não inclui impostos, tarifas ou valores cobrados por bancos e corretoras.
2. **Conexão**: novas cotações e conversões dependem de acesso à internet e da disponibilidade da API.
3. **Horário da consulta**: o horário exibido informa quando o aplicativo consultou a cotação, não necessariamente quando o mercado atualizou o valor.
4. **Histórico**: os registros são armazenados localmente em SharedPreferences, no formato JSON, sem uma conta de usuário ou sincronização própria entre aparelhos.
5. **Identidade do aplicativo**: o pacote é `com.opablosantanaa.cambiopablo`. O nome da pasta do projeto não define a identidade instalada no Android.

---

## 📄 Licença

Projeto desenvolvido para estudo e aprendizado em desenvolvimento Android com Java.

Os componentes visuais adaptados possuem atribuição e termos próprios em [`third_party/react-bits/LICENSE.md`](third_party/react-bits/LICENSE.md) e [`NOTICE.txt`](third_party/react-bits/NOTICE.txt). Esses termos não representam uma licença geral do projeto.

---

## 👨‍💻 Autor

Desenvolvido por **Pablo Santana — @opablosantanaa**.