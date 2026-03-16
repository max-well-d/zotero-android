package org.zotero.android.translation

import java.security.MessageDigest

internal object BaiduSignUtil {
    fun generate(appId: String, query: String, salt: String, secretKey: String): String {
        val raw = "$appId$query$salt$secretKey"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
