package com.resonote.core.network.risk

import com.resonote.core.network.session.ApiSession
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** Creates the SID/EDT pair used by the fixed PC risk protocol for header-only challenges. */
@Singleton
internal class ApiRiskContextFactory @Inject constructor(
    private val clock: Clock,
) {
    private val random = SecureRandom()
    private val webGlHash: String by lazy { BigInteger(64, random).toString() }

    fun complete(challenge: ApiRiskChallenge, session: ApiSession): ApiRiskChallenge {
        if (!challenge.sid.isNullOrBlank() && !challenge.edt.isNullOrBlank()) return challenge
        val key = md5(randomString(16)).take(16)
        val plaintext =
            "mid=${session.mid};userid=${session.userId ?: 0};dfid=${session.dfid ?: 0};" +
                "webgl=$webGlHash;webdriver=0;ts=${clock.millis()};data=${eventData()}"
        val edt =
            Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.encodeToByteArray(), "AES"), IvParameterSpec(RISK_IV.encodeToByteArray()))
                Base64.getEncoder().encodeToString(doFinal(plaintext.encodeToByteArray()))
            }
        val sid =
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run {
                init(
                    Cipher.ENCRYPT_MODE,
                    publicKey,
                    OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
                    random,
                )
                Base64.getEncoder().encodeToString(doFinal(key.encodeToByteArray()))
            }
        return challenge.copy(sid = sid, edt = edt)
    }

    private fun eventData(): String {
        val sentinel = 0xffffffffL - random.nextInt(20)
        val entries = mutableListOf("5,0,0", "5,$sentinel,0", "5,0,0", "5,$sentinel,0")
        var timestamp = randomInt(5, 20)
        var eventIndex = 0
        entries += "6,$timestamp,$eventIndex,750,500"
        entries += "6,$sentinel,$eventIndex,750,500"
        eventIndex += 1
        repeat(3) {
            timestamp += randomInt(80, 600)
            entries += "5,$timestamp,$eventIndex"
            entries += "5,$sentinel,$eventIndex"
            eventIndex += 1
        }
        val startX = randomInt(200, 600)
        val startY = randomInt(200, 500)
        val endX = randomInt(500, 700)
        val endY = randomInt(80, 150)
        val points = randomInt(30, 60)
        val control1X = startX + (endX - startX) * .3 + randomInt(-80, 80)
        val control1Y = startY + (endY - startY) * .2 + randomInt(-60, 60)
        val control2X = startX + (endX - startX) * .7 + randomInt(-60, 60)
        val control2Y = startY + (endY - startY) * .8 + randomInt(-40, 40)
        var subIndex = 0
        for (index in 0..points) {
            val t = index.toDouble() / points
            val u = 1 - t
            val jitter = max(.5, 3 - t * 2.5)
            val x =
                (u * u * u * startX + 3 * u * u * t * control1X + 3 * u * t * t * control2X + t * t * t * endX +
                    (random.nextDouble() - .5) * jitter).roundToInt()
            val y =
                (u * u * u * startY + 3 * u * u * t * control1Y + 3 * u * t * t * control2Y + t * t * t * endY +
                    (random.nextDouble() - .5) * jitter).roundToInt()
            timestamp += randomInt(8, 50)
            entries += "3,$timestamp,$subIndex,$x,$y"
            entries += "3,$sentinel,$subIndex,$x,$y"
            if (index > 0 && index % 12 == 0) {
                timestamp += randomInt(20, 60)
                entries += "5,$timestamp,$eventIndex"
                entries += "5,$sentinel,$eventIndex"
                eventIndex += 1
            }
            subIndex = (subIndex + 1) % 2
        }
        timestamp += randomInt(5, 30)
        entries += "3,$timestamp,1,${endX + randomInt(-5, 5)},${endY + randomInt(-5, 5)}"
        entries += "3,$sentinel,1,$endX,$endY"
        return entries.joinToString(":")
    }

    private fun randomString(length: Int): String =
        buildString(length) { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    private fun randomInt(min: Int, max: Int): Int = min + random.nextInt(max - min + 1)

    private fun md5(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.encodeToByteArray()).joinToString("") { "%02x".format(it) }

    private val publicKey by lazy {
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(RISK_PUBLIC_KEY)))
    }

    private companion object {
        const val RISK_IV = "kugousecurity123"
        const val ALPHABET = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val RISK_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoW2+Ylo8ALePSQTP0xBFlFmEOHvBD9tS+s7DBlfKEu3RzzvZTaX1JtYbX4+AVUqj6ARz8IM+CKByqGFvbHN/W64XxNI+q7z36ajCL3VTJ2W5G9MCJitc6oGbire4NQfhaEq0nC+hxBWQvCbIFflA2ItrLUbSU7z1bHA/a+jlQm4OWvY+IKnTryOJTPuT1yNOVjbJ8wBLKy2DgQr9pPqWPmEQtGpR5IM9V8Kao6PaSdKYOWGbX3i2+RzIKhvZUxxtJwdVbqPlDPlW9h4/xIBc56Lgvr4aIl8nFtwbj4UJVUTFuGrs0tY9H/tXvZ22dUCKuGxW/gW7ZF+gXz6vHtYarQIDAQAB"
    }
}
