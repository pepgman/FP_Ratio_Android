package com.example.fp_ratio


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    open class Alimentos {
        var alimento: String = ""
        var fosforo: Float = 0.0f
        var potasio: Float = 0.0f
        var proteina: Float = 0.0f
    }

    class Seleccion : Alimentos() {
        var cantidad: Float = 0.0f
    }

    private val MAX_FOSFORO_DIA = 1000f
    private val MAX_POTASIO_DIA = 3000f
    private val MAX_PROTEINA_DIA = 1.5f
    private var listaAlimentos = listOf<Alimentos>()
    private var listaSeleccion = mutableListOf<Seleccion>()
    private var vistaDesplegable = listOf<String>()

    private lateinit var textoFosforo: TextView
    private lateinit var textoPotasio: TextView
    private lateinit var textoProteina: TextView
    private lateinit var textoFP: TextView
    private lateinit var inputPeso: TextView

    private var peso = 0.0f
    private var cantidadTotal = 0
    private var fosforoTotal: Float = 0.0f
    private var potasioTotal: Float = 0.0f
    private var proteinasTotal: Float = 0.0f
    private var ratioFPTotal: Float = 0.0f
    private var alimentoSeleccionado = ""

    private var flag = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Inicializamos el valor de las cajas de Fosforp, Potasio y Proteinas y RatioFP

        textoFosforo = findViewById(R.id.textFosforo)
        textoPotasio = findViewById(R.id.textPotasio)
        textoProteina = findViewById(R.id.textProteina)
        textoFP = findViewById(R.id.textFP)

        // Pone el contendido del archivo de alimentos y características (test.txt) en una lista de objeto Alimentos
        listaAlimentos = convertirCsvAlimentos(resources.openRawResource(R.raw.base_alimentos))

        //Cargamos el atributo "alimento" al array que se desplegará como opciones de alimentos
        vistaDesplegable = cargaAlimentos(listaAlimentos)

        //Buscamos EditText para introducir nuestro peso
        inputPeso = findViewById<EditText>(R.id.input_peso)

        // Create an ArrayAdapter using a simple spinner layout and languages array
        val spinner = findViewById<Spinner>(R.id.spinner_alimentos)
        if (spinner != null) {
            // Set layout to use when the list of choices appear
            val adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, vistaDesplegable
            )
            // Set Adapter to Spinner
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View, position: Int, id: Long
                ) {

                    alimentoSeleccionado = vistaDesplegable[position]
                }

                override fun onNothingSelected(arg0: AdapterView<*>) {}
            }
        }

        findViewById<TextView>(R.id.text_resumen).movementMethod = ScrollingMovementMethod()

        // Actualiza los Views de la pantalla cuando se apreta el botón Añadir si las entradas de Peso y Cantidad
        // ya se han introducido
        findViewById<Button>(R.id.añadir_button).setOnClickListener {
            if (inputPeso.text.toString() == "") {
                val toast1 = Toast.makeText(
                    this@MainActivity,
                    getString(R.string.no_input_peso), Toast.LENGTH_SHORT
                )
                toast1.setGravity(Gravity.CENTER_VERTICAL, 0, 200)
                toast1.show()
            } else {
                if (findViewById<EditText>(R.id.input_cantidad).text.toString() == "") {
                    val toast2 = Toast.makeText(
                        this@MainActivity,
                        getString(R.string.no_input_cantidad), Toast.LENGTH_SHORT
                    )
                    toast2.setGravity(Gravity.CENTER_VERTICAL, 0, 200)
                    toast2.show()
                } else {
                    if (flag) {
                        añadeSeleccion(alimentoSeleccionado)
                        actualizaTotales()
                        flag = true
                    } else {
                        val toast4 = Toast.makeText(
                            this@MainActivity,
                            getString(R.string.no_press_Fin), Toast.LENGTH_SHORT
                        )
                        toast4.setGravity(Gravity.CENTER_VERTICAL, 0, 200)
                        toast4.show()
                    }
                }
            }
        }

        //Quita la última linea del resumen al apretar el botón Quitar Última
        findViewById<Button>(R.id.quitar_button).setOnClickListener {
            if (listaSeleccion.isEmpty()) {
                val toast3 = Toast.makeText(
                    this@MainActivity,
                    getString(R.string.no_hay_alimentos), Toast.LENGTH_SHORT
                )
                toast3.setGravity(Gravity.CENTER_VERTICAL, 0, 200)
                toast3.show()
            } else {
                quitaUltimaSeleccion()
                actualizaTotales()
            }
        }

        inputPeso.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (inputPeso.text.toString() != "") {
                    var p = inputPeso.text.toString()
                    peso = p.toFloat()
                    inputPeso.setText(p)
                    findViewById<EditText>(R.id.input_cantidad).requestFocus()
                    flag = true
                }
            } else {
                val toast1 = Toast.makeText(
                    this@MainActivity,
                    getString(R.string.no_input_peso), Toast.LENGTH_SHORT
                )
                toast1.setGravity(Gravity.CENTER_VERTICAL, 0, 200)
                toast1.show()
            }
            // Esconde teclado
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                findViewById<Button>(R.id.añadir_button).windowToken,
                0
            )
            true
        }

        inputPeso.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                flag = false
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }
        })
    }

    // Busca un alimento dentro de una lista de alimentos y devuelve el objeto
    private fun buscaAlimento(alimento: String, lista: List<Alimentos>): Alimentos {
        var result = Alimentos()
        for (ali in lista) {
            if (ali.alimento == alimento) {
                result = ali
            }
        }
        return result
    }

    // Refresca el texto de todos los alimentos que se van añadiendo con el nuevo alimento seleccionado
    // Se visualiza un Toast con el alimento seleccionado
    private fun añadeSeleccion(alimento: String) {
        val can = findViewById<EditText>(R.id.input_cantidad).text.toString()
        var busqueda = buscaAlimento(alimento, listaAlimentos)
        //Crea un objeto Seleccion con los datos del alimento seleccionado y lo añade a la lista
        var objetoSel = Seleccion()
        objetoSel.alimento = busqueda.alimento
        objetoSel.fosforo = busqueda.fosforo
        objetoSel.potasio = busqueda.potasio
        objetoSel.proteina = busqueda.proteina
        objetoSel.cantidad = can.toFloat()
        listaSeleccion.add(objetoSel)

        val toast3 = Toast.makeText(
            this@MainActivity,
            getString(R.string.selected_item) + " " +
                    "" + alimentoSeleccionado, Toast.LENGTH_SHORT
        )
        toast3.setGravity(Gravity.CENTER_HORIZONTAL, 0, 200)
        toast3.show()

        imprimerPantalla()

        // Esconde teclado
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            findViewById<Button>(R.id.añadir_button).windowToken,
            0
        )
    }

    private fun quitaUltimaSeleccion() {

        if (listaSeleccion.isNotEmpty()) {
            listaSeleccion.removeLast()
            imprimerPantalla()
        }
    }

    private fun imprimerPantalla() {
        var textoFinal = getString(R.string.resumen_inicio) + "\n" + "\n"
        listaSeleccion.forEach() {
            textoFinal += it.alimento + "..." + it.cantidad + "gr \n"
        }
        findViewById<TextView>(R.id.text_resumen).text = textoFinal
    }

    @SuppressLint("SetTextI18n")
    private fun actualizaTotales() {
        fosforoTotal = 0f
        potasioTotal = 0f
        proteinasTotal = 0f
        ratioFPTotal = 0f
        if (listaSeleccion.isNotEmpty()) {
            listaSeleccion.forEach() {
                fosforoTotal += (it.fosforo * it.cantidad) / 100
                potasioTotal += (it.potasio * it.cantidad) / 100
                proteinasTotal += (it.proteina) * it.cantidad / 100
                if (proteinasTotal > 0) {
                    ratioFPTotal = fosforoTotal / proteinasTotal
                } else ratioFPTotal = 0f
            }
        }
        textoFosforo.text = "Fosforo\n" + fosforoTotal.toString() + "mg"
        textoPotasio.text = "Potasio\n" + potasioTotal.toString() + "mg"
        textoProteina.text = "Proteinas\n" + proteinasTotal.toString() + "g"
        textoFP.text = "Ratio FP\n" + ratioFPTotal.toString()
        actualizaColores()
    }

    private fun actualizaColores() {
        if (fosforoTotal < MAX_FOSFORO_DIA) {
            textoFosforo.setBackgroundColor(Color.GREEN)
        } else {
            textoFosforo.setBackgroundColor(Color.RED)
        }
        if (potasioTotal < MAX_POTASIO_DIA) {
            textoPotasio.setBackgroundColor(Color.GREEN)
        } else {
            textoPotasio.setBackgroundColor(Color.RED)
        }
        if (proteinasTotal < (MAX_PROTEINA_DIA * peso)) {
            textoProteina.setBackgroundColor(Color.GREEN)
        } else {
            textoProteina.setBackgroundColor(Color.RED)
        }
    }

    // Convierte archivo formato csv con separación entre comas y sin espacios.
    private fun convertirCsvAlimentos(inputstream: InputStream): List<Alimentos> {
        val linias: List<String> = inputstream.bufferedReader().readLines()
        val lista = mutableListOf<Alimentos>()
        linias.forEach() {
            val ali = Alimentos()
            val linia = it.split(';', ignoreCase = false)
            ali.alimento = linia[0]
            ali.fosforo = linia[1].toFloat()
            ali.potasio = linia[2].toFloat()
            ali.proteina = linia[3].toFloat()
            lista += ali
        }
        return lista
    }

    // Carga el atributo "alimento" de los objetos de la clase Alimentos almacenados en la lista "listaAlimentos"
    private fun cargaAlimentos(lista: List<Alimentos>): List<String> {
        val listaAtributo = mutableListOf<String>()
        for (ali in lista) {
            listaAtributo += ali.alimento
        }
        return listaAtributo
    }
}
