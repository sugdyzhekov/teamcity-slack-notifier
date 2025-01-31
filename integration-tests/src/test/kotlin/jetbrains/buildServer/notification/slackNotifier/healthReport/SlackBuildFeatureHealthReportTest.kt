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

package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.FeatureProviderNotificationRulesHolder
import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.serverSide.impl.NotificationsBuildFeature
import jetbrains.buildServer.util.TestFor
import org.testng.annotations.Test

class SlackBuildFeatureHealthReportTest : BaseSlackHealthReportTest<SlackBuildFeatureHealthReport>() {
    override fun getReport(): SlackBuildFeatureHealthReport {
        return SlackBuildFeatureHealthReport(
            myDescriptor,
            myConnectionManager,
            mySlackApiFactory,
            AggregatedSlackApi(mySlackApiFactory, myFixture.executorServices),
            myFixture.executorServices
        )
    }

    @Test
    fun `test should report missing connection`() {
        `given build type is in scope`() And
                `given build feature is missing connection`()
        `when health is reported`()
        `then report should contain error about missing connection`()
    }

    @Test
    fun `test should report invalid connection`() {
        `given build type is in scope`() And
                `given build feature has invalid connection`()
        `when health is reported`()
        `then report should contain error about invalid connection`()
    }

    @Test
    fun `test should report invalid channel`() {
        `given build type is in scope`() And
                `given build feature has invalid channel`()
        `when health is reported`()
        `then report should contain error about invalid channel`()
    }

    @Test
    fun `test should report channel that bot is not added to`() {
        `given build type is in scope`() And
                `given build feature has channel that bot is not added to`()
        `when health is reported`()
        `then report should contain error about not being added to channel`()
    }

    @Test
    fun `test should not report error if build feature is configured correctly`() {
        `given build type is in scope`() And
                `given build features is configured correctly`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test
    @TestFor(issues = ["TW-67015"])
    fun `test should not report error if there are a lot of members in channel`() {
        `given build type is in scope`() And
                `given build feature has channel with a lot of users`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test
    @TestFor(issues = ["TW-66242"])
    fun `test should not report error if receiver is parameterized`() {
        `given build type is in scope`() And
                `given build feature has parameterized receiver`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 20_000)
    fun `test should not report error if authTest is hanging`() {
        `given build type is in scope`() And
                `given build features is configured correctly`() And
                `given authTest api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 10_000)
    fun `test should not report error if channelsList is hanging`() {
        `given build type is in scope`() And
                `given build features is configured correctly`() And
                `given conversationsList api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 20_000)
    fun `test should not report error if botsInfo hanging`() {
        `given build type is in scope`() And
                `given build feature has invalid channel`() And
                `given botsInfo api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 10_000)
    fun `test should not report error if getConversationMembers hanging`() {
        `given build type is in scope`() And
                `given build features is configured correctly`() And
                `given conversationsMembers api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    private fun `given build feature is missing connection`() {
        addBuildFeature()
    }

    private fun `given build feature has invalid connection`() {
        addBuildFeature(mapOf(SlackProperties.connectionProperty.key to "invalid_connection"))
    }

    private fun `given build feature has invalid channel`() {
        addBuildFeature(
            mapOf(
                SlackProperties.connectionProperty.key to myConnection.id,
                SlackProperties.channelProperty.key to "#invalid_channel"
            )
        )
    }

    private fun `given build feature has channel that bot is not added to`() {
        addBuildFeature(
            mapOf(
                SlackProperties.connectionProperty.key to myConnection.id,
                SlackProperties.channelProperty.key to "#anotherChannel"
            )
        )
    }

    private fun `given build feature has channel with a lot of users`() {
        addBuildFeature(
            mapOf(
                SlackProperties.connectionProperty.key to myConnection.id,
                SlackProperties.channelProperty.key to "#big_conversation"
            )
        )
    }

    private fun `given build features is configured correctly`() {
        addBuildFeature(
            mapOf(
                SlackProperties.connectionProperty.key to myConnection.id,
                SlackProperties.channelProperty.key to "#test_channel"
            )
        )
    }

    private fun addBuildFeature(parameters: Map<String, String> = emptyMap()) {
        myBuildType.addBuildFeature(
            NotificationsBuildFeature.FEATURE_TYPE,
            mapOf(FeatureProviderNotificationRulesHolder.NOTIFIER to myDescriptor.getType()) + parameters
        )
    }

    private fun `then report should contain error about missing connection`() {
        assertResultContains {
            it.category == SlackBuildFeatureHealthReport.invalidBuildFeatureCategory &&
                    (it.additionalData["reason"] as String).contains("connection is not selected")
        }
    }

    private fun `then report should contain error about invalid connection`() {
        assertResultContains {
            it.category == SlackBuildFeatureHealthReport.invalidBuildFeatureCategory &&
                    (it.additionalData["reason"] as String).contains("connection with id")
        }
    }

    private fun `then report should contain error about invalid channel`() {
        assertResultContains {
            it.category == SlackBuildFeatureHealthReport.invalidBuildFeatureCategory &&
                    (it.additionalData["reason"] as String).contains("find channel #invalid_channel")
        }
    }

    private fun `then report should contain error about not being added to channel`() {
        assertResultContains {
            it.category == SlackBuildFeatureHealthReport.botIsNotConfiguredCategory &&
                    (it.additionalData["reason"] as String).contains("should be added to the channel")
        }
    }
}