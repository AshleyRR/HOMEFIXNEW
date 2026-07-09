package com.tunegocio.homefix.ui.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

// NUEVO - MULTIDIOMA:
// Permite obtener textos y plurales desde strings_client.xml.
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*

// NUEVO - MULTIDIOMA:
// Permite acceder a las claves del módulo cliente.
import com.tunegocio.homefix.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

// NUEVO - MULTIDIOMA:
// Traduce solo la etiqueta visible de cada especialidad.
// Los valores internos permanecen en español para no afectar Firebase,
// filtros ni los datos existentes.
@Composable
private fun technicianServiceLabel(serviceType: String): String = when (serviceType) {
    "Todos" -> stringResource(R.string.service_all)
    "Electricidad" -> stringResource(R.string.service_electricity)
    "Gasfitería" -> stringResource(R.string.service_plumbing)
    "Pintura" -> stringResource(R.string.service_painting)
    "Carpintería" -> stringResource(R.string.service_carpentry)
    "Vidriería" -> stringResource(R.string.service_glasswork)
    "Jardinería" -> stringResource(R.string.service_gardening)
    "Cerrajería" -> stringResource(R.string.service_locksmith)
    "Albañilería" -> stringResource(R.string.service_masonry)
    "Muebles a medida" -> stringResource(R.string.service_custom_furniture)
    "Lavado de tapizados" -> stringResource(R.string.service_upholstery_cleaning)
    "Mudanzas" -> stringResource(R.string.service_moving)
    else -> serviceType
}

