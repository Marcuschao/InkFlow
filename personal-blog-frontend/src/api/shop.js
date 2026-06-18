import request from '../utils/request';

export function fetchShopItems() {
  return request({ url: '/shop/items', method: 'get' });
}

export function buyItem(itemId) {
  return request({ url: `/shop/buy/${itemId}`, method: 'post' });
}
