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
<link rel="icon" type="image/svg+xml" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E%3Crect width='32' height='32' rx='8' fill='%231a2236'/%3E%3Cg fill='%2356a0f5'%3E%3Crect x='8' y='7' width='4.2' height='18' rx='1'/%3E%3Crect x='8' y='7' width='13' height='4.2' rx='1'/%3E%3Crect x='8' y='14.5' width='9' height='3.8' rx='1'/%3E%3C/g%3E%3Cpolyline points='8,25 15,20 20,22 26,12' fill='none' stroke='%237cbcfb' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3Ccircle cx='26' cy='12' r='2.6' fill='%237cbcfb'/%3E%3C/svg%3E"/>
<title>FundScout — Private-market intelligence</title>
<style>
:root {
  --bg:        oklch(0.165 0.012 258);
  --surface:   oklch(0.205 0.014 258);
  --surface-2: oklch(0.245 0.016 258);
  --border:    oklch(0.305 0.018 258);
  --border-2:  oklch(0.255 0.016 258);
  --ink:       oklch(0.965 0.004 258);
  --muted:     oklch(0.760 0.014 258);
  --faint:     oklch(0.660 0.014 258);
  --focus:     oklch(0.800 0.110 258);
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
  width: 38px; height: 38px; display: grid; place-items: center; flex: none;
  filter: drop-shadow(0 0 13px oklch(0.685 0.132 258 / 0.35));
}
.brand .mark svg { width: 38px; height: 38px; display: block; }
.brand .wordmark .hl { color: var(--primary-1); }
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
  padding: 0.55rem 0.6rem; min-height: 44px; border-radius: var(--r-sm); cursor: pointer;
  border: 1px solid transparent; transition: background 0.16s var(--ease), border-color 0.16s var(--ease);
}
.nav-item:hover { background: var(--surface); }
.nav-item.active { background: oklch(0.685 0.132 258 / 0.14); border-color: oklch(0.685 0.132 258 / 0.4); }
:focus { outline: none; }
:focus-visible { outline: 2px solid var(--focus); outline-offset: 2px; }
.brand { border-radius: var(--r-sm); }
.nav-item .name { font-weight: 650; letter-spacing: -0.01em; font-size: 0.94rem; }
.nav-item.active .name { color: var(--primary-1); }
.nav-item .meta { color: var(--faint); font-size: 0.74rem; grid-column: 1; display: flex; align-items: center; gap: 0.4rem; }
.nav-item .raised { color: var(--muted); font-size: 0.82rem; text-align: right; font-family: var(--mono); }
.nav-item .stage { color: var(--faint); font-size: 0.7rem; text-align: right; }
.dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; vertical-align: middle; margin-right: 0.4rem; }
.dot.exit { background: var(--accent); }
.dot.risk { background: var(--down); box-shadow: 0 0 6px oklch(0.680 0.170 25 / 0.7); }

/* Alerts */
.alerts { background: oklch(0.680 0.170 25 / 0.10); border: 1px solid oklch(0.680 0.170 25 / 0.35); border-radius: var(--r); padding: 0.85rem 1rem; margin-top: 1.1rem; }
.alerts h4 { margin: 0 0 0.55rem; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--down); font-weight: 680; }
.alert-row { display: flex; gap: 0.6rem; align-items: baseline; padding: 0.28rem 0; }
.alert-sev { font-size: 0.62rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; padding: 0.12rem 0.4rem; border-radius: 5px; flex: none; font-family: var(--mono); }
.alert-sev.Critical { background: var(--down); color: var(--bg); }
.alert-sev.Serious  { background: oklch(0.680 0.170 25 / 0.25); color: var(--down); }
.alert-sev.Watch    { background: var(--surface-2); color: var(--muted); }
.alert-desc { color: var(--ink); font-size: 0.85rem; }
.alert-desc .when { color: var(--faint); }

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
.gauge-num { font-family: var(--mono); font-size: 1.9rem; font-weight: 680; fill: var(--ink); }
.gauge-label { font-size: 0.74rem; fill: var(--faint); }

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

