package com.builder.data.crypto

import android.util.Base64
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts secrets for GitHub Actions using libsodium's sealed box format.
 *
 * GitHub requires secrets to be encrypted with the repository's public key
 * using the crypto_box_seal algorithm (X25519 + XSalsa20-Poly1305).
 *
 * Sealed box format:
 * 1. Generate ephemeral X25519 keypair
 * 2. Compute nonce = Blake2b(ephemeral_public || recipient_public)[0:24]
 * 3. Compute shared secret = X25519(ephemeral_private, recipient_public)
 * 4. Derive encryption key = HSalsa20(shared_secret, zero_nonce)
 * 5. Encrypt with XSalsa20-Poly1305
 * 6. Output = ephemeral_public (32 bytes) || ciphertext (message + 16 byte tag)
 */
@Singleton
class GitHubSecretEncryptor @Inject constructor() {

    companion object {
        private const val KEY_SIZE = 32
        private const val NONCE_SIZE = 24
        private const val TAG_SIZE = 16
        private val ZERO_NONCE = ByteArray(16)
        private val SIGMA = "expand 32-byte k".toByteArray(Charsets.US_ASCII)
    }

    /**
     * Encrypts a secret value using the repository's public key.
     *
     * @param publicKeyBase64 The repository's public key (Base64 encoded)
     * @param secretValue The secret value to encrypt
     * @return Base64 encoded sealed box ciphertext
     */
    fun encryptSecret(publicKeyBase64: String, secretValue: String): String {
        val recipientPublicKey = Base64.decode(publicKeyBase64, Base64.DEFAULT)

        require(recipientPublicKey.size == KEY_SIZE) {
            "Invalid public key size: ${recipientPublicKey.size}, expected $KEY_SIZE"
        }

        // 1. Generate ephemeral X25519 keypair
        val keyPairGenerator = X25519KeyPairGenerator()
        keyPairGenerator.init(X25519KeyGenerationParameters(SecureRandom()))
        val ephemeralKeyPair = keyPairGenerator.generateKeyPair()

        val ephemeralPublic = (ephemeralKeyPair.public as X25519PublicKeyParameters).encoded
        val ephemeralPrivate = ephemeralKeyPair.private as X25519PrivateKeyParameters

        // 2. Compute nonce = Blake2b(ephemeral_public || recipient_public)[0:24]
        val nonce = computeSealedBoxNonce(ephemeralPublic, recipientPublicKey)

        // 3. Compute shared secret using X25519
        val sharedSecret = ByteArray(KEY_SIZE)
        val agreement = X25519Agreement()
        agreement.init(ephemeralPrivate)
        agreement.calculateAgreement(
            X25519PublicKeyParameters(recipientPublicKey, 0),
            sharedSecret,
            0
        )

        // 4. Derive encryption key using HSalsa20
        val encryptionKey = hsalsa20(sharedSecret, ZERO_NONCE)

        // 5. Encrypt with XSalsa20-Poly1305
        val plaintext = secretValue.toByteArray(Charsets.UTF_8)
        val ciphertext = xsalsa20Poly1305Encrypt(encryptionKey, nonce, plaintext)

        // 6. Output = ephemeral_public || ciphertext
        val sealedBox = ByteArray(KEY_SIZE + ciphertext.size)
        System.arraycopy(ephemeralPublic, 0, sealedBox, 0, KEY_SIZE)
        System.arraycopy(ciphertext, 0, sealedBox, KEY_SIZE, ciphertext.size)

        Timber.d("Encrypted secret: ${plaintext.size} bytes -> ${sealedBox.size} bytes sealed box")

        return Base64.encodeToString(sealedBox, Base64.NO_WRAP)
    }

    /**
     * Computes the nonce for sealed box as Blake2b(ephemeral_public || recipient_public)[0:24]
     */
    private fun computeSealedBoxNonce(ephemeralPublic: ByteArray, recipientPublic: ByteArray): ByteArray {
        val digest = Blake2bDigest(NONCE_SIZE * 8) // 192 bits = 24 bytes
        digest.update(ephemeralPublic, 0, ephemeralPublic.size)
        digest.update(recipientPublic, 0, recipientPublic.size)

        val nonce = ByteArray(NONCE_SIZE)
        digest.doFinal(nonce, 0)
        return nonce
    }

