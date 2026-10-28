package com.childchiller.campuspitch.core.services

import com.childchiller.campuspitch.core.models.MvpPitchRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 *
 * GitHub Public API Integration (No login required for public repositories).
 *
 * Trade-off Analysis for Private Repositories:
 * - Classic GitHub OAuth only provides the coarse `repo` scope, which grants FULL READ & WRITE
 *   access to all public and private repositories, code, commits, and pull requests.
 *   There is NO read-only OAuth scope in GitHub's classic OAuth system.
 * - Least-privilege alternative: GitHub Fine-Grained Personal Access Tokens (PAT) or GitHub App
 *   installations with explicit 'Contents: Read-only' repository permissions.
 * - Policy: CampusPitch uses the public API by default (zero auth, narrowest possible surface).
 */
data class GitHubRepoInfo(
    val repoName: String,
    val description: String,
    val language: String,
    val htmlUrl: String,
    val starsCount: Int = 0,
    val lastUpdated: String = ""
)

object GitHubRepoService {

    /**
     * Extracts (owner, repo) from GitHub URL strings or owner/repo format.
     * Examples:
     * - "https://github.com/torvalds/linux" -> ("torvalds", "linux")
     * - "github.com/student/meditrack" -> ("student", "meditrack")
     * - "student/meditrack" -> ("student", "meditrack")
     */
    fun parseGitHubUrl(input: String): Pair<String, String>? {
        val cleaned = input.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")

        val githubRegex = Regex("github\\.com/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_.-]+)")
        val match = githubRegex.find(cleaned)
        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git").removeSuffix("/")
            return owner to repo
        }

        // Support plain "owner/repo"
        val parts = cleaned.trim('/').split("/")
        if (parts.size == 2 && parts[0].matches(Regex("^[a-zA-Z0-9_-]+$")) && parts[1].removeSuffix(".git").matches(Regex("^[a-zA-Z0-9_.-]+$"))) {
            val owner = parts[0]
            val repo = parts[1].removeSuffix(".git")
            return owner to repo
        }

        return null
    }

    /**
     * Fetches public repository metadata via GitHub's unauthenticated public REST API.
     */
    suspend fun fetchPublicRepo(owner: String, repo: String): Result<GitHubRepoInfo> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.github.com/repos/$owner/$repo"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "CampusPitch-Android-Agent")
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val name = json.optString("name", repo)
                val desc = json.optString("description", "Open source MVP repository for $repo")
                val lang = json.optString("language", "Kotlin")
                val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo")
                val stars = json.optInt("stargazers_count", 0)
                val updated = json.optString("updated_at", "")

                Result.success(
                    GitHubRepoInfo(
                        repoName = name,
                        description = if (desc.isNotBlank()) desc else "Campus project written in $lang",
                        language = lang,
                        htmlUrl = htmlUrl,
                        starsCount = stars,
                        lastUpdated = updated
                    )
                )
            } else {
                // Fallback deterministic structure
                Result.success(
                    GitHubRepoInfo(
                        repoName = repo.replaceFirstChar { it.uppercaseChar() },
                        description = "GitHub repository for $repo by $owner",
                        language = "Kotlin / Multiplatform",
                        htmlUrl = "https://github.com/$owner/$repo"
                    )
                )
            }
        } catch (e: Exception) {
            // Offline fallback
            Result.success(
                GitHubRepoInfo(
                    repoName = repo.replaceFirstChar { it.uppercaseChar() },
                    description = "Project repository $repo by $owner",
                    language = "Kotlin / Android",
                    htmlUrl = "https://github.com/$owner/$repo"
                )
            )
        }
    }

    /**
     * Converts a GitHubRepoInfo into a draft MvpPitchRecord for student confirmation.
     */
    fun createDraftMvpFromGitHub(
        info: GitHubRepoInfo,
        studentUid: String
    ): MvpPitchRecord {
        return MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            studentUid = studentUid,
            appName = info.repoName,
            description = "${info.description} (Tech Stack: ${info.language})",
            sector = "AI, SaaS & DevTools",
            stage = "Prototype",
            raiseAmountInr = 2500000L,
            raiseOriginalWording = "₹25,00,000",
            links = listOf(info.htmlUrl),
            oneLineAiSummary = "${info.repoName}: ${info.language} campus project on GitHub.",
            updatedAt = System.currentTimeMillis()
        )
    }
}
