package com.example.tailorscounterpart.navigation


    sealed class MainNavScreen(val route: String){
        object Home: MainNavScreen(route = "Home")
        object CustomerAddEdit: MainNavScreen(route = "customerAddEdit?id={id}"){
            fun goToCustomerAddEdit(id: Long = 0): String {
                return this.route.replace(oldValue = "{id}", newValue = id.toString())
            }
        }
        object Customer: MainNavScreen(route = "Customer")
        object CustomerDetail: MainNavScreen(route = "customer_detail/{id}"){
            fun goToCustomerDetail(id: Long): String {
                return this.route.replace(oldValue = "{id}", newValue = id.toString())
            }
        }
        object Settings: MainNavScreen(route = "Settings")
    }
