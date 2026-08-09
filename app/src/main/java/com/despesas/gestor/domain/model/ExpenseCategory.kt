package com.despesas.gestor.domain.model

/**
 * Categorias de despesa suportadas pela app.
 *
 * Cada categoria traz palavras-chave usadas pela classificação automática:
 * quando o texto de uma fatura contém uma destas palavras (sobretudo no nome do
 * comerciante), a fatura é atribuída a esta categoria. O classificador
 * (ver [com.despesas.gestor.data.ocr.ReceiptParser]) pontua todas as categorias
 * e escolhe a mais provável, pelo que faz sentido ter listas abrangentes.
 *
 * Palavras de uma só palavra são procuradas com fronteiras (ex.: "gás" não
 * combina dentro de "gasóleo"); expressões com espaços são procuradas como
 * subcadeia.
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
            "jumbo", "minipreco", "mini preco", "mini-preço", "intermarche",
            "intermarché", "mercadona", "el corte ingles", "el corte inglês",
            "supermercado", "hipermercado", "mercearia", "talho", "frutaria",
            "spar", "meu super", "amanhecer", "froiz", "coviran", "leclerc",
            "e.leclerc", "recheio", "makro", "apolonia", "apolónia", "celeiro",
            "go natural", "mercado"
        )
    ),
    RESTAURACAO(
        id = "restauracao",
        displayName = "Restauração",
        keywords = listOf(
            "restaurante", "cafe", "café", "cafetaria", "cafeteria", "snack",
            "snack-bar", "tasca", "taberna", "cervejaria", "marisqueira",
            "churrasqueira", "pizzaria", "pizza", "hamburgueria", "mcdonald",
            "mccafe", "burger king", "burguer", "kfc", "telepizza", "dominos",
            "domino's", "pizza hut", "h3", "vitaminas", "poke", "sushi",
            "kebab", "doner", "gelataria", "gelados", "santini", "starbucks",
            "pastelaria", "padaria portuguesa", "prego", "francesinha", "tapas",
            "wok", "noodles", "grelha", "grelhados", "brasa", "esplanada",
            "quiosque", "adega", "food", "confeitaria", "docaria", "doçaria"
        )
    ),
    TRANSPORTES(
        id = "transportes",
        displayName = "Transportes",
        keywords = listOf(
            "galp", "bp", "repsol", "cepsa", "prio", "combustivel", "combustível",
            "gasolina", "gasoleo", "gasóleo", "gpl", "abastecimento", "shell",
            "petrogal", "aral", "portagem", "portagens", "via verde", "brisa",
            "ascendi", "comboios", "carris", "metro", "metropolitano", "bolt",
            "uber", "free now", "cabify", "taxi", "táxi", "estacionamento",
            "parquimetro", "parquímetro", "parcometro", "parcómetro", "empark",
            "saba", "flixbus", "rede expressos", "transdev", "gira", "trotinete"
        )
    ),
    SAUDE(
        id = "saude",
        displayName = "Saúde",
        keywords = listOf(
            "farmacia", "farmácia", "farmacias", "farmácias", "parafarmacia",
            "parafarmácia", "clinica", "clínica", "hospital", "dentista",
            "dentaria", "dentária", "medico", "médico", "medicina", "analises",
            "análises", "laboratorio", "laboratório", "labco", "unilabs",
            "germano de sousa", "cuf", "luz saude", "lusiadas", "lusíadas",
            "hpa", "sns", "consulta", "fisioterapia", "psicolog", "nutricion",
            "veterinario", "veterinária", "veterinaria", "vet", "otica", "ótica",
            "oculista", "multiopticas", "opticalia", "wells", "medis",
            "advancecare"
        )
    ),
    VESTUARIO(
        id = "vestuario",
        displayName = "Vestuário",
        keywords = listOf(
            "zara", "bershka", "pull&bear", "pull and bear", "stradivarius",
            "massimo dutti", "oysho", "h&m", "primark", "lefties", "springfield",
            "tiffosi", "levis", "levi's", "calzedonia", "intimissimi",
            "women secret", "women'secret", "parfois", "cortefiel", "c&a",
            "sport zone", "sportzone", "decathlon", "nike", "adidas",
            "jd sports", "foot locker", "footlocker", "sports direct", "seaside",
            "deichmann", "sapataria", "calçado", "calcado", "sapatos", "roupa",
            "vestuario", "vestuário", "boutique"
        )
    ),
    CASA(
        id = "casa",
        displayName = "Casa",
        keywords = listOf(
            "ikea", "leroy merlin", "leroy", "aki", "maxmat", "bricomarche",
            "bricomarché", "bricodepot", "brico depot", "conforama", "jysk",
            "flying tiger", "sostrene", "søstrene", "worten", "radio popular",
            "rádio popular", "pcdiga", "fnac", "media markt", "mediamarkt",
            "ferragens", "bricolage", "moveis", "móveis", "eletrodomestico",
            "electrodomestico", "mobiliario", "mobiliário", "utilidades",
            "decoracao", "decoração"
        )
    ),
    LAZER(
        id = "lazer",
        displayName = "Lazer",
        keywords = listOf(
            "cinema", "cinemas", "uci", "spotify", "netflix", "hbo", "disney",
            "prime video", "dazn", "twitch", "steam", "playstation", "xbox",
            "nintendo", "epic games", "google play", "app store", "itunes",
            "audible", "kindle", "livraria", "bertrand", "wook", "ginasio",
            "ginásio", "fitness", "holmes place", "fitness hut", "phive",
            "solinca", "piscina", "spa", "teatro", "museu", "concerto",
            "bilhete", "ticketline", "blueticket", "zoo", "oceanario",
            "oceanário", "kidzania", "brinquedos", "imaginarium", "hobby"
        )
    ),
    CONTAS(
        id = "contas",
        displayName = "Contas",
        keywords = listOf(
            "edp", "endesa", "iberdrola", "galp energia", "galp power",
            "goldenergy", "gold energy", "plenitude", "coopernico", "coopérnico",
            "luzboa", "energia", "eletricidade", "electricidade", "luz", "agua",
            "água", "aguas", "águas", "epal", "smas", "indaqua", "be water",
            "gas", "gás", "gas natural", "dianagas", "meo", "nos", "vodafone",
            "nowo", "digi", "uzo", "woo", "internet", "telecomunicacoes",
            "telecomunicações", "fibra", "tarifario", "tarifário", "seguro",
            "seguros", "apolice", "apólice", "fidelidade", "tranquilidade",
            "ageas", "allianz", "zurich", "generali", "mapfre", "renda",
            "condominio", "condomínio", "imi", "iuc"
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