@Composable
fun TechnicianListScreen(navController: NavController) {

    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var technicians by remember { mutableStateOf(listOf<UserModel>()) }
    var filteredTechnicians by remember { mutableStateOf(listOf<UserModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedTechnicianUid by remember { mutableStateOf<String?>(null) }

    var completedCountMap by remember { mutableStateOf(mapOf<String, Int>()) } //PARA EL CONTEO DE TRABAJOS
    var clientDistrict by remember { mutableStateOf("") } //PARA UBICACION

    val filters = listOf(
        "Todos",
        "Electricidad",
        "Gasfitería",
        "Pintura",
        "Carpintería",
        "Vidriería",
        "Jardinería",
        "Cerrajería",
        "Albañilería",
        "Muebles a medida",
        "Lavado de tapizados",
        "Mudanzas"
    )

    LaunchedEffect(Unit) {
        // ── NUEVO: obtener distrito del cliente ──
        val auth = FirebaseAuth.getInstance()
        val clientUid = auth.currentUser?.uid ?: ""

        db.collection("users").document(clientUid).get()
            .addOnSuccessListener { doc ->
                clientDistrict = doc.getString("district") ?: ""
            }
        // ────────────────────────────────────────
        db.collection("users")
            .whereEqualTo("role", "technician")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                technicians = snapshot?.documents?.mapNotNull {
                    it.toObject(UserModel::class.java)
                } ?: emptyList()
                filteredTechnicians = technicians

                val uids = technicians.map { it.uid }
                uids.forEach { uid ->
                    db.collection("requests")
                        .whereEqualTo("technicianId", uid)
                        .whereEqualTo("status", "completada")
                        .get()
                        .addOnSuccessListener { reqSnapshot ->
                            completedCountMap = completedCountMap + (uid to reqSnapshot.size())
                        }
                }
            }
    }

    LaunchedEffect(selectedFilter, searchQuery, technicians) {
        filteredTechnicians = technicians.filter { tech ->
            val matchesFilter = selectedFilter == "Todos" ||
                    tech.specialties.contains(selectedFilter)

            val matchesSearch = searchQuery.isEmpty() ||
                    tech.name.contains(searchQuery, ignoreCase = true) ||
                    tech.specialties.any { it.contains(searchQuery, ignoreCase = true) }

            matchesFilter && matchesSearch
        }
    }

    fun openWhatsApp(phone: String, message: String) {
        // MODIFICADO - MULTIDIOMA:
        // Recibe el mensaje ya traducido desde la interfaz.
        // La apertura de WhatsApp permanece igual.
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val uri = Uri.parse("https://wa.me/$fullNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }

    Scaffold(
        bottomBar = {
            ClientBottomBar(navController = navController, current = "technicians")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.technicians_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )

                    /*IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }*/
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.technicians_search_hint), color = TextHint)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filtro por especialidad
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = stringResource(R.string.technicians_specialty),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (selectedFilter != "Todos") Primary else CardBorder
                    ),
                    onClick = { expanded = !expanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (selectedFilter != "Todos") Primary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = technicianServiceLabel(selectedFilter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedFilter != "Todos") Primary else MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (selectedFilter != "Todos") FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Botón limpiar filtro
                            if (selectedFilter != "Todos") {
                                IconButton(
                                    onClick = { selectedFilter = "Todos" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.technicians_clear_filter),
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = technicianServiceLabel(filter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedFilter == filter) Primary
                                    else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (selectedFilter == filter)
                                        FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedFilter = filter
                                expanded = false
                            },
                            leadingIcon = {
                                if (selectedFilter == filter) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingIcon = {
                                if (filter == "Todos" && selectedFilter != "Todos") {
                                    Text(
                                        text = stringResource(R.string.technicians_view_all),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (filteredTechnicians.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "", style = MaterialTheme.typography.headlineLarge)

                        Text(
                            text = if (selectedFilter == "Todos") {
                                stringResource(R.string.technicians_none_available)
                            } else {
                                stringResource(
                                    R.string.technicians_none_for_specialty,
                                    technicianServiceLabel(selectedFilter)
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(R.string.technicians_try_another_filter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = pluralStringResource(
                                R.plurals.technicians_available_count,
                                filteredTechnicians.size,
                                filteredTechnicians.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    // Técnicos del mismo distrito
                    if (filteredTechnicians.any { it.district == clientDistrict }) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.technicians_in_your_district),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(filteredTechnicians.filter { it.district == clientDistrict }) { tech ->
                            // NUEVO - MULTIDIOMA:
                            // El mensaje se traduce aquí y se envía sin cambiar la lógica.
                            val whatsappMessage = stringResource(
                                R.string.technicians_whatsapp_message,
                                tech.name
                            )

                            TechnicianCard(
                                technician = tech,
                                isSelected = selectedTechnicianUid == tech.uid,
                                completedJobs = completedCountMap[tech.uid] ?: 0,
                                onCardClick = {
                                    selectedTechnicianUid =
                                        if (selectedTechnicianUid == tech.uid) null else tech.uid
                                },
                                onWhatsAppClick = { openWhatsApp(tech.whatsapp, whatsappMessage) }
                            )
                        }
                    }

                    // Técnicos de otros distritos
                    if (filteredTechnicians.any { it.district != clientDistrict }) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.technicians_other_districts),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(filteredTechnicians.filter { it.district != clientDistrict }) { tech ->
                            // NUEVO - MULTIDIOMA:
                            val whatsappMessage = stringResource(
                                R.string.technicians_whatsapp_message,
                                tech.name
                            )

                            TechnicianCard(
                                technician = tech,
                                isSelected = selectedTechnicianUid == tech.uid,
                                completedJobs = completedCountMap[tech.uid]
                                    ?: 0,
                                onCardClick = {
                                    selectedTechnicianUid =
                                        if (selectedTechnicianUid == tech.uid) null else tech.uid
                                },
                                onWhatsAppClick = {
                                    openWhatsApp(tech.whatsapp, whatsappMessage)
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicianCard(
    technician: UserModel,
    isSelected: Boolean,
    completedJobs: Int,
    onCardClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    var imageBitmap by remember(technician.selfieUrl) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(technician.selfieUrl) {
        if (technician.selfieUrl.isNotBlank()) {
            imageBitmap = try {
                withContext(Dispatchers.IO) {
                    val url = URL(technician.selfieUrl)
                    android.graphics.BitmapFactory.decodeStream(url.openStream())
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = TechnicianColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.technicians_photo),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(26.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = technician.name.firstOrNull()
                                    ?.toString()?.uppercase() ?: "T",
                                style = MaterialTheme.typography.titleLarge,
                                color = TechnicianColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        //se agrego el nombre y apellido
                        text = "${technician.name} ${technician.lastName}".trim(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (technician.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(14.dp)
                            )

                            Text(
                                text = "${"%.1f".format(technician.rating)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.client_no_ratings),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )
                    }

                    if (technician.yearsExp > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.technicians_years_experience,
                                technician.yearsExp,
                                technician.yearsExp
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Success.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.technicians_active),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


            if (technician.specialties.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    technician.specialties.take(3).forEach { specialty ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TechnicianColor.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = technicianServiceLabel(specialty),
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TechnicianColor
                            )
                        }
                    }

                    if (technician.specialties.size > 3) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariant
                        ) {
                            Text(
                                text = "+${technician.specialties.size - 3}",
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp) //ANTES ERA 6.DP
                    ) {
                        // Trabajos completados NUEVO
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (completedJobs == 0) {
                                    stringResource(R.string.client_new_on_platform)
                                } else {
                                    pluralStringResource(
                                        R.plurals.technicians_completed_services,
                                        completedJobs,
                                        completedJobs
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (completedJobs > 0) Success else TextSecondary,
                                fontWeight = if (completedJobs > 0) FontWeight.Medium else FontWeight.Normal
                            )
                        }

                        // Descripción profesional
                        if (technician.bio.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.technicians_bio_format, technician.bio),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onWhatsAppClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen
                )
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.technicians_contact_whatsapp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}