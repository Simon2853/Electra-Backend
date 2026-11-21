package com.electra.protocolos

import com.electra.protocolos.client.PruebaRemota
import com.electra.protocolos.client.UsuarioRemoto
import com.electra.protocolos.dto.ProtocoloRequest
import com.electra.protocolos.model.EstadoProtocolo
import com.electra.protocolos.model.Protocolo

/** Datos de ejemplo compartidos por las pruebas unitarias. */
object Datos {

    val tecnico = UsuarioRemoto("camila@duocuc.cl", "Camila", "tecnico", veRegistrosDeTodos = false, creadaEn = 100)
    val supervisor = UsuarioRemoto("jefe@duocuc.cl", "Jefe", "supervisor", veRegistrosDeTodos = true, creadaEn = 0)
    val admin = UsuarioRemoto("admin@duocuc.cl", "Admin", "admin", veRegistrosDeTodos = true, creadaEn = 0)

    fun solicitud(creadoPor: String = tecnico.usuario) = ProtocoloRequest(
        equipo = " TR-001 ",
        fecha = "21/11/2026",
        responsable = " Camila Rojas ",
        observaciones = " Revisión anual ",
        creadoPor = creadoPor
    )

    fun protocolo(
        id: Long = 123,
        creadoPor: String = tecnico.usuario,
        estado: String = EstadoProtocolo.EN_TERRENO,
        creadoEn: Long = 1_000
    ) = Protocolo(
        id = id, numero = "#" + id.toString().padStart(6, '0'), equipo = "TR-001", fecha = "21/11/2026",
        estado = estado, responsable = "Camila Rojas", creadoPor = creadoPor, creadoEn = creadoEn
    )

    val prueba = PruebaRemota(1, "TR-001", "Resistencia de aislamiento", "21/11/2026", "14:30", "125.5", "MΩ", "Aprobado")
}
