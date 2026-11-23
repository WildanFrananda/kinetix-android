package com.kinetix.shop.architecture

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class PrivacyManifestTest {
    @Test
    fun manifestRemovesAdvertisingIdPermissionsAndDisablesAdIdCollection() {
        val manifestFile = resolveManifestFile()
        assertTrue("Missing AndroidManifest.xml", manifestFile.exists())

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val doc = factory.newDocumentBuilder().parse(manifestFile)

        val removedPermissions = mutableSetOf<String>()
        val permissionNodes = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val element = permissionNodes.item(i) as Element
            val name = element.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
            val toolsNode = element.getAttributeNS("http://schemas.android.com/tools", "node")
            if (toolsNode == "remove") {
                removedPermissions.add(name)
            }
        }

        val expectedRemoved = setOf(
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
        )
        assertTrue(
            "Expected manifest to remove $expectedRemoved, found $removedPermissions",
            removedPermissions.containsAll(expectedRemoved),
        )

        val metaData = mutableMapOf<String, String>()
        val metaNodes = doc.getElementsByTagName("meta-data")
        for (i in 0 until metaNodes.length) {
            val element = metaNodes.item(i) as Element
            val name = element.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
            val value = element.getAttributeNS("http://schemas.android.com/apk/res/android", "value")
            metaData[name] = value
        }

        assertEquals(
            "false",
            metaData["google_analytics_adid_collection_enabled"],
        )
    }

    private fun resolveManifestFile(): File {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate AndroidManifest.xml")
    }
}
