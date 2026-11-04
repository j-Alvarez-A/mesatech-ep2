package cl.duoc.api_cloud_native.Repository;

import cl.duoc.api_cloud_native.entity.ItemCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Long> {
    List<ItemCatalogo> findByTipo(String tipo);
}