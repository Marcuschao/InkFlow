const fontFamily =
  '"Inter", "DM Sans", -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif';

const brutalBorder = '2px solid';
const brutalShadow = '4px 4px 0 #000';
const brutalShadowLg = '5px 5px 0 #000';

const shared = {
  Button: {
    borderRadiusMedium: '10px',
    borderRadiusSmall: '8px',
    heightMedium: '40px',
    fontWeight: '700',
  },
  Card: {
    borderRadius: '20px',
    paddingMedium: '20px',
  },
  Input: {
    borderRadius: '10px',
  },
  Tag: {
    borderRadius: '999px',
  },
  Tabs: {
    tabBorderRadius: '999px',
    tabFontWeightActive: '700',
    tabColorSegment: '#E8C547',
    tabTextColorActiveSegment: '#000000',
    tabTextColorSegment: '#52525B',
    tabTextColorHoverSegment: '#000000',
    colorSegment: '#FFFFFF',
  },
  Pagination: {
    itemBorderRadius: '10px',
  },
  DataTable: {
    borderRadius: '20px',
    thFontWeight: '700',
  },
  Modal: {
    borderRadius: '20px',
  },
};

export const darkThemeOverrides = {
  ...shared,
  common: {
    primaryColor: '#D4AF37',
    primaryColorHover: '#E8C547',
    primaryColorPressed: '#C4A030',
    primaryColorSuppl: '#D4AF37',
    borderRadius: '10px',
    fontFamily,
    textColor1: '#FAFAFA',
    textColor2: '#A1A1AA',
    textColor3: '#71717A',
    borderColor: 'rgba(255,255,255,0.85)',
    dividerColor: 'rgba(255,255,255,0.85)',
    boxShadow1: '4px 4px 0 rgba(255,255,255,0.12)',
    boxShadow2: '5px 5px 0 rgba(255,255,255,0.12)',
    bodyColor: '#121212',
    cardColor: '#1A1A1A',
    hoverColor: 'rgba(255, 214, 0, 0.12)',
    inputColor: '#1A1A1A',
    popoverColor: '#1A1A1A',
    modalColor: '#1A1A1A',
  },
  Button: {
    ...shared.Button,
    textColorPrimary: '#000000',
    textColorHoverPrimary: '#000000',
    textColorPressedPrimary: '#000000',
    textColorFocusPrimary: '#000000',
    borderPrimary: '2px solid rgba(255,255,255,0.85)',
    borderHoverPrimary: '2px solid rgba(255,255,255,0.85)',
    borderPressedPrimary: '2px solid rgba(255,255,255,0.85)',
    borderFocusPrimary: '2px solid rgba(255,255,255,0.85)',
    colorPrimary: '#D4AF37',
    colorHoverPrimary: '#E8C547',
    colorPressedPrimary: '#C4A030',
    colorFocusPrimary: '#D4AF37',
  },
  Menu: {
    itemColorActive: '#FFD600',
    itemColorActiveHover: '#FFE033',
    itemTextColorActive: '#000000',
    itemTextColorActiveHover: '#000000',
    borderRadius: '999px',
  },
  Input: {
    borderRadius: '10px',
    boxShadowFocus: '0 0 0 2px #FFD600',
    color: '#1A1A1A',
    colorFocus: '#1A1A1A',
    border: '2px solid rgba(255,255,255,0.85)',
    borderHover: '2px solid rgba(255,255,255,0.85)',
    borderFocus: '2px solid #FFD600',
  },
  Card: {
    borderRadius: '20px',
    paddingMedium: '20px',
    color: '#1A1A1A',
    colorModal: '#1A1A1A',
    borderColor: 'rgba(255,255,255,0.85)',
  },
  Tag: {
    borderRadius: '999px',
    colorBordered: 'rgba(255,255,255,0.85)',
  },
  DataTable: {
    borderRadius: '20px',
    thFontWeight: '700',
    thColor: '#222222',
    tdColor: 'transparent',
    tdColorHover: 'rgba(255, 214, 0, 0.12)',
    borderColor: 'rgba(255,255,255,0.85)',
  },
  Badge: {
    color: '#FFD600',
  },
  Switch: {
    railColorActive: '#FFD600',
  },
  Tabs: {
    ...shared.Tabs,
    tabColorSegment: '#E8C547',
    tabTextColorActiveSegment: '#000000',
    tabTextColorSegment: '#A1A1AA',
    tabTextColorHoverSegment: '#FAFAFA',
    colorSegment: '#1A1A1A',
  },
};

export const lightThemeOverrides = {
  ...shared,
  common: {
    primaryColor: '#E8C547',
    primaryColorHover: '#D4AF37',
    primaryColorPressed: '#C4A030',
    primaryColorSuppl: '#E8C547',
    borderRadius: '10px',
    fontFamily,
    textColor1: '#000000',
    textColor2: '#52525B',
    textColor3: '#71717A',
    borderColor: '#000000',
    dividerColor: '#000000',
    boxShadow1: brutalShadow,
    boxShadow2: brutalShadowLg,
    bodyColor: '#FFFFFF',
    cardColor: '#FFFFFF',
    hoverColor: 'rgba(255, 214, 0, 0.2)',
  },
  Button: {
    ...shared.Button,
    textColorPrimary: '#000000',
    textColorHoverPrimary: '#000000',
    textColorPressedPrimary: '#000000',
    textColorFocusPrimary: '#000000',
    borderPrimary: '2px solid #000000',
    borderHoverPrimary: '2px solid #000000',
    borderPressedPrimary: '2px solid #000000',
    borderFocusPrimary: '2px solid #000000',
    colorPrimary: '#E8C547',
    colorHoverPrimary: '#D4AF37',
    colorPressedPrimary: '#C4A030',
    colorFocusPrimary: '#E8C547',
  },
  Menu: {
    itemColorActive: '#000000',
    itemColorActiveHover: '#000000',
    itemTextColorActive: '#FFFFFF',
    itemTextColorActiveHover: '#FFFFFF',
    borderRadius: '999px',
  },
  Input: {
    borderRadius: '10px',
    boxShadowFocus: '0 0 0 2px #FFD600',
    border: '2px solid #000000',
    borderHover: '2px solid #000000',
    borderFocus: '2px solid #FFD600',
  },
  Card: {
    borderRadius: '20px',
    paddingMedium: '20px',
    boxShadow: brutalShadowLg,
    borderColor: '#000000',
  },
  Tag: {
    borderRadius: '999px',
    colorBordered: '#000000',
  },
  DataTable: {
    borderRadius: '20px',
    thFontWeight: '700',
    thColor: '#F4F4F5',
    borderColor: '#000000',
  },
  Badge: {
    color: '#EF4444',
  },
};

export const themeOverrides = lightThemeOverrides;
