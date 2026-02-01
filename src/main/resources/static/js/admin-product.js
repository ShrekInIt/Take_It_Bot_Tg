class ProductManager {
    static API_BASE() { return `${new AuthManager().API_BASE}/products`; }
    static _inited = false;

    static init() {
        if (this._inited) return;
        this._inited = true;

        document.addEventListener('click', (e) => {
            const editBtn = e.target.closest('.js-edit-product');
            if (editBtn) return ProductManager.showProductModalById(Number(editBtn.dataset.id), 'edit');

            const deleteBtn = e.target.closest('.js-delete-product');
            if (deleteBtn) return ProductManager.deleteProduct(Number(deleteBtn.dataset.id));

            const createOpen = e.target.closest('#openCreateProductModal') || e.target.closest('[data-action="open-create-product"]');
            if (createOpen) return ProductManager.showProductModal({}, 'create');

            const searchBtn = e.target.closest('#searchProductBtn') || e.target.closest('[data-action="search-products"]');
            if (searchBtn) return ProductManager.searchProducts();

            const resetBtn = e.target.closest('#resetProductBtn') || e.target.closest('[data-action="reset-products"]');
            if (resetBtn) return ProductManager.loadProducts();
        });

        const searchInput = document.getElementById('productSearchInput');
        if (searchInput) {
            searchInput.addEventListener('keydown', (ev) => {
                if (ev.key === 'Enter') {
                    ev.preventDefault();
                    ProductManager.searchProducts();
                }
            });
        }
    }

    // ----------------- Load / render -----------------
    static async loadProducts() {
        try {
            const response = await axios.get(this.API_BASE());
            const products = response.data;
            const tbody = document.getElementById('productsTableBody');
            if (!tbody) return;
            tbody.innerHTML = '';
            if (!products || products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Нет продуктов</td></tr>';
                return;
            }
            products.forEach(product => tbody.appendChild(ProductManager.createProductRowElement(product)));
        } catch (e) {
            console.error('Ошибка загрузки продуктов:', e);
            const tbody = document.getElementById('productsTableBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Ошибка загрузки</td></tr>';
            ToastManager.showToast('Ошибка загрузки продуктов', 'danger');
        }
    }

    static createProductRowElement(product) {
        const tr = document.createElement('tr');
        tr.setAttribute('data-product-id', product.id);

        const categoryDisplay = product.categoryName ?? (product.category?.name) ?? '—';
        const amount = product.amount != null ? product.amount : (product.price != null ? product.price : 0);

        tr.innerHTML = `
      <td>${product.id}</td>
      <td>${ProductManager.escapeHtml(product.name ?? 'Без названия')}</td>
      <td>${amount} ₽</td>
      <td>${ProductManager.escapeHtml(categoryDisplay)}</td>
      <td>
        <span class="badge ${product.available ? 'bg-success' : 'bg-danger'}">
          ${product.available ? 'В наличии' : 'Нет в наличии'}
        </span>
      </td>
      <td>
        <button class="btn btn-sm btn-outline-primary js-edit-product" data-id="${product.id}" title="Редактировать">
          <i class="bi bi-pencil"></i>
        </button>
        <button class="btn btn-sm btn-outline-danger ms-1 js-delete-product" data-id="${product.id}" title="Удалить">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
        return tr;
    }

    static updateProductRow(product) {
        const tbody = document.getElementById('productsTableBody');
        if (!tbody) return;
        const existingRow = tbody.querySelector(`tr[data-product-id="${product.id}"]`);
        const newRow = ProductManager.createProductRowElement(product);
        if (existingRow) existingRow.replaceWith(newRow);
        else tbody.prepend(newRow);
    }

    // ----------------- Modal -----------------
    static async showProductModalById(productId, mode = 'edit') {
        try {
            const response = await axios.get(`${ProductManager.API_BASE()}/${productId}`);
            const product = response.data;
            await ProductManager.showProductModal(product, mode);
        } catch (e) {
            console.error('Не удалось загрузить продукт:', e);
            ToastManager.showToast('Не удалось загрузить продукт', 'danger');
        }
    }

    static async showProductModal(product = {}, mode = 'view') {
        const oldModal = document.getElementById('productModal');
        if (oldModal) {
            const inst = bootstrap.Modal.getInstance(oldModal);
            if (inst) inst.hide();
            oldModal.remove();
        }
        if (mode === 'create') {
            product = { name:'', amount:0, size:'', count:0, categoryId:null, available:false, photo:'', description:'' };
        }

        // Загружаем категории
        let categories = [];
        try {
            const res = await axios.get(`${new AuthManager().API_BASE.replace('/products','')}/categories`);
            categories = Array.isArray(res.data) ? res.data : [];
        } catch (e) { console.warn(e); }

        const categoryOptions = categories.map(c =>
            `<option value="${c.id}" ${Number(product.categoryId) === Number(c.id) ? 'selected' : ''}>
    ${ProductManager.escapeHtml(c.name)}
   </option>`
        ).join('');


        const modalHtml = `
<div class="modal fade" id="productModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">${mode==='create'?'Создать продукт':mode==='edit'?'Редактировать продукт':'Просмотр продукта'} ${product.id?('#'+product.id):''}</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div id="productError" class="alert alert-danger d-none"></div>
        <div class="mb-3">
          <label class="form-label">Название</label>
          ${mode==='view'?`<p>${ProductManager.escapeHtml(product.name||'—')}</p>`:`<input id="productName" class="form-control" value="${ProductManager.escapeHtml(product.name||'')}">`}
        </div>
        <div class="row mb-3">
          <div class="col-md-4">
            <label class="form-label">Цена</label>
            ${mode==='view'?`<p>${product.amount||0} ₽</p>`:`<input id="productAmount" type="number" class="form-control" value="${product.amount||0}">`}
          </div>
          <div class="col-md-4">
            <label class="form-label">Размер</label>
            ${mode==='view'?`<p>${ProductManager.escapeHtml(product.size||'—')}</p>`:`<input id="productSize" class="form-control" value="${ProductManager.escapeHtml(product.size||'')}">`}
          </div>
          <div class="col-md-4">
            <label class="form-label">Кол-во</label>
            ${mode==='view'?`<p>${product.count||0}</p>`:`<input id="productCount" type="number" class="form-control" value="${product.count||0}">`}
          </div>
        </div>
        <div class="mb-3">
          <label class="form-label">Категория</label>
          ${mode==='view'?`<p>${ProductManager.escapeHtml(product.categoryName??product.category?.name??'—')}</p>`:`<select id="productCategory" class="form-select"><option value="">Без категории</option>${categoryOptions}</select>`}
        </div>
        <div class="mb-3 form-check">
          ${mode==='view'?`<p>В наличии: ${product.available?'Да':'Нет'}</p>`:`<input id="productAvailable" type="checkbox" class="form-check-input" ${product.available?'checked':''}><label class="form-check-label ms-2">В наличии</label>`}
        </div>
        <div class="mb-3">
          <label class="form-label">Фото (URL)</label>
          ${mode==='view'?`<p>${ProductManager.escapeHtml(product.photo||'—')}</p>`:`<input id="productPhoto" class="form-control" value="${ProductManager.escapeHtml(product.photo||'')}">`}
        </div>

        ${mode!=='view'?`
<hr>
<h6>Фотографии продукта</h6>
<div class="input-group input-group-sm mb-2">
 <div class="input-group input-group-sm mb-2">
  <select id="imageFolderSelect" class="form-select form-select-sm"></select>
  <button id="deleteImageFolderBtn" class="btn btn-outline-danger btn-sm" title="Удалить папку">🗑</button>
</div>

  <input id="imageFolderInput" class="form-control form-control-sm" placeholder="Новая папка (опционально)">
  <button id="imageFolderUseBtn" class="btn btn-sm btn-outline-secondary">Использовать</button>
</div>
<div id="imageDropZone" class="border border-2 border-dashed rounded p-3 text-center text-muted mb-2">
  Перетащите изображение сюда или кликните
  <input type="file" id="imageFileInput" accept="image/*" hidden>
</div>
<div id="imageGallery" class="d-flex flex-wrap gap-2"></div>
` : ''}

        <div class="mb-3">
          <label class="form-label">Описание</label>
          ${mode==='view'?`<p>${ProductManager.escapeHtml(product.description||'—')}</p>`:`<textarea id="productDescription" class="form-control" rows="4">${ProductManager.escapeHtml(product.description||'')}</textarea>`}
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
        ${mode==='edit'?`<button class="btn btn-primary" id="productSaveBtn">Сохранить</button>`:''}
        ${mode==='create'?`<button class="btn btn-primary" id="productCreateBtn">Создать</button>`:''}
      </div>
    </div>
  </div>
</div>
`;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById('productModal');
        const bsModal = new bootstrap.Modal(modalEl);

        // Привязка кнопок
        if (mode==='create') document.getElementById('productCreateBtn').addEventListener('click',()=>ProductManager.createProduct());
        if (mode==='edit') document.getElementById('productSaveBtn').addEventListener('click',()=>ProductManager.updateProduct(product.id));

        bsModal.show();

        if ( mode !== 'view') {
            ProductManager.initImageUpload(product.id ?? null);
        }

        if (mode !== 'view' && product.categoryId != null) {
            const categorySelect = document.getElementById('productCategory');
            if (categorySelect) {
                categorySelect.value = String(product.categoryId);
            }
        }

        if (product.id && mode!=='view') ProductManager.initImageUpload(product.id ?? null);
    }

    static initImageUpload(productId) {
        const drop = document.getElementById('imageDropZone');
        const input = document.getElementById('imageFileInput');
        const folderSelect = document.getElementById('imageFolderSelect');
        const folderInput = document.getElementById('imageFolderInput');
        const useBtn = document.getElementById('imageFolderUseBtn');

        if (!drop || !input || !folderSelect) {
            console.warn('Image upload UI not found');
            return;
        }
        // drag & drop
        drop.addEventListener('click',()=>input.click());
        drop.addEventListener('dragover',e=>{e.preventDefault();drop.classList.add('bg-light');});
        drop.addEventListener('dragleave',()=>drop.classList.remove('bg-light'));
        drop.addEventListener('drop',e=>{e.preventDefault();drop.classList.remove('bg-light'); ProductManager.uploadImage(e.dataTransfer.files[0], folderSelect.value, productId);});
        input.addEventListener('change',()=>{ProductManager.uploadImage(input.files[0], folderSelect.value, productId); input.value='';});

        // Загрузка папок
        ProductManager.loadImageFolders(productId);

        // Создание новой папки
        useBtn.addEventListener('click', async ()=>{
            const val = folderInput.value.trim();
            if(!val) return;
            const safe = val.replace(/\s+/g,'_').replace(/[\/\\]/g,'');
            try {
                await axios.post('/api/admin/products/images/folders', null, { params:{ folder: safe } });
                if(![...folderSelect.options].some(o=>o.value===safe)) folderSelect.insertAdjacentHTML('beforeend',`<option value="${safe}">${safe}</option>`);
                folderSelect.value = safe;
                await ProductManager.loadImages(folderSelect.value, productId);
            } catch(err){ console.error(err); ToastManager.showToast('Не удалось создать папку','danger'); }
        });
    }

    static async loadImageFolders(productId) {
        const select = document.getElementById('imageFolderSelect');
        if(!select) return;
        try{
            const res = await axios.get('/api/admin/products/images/folders', {
                params: { scope: 'products' }
            })
            select.innerHTML = res.data.map(f=>`<option value="${f}">${f}</option>`).join('');
            if(res.data.length>0){
                select.value = res.data[0];
                await ProductManager.loadImages(select.value, productId);
            }
            select.addEventListener('change',()=>ProductManager.loadImages(select.value, productId));
        }catch(err){ console.error(err); }
    }

    static async loadImages(folder, productId){
        const gallery = document.getElementById('imageGallery');
        gallery.innerHTML='Загрузка...';
        try{
            const res = await axios.get('/api/admin/products/images',{ params:{ folder } });
            gallery.innerHTML='';
            if(!res.data || res.data.length===0){ gallery.innerHTML='<p class="text-muted">Нет фотографий в этой папке</p>'; return; }res.data.forEach(name => {
                const url = `/uploads/products/${folder}/${name}`;
                gallery.insertAdjacentHTML('beforeend', `
<div class="position-relative border rounded p-1">
  <img src="${url}"
       style="width:90px;height:90px;object-fit:cover;cursor:pointer"
       title="Выбрать фото"
       onclick="ProductManager.setProductPhoto(${productId ?? 'null'},'${url}')" alt="">

  <button class="btn btn-sm btn-danger position-absolute top-0 end-0 p-0"
          style="width:18px;height:18px;font-size:12px"
          onclick="ProductManager.deleteImage('${url}','${folder}',${productId ?? 'null'})">
    ✕
  </button>
</div>
`);
            });
        }catch(err){ console.error(err); gallery.innerHTML='<p class="text-danger">Ошибка загрузки фото</p>'; }
    }

    static async deleteImage(url, folder, productId) {
        if (!confirm('Удалить изображение?')) return;

        try {
            await axios.delete('/api/admin/products/images', {
                params: { url }
            });

            ToastManager.showToast('Изображение удалено', 'success');

            // если удалили текущее фото продукта — очистим
            const photoInput = document.getElementById('productPhoto');
            if (photoInput && photoInput.value === url) {
                photoInput.value = '';
            }

            await ProductManager.loadImages(folder, productId);

        } catch (err) {
            console.error(err);
            ToastManager.showToast('Не удалось удалить изображение', 'danger');
        }
    }

    static async uploadImage(file, folder, productId){
        if(!file) return;
        const fd = new FormData();
        fd.append('file',file);
        fd.append('folder',folder);
        try{
            const res = await axios.post('/api/admin/products/images/upload', fd, { headers:{ 'Content-Type':'multipart/form-data' } });
            ToastManager.showToast('Фото загружено','success');
            await ProductManager.loadImages(folder, productId);
            if(productId && res.data.url){
                ProductManager.setProductPhoto(productId, res.data.url);
            } else {
                document.getElementById('productPhoto').value = res.data.url;
            }
        }catch(err){ console.error(err); ToastManager.showToast('Ошибка загрузки','danger'); }
    }

    static async setProductPhoto(productId, url) {
        if (!productId) {
            console.warn('Продукт ещё не создан, фото будет применено после сохранения');
            const photoInput = document.getElementById('productPhoto');
            if (photoInput) photoInput.value = url;
            ToastManager.showToast('Фото будет применено после создания продукта', 'info');
            return;
        }

        try {
            await axios.put(`/api/admin/products/images/${productId}/photo`, {
                photo: url
            });

            document.getElementById('productPhoto').value = url;
            ToastManager.showToast('Фото установлено', 'success');

        } catch (err) {
            console.error(err);
            ToastManager.showToast('Не удалось установить фото', 'danger');
        }
    }




// ----------------- Helpers: create / update / delete -----------------
    static escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    static async createProduct() {
        const errBlock = document.getElementById('productError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('productName')?.value?.trim();
        let amount = Number(document.getElementById('productAmount')?.value);
        if (!Number.isFinite(amount)) amount = 0;
        const size = document.getElementById('productSize')?.value?.trim() || null;
        let count = Number(document.getElementById('productCount')?.value);
        if (!Number.isFinite(count)) count = 0;
        const catVal = document.getElementById('productCategory')?.value;
        const categoryId = (catVal && !isNaN(Number(catVal))) ? Number(catVal) : null;
        const available = !!document.getElementById('productAvailable')?.checked;
        const photo = document.getElementById('productPhoto')?.value?.trim() || null;
        const description = document.getElementById('productDescription')?.value?.trim() || null;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название продукта'; errBlock.classList.remove('d-none'); }
            return;
        }

        // Формируем payload с единой схемой: amount, count, categoryId, available, photo, description
        const payload = { name, amount, size, count, categoryId, available, photo, description };

        try {
            const response = await axios.post(ProductManager.API_BASE(), payload);
            const created = response.data;

            // Снимаем фокус, чтобы не было aria-hidden warning
            document.activeElement?.blur();
            const modalEl = document.getElementById('productModal');
            bootstrap.Modal.getInstance(modalEl)?.hide();

            // Обновляем строку в таблице
            ProductManager.updateProductRow(created);
            ToastManager.showToast('Продукт создан', 'success');
        } catch (error) {
            console.error('Ошибка создания продукта:', error);
            const message = error?.response?.data?.message || error.message || 'Ошибка при создании продукта';
            if (errBlock) { errBlock.textContent = message; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка при создании', 'danger');
        }
    }

    static async updateProduct(productId) {
        const errBlock = document.getElementById('productError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('productName')?.value?.trim();
        let amount = Number(document.getElementById('productAmount')?.value);
        if (!Number.isFinite(amount)) amount = 0;
        const size = document.getElementById('productSize')?.value?.trim() || null;
        let count = Number(document.getElementById('productCount')?.value);
        if (!Number.isFinite(count)) count = 0;
        const catVal = document.getElementById('productCategory')?.value;
        const categoryId = (catVal && !isNaN(Number(catVal))) ? Number(catVal) : null;
        const available = !!document.getElementById('productAvailable')?.checked;
        const photo = document.getElementById('productPhoto')?.value?.trim() || null;
        const description = document.getElementById('productDescription')?.value?.trim() || null;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название продукта'; errBlock.classList.remove('d-none'); }
            return;
        }

        const payload = { name, amount, size, count, categoryId, available, photo, description };

        try {
            const response = await axios.put(`${ProductManager.API_BASE()}/${productId}`, payload);
            const updated = response.data;

            document.activeElement?.blur();
            const modalEl = document.getElementById('productModal');
            bootstrap.Modal.getInstance(modalEl)?.hide();

            ProductManager.updateProductRow(updated);
            ToastManager.showToast('Продукт обновлён', 'success');
        } catch (error) {
            console.error('Ошибка обновления продукта:', error);
            const message = error?.response?.data?.message || error.message || 'Ошибка при обновлении продукта';
            if (errBlock) { errBlock.textContent = message; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка при обновлении', 'danger');
        }
    }

    static async deleteProduct(productId) {
        if (!confirm('Удалить продукт?')) return;
        try {
            await axios.delete(`${ProductManager.API_BASE()}/${productId}`);
            document.querySelector(`tr[data-product-id="${productId}"]`)?.remove();
            ToastManager.showToast('Продукт удалён', 'success');
        } catch (error) {
            console.error('Ошибка удаления продукта:', error);
            ToastManager.showToast('Не удалось удалить продукт', 'danger');
        }
    }

    static async searchProducts() {
        const query = document.getElementById('productSearchInput')?.value?.trim() || '';
        if (!query) {
            return this.loadProducts(); // если пустой запрос, грузим все
        }

        try {
            const response = await axios.get(`${this.API_BASE()}/search`, { params: { name: query } });
            const products = response.data;
            const tbody = document.getElementById('productsTableBody');
            if (!tbody) return;
            tbody.innerHTML = '';
            if (!products || products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Нет продуктов</td></tr>';
                return;
            }
            products.forEach(p => tbody.appendChild(this.createProductRowElement(p)));
        } catch (e) {
            console.error('Ошибка поиска продуктов:', e);
            ToastManager.showToast('Ошибка поиска продуктов', 'danger');
        }
    }


    // ----------------- helper: load product by id and open modal -----------------

}

// ----------------- expose small compatibility globals (если где-то они используются) -----------------
function editProduct(id) { ProductManager.showProductModalById(id, 'edit'); }
function deleteProduct(id) { ProductManager.deleteProduct(id); }

// ----------------- Автоинициализация (если DOM уже содержит таблицу) -----------------
document.addEventListener('DOMContentLoaded', () => {
    ProductManager.init();
    // если страница сразу показывает продукты — можно инициировать загрузку:
    const tbody = document.getElementById('productsTableBody');
    if (tbody) ProductManager.loadProducts();
});

document.getElementById('deleteImageFolderBtn')
    ?.addEventListener('click', async () => {
        const folder = folderSelect.value;
        if (!folder) return;

        if (!confirm(`Удалить папку "${folder}" со всеми изображениями?`)) return;

        try {
            await axios.delete('/api/admin/products/images/folders', {
                params: { folder }
            });

            ToastManager.showToast('Папка удалена', 'success');
            await ProductManager.loadImageFolders(productId);
        } catch(err){
            const msg =
                err?.response?.data?.message ||
                'Нельзя удалить папку: она используется продуктом';

            ToastManager.showToast(msg, 'warning');
        }

    });



