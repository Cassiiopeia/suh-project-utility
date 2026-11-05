package me.suhsaechan.idea.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.idea.entity.IdeaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdeaItemRepository extends JpaRepository<IdeaItem, UUID> {

  List<IdeaItem> findAllByOrderByCreatedDateDesc();

  List<IdeaItem> findTop5ByOrderByCreatedDateDesc();
}
