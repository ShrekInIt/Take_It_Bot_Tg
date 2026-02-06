/* global axios, bootstrap */
/**
 * @typedef {Object} OrderDto
 * @property {number} id
 * @typedef {{ name?:string, quantity?:number, price?:number }} AddonDto
 * @typedef {{ id:number, quantity?:number, price?:number, product?:ProductDto, addons?:AddonDto[] }} OrderItemDto
 * @typedef {{
 *   id:number,
 *   createdAt?: any,
 *   status?:string,
 *   totalAmount?:number,
 *   userName?:string,
 *   user?:UserDto,
 *   deliveryType?:string,
 *   deliveryAddress?:string,
 *   comment?:string,
 *   items?:OrderItemDto[]
 * }} OrderDto
 */
/**
 * @typedef {{ hide(): void, show(): void, dispose(): void }} BsModalInstance
 * @typedef {{ getInstance(el: Element): (BsModalInstance|null), new(el: Element): BsModalInstance }} BsModalCtor
 * @typedef {{ Modal: BsModalCtor }} BootstrapNs
 */
/** @type {BootstrapNs} */

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

    /** @param {OrderDto} order */
    static createOrderRowElement(order) {
        const tr = document.createElement('tr');
        tr.setAttribute('data-order-id', String(order.id));

        const createdDate = OrderManager.formatDate(order.createdAt);
        console.log(order);
        const userName =
            order.userName ||
            order.user?.name ||
            order.user?.username ||
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

    /** @param {any} value @returns {string} */
    static formatDate(value) {
        if (!value) return '—';

        try {
            let date;

            if (Array.isArray(value) && value.length >= 6) {
                const [
                    year,
                    month,
                    day,
                    hour,
                    minute,
                    second,
                    nano = 0
                ] = value;

                date = new Date(
                    year,
                    month - 1,
                    day,
                    hour,
                    minute,
                    second,
                    Math.floor(nano / 1_000_000)
                );
            }
            else if (value instanceof Date) {
                date = value;
            }
            else if (typeof value === 'number') {
                date = new Date(
                    String(value).length === 10 ? value * 1000 : value
                );
            }
            else if (typeof value === 'string') {
                date = new Date(value.includes(' ')
                    ? value.replace(' ', 'T')
                    : value
                );
            } else {
                console.log('Неизвестный формат даты:', value);
                return '—';
            }

            if (isNaN(date.getTime())) {
                console.log('Невалидная дата:', value);
                return 'Некорректная дата';
            }

            return date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });

        } catch (e) {
            console.error('Ошибка форматирования даты:', e);
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
            await OrderManager.loadOrders();
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

    /** @param {OrderDto} order
     * @param mode
     */
    static showOrderModal(order, mode = 'view') {

        const oldModal = document.getElementById('orderModal');
        if (oldModal) {
            const instance = bootstrap.Modal.getInstance(oldModal);
            if (instance) {
                instance.dispose();
            }
            oldModal.remove();
        }
        let orderItemsHtml;

        if (order.items && order.items.length > 0) {
            orderItemsHtml = order.items.map(item => {
                const productName = item.product?.name || 'Неизвестный продукт';

                const baseRow = `
                <tr>
                    <td>${productName}</td>
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
            `;

                let addonsHtml = '';
                if (item.addons && item.addons.length > 0) {
                    addonsHtml = item.addons.map(addon => `
                    <tr class="table-secondary">
                        <td>— ${addon.name || 'Неизвестная добавка'}</td>
                        <td>${addon.quantity || 1}</td>
                        <td>${addon.price || 0} ₽</td>
                        <td>${(addon.quantity || 1) * (addon.price || 0)} ₽</td>
                        ${mode === 'edit' ? '<td></td>' : ''}
                    </tr>
                `).join('');
                }

                return baseRow + addonsHtml;
            }).join('');
        } else {
            orderItemsHtml = '<tr><td colspan="5" class="text-center">Нет позиций</td></tr>';
        }

        const statusOptions = [
            { value: 'PENDING', label: 'Ожидает' },
            { value: 'CONFIRMED', label: 'Подтвержден' },
            { value: 'PROCESSING', label: 'В обработке' },
            { value: 'COMPLETED', label: 'Завершен' },
            { value: 'CANCELLED', label: 'Отменен' }
        ];

        const statusSelect = statusOptions.map(opt =>
            `<option value="${opt.value}" ${order.status === opt.value ? 'selected' : ''}>
            ${opt.label}
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
                                <p><strong>Пользователь:</strong> ${order.user?.username || 'Неизвестный'}</p>
                                <p><strong>Телефон:</strong> ${order.user?.phoneNumber || '—'}</p>
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
                                <p class="mt-3">
                                    <strong>Общая сумма:</strong>
                                    <span id="orderTotalAmount">${order.totalAmount || 0}</span> ₽
                                </p>
                                <p><strong>Тип доставки:</strong> ${OrderManager.getDeliveryTypeText(order.deliveryType)}</p>

                                ${order.deliveryType === 'DELIVERY'
            ? `<p><strong>Адрес доставки:</strong> ${order.deliveryAddress || '—'}</p>`
            : ''}
                            </div>
                        </div>

                        <h6>Состав заказа</h6>
                        <div class="table-responsive">
                            <table class="table table-sm">
                                <thead>
                                    <tr>
                                        <th>Продукт</th>
                                        <th>Количество</th>
                                        <th>Цена</th>
                                        <th>Сумма</th>
                                        ${mode === 'edit' ? '<th></th>' : ''}
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
                            <h6>Комментарий</h6>
                            <p>${order.comment || 'Нет комментария'}</p>
                        </div>

                        <div id="orderError" class="alert alert-danger d-none mt-3"></div>
                    </div>

                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
                        ${mode === 'edit' ? `
                            <button class="btn btn-primary"
                                    onclick="OrderManager.updateOrder(${order.id})">
                                Сохранить
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

    static getDeliveryTypeText(type) {
        if (!type) return '—';

        switch (type.toUpperCase()) {
            case 'PICKUP':
                return 'Самовывоз';
            case 'DELIVERY':
                return 'Доставка';
            default:
                return type;
        }
    }


    /**
     * @param orderId
     */
    static async updateOrder(orderId) {
        const statusSelect = document.getElementById('orderStatusSelect');
        if (!statusSelect) return;

        const newStatus = statusSelect.value;

        try {
            const response = await axios.put(`${new AuthManager().API_BASE}/orders/${orderId}`, {
                status: newStatus
            });

            const updatedOrder = response.data;

            OrderManager.updateOrderRow(updatedOrder);

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

            const response = await axios.get(`${new AuthManager().API_BASE}/orders/${orderId}`);
            const updatedOrder = response.data;

            const tableBody = document.getElementById('orderItemsTableBody');
            if (tableBody) {
                let itemsHtml;
                if (updatedOrder.items && updatedOrder.items.length > 0) {
                    itemsHtml = updatedOrder.items.map(item => {
                        const baseRow = `
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
    `;

                        let addonsHtml = '';
                        if (item.addons && item.addons.length > 0) {
                            addonsHtml = item.addons.map(addon => `
            <tr class="table-secondary">
                <td>— ${addon.name}</td>
                <td>${addon.quantity || 1}</td>
                <td>${addon.price || 0} ₽</td>
                <td>${(addon.quantity || 1) * (addon.price || 0)} ₽</td>
                <td></td>
            </tr>
        `).join('');
                        }

                        return baseRow + addonsHtml;
                    }).join('');
                } else {
                    itemsHtml = '<tr><td colspan="5" class="text-center">Нет позиций</td></tr>';
                }
                tableBody.innerHTML = itemsHtml;
            }

            const totalAmountElement = document.getElementById('orderTotalAmount');
            const itemsTotalElement = document.getElementById('orderItemsTotal');

            if (totalAmountElement) {
                totalAmountElement.textContent = String(updatedOrder.totalAmount || 0);
            }

            if (itemsTotalElement) {
                itemsTotalElement.textContent = String(updatedOrder.totalAmount || 0);
            }

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
}

function viewOrder(orderId) {
    OrderManager.viewOrder(orderId).catch(console.error);
}

function editOrder(orderId) {
    OrderManager.editOrder(orderId).catch(console.error);
}

function searchOrders() {
    OrderManager.searchOrders().catch(console.error);
}

function loadOrders() {
    OrderManager.loadOrders().catch(console.error);
}
