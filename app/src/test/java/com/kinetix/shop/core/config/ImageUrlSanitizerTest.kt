package com.kinetix.shop.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlSanitizerTest {
    @Test
    fun acceptsValidHttpsUrlsAndRejectsBlankHttpOrMalformedUrls() {
        assertEquals(
            "https://cdn.kinetix.test/images/mouse.png",
            ImageUrlSanitizer.sanitizeHttpsUrl("  https://cdn.kinetix.test/images/mouse.png  "),
        )
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl(null))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl(""))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl("   "))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl("http://cdn.kinetix.test/mouse.png"))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl("file:///etc/passwd"))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl("https:///no-host.png"))
        assertNull(ImageUrlSanitizer.sanitizeHttpsUrl("https://bad host/image.png"))
    }
}