/* KPI band */
.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 1px; background: var(--border-2); border: 1px solid var(--border-2); border-radius: var(--r); overflow: hidden; margin: 0.4rem 0 1.3rem; }
.kpi { background: var(--surface); padding: 1.05rem 1.2rem; }
.kpi .k-val { font-family: var(--mono); font-size: 1.75rem; font-weight: 680; letter-spacing: -0.02em; line-height: 1.1; }
.kpi.lead .k-val { color: var(--primary-1); }
.kpi.gold .k-val { color: var(--accent); }
.kpi .k-label { color: var(--faint); font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.07em; margin-top: 0.3rem; }

/* Market summary */
.summary { background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--r); padding: 1.05rem 1.2rem; margin-bottom: 1.3rem; }
.summary h3 { margin: 0 0 0.5rem; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--faint); font-weight: 620; }
.summary p { margin: 0; color: var(--muted); font-size: 0.92rem; line-height: 1.6; max-width: 72ch; text-wrap: pretty; }
.summary p strong { color: var(--ink); font-weight: 600; }

/* Asymmetric board */
.board { display: grid; gap: 1rem; grid-template-columns: 1.7fr 1fr; align-items: start; }
.board .hero { grid-column: 1; }
.board .side { grid-column: 2; }
.board .rest { grid-column: 1 / -1; display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); }

/* Ranked breakdown lists (horizontal bars) */
.rank { display: flex; flex-direction: column; gap: 0.7rem; }
.rank.lg { gap: 0.95rem; }
.rank-row { display: grid; grid-template-columns: 1fr auto; gap: 0.3rem 1.2rem; align-items: center; }
.rank-row .rk-bar { grid-column: 1 / -1; }
.rank-row .rk-name { color: var(--ink); font-size: 0.85rem; display: flex; align-items: center; gap: 0.5rem; }
.rank.lg .rk-name { font-size: 0.92rem; }
.rank-row .rk-val { color: var(--muted); font-family: var(--mono); font-size: 0.82rem; }
.rk-rank { font-family: var(--mono); color: var(--faint); font-size: 0.78rem; width: 1.1rem; flex: none; }
.swatch { width: 9px; height: 9px; border-radius: 3px; flex: none; }

/* Funding timeline (area trend) */
.trend { width: 100%; height: 130px; display: block; }
.trend .area { opacity: 0.18; }
.trend .line { fill: none; stroke: var(--primary-1); stroke-width: 2; }
.trend .pt { fill: var(--primary-1); }
.trend-axis { display: flex; justify-content: space-between; color: var(--faint); font-family: var(--mono); font-size: 0.68rem; margin-top: 0.4rem; }

/* Stage histogram (vertical bars) */
.histo { display: flex; align-items: flex-end; gap: 0.5rem; height: 120px; }
.histo .col { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; align-items: center; gap: 0.4rem; height: 100%; }
.histo .vbar { width: 100%; max-width: 30px; background: var(--primary); border-radius: 4px 4px 0 0; min-height: 3px; transition: height 0.7s var(--ease); }
.histo .v-count { font-family: var(--mono); font-size: 0.74rem; color: var(--muted); }
.histo .v-lbl { color: var(--faint); font-size: 0.64rem; text-align: center; white-space: nowrap; }

/* Leaderboard table */
.lb { width: 100%; border-collapse: collapse; }
.lb td { padding: 0.5rem 0.2rem; border-top: 1px solid var(--border-2); font-size: 0.86rem; }
.lb tr:first-child td { border-top: none; }
.lb td.r { font-family: var(--mono); color: var(--faint); width: 1.4rem; }
.lb td.v { font-family: var(--mono); color: var(--accent); text-align: right; }

