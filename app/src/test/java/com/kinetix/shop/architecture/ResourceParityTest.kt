package com.kinetix.shop.architecture

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ResourceParityTest {
    private val placeholderRegex = Regex("""%\d+\$(?:\.\d+)?[dfs]""")

    @Test
    fun indonesianStringsHaveExactKeyAndPlaceholderParityWithDefaultEnglish() {
        val resDir = resolveResDirectory()
        val defaultStringsFile = File(resDir, "values/strings.xml")
        val indonesianStringsFile = File(resDir, "values-in/strings.xml")
        val localesConfigFile = File(resDir, "xml/locales_config.xml")

        assertTrue("Missing default strings.xml", defaultStringsFile.exists())
        assertTrue("Missing Indonesian values-in/strings.xml", indonesianStringsFile.exists())
        assertTrue("Missing xml/locales_config.xml", localesConfigFile.exists())

        val defaultStrings = parseStringEntries(defaultStringsFile)
        val indonesianStrings = parseStringEntries(indonesianStringsFile)
        assertEquals(
            "String key mismatch between values/strings.xml and values-in/strings.xml",
            defaultStrings.keys,
            indonesianStrings.keys,
        )

        for ((key, englishValue) in defaultStrings) {
            val indonesianValue = indonesianStrings.getValue(key)
            assertTrue("Blank Indonesian string for key $key", indonesianValue.isNotBlank())
            val englishPlaceholders = placeholderRegex.findAll(englishValue).map { it.value }.toSet()
            val indonesianPlaceholders = placeholderRegex.findAll(indonesianValue).map { it.value }.toSet()
            assertEquals(
                "Placeholder mismatch for string key $key",
                englishPlaceholders,
                indonesianPlaceholders,
            )
        }

        val defaultPlurals = parsePluralEntries(defaultStringsFile)
        val indonesianPlurals = parsePluralEntries(indonesianStringsFile)
        assertEquals(
            "Plurals key mismatch between values/strings.xml and values-in/strings.xml",
            defaultPlurals.keys,
            indonesianPlurals.keys,
        )
        assertTrue("Expected at least one <plurals> entry for counts", defaultPlurals.isNotEmpty())

        for ((key, englishItems) in defaultPlurals) {
            val indonesianItems = indonesianPlurals.getValue(key)
            val englishOther = englishItems["other"].orEmpty()
            val indonesianOther = indonesianItems["other"].orEmpty()
            assertTrue("Missing 'other' quantity for Indonesian plural $key", indonesianOther.isNotBlank())
            val englishPlaceholders = placeholderRegex.findAll(englishOther).map { it.value }.toSet()
            val indonesianPlaceholders = placeholderRegex.findAll(indonesianOther).map { it.value }.toSet()
            assertEquals(
                "Placeholder mismatch for plural key $key",
                englishPlaceholders,
                indonesianPlaceholders,
            )
        }
    }

    private fun resolveResDirectory(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate src/main/res from working directory ${File(".").absolutePath}")
    }

    private fun parseStringEntries(
        xmlFile: File,
    ): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val map = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            map[element.getAttribute("name")] = element.textContent
        }
        return map
    }

    private fun parsePluralEntries(
        xmlFile: File,
    ): Map<String, Map<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val pluralNodes = doc.getElementsByTagName("plurals")
        val map = linkedMapOf<String, Map<String, String>>()
        for (i in 0 until pluralNodes.length) {
            val pluralElement = pluralNodes.item(i) as Element
            val itemNodes = pluralElement.getElementsByTagName("item")
            val items = linkedMapOf<String, String>()
            for (j in 0 until itemNodes.length) {
                val itemElement = itemNodes.item(j) as Element
                items[itemElement.getAttribute("quantity")] = itemElement.textContent
            }
            map[pluralElement.getAttribute("name")] = items
        }
        return map
    }
}
