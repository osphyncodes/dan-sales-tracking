package com.example.loantracker

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.loantracker.data.local.UserPreferences
import com.example.loantracker.data.local.database.AppDatabase
import com.example.loantracker.functions.getTopAppBarColors
import com.example.loantracker.navigation.MainNavGraph
import com.example.loantracker.navigation.MainNavScreen
import com.example.loantracker.navigation.ProjectNavScreen
import com.example.loantracker.ui.components.AppLockObserver
import com.example.loantracker.ui.screens.auth.LockScreen
import com.example.loantracker.ui.theme.LoanTrackerTheme
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.LoanDetailAction
import com.example.loantracker.viewmodel.ValViewModel
import com.example.loantracker.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.launch

private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val primaryDestinations = listOf(
    PrimaryDestination(MainNavScreen.Home.route, "Overview", Icons.Default.Home),
    PrimaryDestination(MainNavScreen.Customer.route, "Customers", Icons.Default.People),
    PrimaryDestination(MainNavScreen.Settings.route, "Settings", Icons.Default.Settings)
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var userPreferences: UserPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LoanTrackerTheme {
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                val currentRoute = entry?.destination?.route
                val valViewModel: ValViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val customerViewModel: CustomerViewModel = hiltViewModel()
                val currency by userPreferences.currency.collectAsState(initial = "MK")
                val scope = rememberCoroutineScope()
                var isSearching by remember { mutableStateOf(false) }
                var showMenu by remember { mutableStateOf(false) }
                var showLoanMenu by remember { mutableStateOf(false) }
                var showDatePicker by remember { mutableStateOf(false) }
                val searchQuery by customerViewModel.searchQuery.collectAsState()
                val focusRequester = remember { FocusRequester() }
                val isPrimaryDestination = primaryDestinations.any { it.route == currentRoute }

                LaunchedEffect(isSearching) {
                    if (isSearching) focusRequester.requestFocus()
                }
                LaunchedEffect(currentRoute) {
                    if (currentRoute != MainNavScreen.Customer.route) isSearching = false
                }
                AppLockObserver(authViewModel)

                if (authViewModel.isAuthenticated && !authViewModel.isChangingPin) {
                    if (showDatePicker) {
                        val state = androidx.compose.material3.rememberDateRangePickerState(
                            selectableDates = object : androidx.compose.material3.SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                    utcTimeMillis <= System.currentTimeMillis()
                            }
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val start = state.selectedStartDateMillis?.let {
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                                    }
                                    val end = state.selectedEndDateMillis?.let {
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                                    }
                                    customerViewModel.onDateRangeChange(start, end)
                                    showDatePicker = false
                                }) { Text("Apply") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                            }
                        ) {
                            androidx.compose.material3.DateRangePicker(state = state)
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            if (isPrimaryDestination) {
                                NavigationBar {
                                    primaryDestinations.forEach { destination ->
                                        NavigationBarItem(
                                            selected = currentRoute == destination.route,
                                            onClick = {
                                                navController.navigate(destination.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                                            label = { Text(destination.label) }
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (currentRoute == MainNavScreen.Customer.route && !isSearching) {
                                androidx.compose.material3.FloatingActionButton(
                                    onClick = {
                                        navController.navigate(MainNavScreen.CustomerAddEdit.goToCustomerAddEdit())
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add customer"
                                    )
                                }
                            }
                        },
                        topBar = {
                            if (currentRoute != ProjectNavScreen.Payments.route) {
                                TopAppBar(
                                    title = {
                                        if (isSearching && currentRoute == MainNavScreen.Customer.route) {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = customerViewModel::onSearchQueryChange,
                                                placeholder = { Text("Search customers") },
                                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                                singleLine = true
                                            )
                                        } else {
                                            val title = when (currentRoute) {
                                                MainNavScreen.Home.route -> "Loan Tracker"
                                                MainNavScreen.Customer.route -> "Customers"
                                                MainNavScreen.Settings.route -> "Settings"
                                                MainNavScreen.CustomerDetail.route -> valViewModel.name
                                                MainNavScreen.CustomerAddEdit.route -> valViewModel.title.value
                                                ProjectNavScreen.Overview.route -> valViewModel.activeProject?.name ?: "Loan details"
                                                ProjectNavScreen.Payments.route -> "Payments"
                                                else -> valViewModel.title.value.ifBlank { "Loan Tracker" }
                                            }
                                            Text(
                                                title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        if (isSearching && currentRoute == MainNavScreen.Customer.route) {
                                            IconButton(onClick = {
                                                isSearching = false
                                                customerViewModel.onSearchQueryChange("")
                                            }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                                            }
                                        } else if (!isPrimaryDestination) {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }
                                        }
                                    },
                                    colors = getTopAppBarColors(),
                                    actions = {
                                        if (currentRoute == MainNavScreen.Customer.route) {
                                            if (!isSearching) {
                                                IconButton(onClick = { isSearching = true }) {
                                                    Icon(Icons.Default.Search, contentDescription = "Search customers")
                                                }
                                                IconButton(onClick = { showMenu = true }) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "Customer filters")
                                                }
                                                DropdownMenu(
                                                    expanded = showMenu,
                                                    onDismissRequest = { showMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Filter by date") },
                                                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                                        onClick = {
                                                            showMenu = false
                                                            showDatePicker = true
                                                        }
                                                    )
                                                }
                                            } else if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { customerViewModel.onSearchQueryChange("") }) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Clear search"
                                                    )
                                                }
                                            }
                                        } else if (currentRoute == ProjectNavScreen.Overview.route) {
                                            IconButton(onClick = { showLoanMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Loan actions")
                                            }
                                            DropdownMenu(
                                                expanded = showLoanMenu,
                                                onDismissRequest = { showLoanMenu = false }
                                            ) {
                                                listOf(
                                                    "Edit loan" to LoanDetailAction.EDIT,
                                                    "Delete loan" to LoanDetailAction.DELETE,
                                                    "Add payment" to LoanDetailAction.ADD_PAYMENT,
                                                    "Payment history" to null,
                                                    "Add notes" to LoanDetailAction.ADD_NOTE
                                                ).forEach { (label, action) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            showLoanMenu = false
                                                            if (action == null) {
                                                                navController.navigate(
                                                                    ProjectNavScreen.Payments.goToPayments(
                                                                        valViewModel.activeProjectId
                                                                    )
                                                                )
                                                            } else {
                                                                valViewModel.requestLoanDetailAction(action)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { paddingValues ->
                        MainNavGraph(
                            navController = navController,
                            database = database,
                            paddingValues = paddingValues,
                            customerViewModel = customerViewModel,
                            valViewModel = valViewModel,
                            authViewModel = authViewModel,
                            currency = currency,
                            onCurrencyChange = { value ->
                                if (value.isNotBlank()) scope.launch { userPreferences.setCurrency(value.trim()) }
                            }
                        )
                    }
                } else {
                    LockScreen(
                        authViewModel,
                        showBiometric = authViewModel.mode == "VERIFY" &&
                            BiometricManager.from(this@MainActivity)
                                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                            BiometricManager.BIOMETRIC_SUCCESS,
                        onBiometric = { showBiometricUnlock(authViewModel) }
                    )
                }
            }
        }
    }

    private fun showBiometricUnlock(authViewModel: AuthViewModel) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authViewModel.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Loan Tracker")
                .setSubtitle("Authenticate with your device biometric")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }
}
