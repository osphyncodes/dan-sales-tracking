package com.example.loantracker.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectNavScreenTest {
    @Test fun projectRoutesIncludeTheSelectedProjectId() {
        assertEquals("project/42/overview", ProjectNavScreen.Overview.goToOverview(42))
        assertEquals("project/42/measurements", ProjectNavScreen.Measurement.goToMeasurements(42))
        assertEquals("project/42/payments", ProjectNavScreen.Payments.goToPayments(42))
    }
}