/* Skeleton + empty */
.sk { background: linear-gradient(90deg, var(--surface) 25%, var(--surface-2) 37%, var(--surface) 63%); background-size: 400% 100%; animation: shimmer 1.3s infinite linear; border-radius: var(--r-sm); }
@keyframes shimmer { from { background-position: 100% 0; } to { background-position: -100% 0; } }
.empty { color: var(--muted); padding: 3rem 1rem; text-align: center; }

@media (max-width: 980px) {
  .board { grid-template-columns: 1fr; }
  .board .hero, .board .side { grid-column: 1; }
}
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
  <div class="brand" id="brand" role="button" tabindex="0" aria-label="FundScout — go to market overview">
    <div class="mark" aria-hidden="true">
      <svg viewBox="0 0 48 48" fill="none">
        <defs>
          <linearGradient id="fsF" x1="9" y1="7" x2="30" y2="42" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#73b2f8"/>
            <stop offset="1" stop-color="#3c7ce0"/>
          </linearGradient>
        </defs>
        <g fill="#33507c">
          <rect x="23" y="30" width="5" height="13" rx="1.2"/>
          <rect x="30" y="24" width="5" height="19" rx="1.2"/>
          <rect x="37" y="17" width="5" height="26" rx="1.2"/>
        </g>
        <g fill="url(#fsF)">
          <rect x="9" y="7" width="6" height="35" rx="1.6"/>
          <rect x="9" y="7" width="20" height="6" rx="1.6"/>
          <rect x="9" y="21.5" width="14" height="5.4" rx="1.6"/>
        </g>
        <polyline points="7,38 17,31 25,26 35,15 44,9" fill="none" stroke="#6cb2f8" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
        <g fill="#7cbcfb">
          <circle cx="7" cy="38" r="2.7"/>
          <circle cx="25" cy="26" r="2.7"/>
          <circle cx="44" cy="9" r="2.7"/>
        </g>
      </svg>
    </div>
    <div>
      <div class="wordmark">Fund<span class="hl">Scout</span></div>
      <div class="subtitle">Private-market intelligence</div>
    </div>
  </div>
  <div class="ticker" id="ticker" aria-label="Market summary"></div>
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

function bar(value, max, cls, color) {
  const pct = max > 0 ? Math.max(2, Math.round(value / max * 100)) : 0;
  const c = color ? `background:${color};` : '';
  return `<div class="bar-track"><div class="bar-fill ${cls||''}" data-w="${pct}" style="${c}"></div></div>`;
}
function animateBars(root) {
  requestAnimationFrame(() => {
    root.querySelectorAll('.bar-fill').forEach(b => b.style.width = b.dataset.w + '%');
    root.querySelectorAll('.histo .vbar').forEach(b => b.style.height = b.dataset.h + '%');
  });
}

const SECTOR_COLOR = {
  'Artificial Intelligence': 'oklch(0.70 0.13 258)',
  'Fintech':    'oklch(0.74 0.13 165)',
  'Biotech':    'oklch(0.70 0.13 300)',
  'Healthtech': 'oklch(0.70 0.15 18)',
  'SaaS':       'oklch(0.76 0.11 220)',
  'E-commerce': 'oklch(0.80 0.13 135)',
  'Robotics':   'oklch(0.81 0.13 70)',
  'Hardware':   'oklch(0.66 0.05 258)',
  'Climate':    'oklch(0.76 0.13 145)',
  'Crypto':     'oklch(0.76 0.14 50)',
  'Gaming':     'oklch(0.68 0.15 330)',
};
const sectorColor = s => SECTOR_COLOR[s] || 'var(--muted)';
const swatch = s => `<span class="swatch" style="background:${sectorColor(s)}"></span>`;

function renderTicker(m) {
  const items = [
    ['Companies', m.companiesTracked, ''],
    ['Capital', m.totalCapitalRaised.display, ''],
    ['Unicorns', m.unicornCount, 'accent'],
  ];
  $('#ticker').innerHTML = items.map(([l, val, c]) =>
    `<div class="tk"><span class="tk-label">${l}</span><span class="tk-val num ${c}">${val}</span></div>`).join('');
}

