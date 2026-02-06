package com.example.bot.Telegram_bot_take_it;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import com.example.bot.Telegram_bot_take_it.utils.ConfectioneryBotClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserService userService;
    @Mock private CartService cartService;
    @Mock private ProductService productService;
    @Mock private ConfectioneryBotClient confectioneryBotClient;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setup() {

    }

    @Test
    void createOrderFromCart_success_createsOrder_deductsStock_sendsToConfectionery() {
        Long chatId = 100L;

        User user = mock(User.class);
        when(user.getName()).thenReturn("Test User");
        when(userService.getUserByChatId(chatId)).thenReturn(Optional.of(user));

        Product product = mock(Product.class);
        when(product.getName()).thenReturn("Cake");
        when(product.getCount()).thenReturn(10);
        when(product.getAmount()).thenReturn(250);
        CartItem cartItem = mock(CartItem.class);
        when(cartItem.getProduct()).thenReturn(product);
        when(cartItem.getCountProduct()).thenReturn(2);
        when(cartItem.getAddons()).thenReturn(List.of());

        when(cartService.getCartItems(chatId)).thenReturn(List.of(cartItem));
        when(cartService.getCartTotal(chatId)).thenReturn(500);

        OrderData orderData = OrderData.builder()
                .deliveryType("PICKUP")
                .phoneNumber("+79990000000")
                .address(null)
                .comments("Без сахара")
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            try {
                o.setId(1L);
            } catch (Exception ignore) {}
            return o;
        });

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.createOrderFromCart(chatId, orderData);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));


        verify(product).setCount(10 - 2);
        verify(productService).saveProduct(product);

        verify(confectioneryBotClient, times(1)).sendOrderToConfectionery(any());

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getTotalAmount()).isEqualTo(500);
        assertThat(result.getDeliveryType()).isEqualTo(Order.DeliveryType.PICKUP);
    }

    @Test
    void createOrderFromCart_throwsWhenCartEmpty() {
        Long chatId = 200L;

        User user = mock(User.class);
        when(userService.getUserByChatId(chatId)).thenReturn(Optional.of(user));
        when(cartService.getCartItems(chatId)).thenReturn(List.of());

        OrderData orderData = OrderData.builder()
                .deliveryType("PICKUP")
                .phoneNumber("+79990000000")
                .build();

        assertThatThrownBy(() -> orderService.createOrderFromCart(chatId, orderData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Корзина пуста");

        verify(orderRepository, never()).save(any());
        verify(confectioneryBotClient, never()).sendOrderToConfectionery(any());
    }

    @Test
    void createOrderFromCart_throwsWhenNotEnoughStock() {
        Long chatId = 300L;

        User user = mock(User.class);
        when(userService.getUserByChatId(chatId)).thenReturn(Optional.of(user));

        Product product = mock(Product.class);
        when(product.getName()).thenReturn("Cake");
        when(product.getCount()).thenReturn(1);

        CartItem cartItem = mock(CartItem.class);
        when(cartItem.getProduct()).thenReturn(product);
        when(cartItem.getCountProduct()).thenReturn(2);


        when(cartService.getCartItems(chatId)).thenReturn(List.of(cartItem));

        OrderData orderData = OrderData.builder()
                .deliveryType("PICKUP")
                .phoneNumber("+79990000000")
                .build();

        assertThatThrownBy(() -> orderService.createOrderFromCart(chatId, orderData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("недоступен");

        verify(orderRepository, never()).save(any());
        verify(productService, never()).saveProduct(any());
        verify(confectioneryBotClient, never()).sendOrderToConfectionery(any());
    }
}
