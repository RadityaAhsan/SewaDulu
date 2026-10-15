const $ = id => document.getElementById(id);
const rupiah = n => 'Rp ' + Number(n).toLocaleString('id-ID');
const esc = s => String(s ?? '').replace(/[&<>"']/g, c =>
    ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

// Data terakhir yang dibaca dari database (lewat API)
const state = { items: [], pelanggan: [], trx: [] };

function tampilPesan(teks, error = false) {
    const el = $('pesan');
    if (!el) return;

    el.innerHTML = `<i class="fa-solid ${error ? 'fa-circle-exclamation' : 'fa-circle-check'}"></i> <span>${teks}</span>`;
    el.className = 'toast ' + (error ? 'error' : 'sukses');
    el.style.display = 'flex';

    clearTimeout(window._timer);
    window._timer = setTimeout(() => { el.style.display = 'none'; }, 4000);
}

async function api(url, method = 'GET', body = null) {
    let res;
    try {
        res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: body ? JSON.stringify(body) : undefined
        });
    } catch (e) {
        throw new Error('Tidak dapat terhubung ke server. Pastikan backend berjalan.');
    }
    let data = null;
    try { data = await res.json(); } catch (_) {}
    if (!res.ok) throw new Error((data && data.error) || `Terjadi kesalahan (kode ${res.status}).`);
    return data;
}

async function jalankan(fn) {
    try { await fn(); } catch (e) { tampilPesan(e.message, true); }
}

const angka = id => $(id).value === '' ? null : Number($(id).value);

// Cegah scroll mouse mengubah angka pada input number
// (penyebab nilai 60000 berubah sendiri jadi 59987 saat halaman di-scroll).
document.addEventListener('wheel', e => {
    const el = document.activeElement;
    if (el && el.type === 'number' && el === e.target) el.blur();
}, { passive: true });

// ==========================================
// FORM
// ==========================================
function ubahKategori() {
    ['tenda', 'kamera', 'sepeda'].forEach(k => {
        const el = $('f-' + k);
        if (el) el.style.display = ($('kategori').value === k) ? 'block' : 'none';
    });
}

function tambahItem() {
    jalankan(async () => {
        const item = await api('/api/items', 'POST', {
            kategori: $('kategori').value,
            nama: $('namaItem').value,
            hargaSewa: angka('hargaSewa'),
            kapasitas: angka('kapasitas'),
            resolusi: $('resolusi').value,
            jenisGigi: $('jenisGigi').value
        });
        tampilPesan(`Item "${item.nama}" berhasil ditambahkan (${item.id}).`);
        ['namaItem', 'hargaSewa', 'kapasitas', 'resolusi', 'jenisGigi'].forEach(i => { if ($(i))$(i).value = ''; });
        await refreshSemua();
    });
}

function tambahPelanggan() {
    jalankan(async () => {
        const p = await api('/api/pelanggan', 'POST', {
            nama: $('namaPlg').value, email: $('emailPlg').value, noHp:$('hpPlg').value
        });
        tampilPesan(`Pelanggan "${p.nama}" berhasil ditambahkan (${p.id}).`);
        ['namaPlg', 'emailPlg', 'hpPlg'].forEach(i => { if ($(i))$(i).value = ''; });
        await refreshSemua();
    });
}

function sewaItem() {
    jalankan(async () => {
        const t = await api('/api/sewa', 'POST', {
            itemId: $('sewaItem').value,
            pelangganId: $('sewaPelanggan').value,
            durasiHari: angka('sewaDurasi')
        });
        tampilPesan(`Berhasil! ${t.itemNama} disewa oleh ${t.pelangganNama} (${t.id}).`);
        await refreshSemua();
    });
}

function kembalikan() {
    jalankan(async () => {
        const t = await api('/api/kembali', 'POST', { transaksiId: $('kembaliTrx').value });
        tampilPesan(`${t.itemNama} dikembalikan. Total biaya: ${rupiah(t.totalBiaya)}`);
        await refreshSemua();
    });
}

