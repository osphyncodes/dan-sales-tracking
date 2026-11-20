package com.example.tailorscounterpart.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import com.example.tailorscounterpart.data.local.entities.CustomerWithProjects
import com.example.tailorscounterpart.data.local.entities.isProjectCompleted
import com.example.tailorscounterpart.navigation.MainNavScreen
import com.example.tailorscounterpart.viewmodel.CustomerViewModel
import com.example.tailorscounterpart.viewmodel.ValViewModel
import kotlinx.coroutines.flow.Flow
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomerCard(
    customerWithProjects: CustomerWithProjects,
    customerViewModel: CustomerViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    navController: NavHostController,
    modifier: Modifier
){
    val customer = customerWithProjects.customer
    val projects = customerWithProjects.projects
    val v: ValViewModel = viewModel()

    val contacts by customerViewModel.getContactsByCustomerId(
        customer.id.toString()
    ).collectAsState(emptyList())

    val status = when {
        projects.isEmpty() -> "No Projects"
        projects.all { it.status.isProjectCompleted() } -> "Finished"
        else -> "Unfinished"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(
                onLongClick = {
                    v.setClickedCard(customer.id)
                },
                onClick = {
                    v.setTracing(customer.name)
                    navController.navigate(MainNavScreen.CustomerDetail.goToCustomerDetail(customer.id))
                }
            )

    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(3f)
            )
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
//                        Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = customer.dateCreated.format(
                            DateTimeFormatter.ofPattern("dd/MMM/yyyy")
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status == "No Projects") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row{
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if(customer.id == v.clickedCard) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sex: ${customer.gender} | Age: ${customer.age}",
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "Locator:")
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "customer.locator",
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier.weight(1f)
            ){
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

//@Composable
//@Preview(showSystemUi = true, showBackground = true)
//fun TracingCardPreview(){
//    TracingCard(
//        onDelete = {},
//        onEdit = {},
//        modifier = Modifier.padding(29.dp)
//    )
//}