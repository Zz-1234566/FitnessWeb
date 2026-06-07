-- 功能7：器械字段改为JSON数组
-- 1. 扩大字段
ALTER TABLE exercise MODIFY COLUMN equipment varchar(500) NOT NULL COMMENT '器械类型(JSON数组)';

-- 2. 逐条更新为具体器械JSON数组

-- ========== 胸部 ==========
UPDATE exercise SET equipment='["杠铃","卧推架","平凳"]' WHERE name='杠铃平卧推';
UPDATE exercise SET equipment='["哑铃","上斜凳"]' WHERE name='上斜哑铃卧推';
UPDATE exercise SET equipment='["哑铃","下斜凳"]' WHERE name='下斜哑铃卧推';
UPDATE exercise SET equipment='["哑铃","平凳"]' WHERE name='哑铃平板卧推';
UPDATE exercise SET equipment='["杠铃","上斜凳","卧推架"]' WHERE name='上斜杠铃卧推';
UPDATE exercise SET equipment='["推胸机"]' WHERE name='器械推胸';
UPDATE exercise SET equipment='["蝴蝶机"]' WHERE name='蝴蝶机夹胸';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索夹胸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='俯卧撑';
UPDATE exercise SET equipment='["无器械"]' WHERE name='上斜俯卧撑';
UPDATE exercise SET equipment='["无器械"]' WHERE name='钻石俯卧撑';
UPDATE exercise SET equipment='["哑铃","平凳"]' WHERE name='哑铃飞鸟';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='低位绳索夹胸';
UPDATE exercise SET equipment='["史密斯机","平凳"]' WHERE name='史密斯卧推';
UPDATE exercise SET equipment='["双杠"]' WHERE name='双杠臂屈伸';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带胸前交叉';

-- ========== 背部 ==========
UPDATE exercise SET equipment='["高位下拉器"]' WHERE name='高位下拉';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃划船';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃划船';
UPDATE exercise SET equipment='["绳索","划船机"]' WHERE name='坐姿绳索划船';
UPDATE exercise SET equipment='["划船机"]' WHERE name='器械坐姿划船';
UPDATE exercise SET equipment='["单杠"]' WHERE name='引体向上';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='直臂下压';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='单臂哑铃划船';
UPDATE exercise SET equipment='["T杠"]' WHERE name='T杠划船';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索面拉';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃耸肩';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃耸肩';
UPDATE exercise SET equipment='["无器械"]' WHERE name='超人式';

-- ========== 肩部 ==========
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃推举';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃推举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃侧平举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃前平举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃俯身飞鸟';
UPDATE exercise SET equipment='["推肩机"]' WHERE name='器械推肩';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索侧平举';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索前平举';
UPDATE exercise SET equipment='["史密斯机"]' WHERE name='史密斯推举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='阿诺德推举';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带侧平举';
UPDATE exercise SET equipment='["侧平举机"]' WHERE name='器械侧平举';

-- ========== 手臂 ==========
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃弯举';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃弯举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='锤式弯举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃交替弯举';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索弯举';
UPDATE exercise SET equipment='["牧师椅","杠铃"]' WHERE name='牧师椅弯举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='俯身臂屈伸';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索下压';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃臂屈伸';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃颈后臂屈伸';
UPDATE exercise SET equipment='["双杠"]' WHERE name='双杠臂屈伸';
UPDATE exercise SET equipment='["单杠"]' WHERE name='反握引体向上';
UPDATE exercise SET equipment='["弯举机"]' WHERE name='器械弯举';
UPDATE exercise SET equipment='["下压机","绳索"]' WHERE name='器械三头下压';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='窄握卧推';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='仰卧臂屈伸';

-- ========== 腿部 ==========
UPDATE exercise SET equipment='["杠铃","深蹲架"]' WHERE name='杠铃深蹲';
UPDATE exercise SET equipment='["无器械"]' WHERE name='徒手深蹲';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃深蹲';
UPDATE exercise SET equipment='["腿举机"]' WHERE name='腿举';
UPDATE exercise SET equipment='["腿屈伸机"]' WHERE name='坐姿腿屈伸';
UPDATE exercise SET equipment='["腿弯举机"]' WHERE name='俯卧腿弯举';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='哑铃弓步蹲';
UPDATE exercise SET equipment='["杠铃","深蹲架"]' WHERE name='杠铃硬拉';
UPDATE exercise SET equipment='["罗马椅"]' WHERE name='罗马椅挺身';
UPDATE exercise SET equipment='["提踵机"]' WHERE name='站姿提踵';
UPDATE exercise SET equipment='["史密斯机"]' WHERE name='史密斯深蹲';
UPDATE exercise SET equipment='["哈克深蹲机"]' WHERE name='哈克深蹲';
UPDATE exercise SET equipment='["倒蹬机"]' WHERE name='倒蹬';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='保加利亚分腿蹲';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带侧步走';
UPDATE exercise SET equipment='["杠铃"]' WHERE name='杠铃弓步蹲';
UPDATE exercise SET equipment='["髋外展机"]' WHERE name='器械髋外展';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='单腿罗马尼亚硬拉';
UPDATE exercise SET equipment='["无器械"]' WHERE name='高抬腿';
UPDATE exercise SET equipment='["无器械"]' WHERE name='原地高抬腿';
UPDATE exercise SET equipment='["跳绳"]' WHERE name='跳绳';
UPDATE exercise SET equipment='["无器械"]' WHERE name='弓步蹲';