// ==========================================
// RENDER
// ==========================================
function renderStats() {
    const { items, trx, pelanggan } = state;
    $('statTotalItem').innerText = items.length;
    $('statTersedia').innerText = items.filter(i => i.statusTersedia).length;
    $('statSewaAktif').innerText = trx.filter(t => !t.selesai).length;
    $('statPelanggan').innerText = pelanggan.length;
}

function muatItem() {
    const f = $('filter').value;
    let items = state.items;
    if (f === 'true') items = items.filter(i => i.statusTersedia);
    else if (f === 'false') items = items.filter(i => !i.statusTersedia);

    $('tabelItem').innerHTML = items.length ? items.map(i => `
    <tr>
      <td><code>${esc(i.id)}</code></td>
      <td>${esc(i.kategori)}</td>
      <td><strong>${esc(i.nama)}</strong></td>
      <td>${rupiah(i.hargaSewa)}</td>
      <td>${Object.entries(i.detail || {}).map(([k, v]) => esc(k) + ': ' + esc(v)).join(', ')}</td>
      <td>
        <span class="badge ${i.statusTersedia ? 'badge-success' : 'badge-danger'}">
          ${i.statusTersedia ? 'Tersedia' : 'Disewa'}
        </span>
      </td>
    </tr>`).join('') : '<tr><td colspan="6" style="text-align:center; color:#64748b;">Belum ada data.</td></tr>';
}

function isiSelect(id, html) {
    const el = $(id);
    if (!el) return;
    const lama = el.value;
    el.innerHTML = html;
    if (lama && [...el.options].some(o => o.value === lama)) el.value = lama;
}

function renderDropdown() {
    const tersedia = state.items.filter(i => i.statusTersedia);
    isiSelect('sewaItem', tersedia.length
        ? tersedia.map(i => `<option value="${esc(i.id)}">${esc(i.id)} - ${esc(i.nama)} (${rupiah(i.hargaSewa)}/hari)</option>`).join('')
        : '<option value="">(tidak ada item tersedia)</option>');

    isiSelect('sewaPelanggan', state.pelanggan.length
        ? state.pelanggan.map(p => `<option value="${esc(p.id)}">${esc(p.id)} - ${esc(p.nama)}</option>`).join('')
        : '<option value="">(belum ada pelanggan)</option>');

    const aktif = state.trx.filter(t => !t.selesai);
    isiSelect('kembaliTrx', aktif.length
        ? aktif.map(t => `<option value="${esc(t.id)}">${esc(t.id)} - ${esc(t.itemNama)} oleh ${esc(t.pelangganNama)}</option>`).join('')
        : '<option value="">(tidak ada sewa berjalan)</option>');
}

function renderTabelTrx() {
    $('tabelTrx').innerHTML = state.trx.length ? state.trx.map(t => `
    <tr>
      <td><code>${esc(t.id)}</code></td>
      <td><strong>${esc(t.itemNama)}</strong></td>
      <td>${esc(t.pelangganNama)}</td>
      <td>${esc(t.tanggalSewa)}</td>
      <td>${esc(t.durasiRencana)} hari</td>
      <td>${t.selesai && t.tanggalKembali ? esc(t.tanggalKembali) : '<i style="color:#94a3b8">belum</i>'}</td>
      <td><strong>${t.selesai ? rupiah(t.totalBiaya) : '-'}</strong></td>
    </tr>`).join('') : '<tr><td colspan="7" style="text-align:center; color:#64748b;">Belum ada transaksi.</td></tr>';
}

// ==========================================
// SINKRONISASI
// ==========================================
async function refreshSemua() {
    const [items, pelanggan, trx] = await Promise.all([
        api('/api/items'), api('/api/pelanggan'), api('/api/transaksi')
    ]);
    state.items = items;
    state.pelanggan = pelanggan;
    state.trx = trx;

    renderStats();
    muatItem();
    renderDropdown();
    renderTabelTrx();
}

document.addEventListener('DOMContentLoaded', () => {
    ubahKategori();
    jalankan(refreshSemua);

    setInterval(() => {
        if (document.hidden) return;
        const aktif = document.activeElement;
        if (aktif && aktif.tagName === 'SELECT') return;
        refreshSemua().catch(() => {});
    }, 5000);
});