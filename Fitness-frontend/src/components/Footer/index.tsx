import {GithubOutlined} from '@ant-design/icons';
import React from 'react';
import {BEIANPNG} from "@/constants";

const Footer: React.FC = () => {
  return (
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div>
        Tatan Smart Fitness_Web
        <span style={{ margin: '0 12px', color: '#999' }}>|</span>
        <a href="https://github.com/Zz-1234566" target="_blank" rel="noreferrer" style={{ color: 'black' }}>
             <GithubOutlined /> GitHub
        </a>
      </div>
      <div>
        <a href="https://beian.miit.gov.cn/#/Integrated/index" target="_blank" rel="noreferrer" style={{ color: 'black' }}>
          粤ICP备2026042877号-1
        </a>
        <span style={{ margin: '0 12px', color: '#999' }}>|</span>
        <a href="https://beian.mps.gov.cn/#/query/webSearch?code=44051302000268" target="_blank" rel="noreferrer" style={{ color: 'black' }}>
        <img
          src={BEIANPNG}
          style={{ width: 16, verticalAlign: 'middle', marginRight: 4 }}
        />
         粤公网安备44051302000268号
      </a>
      </div>
    </div>
  );
};

export default Footer;
