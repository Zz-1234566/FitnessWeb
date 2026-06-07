-- 功能6+7：器械字段改为JSON数组 + 动作库器械具体化
-- 使用 id 匹配，确保准确更新
-- 2026-06-07 根据生产数据库实际数据重写

-- 1. 扩大字段
ALTER TABLE exercise MODIFY COLUMN equipment varchar(500) NOT NULL COMMENT '器械类型(JSON数组)';

-- 2. 逐条更新（按id）

-- ========== 热身动作（id 1-30） ==========
UPDATE exercise SET equipment='["弹力带"]' WHERE id=1;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2;
UPDATE exercise SET equipment='["无器械"]' WHERE id=3;
UPDATE exercise SET equipment='["弹力带"]' WHERE id=4;
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE id=5;
UPDATE exercise SET equipment='["无器械"]' WHERE id=6;
UPDATE exercise SET equipment='["无器械"]' WHERE id=7;
UPDATE exercise SET equipment='["弹力带"]' WHERE id=8;
UPDATE exercise SET equipment='["无器械"]' WHERE id=9;
UPDATE exercise SET equipment='["无器械"]' WHERE id=10;
UPDATE exercise SET equipment='["弹力带"]' WHERE id=11;
UPDATE exercise SET equipment='["无器械"]' WHERE id=12;
UPDATE exercise SET equipment='["弹力带"]' WHERE id=13;
UPDATE exercise SET equipment='["无器械"]' WHERE id=14;
UPDATE exercise SET equipment='["无器械"]' WHERE id=15;
UPDATE exercise SET equipment='["无器械"]' WHERE id=16;
UPDATE exercise SET equipment='["无器械"]' WHERE id=17;
UPDATE exercise SET equipment='["无器械"]' WHERE id=18;
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE id=19;
UPDATE exercise SET equipment='["无器械"]' WHERE id=20;
UPDATE exercise SET equipment='["无器械"]' WHERE id=21;
UPDATE exercise SET equipment='["无器械"]' WHERE id=22;
UPDATE exercise SET equipment='["无器械"]' WHERE id=23;
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE id=24;
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE id=25;
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE id=26;
UPDATE exercise SET equipment='["无器械"]' WHERE id=27;
UPDATE exercise SET equipment='["无器械"]' WHERE id=28;
UPDATE exercise SET equipment='["无器械"]' WHERE id=29;
UPDATE exercise SET equipment='["无器械"]' WHERE id=30;

-- ========== 胸部（id 31-45） ==========
UPDATE exercise SET equipment='["杠铃","卧推架","平凳"]' WHERE id=31;
UPDATE exercise SET equipment='["哑铃","平凳"]' WHERE id=32;
UPDATE exercise SET equipment='["杠铃","上斜凳","卧推架"]' WHERE id=33;
UPDATE exercise SET equipment='["哑铃","上斜凳"]' WHERE id=34;
UPDATE exercise SET equipment='["杠铃","下斜凳","卧推架"]' WHERE id=35;
UPDATE exercise SET equipment='["哑铃","平凳"]' WHERE id=36;
UPDATE exercise SET equipment='["哑铃","上斜凳"]' WHERE id=37;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=38;
UPDATE exercise SET equipment='["无器械"]' WHERE id=39;
UPDATE exercise SET equipment='["无器械"]' WHERE id=40;
UPDATE exercise SET equipment='["双杠"]' WHERE id=41;
UPDATE exercise SET equipment='["推胸机"]' WHERE id=42;
UPDATE exercise SET equipment='["哑铃","下斜凳"]' WHERE id=43;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=44;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=45;

-- ========== 背部（id 46-61） ==========
UPDATE exercise SET equipment='["杠铃"]' WHERE id=46;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=47;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=48;
UPDATE exercise SET equipment='["高位下拉器"]' WHERE id=49;
UPDATE exercise SET equipment='["高位下拉器"]' WHERE id=50;
UPDATE exercise SET equipment='["绳索","划船机"]' WHERE id=51;
UPDATE exercise SET equipment='["单杠"]' WHERE id=52;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=53;
UPDATE exercise SET equipment='["T杠"]' WHERE id=54;
UPDATE exercise SET equipment='["划船机"]' WHERE id=55;
UPDATE exercise SET equipment='["高位下拉器"]' WHERE id=56;
UPDATE exercise SET equipment='["绳索"]' WHERE id=57;
UPDATE exercise SET equipment='["哑铃","平凳"]' WHERE id=58;
UPDATE exercise SET equipment='["高位下拉器"]' WHERE id=59;
UPDATE exercise SET equipment='["单杠"]' WHERE id=60;
UPDATE exercise SET equipment='["划船机"]' WHERE id=61;

-- ========== 肩部（id 62-75） ==========
UPDATE exercise SET equipment='["杠铃"]' WHERE id=62;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=63;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=64;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=65;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=66;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=67;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=68;
UPDATE exercise SET equipment='["推肩机"]' WHERE id=69;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=70;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=71;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=72;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=73;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=74;
UPDATE exercise SET equipment='["侧平举机"]' WHERE id=75;

