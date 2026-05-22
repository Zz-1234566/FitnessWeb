import React from 'react';
import { Outlet } from '@umijs/max';

const Admin = () => {
  return (
    <div className="admin-page">
      <Outlet />
    </div>
  );
};

export default Admin;
