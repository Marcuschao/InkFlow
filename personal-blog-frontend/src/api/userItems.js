import request from '../utils/request';

export function fetchUserItems() {
  return request({ url: '/user/items', method: 'get' });
}

export function equipUserItem(id) {
  return request({ url: `/user/items/${id}/equip`, method: 'post' });
}

export function unequipUserItem(id) {
  return request({ url: `/user/items/${id}/unequip`, method: 'post' });
}
