package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.stereotype.Service;

@Service
public class SyrupPriceService {

    /**
     * Проверяет, является ли продукт сиропом
     */
    public boolean isSyrup(Product product) {
        return product.getCategory() != null &&
                (product.getCategory().getName().toLowerCase().contains("сироп") ||
                        product.getName().toLowerCase().contains("сироп"));
    }

    public boolean isSyrup(ProductResponseDto product) {
        if (product == null) {
            return false;
        }

        String productName = product.getName() != null ? product.getName().toLowerCase() : "";
        String categoryName = product.getCategory() != null && product.getCategory().getName() != null
                ? product.getCategory().getName().toLowerCase()
                : "";

        return categoryName.contains("сироп") || productName.contains("сироп");
    }

    /**
     * Рассчитывает цену сиропа на основе объема основного напитка
     */
    public Long calculateSyrupPriceForSize(Product syrupProduct, Product mainProduct) {
        long basePrice = syrupProduct.getAmount();

        if (mainProduct.getSize() != null) {
            try {
                String sizeStr = mainProduct.getSize().replaceAll("[^0-9]", "");
                if (!sizeStr.isEmpty()) {
                    int volume = Integer.parseInt(sizeStr);

                    if (volume > 200) {
                        return 50L;
                    } else {
                        return 35L;
                    }
                }
            } catch (NumberFormatException e) {
                return basePrice;
            }
        }
        return basePrice;
    }

    public Long calculateSyrupPriceForSize(ProductResponseDto syrupProduct, Product mainProduct) {
        if (syrupProduct == null) {
            return 0L;
        }

        long basePrice = syrupProduct.getAmount() != null ? syrupProduct.getAmount() : 0L;
        if (mainProduct == null || mainProduct.getSize() == null) {
            return basePrice;
        }

        try {
            String sizeStr = mainProduct.getSize().replaceAll("[^0-9]", "");
            if (!sizeStr.isEmpty()) {
                int volume = Integer.parseInt(sizeStr);
                return volume > 200 ? 50L : 35L;
            }
        } catch (NumberFormatException e) {
            return basePrice;
        }

        return basePrice;
    }

    public Long calculateSyrupPriceForSize(Product syrupProduct, ProductResponseDto mainProduct) {
        long basePrice = syrupProduct.getAmount() != null ? syrupProduct.getAmount() : 0L;
        if (mainProduct == null || mainProduct.getSize() == null) {
            return basePrice;
        }

        try {
            String sizeStr = mainProduct.getSize().replaceAll("[^0-9]", "");
            if (!sizeStr.isEmpty()) {
                int volume = Integer.parseInt(sizeStr);
                return volume > 200 ? 50L : 35L;
            }
        } catch (NumberFormatException e) {
            return basePrice;
        }

        return basePrice;
    }

    public Long calculateSyrupPriceForSize(ProductResponseDto syrupProduct, ProductResponseDto mainProduct) {
        if (syrupProduct == null) {
            return 0L;
        }

        long basePrice = syrupProduct.getAmount() != null ? syrupProduct.getAmount() : 0L;
        if (mainProduct == null || mainProduct.getSize() == null) {
            return basePrice;
        }

        try {
            String sizeStr = mainProduct.getSize().replaceAll("[^0-9]", "");
            if (!sizeStr.isEmpty()) {
                int volume = Integer.parseInt(sizeStr);
                return volume > 200 ? 50L : 35L;
            }
        } catch (NumberFormatException e) {
            return basePrice;
        }

        return basePrice;
    }
}
