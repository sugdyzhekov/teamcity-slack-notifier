package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.notification.slackNotifier.notification.VerboseMessageBuilderFactory
import jetbrains.buildServer.notification.slackNotifier.teamcity.findBuildTypeSettingsByExternalId
import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class BuildTypeSlackNotifierSettingsController(
    private val pluginDescriptor: PluginDescriptor,
    private val projectManager: ProjectManager,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    webControllerManager: WebControllerManager,
    descriptor: BuildTypeSlackNotifierDescriptor,
    private val webLinks: WebLinks
) : BaseController() {

    init {
        webControllerManager.registerController(descriptor.editParametersUrl, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editBuildTypeSlackNotifierSettings.jsp"))

        val buildTypeId = request.getParameter("buildTypeId")
        val featureId = request.getParameter("featureId")

        val buildType = projectManager.findBuildTypeSettingsByExternalId(buildTypeId)
            ?: throw BuildTypeNotFoundException("Can't find build type or build template with id '${buildTypeId}'")

        val project = buildType.project

        val availableConnections = oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        mv.model["availableConnections"] = availableConnections

        val feature = buildType.findBuildFeatureById(featureId)

        val defaultProperties = mapOf(
                SlackProperties.maximumNumberOfChangesProperty.key to VerboseMessageBuilderFactory.defaultMaximumNumberOfChanges.toString()
        )

        mv.model["propertiesBean"] = BasePropertiesBean(
            feature?.parameters ?: defaultProperties,
            defaultProperties
        )
        mv.model["properties"] = SlackProperties()
        mv.model["buildTypeId"] = buildTypeId
        mv.model["createConnectionUrl"] =
            webLinks.getEditProjectPageUrl(buildType.project.externalId) + "&tab=oauthConnections"

        return mv
    }
}