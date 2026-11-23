package com.permalert.domain

enum class AlertType { START, END, NONE }

/** Узкий классификатор для доверенных локальных источников: MAX, МЧС и SMS. */
object Classifier {
    private val training = listOf("учения", "тренировк", "учебн")

    private val endPhrases = listOf(
        "отбой угрозы", "отбой воздушной тревоги", "отбой ракетной опасности",
        "отбой беспилотной опасности", "угроза миновала", "опасность миновала",
        "угроза снята", "угрозы нет", "опасность снята"
    )

    private val startPhrases = listOf(
        "беспилотная опасность", "беспилотной опасности",
        "угроза бпла", "угроза беспилот", "угроза ракет",
        "атака бпла", "атаки бпла", "обнаружен бпла", "обнаружен беспилот",
        "обнаружение бпла", "обнаружение беспилот", "беспилотник обнаружен",
        "беспилотник замечен", "бпла замечен", "ракетная опасность",
        "ракетной опасности", "ракетная угроза", "ракетной угрозы",
        "воздушная тревога", "угроза атаки", "опасность атаки"
    )

    private val explicitNegation = Regex(
        "(?:угроз\\w*|опасност\\w*|атак\\w*|бпла|беспилот\\w*|ракетн\\w*)" +
            "[^.!?]{0,55}(?:не подтвержден\\w*|не объявлен\\w*|не вводил\\w*|не было|не существует|отсутствует|опровергнут\\w*|ложн\\w*|фейк)"
    )

    /** Возвращает тип только при наличии конкретной формулировки угрозы или отбоя. */
    fun classifyStrict(text: String): AlertType {
        val normalized = text.lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank() || training.any { it in normalized }) return AlertType.NONE

        // Отбой проверяется первым: он имеет приоритет над словами «угроза» и «БПЛА».
        if (endPhrases.any { it in normalized }) return AlertType.END
        if (("отменен" in normalized || "снят" in normalized) && "беспилотн" in normalized) return AlertType.END

        // Не запускаем сирену по явно опровергнутым/неподтверждённым сообщениям.
        if (explicitNegation.containsMatchIn(normalized)) return AlertType.NONE
        if (startPhrases.any { it in normalized }) return AlertType.START
        return AlertType.NONE
    }
}