-- ========== 手臂-二头（id 76-84） ==========
UPDATE exercise SET equipment='["杠铃"]' WHERE id=76;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=77;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=78;
UPDATE exercise SET equipment='["牧师椅","杠铃"]' WHERE id=79;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=80;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=81;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=82;
UPDATE exercise SET equipment='["哑铃","上斜凳"]' WHERE id=83;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=84;

-- ========== 手臂-三头（id 85-93） ==========
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=85;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=86;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=87;
UPDATE exercise SET equipment='["杠铃","卧推架"]' WHERE id=88;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=89;
UPDATE exercise SET equipment='["臂屈伸机"]' WHERE id=90;
UPDATE exercise SET equipment='["双杠"]' WHERE id=91;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=92;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=93;

-- ========== 腿部（id 94-125） ==========
UPDATE exercise SET equipment='["杠铃","深蹲架"]' WHERE id=94;
UPDATE exercise SET equipment='["杠铃","深蹲架"]' WHERE id=95;
UPDATE exercise SET equipment='["腿举机"]' WHERE id=96;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=97;
UPDATE exercise SET equipment='["腿屈伸机"]' WHERE id=98;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=99;
UPDATE exercise SET equipment='["哈克深蹲机"]' WHERE id=100;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=101;
UPDATE exercise SET equipment='["史密斯机"]' WHERE id=102;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=103;
UPDATE exercise SET equipment='["腿屈伸机"]' WHERE id=104;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=105;
UPDATE exercise SET equipment='["腿弯举机"]' WHERE id=106;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=107;
UPDATE exercise SET equipment='["腿弯举器"]' WHERE id=108;
UPDATE exercise SET equipment='["杠铃"]' WHERE id=109;
UPDATE exercise SET equipment='["壶铃"]' WHERE id=110;
UPDATE exercise SET equipment='["腿弯举机"]' WHERE id=111;
UPDATE exercise SET equipment='["腿弯举机"]' WHERE id=112;
UPDATE exercise SET equipment='["杠铃","卧推凳"]' WHERE id=113;
UPDATE exercise SET equipment='["哑铃","卧推凳"]' WHERE id=114;
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE id=115;
UPDATE exercise SET equipment='["壶铃"]' WHERE id=116;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=117;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=118;
UPDATE exercise SET equipment='["髋外展机"]' WHERE id=119;
UPDATE exercise SET equipment='["弹力带"]' WHERE id=120;
UPDATE exercise SET equipment='["提踵机"]' WHERE id=121;
UPDATE exercise SET equipment='["提踵机"]' WHERE id=122;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=123;
UPDATE exercise SET equipment='["腿举机"]' WHERE id=124;
UPDATE exercise SET equipment='["无器械"]' WHERE id=125;

-- ========== 核心（id 126-139） ==========
UPDATE exercise SET equipment='["无器械"]' WHERE id=126;
UPDATE exercise SET equipment='["无器械"]' WHERE id=127;
UPDATE exercise SET equipment='["无器械"]' WHERE id=128;
UPDATE exercise SET equipment='["无器械"]' WHERE id=129;
UPDATE exercise SET equipment='["单杠"]' WHERE id=130;
UPDATE exercise SET equipment='["无器械"]' WHERE id=131;
UPDATE exercise SET equipment='["卷腹机"]' WHERE id=132;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=133;
UPDATE exercise SET equipment='["无器械"]' WHERE id=134;
UPDATE exercise SET equipment='["健腹轮"]' WHERE id=135;
UPDATE exercise SET equipment='["单杠"]' WHERE id=136;
UPDATE exercise SET equipment='["无器械"]' WHERE id=137;
UPDATE exercise SET equipment='["无器械"]' WHERE id=138;
UPDATE exercise SET equipment='["无器械"]' WHERE id=139;

-- ========== 功能性训练（id 140-150） ==========
UPDATE exercise SET equipment='["壶铃"]' WHERE id=140;
UPDATE exercise SET equipment='["壶铃"]' WHERE id=141;
UPDATE exercise SET equipment='["药球"]' WHERE id=142;
UPDATE exercise SET equipment='["战绳"]' WHERE id=143;
UPDATE exercise SET equipment='["壶铃"]' WHERE id=144;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=145;
UPDATE exercise SET equipment='["六角杠铃"]' WHERE id=146;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=147;
UPDATE exercise SET equipment='["TRX"]' WHERE id=148;
UPDATE exercise SET equipment='["哑铃"]' WHERE id=149;
UPDATE exercise SET equipment='["腿举机"]' WHERE id=150;

-- ========== 额外收录动作（大ID） ==========
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568619966466;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568661909506;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568661909507;
UPDATE exercise SET equipment='["跳绳"]' WHERE id=2056243568661909508;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568729018370;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568729018371;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056243568791932930;
UPDATE exercise SET equipment='["无器械"]' WHERE id=2056319802511155201;
