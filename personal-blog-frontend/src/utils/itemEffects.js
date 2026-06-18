export function parseEffectConfig(effectConfig) {
  if (!effectConfig) return {};
  if (typeof effectConfig === 'object') return effectConfig;
  try {
    return JSON.parse(effectConfig);
  } catch {
    return {};
  }
}

export function effectClass(equippedItems, type) {
  const item = (equippedItems || []).find((entry) => entry.type === type);
  return parseEffectConfig(item?.effectConfig).className || '';
}

export function hasEquippedType(equippedItems, type) {
  return (equippedItems || []).some((entry) => entry.type === type);
}
