package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUserUserIdOrderByItemItemCodeAsc(Long userId);

    boolean existsByUserUserIdAndItemItemCode(Long userId, String itemCode);

    void deleteByUserUserIdAndItemItemCode(Long userId, String itemCode);

    java.util.Optional<UserItem> findByUserUserIdAndItemItemCode(Long userId, String itemCode);

    List<UserItem> findByItemItemCodeAndNotificationEnabledTrue(String itemCode);
}
