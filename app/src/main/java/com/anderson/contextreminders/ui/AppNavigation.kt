package com.anderson.contextreminders.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object List : Screen("reminders_list")
    data object Create : Screen("reminder_create")
    data object Detail : Screen("reminder_detail/{reminderId}") {
        fun createRoute(reminderId: String) = "reminder_detail/$reminderId"
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.List.route,
        modifier = modifier
    ) {
        composable(route = Screen.List.route) {
            ReminderListScreen(
                navController = navController,
                onCrearRecordatorio = { navController.navigate(Screen.Create.route) },
                onRecordatorioClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
            )
        }
        composable(route = Screen.Create.route) {
            CreateReminderScreen(
                navController = navController,
                onVolver = { navController.popBackStack() },
                onGuardarExito = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("reminderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId") ?: ""
            ReminderDetailScreen(
                navController = navController,
                reminderId = reminderId,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
