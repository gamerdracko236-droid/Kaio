package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class SpeakerId(
    val displayName: String,
    @DrawableRes val portraitRes: Int,
    val pitch: Float,
    val speechRate: Float
) {
    KAUAN("Kauan (Guardião)", R.drawable.img_portrait_kauan, 1.05f, 1.04f),
    SEVERINO("Mestre Severino", R.drawable.img_portrait_severino, 0.78f, 0.90f),
    TAINA("Tainá das Águas", R.drawable.img_portrait_kauan, 1.24f, 1.05f),
    MATEUS("Mateus do Trovão", R.drawable.img_portrait_kauan, 0.96f, 1.12f),
    DONA_SOCORRO("Dona Socorro", R.drawable.img_portrait_severino, 1.12f, 0.92f),
    CORONEL_UMBRA("Coronel Umbra", R.drawable.img_portrait_umbra, 0.68f, 0.86f)
}

data class DialogueChoice(
    val label: String,
    val nextLineIndex: Int,
    val bonusXp: Int = 0
)

data class DialogueLine(
    val speaker: SpeakerId,
    val expression: String,
    val text: String,
    val choices: List<DialogueChoice> = emptyList()
)

data class RegionDefinition(
    val index: Int,
    val id: String,
    val name: String,
    val subtitle: String,
    val chapterRange: IntRange,
    val description: String,
    val mainQuestId: String,
    val mainQuestTitle: String,
    val mainQuestObjective: String,
    val bossName: String,
    val bossTitle: String,
    val bossMaxHp: Float,
    val skyTopRgb: Triple<Float, Float, Float>,
    val skyBottomRgb: Triple<Float, Float, Float>,
    val groundRgb: Triple<Float, Float, Float>,
    val isNight: Boolean,
    val musicTheme: String,
    val npcName: String,
    val npcSpeaker: SpeakerId,
    val introDialogue: List<DialogueLine>,
    val npcDialogue: List<DialogueLine>,
    val victoryDialogue: List<DialogueLine>
)

data class SkillNode(
    val id: String,
    val style: CombatStyle,
    val name: String,
    val description: String,
    val energyCost: Float,
    val damage: Float,
    val cooldownSeconds: Float,
    val isUltimate: Boolean = false
)

data class ShopItemDef(
    val id: String,
    val name: String,
    val description: String,
    val priceCoins: Int,
    val iconLabel: String
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String
)

data class JournalEntryDef(
    val id: String,
    val unlockChapter: Int,
    val title: String,
    val body: String
)

object WorldCatalog {

    val skills: List<SkillNode> = listOf(
        SkillNode("chama_1", CombatStyle.CHAMA, "Corte da Brasa", "Avanço flamejante que queima os Encantados.", 20f, 40f, 3.5f),
        SkillNode("chama_2", CombatStyle.CHAMA, "Roda da Fogueira", "Giro em chamas de 360° que repele cercos.", 30f, 58f, 6.0f),
        SkillNode("chama_ult", CombatStyle.CHAMA, "Sol do Meio-Dia", "Técnica suprema dos Guardiões da Chama.", 75f, 150f, 12.0f, true),

        SkillNode("tempestade_1", CombatStyle.TEMPESTADE, "Lampejo Seco", "Investida elétrica instantânea.", 18f, 38f, 2.8f),
        SkillNode("tempestade_2", CombatStyle.TEMPESTADE, "Trovão em Cadeia", "Múltiplos cortes rápidos que atordoam.", 28f, 55f, 5.5f),
        SkillNode("tempestade_ult", CombatStyle.TEMPESTADE, "Relâmpago do Sertão", "Dragão elétrico que rasga o campo.", 70f, 145f, 11.5f, true),

        SkillNode("caatinga_1", CombatStyle.CAATINGA, "Golpe de Mandacaru", "Corte pesado que estilhaça a postura.", 22f, 44f, 4.0f),
        SkillNode("caatinga_2", CombatStyle.CAATINGA, "Muralha de Granito", "Impacto sísmico com redução de dano.", 32f, 60f, 6.5f),
        SkillNode("caatinga_ult", CombatStyle.CAATINGA, "Fúria dos Monólitos", "Destrói guardas de chefes em área.", 75f, 155f, 13.0f, true),

        SkillNode("aguas_1", CombatStyle.AGUAS, "Maré da Enxurrada", "Sequência fluida que empurra inimigos.", 18f, 36f, 3.0f),
        SkillNode("aguas_2", CombatStyle.AGUAS, "Redemoinho do Jaguaribe", "Vórtice contínuo que recupera fôlego.", 26f, 52f, 5.2f),
        SkillNode("aguas_ult", CombatStyle.AGUAS, "Enchente Sagrada", "Onda cortante que varre a arena.", 70f, 140f, 11.0f, true),

        SkillNode("lua_1", CombatStyle.LUA, "Crescente Sombrio", "Corte em arco com alta chance crítica.", 20f, 42f, 3.2f),
        SkillNode("lua_2", CombatStyle.LUA, "Eclipse Silencioso", "Salto cortante em dupla meia-lua.", 30f, 62f, 6.0f),
        SkillNode("lua_ult", CombatStyle.LUA, "Dança das Sete Luas", "Tempestade de lâminas prateadas.", 75f, 160f, 12.5f, true)
    )

