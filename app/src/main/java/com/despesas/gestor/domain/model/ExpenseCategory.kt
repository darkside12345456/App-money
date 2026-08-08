package com.despesas.gestor.domain.model

/**
 * Categorias de despesa suportadas pela app.
 *
 * Cada categoria traz um conjunto de palavras-chave usadas pela classificação
 * automática do OCR: quando o texto de uma fatura contém uma destas palavras
 * (no nome do comerciante ou nos itens), a fatura é atribuída a esta categoria.
 *
 * A ordem importa: categorias mais específicas devem aparecer antes das
 * genéricas para que o "primeiro que combina" faça sentido.
 */
enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val keywords: List<String>
) {
    SUPERMERCADO(
        id = "supermercado",
        displayName = "Supermercado",
        keywords = listOf(
            "continente", "pingo doce", "pingodoce", "lidl", "aldi", "auchan",
            "jumbo", "minipreco", "mini preco", "intermarche", "intermarché",
            "mercadona", "el corte", "supermercado", "super", "mercearia",
            "talho", "padaria", "frutaria", "spar", "meu super", "amanhecer"
        )
    ),
    RESTAURACAO(
        id = "restauracao",
        displayName = "Restauração",
        keywords = listOf(
            "restaurante", "cafe", "café", "snack", "bar", "tasca", "pizzaria",
            "hamburguer", "mcdonald", "burger king", "kfc", "telepizza",
            "padaria portuguesa", "starbucks", "pastelaria", "churrasqueira",
            "cerveja", "menu", "almoço", "jantar", "esplanada"
        )
    ),
    TRANSPORTES(
        id = "transportes",
        displayName = "Transportes",
        keywords = listOf(
            "galp", "bp", "repsol", "cepsa", "prio", "combustivel", "combustível",
            "gasolina", "gasoleo", "gasóleo", "portagem", "via verde", "cp comboios",
            "carris", "metro", "metropolitano", "bolt", "uber", "taxi", "táxi",
            "parque", "estacionamento", "posto"
        )
    ),
    SAUDE(
        id = "saude",
        displayName = "Saúde",
        keywords = listOf(
            "farmacia", "farmácia", "parafarmacia", "clinica", "clínica",
            "hospital", "dentista", "medico", "médico", "análises", "analises",
            "otica", "ótica", "wells"
        )
    ),
    VESTUARIO(
        id = "vestuario",
        displayName = "Vestuário",
        keywords = listOf(
            "zara", "bershka", "pull", "stradivarius", "h&m", "hm", "primark",
            "sport zone", "sportzone", "nike", "adidas", "decathlon", "mango",
            "lefties", "c&a", "roupa", "calçado", "calcado", "sapataria"
        )
    ),
    CASA(
        id = "casa",
        displayName = "Casa",
        keywords = listOf(
            "ikea", "leroy", "leroy merlin", "aki", "maxmat", "worten", "fnac",
            "conforama", "bricolage", "ferragens", "moveis", "móveis", "jysk"
        )
    ),
    LAZER(
        id = "lazer",
        displayName = "Lazer",
        keywords = listOf(
            "cinema", "nos cinemas", "cinemas", "spotify", "netflix", "steam",
            "livraria", "bertrand", "fnac lazer", "ginasio", "ginásio", "fitness",
            "jogos", "concerto", "bilhete"
        )
    ),
    CONTAS(
        id = "contas",
        displayName = "Contas",
        keywords = listOf(
            "edp", "endesa", "iberdrola", "galp energia", "goldenergy", "luz",
            "eletricidade", "electricidade", "agua", "água", "epal", "aguas",
            "águas", "gas", "gás", "meo", "nos", "vodafone", "nowo", "internet",
            "telecomunicacoes", "telecomunicações"
        )
    ),
    OUTROS(
        id = "outros",
        displayName = "Outros",
        keywords = emptyList()
    );

    companion object {
        fun fromId(id: String?): ExpenseCategory =
            entries.firstOrNull { it.id == id } ?: OUTROS
    }
}
