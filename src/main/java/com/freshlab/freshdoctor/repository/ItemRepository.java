package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, String> {

    List<Item> findByActiveTrueOrderByItemNameAsc();

    List<Item> findByActiveTrueAndItemNameContainingIgnoreCaseOrderByItemNameAsc(String keyword);

    List<Item> findByCategoryOrderByItemNameAsc(String category);
}
