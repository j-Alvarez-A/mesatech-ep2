package cl.duoc.api_cloud_native.repository;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaLocalRepository extends JpaRepository<CategoriaLocal, Long> {

    List<CategoriaLocal> findAllByOrderByNombreAsc();

    Optional<CategoriaLocal> findFirstByNombreIgnoreCase(String nombre);
}
