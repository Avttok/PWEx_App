package com.pwexpress.core.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pwexpress.core.designsystem.Pw
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Um ponto no mapa (parada da corrida). */
data class PinoMapa(val lat: Double, val lng: Double, val coleta: Boolean, val atual: Boolean, val concluida: Boolean)

/** Marcador livre (ex.: motoboy no mapa do gestor): cor em hex e rótulo curto. */
data class MarcadorMapa(val id: String, val lat: Double, val lng: Double, val cor: String, val rotulo: String)

private fun Color.hex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private const val ESTILO_CLARO = "https://tiles.openfreemap.org/styles/positron"
private const val ESTILO_ESCURO = "https://tiles.openfreemap.org/styles/dark"

/**
 * Mapa do app (MapLibre + OpenStreetMap). Tema claro/noturno automático,
 * rota planejada em dourado, paradas e a posição do motoboy.
 */
@Composable
fun PwMapa(
    paradas: List<PinoMapa>,
    rota: String?,
    minhaPosicao: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    marcadores: List<MarcadorMapa> = emptyList(),
    onToque: ((Double, Double) -> Unit)? = null,
    /** Mude este valor para o mapa enquadrar tudo de novo. */
    enquadrar: Any? = null,
) {
    val ctx = LocalContext.current
    val escuro = Pw.cores.escuro
    val ouro = Pw.cores.ouro.hex()
    val toque by rememberUpdatedState(onToque)
    val ciclo = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(ctx.applicationContext)
        MapView(ctx).apply { onCreate(null) }
    }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estilo by remember { mutableStateOf<Style?>(null) }
    var enquadrou by remember { mutableStateOf(false) }

    // ciclo de vida do MapView
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(obs)
        onDispose {
            ciclo.lifecycle.removeObserver(obs)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // estilo (recarrega ao trocar o tema)
    LaunchedEffect(enquadrar) { enquadrou = false }

    LaunchedEffect(escuro, ouro) {
        estilo = null
        enquadrou = false
        mapView.getMapAsync { m ->
            mapa = m
            m.uiSettings.isCompassEnabled = false
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.addOnMapClickListener { p -> toque?.let { it(p.latitude, p.longitude); true } ?: false }
            m.setStyle(Style.Builder().fromUri(if (escuro) ESTILO_ESCURO else ESTILO_CLARO)) { s ->
                criarCamadas(s, escuro, ouro)
                estilo = s
            }
        }
    }

    // dados
    LaunchedEffect(estilo, paradas, rota, minhaPosicao, marcadores) {
        val s = estilo ?: return@LaunchedEffect
        val m = mapa ?: return@LaunchedEffect

        val linha = rota?.let { decodificarPolyline(it) }.orEmpty()
        (s.getSource("rota") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                if (linha.size >= 2) listOf(Feature.fromGeometry(LineString.fromLngLats(linha.map { Point.fromLngLat(it.second, it.first) })))
                else emptyList(),
            ),
        )

        (s.getSource("paradas") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                paradas.map { p ->
                    Feature.fromGeometry(Point.fromLngLat(p.lng, p.lat)).apply {
                        addStringProperty("cor", if (p.coleta) ouro else "#111114")
                        addNumberProperty("raio", if (p.atual) 11 else 7)
                        addNumberProperty("opacidade", if (p.concluida) 0.45 else 1.0)
                    }
                },
            ),
        )

        (s.getSource("eu") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                minhaPosicao?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.second, it.first))) } ?: emptyList(),
            ),
        )

        (s.getSource("marcadores") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                marcadores.map { mk ->
                    Feature.fromGeometry(Point.fromLngLat(mk.lng, mk.lat)).apply {
                        addStringProperty("cor", mk.cor)
                        addStringProperty("rotulo", mk.rotulo)
                    }
                },
            ),
        )

        if (!enquadrou) {
            val pontos = paradas.map { LatLng(it.lat, it.lng) } + listOfNotNull(minhaPosicao?.let { LatLng(it.first, it.second) }) +
                marcadores.map { LatLng(it.lat, it.lng) }
            when {
                pontos.size >= 2 -> m.easeCamera(
                    CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(pontos).build(), 120),
                    700,
                )
                pontos.size == 1 -> m.easeCamera(CameraUpdateFactory.newLatLngZoom(pontos[0], 15.0), 700)
            }
            if (pontos.isNotEmpty()) enquadrou = true
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun criarCamadas(s: Style, escuro: Boolean, ouro: String) {
    adicionarEstabelecimentos(s, escuro)
    s.addSource(GeoJsonSource("marcadores"))
    s.addSource(GeoJsonSource("rota"))
    s.addSource(GeoJsonSource("paradas"))
    s.addSource(GeoJsonSource("eu"))

    s.addLayer(
        LineLayer("rota-contorno", "rota").withProperties(
            PropertyFactory.lineColor("#0A0A0A"),
            PropertyFactory.lineOpacity(0.45f),
            PropertyFactory.lineWidth(9f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    s.addLayer(
        LineLayer("rota", "rota").withProperties(
            PropertyFactory.lineColor(ouro),
            PropertyFactory.lineWidth(5.5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    s.addLayer(
        CircleLayer("paradas", "paradas").withProperties(
            PropertyFactory.circleRadius(Expression.get("raio")),
            PropertyFactory.circleColor(Expression.toColor(Expression.get("cor"))),
            PropertyFactory.circleOpacity(Expression.get("opacidade")),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f),
        ),
    )
    s.addLayer(
        CircleLayer("eu-halo", "eu").withProperties(
            PropertyFactory.circleRadius(20f),
            PropertyFactory.circleColor(ouro),
            PropertyFactory.circleOpacity(0.22f),
        ),
    )
    s.addLayer(
        CircleLayer("eu", "eu").withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor("#F3D77E"),
            PropertyFactory.circleStrokeColor("#0A0A0A"),
            PropertyFactory.circleStrokeWidth(3f),
        ),
    )
    s.addLayer(
        CircleLayer("marcadores", "marcadores").withProperties(
            PropertyFactory.circleRadius(13f),
            PropertyFactory.circleColor(Expression.toColor(Expression.get("cor"))),
            PropertyFactory.circleStrokeColor(if (escuro) "#0A0A0A" else "#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f),
        ),
    )
    s.addLayer(
        SymbolLayer("marcadores-rotulo", "marcadores").withProperties(
            PropertyFactory.textField(Expression.get("rotulo")),
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(arrayOf(0f, 1.3f)),
            PropertyFactory.textColor(if (escuro) "#F5F2EA" else "#16130C"),
            PropertyFactory.textHaloColor(if (escuro) "#0B0B0D" else "#FFFFFF"),
            PropertyFactory.textHaloWidth(1.6f),
            PropertyFactory.textAllowOverlap(true),
        ),
    )
}

/**
 * Nomes de estabelecimentos (comércio, restaurantes, postos…), a partir do zoom de bairro.
 * Os estilos limpos não mostram, mas os dados estão nos mesmos mapas (camada "poi").
 */
private fun adicionarEstabelecimentos(s: Style, escuro: Boolean) {
    if (s.getSource("openmaptiles") == null) return
    s.addLayer(
        CircleLayer("pwex-poi-ponto", "openmaptiles").apply {
            sourceLayer = "poi"
            minZoom = 15f
            setFilter(Expression.has("name"))
            setProperties(
                PropertyFactory.circleRadius(3f),
                PropertyFactory.circleColor(if (escuro) "#D4AF37" else "#B8901F"),
                PropertyFactory.circleStrokeColor(if (escuro) "#0B0B0D" else "#FFFFFF"),
                PropertyFactory.circleStrokeWidth(1.2f),
            )
        },
    )
    s.addLayer(
        SymbolLayer("pwex-poi", "openmaptiles").apply {
            sourceLayer = "poi"
            minZoom = 15f
            setFilter(Expression.has("name"))
            setProperties(
                PropertyFactory.textField(Expression.coalesce(Expression.get("name:pt"), Expression.get("name"))),
                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                PropertyFactory.textSize(11.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.textOffset(arrayOf(0f, 0.5f)),
                PropertyFactory.textMaxWidth(9f),
                PropertyFactory.textOptional(true),
                PropertyFactory.textColor(if (escuro) "#E9C766" else "#6B5410"),
                PropertyFactory.textHaloColor(if (escuro) "#0B0B0D" else "#FFFFFF"),
                PropertyFactory.textHaloWidth(1.4f),
            )
        },
    )
}
