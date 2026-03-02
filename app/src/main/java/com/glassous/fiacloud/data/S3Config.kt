package com.glassous.fiacloud.data

import java.util.UUID

data class S3Config(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val bucketName: String = "",
    val region: String = "us-east-1"
)
