package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.domain.UserItem;
import com.freshlab.freshdoctor.dto.WatchItemResponse;
import com.freshlab.freshdoctor.exception.DuplicateWatchItemException;
import com.freshlab.freshdoctor.exception.UnsupportedWatchItemException;
import com.freshlab.freshdoctor.exception.WatchItemNotFoundException;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import com.freshlab.freshdoctor.repository.UserRepository;
import com.freshlab.freshdoctor.security.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WatchItemService {

    private static final Set<String> MVP_ITEM_CODES = Set.of("1001", "1002", "1003", "1004", "1005");

    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<WatchItemResponse> getWatchItems(Long userId) {
        requireUser(userId);
        return userItemRepository.findByUserUserIdOrderByItemItemCodeAsc(userId).stream()
                .map(WatchItemResponse::from)
                .toList();
    }

    @Transactional
    public WatchItemResponse addWatchItem(Long userId, String itemCode) {
        validateMvpItemCode(itemCode);
        if (userItemRepository.existsByUserUserIdAndItemItemCode(userId, itemCode)) {
            throw new DuplicateWatchItemException();
        }

        User user = requireUser(userId);
        Item item = itemRepository.findById(itemCode)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(UnsupportedWatchItemException::new);

        UserItem userItem = new UserItem();
        userItem.setUser(user);
        userItem.setItem(item);
        userItem.setNotificationEnabled(true);
        try {
            return WatchItemResponse.from(userItemRepository.saveAndFlush(userItem));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateWatchItemException();
        }
    }

    @Transactional
    public void deleteWatchItem(Long userId, String itemCode) {
        UserItem userItem = userItemRepository.findByUserUserIdAndItemItemCode(userId, itemCode)
                .orElseThrow(WatchItemNotFoundException::new);
        userItemRepository.delete(userItem);
    }

    @Transactional
    public WatchItemResponse updateNotification(Long userId, String itemCode, boolean enabled) {
        UserItem userItem = userItemRepository.findByUserUserIdAndItemItemCode(userId, itemCode)
                .orElseThrow(WatchItemNotFoundException::new);
        userItem.setNotificationEnabled(enabled);
        return WatchItemResponse.from(userItem);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(InvalidTokenException::new);
    }

    private void validateMvpItemCode(String itemCode) {
        if (!MVP_ITEM_CODES.contains(itemCode)) {
            throw new UnsupportedWatchItemException();
        }
    }
}
