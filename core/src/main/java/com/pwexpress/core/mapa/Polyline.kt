package com.pwexpress.core.mapa

/** Decodifica polyline do Google (precisão 5) em pares (lat, lng). */
fun decodificarPolyline(texto: String): List<Pair<Double, Double>> {
    val pontos = ArrayList<Pair<Double, Double>>()
    var i = 0
    var lat = 0
    var lng = 0
    while (i < texto.length) {
        for (eixo in 0..1) {
            var resultado = 0
            var deslocamento = 0
            var b: Int
            do {
                b = texto[i++].code - 63
                resultado = resultado or ((b and 0x1f) shl deslocamento)
                deslocamento += 5
            } while (b >= 0x20 && i < texto.length)
            val delta = if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1
            if (eixo == 0) lat += delta else lng += delta
        }
        pontos.add(lat / 1e5 to lng / 1e5)
    }
    return pontos
}
