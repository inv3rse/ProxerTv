package com.example.dennis.proxertv.base

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.mozilla.javascript.Context

/**
 * Interceptor that tries to handle the CloudFlare js challenge
 */
class CloudFlareInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response? {
        // always set the user agent
        val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()

        val response = chain.proceed(request)

        if (response.code() == 503 && "cloudflare-nginx" == response.header("Server")) {

            val body = response.body().string()

            val operationPattern = Regex("setTimeout\\(function\\(\\)\\{\\s+(var s,t,o,p,b,r,e,a,k,i,n,g,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n")
            val passPattern = Regex("name=\"pass\" value=\"(.+?)\"")
            val challengePattern = Regex("name=\"jschl_vc\" value=\"(\\w+)\"")

            val rawOperation = operationPattern.find(body)?.groupValues?.get(1)
            val challenge = challengePattern.find(body)?.groupValues?.get(1)
            val challengePass = passPattern.find(body)?.groupValues?.get(1)

            if (rawOperation != null && challenge != null && challengePass != null) {
                val js = rawOperation.replace(Regex("a\\.value = (parseInt\\(.+?\\)).+"), "$1")
                        .replace(Regex("\\s{3,}[a-z](?: = |\\.).+"), "")
                        .replace("\n", "")

                // init js engine
                val rhino = Context.enter()
                rhino.optimizationLevel = -1
                val scope = rhino.initStandardObjects()

                val result = (rhino.evaluateString(scope, js, "CloudFlare JS Challenge", 1, null) as Double).toInt()
                val requestUrl = response.request().url()
                val answer = (result + requestUrl.host().length).toString()

                val url = HttpUrl.Builder()
                        .scheme(requestUrl.scheme())
                        .host(requestUrl.host())
                        .addPathSegment("/cdn-cgi/l/chk_jschl")
                        .addQueryParameter("jschl_vc", challenge)
                        .addQueryParameter("pass", challengePass)
                        .addQueryParameter("jschl_answer", answer)
                        .build()

                val challengeRequest = Request.Builder().get()
                        .url(url)
                        .header("Referer", requestUrl.toString())
                        .header("User-Agent", USER_AGENT)
                        .build()

                // javascript waits 4 seconds until it proceeds
                Thread.sleep(4500)
                return chain.proceed(challengeRequest)
            }
        }

        return response
    }

    companion object{
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0"
    }
}