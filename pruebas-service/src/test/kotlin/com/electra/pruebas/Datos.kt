package com.electra.pruebas

import com.electra.pruebas.client.UsuarioRemoto
import com.electra.pruebas.dto.PruebaRequest
import com.electra.pruebas.model.Prueba

/** Datos de ejemplo compartidos por las pruebas unitarias. */
object Datos {

    val tecnico = UsuarioRemoto(usuario = "camila@duocuc.cl", rol = "tecnico", veRegistrosDeTodos = false, creadaEn = 100)
    val supervisor = UsuarioRemoto(usuario = "jefe@duocuc.cl", rol = "supervisor", veRegistrosDeTodos = true, creadaEn = 500)
    val admin = UsuarioRemoto(usuario = "admin@duocuc.cl", rol = "admin", veRegistrosDeTodos = true, creadaEn = 0)

    fun solicitud(
        creadoPor: String = tecnico.usuario,
        latitud: Double? = -33.45,
        longitud: Double? = -70.66
    ) = PruebaRequest(
        equipo = " TR-001 ",
        tipoPrueba = "Resistencia de aislamiento",
        fecha = "21/11/2026",
        hora = "14:30",
        medicion = "125.5",
        unidad = "MΩ",
        resultado = "Aprobado",
        observaciones = " Sin novedad ",
        latitud = latitud,
        longitud = longitud,
        creadoPor = creadoPor
    )

    fun prueba(id: Long = 1, creadoPor: String = tecnico.usuario, creadoEn: Long = 1_000, equipo: String = "TR-001") = Prueba(
        id = id, equipo = equipo, tipoPrueba = "Resistencia de aislamiento", fecha = "21/11/2026", hora = "14:30",
        medicion = "125.5", unidad = "MΩ", resultado = "Aprobado", creadoPor = creadoPor, creadoEn = creadoEn
    )
}