-- ========== 核心 ==========
UPDATE exercise SET equipment='["无器械"]' WHERE name='平板支撑';
UPDATE exercise SET equipment='["无器械"]' WHERE name='侧平板支撑';
UPDATE exercise SET equipment='["无器械"]' WHERE name='卷腹';
UPDATE exercise SET equipment='["无器械"]' WHERE name='仰卧抬腿';
UPDATE exercise SET equipment='["无器械"]' WHERE name='俄罗斯转体';
UPDATE exercise SET equipment='["卷腹机"]' WHERE name='器械卷腹';
UPDATE exercise SET equipment='["无器械"]' WHERE name='登山者';
UPDATE exercise SET equipment='["无器械"]' WHERE name='死虫式';
UPDATE exercise SET equipment='["无器械"]' WHERE name='鸟狗式';
UPDATE exercise SET equipment='["无器械"]' WHERE name='仰卧交替抬腿';
UPDATE exercise SET equipment='["无器械"]' WHERE name='空中自行车';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带抗旋转';
UPDATE exercise SET equipment='["绳索","龙门架"]' WHERE name='绳索伐木';

-- ========== 热身动作（各肌群） ==========
UPDATE exercise SET equipment='["无器械"]' WHERE name='开合跳';
UPDATE exercise SET equipment='["无器械"]' WHERE name='手臂环绕';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带拉开';
UPDATE exercise SET equipment='["无器械"]' WHERE name='肩胛骨收缩';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带拉肩';
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE name='猫牛式';
UPDATE exercise SET equipment='["无器械"]' WHERE name='手臂交叉伸展';
UPDATE exercise SET equipment='["弹力带"]' WHERE name='弹力带外旋';
UPDATE exercise SET equipment='["无器械"]' WHERE name='手腕旋转';
UPDATE exercise SET equipment='["无器械"]' WHERE name='前臂伸展';
UPDATE exercise SET equipment='["无器械"]' WHERE name='腿部动态拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='髋关节环绕';
UPDATE exercise SET equipment='["无器械"]' WHERE name='躯干旋转';
UPDATE exercise SET equipment='["无器械"]' WHERE name='骨盆倾斜';

-- ========== 拉伸动作（各肌群） ==========
UPDATE exercise SET equipment='["无器械"]' WHERE name='胸部拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='门框胸肌拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='婴儿式';
UPDATE exercise SET equipment='["无器械"]' WHERE name='交叉手臂拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='三头肌拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='股四头肌拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='站立体前屈';
UPDATE exercise SET equipment='["无器械"]' WHERE name='坐姿体前屈';
UPDATE exercise SET equipment='["无器械"]' WHERE name='眼镜蛇式';
UPDATE exercise SET equipment='["无器械"]' WHERE name='腹部拉伸';

-- ========== 其他/新增动作 ==========
UPDATE exercise SET equipment='["战绳"]' WHERE name='战绳摆动';
UPDATE exercise SET equipment='["壶铃"]' WHERE name='壶铃摆荡';
UPDATE exercise SET equipment='["壶铃"]' WHERE name='壶铃深蹲';
UPDATE exercise SET equipment='["药球"]' WHERE name='药球旋转抛';
UPDATE exercise SET equipment='["TRX"]' WHERE name='TRX划船';
UPDATE exercise SET equipment='["TRX"]' WHERE name='TRX胸推';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='农夫行走';
UPDATE exercise SET equipment='["哑铃"]' WHERE name='泽奇深蹲';
UPDATE exercise SET equipment='["瑜伽垫"]' WHERE name='臀桥';
UPDATE exercise SET equipment='["无器械"]' WHERE name='动态拉伸';

-- ========== 额外收录动作 ==========
UPDATE exercise SET equipment='["无器械"]' WHERE name='肩部拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='手臂拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='背部拉伸';
UPDATE exercise SET equipment='["无器械"]' WHERE name='腿部拉伸';
