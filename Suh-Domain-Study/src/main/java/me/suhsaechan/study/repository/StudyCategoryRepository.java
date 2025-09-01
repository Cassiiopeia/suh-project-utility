package me.suhsaechan.study.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.study.entity.StudyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * StudyCategoryRepository는 스터디 카테고리 정보에 대한 데이터 액세스를 제공합니다.
 */
@Repository
public interface StudyCategoryRepository extends JpaRepository<StudyCategory, UUID> {

    /**
     * 최상위 카테고리 목록 조회 (부모 카테고리가 없는)
     */
    List<StudyCategory> findByParentIsNullOrderByDisplayOrderAsc();

    /**
     * 카테고리 이름으로 카테고리 검색
     */
    Optional<StudyCategory> findByName(String name);

}
