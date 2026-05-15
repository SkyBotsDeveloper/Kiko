package com.skybots.kiko.actions.apps

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.memory.MemoryRepository

class RealAppActionHandler(
    private val installedAppRepository: InstalledAppRepository,
    private val appMatcher: AppMatcher,
    private val appLauncher: AppLauncher,
    private val clarificationManager: ClarificationManager,
    private val memoryRepository: MemoryRepository? = null,
) : AppActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult {
        val query = intent.appQuery ?: intent.target ?: intent.rawText
        val apps = installedAppRepository.getLaunchableApps()
        findRememberedApp(query, apps)?.let { app ->
            return openApp(app, intent.languageHint)
        }

        return when (val match = appMatcher.match(query, apps)) {
            is AppMatchResult.Single -> openApp(match.app, intent.languageHint)
            is AppMatchResult.Multiple -> {
                clarificationManager.setPending(
                    type = PendingActionType.OPEN_APP,
                    candidates = match.candidates.map { app ->
                        ClarificationCandidate(
                            id = app.packageName,
                            label = app.label,
                            subtitle = app.packageName,
                        )
                    },
                    languageHint = intent.languageHint,
                    originalQuery = query,
                )
                AssistantActionResult(
                    response = LocalizedResponses.multipleApps(intent.languageHint),
                )
            }
            AppMatchResult.None -> AssistantActionResult(
                response = LocalizedResponses.appNotFound(intent.languageHint),
            )
        }
    }

    override fun handleClarification(
        candidate: ClarificationCandidate,
        languageHint: LanguageHint,
    ): AssistantActionResult =
        openApp(
            app = InstalledApp(
                label = candidate.label,
                packageName = candidate.id,
            ),
            languageHint = languageHint,
        )

    private fun openApp(
        app: InstalledApp,
        languageHint: LanguageHint,
    ): AssistantActionResult =
        if (appLauncher.launch(app.packageName)) {
            AssistantActionResult(
                response = LocalizedResponses.openingApp(app.label, languageHint),
            )
        } else {
            AssistantActionResult(
                response = LocalizedResponses.appLaunchFailed(app.label, languageHint),
            )
        }

    private fun findRememberedApp(
        query: String,
        apps: List<InstalledApp>,
    ): InstalledApp? {
        val memory = memoryRepository ?: return null
        if (!memory.isPersonalizationEnabled()) return null

        val alias = memory.findAppAlias(query) ?: return null
        return apps.firstOrNull { it.packageName == alias.packageName }
            ?: InstalledApp(
                label = alias.appLabel,
                packageName = alias.packageName,
            )
    }
}
