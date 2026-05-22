import React from 'react';
import CustomNavbar from '../CustomNavbar';
import Footer from '../Footer';
import './index.less';

const CustomLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="custom-layout">
    <CustomNavbar />
    <main className="custom-layout-main">{children}</main>
    <Footer />
  </div>
);

export default CustomLayout;