    /**
     * HSalsa20 core function - derives a 32-byte key from shared secret.
     * This is the key derivation step used in crypto_box.
     */
    private fun hsalsa20(key: ByteArray, nonce: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be 32 bytes" }
        require(nonce.size == 16) { "Nonce must be 16 bytes" }

        // Initialize state with key, nonce, and sigma constant
        val state = IntArray(16)

        // sigma[0..3]
        state[0] = littleEndianToInt(SIGMA, 0)
        state[5] = littleEndianToInt(SIGMA, 4)
        state[10] = littleEndianToInt(SIGMA, 8)
        state[15] = littleEndianToInt(SIGMA, 12)

        // key[0..31]
        state[1] = littleEndianToInt(key, 0)
        state[2] = littleEndianToInt(key, 4)
        state[3] = littleEndianToInt(key, 8)
        state[4] = littleEndianToInt(key, 12)
        state[11] = littleEndianToInt(key, 16)
        state[12] = littleEndianToInt(key, 20)
        state[13] = littleEndianToInt(key, 24)
        state[14] = littleEndianToInt(key, 28)

        // nonce[0..15]
        state[6] = littleEndianToInt(nonce, 0)
        state[7] = littleEndianToInt(nonce, 4)
        state[8] = littleEndianToInt(nonce, 8)
        state[9] = littleEndianToInt(nonce, 12)

        // 20 rounds (10 double rounds)
        for (i in 0 until 10) {
            // Column round
            quarterRound(state, 0, 4, 8, 12)
            quarterRound(state, 5, 9, 13, 1)
            quarterRound(state, 10, 14, 2, 6)
            quarterRound(state, 15, 3, 7, 11)

            // Diagonal round
            quarterRound(state, 0, 1, 2, 3)
            quarterRound(state, 5, 6, 7, 4)
            quarterRound(state, 10, 11, 8, 9)
            quarterRound(state, 15, 12, 13, 14)
        }

        // Extract the derived key from specific positions
        val derivedKey = ByteArray(KEY_SIZE)
        intToLittleEndian(state[0], derivedKey, 0)
        intToLittleEndian(state[5], derivedKey, 4)
        intToLittleEndian(state[10], derivedKey, 8)
        intToLittleEndian(state[15], derivedKey, 12)
        intToLittleEndian(state[6], derivedKey, 16)
        intToLittleEndian(state[7], derivedKey, 20)
        intToLittleEndian(state[8], derivedKey, 24)
        intToLittleEndian(state[9], derivedKey, 28)

        return derivedKey
    }

    /**
     * XSalsa20-Poly1305 authenticated encryption.
     * Returns ciphertext || 16-byte Poly1305 tag
     */
    private fun xsalsa20Poly1305Encrypt(key: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be 32 bytes" }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 24 bytes" }

        // XSalsa20: first 16 bytes of nonce go through HSalsa20, last 8 bytes are the stream nonce
        val subKey = hsalsa20(key, nonce.copyOfRange(0, 16))

        // The remaining 8 bytes of nonce, padded with zeros
        val streamNonce = ByteArray(8)
        System.arraycopy(nonce, 16, streamNonce, 0, 8)

        // Generate Poly1305 key (first 32 bytes of XSalsa20 stream)
        val poly1305Key = ByteArray(KEY_SIZE)
        val salsa = Salsa20Engine()
        salsa.init(true, ParametersWithIV(KeyParameter(subKey), streamNonce))
        salsa.processBytes(ByteArray(KEY_SIZE), 0, KEY_SIZE, poly1305Key, 0)

        // Encrypt plaintext with XSalsa20 (skip first 32 bytes used for Poly1305 key)
        val ciphertext = ByteArray(plaintext.size)
        // Reset and skip 64 bytes (we need to skip the first block)
        salsa.init(true, ParametersWithIV(KeyParameter(subKey), streamNonce))
        val skipBlock = ByteArray(64)
        salsa.processBytes(skipBlock, 0, 64, skipBlock, 0)
        salsa.processBytes(plaintext, 0, plaintext.size, ciphertext, 0)

        // Compute Poly1305 tag over ciphertext
        val poly1305 = Poly1305()
        poly1305.init(KeyParameter(poly1305Key))
        poly1305.update(ciphertext, 0, ciphertext.size)
        val tag = ByteArray(TAG_SIZE)
        poly1305.doFinal(tag, 0)

        // Return tag || ciphertext (NaCl secretbox format)
        val result = ByteArray(TAG_SIZE + ciphertext.size)
        System.arraycopy(tag, 0, result, 0, TAG_SIZE)
        System.arraycopy(ciphertext, 0, result, TAG_SIZE, ciphertext.size)

        return result
    }

    private fun quarterRound(state: IntArray, a: Int, b: Int, c: Int, d: Int) {
        state[b] = state[b] xor rotateLeft(state[a] + state[d], 7)
        state[c] = state[c] xor rotateLeft(state[b] + state[a], 9)
        state[d] = state[d] xor rotateLeft(state[c] + state[b], 13)
        state[a] = state[a] xor rotateLeft(state[d] + state[c], 18)
    }

    private fun rotateLeft(value: Int, bits: Int): Int {
        return (value shl bits) or (value ushr (32 - bits))
    }

    private fun littleEndianToInt(bs: ByteArray, off: Int): Int {
        return (bs[off].toInt() and 0xff) or
                ((bs[off + 1].toInt() and 0xff) shl 8) or
                ((bs[off + 2].toInt() and 0xff) shl 16) or
                ((bs[off + 3].toInt() and 0xff) shl 24)
    }

    private fun intToLittleEndian(n: Int, bs: ByteArray, off: Int) {
        bs[off] = n.toByte()
        bs[off + 1] = (n ushr 8).toByte()
        bs[off + 2] = (n ushr 16).toByte()
        bs[off + 3] = (n ushr 24).toByte()
    }
}