function renderNav() {
  const market = `<li class="nav-item ${state.selected ? '' : 'active'}" data-id="__market__"
    role="button" tabindex="0" ${state.selected ? '' : 'aria-current="true"'}>
    <span class="name">Market overview</span><span class="meta">${state.companies.length} companies tracked</span></li>`;
  const rows = state.companies.map(c => `
    <li class="nav-item ${state.selected === c.companyId ? 'active' : ''}" data-id="${c.companyId}"
      role="button" tabindex="0" ${state.selected === c.companyId ? 'aria-current="true"' : ''}>
      <span class="name">${c.riskCount > 0 ? `<span class="dot risk" title="Has alerts"></span>` : (c.hasExited ? '<span class="dot exit" title="Exited"></span>' : '')}${esc(c.name)}</span>
      <span class="raised num">${c.totalCapitalRaised.display}</span>
      <span class="meta">${swatch(c.sector)}${esc(c.sector)}</span>
      <span class="stage">${c.latestStage ? esc(c.latestStage) : ''}</span>
    </li>`).join('');
  $('#nav').innerHTML = market + rows;
  $('#co-count').textContent = state.companies.length;
}

// --- Market overview pieces -------------------------------------------------

function kpiBand(m) {
  const v = m.fundingVelocityPerYear == null ? '—' : (+m.fundingVelocityPerYear).toFixed(1);
  const tiles = [
    ['lead', m.totalCapitalRaised.display, 'Capital raised'],
    ['', m.companiesTracked, 'Companies'],
    ['gold', m.unicornCount, 'Unicorns'],
    ['', v, 'Deals / year'],
    ['', moneyOr(m.medianRoundSize), 'Median round'],
    ['', m.totalRounds, 'Total rounds'],
  ];
  return `<div class="kpis">${tiles.map(([c, val, l]) =>
    `<div class="kpi ${c}"><div class="k-val">${val}</div><div class="k-label">${l}</div></div>`).join('')}</div>`;
}

function marketSummary(m) {
  if (!m.fundingBySector.length) return '';
  const total = Number(m.totalCapitalRaised.amount) || 1;
  const top = m.fundingBySector[0];
  const share = Math.round(Number(top.funding.amount) / total * 100);
  const inv = m.topInvestors[0];
  const sentences = [
    `<strong>${esc(top.key)}</strong> leads the tracked cohort, drawing <strong>${share}%</strong> of all capital raised across ${m.companiesTracked} companies.`,
    inv ? `<strong>${esc(inv.investor)}</strong> is the most active investor with ${inv.deals} deals.` : '',
    `${m.unicornCount} ${m.unicornCount === 1 ? 'company has' : 'companies have'} reached unicorn scale` +
      (m.exitedCompanies ? `, and ${m.exitedCompanies} ${m.exitedCompanies === 1 ? 'has' : 'have'} exited.` : '.'),
    m.fundingVelocityPerYear ? `Funding is running at <strong>${(+m.fundingVelocityPerYear).toFixed(1)}</strong> rounds per year.` : '',
  ].filter(Boolean);
  return `<div class="summary"><h3>Market summary</h3><p>${sentences.join(' ')}</p></div>`;
}

function sectorBars(m) {
  const rows = m.fundingBySector.slice(0, 8);
  const max = Math.max(...rows.map(r => Number(r.funding.amount)), 0);
  const body = rows.map(r => `
    <div class="rank-row">
      <span class="rk-name">${swatch(r.key)}${esc(r.key)}</span>
      <span class="rk-val">${r.funding.display}</span>
      <span class="rk-bar">${bar(Number(r.funding.amount), max, '', sectorColor(r.key))}</span>
    </div>`).join('');
  return `<div class="panel hero"><h3>Funding by sector</h3><div class="rank lg">${body}</div></div>`;
}

