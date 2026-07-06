package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUserUserId(Long userId);

    boolean existsByUserUserIdAndItemItemCode(Long userId, String itemCode);

    void deleteByUserUserIdAndItemItemCode(Long userId, String itemCode);
}
