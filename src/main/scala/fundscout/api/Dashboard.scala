package fundscout.api

/** The FundScout web dashboard: a single self-contained HTML page served by the
  * HTTP server and rendered against the JSON API in the browser.
  *
  * It is embedded as a string (rather than read from disk) so the server has no
  * runtime file or packaging dependencies — consistent with the rest of the
  * project. The markup, styles, and vanilla-JS client all live in [[html]];
  * there is no build step and no third-party frontend library.
  */
object Dashboard:

  val html: String =
    """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>FundScout — Private-market intelligence</title>
<style>
:root {
  --bg:        oklch(0.165 0.012 258);
  --surface:   oklch(0.205 0.014 258);
  --surface-2: oklch(0.245 0.016 258);
  --border:    oklch(0.305 0.018 258);
  --border-2:  oklch(0.255 0.016 258);
  --ink:       oklch(0.965 0.004 258);
  --muted:     oklch(0.735 0.014 258);
  --faint:     oklch(0.580 0.014 258);
  --primary:   oklch(0.685 0.132 258);
  --primary-1: oklch(0.760 0.120 258);
  --accent:    oklch(0.805 0.135 76);
  --up:        oklch(0.760 0.130 168);
  --down:      oklch(0.680 0.170 25);
  --r: 12px;
  --r-sm: 8px;
  --ease: cubic-bezier(0.16, 1, 0.3, 1);
  --mono: ui-monospace, "SF Mono", "JetBrains Mono", "Cascadia Code", Menlo, monospace;
  --sans: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
}
* { box-sizing: border-box; }
html, body { height: 100%; }
body {
  margin: 0;
  font-family: var(--sans);
  color: var(--ink);
  background: var(--bg);
  background-image: radial-gradient(120% 70% at 50% -10%, oklch(0.685 0.132 258 / 0.16), transparent 60%);
  background-attachment: fixed;
  -webkit-font-smoothing: antialiased;
  font-size: 14px;
  line-height: 1.5;
}
.num { font-family: var(--mono); font-variant-numeric: tabular-nums; }

/* Top bar -------------------------------------------------------------- */
.topbar {
  position: sticky; top: 0; z-index: 20;
  display: flex; align-items: center; justify-content: space-between; gap: 1.5rem;
  padding: 0.85rem 1.5rem;
  background: oklch(0.165 0.012 258 / 0.82);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-2);
}
.brand { display: flex; align-items: center; gap: 0.7rem; cursor: pointer; user-select: none; }
.brand .mark {
  width: 26px; height: 26px; display: grid; place-items: center;
  color: var(--bg); background: var(--primary); border-radius: 7px;
  font-weight: 800; box-shadow: 0 0 18px oklch(0.685 0.132 258 / 0.45);
}
.brand .wordmark { font-weight: 680; letter-spacing: -0.01em; font-size: 0.98rem; }
.brand .subtitle { color: var(--faint); font-size: 0.72rem; letter-spacing: 0.01em; }
.ticker { display: flex; gap: 1.6rem; flex-wrap: wrap; }
.ticker .tk { display: flex; flex-direction: column; align-items: flex-end; }
.ticker .tk-label { color: var(--faint); font-size: 0.66rem; text-transform: uppercase; letter-spacing: 0.07em; }
.ticker .tk-val { font-size: 0.98rem; font-weight: 620; }
.ticker .tk-val.accent { color: var(--accent); }

/* Workspace ------------------------------------------------------------ */
.workspace { display: grid; grid-template-columns: 304px 1fr; min-height: calc(100dvh - 57px); }
.sidebar { border-right: 1px solid var(--border-2); padding: 1rem 0.75rem; }
.sidebar-head {
  display: flex; align-items: baseline; justify-content: space-between;
  padding: 0 0.55rem 0.6rem; color: var(--faint);
  font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.08em;
}
.nav-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; }
.nav-item {
  display: grid; grid-template-columns: 1fr auto; gap: 0.2rem 0.6rem; align-items: center;
  padding: 0.6rem 0.6rem; border-radius: var(--r-sm); cursor: pointer;
  border: 1px solid transparent; transition: background 0.16s var(--ease), border-color 0.16s var(--ease);
}
.nav-item:hover { background: var(--surface); }
.nav-item.active { background: oklch(0.685 0.132 258 / 0.14); border-color: oklch(0.685 0.132 258 / 0.4); }
.nav-item .name { font-weight: 560; letter-spacing: -0.005em; }
.nav-item.active .name { color: var(--primary-1); }
.nav-item .meta { color: var(--faint); font-size: 0.74rem; grid-column: 1; }
.nav-item .raised { color: var(--muted); font-size: 0.8rem; text-align: right; }
.nav-item .stage { color: var(--faint); font-size: 0.7rem; text-align: right; }
.dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; vertical-align: middle; margin-right: 0.4rem; }
.dot.exit { background: var(--accent); }

/* Detail --------------------------------------------------------------- */
.detail { padding: 1.6rem 1.8rem 3rem; min-width: 0; }
.view { animation: rise 0.32s var(--ease) both; }
@keyframes rise { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.view-title { font-size: 1.45rem; font-weight: 660; letter-spacing: -0.02em; margin: 0 0 0.15rem; text-wrap: balance; }
.view-sub { color: var(--muted); margin: 0 0 1.4rem; }
.chip {
  display: inline-flex; align-items: center; gap: 0.35rem; padding: 0.18rem 0.55rem;
  border-radius: 999px; font-size: 0.72rem; font-weight: 560;
  background: var(--surface-2); color: var(--muted); border: 1px solid var(--border);
}
.chip.primary { background: oklch(0.685 0.132 258 / 0.16); color: var(--primary-1); border-color: oklch(0.685 0.132 258 / 0.35); }
.chip.accent  { background: oklch(0.805 0.135 76 / 0.16); color: var(--accent); border-color: oklch(0.805 0.135 76 / 0.4); }

.panels { display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: 1rem; margin-top: 1.2rem; }
.panel { background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--r); padding: 1.1rem 1.15rem; }
.panel h3 { margin: 0 0 0.9rem; font-size: 0.74rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--faint); font-weight: 620; }
.panel.span-2 { grid-column: 1 / -1; }

/* Score gauge */
.gauge-row { display: flex; align-items: center; gap: 1.2rem; margin-bottom: 1rem; }
.gauge { width: 104px; height: 104px; flex: none; }
.gauge .track { fill: none; stroke: var(--border); stroke-width: 9; }
.gauge .fill { fill: none; stroke: var(--primary); stroke-width: 9; stroke-linecap: round;
  transform: rotate(-90deg); transform-origin: center; transition: stroke-dashoffset 0.9s var(--ease); }
.gauge-num { font-family: var(--mono); font-size: 1.9rem; font-weight: 680; }
.gauge-label { color: var(--faint); font-size: 0.74rem; }

.bars { display: flex; flex-direction: column; gap: 0.5rem; }
.bar-row { display: grid; grid-template-columns: 130px 1fr 32px; align-items: center; gap: 0.6rem; }
.bar-row .lbl { color: var(--muted); font-size: 0.78rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar-track { height: 7px; background: var(--surface-2); border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; width: 0; border-radius: 999px; background: var(--primary); transition: width 0.7s var(--ease); }
.bar-fill.accent { background: var(--accent); }
.bar-val { font-family: var(--mono); font-size: 0.78rem; color: var(--muted); text-align: right; }

.reasons { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.4rem; }
.reason { font-size: 0.76rem; padding: 0.3rem 0.55rem; border-radius: var(--r-sm); border: 1px solid var(--border); display: flex; gap: 0.4rem; align-items: flex-start; }
.reason .sign { font-family: var(--mono); font-weight: 700; }
.reason.pos { background: oklch(0.760 0.130 168 / 0.10); border-color: oklch(0.760 0.130 168 / 0.30); }
.reason.pos .sign { color: var(--up); }
.reason.neg { background: oklch(0.680 0.170 25 / 0.10); border-color: oklch(0.680 0.170 25 / 0.30); }
.reason.neg .sign { color: var(--down); }

/* Valuation range */
.val-figure { font-family: var(--mono); font-size: 1.6rem; font-weight: 680; letter-spacing: -0.01em; }
.range { position: relative; height: 8px; background: var(--surface-2); border-radius: 999px; margin: 1.1rem 0 0.4rem; }
.range .span { position: absolute; height: 100%; border-radius: 999px; background: linear-gradient(90deg, oklch(0.685 0.132 258 / 0.5), var(--primary)); width: 0; transition: width 0.7s var(--ease) 0.1s, left 0.7s var(--ease) 0.1s; }
.range .mid { position: absolute; top: -3px; width: 2px; height: 14px; background: var(--accent); border-radius: 2px; }
.range-labels { display: flex; justify-content: space-between; color: var(--faint); font-size: 0.72rem; font-family: var(--mono); }
.conf { margin-top: 0.9rem; }
.conf .High   { color: var(--up); }
.conf .Medium { color: var(--accent); }
.conf .Low    { color: var(--down); }
.bullets { margin: 0.9rem 0 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 0.4rem; }
.bullets li { color: var(--muted); font-size: 0.82rem; padding-left: 1rem; position: relative; }
.bullets li::before { content: "•"; position: absolute; left: 0; color: var(--primary); }

/* Forecast */
.nextround { display: flex; align-items: baseline; gap: 0.5rem; margin-bottom: 0.2rem; }
.nextround .pct { font-family: var(--mono); font-size: 2rem; font-weight: 700; color: var(--primary-1); }
.nextround .cap { color: var(--muted); }
.kv { display: flex; gap: 1.4rem; margin: 0.5rem 0 0.2rem; color: var(--muted); font-size: 0.82rem; }
.kv b { color: var(--ink); font-family: var(--mono); font-weight: 600; }

/* Events table */
.events { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
.events th { text-align: left; color: var(--faint); font-weight: 560; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em; padding: 0 0.8rem 0.55rem; }
.events td { padding: 0.6rem 0.8rem; border-top: 1px solid var(--border-2); }
.events td.n { font-family: var(--mono); }
.events tr td:first-child { color: var(--muted); }

/* Market breakdown lists */
.market-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; }
.rank { display: flex; flex-direction: column; gap: 0.55rem; }
.rank-row { display: grid; grid-template-columns: 1fr auto; gap: 0.2rem 0.8rem; align-items: center; }
.rank-row .rk-bar { grid-column: 1 / -1; }
.rank-row .rk-name { color: var(--ink); font-size: 0.85rem; }
.rank-row .rk-val { color: var(--muted); font-family: var(--mono); font-size: 0.82rem; }

/* Skeleton + empty */
.sk { background: linear-gradient(90deg, var(--surface) 25%, var(--surface-2) 37%, var(--surface) 63%); background-size: 400% 100%; animation: shimmer 1.3s infinite linear; border-radius: var(--r-sm); }
@keyframes shimmer { from { background-position: 100% 0; } to { background-position: -100% 0; } }
.empty { color: var(--muted); padding: 3rem 1rem; text-align: center; }

@media (max-width: 860px) {
  .workspace { grid-template-columns: 1fr; }
  .sidebar { border-right: none; border-bottom: 1px solid var(--border-2); }
  .ticker { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: 0.001ms !important; transition-duration: 0.001ms !important; }
}
</style>
</head>
<body>
<header class="topbar">
  <div class="brand" id="brand" title="Market overview">
    <div class="mark">F</div>
    <div>
      <div class="wordmark">FundScout</div>
      <div class="subtitle">Private-market intelligence</div>
    </div>
  </div>
  <div class="ticker" id="ticker"></div>
</header>
<main class="workspace">
  <aside class="sidebar">
    <div class="sidebar-head"><span>Companies</span><span id="co-count"></span></div>
    <ul class="nav-list" id="nav"></ul>
  </aside>
  <section class="detail" id="detail"></section>
</main>
<script>
const $ = (s, r=document) => r.querySelector(s);
const esc = s => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const api = async p => { const r = await fetch(p); if (!r.ok) throw new Error(p + ' -> ' + r.status); return r.json(); };
const moneyOr = m => m ? m.display : '—';
const state = { market: null, companies: [], selected: null };

function bar(value, max, cls) {
  const pct = max > 0 ? Math.max(2, Math.round(value / max * 100)) : 0;
  return `<div class="bar-track"><div class="bar-fill ${cls||''}" data-w="${pct}"></div></div>`;
}
function animateBars(root) {
  requestAnimationFrame(() => root.querySelectorAll('.bar-fill').forEach(b => b.style.width = b.dataset.w + '%'));
}

function renderTicker(m) {
  const v = m.fundingVelocityPerYear == null ? '—' : m.fundingVelocityPerYear + '/yr';
  const items = [
    ['Companies', m.companiesTracked, ''],
    ['Capital raised', m.totalCapitalRaised.display, ''],
    ['Median round', moneyOr(m.medianRoundSize), ''],
    ['Velocity', v, ''],
    ['Unicorns', m.unicornCount, 'accent'],
  ];
  $('#ticker').innerHTML = items.map(([l, val, c]) =>
    `<div class="tk"><span class="tk-label">${l}</span><span class="tk-val num ${c}">${val}</span></div>`).join('');
}

function renderNav() {
  const market = `<li class="nav-item ${state.selected ? '' : 'active'}" data-id="__market__">
    <span class="name">Market overview</span><span class="meta">${state.companies.length} companies tracked</span></li>`;
  const rows = state.companies.map(c => `
    <li class="nav-item ${state.selected === c.companyId ? 'active' : ''}" data-id="${c.companyId}">
      <span class="name">${c.hasExited ? '<span class="dot exit"></span>' : ''}${esc(c.name)}</span>
      <span class="raised num">${c.totalCapitalRaised.display}</span>
      <span class="meta">${esc(c.sector)}</span>
      <span class="stage">${c.latestStage ? esc(c.latestStage) : ''}</span>
    </li>`).join('');
  $('#nav').innerHTML = market + rows;
  $('#co-count').textContent = state.companies.length;
}

function rankPanel(title, rows, valueOf, nameOf) {
  const max = Math.max(...rows.map(valueOf), 0);
  const body = rows.length ? rows.map(r => `
    <div class="rank-row">
      <span class="rk-name">${esc(nameOf(r))}</span>
      <span class="rk-val">${r.funding ? r.funding.display : r.deals + ' deals'}</span>
      <span class="rk-bar">${bar(valueOf(r), max)}</span>
    </div>`).join('') : '<div class="empty">No data</div>';
  return `<div class="panel"><h3>${title}</h3><div class="rank">${body}</div></div>`;
}

function renderMarket() {
  const m = state.market;
  const detail = $('#detail');
  const sectors = rankPanel('Funding by sector', m.fundingBySector.slice(0, 6), r => Number(r.funding.amount), r => r.key);
  const countries = rankPanel('Funding by country', m.fundingByCountry.slice(0, 6), r => Number(r.funding.amount), r => r.key);
  const investors = rankPanel('Most active investors', m.topInvestors.slice(0, 6), r => r.deals, r => r.investor);
  const unicorns = m.unicorns.length
    ? `<div class="panel"><h3>Unicorns</h3><div class="rank">${m.unicorns.map(u =>
        `<div class="rank-row"><span class="rk-name">${esc(u.name)}</span><span class="rk-val">${u.valuation.display}</span></div>`).join('')}</div></div>`
    : '';
  detail.innerHTML = `<div class="view">
    <h1 class="view-title">Market overview</h1>
    <p class="view-sub">${m.companiesTracked} companies · ${m.totalRounds} rounds · ${m.totalCapitalRaised.display} raised · ${m.exitedCompanies} exited</p>
    <div class="market-grid">${sectors}${countries}${investors}${unicorns}</div>
  </div>`;
  animateBars(detail);
}

function reasonChips(list, polarityKey) {
  return list.map(r => {
    const pol = polarityKey ? r[polarityKey] : r.polarity;
    const cls = pol === 'Positive' ? 'pos' : pol === 'Negative' ? 'neg' : '';
    const sign = pol === 'Positive' ? '+' : pol === 'Negative' ? '−' : '•';
    const text = r.description || r;
    return `<div class="reason ${cls}"><span class="sign">${sign}</span><span>${esc(text)}</span></div>`;
  }).join('');
}

function scorePanel(s) {
  const C = 2 * Math.PI * 47;
  const off = C * (1 - s.overall / 100);
  const comps = s.components.map(c =>
    `<div class="bar-row"><span class="lbl">${esc(c.component)}</span>${bar(c.value, 100)}<span class="bar-val">${c.value}</span></div>`).join('');
  const reasons = reasonChips(s.components.flatMap(c => c.reasons));
  return `<div class="panel span-2"><h3>Startup score</h3>
    <div class="gauge-row">
      <svg class="gauge" viewBox="0 0 104 104">
        <circle class="track" cx="52" cy="52" r="47"></circle>
        <circle class="fill" cx="52" cy="52" r="47" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${C.toFixed(1)}" data-off="${off.toFixed(1)}"></circle>
        <text x="52" y="50" text-anchor="middle" dominant-baseline="middle" class="gauge-num" fill="currentColor">${s.overall}</text>
        <text x="52" y="68" text-anchor="middle" class="gauge-label" fill="var(--faint)">/ 100</text>
      </svg>
      <div class="bars" style="flex:1">${comps}</div>
    </div>
    <div class="reasons">${reasons}</div>
  </div>`;
}

function valuationPanel(v) {
  const lo = Number(v.low.amount), hi = Number(v.high.amount);
  const top = hi * 1.15 || 1;
  const left = (lo / top * 100), width = ((hi - lo) / top * 100), mid = ((lo + hi) / 2 / top * 100);
  return `<div class="panel"><h3>Valuation estimate</h3>
    <div class="val-figure">${v.low.display} – ${v.high.display}</div>
    <div class="range"><div class="span" data-left="${left}" data-w="${width}"></div><div class="mid" style="left:${mid}%"></div></div>
    <div class="range-labels"><span>0</span><span>${v.high.display}</span></div>
    <div class="conf">Confidence: <b class="${v.confidence}">${v.confidence}</b></div>
    <ul class="bullets">${v.reasoning.map(r => `<li>${esc(r)}</li>`).join('')}</ul>
  </div>`;
}

function forecastPanel(f) {
  const nr = f.nextRound;
  const size = nr.expectedAmount ? `${nr.expectedAmount.low.display}–${nr.expectedAmount.high.display}` : '—';
  const time = nr.expectedMonths ? `${nr.expectedMonths.low}–${nr.expectedMonths.high} mo` : '—';
  const maxO = 100;
  const outcomes = f.outcomes.map(o =>
    `<div class="bar-row"><span class="lbl">${esc(o.outcome)}</span>${bar(o.probabilityPercent, maxO)}<span class="bar-val">${o.probabilityPercent}%</span></div>`).join('');
  return `<div class="panel"><h3>Forecast</h3>
    <div class="nextround"><span class="pct">${nr.probabilityPercent}%</span><span class="cap">next-round likelihood</span></div>
    <div class="kv"><span>Expected size <b>${size}</b></span><span>Timing <b>${time}</b></span></div>
    <div class="bars" style="margin-top:1rem">${outcomes}</div>
  </div>`;
}

async function renderCompany(id) {
  const detail = $('#detail');
  detail.innerHTML = `<div class="view"><div class="sk" style="height:2rem;width:240px"></div>
    <div class="panels"><div class="sk" style="height:220px"></div><div class="sk" style="height:220px"></div></div></div>`;
  try {
    const [p, score, val, fc] = await Promise.all([
      api('/companies/' + id), api('/companies/' + id + '/score'),
      api('/companies/' + id + '/valuation'), api('/companies/' + id + '/forecast')]);
    const chips = [`<span class="chip">${esc(p.sector)}</span>`, `<span class="chip">${esc(p.headquarters)}</span>`];
    if (p.latestStage) chips.push(`<span class="chip primary">${esc(p.latestStage)}</span>`);
    if (p.latestValuation) chips.push(`<span class="chip accent">${p.latestValuation.display} valuation</span>`);
    if (p.hasExited) chips.push(`<span class="chip">Exited</span>`);
    const events = p.events.slice().reverse().map(e => `<tr>
      <td class="n">${e.date}</td><td>${esc(e.stage)}</td><td class="n">${e.amount.display}</td>
      <td>${e.leadInvestor ? esc(e.leadInvestor) : '—'}</td>
      <td class="n">${e.postMoneyValuation ? e.postMoneyValuation.display : '—'}</td></tr>`).join('');
    detail.innerHTML = `<div class="view">
      <h1 class="view-title">${esc(p.name)}</h1>
      <p class="view-sub">${p.rounds} rounds · ${p.totalCapitalRaised.display} raised</p>
      <div style="display:flex;gap:0.4rem;flex-wrap:wrap">${chips.join('')}</div>
      <div class="panels">
        ${scorePanel(score)}
        ${valuationPanel(val)}
        ${forecastPanel(fc)}
        <div class="panel span-2"><h3>Funding history</h3>
          <table class="events"><thead><tr><th>Date</th><th>Stage</th><th>Amount</th><th>Lead investor</th><th>Post-money</th></tr></thead>
          <tbody>${events}</tbody></table></div>
      </div></div>`;
    animateBars(detail);
    requestAnimationFrame(() => {
      const fill = detail.querySelector('.gauge .fill'); if (fill) fill.style.strokeDashoffset = fill.dataset.off;
      const span = detail.querySelector('.range .span'); if (span) { span.style.left = span.dataset.left + '%'; span.style.width = span.dataset.w + '%'; }
    });
  } catch (err) {
    detail.innerHTML = `<div class="empty">Could not load ${esc(id)}.<br>${esc(err.message)}</div>`;
  }
}

function select(id) {
  state.selected = id === '__market__' ? null : id;
  location.hash = state.selected || '';
  renderNav();
  if (state.selected) renderCompany(state.selected); else renderMarket();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

$('#nav').addEventListener('click', e => { const li = e.target.closest('.nav-item'); if (li) select(li.dataset.id); });
$('#brand').addEventListener('click', () => select('__market__'));

async function boot() {
  $('#detail').innerHTML = `<div class="view"><div class="sk" style="height:2rem;width:220px;margin-bottom:1.4rem"></div>
    <div class="market-grid"><div class="sk" style="height:200px"></div><div class="sk" style="height:200px"></div><div class="sk" style="height:200px"></div></div></div>`;
  try {
    const [market, companies] = await Promise.all([api('/market'), api('/companies')]);
    state.market = market; state.companies = companies;
    renderTicker(market);
    const initial = decodeURIComponent(location.hash.replace(/^#/, ''));
    if (initial && companies.some(c => c.companyId === initial)) select(initial);
    else { renderNav(); renderMarket(); }
  } catch (err) {
    $('#detail').innerHTML = `<div class="empty">Could not reach the FundScout API.<br>${esc(err.message)}</div>`;
  }
}
boot();
</script>
</body>
</html>"""
