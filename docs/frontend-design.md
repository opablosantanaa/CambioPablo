# Frontend Câmbio

Aplicativo Android nativo em Java e XML, com identidade visual própria para Câmbio Pablo.

## Direção visual

Referência: https://cdn.dribbble.com/userupload/42812880/file/original-d1fe5f69598e2004e9ea3e1c84094b7a.png

Adaptação da carteira financeira da referência: azul no topo, superfície escura, painéis com contorno translúcido e ação violeta. O resultado da conversão é o foco. Não há saldos, variações ou gráficos simulados.

| Token | Valor | Uso |
| --- | --- | --- |
| Azul profundo | #050B20 | Fundo |
| Azul luminoso | #386AA6 | Topo |
| Painel | #141E38 | Superfície |
| Violeta | #7945FF | Ação principal e aba ativa |
| Menta | #57E9BB | Destino em reais |
| Texto | #F6F8FF | Conteúdo principal |

Tipografia: sans-serif do Android para uma leitura próxima à referência, com sans-serif-medium em títulos, resultado e campo de valor. Escala de 11–16sp para suporte, 20–28sp para títulos e até 42sp para resultado; ajuste automático para valores longos.

Layout alinhado à esquerda, valor de entrada à direita, resultado no topo, campos agrupados em um painel e navegação fixa abaixo da área rolável. Espaçamento externo de 24dp. Contorno dos painéis de 24dp; controles internos de 16dp. Em telas abaixo de 360dp ou com fontes ampliadas, moeda e valor ficam empilhados.

```text
Marca                              Ajuda
Você recebe em reais
Resultado
┌───────────────────────────────────────┐
│ Você converte                         │
│ Moeda de origem                 Valor │
│ Real brasileiro              Destino │
│             Converter agora           │
│ Cotação consultada e horário          │
└───────────────────────────────────────┘
Cotações                       Atualizar
Moedas com preços reais da API
┌───────────────────────────────────────┐
│ Converter       Cotações    Histórico │
└───────────────────────────────────────┘
```

A revisão da direção removeu elementos próprios de carteira cripto que não fazem parte deste aplicativo: avatar fictício, saldo pessoal, ganho percentual e gráfico decorativo. As abas oferecem ações existentes: conversão em BRL, cotações e histórico local. Os painéis arredondados e o degradê seguem a direção explicitamente pedida pela referência.

## Comportamento

- Cinco moedas preservadas: USD, EUR, GBP, JPY e BTC, com destino BRL.
- A API, o cliente Retrofit e os modelos permanecem inalterados.
- A tela consulta cotações reais; nenhum preço é incorporado ao código.
- Entrada aceita 1.000,50, 10,50 e 10.50. Valores vazios, zero, malformados e acima do limite recebem validação.
- Carregamento, falha de conexão e resposta inválida têm mensagens e recuperação.
- Histórico persistente com as últimas 20 conversões, apenas no aparelho.
- Mudanças na entrada cancelam a consulta anterior para evitar resultados de valores antigos.
- Insets de barras do sistema e teclado, áreas roláveis, alvos de toque e estados de foco/pressão.
- Sem animações contínuas. Recursos Android guardam o texto da interface.

## Renomeação

`rootProject.name` foi atualizado para CambioPablo. MainActivity passou a usar o R do mesmo namespace da classe. O applicationId da versão nova é com.opablosantanaa.cambiopablo, permitindo instalá-la ao lado do aplicativo original com.opablosantanaa.convertermoedas. O nome de instalação é Câmbio Pablo. Cada aplicativo mantém seu próprio histórico. O SDK local não aponta para a pasta antiga.

## Compilar

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --console=plain
```

APK: app/build/outputs/apk/debug/app-debug.apk.
## Validação concluída

- assembleDebug, lintDebug e testDebugUnitTest: BUILD SUCCESSFUL.
- Lint: zero erros e sete avisos preexistentes de dependências/configuração de catálogo. Nenhum aviso introduzido pelo frontend.
- Teste unitário existente: uma execução, zero falhas.
- Emulador API 35: conversão real de USD para BRL; seleção de EUR e entrada 10.50; preços de USD/EUR/GBP/JPY/BTC; histórico após reiniciar o aplicativo; zero e campo vazio sem crash; entrada 10,50 preservada e convertida com aparelho em inglês; falha de conexão e reconexão, com botão novamente habilitado.
- Revisão visual: tela de 411dp e tela de 320dp. Moeda e valor se empilham na tela menor, navegação permanece disponível e conteúdo é rolável. O resultado e o valor digitado são preservados ao mudar a largura.
- Capturas: frontend-preview.png, frontend-320dp.png e history-preview.png neste diretório.
- O APK foi instalado apenas no emulador criado para validação; o aparelho físico conectado não foi alterado.

## Máscara de centavos no addValue

O campo inicia vazio, mostrando a hint 0,00. Cada dígito digitado desloca o valor em centavos: 1 gera 0,01; depois 0 gera 0,10; depois 5 gera 01,05. Apagar reverte a sequência e volta ao campo vazio com a hint. A máscara mantém duas casas decimais e agrupa milhares. AmountInputFormatter concentra a formatação, e o TextWatcher evita recursão ao atualizar o texto.

Validação desta alteração: quatro testes de regressão da máscara e o teste unitário existente passaram; assembleDebug e lintDebug concluíram sem erros. A sequência completa de digitação e exclusão foi conferida no emulador Android. A mudança do usuário para hint foi preservada.


## Identidade e autoria

Nome do aplicativo: Câmbio Pablo. Namespace e identificador: com.opablosantanaa.cambiopablo. A aba inicial inclui o crédito Desenvolvido por @opablosantanaa. A versão original conserva seu próprio identificador e pode continuar instalada. Esta identidade é uma instalação nova em relação à versão anterior; o histórico não é transferido automaticamente.
