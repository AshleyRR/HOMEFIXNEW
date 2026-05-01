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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.tunegocio.homefix.data.model.UserModel
import com.tunegocio.homefix.navigation.Routes
import com.tunegocio.homefix.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
        db.collection("users")
            .whereEqualTo("role", "technician")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                technicians = snapshot?.documents?.mapNotNull {
                    it.toObject(UserModel::class.java)
                } ?: emptyList()
                filteredTechnicians = technicians
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

    fun openWhatsApp(phone: String, techName: String) {
        val number = phone.replace(Regex("[^0-9]"), "")
        val fullNumber = if (number.startsWith("51")) number else "51$number"
        val message = "Hola $techName, te contacto desde HomeFix. ¿Estás disponible para un servicio?"
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
                .background(Background)
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
                        text = "Técnicos disponibles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Buscar técnico o especialidad...", color = TextHint)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                OutlinedTextField(
                    value = selectedFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por especialidad") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder
                    )
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(CardBackground)
                ) {
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedFilter == filter) Primary else TextPrimary,
                                    fontWeight = if (selectedFilter == filter)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
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
                                }
                            }
                        )
                    }
                }
            }

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
                        Text(text = "😔", style = MaterialTheme.typography.headlineLarge)

                        Text(
                            text = if (selectedFilter == "Todos")
                                "No hay técnicos disponibles"
                            else
                                "No hay técnicos de $selectedFilter disponibles",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "Intenta con otro filtro o vuelve más tarde",
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
                            text = "${filteredTechnicians.size} técnico${if (filteredTechnicians.size != 1) "s" else ""} disponible${if (filteredTechnicians.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    items(filteredTechnicians) { tech ->
                        TechnicianCard(
                            technician = tech,
                            isSelected = selectedTechnicianUid == tech.uid,
                            onCardClick = {
                                selectedTechnicianUid =
                                    if (selectedTechnicianUid == tech.uid) null else tech.uid
                            },
                            onWhatsAppClick = {
                                openWhatsApp(tech.whatsapp, tech.name)
                            }
                        )
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
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                                contentDescription = "Foto del técnico",
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
                        text = technician.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
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
                            text = "Sin calificaciones aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )
                    }

                    if (technician.yearsExp > 0) {
                        Text(
                            text = "${technician.yearsExp} años de experiencia",
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
                        text = "● Activo",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            /*if (technician.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = technician.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }*/

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
                                text = specialty,
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = "Descripción: ${technician.bio}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
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
                    text = "Contactar por WhatsApp",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}