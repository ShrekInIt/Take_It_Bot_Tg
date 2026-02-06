package com.example.bot.Telegram_bot_take_it.admin.repository;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью AdminUser.
 * <p>
 * Наследуется от JpaRepository и автоматически получает стандартные CRUD-операции:
 *  - сохранение (save)
 *  - поиск по ID (findById)
 *  - получение всех записей (findAll)
 *  - удаление (delete и др.)
 * <p>
 * Дополнительно содержит кастомные методы поиска администратора по username.
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * Ищет администратора по имени пользователя (username).
     * <p>
     * Spring Data JPA автоматически генерирует SQL-запрос
     * на основе названия метода.
     *
     * @param username логин администратора
     * @return Optional с найденным AdminUser или пустой, если не найден
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * Проверяет, существует ли администратор с указанным username.
     * <p>
     * Обычно используется для валидации при создании нового администратора,
     * чтобы не допустить дублирование логинов.
     *
     * @param username логин администратора
     * @return true — если такой администратор существует, false — если нет
     */
    boolean existsByUsername(String username);
}