function trendChart(m) {
  const data = m.fundingByYear || [];
  if (data.length < 2)
    return `<div class="panel side"><h3>Funding over time</h3><div class="empty">Not enough history</div></div>`;
  const vals = data.map(d => Number(d.funding.amount));
  const max = Math.max(...vals, 1);
  const W = 300, H = 120, pad = 6;
  const x = i => pad + i * (W - 2 * pad) / (data.length - 1);
  const y = v => H - pad - v / max * (H - 2 * pad);
  const pts = data.map((d, i) => [x(i), y(vals[i])]);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' ');
  const area = `${line} L${x(data.length - 1).toFixed(1)} ${H - pad} L${x(0).toFixed(1)} ${H - pad} Z`;
  const dots = pts.map(p => `<circle class="pt" cx="${p[0].toFixed(1)}" cy="${p[1].toFixed(1)}" r="2.5"></circle>`).join('');
  return `<div class="panel side"><h3>Funding over time</h3>
    <svg class="trend" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" role="img" aria-label="Capital raised by year">
      <path class="area" d="${area}" fill="var(--primary)"></path>
      <path class="line" d="${line}"></path>${dots}
    </svg>
    <div class="trend-axis">${data.map(d => `<span>${d.year}</span>`).join('')}</div></div>`;
}

function stageHisto(m) {
  const data = (m.roundsByStage || []);
  if (!data.length) return '';
  const max = Math.max(...data.map(d => d.count), 0);
  const cols = data.map(d => `
    <div class="col">
      <span class="v-count">${d.count}</span>
      <div class="vbar" data-h="${max ? Math.max(4, Math.round(d.count / max * 100)) : 0}"></div>
      <span class="v-lbl">${esc(d.stage)}</span>
    </div>`).join('');
  return `<div class="panel"><h3>Rounds by stage</h3><div class="histo">${cols}</div></div>`;
}

function investorRanked(m) {
  const rows = m.topInvestors.slice(0, 6);
  const max = Math.max(...rows.map(r => r.deals), 0);
  const body = rows.length ? rows.map((r, i) => `
    <div class="rank-row">
      <span class="rk-name"><span class="rk-rank">${i + 1}</span>${esc(r.investor)}</span>
      <span class="rk-val">${r.deals} deals</span>
      <span class="rk-bar">${bar(r.deals, max)}</span>
    </div>`).join('') : '<div class="empty">No data</div>';
  return `<div class="panel"><h3>Most active investors</h3><div class="rank">${body}</div></div>`;
}

function countryBars(m) {
  const rows = m.fundingByCountry.slice(0, 6);
  const max = Math.max(...rows.map(r => Number(r.funding.amount)), 0);
  const body = rows.length ? rows.map(r => `
    <div class="rank-row">
      <span class="rk-name">${esc(r.key)}</span>
      <span class="rk-val">${r.funding.display}</span>
      <span class="rk-bar">${bar(Number(r.funding.amount), max)}</span>
    </div>`).join('') : '<div class="empty">No data</div>';
  return `<div class="panel"><h3>Funding by country</h3><div class="rank">${body}</div></div>`;
}

function unicornBoard(m) {
  if (!m.unicorns.length) return `<div class="panel"><h3>Unicorns</h3><div class="empty">None yet</div></div>`;
  const rows = m.unicorns.slice(0, 8).map((u, i) =>
    `<tr><td class="r">${i + 1}</td><td>${esc(u.name)}</td><td class="v">${u.valuation.display}</td></tr>`).join('');
  return `<div class="panel"><h3>Unicorn leaderboard</h3><table class="lb"><tbody>${rows}</tbody></table></div>`;
}

