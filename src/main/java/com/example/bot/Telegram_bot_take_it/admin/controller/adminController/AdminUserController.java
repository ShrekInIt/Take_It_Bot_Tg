package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST-контроллер админ-панели для управления пользователями.
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Предоставляет операции:
 *  - получение списка пользователей (все / последние)
 *  - получение пользователя по ID
 *  - поиск пользователей по имени
 *  - создание пользователя
 *  - обновление данных пользователя
 *  - удаление пользователя
 * <p>
 * Ответы для админки возвращаются в формате AdminUserDto.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    /**
     * Сервис с бизнес-логикой работы с пользователями:
     * получение, поиск, создание, обновление, удаление.
     */
    private final UserService userService;

    /**
     * Возвращает список всех пользователей в формате, удобном для админки.
     *
     * @return список пользователей в виде AdminUserDto
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUserDto());
    }

    /**
     * Возвращает список последних (недавно появившихся) пользователей.
     * <p>
     * В текущей реализации возвращает 10 последних пользователей.
     *
     * @return список последних пользователей в виде AdminUserDto
     */
    @GetMapping("/users/recent")
    public ResponseEntity<List<AdminUserDto>> getRecentUsers() {
        return ResponseEntity.ok(userService.findRecentUsersDto(10));
    }

    /**
     * Возвращает пользователя по его ID.
     * <p>
     * Если пользователь не найден — возвращает 404 Not Found.
     *
     * @param id идентификатор пользователя
     * @return найденный пользователь (AdminUserDto) или 404
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        Optional<AdminUserDto> user = userService.findByIdUserDto(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ищет пользователей по имени (параметр name).
     * <p>
     * Сервис возвращает список пользователей, затем они преобразуются в AdminUserDto
     * через OrderMapper.toDtoUser(...), чтобы отдать результат в формате админки.
     *
     * @param name строка поиска по имени пользователя
     * @return список найденных пользователей в виде AdminUserDto
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<AdminUserDto>> search(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                userService.searchByName(name)
                        .stream()
                        .map(OrderMapper::toDtoUser)
                        .toList()
        );
    }

    /**
     * Обновляет данные пользователя по ID.
     * <p>
     * Принимает JSON-объект с обновляемыми полями (Map<String, Object>),
     * передаёт их в userService.update(...), затем возвращает обновлённого пользователя в виде AdminUserDto.
     *
     * @param id  идентификатор пользователя
     * @param req набор обновляемых полей (ключи/значения из тела запроса)
     * @return обновлённый пользователь в виде AdminUserDto
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> update(@PathVariable Long id, @RequestBody Map<String,Object> req) {
        User updated = userService.update(id, req);
        return ResponseEntity.ok(OrderMapper.toDtoUser(updated));
    }

    /**
     * Удаляет пользователя по ID.
     *
     * @param id идентификатор пользователя
     * @return HTTP 200 OK после удаления
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Создаёт нового пользователя.
     * <p>
     * Принимает JSON-объект с данными пользователя (Map<String, Object>),
     * создаёт пользователя через userService.create(...),
     * и возвращает созданного пользователя в формате AdminUserDto.
     *
     * @param req данные для создания пользователя (ключи/значения из тела запроса)
     * @return созданный пользователь в виде AdminUserDto
     */
    @PostMapping("/users")
    public ResponseEntity<AdminUserDto> create(@RequestBody Map<String, Object> req) {
        User created = userService.create(req);
        return ResponseEntity.ok(OrderMapper.toDtoUser(created));
    }
}
