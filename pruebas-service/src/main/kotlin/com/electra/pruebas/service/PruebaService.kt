package com.electra.pruebas.service

import com.electra.pruebas.client.ServicioUsuarios
import com.electra.pruebas.dto.PruebaRequest
import com.electra.pruebas.exception.NoEncontrado
import com.electra.pruebas.exception.Prohibido
import com.electra.pruebas.exception.SolicitudInvalida
import com.electra.pruebas.model.Prueba
import com.electra.pruebas.repository.PruebaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reglas de negocio de las pruebas. Antes de guardar o consultar le pregunta a usuarios-service
 * (vía Feign) quién es el usuario, así cada uno ve solo lo que le corresponde según su rol.
 */
@Service
@Transactional
class PruebaService(
    private val repositorio: PruebaRepository,
    private val usuarios: ServicioUsuarios,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    fun registrar(datos: PruebaRequest): Prueba {
        val creador = try {
            usuarios.obtener(datos.creadoPor)
        } catch (e: NoEncontrado) {
            throw SolicitudInvalida("El creador ${datos.creadoPor.trim()} no existe en usuarios-service")
        }
        if (creador.rol == "supervisor") {
            throw Prohibido("El rol supervisor no registra pruebas (separación de funciones)")
        }
        val tieneLatitud = datos.latitud != null
        if (tieneLatitud != (datos.longitud != null)) {
            throw SolicitudInvalida("La ubicación debe incluir latitud y longitud, o ninguna")
        }
        return repositorio.save(
            Prueba(
                equipo = datos.equipo.trim(),
                tipoPrueba = datos.tipoPrueba.trim(),
                fecha = datos.fecha,
                hora = datos.hora,
                medicion = datos.medicion,
                unidad = datos.unidad.trim(),
                resultado = datos.resultado,
                observaciones = datos.observaciones.trim(),
                fotoUrl = datos.fotoUrl,
                latitud = datos.latitud,
                longitud = datos.longitud,
                creadoPor = creador.usuario,
                creadoEn = reloj()
            )
        )
    }

    /** Pruebas visibles para [usuario], de la más reciente a la más antigua; [equipo] filtra el historial. */
    @Transactional(readOnly = true)
    fun listar(usuario: String, equipo: String? = null): List<Prueba> {
        val alcance = usuarios.alcanceDe(usuario)
        return repositorio.buscar(alcance.creador, alcance.creadosDesde, equipo?.trim()?.ifBlank { null })
    }

    /** Una prueba que el usuario no puede ver responde igual que si no existiera (404). */
    @Transactional(readOnly = true)
    fun buscar(id: Long, usuario: String): Prueba {
        val alcance = usuarios.alcanceDe(usuario)
        return repositorio.findById(id).orElse(null)?.takeIf { alcance.puedeVer(it) }
            ?: throw NoEncontrado("No existe la prueba $id")
    }

    @Transactional(readOnly = true)
    fun equiposConRegistros(usuario: String): List<String> {
        val alcance = usuarios.alcanceDe(usuario)
        return repositorio.equipos(alcance.creador, alcance.creadosDesde)
    }

    /** Solo quien la registró o un administrador pueden eliminar una prueba. */
    fun eliminar(id: Long, usuario: String) {
        val prueba = buscar(id, usuario)
        val solicitante = usuarios.obtener(usuario)
        if (prueba.creadoPor != solicitante.usuario && solicitante.rol != "admin") {
            throw Prohibido("Solo quien registró la prueba o un administrador puede eliminarla")
        }
        repositorio.delete(prueba)
    }
}
