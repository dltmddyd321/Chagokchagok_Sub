package com.windrr.hyundaesub.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiClientTest {

    @Test
    fun baseUrl_addsTrailingSlashOnlyWhenMissing() {
        assertEquals("https://chagokchagok-api.duckdns.org/", ApiClient.baseUrl("https://chagokchagok-api.duckdns.org"))
        assertEquals("https://chagokchagok-api.duckdns.org/", ApiClient.baseUrl("https://chagokchagok-api.duckdns.org/"))
        assertEquals("https://chagokchagok-api.duckdns.org/", ApiClient.baseUrl("  https://chagokchagok-api.duckdns.org "))
    }
}
