package cl.duoc.api_cloud_native.repository;

import cl.duoc.api_cloud_native.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Recupera las solicitudes filtrando por el correo del usuario
    List<Solicitud> findByUsuarioSolicitante(String usuarioSolicitante);
}