document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('main[data-api]');
    const api = root.dataset.api;
    const $ = (id) => document.getElementById(id);
    const fmt = (n) => (n||0).toLocaleString('ko-KR');

    const today = new Date();
    const to = today.toISOString().slice(0,10);
    const fromD = new Date(today); fromD.setDate(today.getDate()-29);
    const from = fromD.toISOString().slice(0,10);

    document.getElementById('fromDate').value = from;
    document.getElementById('toDate').value = to;

    async function load() {
        const f = document.getElementById('fromDate').value;
        const t = document.getElementById('toDate').value;
        const res = await fetch(`${api}?from=${f}&to=${t}`);
        const data = await res.json();

        $('kpiOrders').textContent = fmt(data.orders);
        $('kpiGross').textContent  = fmt(data.grossSales);
        $('kpiDisc').textContent   = fmt(data.discounts);
        $('kpiNet').textContent    = fmt(data.netSales);
    }

    document.getElementById('btnApply').addEventListener('click', load);
    load();
});
