package com.builder.data.remote.github

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PKCE (Proof Key for Code Exchange) utilities for OAuth Authorization Code Flow.
 *
 * Implements RFC 7636: https://tools.ietf.org/html/rfc7636
 */
object PKCEUtils {

    /**
     * Generates a cryptographically random code verifier.
     *
     * @return A URL-safe base64-encoded random string (43-128 characters)
     */
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32) // 32 bytes = 43 characters when base64 encoded
        secureRandom.nextBytes(bytes)

        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Generates a code challenge from the code verifier using SHA-256.
     *
     * @param codeVerifier The code verifier to hash
     * @return A URL-safe base64-encoded SHA-256 hash of the code verifier
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)

        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Data class to hold PKCE parameters.
     */
    data class PKCEParams(
        val codeVerifier: String,
        val codeChallenge: String
    )

    /**
     * Generates both code verifier and code challenge.
     *
     * @return PKCEParams containing both verifier and challenge
     */
    fun generatePKCEParams(): PKCEParams {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        return PKCEParams(verifier, challenge)
    }
}
