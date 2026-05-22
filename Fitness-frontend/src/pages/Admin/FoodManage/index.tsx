import { PlusOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Empty, Form, Image, Input, InputNumber, Popconfirm, Select, Spin, Upload, message } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { deleteFoodItem, listAdminFoods, saveFoodItem, uploadFoodImage } from '@/services/ant-design-pro/api';
import AppDesktopDialog from '@/components/AppDesktopDialog';
import '../UserManage/index.less';

const categoryOptions = ['主食', '蛋白质', '蔬菜', '水果', '乳制品', '坚果', '饮品', '补剂', '即食']
  .map((value) => ({ label: value, value }));

const kjToKcal = (value?: number) => (value || 0) / 4.184;

const FoodManage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [foodList, setFoodList] = useState<API.FoodItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<API.FoodItem | null>(null);
  const [form] = Form.useForm();

  const fetchFoods = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listAdminFoods(keyword || undefined);
      setFoodList(data || []);
    } catch (error: any) {
      message.error(error?.message || '加载食物失败');
    } finally {
      setLoading(false);
    }
  }, [keyword]);

  useEffect(() => {
    fetchFoods();
  }, [fetchFoods]);

  const openEditor = (record?: API.FoodItem) => {
    setEditing(record || null);
    form.setFieldsValue(record || {
      unit: 'g',
      baseAmount: 100,
      calories: 0,
      protein: 0,
      carbs: 0,
      fat: 0,
      fiber: 0,
      isSystem: 1,
    });
    setOpen(true);
  };

  const groupedCount = useMemo(() => ({
    total: foodList.length,
    system: foodList.filter((item) => item.isSystem === 1).length,
    custom: foodList.filter((item) => item.isSystem !== 1).length,
  }), [foodList]);

  const closeEditor = () => {
    setOpen(false);
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const values = await form.validateFields();
      await saveFoodItem({ ...editing, ...values, id: editing?.id });
      message.success('保存成功');
      setOpen(false);
      fetchFoods();
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.message || '保存失败');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (foodId?: number) => {
    if (!foodId) return;
    try {
      await deleteFoodItem(foodId);
      message.success('删除成功');
      if (editing?.id === foodId) {
        setOpen(false);
        setEditing(null);
      }
      fetchFoods();
    } catch (error: any) {
      message.error(error?.message || '删除失败');
    }
  };

  const renderEditorBody = () => (
    <Form form={form} layout="vertical" requiredMark={false}>
      <Form.Item name="name" label="食物名称" rules={[{ required: true }]}>
        <Input maxLength={60} />
      </Form.Item>
      <Form.Item name="imageUrl" label="食物图片">
        <Input
          addonAfter={(
            <Upload
              showUploadList={false}
              customRequest={async ({ file, onSuccess, onError }) => {
                try {
                  const fd = new FormData();
                  fd.append('file', file as File);
                  if (editing?.id) {
                    fd.append('foodId', String(editing.id));
                  }
                  const url = await uploadFoodImage(fd);
                  form.setFieldValue('imageUrl', url);
                  onSuccess?.(url);
                } catch (e) {
                  onError?.(e as Error);
                }
              }}
            >
              <Button type="link" icon={<UploadOutlined />}>上传</Button>
            </Upload>
          )}
        />
      </Form.Item>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
        <Form.Item name="category" label="分类">
          <Select options={categoryOptions} allowClear />
        </Form.Item>
        <Form.Item name="unit" label="单位" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="baseAmount" label="基准量" rules={[{ required: true }]}>
          <InputNumber min={1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="calories" label="能量(kJ)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="protein" label="蛋白质(g)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="carbs" label="碳水(g)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="fat" label="脂肪(g)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="fiber" label="纤维(g)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <div style={{ color: 'var(--text-3)', fontSize: 12 }}>录入食物能量时填写包装上的 kJ，系统会自动按 1 kcal = 4.184 kJ 换算展示。</div>
    </Form>
  );

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h1>食物管理</h1>
        <p>管理员可维护全部食物，包括用户自定义食物和图片。</p>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-card-value">{groupedCount.total}</div>
          <div className="stat-card-label">食物总数</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value">{groupedCount.system}</div>
          <div className="stat-card-label">系统食物</div>
        </div>
      </div>

      <div className="admin-main-card">
        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <span className="admin-toolbar-title">食物列表</span>
            <Button type="text" size="small" icon={<ReloadOutlined />} onClick={fetchFoods} />
          </div>
          <div className="admin-toolbar-right">
            <Input
              placeholder="搜索食物"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={fetchFoods}
              style={{ width: 220 }}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>
              新建食物
            </Button>
          </div>
        </div>

        <div className="admin-user-list-head">
          <div className="admin-user-col admin-user-col--user">食物</div>
          <div className="admin-user-col admin-user-col--goal">能量</div>
          <div className="admin-user-col admin-user-col--body">营养</div>
          <div className="admin-user-col admin-user-col--role">归属</div>
          <div className="admin-user-col admin-user-col--actions">操作</div>
        </div>

        <Spin spinning={loading}>
          <div className="admin-user-list">
            {foodList.length === 0 ? (
              <div className="admin-user-empty">
                <Empty description="暂无食物数据" />
              </div>
            ) : foodList.map((record) => (
              <div key={record.id} className="admin-user-row admin-food-row">
                <div className="admin-user-col admin-user-col--user">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    {record.imageUrl ? (
                      <Image src={record.imageUrl} width={44} height={44} style={{ borderRadius: 12, objectFit: 'cover' }} />
                    ) : (
                      <div className="user-avatar-initial unknown" style={{ width: 44, height: 44, borderRadius: 12 }}>食</div>
                    )}
                    <div>
                      <div className="user-name">{record.name}</div>
                      <div className="user-meta">{record.category || '未分类'} · {record.baseAmount || 100} {record.unit || 'g'}</div>
                    </div>
                  </div>
                </div>
                <div className="admin-user-col admin-user-col--goal">
                  {(record.calories || 0).toFixed(0)} kJ / {kjToKcal(record.calories).toFixed(0)} kcal
                </div>
                <div className="admin-user-col admin-user-col--body body-stats">
                  <span>蛋白 {record.protein || 0}g</span>
                  <span>碳水 {record.carbs || 0}g</span>
                  <span>脂肪 {record.fat || 0}g</span>
                  <span>纤维 {record.fiber || 0}g</span>
                </div>
                <div className="admin-user-col admin-user-col--role">
                  {record.isSystem === 1 ? '系统食物' : `用户#${record.createdBy || '-'}`}
                </div>
                <div className="admin-user-col admin-user-col--actions">
                  <button type="button" className="admin-link-btn" onClick={() => openEditor(record)}>
                    编辑
                  </button>
                  <Popconfirm
                    title="确认删除这个食物？"
                    description="删除后将不再出现在食物库里。"
                    onConfirm={() => handleDelete(record.id)}
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
        open={open}
        title={editing ? '编辑食物' : '新建食物'}
        subtitle="统一维护食物图片、营养信息和系统分类。"
        onClose={closeEditor}
        footer={(
          <>
            {editing?.id ? (
              <Popconfirm
                title="确认删除这个食物？"
                description="删除后将不再出现在食物库里。"
                onConfirm={() => handleDelete(editing.id)}
              >
                <Button danger>删除食物</Button>
              </Popconfirm>
            ) : (
              <Button onClick={closeEditor}>取消</Button>
            )}
            <Button type="primary" loading={saving} onClick={handleSave}>
              保存
            </Button>
          </>
        )}
      >
        {renderEditorBody()}
      </AppDesktopDialog>
    </div>
  );
};

export default FoodManage;
