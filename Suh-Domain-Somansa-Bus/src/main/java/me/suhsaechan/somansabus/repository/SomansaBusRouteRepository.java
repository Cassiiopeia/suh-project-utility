package me.suhsaechan.somansabus.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SomansaBusRouteRepository extends JpaRepository<SomansaBusRoute, UUID> {

  List<SomansaBusRoute> findByIsActiveTrue();

  List<SomansaBusRoute> findByCaralias(String caralias);

  List<SomansaBusRoute> findByCaraliasAndIsActiveTrue(String caralias);

  Optional<SomansaBusRoute> findByDisptid(Integer disptid);

  List<SomansaBusRoute> findByIsActiveTrueOrderByDepartureTimeAsc();

  @Query("SELECT MAX(r.updatedDate) FROM SomansaBusRoute r")
  Optional<LocalDateTime> findMaxUpdatedDate();
}
