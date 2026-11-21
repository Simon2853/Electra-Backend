package com.electra.protocolos.service

import com.electra.protocolos.client.PruebaRemota
import com.electra.protocolos.client.ServiciosRemotos
import com.electra.protocolos.dto.ProtocoloRequest
import com.electra.protocolos.exception.Conflicto
import com.electra.protocolos.exception.NoEncontrado
import com.electra.protocolos.exception.Prohibido
import com.electra.protocolos.exception.SolicitudInvalida
import com.electra.protocolos.model.EstadoProtocolo
import com.electra.protocolos.model.NumeroProtocolo
import com.electra.protocolos.model.Protocolo
import com.electra.protocolos.repository.ProtocoloRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reglas de negocio de los protocolos:
 * - el número correlativo lo asigna el servidor (único en toda la empresa);
 * - crean protocolos el técnico y el administrador; formalizan el supervisor y el administrador;
 * - un protocolo formalizado ya no se modifica ni se elimina (tiene valor ante el cliente).
 * Quién es cada usuario se le pregunta a usuarios-service vía Feign.
 */
@Service
@Transactional
class ProtocoloService(
    private val repositorio: ProtocoloRepository,
    private val remotos: ServiciosRemotos,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    fun crear(datos: ProtocoloRequest): Protocolo {
        val creador = try {
            remotos.usuario(datos.creadoPor)
        } catch (e: NoEncontrado) {
            throw SolicitudInvalida("El creador ${datos.creadoPor.trim()} no existe en usuarios-service")
        }
        if (!creador.puedeCrearProtocolos) throw Prohibido("El rol ${creador.rol} no crea protocolos (separación de funciones)")

        val guardado = repositorio.save(
            Protocolo(
                equipo = datos.equipo.trim(),
                fecha = datos.fecha,
                responsable = datos.responsable.trim(),
                observaciones = datos.observaciones.trim(),
                creadoPor = creador.usuario,
                creadoEn = reloj()
            )
        )
        // El id lo acaba de asignar MySQL (AUTO_INCREMENT), así que el número no se puede repetir.
        guardado.numero = NumeroProtocolo.desdeId(guardado.id!!)
        return guardado
    }

    /** Número que tendría el próximo protocolo; es referencial porque otro técnico podría guardar antes. */
    @Transactional(readOnly = true)
    fun siguienteNumero(): String = NumeroProtocolo.desdeId(repositorio.mayorId() + 1)

    @Transactional(readOnly = true)
    fun listar(usuario: String): List<Protocolo> {
        val alcance = remotos.alcanceDe(usuario)
        return repositorio.buscar(alcance.creador, alcance.creadosDesde)
    }

    /** Un protocolo que el usuario no puede ver responde igual que si no existiera (404). */
    @Transactional(readOnly = true)
    fun buscar(numero: String, usuario: String): Protocolo {
        val normalizado = NumeroProtocolo.normalizar(numero) ?: throw SolicitudInvalida("Número de protocolo inválido: $numero")
        val alcance = remotos.alcanceDe(usuario)
        return repositorio.findByNumero(normalizado)?.takeIf { alcance.puedeVer(it) }
            ?: throw NoEncontrado("No existe el protocolo $normalizado")
    }

    fun cambiarEstado(numero: String, estado: String, usuario: String): Protocolo {
        if (estado !in EstadoProtocolo.TODOS) throw SolicitudInvalida("Estado desconocido: $estado")
        val protocolo = buscar(numero, usuario)
        if (protocolo.estado == estado) return protocolo
        if (protocolo.formalizado) throw Conflicto("El protocolo ${protocolo.numero} ya está formalizado y no cambia de estado")

        // Solo queda un cambio posible: de "Registrado en terreno" a "Formalizado".
        val solicitante = remotos.usuario(usuario)
        if (!solicitante.puedeFormalizar) throw Prohibido("El rol ${solicitante.rol} no puede formalizar protocolos")
        return protocolo.apply {
            this.estado = EstadoProtocolo.FORMALIZADO
            formalizadoPor = solicitante.usuario
            formalizadoEn = reloj()
        }
    }

    fun actualizarObservaciones(numero: String, observaciones: String, usuario: String): Protocolo {
        val protocolo = buscar(numero, usuario)
        if (protocolo.formalizado) throw Conflicto("El protocolo ${protocolo.numero} ya está formalizado y no se puede modificar")
        return protocolo.apply { this.observaciones = observaciones.trim() }
    }

    /** Pruebas del equipo del protocolo, pedidas a pruebas-service con el alcance de quien consulta. */
    @Transactional(readOnly = true)
    fun pruebasDelProtocolo(numero: String, usuario: String): List<PruebaRemota> {
        val protocolo = buscar(numero, usuario)
        return remotos.pruebasDe(usuario, protocolo.equipo)
    }

    /** Solo quien lo creó o un administrador, y nunca si ya está formalizado. */
    fun eliminar(numero: String, usuario: String) {
        val protocolo = buscar(numero, usuario)
        if (protocolo.formalizado) throw Conflicto("El protocolo ${protocolo.numero} ya está formalizado y no se puede eliminar")
        val solicitante = remotos.usuario(usuario)
        if (protocolo.creadoPor != solicitante.usuario && solicitante.rol != "admin") {
            throw Prohibido("Solo quien creó el protocolo o un administrador puede eliminarlo")
        }
        repositorio.delete(protocolo)
    }
}
