//SVG 图表
import { fmt } from './core.js';

const W = 720;

function niceScale(rawMax, count = 4, integer = false) {
  if (!(rawMax > 0)) rawMax = integer ? count : 10;
  const rough = rawMax / count;
  const mag = Math.pow(10, Math.floor(Math.log10(rough)));
  const norm = rough / mag;
  const ladder = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10;
  let step = ladder * mag;
  if (integer) step = Math.max(1, Math.ceil(step));
  return { max: step * count, step, count };
}

function axisLabels({ max, step, count }) {
  return Array.from({ length: count + 1 }, (_, i) => Math.round(step * i * 1000) / 1000);
}

function shortDate(d) {
  return String(d).slice(5).replace('-', '/');
}

/** 折线 / 面积图 series: [{ name, color, values: number[] }] */
export function lineChart({ labels, series, height = 210, format = fmt.money, integer = false }) {
  const padL = 46, padR = 12, padT = 14, padB = 26;
  const innerW = W - padL - padR;
  const innerH = height - padT - padB;
  const scale = niceScale(Math.max(...series.flatMap((s) => s.values), 0) * 1.12, 4, integer);
  const max = scale.max;
  const x = (i) => padL + (labels.length <= 1 ? innerW / 2 : (innerW * i) / (labels.length - 1));
  const y = (v) => padT + innerH - (innerH * Math.max(0, v)) / max;

  const grid = axisLabels(scale)
    .map((v) => `<line x1="${padL}" y1="${y(v).toFixed(1)}" x2="${W - padR}" y2="${y(v).toFixed(1)}"/>`)
    .join('');
  const axis = axisLabels(scale)
    .map((v) => `<text x="${padL - 8}" y="${y(v) + 3.5}" text-anchor="end">${format(v)}</text>`)
    .join('');

  const step = Math.ceil(labels.length / 8);
  const xLabels = labels
    .map((d, i) => (i % step === 0 || i === labels.length - 1
      ? `<text x="${x(i).toFixed(1)}" y="${height - 8}" text-anchor="middle">${shortDate(d)}</text>`
      : ''))
    .join('');

  const paths = series.map((s) => {
    const pts = s.values.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`);
    const line = `<polyline points="${pts.join(' ')}" fill="none" stroke="${s.color}" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round"/>`;
    const area = `<polygon points="${x(0).toFixed(1)},${padT + innerH} ${pts.join(' ')} ${x(labels.length - 1).toFixed(1)},${padT + innerH}" fill="${s.color}" fill-opacity=".08"/>`;
    const dots = s.values.map((v, i) => `<circle cx="${x(i).toFixed(1)}" cy="${y(v).toFixed(1)}" r="2.6" fill="#fff" stroke="${s.color}" stroke-width="2"/>`).join('');
    return `<g>${s.area === false ? '' : area}${line}${dots}</g>`;
  }).join('');

  return `<div class="chart"><svg viewBox="0 0 ${W} ${height}" role="img">
    <g class="grid">${grid}</g>
    <g class="axis">${axis}${xLabels}</g>
    ${paths}
  </svg></div>`;
}

/** 分组柱状图 */
export function barChart({ labels, series, height = 210, format = (v) => fmt.num(v), integer = true, legend = '' }) {
  const padL = 40, padR = 12, padT = 14, padB = 26;
  const innerW = W - padL - padR;
  const innerH = height - padT - padB;
  const scale = niceScale(Math.max(...series.flatMap((s) => s.values), 0) * 1.1, 4, integer);
  const max = scale.max;
  const groupW = innerW / Math.max(1, labels.length);
  const barW = Math.min(22, (groupW * 0.62) / series.length);
  const y = (v) => padT + innerH - (innerH * Math.max(0, v)) / max;

  const grid = axisLabels(scale)
    .map((v) => `<line x1="${padL}" y1="${y(v).toFixed(1)}" x2="${W - padR}" y2="${y(v).toFixed(1)}"/>`).join('');
  const axis = axisLabels(scale)
    .map((v) => `<text x="${padL - 8}" y="${y(v) + 3.5}" text-anchor="end">${format(v)}</text>`).join('');

  const step = Math.ceil(labels.length / 8);
  const xLabels = labels.map((d, i) => (i % step === 0
    ? `<text x="${(padL + groupW * i + groupW / 2).toFixed(1)}" y="${height - 8}" text-anchor="middle">${shortDate(d)}</text>`
    : '')).join('');

  const bars = labels.map((_, i) => {
    const base = padL + groupW * i + (groupW - barW * series.length) / 2;
    return series.map((s, si) => {
      const v = s.values[i] || 0;
      const h = Math.max(v > 0 ? 2 : 0, padT + innerH - y(v));
      return `<rect x="${(base + barW * si).toFixed(1)}" y="${(padT + innerH - h).toFixed(1)}" width="${barW.toFixed(1)}" height="${h.toFixed(1)}" rx="3" fill="${s.color}" fill-opacity="${si === 0 ? 1 : .55}"/>`;
    }).join('');
  }).join('');

  return `<div class="chart"><svg viewBox="0 0 ${W} ${height}" role="img">
    <g class="grid">${grid}</g>
    <g class="axis">${axis}${xLabels}</g>
    ${bars}
  </svg>${legend ? `<div class="legend">${legend}</div>` : ''}</div>`;
}

