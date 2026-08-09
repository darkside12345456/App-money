# Gestor de Despesas Pessoais

App Android para gerir o orçamento mensal, registar despesas a partir de **fotos
de faturas com leitura automática (OCR)** e organizar tudo por categorias.
Todos os dados ficam **guardados apenas no telemóvel** (sem nuvem).

## Funcionalidades

- **Rendimento mensal** — introduz o ordenado; é a base do orçamento do mês.
- **Foto de faturas com OCR automático** — ao tirar foto, a app extrai
  automaticamente os itens, os valores individuais e o total, e classifica a
  fatura numa categoria. O reconhecimento corre **no dispositivo** (ML Kit),
  sem enviar nada para a internet.
- **Navegação do geral para o detalhe:**
  - Categoria (ex.: Supermercado) → total gasto na categoria.
  - Tocar na categoria → lista de faturas (valor + data).
  - Tocar numa fatura → lista de itens, um a um, com o respetivo valor.
- **Despesas fixas / contas** — luz, água, gás, internet, etc., com data, valor
  e estado de pagamento.
- **Balanço mensal** — total gasto, quanto sobra do ordenado, repartição por
  categoria e comparação entre meses com gráficos simples.
- **Listas de compras** — criar listas, adicionar itens, marcar como comprado
  e apagar.
- **Navegação entre meses** — consulta e edita qualquer mês (não só o atual)
  em todos os ecrãs, com um seletor `‹ mês ›` partilhado.
- **Orçamentos por categoria** — define limites mensais e recebe um aviso
  visual quando ultrapassas.
- **Contas recorrentes** — marca uma conta como recorrente e copia-a para o
  mês seguinte com um toque.
- **Editar faturas** — corrige comerciante, categoria, data, total e itens de
  uma fatura já guardada.
- **Rendimento que transita** — o ordenado do último mês é assumido por
  defeito nos meses seguintes.
- **Cópia de segurança local** — exporta/importa todos os dados para um
  ficheiro JSON que tu controlas (continua sem nuvem).
- **Bloqueio biométrico** — protege a app com impressão digital / rosto.
- **Lembrete de contas** — aviso diário quando há contas do mês por pagar.

## Arquitetura

Stack moderno e minimalista, 100% Kotlin:

| Camada | Tecnologia |
|--------|------------|
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose |
| Estado | ViewModel + Kotlin Flows |
| Base de dados | Room (SQLite local) |
| OCR | ML Kit Text Recognition (on-device) + pré-processamento de imagem |
| Câmara | `ActivityResultContracts.TakePicture` + FileProvider |
| Imagens | Coil |
| Agendamento | WorkManager (lembrete diário) |
| Segurança | AndroidX Biometric (bloqueio da app) |
| Backup | Storage Access Framework + JSON (`org.json`) |

```
com.despesas.gestor
├── data
│   ├── local        # Room: entidades, DAOs, base de dados
│   ├── ocr          # OcrService (ML Kit) + ReceiptParser
│   └── repository   # GestorRepository (ponto único de dados)
├── domain/model     # ExpenseCategory e modelos
└── ui
    ├── theme         # Tema Material 3 (claro/escuro)
    ├── navigation    # Rotas + barra inferior
    ├── components    # Cartões, gráficos, avatares de categoria
    └── screens       # home, categories, capture, fixed, balance, shopping
```

### Como funciona o OCR item-a-item

1. `OcrService` pré-processa a foto (rotação EXIF + escala de cinzentos e
   contraste) e corre o ML Kit, devolvendo as linhas de texto com a respetiva
   posição no ecrã.
2. `ReceiptParser` (código puro, testável) transforma essas linhas numa fatura
   estruturada:
   - **Agrupa** fragmentos à mesma altura numa linha visual (descrição +
     preço, muitas vezes em colunas separadas).
   - **Deteta o total** procurando linhas com `TOTAL` (dá prioridade a
     "TOTAL A PAGAR" e ignora subtotais/IVA).
   - **Deteta itens** — descrição seguida de um preço no fim da linha, com
     filtragem de linhas que não são itens (IVA, troco, NIF, cartão, ...).
   - **Classifica a categoria** por palavras-chave (comerciante e itens).
   - **Deteta a data** por expressões regulares.
   - Interpreta preços em formato europeu (`1.234,56`) e anglo-saxónico.

O ecrã de captura mostra o resultado já preenchido para confirmação rápida,
permitindo corrigir qualquer campo antes de guardar.

## Compilar

```bash
./gradlew assembleDebug
```

Requer o Android SDK (compileSdk 34) e acesso ao Google Maven para as
dependências AndroidX/Compose/ML Kit.

## Testes

O parser de faturas tem testes unitários de JVM (não precisam de emulador):

```bash
./gradlew test
```

Cobrem a extração de itens (incluindo descontos ignorados e itens ao peso),
deteção de total, classificação de categoria, interpretação de preços e o
round-trip da cópia de segurança (JSON).
