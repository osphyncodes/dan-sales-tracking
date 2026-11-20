package com.example.loantracker.ui.screens.topappbars

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.room.Query
import com.example.loantracker.navigation.MainNavScreen
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ValViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    onMenuClick: () -> Unit = {},
    isSearching: Boolean,
    setIsSearching: () -> Unit = {},
    currentRoute: String,
    searchQuery: String,
    customerViewModel: CustomerViewModel,
    focusRequester: FocusRequester,
    v: ValViewModel,
    navController: NavController,
    showMenu: Boolean,
    setShowMenu: () -> Unit = {},
    setShowDatePicker: () -> Unit = {}
) {
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
            } else {
                Text(
                    when(currentRoute){
                        MainNavScreen.Home.route -> "Tracing App"
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
                    setIsSearching()
//                    isSearching = false
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF00695C), // Greenish Teal
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            if (currentRoute == MainNavScreen.Customer.route) {
                if (!isSearching) {
                    IconButton(
                        onClick = setIsSearching
//                            isSearching = true
//
//                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = setShowMenu
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = setShowMenu
                    ) {
                        listOf("All", "Unfinished", "Finished", "Balances").forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter) },
                                onClick = {
                                    customerViewModel.onFilterChange(filter)
                                    setShowMenu()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Date Range") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {

                                setShowMenu()
                                setShowDatePicker()
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