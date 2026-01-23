class OrderManager {
    static async loadOrders() {
        try {
            console.log('Загрузка заказов...');
            const response = await axios.get(`${new AuthManager().API_BASE}/orders`);
            const orders = response.data;
            const tbody = document.getElementById('ordersTableBody');

            if (tbody) {
                tbody.innerHTML = '';

                if (!orders || orders.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="7" class="text-center">Нет заказов</td></tr>';
                    return;
                }

                orders.forEach(order => {
                    const tr = OrderManager.createOrderRowElement(order);
                    tbody.appendChild(tr);
                });
            }
        } catch (error) {
            console.error('Ошибка загрузки заказов:', error);
            const tbody = document.getElementById('ordersTableBody');
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Ошибка загрузки</td></tr>';
            }
        }
    }

    static createOrderRowElement(order) {
        const tr = document.createElement('tr');
        tr.setAttribute('data-order-id', order.id);

        const createdDate = OrderManager.formatDate(order.createdAt);
        const userName = order.user?.username ||
            order.user?.firstName ||
            order.user?.telegramUsername ||
            'Неизвестный';

        tr.innerHTML = `
            <td>${order.id}</td>
            <td>${userName}</td>
            <td>${order.totalAmount || 0} ₽</td>
            <td>
                <span class="badge ${OrderManager.getStatusBadgeClass(order.status)}">
                    ${OrderManager.getStatusText(order.status)}
                </span>
            </td>
            <td>${createdDate}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewOrder(${order.id})">
                    <i class="bi bi-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-warning" onclick="editOrder(${order.id})">
                    <i class="bi bi-pencil"></i>
                </button>
            </td>
        `;
        return tr;
    }

    static formatDate(dateString) {
        if (!dateString) return '—';

        try {
            const date = new Date(dateString);

            // Проверяем валидность даты
            if (isNaN(date.getTime())) {
                return 'Некорректная дата';
            }

            return date.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            console.error('Ошибка форматирования даты:', error);
            return '—';
        }
    }

    static getStatusBadgeClass(status) {
        if (!status) return 'bg-secondary';

        const statusUpper = status.toUpperCase();
        switch(statusUpper) {
            case 'NEW':
            case 'PENDING':
                return 'bg-info';
            case 'PROCESSING':
            case 'CONFIRMED':
                return 'bg-warning';
            case 'COMPLETED':
            case 'ЗАВЕРШЕН':
                return 'bg-success';
            case 'CANCELLED':
            case 'CANCELED':
                return 'bg-danger';
            default:
                return 'bg-secondary';
        }
    }

    static getStatusText(status) {
        if (!status) return 'Неизвестно';

        const statusUpper = status.toUpperCase();
        switch(statusUpper) {
            case 'NEW': return 'Новый';
            case 'PENDING': return 'Ожидает';
            case 'CONFIRMED': return 'Подтвержден';
            case 'PROCESSING': return 'В обработке';
            case 'COMPLETED':
            case 'ЗАВЕРШЕН':
                return 'Завершен';
            case 'CANCELLED':
            case 'CANCELED':
                return 'Отменен';
            default:
                return status;
        }
    }

    static async searchOrders() {
        const searchInput = document.getElementById('orderSearchInput');
        if (!searchInput) return;

        const query = searchInput.value.trim();
        const tbody = document.getElementById('ordersTableBody');

        if (!tbody) return;

        if (!query) {
            OrderManager.loadOrders();
            return;
        }

        tbody.innerHTML = '<tr><td colspan="7" class="text-center">Поиск...</td></tr>';

        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/orders/search`, {
                params: { username: query }
            });

            const result = response.data;

            tbody.innerHTML = '';

            if (!result || (Array.isArray(result) && result.length === 0)) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center">Ничего не найдено</td></tr>';
                return;
            }

            if (Array.isArray(result)) {
                result.forEach(order => {
                    const tr = OrderManager.createOrderRowElement(order);
                    tbody.appendChild(tr);
                });
            } else {
                const tr = OrderManager.createOrderRowElement(result);
                tbody.appendChild(tr);
            }

            ToastManager.showToast(`Найдено заказов: ${Array.isArray(result) ? result.length : 1}`, 'info');
        } catch (error) {
            console.error('Ошибка поиска заказов:', error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Ошибка при поиске</td></tr>';
            ToastManager.showToast('Ошибка при поиске', 'danger');
        }
    }

    static async viewOrder(orderId) {
        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/orders/${orderId}`);
            const order = response.data;

            OrderManager.showOrderModal(order, 'view');
        } catch (error) {
            console.error('Ошибка загрузки заказа:', error);
            ToastManager.showToast('Не удалось загрузить заказ', 'danger');
        }
    }

    static async editOrder(orderId) {
        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/orders/${orderId}`);
            const order = response.data;

            OrderManager.showOrderModal(order, 'edit');
        } catch (error) {
            console.error('Ошибка загрузки заказа:', error);
            ToastManager.showToast('Не удалось загрузить заказ', 'danger');
        }
    }

    static showOrderModal(order, mode = 'view') {
        // Удаляем старую модалку
        const oldModal = document.getElementById('orderModal');
        if (oldModal) oldModal.remove();

        // Подготавливаем HTML для позиций заказа
        let orderItemsHtml;
        if (order.items && order.items.length > 0) {
            orderItemsHtml = order.items.map(item => `
                <tr>
                    <td>${item.product?.name || 'Неизвестный продукт'}</td>
                    <td>${item.quantity || 1}</td>
                    <td>${item.price || 0} ₽</td>
                    <td>${(item.quantity || 1) * (item.price || 0)} ₽</td>
                    ${mode === 'edit' ? `
                    <td>
                        <button class="btn btn-sm btn-outline-danger" 
                                onclick="OrderManager.removeOrderItem(${order.id}, ${item.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                    ` : ''}
                </tr>
            `).join('');
        } else {
            orderItemsHtml = '<tr><td colspan="5" class="text-center">Нет позиций</td></tr>';
        }

        // Статусы для выпадающего списка
        const statusOptions = [
            { value: 'PENDING', label: 'Ожидает' },
            { value: 'CONFIRMED', label: 'Подтвержден' },
            { value: 'PROCESSING', label: 'В обработке' },
            { value: 'COMPLETED', label: 'Завершен' },
            { value: 'CANCELLED', label: 'Отменен' }
        ];

        const statusSelect = statusOptions.map(option =>
            `<option value="${option.value}" ${order.status === option.value ? 'selected' : ''}>
                ${option.label}
            </option>`
        ).join('');

        const modalHtml = `
        <div class="modal fade" id="orderModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            ${mode === 'view' ? 'Просмотр заказа' : 'Редактирование заказа'} #${order.id}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <h6>Информация о заказе</h6>
                                <p><strong>Пользователь:</strong> ${order.user?.username || order.user?.firstName || 'Неизвестный'}</p>
                                <p><strong>Telegram ID:</strong> ${order.user?.telegramId || '—'}</p>
                                <p><strong>Телефон:</strong> ${order.user?.phoneNumber || '—'}</p>
                                <p><strong>Дата создания:</strong> ${OrderManager.formatDate(order.createdAt)}</p>
                            </div>
                            <div class="col-md-6">
                                <h6>Статус заказа</h6>
                                ${mode === 'edit' ? `
                                <select class="form-select mb-3" id="orderStatusSelect">
                                    ${statusSelect}
                                </select>
                                ` : `
                                <span class="badge ${OrderManager.getStatusBadgeClass(order.status)} fs-6">
                                    ${OrderManager.getStatusText(order.status)}
                                </span>
                                `}
                                <p class="mt-3"><strong>Общая сумма:</strong> <span id="orderTotalAmount">${order.totalAmount || 0}</span> ₽</p>
                                <p><strong>Адрес доставки:</strong> ${order.deliveryAddress || 'Не указан'}</p>
                            </div>
                        </div>

                        <h6>Состав заказа</h6>
                        <div class="table-responsive">
                            <table class="table table-sm">
                                <thead>
                                    <tr>
                                        <th>Продукт</th>
                                        <th>Количество</th>
                                        <th>Цена за шт.</th>
                                        <th>Сумма</th>
                                        ${mode === 'edit' ? '<th>Действия</th>' : ''}
                                    </tr>
                                </thead>
                                <tbody id="orderItemsTableBody">
                                    ${orderItemsHtml}
                                </tbody>
                                <tfoot>
                                    <tr>
                                        <td colspan="3" class="text-end"><strong>Итого:</strong></td>
                                        <td><strong id="orderItemsTotal">${order.totalAmount || 0}</strong> ₽</td>
                                        ${mode === 'edit' ? '<td></td>' : ''}
                                    </tr>
                                </tfoot>
                            </table>
                        </div>

                        <div class="mt-3">
                            <h6>Комментарий к заказу</h6>
                            <p>${order.comment || 'Нет комментария'}</p>
                        </div>

                        <div id="orderError" class="alert alert-danger d-none mt-3"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            Закрыть
                        </button>
                        ${mode === 'edit' ? `
                        <button type="button" class="btn btn-primary" onclick="OrderManager.updateOrder(${order.id})">
                            Сохранить изменения
                        </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);

        const modal = new bootstrap.Modal(document.getElementById('orderModal'));
        modal.show();
    }

    static async updateOrder(orderId) {
        const statusSelect = document.getElementById('orderStatusSelect');
        if (!statusSelect) return;

        const newStatus = statusSelect.value;

        try {
            const response = await axios.put(`${new AuthManager().API_BASE}/orders/${orderId}`, {
                status: newStatus
            });

            const updatedOrder = response.data;

            // Обновляем строку в таблице
            OrderManager.updateOrderRow(updatedOrder);

            // Закрываем модалку
            const modalEl = document.getElementById('orderModal');
            bootstrap.Modal.getInstance(modalEl).hide();

            ToastManager.showToast('Заказ обновлен', 'success');
        } catch (error) {
            console.error('Ошибка обновления заказа:', error);
            const errorBlock = document.getElementById('orderError');
            if (errorBlock) {
                errorBlock.textContent = error.response?.data?.message || 'Ошибка при обновлении';
                errorBlock.classList.remove('d-none');
            }
            ToastManager.showToast('Ошибка при обновлении заказа', 'danger');
        }
    }

    static async removeOrderItem(orderId, itemId) {
        if (!confirm('Удалить позицию из заказа?')) return;

        try {
            await axios.delete(`${new AuthManager().API_BASE}/orders/${orderId}/items/${itemId}`);

            // Перезагружаем данные заказа
            const response = await axios.get(`${new AuthManager().API_BASE}/orders/${orderId}`);
            const updatedOrder = response.data;

            // Обновляем таблицу в модалке
            const tableBody = document.getElementById('orderItemsTableBody');
            if (tableBody) {
                let itemsHtml;
                if (updatedOrder.items && updatedOrder.items.length > 0) {
                    itemsHtml = updatedOrder.items.map(item => `
                        <tr>
                            <td>${item.product?.name || 'Неизвестный продукт'}</td>
                            <td>${item.quantity || 1}</td>
                            <td>${item.price || 0} ₽</td>
                            <td>${(item.quantity || 1) * (item.price || 0)} ₽</td>
                            <td>
                                <button class="btn btn-sm btn-outline-danger" 
                                        onclick="OrderManager.removeOrderItem(${orderId}, ${item.id})">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>
                    `).join('');
                } else {
                    itemsHtml = '<tr><td colspan="5" class="text-center">Нет позиций</td></tr>';
                }
                tableBody.innerHTML = itemsHtml;
            }

            // Обновляем общую сумму в модалке
            const totalAmountElement = document.getElementById('orderTotalAmount');
            const itemsTotalElement = document.getElementById('orderItemsTotal');

            if (totalAmountElement) {
                totalAmountElement.textContent = updatedOrder.totalAmount || 0;
            }

            if (itemsTotalElement) {
                itemsTotalElement.textContent = updatedOrder.totalAmount || 0;
            }

            // Обновляем строку в основной таблице
            OrderManager.updateOrderRow(updatedOrder);

            ToastManager.showToast('Позиция удалена', 'success');
        } catch (error) {
            console.error('Ошибка удаления позиции:', error);
            ToastManager.showToast('Не удалось удалить позицию', 'danger');
        }
    }

    static updateOrderRow(order) {
        const tbody = document.getElementById('ordersTableBody');
        if (!tbody) return;

        const existingRow = tbody.querySelector(`tr[data-order-id="${order.id}"]`);
        const newRow = OrderManager.createOrderRowElement(order);

        if (existingRow) {
            existingRow.replaceWith(newRow);
        }
    }

    // Вспомогательные методы для поиска по DOM
    static findElementByText(element, text) {
        const elements = element.querySelectorAll('*');
        for (let el of elements) {
            if (el.textContent && el.textContent.includes(text)) {
                return el;
            }
        }
        return null;
    }
}

// Глобальные функции для совместимости с HTML
function viewOrder(orderId) {
    OrderManager.viewOrder(orderId);
}

function editOrder(orderId) {
    OrderManager.editOrder(orderId);
}

function searchOrders() {
    OrderManager.searchOrders();
}

function loadOrders() {
    OrderManager.loadOrders();
}