/** 横向条形（Top10 动销） */
export function hbarChart({ labels, values, height = 300, color = '#2f9e44', format = (v) => fmt.num(v) + ' 件' }) {
  const padL = 108, padR = 56, padT = 8;
  const rowH = (height - padT - 8) / Math.max(1, labels.length);
  const innerW = W - padL - padR;
  const max = Math.max(...values, 1);

  const rows = labels.map((label, i) => {
    const v = values[i] || 0;
    const w = Math.max(v > 0 ? 3 : 0, (innerW * v) / max);
    const top = padT + rowH * i + rowH / 2;
    return `<g>
      <text x="${padL - 12}" y="${top + 4}" text-anchor="end" style="font-size:10px" fill="#5c6b61">${label.length > 9 ? label.slice(0, 9) + '…' : label}</text>
      <rect x="${padL}" y="${top - 7}" width="${innerW}" height="14" rx="7" fill="#f1f5f2"/>
      <rect x="${padL}" y="${top - 7}" width="${w.toFixed(1)}" height="14" rx="7" fill="${color}" fill-opacity="${0.95 - i * 0.045}"/>
      <text x="${W - 8}" y="${top + 4}" text-anchor="end" style="font-size:10px" fill="#6f7f74">${format(v)}</text>
    </g>`;
  }).join('');

  return `<div class="chart"><svg viewBox="0 0 ${W} ${height}" role="img">${rows}</svg></div>`;
}

/** 环形图（占比） */
export function donutChart({ items, size = 168, thickness = 18 }) {
  const sum = items.reduce((s, it) => s + Math.max(0, it.value), 0);
  const total = sum || 1;
  const r = size / 2 - thickness / 2 - 2;
  const c = size / 2;
  let acc = -Math.PI / 2;
  const arcs = items.map((it) => {
    const value = Math.max(0, it.value);
    if (value <= 0) return '';
    const angle = (value / total) * Math.PI * 2;
    const a0 = acc;
    acc += angle;
    const a1 = acc;
    //只有一段时直接画整圈
    if (angle >= Math.PI * 2 - 0.001) {
      return `<circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="${it.color}" stroke-width="${thickness}"/>`;
    }
    const large = angle > Math.PI ? 1 : 0;
    const x0 = c + r * Math.cos(a0), y0 = c + r * Math.sin(a0);
    const x1 = c + r * Math.cos(a1), y1 = c + r * Math.sin(a1);
    return `<path d="M ${x0.toFixed(2)} ${y0.toFixed(2)} A ${r} ${r} 0 ${large} 1 ${x1.toFixed(2)} ${y1.toFixed(2)}" fill="none" stroke="${it.color}" stroke-width="${thickness}" stroke-linecap="butt"/>`;
  }).join('');
  return `<div class="donut-wrap">
    <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}">
      <circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="#f1f5f2" stroke-width="${thickness}"/>
      ${arcs}
      <text x="${c}" y="${c - 1}" text-anchor="middle" class="donut-num">${fmt.num(sum)}</text>
      <text x="${c}" y="${c + 15}" text-anchor="middle" class="donut-cap">合计</text>
    </svg>
    <div class="legend donut-legend">${items.map((it) =>
      `<span><i style="background:${it.color}"></i>${it.name}<b>${fmt.num(it.value)}</b></span>`).join('')}</div>
  </div>`;
}
