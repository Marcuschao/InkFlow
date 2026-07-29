const fontFamily = 'Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif';

const shared = {
  common: { primaryColor:'#d96343', primaryColorHover:'#bc4f33', primaryColorPressed:'#a8432b', primaryColorSuppl:'#d96343', borderRadius:'10px', fontFamily },
  Button: { borderRadiusMedium:'8px', borderRadiusSmall:'6px', heightMedium:'38px', heightSmall:'32px', fontWeight:'600' },
  Card: { borderRadius:'10px', paddingMedium:'20px' },
  Input: { borderRadius:'8px', heightMedium:'40px' },
  Tag: { borderRadius:'5px' },
  Tabs: { tabBorderRadius:'6px', tabFontWeightActive:'600' },
  Pagination: { itemBorderRadius:'6px' },
  DataTable: { borderRadius:'8px', thFontWeight:'600' },
  Modal: { borderRadius:'10px' },
};

export const lightThemeOverrides = {
  ...shared,
  common:{...shared.common,textColor1:'#24211f',textColor2:'#4f4a46',textColor3:'#6f6a65',borderColor:'#e5e0da',dividerColor:'#ece7e1',bodyColor:'#f7f5f2',cardColor:'#ffffff',hoverColor:'#fbede8',inputColor:'#ffffff',popoverColor:'#ffffff',modalColor:'#ffffff',boxShadow1:'0 8px 24px rgba(55,45,38,.08)',boxShadow2:'0 18px 48px rgba(55,45,38,.14)'},
  Button:{...shared.Button,textColorPrimary:'#fff',colorPrimary:'#d96343',colorHoverPrimary:'#bc4f33',colorPressedPrimary:'#a8432b',borderPrimary:'1px solid #d96343',borderHoverPrimary:'1px solid #bc4f33'},
  Menu:{itemColorActive:'#fbede8',itemColorActiveHover:'#f7d8cd',itemTextColorActive:'#bc4f33',itemTextColorActiveHover:'#bc4f33',borderRadius:'8px'},
  Input:{...shared.Input,border:'1px solid #e5e0da',borderHover:'1px solid #b9b0a8',borderFocus:'1px solid #d96343',boxShadowFocus:'0 0 0 3px rgba(217,99,67,.14)'},
  DataTable:{...shared.DataTable,thColor:'#f5f2ee',tdColor:'#fff',tdColorHover:'#fbede8',borderColor:'#e5e0da'},
  Tabs:{...shared.Tabs,tabColorSegment:'#fff',colorSegment:'#f1ede8',tabTextColorActiveSegment:'#bc4f33'},
};

export const darkThemeOverrides = {
  ...shared,
  common:{...shared.common,primaryColor:'#e47b5d',primaryColorHover:'#f08e71',primaryColorPressed:'#c96245',textColor1:'#f3ece5',textColor2:'#d8d1ca',textColor3:'#aaa29a',borderColor:'#3b3530',dividerColor:'#332e2a',bodyColor:'#151311',cardColor:'#211d1a',hoverColor:'#302a25',inputColor:'#25211e',popoverColor:'#25211e',modalColor:'#25211e',boxShadow1:'0 8px 24px rgba(0,0,0,.28)',boxShadow2:'0 18px 48px rgba(0,0,0,.38)'},
  Button:{...shared.Button,textColorPrimary:'#fff',colorPrimary:'#e47b5d',colorHoverPrimary:'#f08e71',colorPressedPrimary:'#c96245',borderPrimary:'1px solid #e47b5d'},
  Menu:{itemColorActive:'rgba(228,123,93,.14)',itemTextColorActive:'#f29a7f',borderRadius:'8px'},
  Input:{...shared.Input,border:'1px solid #3b3530',borderHover:'1px solid #5a5049',borderFocus:'1px solid #e47b5d',boxShadowFocus:'0 0 0 3px rgba(228,123,93,.14)'},
  DataTable:{...shared.DataTable,thColor:'#2b2521',tdColor:'#211d1a',tdColorHover:'#352923',borderColor:'#3b3530'},
  Tabs:{...shared.Tabs,tabColorSegment:'#2b2521',colorSegment:'#1b1816',tabTextColorActiveSegment:'#f08e71'},
};

export const themeOverrides = lightThemeOverrides;
