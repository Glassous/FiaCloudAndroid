package com.glassous.fiacloud.data

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString

object S3Repository {
    private var s3Client: S3Client? = null

    data class S3Object(
        val key: String,
        val isFolder: Boolean,
        val displayName: String
    )

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

    suspend fun listObjects(bucketName: String, prefix: String = "", delimiter: String = "/"): List<S3Object> = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            val request = ListObjectsV2Request {
                bucket = bucketName
                this.prefix = if (prefix.isEmpty()) null else prefix
                this.delimiter = delimiter
            }
            val response = client.listObjectsV2(request)
            
            val folders = response.commonPrefixes?.mapNotNull { commonPrefix ->
                commonPrefix.prefix?.let { p ->
                    S3Object(
                        key = p,
                        isFolder = true,
                        displayName = p.removeSuffix("/").substringAfterLast("/")
                    )
                }
            } ?: emptyList()

            val files = response.contents?.mapNotNull { content ->
                content.key?.let { k ->
                    if (k == prefix) return@let null // 排除当前目录
                    S3Object(
                        key = k,
                        isFolder = false,
                        displayName = k.substringAfterLast("/")
                    )
                }
            } ?: emptyList()

            folders + files
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getObjectContent(bucketName: String, key: String): String = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            val request = GetObjectRequest {
                bucket = bucketName
                this.key = key
            }
            client.getObject(request) { response ->
                response.body?.decodeToString() ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun putObjectContent(bucketName: String, key: String, content: String) = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            val request = PutObjectRequest {
                bucket = bucketName
                this.key = key
                body = ByteStream.fromString(content)
            }
            client.putObject(request)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun deleteObject(bucketName: String, key: String, isFolder: Boolean) = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            if (isFolder) {
                // 列出该目录下所有对象并删除
                var continuationToken: String? = null
                do {
                    val listRequest = ListObjectsV2Request {
                        bucket = bucketName
                        prefix = key
                        this.continuationToken = continuationToken
                    }
                    val listResponse = client.listObjectsV2(listRequest)
                    val objectsToDelete = listResponse.contents?.mapNotNull { it.key } ?: emptyList()
                    
                    if (objectsToDelete.isNotEmpty()) {
                        val deleteRequest = DeleteObjectsRequest {
                            bucket = bucketName
                            delete {
                                objects = objectsToDelete.map { ObjectIdentifier { this.key = it } }
                            }
                        }
                        client.deleteObjects(deleteRequest)
                    }
                    continuationToken = listResponse.nextContinuationToken
                } while (listResponse.isTruncated == true)
            } else {
                val request = DeleteObjectRequest {
                    bucket = bucketName
                    this.key = key
                }
                client.deleteObject(request)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun renameObject(bucketName: String, oldKey: String, newKey: String, isFolder: Boolean) = withContext(Dispatchers.IO) {
        val client = s3Client ?: throw IllegalStateException("S3 客户端未配置，请前往设置页面进行配置。")
        try {
            if (isFolder) {
                // 列出所有子项，依次复制并删除
                var continuationToken: String? = null
                do {
                    val listRequest = ListObjectsV2Request {
                        bucket = bucketName
                        prefix = oldKey
                        this.continuationToken = continuationToken
                    }
                    val listResponse = client.listObjectsV2(listRequest)
                    listResponse.contents?.forEach { obj ->
                        val k = obj.key ?: return@forEach
                        val targetKey = k.replaceFirst(oldKey, newKey)
                        
                        // 复制
                        val copyRequest = CopyObjectRequest {
                            bucket = bucketName
                            copySource = "$bucketName/$k"
                            key = targetKey
                        }
                        client.copyObject(copyRequest)
                        
                        // 删除旧的
                        val deleteRequest = DeleteObjectRequest {
                            bucket = bucketName
                            key = k
                        }
                        client.deleteObject(deleteRequest)
                    }
                    continuationToken = listResponse.nextContinuationToken
                } while (listResponse.isTruncated == true)
            } else {
                // 复制
                val copyRequest = CopyObjectRequest {
                    bucket = bucketName
                    copySource = "$bucketName/$oldKey"
                    key = newKey
                }
                client.copyObject(copyRequest)
                
                // 删除旧的
                val deleteRequest = DeleteObjectRequest {
                    bucket = bucketName
                    key = oldKey
                }
                client.deleteObject(deleteRequest)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
