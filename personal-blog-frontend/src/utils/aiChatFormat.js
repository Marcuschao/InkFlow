export function stripCitationMarkers(text) {
  if (!text) return '';
  return text
    .replace(/\[\d+\]/g, '')
    .replace(/[ \t]+([，。；：、])/g, '$1')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

export function docDisplayName(title) {
  if (!title) return '未知文档';
  const base = String(title).split(/[/\\]/).pop() || String(title);
  return base.replace(/\.(md|markdown|txt|pdf|docx?|html?)$/i, '') || base;
}

export function formatSourceLine(sources, max = 3) {
  if (!sources?.length) return '';
  const names = [];
  const seen = new Set();
  for (const s of sources) {
    const name = docDisplayName(s.title);
    if (!seen.has(name)) {
      seen.add(name);
      names.push(name);
    }
  }
  if (!names.length) return '';
  const shown = names.slice(0, max);
  const more = names.length > max;
  return `来源：${shown.join('、')}${more ? ' 等' : ''}`;
}
