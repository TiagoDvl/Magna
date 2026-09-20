package com.tick.magna.util

/**
 * The same profile, addressed to the app instead of to the web.
 *
 * Every social link the register holds is an `https://` one, so tapping a chip handed the
 * profile to a browser even with the app installed and signed in. The apps all answer a scheme
 * of their own, and a scheme nothing is registered for fails loudly — which is what makes the
 * fallback to the web link reliable rather than a guess about what is installed.
 *
 * Measured over forty deputados, the register holds exactly four hosts: facebook.com 22 times,
 * twitter.com 21, instagram.com 21 and youtube.com 14. No `x.com` at all, which is why the
 * old host is the one matched rather than the new one.
 *
 * Returns null when there is nothing better than the web link, and the caller opens the URL it
 * already had.
 */
internal fun appDeRedeSocial(url: String): String? {
    val semEsquema = url.substringAfter("://", url)
    val host = semEsquema.substringBefore('/').lowercase().removePrefix("www.")
    val caminho = semEsquema.substringAfter('/', "").substringBefore('?').trim('/')
    val primeiro = caminho.substringBefore('/')

    return when (host) {
        "instagram.com" -> perfil(primeiro, INSTAGRAM_RESERVADO)?.let { "instagram://user?username=$it" }

        "twitter.com", "x.com" ->
            perfil(primeiro, TWITTER_RESERVADO)?.let { "twitter://user?screen_name=$it" }

        // Facebook has no scheme that takes a username: `fb://page` wants the numeric id, which
        // the register does not carry. This one hands the app the web address and lets it
        // resolve the name itself.
        "facebook.com" -> if (caminho.isEmpty()) null else "fb://facewebmodal/f?href=" + url.percentEncoded()

        // YouTube already claims its own https links, and its scheme addresses videos rather
        // than the channels the register points at.
        else -> null
    }
}

/**
 * The first path segment, when it is a profile rather than one of the site's own sections.
 *
 * `instagram.com/p/xyz` is a post and `twitter.com/i/status/…` is a tweet; handing either to
 * `?username=` opens the app on a profile that does not exist.
 */
private fun perfil(segmento: String, reservados: Set<String>): String? {
    val nome = segmento.trim()

    return nome.takeIf { it.isNotEmpty() && it.lowercase() !in reservados }
}

private val INSTAGRAM_RESERVADO = setOf("p", "reel", "reels", "explore", "stories", "tv", "accounts")

private val TWITTER_RESERVADO = setOf("i", "intent", "home", "search", "hashtag", "share", "status")
