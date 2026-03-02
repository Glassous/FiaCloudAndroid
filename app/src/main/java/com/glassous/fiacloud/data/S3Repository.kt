package com.glassous.fiacloud.data

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import aws.smithy.kotlin.runtime.collections.Attributes

object S3Repository {
    private var s3Client: S3Client? = null

    suspend fun updateConfig(config: S3Config) {
        try {
            s3Client?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            s3Client = S3Client {
                this.region = if (config.region.isBlank()) "us-east-1" else config.region
                credentialsProvider = object : CredentialsProvider {
                    override suspend fun resolve(attributes: Attributes): Credentials {
                        return Credentials(config.accessKey, config.secretKey)
                    }
                }
                if (config.endpoint.isNotBlank()) {
                    val fullEndpoint = if (!config.endpoint.startsWith("http://") && !config.endpoint.startsWith("https://")) {
                        "https://${config.endpoint}"
                    } else {
                        config.endpoint
                    }
                    this.endpointUrl = Url.parse(fullEndpoint)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun listObjects(bucketName: String): List<String> = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            val request = ListObjectsV2Request {
                bucket = bucketName
            }
            val response = client.listObjectsV2(request)
            response.contents?.mapNotNull { it.key } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
