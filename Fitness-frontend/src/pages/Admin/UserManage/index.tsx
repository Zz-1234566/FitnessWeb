import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  adminUpdateUser,
  batchDeleteUsers,
  deleteUser,
  searchUsers,
} from '@/services/ant-design-pro/api';
import {
  Button,
  Checkbox,
  Empty,
  Form,
  Grid,
  Image as AntImage,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Spin,
  Tooltip,
  message,
} from 'antd';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';
import {
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
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

const StatCard: React.FC<{
  icon: React.ReactNode;
  value: number;
  label: string;
  delay?: string;
}> = ({ icon, value, label, delay }) => (
  <div className="stat-card" style={delay ? { animationDelay: delay } : undefined}>
    <div className="stat-card-header">
      <div className="stat-card-icon">{icon}</div>
    </div>
    <div className="stat-card-value">{value.toLocaleString()}</div>
    <div className="stat-card-label">{label}</div>
  </div>
);

const UserCell: React.FC<{ record: CurrentUser }> = ({ record }) => {
  const initial = record.username?.[0] || '?';
  const genderClass = record.gender === 1 ? 'male' : record.gender === 0 ? 'female' : 'unknown';
  const metaItems = [
    record.gender === 1 ? '男' : record.gender === 0 ? '女' : null,
    record.height ? `${record.height}cm` : null,
    record.weight ? `${record.weight}kg` : null,
    record.age ? `${record.age}岁` : null,
  ].filter(Boolean);
  const avatarEl = record.avatarUrl ? (
    <AntImage
      src={record.avatarUrl}
      width={40}
      height={40}
      preview
      style={{ borderRadius: 12, objectFit: 'cover' }}
      className="user-avatar-img"
    />
  ) : (
    <div className={`user-avatar-initial ${genderClass}`}>{initial}</div>
  );
  return (
    <div className="user-cell">
      {avatarEl}
      <div>
        <div className="user-name">{record.username}</div>
        <div className="user-account">{record.userAccount}</div>
        {metaItems.length ? <div className="user-meta">{metaItems.join(' · ')}</div> : null}
        <div className="user-chips">
          <span className={record.userRole === 1 ? 'tag tag-admin' : 'tag tag-user'}>
            {record.userRole === 1 ? '管理员' : '用户'}
          </span>
          {record.fitnessGoal ? <span className="tag tag-goal">{record.fitnessGoal}</span> : null}
        </div>
      </div>
    </div>
  );
};

const UserManage: React.FC = () => {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [form] = Form.useForm();
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [userData, setUserData] = useState<CurrentUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<number | 'all'>('all');
  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editingUser, setEditingUser] = useState<CurrentUser | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const userList = (await searchUsers({
        username: searchKeyword || undefined,
      })) as unknown as CurrentUser[];
      setUserData(userList || []);
    } catch (error: any) {
      message.error(error?.message || '加载用户失败');
    } finally {
      setLoading(false);
    }
  }, [searchKeyword]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const filteredUsers = useMemo(() => (
    roleFilter === 'all'
      ? userData
      : userData.filter((user) => user.userRole === roleFilter)
  ), [roleFilter, userData]);

  const stats = useMemo(() => {
    const total = userData.length;
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const todayNew = userData.filter((u) => {
      if (!u.createTime) return false;
      const d = new Date(u.createTime);
      const ds = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      return ds === todayStr;
    }).length;
    const admins = userData.filter((u) => u.userRole === 1).length;
    return { total, todayNew, admins };
  }, [userData]);

  const allChecked = filteredUsers.length > 0 && selectedRowKeys.length === filteredUsers.length;
  const indeterminate = selectedRowKeys.length > 0 && selectedRowKeys.length < filteredUsers.length;

  const handleSelectAll = (e: CheckboxChangeEvent) => {
    if (e.target.checked) {
      setSelectedRowKeys(filteredUsers.map((user) => Number(user.id)));
      return;
    }
    setSelectedRowKeys([]);
  };

  const handleToggleRow = (id?: number) => {
    if (!id) return;
    setSelectedRowKeys((current) => (
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id]
    ));
  };

  const handleRefresh = () => {
    setSelectedRowKeys([]);
    if (searchKeyword || roleFilter !== 'all') {
      setSearchKeyword('');
      setRoleFilter('all');
      return;
    }
    fetchUsers();
  };

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要删除的用户');
      return;
    }
    try {
      await batchDeleteUsers(selectedRowKeys);
      message.success(`成功删除 ${selectedRowKeys.length} 个用户`);
      setSelectedRowKeys([]);
      fetchUsers();
    } catch (error: any) {
      message.error(error?.message || '批量删除失败');
    }
  };

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

  const handleSaveEdit = async () => {
    if (!editingUser?.id) return;
    try {
      setEditLoading(true);
      const values = await form.validateFields();
      await adminUpdateUser({
        id: editingUser.id,
        ...values,
      });
      message.success('编辑成功');
      setEditOpen(false);
      setEditingUser(null);
      fetchUsers();
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.message || '编辑失败，请重试');
      }
    } finally {
      setEditLoading(false);
    }
  };

  const handleDeleteOne = async (id?: number) => {
    if (!id) return;
    try {
      await deleteUser(id);
      message.success('删除成功');
      setSelectedRowKeys((current) => current.filter((item) => item !== id));
      fetchUsers();
    } catch (error: any) {
      message.error(error?.message || '删除失败');
    }
  };

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h1>用户管理</h1>
        <p>管理和查看所有注册用户的信息</p>
      </div>

      <div className="stat-grid">
        <StatCard icon={<TeamOutlined />} value={stats.total} label="总用户数" delay="0.05s" />
        <StatCard icon={<UserAddOutlined />} value={stats.todayNew} label="今日新增" delay="0.1s" />
      </div>

      <div className="admin-main-card">
        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <span className="admin-toolbar-title">用户列表</span>
            <Tooltip title="刷新">
              <Button type="text" size="small" icon={<ReloadOutlined />} onClick={handleRefresh} />
            </Tooltip>
          </div>
          <div className="admin-toolbar-right">
            <Input
              placeholder="搜索用户名"
              prefix={<SearchOutlined style={{ color: 'var(--text-3)' }} />}
              allowClear
              size="middle"
              className="admin-search"
              style={{ width: isMobile ? '100%' : 200, borderRadius: 12 }}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={fetchUsers}
            />
            <Select
              placeholder="全部用户"
              className="admin-role-select"
              style={{ width: isMobile ? '100%' : 130 }}
              options={[
                { label: '全部用户', value: 'all' },
                { label: '管理员', value: 1 },
                { label: '普通用户', value: 0 },
              ]}
              value={roleFilter}
              onChange={(v) => setRoleFilter((v ?? 'all') as number | 'all')}
            />
          </div>
        </div>

        {!isMobile && (
          <div className="admin-list-batchbar">
            <Checkbox
              checked={allChecked}
              indeterminate={indeterminate}
              onChange={handleSelectAll}
            >
              已选 {selectedRowKeys.length} 项
            </Checkbox>
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              onClick={handleBatchDelete}
              disabled={selectedRowKeys.length === 0}
            >
              批量删除
            </Button>
          </div>
        )}

        {!isMobile && (
          <div className="admin-user-list-head">
            <div className="admin-user-col admin-user-col--check" />
            <div className="admin-user-col admin-user-col--user">用户信息</div>
            <div className="admin-user-col admin-user-col--gender">性别</div>
            <div className="admin-user-col admin-user-col--body">身体数据</div>
            <div className="admin-user-col admin-user-col--goal">目标</div>
            <div className="admin-user-col admin-user-col--role">角色</div>
            <div className="admin-user-col admin-user-col--time">注册时间</div>
            <div className="admin-user-col admin-user-col--actions">操作</div>
          </div>
        )}

        <Spin spinning={loading}>
          <div className="admin-user-list">
            {filteredUsers.length === 0 ? (
              <div className="admin-user-empty">
                <Empty description="暂无用户数据" />
              </div>
            ) : filteredUsers.map((user) => (
              <div key={user.id} className="admin-user-row">
                {!isMobile && (
                  <div className="admin-user-col admin-user-col--check">
                    <Checkbox
                      checked={selectedRowKeys.includes(Number(user.id))}
                      onChange={() => handleToggleRow(user.id)}
                    />
                  </div>
                )}
                <div className="admin-user-col admin-user-col--user">
                  <UserCell record={user} />
                </div>
                {!isMobile && (
                  <>
                    <div className="admin-user-col admin-user-col--gender">
                      {user.gender === 1 ? '男' : user.gender === 0 ? '女' : '-'}
                    </div>
                    <div className="admin-user-col admin-user-col--body body-stats">
                      {user.height ? <span>{user.height}cm</span> : null}
                      {user.weight ? <span>{user.weight}kg</span> : null}
                      {user.age ? <span>{user.age}岁</span> : null}
                    </div>
                    <div className="admin-user-col admin-user-col--goal">
                      {user.fitnessGoal ? <span className="tag tag-goal">{user.fitnessGoal}</span> : '-'}
                    </div>
                    <div className="admin-user-col admin-user-col--role">
                      {user.userRole === 1 ? (
                        <span className="tag tag-admin">管理员</span>
                      ) : (
                        <span className="tag tag-user">用户</span>
                      )}
                    </div>
                    <div className="admin-user-col admin-user-col--time">{user.createTime || '-'}</div>
                  </>
                )}
                <div className="admin-user-col admin-user-col--actions">
                  <button type="button" className="admin-link-btn" onClick={() => openEditor(user)}>
                    编辑
                  </button>
                  <Popconfirm
                    title="确认删除该用户？"
                    onConfirm={() => handleDeleteOne(user.id)}
                  >
                    <button type="button" className="admin-link-btn admin-link-btn--danger">
                      删除
                    </button>
                  </Popconfirm>
                </div>
              </div>
            ))}
          </div>
        </Spin>
      </div>

      <AppDesktopDialog
        open={editOpen}
        title="编辑用户信息"
        subtitle="统一维护用户基础资料和训练目标。"
        onClose={() => {
          setEditOpen(false);
          setEditingUser(null);
        }}
        footer={(
          <>
            <Button
              onClick={() => {
                setEditOpen(false);
                setEditingUser(null);
              }}
            >
              取消
            </Button>
            <Button type="primary" loading={editLoading} onClick={handleSaveEdit}>
              保存
            </Button>
          </>
        )}
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="fitnessGoal" label="健身目标">
              <Select allowClear options={fitnessGoals} />
            </Form.Item>
            <Form.Item name="age" label="年龄">
              <InputNumber min={10} max={100} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="height" label="身高">
              <InputNumber min={50} max={250} step={0.1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="weight" label="体重">
              <InputNumber min={20} max={300} step={0.1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="gender" label="性别">
              <Select
                allowClear
                options={[
                  { label: '男', value: 1 },
                  { label: '女', value: 0 },
                ]}
              />
            </Form.Item>
          </div>
        </Form>
      </AppDesktopDialog>
    </div>
  );
};

export default UserManage;
