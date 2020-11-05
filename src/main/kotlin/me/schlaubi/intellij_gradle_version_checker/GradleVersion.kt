/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Rittmeister
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.schlaubi.intellij_gradle_version_checker

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

lateinit var latestGradleVersion: GithubGradleVersion
        private set
// https://regex101.com/r/oVpaYd/1/
private val gradleVersionPattern = """((?:[0-9])+)\.((?:[0-9])+)(?:\.((?:[0-9])+))?""".toRegex()
private const val latestGradleVersionEndpoint = "https://api.github.com/repos/gradle/gradle/releases/latest"
private val httpClient = HttpClient {
    install(JsonFeature) {
        val json  = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        serializer = KotlinxSerializer(json)
    }
}

@Serializable(with = GradleVersionSerializer::class)
data class GradleVersion(val major: Int, val minor: Int, val revision: Int?) : Comparable<GradleVersion> {
    override fun compareTo(other: GradleVersion): Int {
        return when {
            major != other.major -> if(other.major < major) MAJOR else TOO_NEW
            minor != other.minor -> if(other.minor < minor) MINOR else TOO_NEW
            revision != other.revision -> if((revision ?: 0) > (other.revision ?: 0)) REVISION else TOO_NEW
            else -> 0
        }
    }

    companion object {
        const val REVISION = -1
        const val MINOR = -2
        const val MAJOR = -3
        const val TOO_NEW = 1
    }

    override fun toString(): String = "$major.$minor${if (revision != null) ".$revision" else ""}"
}

suspend fun fetchGradleVersion() {
    latestGradleVersion = httpClient.get(latestGradleVersionEndpoint)
}

@Serializable
data class GithubGradleVersion(
    val url: String,
    @SerialName("assets_url")
    val assetsUrl: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val id: Int,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("target_commitish")
    val targetCommitish: String,
    @SerialName("name")
    val gradleVersion: GradleVersion,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String,
    val body: String
)

internal class GradleVersionSerializer : KSerializer<GradleVersion> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("version", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): GradleVersion = decoder.decodeString().parseGradleVersion() ?: error("Invalid version format")

    override fun serialize(encoder: Encoder, value: GradleVersion) = encoder.encodeString(value.toString())

}

fun String.parseGradleVersion(): GradleVersion? {
    val match = gradleVersionPattern.matchEntire(this) ?: return null
    val (_, major, minor) = match.groupValues
    val revision = match.groupValues[3]

    return GradleVersion(major.toInt(), minor.toInt(), if(revision.isBlank()) null else revision.toInt())
}
