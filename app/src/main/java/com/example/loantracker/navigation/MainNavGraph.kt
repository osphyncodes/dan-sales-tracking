package com.example.loantracker.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navigation
import com.example.loantracker.data.local.database.AppDatabase
import com.example.loantracker.data.local.repository.VillageRepository
import com.example.loantracker.data.local.repository.NoteRepository
import com.example.loantracker.data.local.repository.PaymentRepository
import com.example.loantracker.data.local.repository.ProjectRepository
import com.example.loantracker.ui.screens.HomeScreen
import com.example.loantracker.ui.screens.DashboardScreen
import com.example.loantracker.ui.screens.customer.DetailScreen
import com.example.loantracker.ui.screens.customer.FormCustomerAddEdit
import com.example.loantracker.ui.screens.settings.SettingsScreen
import com.example.loantracker.ui.screens.project.ProjectOverviewScreen
import com.example.loantracker.ui.screens.project.ProjectPaymentsScreen
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ProjectViewModel
import com.example.loantracker.viewmodel.ValViewModel
import com.example.loantracker.viewmodel.auth.AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavGraph(
    navController: NavHostController,
    database: AppDatabase,
    paddingValues: PaddingValues,
    customerViewModel: CustomerViewModel,
    valViewModel: ValViewModel,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
){
    val projectViewModel = androidx.compose.runtime.remember(database) {
        val projectRepository = ProjectRepository(dao = database.projectDao())
        val paymentRepository = PaymentRepository(dao = database.paymentDao())
        val noteRepository = NoteRepository(dao = database.noteDao())
        ProjectViewModel(
            projectRepository = projectRepository,
            noteRepository = noteRepository,
            paymentRepository = paymentRepository,
        )
    }

    NavHost(
        startDestination = "main",
        navController = navController
    )
    {
        navigation(
            startDestination = MainNavScreen.Home.route,
            route = "main"
        )
        {
            composable(MainNavScreen.Home.route) {
                DashboardScreen(
                    database = database,
                    paddingValues = paddingValues,
                    currency = currency
                )
            }

            composable(
                route = MainNavScreen.Customer.route
            )
            {
                HomeScreen(
                    customerViewModel = customerViewModel,
                    projectViewModel = projectViewModel,
                    navController = navController,
                    paddingValues = paddingValues,
                    v = valViewModel,
                    currency = currency
                )
            }

            composable(
                route = MainNavScreen.Settings.route
            ) {
                SettingsScreen(
                    authViewModel = authViewModel,
                    paddingValues = paddingValues,
                    currency = currency,
                    onCurrencyChange = onCurrencyChange
                )
            }

            composable(
                route = MainNavScreen.CustomerAddEdit.route,
                arguments = listOf(
                    navArgument(name = "id"){
                        type = NavType.IntType
                    }
                )
            )
            {
                val id = it.arguments?.get("id").toString()

                FormCustomerAddEdit(
                    id = id,
                    viewModel = customerViewModel,
                    paddingValues = paddingValues,
                    navController = navController,
                    v = valViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(
                route = MainNavScreen.CustomerDetail.route,
                arguments = listOf(
                    navArgument(name = "id"){
                        type = NavType.IntType
                    }
                )
            ) {
                val id = it.arguments?.get("id").toString()

                DetailScreen(
                    paddingValues = paddingValues,
                    projectViewModel = projectViewModel,
                    customerViewModel = customerViewModel,
                    v = valViewModel,
                    customerId = id.toLong(),
                    navController = navController,
                    currency = currency,
                    authViewModel = authViewModel
                )
            }
        }

        navigation(
            startDestination = ProjectNavScreen.Overview.route,
            route = ProjectNavScreen.Home.route
        ){
            composable(route = ProjectNavScreen.Overview.route){
                val projectId = it.arguments?.get("projectId").toString()
                ProjectOverviewScreen(
                    paddingValues = paddingValues,
                    projectId = projectId.toLong(),
                    projectViewModel = projectViewModel,
                    customerViewModel = customerViewModel,
                    currency = currency,
                    valViewModel = valViewModel,
                    onPaymentsClick = {
                        navController.navigate(ProjectNavScreen.Payments.goToPayments(projectId.toLong()))
                    },
                    onProjectDeleted = { customerId ->
                        navController.popBackStack(
                            route = com.example.loantracker.navigation.MainNavScreen.CustomerDetail.goToCustomerDetail(customerId),
                            inclusive = false
                        )
                    }
                )
            }
            composable(route = ProjectNavScreen.Payments.route){
                ProjectPaymentsScreen(
                    paddingValues = paddingValues,
                    projectViewModel = projectViewModel,
                    valViewModel = valViewModel,
                    currency = currency,
                    onBack = { navController.popBackStack() }
                )
            }
        }

    }
}
