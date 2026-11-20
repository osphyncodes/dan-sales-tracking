package com.example.loantracker.navigation


    sealed class ProjectNavScreen(val route: String){
        object Home: ProjectNavScreen(route = "project/{projectId}"){
            fun goToProjectHome(projectId: Long = 0): String {
                return this.route.replace(oldValue = "{projectId}", newValue = projectId.toString())
            }
        }

        object Overview: ProjectNavScreen(route = "project/{projectId}/overview"){
            fun goToOverview(projectId: Long = 0): String {
                return this.route.replace(oldValue = "{projectId}", newValue = projectId.toString())
            }
        }
        object Payments: ProjectNavScreen(route = "project/{projectId}/payments"){
            fun goToPayments(projectId: Long = 0): String {
                return this.route.replace(oldValue = "{projectId}", newValue = projectId.toString())
            }
        }

    }
