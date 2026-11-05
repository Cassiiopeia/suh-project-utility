package me.suhsaechan.clipboard.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.clipboard.entity.ClipboardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClipboardItemRepository extends JpaRepository<ClipboardItem, UUID> {

  List<ClipboardItem> findAllByOrderByCreatedDateDesc();
}