    val shopItems: List<ShopItemDef> = listOf(
        ShopItemDef("pocao", "Chá de Aroeira", "Recupera 65 de Vida imediatamente em combate.", 25, "Cura"),
        ShopItemDef("antidoto", "Elixir de Folha Santa", "Purifica veneno e recupera 25 de Vida.", 30, "Antídoto"),
        ShopItemDef("kunai", "Faca de Ipê-Roxo", "Lança veneno sagrado que impede regeneração do chefe.", 35, "Ataque"),
        ShopItemDef("rapadura", "Rapadura Energética", "Restaura 60 de Energia e Stamina na hora.", 20, "Energia")
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("ach_first_blood", "Despertar da Chama", "Derrote o primeiro Encantado da Sombra na Vila das Almas."),
        AchievementDef("ach_parry", "Pulso Firme", "Execute 3 contra-ataques (Parry) perfeitos."),
        AchievementDef("ach_act1", "Filho de Antônio Brasa", "Conclua o Ato I derrotando o Moedor de Ossos no Engenho."),
        AchievementDef("ach_styles", "Mestre dos Cinco Fôlegos", "Desbloqueie técnicas em todos os 5 estilos de combate."),
        AchievementDef("ach_umbra", "Alvorada do Sertão", "Derrote o Coronel Umbra na Fortaleza do Eclipse.")
    )

    val journalEntries: List<JournalEntryDef> = listOf(
        JournalEntryDef("j_1", 1, "A Noite em Vila das Almas", "Criaturas chamadas Encantados da Sombra surgiram com a lua rubra. A lâmina antiga de Antônio Brasa despertou em chamas nas mãos de Kauan."),
        JournalEntryDef("j_2", 3, "O Treino na Serra dos Ventos", "Mestre Severino revelou que os Guardiões da Chama caíram há dez anos quando o Eclipse começou a se formar sobre o sertão."),
        JournalEntryDef("j_3", 6, "As Cinzas do Engenho", "O Moedor de Ossos guardava uma carta queimada revelando que Antônio Brasa partiu rumo ao Templo da Chama para conter uma fissura."),
        JournalEntryDef("j_4", 8, "A Verdade de Fazenda dos Umbuzeiros", "Mateus descobriu que seu irmão foi transformado pelo Coronel Umbra, não por traição de Antônio Brasa. A rivalidade virou aliança."),
        JournalEntryDef("j_5", 10, "O Sacrifício e o Amanhecer", "Na Fortaleza do Eclipse, Antônio Brasa havia selado metade da escuridão com sua própria vida. Ao derrotar Umbra, o sol voltou a brilhar sobre o sertão.")
    )

