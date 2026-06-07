import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { adminUpdateUser, deleteUser, searchUsers } from '@/services/ant-design-pro/api';
import { Button, Empty, Form, Image as AntImage, Input, InputNumber, Popconfirm, Select, Spin, message } from 'antd';
import { DeleteOutlined, EditOutlined, ReloadOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons';
import AppDesktopDialog from '@/components/AppDesktopDialog';
import './index.less';

type CurrentUser = API.CurrentUser;

const fitnessGoals = [
  { value: 'muscle_gain', label: '增肌' },
  { value: 'fat_loss', label: '减脂' },
  { value: 'endurance', label: '耐力提升' },
  { value: 'flexibility', label: '柔韧性' },
  { value: 'general_fitness', label: '综合健身' },
];

const UserManage: React.FC = () => {
  const [form] = Form.useForm();
  const [userData, setUserData] = useState<CurrentUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const keywordRef = useRef(keyword);
  keywordRef.current = keyword;
  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editingUser, setEditingUser] = useState<CurrentUser | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res: any = await searchUsers({ username: keywordRef.current || undefined });
      setUserData(Array.isArray(res) ? res : []);
    } catch (error: any) {
      message.error(error?.message || '加载用户失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const openEditor = (user: CurrentUser) => {
    setEditingUser(user);
    form.setFieldsValue({
      username: user.username,
      gender: user.gender,
      height: user.height,
      weight: user.weight,
      age: user.age,
      fitnessGoal: user.fitnessGoal,
    });
    setEditOpen(true);
  };

  const handleSave = async () => {
    if (!editingUser?.id) return;
    try {
      setEditLoading(true);
      const values = await form.validateFields();
      await adminUpdateUser({ id: editingUser.id, ...values });
      message.success('编辑成功');
      setEditOpen(false);
      setEditingUser(null);
      fetchUsers();
    } catch (error: any) {
      if (!error?.errorFields) message.error(error?.message || '编辑失败');
    } finally {
      setEditLoading(false);
    }
  };

  const handleDelete = async (id?: number) => {
    if (!id) return;
    try {
      await deleteUser(id);
      message.success('删除成功');
      fetchUsers();
    } catch (error: any) {
      message.error(error?.message || '删除失败');
    }
  };

  return (
    <div className="um-page">
      <div className="um-header">
        <div className="um-header-text"><h1>用户管理</h1></div>
      </div>

      <div className="um-main-card">
        <div className="um-toolbar">
          <div className="um-toolbar-left">
            <span className="um-toolbar-title">用户列表</span>
            <span className="um-toolbar-count">{userData.length} 条</span>
          </div>
          <div className="um-toolbar-right">
            <Input
              placeholder="搜索用户名"
              prefix={<SearchOutlined style={{ color: 'var(--text-3)' }} />}
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={fetchUsers}
              style={{ width: 220, borderRadius: 10 }}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchUsers}>刷新</Button>
          </div>
        </div>

        <Spin spinning={loading}>
          {userData.length === 0 ? (
            <div className="um-empty"><Empty description="暂无用户数据" /></div>
          ) : (
            <div className="um-list">
              {userData.map((user) => (
                <div key={user.id} className="um-row-item">
                  <div className="um-row-info">
                    {user.avatarUrl ? (
                      <AntImage src={user.avatarUrl} width={36} height={36} preview={false} style={{ borderRadius: 10, objectFit: 'cover' }} />
                    ) : (
                      <div className="um-avatar">{user.username?.[0] || '?'}</div>
                    )}
                    <div className="um-row-text">
                      <div className="um-row-name">{user.username}{user.userRole === 1 ? ' (管理员)' : ''}</div>
                      <div className="um-row-sub">{user.userAccount}</div>
                    </div>
                  </div>
                  <div className="um-row-actions">
                    <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openEditor(user)}>编辑</Button>
                    <Popconfirm title="确认删除该用户？" onConfirm={() => handleDelete(user.id)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
                      <Button type="text" size="small" danger icon={<DeleteOutlined />}>删除</Button>
                    </Popconfirm>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Spin>
      </div>

      <AppDesktopDialog
        open={editOpen}
        title="编辑用户"
        onClose={() => { setEditOpen(false); setEditingUser(null); }}
        footer={
          <>
            <Button onClick={() => { setEditOpen(false); setEditingUser(null); }}>取消</Button>
            <Button type="primary" loading={editLoading} onClick={handleSave}>保存</Button>
          </>
        }
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="gender" label="性别">
              <Select allowClear options={[{ label: '男', value: 1 }, { label: '女', value: 0 }]} />
            </Form.Item>
            <Form.Item name="age" label="年龄">
              <InputNumber min={10} max={100} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="height" label="身高 (cm)">
              <InputNumber min={50} max={250} step={0.1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="weight" label="体重 (kg)">
              <InputNumber min={20} max={300} step={0.1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="fitnessGoal" label="健身目标" style={{ gridColumn: '1 / -1' }}>
              <Select allowClear options={fitnessGoals} />
            </Form.Item>
          </div>
        </Form>
      </AppDesktopDialog>
    </div>
  );
};

export default UserManage;
