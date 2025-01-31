/*
 *  Copyright 2000-2022 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.slack

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import jetbrains.buildServer.notification.slackNotifier.concurrency.getAsync
import jetbrains.buildServer.serverSide.executors.ExecutorServices
import java.util.concurrent.TimeUnit

class CachingSlackWebApi(
        private val slackApi: SlackWebApi,
        private val executorServices: ExecutorServices,
        private val defaultTimeoutSeconds: Long = 300
) : SlackWebApi {
    private val authTestCache = createCache<String, AuthTestResult>()
    private val botsInfoCache = createCache<String, MaybeBot>()
    private val usersInfoCache = createCache<String, MaybeUser>()
    private val userIdentityCache = createCache<String, UserIdentity>()
    private val teamInfoCache = createCache<String, MaybeTeam>()

    private val readTimeoutMs = 5_000L

    /**
     * Posts new message every time, makes no sense to cache
     */
    override fun postMessage(token: String, payload: Message): MaybeMessage {
        return slackApi.postMessage(token, payload)
    }

    /**
     * All conversations should be cached, not only the ones got for the specified cursor
     * See [AggregatedSlackApi]
     */
    override fun conversationsList(token: String, cursor: String?, types: String): ChannelsList {
        return slackApi.conversationsList(token, cursor, types)
    }

    /**
     * All users should be cached, not only the ones got for the specified cursor
     * See [AggregatedSlackApi]
     */
    override fun usersList(token: String, cursor: String?): UsersList {
        return slackApi.usersList(token, cursor)
    }

    override fun authTest(token: String): AuthTestResult {
        return authTestCache.getAsync(token, readTimeoutMs) {
            slackApi.authTest(token)
        }
    }

    override fun botsInfo(token: String, botId: String): MaybeBot {
        return botsInfoCache.getAsync("$token;;$botId", readTimeoutMs) {
            slackApi.botsInfo(token, botId)
        }
    }

    override fun usersInfo(token: String, userId: String): MaybeUser {
        return usersInfoCache.getAsync("$token;;$userId", readTimeoutMs) {
            slackApi.usersInfo(token, userId)
        }
    }

    /**
     * All conversation members should be cached, not only the ones for the given cursor
     * See [AggregatedSlackApi]
     */
    override fun conversationsMembers(token: String, channelId: String, cursor: String?): ConversationMembers {
        return slackApi.conversationsMembers(token, channelId, cursor)
    }

    /**
     * OAuth access happens rarely for the same code, so it doesn't make sense to cache it
     */
    override fun oauthAccess(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUrl: String
    ): OauthAccessToken {
        return slackApi.oauthAccess(clientId, clientSecret, code, redirectUrl)
    }

    override fun usersIdentity(token: String): UserIdentity {
        return userIdentityCache.getAsync(token, readTimeoutMs) {
            slackApi.usersIdentity(token)
        }
    }

    override fun teamInfo(token: String, team: String): MaybeTeam {
        return teamInfoCache.getAsync("$token;;$team", readTimeoutMs) {
            slackApi.teamInfo(token, team)
        }
    }

    private fun <K, V> createCache(): AsyncCache<K, V> {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(defaultTimeoutSeconds, TimeUnit.SECONDS)
            .executor(executorServices.lowPriorityExecutorService)
            .buildAsync<K, V>()
    }
}