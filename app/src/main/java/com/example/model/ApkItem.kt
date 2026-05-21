package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApkItem(
    val name: String,
    val developer: String,
    val version: String,
    val size: String,
    val rating: String,
    val icon: String,
    val banner: String,
    val screenshots: List<String>?,
    val download: String,
    val description: String,
    val category: String
) {
    // Helper to sanitize download URLs, ensuring no trailing whitespaces or line-breaks
    val cleanedDownloadUrl: String
        get() = download.trim()
}
