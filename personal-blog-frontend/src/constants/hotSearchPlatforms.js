export const HOT_SEARCH_PLATFORM_META = {
  weibo: { color: '#E6162D', short: '微', searchUrl: (q) => `https://s.weibo.com/weibo?q=${encodeURIComponent(q)}` },
  zhihu: { color: '#0066FF', short: '知', searchUrl: (q) => `https://www.zhihu.com/search?type=content&q=${encodeURIComponent(q)}` },
  toutiao: { color: '#F04142', short: '头', searchUrl: (q) => `https://www.toutiao.com/search/?keyword=${encodeURIComponent(q)}` },
  baidu: { color: '#2932E1', short: '百', searchUrl: (q) => `https://www.baidu.com/s?wd=${encodeURIComponent(q)}` },
  csdn: { color: '#FC5531', short: 'C', searchUrl: (q) => `https://so.csdn.net/so/search?q=${encodeURIComponent(q)}` },
};

export function getPlatformMeta(sourceId) {
  return HOT_SEARCH_PLATFORM_META[sourceId] || { color: '#4E5969', short: '热', searchUrl: (q) => `https://www.baidu.com/s?wd=${encodeURIComponent(q)}` };
}

export function buildPlatformSearchUrl(sourceId, keyword) {
  const q = keyword.trim();
  if (!q) return '';
  return getPlatformMeta(sourceId).searchUrl(q);
}
