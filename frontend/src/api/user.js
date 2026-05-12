import request from '../utils/request'

export function userRegister(data) {
  return request.post('/user/register', data)
}

export function userLogin(data) {
  return request.post('/user/login', data)
}

export function userLogout() {
  return request.post('/user/logout')
}

export function getLoginUser() {
  return request.get('/user/current')
}

export function userCreate(data) {
  return request.post('/user/create', data)
}

export function userDelete(data) {
  return request.post('/user/delete', data)
}

export function userUpdate(data) {
  return request.post('/user/update', data)
}

export function userListByPage(data) {
  return request.post('/user/list/page', data)
}

export function getUserById(id) {
  return request.get(`/user/get/${id}`)
}

export function getUserVOById(id) {
  return request.get(`/user/get/vo/${id}`)
}
