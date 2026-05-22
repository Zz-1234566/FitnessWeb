export default [
  // 根路径重定向到欢迎页
  { path: '/', redirect: '/welcome' },

  // 登录注册页面路由
  {
    path: '/user',
    layout: false,
    routes: [
      {name: '登录', path: '/user/Login', component: './user/Auth'},
      {name: '注册', path: '/user/register', component: './user/Auth'},
      { name: '安全中心', path: '/user/updatePassword', component: './user/Auth' },
    ],
  },

// 公共可直接访问页面路由
  {path: '/welcome', name: '训练导航', icon: 'trophy', component: './welcome'},
  {path: '/exercises', name: '动作库', icon: 'fire', component: './exercises'},

// 用户页面路由
  {path: '/user/chat', name: '智能教练', icon: 'smile', component: './user/chat'},
  {path: '/user/favorites', name: '我的收藏', icon: 'star', component: './user/favorites'},
  {path: '/user/profile', name: '个人中心', icon: 'user', component: './user/profile'},

  // 管理页面路由
  {
    path: '/admin',
    name: '管理页',
    icon: 'crown',
    access: 'canAdmin',
    component: './Admin',
    routes: [
      {path: '/admin/user-manage', name: '用户管理', component: './Admin/UserManage'},
      {path: '/admin/food-manage', name: '食物管理', component: './Admin/FoodManage'},
    ],
  },

  // 全局 404（layout: false 不走 onPageChange，不会被误跳转到登录页）
  {path: '*', layout: false, component: './404'},

];
