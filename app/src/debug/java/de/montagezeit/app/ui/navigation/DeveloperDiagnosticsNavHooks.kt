package de.montagezeit.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.montagezeit.app.ui.screen.diagnostics.DeveloperDiagnosticsScreen
import de.montagezeit.app.ui.screen.diagnostics.DeveloperDiagnosticsTraceScreen

internal const val DEVELOPER_DIAGNOSTICS_ROUTE = "developer_diagnostics"
private const val DEVELOPER_DIAGNOSTICS_TRACE_ROUTE = "developer_diagnostics/trace/{traceId}"

internal fun developerDiagnosticsTraceRoute(traceId: String): String {
    return "developer_diagnostics/trace/$traceId"
}

fun NavGraphBuilder.registerDeveloperDiagnosticsRoutes(navController: NavHostController) {
    composable(DEVELOPER_DIAGNOSTICS_ROUTE) {
        DeveloperDiagnosticsScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenTrace = { traceId ->
                navController.navigate(developerDiagnosticsTraceRoute(traceId))
            }
        )
    }

    composable(
        route = DEVELOPER_DIAGNOSTICS_TRACE_ROUTE,
        arguments = listOf(navArgument("traceId") { type = NavType.StringType })
    ) {
        DeveloperDiagnosticsTraceScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
