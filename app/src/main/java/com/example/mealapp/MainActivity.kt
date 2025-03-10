package com.example.mealapp
import coil.compose.AsyncImage
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealApp()
        }
    }
}

@Preview
@Composable
fun MealApp() {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) } // ⬅ Controlla se mostrare la SplashScreen

    LaunchedEffect(Unit) {
        delay(3000) // ⬅ Aspetta 3 secondi
        showSplash = false // ⬅ Nasconde la SplashScreen
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSplash) {
            SplashScreen() // ⬅ Mostra la SplashScreen sopra tutto
        } else {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController) }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    NavHost(navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("profile") { ProfileScreen() }
                    }
                }
            }
        }
    }
}

object RetrofitInstance {
    private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

    val api: MealApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealApiService::class.java)
    }
}
@Composable
fun HomeScreen(navController: NavHostController) {
    val meals = remember { mutableStateListOf<Meal>() }
    val coroutineScope = rememberCoroutineScope()
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }

    val isLoading by remember { mutableStateOf(false) }


    // Ottiene i piatti filtrati per categoria
    suspend fun getMealsByCategory(category: String): List<Meal> {
        return try {
            val response = RetrofitInstance.api.getMealsByCategory(category)
            response.meals ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun searchMeals(query: String = "") {
        coroutineScope.launch {
            try {
                val response = if (query.isNotEmpty()) {
                    RetrofitInstance.api.searchMeals(query) // Chiamata API per la ricerca
                } else {
                    RetrofitInstance.api.getRandomMeal() // Chiamata API per piatti casuali
                }

                meals.clear() // 🔥 SVUOTA la lista prima di aggiungere nuovi piatti

                response.meals?.let { newMeals ->
                    meals.addAll(newMeals) // 🔹 Aggiunge direttamente tutti i risultati
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }





    // Funzione che carica i piatti casuali iniziali
    suspend fun getRandomMeals(): List<Meal> {
        val response = RetrofitInstance.api.getRandomMeal()
        return response.meals ?: emptyList()
    }

    // Funzione di caricamento per le categorie
    suspend fun getCategories(): List<String> {
        val response = RetrofitInstance.api.getCategories()
        return response.categories.map { it.strCategory }
    }
    suspend fun loadMoreRandomMeals() {
        val randomMeals = getRandomMeals() // Chiamata per ottenere i piatti casuali
        meals.addAll(randomMeals) // Aggiungi i piatti alla lista esistente
    }
    // Carica le categorie e piatti casuali quando la schermata viene creata
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                categories = getCategories() // Carica le categorie
                loadMoreRandomMeals() // Carica i piatti casuali iniziali
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        //testo e logo
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Make your own food,\nstay at home",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFC107),
                    textAlign = TextAlign.Start
                )
            }
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        //barra di ricerca
        var searchText by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text("Search any recipe") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Black
            )
        )

        LaunchedEffect(searchText) {
            // Se il testo di ricerca è vuoto, svuota i risultati e carica piatti casuali
            if (searchText.isEmpty()) {
                meals.clear()
                loadMoreRandomMeals()  // Ricarica i piatti casuali
            } else {
                searchMeals(searchText)  // Altrimenti, esegui la ricerca

            }
        }




        Spacer(modifier = Modifier.height(8.dp))

        //bottoni filtro per categoria
        if (categories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                items(categories) { category ->
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                meals.clear()
                                meals.addAll(getMealsByCategory(category))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(category, color = Color.Black)
                    }
                }
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scroll dei piatti casuali
        LazyRow(
            modifier = Modifier.padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(meals) { meal ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { selectedMeal = meal }, // Aggiungi click per il pop-up
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(250.dp)
                            .background(Color(0xFFFFC107), shape = RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = meal.strMealThumb,
                                contentDescription = meal.strMeal,
                                modifier = Modifier.size(150.dp),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = meal.strMeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Caricamento infinito solo se la ricerca è vuota
            item {
                if (!isLoading && searchText.isEmpty()) {  // Se la barra di ricerca è vuota
                    LaunchedEffect(meals.size) {
                        loadMoreRandomMeals() // Carica più piatti quando si è alla fine
                    }
                }
            }
        }

    }



    //pop-up con dettagli piatto
    if (selectedMeal != null) {
        MealDetailsDialog(meal = selectedMeal!!, onDismiss = { selectedMeal = null })
    }
}

@Composable
fun MealDetailsDialog(meal: Meal, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(meal.strMeal, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = meal.strMealThumb,
                    contentDescription = meal.strMeal,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                //ingredienti in 3 colonne
                val ingredients = meal.getIngredientList()
                if (ingredients.isNotEmpty()) {
                    Text("Ingredients:", fontWeight = FontWeight.Bold)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3), // 3 colonne
                        modifier = Modifier.fillMaxWidth().height(100.dp) // Altezza fissa per non occupare troppo spazio
                    ) {
                        items(ingredients) { ingredient ->
                            Text(
                                text = ingredient,
                                modifier = Modifier.padding(4.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                //istruzioni scrollabili
                Text("Instructions:", fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Text(meal.strInstructions ?: "No description available.")
                }
            }
        }
    )
}

@Composable
fun ProfileScreen() {
    var meal by remember { mutableStateOf<Meal?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    var spaces by remember { mutableStateOf(mutableListOf<String>()) }
    var shuffledLetters by remember { mutableStateOf(mutableListOf<String>()) }

    LaunchedEffect(refreshKey) {
        coroutineScope.launch {
            try {
                var response: MealResponse?
                var validMeal: Meal? = null

                // Cerca un piatto che abbia un nome di massimo 12 caratteri
                while (validMeal == null) {
                    response = RetrofitInstance.api.getRandomMeal()
                    validMeal = response?.meals?.firstOrNull { it.strMeal.length <= 12 }
                }

                meal = validMeal
                Log.d("ProfileScreen", "Meal loaded: ${meal?.strMeal}")

                meal?.strMeal?.let { originalName ->
                    spaces = originalName.map { if (it != ' ') "" else " " }.toMutableStateList()
                    shuffledLetters = originalName.replace(" ", "").toList()
                        .shuffled()
                        .map { it.toString() }
                        .toMutableStateList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ProfileScreen", "Error loading meal", e)
            }
        }
    }



    meal?.let { currentMeal ->
        val originalName = currentMeal.strMeal
        var selectedLetter by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = currentMeal.strMealThumb,
                contentDescription = currentMeal.strMeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.Center) {
                spaces.forEachIndexed { index, space ->
                    if (space.isEmpty()) {
                        DropBox(index, spaces, selectedLetter, shuffledLetters) { newLetter ->
                            spaces[index] = newLetter
                            selectedLetter = null
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .background(Color(0xFFFFE0B2), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Black)
                                .clickable {
                                    shuffledLetters.add(space)
                                    spaces[index] = ""
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = space, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.Center) {
                shuffledLetters.forEach { letter ->
                    DraggableLetter(letter) { clickedLetter ->
                        selectedLetter = clickedLetter
                        shuffledLetters.remove(clickedLetter)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val result = spaces.joinToString("")
                    if (result == originalName) {
                        isCorrect = true
                        feedbackMessage = "\u2714 Correct!"
                    } else {
                        isCorrect = false
                        feedbackMessage = "\u274C Wrong! The right answer was: $originalName"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
            ) {
                Text("Check Answer", color = Color.White)
            }

            feedbackMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect == true) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        refreshKey++
                        feedbackMessage = null
                        isCorrect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA))
                ) {
                    Text("Play Again", color = Color.White)
                }
            }
        }
    }
}



@Composable
fun DropBox(
    index: Int,
    spaces: MutableList<String>,
    selectedLetter: String?,
    shuffledLetters: MutableList<String>,
    onDrop: (String) -> Unit
) {
    if (spaces[index] == " ") {
        Box(
            modifier = Modifier.size(20.dp), //piccolo spazio tra le parole
        )
    } else {
        //altrimenti casella interattiva per le lettere
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(4.dp)
                .clickable {
                    selectedLetter?.let {
                        if (spaces[index].isEmpty()) {
                            onDrop(it)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(spaces[index], fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}


//lettera cliccabile
@Composable
fun DraggableLetter(letter: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFFFFE135), shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
            .padding(4.dp)
            .clickable {
                onClick(letter)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(letter, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) }

    NavigationBar(containerColor = Color(0xFFFFC107)) {
        NavigationBarItem(
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.banana_icon),
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home") },
            selected = false,
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.bana_logo_2),
                    contentDescription = "Game",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Game") },
            selected = false,
            onClick = {
                showDialog = true //mostra il popup
            }
        )
    }

    //popup Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Guess the food") },
            text = {
                Text("To play, you have to guess the name of the dish! Use the letters to complete the name. Good luck!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        navController.navigate("profile") // Naviga alla schermata del profilo dopo che l'utente ha visto il messaggio
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class MealResponse(val meals: List<Meal>)

data class CategoriesResponse(
    val categories: List<Category>
)

data class Category(
    val strCategory: String
)

data class MealsResponse(
    val meals: List<Meal>?
)

data class Meal(
    val strMeal: String,
    val strMealThumb: String,
    val strInstructions: String?,
    val strIngredient1: String?, val strIngredient2: String?, val strIngredient3: String?,
    val strIngredient4: String?, val strIngredient5: String?, val strIngredient6: String?,
    val strIngredient7: String?, val strIngredient8: String?, val strIngredient9: String?,
    val strIngredient10: String?, val strIngredient11: String?, val strIngredient12: String?,
    val strIngredient13: String?, val strIngredient14: String?, val strIngredient15: String?,
    val strIngredient16: String?, val strIngredient17: String?, val strIngredient18: String?,
    val strIngredient19: String?, val strIngredient20: String?
) {
    fun getIngredientList(): List<String> {
        return listOfNotNull(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        ).filter { it.isNotBlank() }
    }
}
