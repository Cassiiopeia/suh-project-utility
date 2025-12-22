package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SomansaBusRouteRepository extends JpaRepository<SomansaBusRoute, UUID> {

  List<SomansaBusRoute> findByIsActiveTrue();

  List<SomansaBusRoute> findByCaralias(String caralias);

  List<SomansaBusRoute> findByCaraliasAndIsActiveTrue(String caralias);

  Optional<SomansaBusRoute> findByDisptid(Integer disptid);

  List<SomansaBusRoute> findByIsActiveTrueOrderByDepartureTimeAsc();
}
