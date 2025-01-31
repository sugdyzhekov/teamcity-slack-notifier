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

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class ChoosingMessageBuilderFactory(
        private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
        private val verboseMessageBuilderFactory: VerboseMessageBuilderFactory
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        val messageFormat = user.getPropertyValue(SlackProperties.messageFormatProperty)
        if (messageFormat == "verbose") {
            return verboseMessageBuilderFactory.get(user)
        }

        return simpleMessageBuilderFactory.get(user)
    }
}