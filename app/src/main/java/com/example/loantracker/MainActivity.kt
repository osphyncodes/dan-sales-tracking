package com.example.loantracker

import android.os.Build
import android.os.Bundle
import androidx.compose.material3.SelectableDates
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.loantracker.data.local.database.AppDatabase
import com.example.loantracker.data.local.UserPreferences
import com.example.loantracker.data.local.entities.CustomerEntity
import com.example.loantracker.functions.getTopAppBarColors
import com.example.loantracker.navigation.MainNavGraph
import com.example.loantracker.navigation.MainNavScreen
import com.example.loantracker.navigation.ProjectNavScreen
import com.example.loantracker.ui.components.AppLockObserver
import com.example.loantracker.ui.screens.auth.LockScreen
import com.example.loantracker.ui.theme.LoanTrackerTheme
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ValViewModel
import com.example.loantracker.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.loantracker.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var database: AppDatabase
    @Inject
    lateinit var userPreferences: UserPreferences
//    private  val customerViewModel: CustomerViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )
        enableEdgeToEdge()

        setContent {
            LoanTrackerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val v: ValViewModel = hiltViewModel()
                val authVm: AuthViewModel = hiltViewModel()
                val customerViewModel: CustomerViewModel = hiltViewModel()
                val currency by userPreferences.currency.collectAsState(initial = "MK")
                val scope = rememberCoroutineScope()

                var isSearching by remember { mutableStateOf(false) }
                val searchQuery by customerViewModel.searchQuery.collectAsState()
                var showMenu by remember { mutableStateOf(false) }
                var showDatePicker by remember { mutableStateOf(false) }

                val focusRequester = remember { FocusRequester() }

                val navBarShowRoutes = listOf<String>(
                    MainNavScreen.Home.route,
                    MainNavScreen.Settings.route,
                    MainNavScreen.Customer.route,
                    ProjectNavScreen.Home.route,
                    ProjectNavScreen.Payments.route,
                    ProjectNavScreen.Overview.route,
                    ProjectNavScreen.Measurement.route
                )

                LaunchedEffect(isSearching) {
                    if (isSearching) {
                        focusRequester.requestFocus()
                    }
                }

                AppLockObserver(authVm)

                if (authVm.isAuthenticated && !authVm.isChangingPin) {
                    if (showDatePicker) {
                        val dateRangePickerState = rememberDateRangePickerState(
                            selectableDates = object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    return utcTimeMillis <= System.currentTimeMillis()
                                }
                            }
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val start = dateRangePickerState.selectedStartDateMillis?.let {
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                                    }
                                    val end = dateRangePickerState.selectedEndDateMillis?.let {
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                                    }
                                    customerViewModel.onDateRangeChange(start, end)
                                    showDatePicker = false
                                }) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DateRangePicker(
                                state = dateRangePickerState,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            if(
                                currentRoute in navBarShowRoutes
                            ){
                                NavigationBar(
                                    containerColor = Color(0xFF00695C),
                                    contentColor = Color.White
                                ) {
                                    currentRoute?.contains(other = "project/", ignoreCase = false)?.let {
                                        if (!it) {
                                            NavigationBarItem(
                                                selected = currentRoute == MainNavScreen.Home.route,
                                                onClick = { navController.navigate(MainNavScreen.Home.route) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Home,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                },
                                                label = {
                                                    Text(stringResource(R.string.home), color = Color.White)
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == MainNavScreen.Customer.route,
                                                onClick = { navController.navigate(MainNavScreen.Customer.route) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.People,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                },
                                                label = { Text(stringResource(R.string.customers), color = Color.White) }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == MainNavScreen.Settings.route,
                                                onClick = { navController.navigate(MainNavScreen.Settings.route) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                },
                                                label = { Text(stringResource(R.string.settings), color = Color.White) }
                                            )
                                        } else {
                                            NavigationBarItem(
                                                selected = currentRoute == ProjectNavScreen.Overview.route,
                                                onClick = {
                                                    v.activeProjectId?.let { projectId ->
                                                        navController.navigate(ProjectNavScreen.Overview.goToOverview(projectId))
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Event,
                                                        contentDescription = "Project"
                                                    )
                                                },
                                                label = {
                                                    Text("Project")
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == ProjectNavScreen.Measurement.route,
                                                onClick = {
                                                    v.activeProjectId.let{ projectId ->
                                                        navController.navigate(ProjectNavScreen.Measurement.goToMeasurements(projectId))
                                                    }


                                                },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Straighten,
                                                        contentDescription = "Measurements"
                                                    )
                                                },
                                                label = {
                                                    Text("Measurements")
                                                }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == ProjectNavScreen.Payments.route,
                                                onClick = {
                                                    v.activeProjectId.let{ projectId ->
                                                        navController.navigate(ProjectNavScreen.Payments.goToPayments(projectId))
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Payments,
                                                        contentDescription = "Payments"
                                                    )
                                                },
                                                label = {
                                                    Text("Payments")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if(currentRoute == "Customer" && !isSearching){
                                FloatingActionButton(
                                    onClick = {
                                        v.setTitle("Add Customer")
                                        navController.navigate(
                                            route = MainNavScreen.CustomerAddEdit.goToCustomerAddEdit()
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null
                                    )
                                }
                            }

                        },
                        topBar = {
                            if (currentRoute != ProjectNavScreen.Overview.route){
                                TopAppBar(
                                title = {
                                    if (isSearching && currentRoute == MainNavScreen.Customer.route) {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { customerViewModel.onSearchQueryChange(it) },
                                            placeholder = {
                                                Text(
                                                    "Search...",
                                                    color = Color(0xFFB2DFDB) // Light Teal placeholder
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = Color.White,
                                                focusedLabelColor = Color.Transparent,
                                                unfocusedLabelColor = Color.Transparent,
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    else {
                                        Text(
                                            when(currentRoute){
                                                MainNavScreen.Home.route -> "Loan Tracker"
                                                MainNavScreen.Customer.route -> "Customers"
                                                MainNavScreen.Settings.route -> "Settings"
                                                MainNavScreen.CustomerDetail.route -> v.name
                                                else -> v.title.value
                                            },
                                            color = Color.White
                                        )
                                    }
                                },
                                navigationIcon = {
                                    if (isSearching && currentRoute == MainNavScreen.Customer.route) {
                                        IconButton(onClick = {
                                            isSearching = false
                                            customerViewModel.onSearchQueryChange("")
                                        }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                        }
                                    } else if(currentRoute != "Home" && currentRoute != "Customer"){
                                        IconButton(

                                            onClick = {
                                                navController.popBackStack()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "back",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                },
                                colors = getTopAppBarColors(),
                                actions = {
                                    if (currentRoute == MainNavScreen.Customer.route) {
                                        if (!isSearching) {
                                            IconButton(
                                                onClick = { isSearching = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = Color.White
                                                )
                                            }
                                            IconButton(
                                                onClick = { showMenu = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Menu",
                                                    tint = Color.White
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                listOf("All", "Unfinished", "Finished", "Balances").forEach { filter ->
                                                    DropdownMenuItem(
                                                        text = { Text(filter) },
                                                        onClick = {
                                                            customerViewModel.onFilterChange(filter)
                                                            showMenu = false
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text("Date Range") },
                                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                                    onClick = {
                                                        showDatePicker = true
                                                        showMenu = false
                                                    }
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = { customerViewModel.onSearchQueryChange("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            )
                            }
                            if (currentRoute?.contains(other = "project", ignoreCase = true) == true){
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = "Project",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            v.activeProject?.let { project ->
                                                Text(
                                                    text = project.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = {
                                                v.activeProject?.let { project ->
                                                    v.activeProject?.let { project ->
                                                        navController.popBackStack(
                                                            route = "customer_detail/${project.customerId}",
                                                            inclusive = false
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    },
                                    colors = getTopAppBarColors()
                                )
                            }
                        }

                    ) { paddingValues ->
                        MainNavGraph(
                            navController = navController,
                            database,
                            paddingValues = paddingValues,
                            customerViewModel = customerViewModel,
                            valViewModel = v,
                            currency = currency,
                            onCurrencyChange = { value ->
                                if (value.isNotBlank()) {
                                    scope.launch { userPreferences.setCurrency(value.trim()) }
                                }
                            }
                        )
                    }
                }else{
                    LockScreen(
                        authVm,
                        showBiometric = authVm.mode == "VERIFY" &&
                                BiometricManager.from(this@MainActivity)
                                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                                BiometricManager.BIOMETRIC_SUCCESS,
                        onBiometric = { showBiometricUnlock(authVm) }
                    )
                }

//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
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
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Loan Tracker")
            .setSubtitle("Authenticate with your fingerprint or device biometric")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(promptInfo)
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    LoanTrackerTheme {
//        Greeting("Android")
//    }
//}