    val regions: List<RegionDefinition> = listOf(
        RegionDefinition(
            index = 1,
            id = "vila_das_almas",
            name = "Vila das Almas",
            subtitle = "Caps. 1–2 • O Despertar na Terra Natal",
            chapterRange = 1..2,
            description = "Comunidade acolhedora cercada por mandacarus e casas de taipa, agora ameaçada pelas sombras noturnas.",
            mainQuestId = "q_vila",
            mainQuestTitle = "Proteger a Vila das Almas",
            mainQuestObjective = "Fale com Mestre Severino e derrote o Espreitador do Poço.",
            bossName = "Espreitador do Poço",
            bossTitle = "Encantado Alfa das Sombras",
            bossMaxHp = 220f,
            skyTopRgb = Triple(0.16f, 0.12f, 0.24f),
            skyBottomRgb = Triple(0.85f, 0.42f, 0.20f),
            groundRgb = Triple(0.62f, 0.44f, 0.28f),
            isNight = false,
            musicTheme = "vila",
            npcName = "Mestre Severino",
            npcSpeaker = SpeakerId.SEVERINO,
            introDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Determinado", "As sombras invadiram a Vila das Almas! Esta peixeira do meu pai pegou fogo quando tentei defender minha irmã!"),
                DialogueLine(SpeakerId.SEVERINO, "Sério", "Essa chama pertence aos Guardiões, rapaz. Sou Severino. Segure firme a lâmina — o líder dessas criaturas vem aí!")
            ),
            npcDialogue = listOf(
                DialogueLine(
                    SpeakerId.SEVERINO, "Instrutor",
                    "Antes de partir para a Caatinga Profunda, lembre-se do treino: defenda no instante exato do golpe inimigo para abrir o contra-ataque.",
                    choices = listOf(
                        DialogueChoice("Estou pronto para aprender!", 1, bonusXp = 25),
                        DialogueChoice("Fale-me sobre meu pai, Antônio Brasa.", 2, bonusXp = 25)
                    )
                ),
                DialogueLine(SpeakerId.SEVERINO, "Aprovado", "Ótimo espírito! Use o botão de Defesa no brilho vermelho do inimigo e libere seu Estilo da Chama."),
                DialogueLine(SpeakerId.SEVERINO, "Nostálgico", "Antônio Brasa foi o maior espadachim deste sertão. Ele desapareceu tentando impedir o Eclipse Perpétuo.")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.DONA_SOCORRO, "Aliviada", "Você salvou nossa vila, Kauan! Leve este chá de aroeira e siga Mestre Severino pela Caatinga Profunda."),
                DialogueLine(SpeakerId.KAUAN, "Resoluto", "Eu vou descobrir a verdade sobre meu pai e acabar com essa maldição.")
            )
        ),
        RegionDefinition(
            index = 2,
            id = "caatinga_profunda",
            name = "Caatinga Profunda",
            subtitle = "Caps. 3–4 • Trilhas de Espinhos e Fogo",
            chapterRange = 3..4,
            description = "Sertão fechado sob sol escaldante e trilhas estreitas entre pedregulhos e cactos gigantes.",
            mainQuestId = "q_caatinga",
            mainQuestTitle = "Sobreviver aos Espinhos",
            mainQuestObjective = "Encontre Tainá e vença o Devorador de Jurema.",
            bossName = "Devorador de Jurema",
            bossTitle = "Fera Blindada da Seca",
            bossMaxHp = 300f,
            skyTopRgb = Triple(0.24f, 0.45f, 0.72f),
            skyBottomRgb = Triple(0.92f, 0.72f, 0.44f),
            groundRgb = Triple(0.72f, 0.54f, 0.32f),
            isNight = false,
            musicTheme = "exploracao",
            npcName = "Tainá das Águas",
            npcSpeaker = SpeakerId.TAINA,
            introDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Alerta", "Cuidado onde pisa, novato! As feras da Caatinga Profunda atacam em bando e têm casca dura."),
                DialogueLine(SpeakerId.KAUAN, "Confiante", "Sou Kauan, discípulo de Severino. Juntos vamos abrir caminho até a Serra dos Ventos!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Parceira", "Trouxe técnicas do Estilo das Águas. Se o calor apertar, alterne os estilos para manter o combo fluido.")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Impressionada", "Esse golpe de chama quebrou a carapaça do Devorador! A subida para a Serra dos Ventos está livre.")
            )
        ),
        RegionDefinition(
            index = 3,
            id = "serra_dos_ventos",
            name = "Serra dos Ventos",
            subtitle = "Caps. 5–6 • O Treino dos Guardiões",
            chapterRange = 5..6,
            description = "Planalto alto de neblina fria, ventos cortantes e pedras sagradas onde treinam os sentinelas.",
            mainQuestId = "q_serra",
            mainQuestTitle = "A Prova do Relâmpago",
            mainQuestObjective = "Enfrente o teste de Mateus e o Espírito da Ventania.",
            bossName = "Gavião das Brumas",
            bossTitle = "Espectro Veloz da Serra",
            bossMaxHp = 380f,
            skyTopRgb = Triple(0.18f, 0.30f, 0.44f),
            skyBottomRgb = Triple(0.62f, 0.74f, 0.80f),
            groundRgb = Triple(0.38f, 0.48f, 0.42f),
            isNight = false,
            musicTheme = "exploracao",
            npcName = "Mateus do Trovão",
            npcSpeaker = SpeakerId.MATEUS,
            introDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Desafiador", "Então você é o filho de Antônio Brasa? Não pense que o sobrenome garante vaga entre os Guardiões!"),
                DialogueLine(SpeakerId.KAUAN, "Firme", "Não luto pelo meu sobrenome, Mateus. Luto para proteger quem ficou para trás!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Rival", "O Gavião das Brumas é rápido demais para cortes lentos. Use esquiva lateral e o Estilo da Tempestade.")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Respeitoso", "Hmph... nada mal. Talvez você realmente consiga acompanhar nosso ritmo na Feira de Pedra Branca.")
            )
        ),
        RegionDefinition(
            index = 4,
            id = "feira_pedra_branca",
            name = "Feira de Pedra Branca",
            subtitle = "Caps. 7–8 • Mistérios no Mercado",
            chapterRange = 7..8,
            description = "Vila histórica cheia de barracas, lampiões coloridos e pistas sobre o paradeiro dos desaparecidos.",
            mainQuestId = "q_feira",
            mainQuestTitle = "Sombras Entre os Lampiões",
            mainQuestObjective = "Investigue os desaparecimentos e derrote o Ilusionista da Feira.",
            bossName = "Violeiro Maldito",
            bossTitle = "Encantado das Cordas Cortantes",
            bossMaxHp = 450f,
            skyTopRgb = Triple(0.14f, 0.14f, 0.28f),
            skyBottomRgb = Triple(0.75f, 0.38f, 0.25f),
            groundRgb = Triple(0.55f, 0.46f, 0.38f),
            isNight = false,
            musicTheme = "vila",
            npcName = "Dona Socorro",
            npcSpeaker = SpeakerId.DONA_SOCORRO,
            introDialogue = listOf(
                DialogueLine(SpeakerId.DONA_SOCORRO, "Preocupada", "Os comerciantes ouviram acordes estranhos perto do mercado e sumiram antes da meia-noite!"),
                DialogueLine(SpeakerId.KAUAN, "Atento", "Vou patrulhar a feira e descobrir quem está por trás disso.")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.DONA_SOCORRO, "Sábia", "Compre suprimentos na Loja antes de entrar no Engenho Abandonado. Folha Santa salva vidas!")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Surpreso", "O Violeiro deixou cair um mapa marcado para o Engenho Abandonado. Há uma base inimiga lá dentro!")
            )
        ),
        RegionDefinition(
            index = 5,
            id = "engenho_abandonado",
            name = "Engenho Abandonado",
            subtitle = "Caps. 9–10 • Clímax do Ato I",
            chapterRange = 9..10,
            description = "Ruínas de pedra e ferro tomadas por cinzas e engrenagens sombrias.",
            mainQuestId = "q_engenho",
            mainQuestTitle = "O Moedor de Ossos",
            mainQuestObjective = "Invada o salão principal e derrote o General do Engenho.",
            bossName = "Moedor de Ossos",
            bossTitle = "Lua Sombria Inferior",
            bossMaxHp = 560f,
            skyTopRgb = Triple(0.10f, 0.06f, 0.08f),
            skyBottomRgb = Triple(0.45f, 0.18f, 0.12f),
            groundRgb = Triple(0.34f, 0.28f, 0.26f),
            isNight = true,
            musicTheme = "combate",
            npcName = "Mestre Severino",
            npcSpeaker = SpeakerId.SEVERINO,
            introDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Tenso", "Aqui dentro reside uma Lua Sombria. Prepare sua lâmina e não desperdice seu fôlego!"),
                DialogueLine(SpeakerId.KAUAN, "Feroz", "Por todas as famílias do sertão, hoje essa escuridão acaba!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Estratégico", "Quando o Moedor de Ossos entrar na segunda fase, use sua Técnica Suprema e Facas de Ipê-Roxo!")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Emocionado", "Esta carta nos destroços... Meu pai esteve aqui há cinco anos! Ele foi rumo ao Templo da Chama para conter o Eclipse!")
            )
        ),
        RegionDefinition(
            index = 6,
            id = "cemiterio_sombras",
            name = "Cemitério das Sombras",
            subtitle = "Caps. 11–12 • Segredos do Estilo Lunar",
            chapterRange = 11..12,
            description = "Campo antigo de cruzes de madeira, névoa roxa e almas presas sob o luar.",
            mainQuestId = "q_cemiterio",
            mainQuestTitle = "Libertar as Almas Presas",
            mainQuestObjective = "Domine o Estilo da Lua e vença a Dama do Véu Roxo.",
            bossName = "Dama do Véu Roxo",
            bossTitle = "Tecelã de Pesadelos",
            bossMaxHp = 660f,
            skyTopRgb = Triple(0.06f, 0.05f, 0.16f),
            skyBottomRgb = Triple(0.28f, 0.18f, 0.44f),
            groundRgb = Triple(0.26f, 0.24f, 0.32f),
            isNight = true,
            musicTheme = "combate",
            npcName = "Tainá das Águas",
            npcSpeaker = SpeakerId.TAINA,
            introDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Sussurro", "A névoa aqui reflete memórias antigas... Não deixe a Dama do Véu Roxo confundir seus sentidos!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Firme", "Observe os brilhos crescentes: você acabou de despertar a sintonia com o Estilo da Lua.")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Sereno", "As almas descansaram em paz. Agora vamos socorrer a Fazenda dos Umbuzeiros.")
            )
        ),
        RegionDefinition(
            index = 7,
            id = "fazenda_umbuzeiros",
            name = "Fazenda dos Umbuzeiros",
            subtitle = "Caps. 13–14 • Verdade e Aliança",
            chapterRange = 13..14,
            description = "Grande propriedade rural sitiada pelos exércitos do Coronel Umbra.",
            mainQuestId = "q_fazenda",
            mainQuestTitle = "Defesa dos Umbuzeiros",
            mainQuestObjective = "Proteja os moradores e derrote o Cavaleiro da Seca.",
            bossName = "Cavaleiro da Seca",
            bossTitle = "Arauto do Coronel Umbra",
            bossMaxHp = 780f,
            skyTopRgb = Triple(0.20f, 0.14f, 0.10f),
            skyBottomRgb = Triple(0.86f, 0.48f, 0.22f),
            groundRgb = Triple(0.56f, 0.44f, 0.26f),
            isNight = false,
            musicTheme = "combate",
            npcName = "Mateus do Trovão",
            npcSpeaker = SpeakerId.MATEUS,
            introDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Abalado", "Kauan... eu estava errado. Foi o Coronel Umbra quem destruiu meu povoado, e seu pai tentou nos salvar!"),
                DialogueLine(SpeakerId.KAUAN, "Determinado", "Então esqueça o passado, Mateus! Vamos defender esta fazenda lado a lado!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Aliado", "Meu Estilo da Tempestade agora luta ao seu lado. Vamos abrir caminho até Santa Esperança!")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.MATEUS, "Leal", "A vitória é nossa! Em Santa Esperança o grande ferreiro vai temperar sua lâmina ao nível máximo.")
            )
        ),
        RegionDefinition(
            index = 8,
            id = "santa_esperanca",
            name = "Cidade de Santa Esperança",
            subtitle = "Caps. 15–16 • O Quartel dos Guardiões",
            chapterRange = 15..16,
            description = "Cidade histórica iluminada por fogueiras de resistência e quartel secreto da ordem.",
            mainQuestId = "q_cidade",
            mainQuestTitle = "A Prova dos Mestres",
            mainQuestObjective = "Prepare seu equipamento e vença o Invasor da Torre.",
            bossName = "General Ferro-Negro",
            bossTitle = "Lua Superior Quatro",
            bossMaxHp = 920f,
            skyTopRgb = Triple(0.14f, 0.22f, 0.36f),
            skyBottomRgb = Triple(0.88f, 0.62f, 0.36f),
            groundRgb = Triple(0.52f, 0.46f, 0.42f),
            isNight = false,
            musicTheme = "vila",
            npcName = "Mestre Severino",
            npcSpeaker = SpeakerId.SEVERINO,
            introDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Orgulhoso", "Bem-vindo a Santa Esperança, Kauan. Aqui estão reunidos os últimos mestres de todos os estilos."),
                DialogueLine(SpeakerId.KAUAN, "Focado", "Vou evoluir minha lâmina e partir para o Templo da Chama!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Mestre", "Melhore seus atributos na Árvore de Habilidades antes de enfrentar o General Ferro-Negro.")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Solene", "Você superou os mestres antigos. O caminho para o Templo da Chama está aberto!")
            )
        ),
        RegionDefinition(
            index = 9,
            id = "templo_da_chama",
            name = "Templo da Chama",
            subtitle = "Caps. 17–18 • O Coração do Sol",
            chapterRange = 17..18,
            description = "Santuário ancestral esculpido em rocha vermelha e brasas eternas sob os cânions.",
            mainQuestId = "q_templo",
            mainQuestTitle = "O Legado de Antônio Brasa",
            mainQuestObjective = "Desperte a Chama Suprema e derrote o Guardião Corrompido.",
            bossName = "Sombra de Brasa",
            bossTitle = "Lua Superior Dois • Guardião Caído",
            bossMaxHp = 1100f,
            skyTopRgb = Triple(0.24f, 0.08f, 0.05f),
            skyBottomRgb = Triple(0.96f, 0.40f, 0.14f),
            groundRgb = Triple(0.38f, 0.18f, 0.14f),
            isNight = true,
            musicTheme = "combate",
            npcName = "Tainá das Águas",
            npcSpeaker = SpeakerId.TAINA,
            introDialogue = listOf(
                DialogueLine(SpeakerId.TAINA, "Admirada", "As inscrições nas pedras mostram o nome do seu pai! Antônio Brasa selou metade do poder do Eclipse aqui!"),
                DialogueLine(SpeakerId.CORONEL_UMBRA, "Ameaçador", "Tarde demais, pequeno guardião. Quando o eclipse se fechar, nem a chama do sol restará!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Inabalável", "Pai... eu sinto sua força nesta lâmina. Não vamos recuar!")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.KAUAN, "Pronto", "A Marca do Sol despertou! Agora só falta um lugar: a Fortaleza do Eclipse!")
            )
        ),
        RegionDefinition(
            index = 10,
            id = "fortaleza_eclipse",
            name = "Fortaleza do Eclipse",
            subtitle = "Caps. 19–20 • O Confronto Final",
            chapterRange = 19..20,
            description = "Cidadela suspensa sob um eclipse solar perpétuo onde reside o Coronel Umbra.",
            mainQuestId = "q_final",
            mainQuestTitle = "Alvorada Sobre o Sertão",
            mainQuestObjective = "Derrote o Coronel Umbra nas 3 fases do Eclipse Final.",
            bossName = "Coronel Umbra",
            bossTitle = "Senhor do Eclipse Perpétuo",
            bossMaxHp = 1450f,
            skyTopRgb = Triple(0.05f, 0.02f, 0.10f),
            skyBottomRgb = Triple(0.42f, 0.08f, 0.16f),
            groundRgb = Triple(0.18f, 0.14f, 0.22f),
            isNight = true,
            musicTheme = "chefe_final",
            npcName = "Coronel Umbra",
            npcSpeaker = SpeakerId.CORONEL_UMBRA,
            introDialogue = listOf(
                DialogueLine(SpeakerId.CORONEL_UMBRA, "Frio", "Você tem os mesmos olhos de Antônio Brasa. Venha, jovem guardião... mostre se sua chama sobrevive à noite eterna!"),
                DialogueLine(SpeakerId.KAUAN, "Supremo", "Todos os estilos, todos os amigos e o sertão inteiro lutam comigo hoje! Vamos trazer o amanhecer de volta!")
            ),
            npcDialogue = listOf(
                DialogueLine(SpeakerId.SEVERINO, "Último Conselho", "Segure firme até a terceira fase dele, Kauan! Quando a luz romper o eclipse, acerte o golpe final!")
            ),
            victoryDialogue = listOf(
                DialogueLine(SpeakerId.CORONEL_UMBRA, "Derrotado", "Essa luz... é forte demais... Antônio... seu filho... trouxe o sol de volta..."),
                DialogueLine(SpeakerId.KAUAN, "Vitorioso", "O Eclipse acabou! O sol voltou a brilhar sobre toda a nossa terra!")
            )
        )
    )

    fun regionById(id: String): RegionDefinition =
        regions.firstOrNull { it.id == id } ?: regions.first()

    fun regionForChapter(chapter: Int): RegionDefinition =
        regions.firstOrNull { chapter in it.chapterRange } ?: regions.first()
}