function renderMarket() {
  const m = state.market;
  const detail = $('#detail');
  detail.innerHTML = `<div class="view">
    <h1 class="view-title">Market overview</h1>
    <p class="view-sub">${m.companiesTracked} companies · ${m.totalRounds} rounds · ${m.totalCapitalRaised.display} raised · ${m.exitedCompanies} exited</p>
    ${kpiBand(m)}
    ${marketSummary(m)}
    <div class="board">
      ${sectorBars(m)}
      ${trendChart(m)}
      <div class="rest">
        ${investorRanked(m)}
        ${countryBars(m)}
        ${stageHisto(m)}
        ${unicornBoard(m)}
      </div>
    </div>
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
      <svg class="gauge" viewBox="0 0 104 104" role="img" aria-label="Startup score ${s.overall} out of 100">
        <circle class="track" cx="52" cy="52" r="47"></circle>
        <circle class="fill" cx="52" cy="52" r="47" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${C.toFixed(1)}" data-off="${off.toFixed(1)}"></circle>
        <text x="52" y="50" text-anchor="middle" dominant-baseline="middle" class="gauge-num">${s.overall}</text>
        <text x="52" y="68" text-anchor="middle" class="gauge-label">/ 100</text>
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
    const chips = [];
    (p.founders || []).forEach(f => {
      const lum = f.pedigree === 'Luminary';
      const tag = f.pedigree && f.pedigree !== 'Unknown' ? ' · ' + esc(f.pedigree) : '';
      chips.push(`<span class="chip ${lum ? 'accent' : ''}">${esc(f.name)}${tag}</span>`);
    });
    chips.push(`<span class="chip">${esc(p.sector)}</span>`, `<span class="chip">${esc(p.headquarters)}</span>`);
    if (p.latestStage) chips.push(`<span class="chip primary">${esc(p.latestStage)}</span>`);
    if (p.latestValuation) chips.push(`<span class="chip accent">${p.latestValuation.display} valuation</span>`);
    if (p.hasExited) chips.push(`<span class="chip">Exited</span>`);
    const alerts = (p.riskFlags && p.riskFlags.length) ? `
      <div class="alerts">
        <h4>&#9888; Alerts</h4>
        ${p.riskFlags.map(f => `<div class="alert-row">
          <span class="alert-sev ${f.severity}">${f.severity}</span>
          <span class="alert-desc">${esc(f.description)}${f.date ? ` <span class="when">· ${f.date.slice(0,4)}</span>` : ''}</span>
        </div>`).join('')}
      </div>` : '';
    const events = p.events.slice().reverse().map(e => `<tr>
      <td class="n">${e.date}</td><td>${esc(e.stage)}</td><td class="n">${e.amount.display}</td>
      <td>${e.leadInvestor ? esc(e.leadInvestor) : '—'}</td>
      <td class="n">${e.postMoneyValuation ? e.postMoneyValuation.display : '—'}</td></tr>`).join('');
    detail.innerHTML = `<div class="view">
      <h1 class="view-title">${esc(p.name)}</h1>
      <p class="view-sub">${p.rounds} rounds · ${p.totalCapitalRaised.display} raised</p>
      <div style="display:flex;gap:0.4rem;flex-wrap:wrap">${chips.join('')}</div>
      ${alerts}
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
$('#nav').addEventListener('keydown', e => {
  if (e.key !== 'Enter' && e.key !== ' ') return;
  const li = e.target.closest('.nav-item');
  if (li) { e.preventDefault(); select(li.dataset.id); }
});
const goMarket = () => select('__market__');
$('#brand').addEventListener('click', goMarket);
$('#brand').addEventListener('keydown', e => {
  if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); goMarket(); }
});
// Keep the view in sync if the user edits the URL hash or uses back/forward.
window.addEventListener('hashchange', () => {
  const id = decodeURIComponent(location.hash.replace(/^#/, ''));
  const target = (id && state.companies.some(c => c.companyId === id)) ? id : '__market__';
  const current = state.selected || '__market__';
  if (target !== current) select(target);
});

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
