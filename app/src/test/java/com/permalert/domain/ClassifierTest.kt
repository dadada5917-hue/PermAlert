package com.permalert.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierTest {
    @Test fun recognizesThreatAndAllClear() {
        assertEquals(AlertType.START, Classifier.classifyStrict("Внимание! Угроза атаки БПЛА"))
        assertEquals(AlertType.END, Classifier.classifyStrict("Объявлен отбой угрозы БПЛА"))
    }

    @Test fun ignoresTrainingAndNews() {
        assertEquals(AlertType.NONE, Classifier.classifyStrict("МЧС проводит учебные учения с БПЛА"))
        assertEquals(AlertType.NONE, Classifier.classifyStrict("В новостях показали новый дрон"))
    }

    @Test fun ignoresExplicitlyDisprovedThreat() {
        assertEquals(AlertType.NONE, Classifier.classifyStrict("Сообщение: угроза БПЛА не подтверждена"))
    }
}
