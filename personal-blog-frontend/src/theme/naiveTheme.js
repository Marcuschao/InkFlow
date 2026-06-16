const fontFamily =
  'Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif';

export const themeOverrides = {
  common: {
    primaryColor: '#1E6FFF',
    primaryColorHover: '#0050E6',
    primaryColorPressed: '#0040CC',
    primaryColorSuppl: '#0050E6',
    borderRadius: '8px',
    fontFamily,
    textColor1: '#1D2129',
    textColor2: '#4E5969',
    textColor3: '#86909C',
    borderColor: '#E5E6EB',
    dividerColor: '#E5E6EB',
    boxShadow1: '0 2px 8px rgba(0, 0, 0, 0.04)',
    boxShadow2: '0 4px 16px rgba(0, 0, 0, 0.08)',
    bodyColor: '#F5F6F8',
    cardColor: '#FFFFFF',
    hoverColor: 'rgba(30, 111, 255, 0.06)',
  },
  Button: {
    borderRadiusMedium: '8px',
    borderRadiusSmall: '4px',
    heightMedium: '36px',
    fontWeight: '500',
  },
  Card: {
    borderRadius: '8px',
    paddingMedium: '16px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
  },
  Input: {
    borderRadius: '8px',
    boxShadowFocus: '0 0 0 2px rgba(30, 111, 255, 0.15)',
  },
  Tag: {
    borderRadius: '4px',
  },
  Tabs: {
    tabBorderRadius: '8px',
    tabFontWeightActive: '600',
  },
  Pagination: {
    itemBorderRadius: '8px',
  },
  DataTable: {
    borderRadius: '8px',
    thFontWeight: '600',
    thColor: '#F7F8FA',
  },
  Menu: {
    itemColorActive: 'rgba(30, 111, 255, 0.08)',
    itemColorActiveHover: 'rgba(30, 111, 255, 0.12)',
    itemTextColorActive: '#1E6FFF',
    itemTextColorActiveHover: '#0050E6',
  },
  Badge: {
    color: '#1E6FFF',
  },
  Switch: {
    railColorActive: '#1E6FFF',
  },
};
