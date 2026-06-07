import { CameraOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, ShopOutlined, UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Form, Image, Input, InputNumber, Popconfirm, Select, Spin, Upload, message } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useModel } from '@umijs/max';
import { deleteFoodItem, listAdminFoods, saveFoodItem, uploadFoodImage, recognizeFoodImage } from '@/services/ant-design-pro/api';
import AppDesktopDialog from '@/components/AppDesktopDialog';
import '../UserManage/index.less';
import './index.less';

const categoryOptions = ['主食', '蛋白质', '蔬菜', '水果', '乳制品', '坚果', '饮品', '补剂', '即食']
  .map((value) => ({ label: value, value }));

const kjToKcal = (v?: number) => (v || 0) / 4.184;

const FoodManage: React.FC = () => {
  const isAdmin = useModel('@@initialState')?.initialState?.currentUser?.userRole === 1;
  const [keyword, setKeyword] = useState('');
  const keywordRef = useRef(keyword);
  keywordRef.current = keyword;
  const [foodList, setFoodList] = useState<API.FoodItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<API.FoodItem | null>(null);
  const [form] = Form.useForm();
  const [recognizing, setRecognizing] = useState(false);
  const [recognizeResult, setRecognizeResult] = useState<string | null>(null);
  const [recognizeError, setRecognizeError] = useState('');
  const recognizeFileRef = useRef<HTMLInputElement>(null);

  const fetchFoods = useCallback(async () => {
    setLoading(true);
    try {
      const res: any = await listAdminFoods(keywordRef.current || undefined);
      setFoodList(Array.isArray(res) ? res : []);
    } catch (error: any) {
      message.error(error?.message || '加载食物失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchFoods(); }, [fetchFoods]);

  const openEditor = (record?: API.FoodItem) => {
    setEditing(record || null);
    setRecognizeResult(null);
    setRecognizeError('');
    form.setFieldsValue(record || { unit: 'g', baseAmount: 100, calories: 0, protein: 0, carbs: 0, fat: 0, fiber: 0 });
    setOpen(true);
  };

  const closeEditor = () => { setOpen(false); setRecognizeResult(null); setRecognizeError(''); };

  const handleSave = async () => {
    try {
      setSaving(true);
      const values = await form.validateFields();
      await saveFoodItem({ ...editing, ...values, id: editing?.id });
      message.success('保存成功');
      setOpen(false);
      fetchFoods();
    } catch (error: any) {
      if (!error?.errorFields) message.error(error?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (foodId?: number) => {
    if (!foodId) return;
    try {
      await deleteFoodItem(foodId);
      message.success('删除成功');
      if (editing?.id === foodId) { setOpen(false); setEditing(null); }
      fetchFoods();
    } catch (error: any) {
      message.error(error?.message || '删除失败');
    }
  };

  const handleRecognize = async (file: File) => {
    setRecognizing(true);
    setRecognizeResult(null);
    setRecognizeError('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      const r: any = await recognizeFoodImage(formData);
      if (!r) { setRecognizeError('识别失败，请重试'); return; }
      const nutrition = r.nutritionPerUnit || r.nutritionPer100g || {};
      const perUnit = r.perUnitAmount || 100;
      form.setFieldsValue({
        name: r.foodName || '未知食物',
        imageUrl: r.imageUrl || '',
        unit: r.unit || 'g',
        baseAmount: perUnit,
        calories: nutrition.calories || 0,
        protein: nutrition.protein || 0,
        carbs: nutrition.carbs || 0,
        fat: nutrition.fat || 0,
        fiber: nutrition.fiber || 0,
      });
      setRecognizeResult(`已识别: ${r.foodName || '未知食物'}，请核对后保存`);
      message.success('识别成功');
    } catch (error: any) {
      setRecognizeError(error?.message || '识别失败');
    } finally {
      setRecognizing(false);
    }
  };

  return (
    <div className="um-page">
      <div className="um-header">
        <div className="um-header-text"><h1>食物管理</h1></div>
      </div>

      <div className="um-main-card">
        <div className="um-toolbar">
          <div className="um-toolbar-left">
            <span className="um-toolbar-title">食物列表</span>
            <span className="um-toolbar-count">{foodList.length} 条</span>
          </div>
          <div className="um-toolbar-right">
            <Input
              placeholder="搜索食物"
              prefix={<ShopOutlined style={{ color: 'var(--text-3)' }} />}
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={fetchFoods}
              style={{ width: 220, borderRadius: 10 }}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchFoods}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>新建</Button>
          </div>
        </div>

        <Spin spinning={loading}>
          {foodList.length === 0 ? (
            <div className="um-empty"><Empty description="暂无食物数据" /></div>
          ) : (
            <div className="um-list">
              {foodList.map((record) => (
                <div key={record.id} className="um-row-item">
                  <div className="um-row-info">
                    {record.imageUrl ? (
                      <Image src={record.imageUrl} width={36} height={36} preview={false} style={{ borderRadius: 10, objectFit: 'cover' }} />
                    ) : (
                      <div className="um-avatar">食</div>
                    )}
                    <div className="um-row-text">
                      <div className="um-row-name">{record.name}</div>
                      <div className="um-row-sub">{record.category || '未分类'} · {kjToKcal(record.calories).toFixed(0)} kcal</div>
                    </div>
                  </div>
                  <div className="um-row-actions">
                    <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>编辑</Button>
                    <Popconfirm title="确认删除该食物？" onConfirm={() => handleDelete(record.id)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
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
        open={open}
        title={editing ? '编辑食物' : '新建食物'}
        onClose={closeEditor}
        footer={
          <>
            {editing?.id ? (
              <Popconfirm title="确认删除？" onConfirm={() => handleDelete(editing.id)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
                <Button danger>删除</Button>
              </Popconfirm>
            ) : (
              <Button onClick={closeEditor}>取消</Button>
            )}
            <Button type="primary" loading={saving} onClick={handleSave}>保存</Button>
          </>
        }
      >
        {isAdmin && (
          <div style={{ marginBottom: 16 }}>
            <Button icon={<CameraOutlined />} loading={recognizing} onClick={() => recognizeFileRef.current?.click()} size="small">
              {recognizing ? '识别中...' : '拍照/上传识别'}
            </Button>
            <input ref={recognizeFileRef} type="file" accept="image/*" capture="environment" style={{ display: 'none' }} onChange={(e) => { const f = e.target.files?.[0]; if (f) handleRecognize(f); e.target.value = ''; }} />
            {recognizeResult && <Alert type="success" showIcon style={{ marginTop: 8 }} message={recognizeResult} closable onClose={() => setRecognizeResult(null)} />}
            {recognizeError && <Alert type="error" showIcon style={{ marginTop: 8 }} message={recognizeError} closable onClose={() => setRecognizeError('')} />}
          </div>
        )}
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="name" label="食物名称" rules={[{ required: true, message: '请输入食物名称' }]}>
            <Input prefix={<ShopOutlined />} />
          </Form.Item>
          <Form.Item name="imageUrl" label="食物图片">
            <Input
              addonAfter={(
                <Upload showUploadList={false} customRequest={async ({ file, onSuccess, onError }) => {
                  try {
                    const fd = new FormData();
                    fd.append('file', file as File);
                    if (editing?.id) fd.append('foodId', String(editing.id));
                    const url = await uploadFoodImage(fd);
                    form.setFieldValue('imageUrl', url);
                    onSuccess?.(url);
                  } catch (e) { onError?.(e as Error); }
                }}>
                  <Button type="link" icon={<UploadOutlined />} size="small">上传</Button>
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
        </Form>
      </AppDesktopDialog>
    </div>
  );
};

export default FoodManage;
