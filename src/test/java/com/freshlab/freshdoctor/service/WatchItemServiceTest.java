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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchItemServiceTest {

    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    private WatchItemService service;

    @BeforeEach
    void setUp() {
        service = new WatchItemService(userItemRepository, userRepository, itemRepository);
    }

    @Test
    void getsOnlyCurrentUsersWatchItems() {
        User user = user(1L);
        UserItem userItem = userItem(user, item("1001", "배추"), true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userItemRepository.findByUserUserIdOrderByItemItemCodeAsc(1L))
                .thenReturn(List.of(userItem));

        List<WatchItemResponse> result = service.getWatchItems(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemCode()).isEqualTo("1001");
        verify(userItemRepository).findByUserUserIdOrderByItemItemCodeAsc(1L);
    }

    @Test
    void addsWatchItemWithNotificationEnabled() {
        User user = user(1L);
        Item item = item("1001", "배추");
        when(userItemRepository.existsByUserUserIdAndItemItemCode(1L, "1001")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById("1001")).thenReturn(Optional.of(item));
        when(userItemRepository.saveAndFlush(any(UserItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WatchItemResponse result = service.addWatchItem(1L, "1001");

        assertThat(result.itemCode()).isEqualTo("1001");
        assertThat(result.notificationEnabled()).isTrue();
    }

    @Test
    void rejectsDuplicateWatchItem() {
        when(userItemRepository.existsByUserUserIdAndItemItemCode(1L, "1001")).thenReturn(true);

        assertThatThrownBy(() -> service.addWatchItem(1L, "1001"))
                .isInstanceOf(DuplicateWatchItemException.class);
        verify(userItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsItemOutsideMvpFive() {
        assertThatThrownBy(() -> service.addWatchItem(1L, "9999"))
                .isInstanceOf(UnsupportedWatchItemException.class);
        verify(userItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void deletesCurrentUsersWatchItem() {
        UserItem userItem = userItem(user(1L), item("1001", "배추"), true);
        when(userItemRepository.findByUserUserIdAndItemItemCode(1L, "1001"))
                .thenReturn(Optional.of(userItem));

        service.deleteWatchItem(1L, "1001");

        verify(userItemRepository).delete(userItem);
    }

    private User user(Long id) {
        User user = new User();
        user.setUserId(id);
        return user;
    }

    private Item item(String code, String name) {
        Item item = new Item();
        item.setItemCode(code);
        item.setItemName(name);
        item.setActive(true);
        return item;
    }

    private UserItem userItem(User user, Item item, boolean notificationEnabled) {
        UserItem userItem = new UserItem();
        userItem.setUser(user);
        userItem.setItem(item);
        userItem.setNotificationEnabled(notificationEnabled);
        return userItem;
    }
}
