// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** 获取当前的用户 GET /api/user/current */
export async function currentUser(options?: { [key: string]: any }) {
  return request<API.CurrentUser>('/api/user/current', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 退出登录接口 POST /api/user/userlogout */
export async function outLogin(options?: { [key: string]: any }) {
  return request<number>('/api/user/logout', {
    method: 'POST',
    ...(options || {}),
  });
}

/** 登录接口 POST /api/user/Login */
export async function login(body: API.LoginParams, options?: { [key: string]: any }) {
  return request<API.CurrentUser>('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** 注册接口 POST /api/user/register */
export async function register(body: API.RegisterParams, options?: { [key: string]: any }) {
  return request<API.RegisterResult>('/api/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

export type ChatStreamMeta = {
  replyType?: 'training_plan' | 'diet_plan' | 'today_exercise_record' | 'today_diet_record' | 'today_training_plan' | 'today_diet_plan' | 'general_chat' | string;
  showSaveButton?: boolean;
  savePlanType?: 'training' | 'diet' | '';
  showRecordButton?: boolean;
  recordType?: 'training' | 'diet' | '';
};

/** 发送AI聊天消息（SSE流式） POST /api/chat/send/stream
 *  使用原生fetch而非umi的request，因为需要逐块读取流式响应。
 *  普通正文事件使用 data:内容\n\n，完成后可能追加 event:meta 结构化元数据。
 */
export async function sendChatMessageStream(
  body: { message: string; deepThinking?: boolean },
  onChunk: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: Error) => void,
  onMeta?: (meta: ChatStreamMeta) => void,
  signal?: AbortSignal,
  onStatus?: (status: string) => void,
): Promise<void> {
  const res = await fetch('/api/chat/send/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
    signal,
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    onError(new Error((errorData as any)?.description || '请求失败'));
    return;
  }

  const reader = res.body!.getReader();
  const decoder = new TextDecoder();
  let fullText = '';
  let buffer = '';

  const handleSseBlock = (block: string) => {
    const lines = block.split('\n').map((line) => line.trimEnd()).filter(Boolean);
    if (lines.length === 0) return;

    let eventName = 'message';
    const dataLines: string[] = [];
    for (const line of lines) {
      if (line.startsWith(':')) continue;
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
        continue;
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5));
      }
    }

    const rawData = dataLines.join('\n');
    if (!rawData || rawData === '[DONE]' || rawData === ' [DONE]') return;

    if (eventName === 'meta') {
      try {
        onMeta?.(JSON.parse(rawData));
      } catch {}
      return;
    }

    if (eventName === 'status') {
      try {
        const parsed = JSON.parse(rawData);
        if (parsed.status) onStatus?.(parsed.status);
      } catch {}
      return;
    }

    const content = rawData.replace(/\\n/g, '\n');
    if (content) {
      fullText += content;
      onChunk(content);
    }
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() || '';

      for (const block of blocks) {
        handleSseBlock(block);
      }
    }
    if (buffer.trim()) {
      handleSseBlock(buffer.trim());
    }
    onDone(fullText);
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return;
    onError(e instanceof Error ? e : new Error('流式请求异常'));
  } finally {
    reader.releaseLock();
  }
}

/** 图片识别 SSE 总结流（只推送最终 AI 总结文本） */
export async function recognizeSummaryStream(
  body: { equipmentName: string; rawData: string; type: string; deepThinking: boolean },
  onChunk: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: Error) => void,
): Promise<void> {
  const res = await fetch('/api/chat/recognize-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    onError(new Error('请求失败: ' + res.status));
    return;
  }

  const reader = res.body!.getReader();
  const decoder = new TextDecoder();
  let fullText = '';
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() || '';

      for (const block of blocks) {
        const lines = block.split('\n').map((l) => l.trimEnd()).filter(Boolean);
        let eventName = 'message';
        const dataLines: string[] = [];
        for (const line of lines) {
          if (line.startsWith(':')) continue;
          if (line.startsWith('event:')) { eventName = line.slice(6).trim(); continue; }
          if (line.startsWith('data:')) { dataLines.push(line.slice(5)); }
        }
        const rawData = dataLines.join('\n');
        if (!rawData || rawData === '[DONE]') continue;

        if (eventName === 'data' || eventName === 'message') {
          const content = rawData.replace(/\\n/g, '\n');
          if (content) {
            fullText += content;
            onChunk(content);
          }
        }
      }
    }
    if (buffer.trim()) {
      const lines = buffer.split('\n').map((l) => l.trimEnd()).filter(Boolean);
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const content = line.slice(5).replace(/\\n/g, '\n');
          if (content) { fullText += content; onChunk(content); }
        }
      }
    }
    onDone(fullText);
  } catch (e) {
    onError(e instanceof Error ? e : new Error('流式请求异常'));
  } finally {
    reader.releaseLock();
  }
}

/** 获取AI聊天记录 GET /api/chat/history */
export async function getChatHistory(options?: { [key: string]: any }) {
  return request<API.ChatHistory>('/api/chat/history', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 搜索用户接口 /api/user/search */
export async function searchUsers(params?: { username?: string }) {
  return request<API.CurrentUser[]>('/api/user/search', {
    method: 'GET',
    params: params || {},
  });
}

/** 删除用户 POST /api/user/delete */
export async function deleteUser(id: number) {
  return request<boolean>('/api/user/delete', {
    method: 'POST',
    data: id,
    headers: {
      'Content-Type': 'application/json',
    },
  });
}

/** 批量删除用户 POST /api/user/delete/batch */
export async function batchDeleteUsers(ids: number[]) {
  return request<boolean>('/api/user/delete/batch', {
    method: 'POST',
    data: ids,
    headers: {
      'Content-Type': 'application/json',
    },
  });
}

/** 管理员编辑用户 POST /api/user/admin/update */
export async function adminUpdateUser(data: {
  id: number;
  username?: string;
  gender?: number;
  height?: number;
  weight?: number;
  age?: number;
  activityLevel?: string;
  fitnessGoal?: string;
}) {
  return request<API.CurrentUser>('/api/user/admin/update', {
    method: 'POST',
    data,
  });
}

/** 上传头像 POST /api/user/upload/avatar */
export async function uploadAvatar(
  body: FormData,
  options?: { [key: string]: any },
) {
  return request<string>('/api/user/upload/avatar', {
    method: 'POST',
    data: body,
    requestType: 'form',
    ...(options || {}),
  });
}

/** 更新个人信息 POST /api/user/update */
export async function updateUserProfile(data?: { [key: string]: any }) {
  return request<API.CurrentUser>('/api/user/update', {
    method: 'POST',
    data,
  });
}

/** AI生成用户画像摘要 POST /api/user/generate-profile */
export async function generateUserProfile(data: { profileData: string }) {
  return request<string>('/api/user/generate-profile', {
    method: 'POST',
    data,
  });
}

/** 修改密码 POST /api/user/updatePassword */
export async function getSummaryNotification(options?: { [key: string]: any }) {
  return request<API.SummaryNotification | null>('/api/user/summary-notification', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function markSummaryNotificationRead(noticeId: string, options?: { [key: string]: any }) {
  return request<boolean>('/api/user/summary-notification/read', {
    method: 'POST',
    data: { noticeId },
    ...(options || {}),
  });
}

export async function markAllSummaryNotificationsRead(options?: { [key: string]: any }) {
  return request<boolean>('/api/user/summary-notification/read-all', {
    method: 'POST',
    ...(options || {}),
  });
}

export async function updateSummaryNotificationStatus(
  noticeId: string,
  data: { read?: boolean; cleared?: boolean },
  options?: { [key: string]: any },
) {
  return request<boolean>('/api/user/summary-notification/status', {
    method: 'POST',
    data: { noticeId, ...data },
    ...(options || {}),
  });
}

export async function updatePassword(options?: { [key: string]: any }) {
  return request<boolean>('/api/user/updatePassword', {
    method: 'POST',
    data: options,
    ...(options || {}),
  });
}

/** 获取所有动作 GET /api/exercise/list */
export async function getExerciseList(options?: { [key: string]: any }) {
  return request<API.Exercise[]>('/api/exercise/list', {
    method: 'GET',
    ...(options || {}),
  });
}

  /** 根据肌群查询动作 GET /api/exercise/listByGroup */
export async function getExercisesByGroup(muscleGroup: string, options?: { [key: string]: any }) {
  return request<API.Exercise[]>('/api/exercise/listByGroup', {
    method: 'GET',
    params: { muscleGroup },
    ...(options || {}),
  });
}

/** 切换收藏 POST /api/user/favorite/toggle */
export async function toggleFavorite(exerciseId: number) {
  return request<number[]>('/api/user/favorite/toggle', {
    method: 'POST',
    data: { exerciseId },
  });
}

/** 获取收藏列表 GET /api/user/favorite/list */
export async function getFavoriteList(options?: { [key: string]: any }) {
  return request<API.Exercise[]>('/api/user/favorite/list', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 保存计划按钮*/
export async function savePlan(data: { type: 'training' | 'diet'; content?: string }, options?: { [key: string]: any }) {
  return request<string>('/api/chat/save-plan', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function quickSaveRecord(data: { type: 'training' | 'diet' }) {
  return request<string>('/api/chat/quick-save-record', {
    method: 'POST',
    data,
  });
}

/** 获取可用模型列表 */
export async function getModelList(options?: { [key: string]: any }) {
  return request<{ models: { name: string; label: string }[]; current: string }>('/api/model/list', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 切换模型 */
export async function switchModel(name: string) {
  return request<string>('/api/model/switch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: { name },
  });
}

/** 获取模型角色配置 */
export async function getModelConfig(options?: { [key: string]: any }) {
  return request<{ purificationModel: string; chatModel: string; defaultModel: string; whisperModel: string; visionModel: string }>('/api/model/config', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 更新模型角色配置 */
export async function updateModelConfig(data: { purificationModel?: string; chatModel?: string; whisperModel?: string; visionModel?: string }) {
  return request<string>('/api/model/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

/** 全量保存模型角色配置（一次性提交所有角色） */
export async function saveAllModelConfig(data: { purificationModel?: string; chatModel?: string; whisperModel?: string; visionModel?: string }) {
  return request<string>('/api/model/config/saveAll', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

/** 添加自定义模型 */
export async function addCustomModel(data: { name: string; label: string; baseUrl: string; apiKey: string; model: string; type: string }) {
  return request<string>('/api/model/custom', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

/** 获取自定义模型详情（编辑回填） */
export async function getCustomModelDetail(name: string) {
  return request<{ name: string; label: string; baseUrl: string; apiKey: string; model: string; type: string }>('/api/model/custom/detail', {
    method: 'GET',
    params: { name },
  });
}

/** 删除自定义模型 */
export async function deleteCustomModel(name: string) {
  return request<string>('/api/model/custom', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    data: { name },
  });
}

/** 更新自定义模型 */
export async function updateCustomModel(data: { name: string; label?: string; baseUrl?: string; apiKey?: string; model?: string; type?: string }) {
  return request<string>('/api/model/custom', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

export async function getTodayRecord(options?: { [key: string]: any }) {
  return request<API.UserRecord>('/api/record/today', {
    method: 'GET',
    ...(options || {}),
  });
}

// ========== 饮食餐次模板 ==========

export async function listDietTemplates(options?: { [key: string]: any }) {
  return request<API.UserDietTemplate[]>('/api/diet-plan/templates', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function saveDietTemplate(data: API.SaveUserDietTemplateRequest, options?: { [key: string]: any }) {
  return request<number>('/api/diet-plan/template/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function deleteDietTemplate(templateId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/diet-plan/template/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: templateId,
    ...(options || {}),
  });
}

// ========== 饮食天模板 ==========

export async function listDietDayTemplates(options?: { [key: string]: any }) {
  return request<API.UserDietDayTemplate[]>('/api/diet-plan/day-templates', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function saveDietDayTemplate(data: API.SaveUserDietDayTemplateRequest, options?: { [key: string]: any }) {
  return request<number>('/api/diet-plan/day-template/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function deleteDietDayTemplate(id: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/diet-plan/day-template/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: id,
    ...(options || {}),
  });
}

// ========== 饮食循环 ==========

export async function listDietCycles(options?: { [key: string]: any }) {
  return request<API.UserDietCycle[]>('/api/diet-plan/cycles', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function getActiveDietCycle(options?: { [key: string]: any }) {
  return request<API.UserDietCycle | null>('/api/diet-plan/cycle/active', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function saveDietCycle(data: API.SaveUserDietCycleRequest, options?: { [key: string]: any }) {
  return request<number>('/api/diet-plan/cycle/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function activateDietCycle(cycleId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/diet-plan/cycle/activate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: cycleId,
    ...(options || {}),
  });
}

export async function deleteDietCycle(cycleId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/diet-plan/cycle/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: cycleId,
    ...(options || {}),
  });
}

export async function addExerciseRecord(data: API.AddExerciseRecordRequest, options?: { [key: string]: any }) {
  return request<boolean>('/api/record/exercise', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function addStructuredExerciseRecord(
  record: API.StructuredExerciseSession,
  options?: { [key: string]: any },
) {
  return request<boolean>('/api/record/exercise/structured', {
    method: 'POST',
    data: { record },
    ...(options || {}),
  });
}

export async function getTodayExerciseRecords(options?: { [key: string]: any }) {
  return request<API.StructuredExerciseSession[]>('/api/record/exercise/today', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function addDietRecord(data: API.AddDietRecordRequest, options?: { [key: string]: any }) {
  return request<boolean>('/api/record/diet', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function deleteExerciseRecord(index: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/record/exercise/delete', {
    method: 'POST',
    data: { index },
    ...(options || {}),
  });
}

export async function updateExerciseRecord(
  index: number,
  record: API.StructuredExerciseSession,
  options?: { [key: string]: any },
) {
  return request<boolean>('/api/record/exercise/update', {
    method: 'POST',
    data: { index, record },
    ...(options || {}),
  });
}

export async function getTodayDietRecords(options?: { [key: string]: any }) {
  return request<any[]>('/api/record/diet/today', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function getAgentTodayTrainingPlan(options?: { [key: string]: any }) {
  return request<API.AgentTodayTrainingPlan | null>('/api/agent/plan/training/today', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function getAgentTodayDietPlan(options?: { [key: string]: any }) {
  return request<API.AgentTodayDietPlan | null>('/api/agent/plan/diet/today', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function deleteDietRecord(index: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/record/diet/delete', {
    method: 'POST',
    data: { index },
    ...(options || {}),
  });
}

export async function updateDietRecord(
  index: number,
  record: API.AddDietRecordRequest,
  options?: { [key: string]: any },
) {
  return request<boolean>('/api/record/diet/update', {
    method: 'POST',
    data: { index, record },
    ...(options || {}),
  });
}

export async function searchFoods(keyword?: string, options?: { [key: string]: any }) {
  return request<API.FoodItem[]>('/api/food/search', {
    method: 'GET',
    params: keyword ? { keyword } : {},
    ...(options || {}),
  });
}

export async function listMyFoods(options?: { [key: string]: any }) {
  return request<API.FoodItem[]>('/api/food/my', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function listAdminFoods(keyword?: string, options?: { [key: string]: any }) {
  return request<API.FoodItem[]>('/api/food/admin/list', {
    method: 'GET',
    params: keyword ? { keyword } : {},
    ...(options || {}),
  });
}

export async function saveFoodItem(data: Partial<API.FoodItem>, options?: { [key: string]: any }) {
  return request<API.FoodItem>('/api/food/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function deleteFoodItem(foodId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/food/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: foodId,
    ...(options || {}),
  });
}

export async function uploadFoodImage(body: FormData, options?: { [key: string]: any }) {
  return request<string>('/api/food/upload-image', {
    method: 'POST',
    data: body,
    requestType: 'form',
    ...(options || {}),
  });
}

export async function getProgressTrend(options?: { [key: string]: any }) {
  return request<API.UserProgressTrend>('/api/user/progress-trend', {
    method: 'GET',
    ...(options || {}),
  });
}

// ========== 训练日模板 ==========

export async function listTrainingTemplates(options?: { [key: string]: any }) {
  return request<API.UserTrainingTemplate[]>('/api/training/templates', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function saveTrainingTemplate(data: API.SaveUserTrainingTemplateRequest, options?: { [key: string]: any }) {
  return request<number>('/api/training/template/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function deleteTrainingTemplate(templateId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/training/template/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: templateId,
    ...(options || {}),
  });
}

// ========== 训练循环 ==========

export async function listTrainingCycles(options?: { [key: string]: any }) {
  return request<API.UserTrainingCycle[]>('/api/training/cycles', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function getActiveTrainingCycle(options?: { [key: string]: any }) {
  return request<API.UserTrainingCycle | null>('/api/training/cycle/active', {
    method: 'GET',
    ...(options || {}),
  });
}

export async function saveTrainingCycle(data: API.SaveUserTrainingCycleRequest, options?: { [key: string]: any }) {
  return request<number>('/api/training/cycle/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

export async function activateTrainingCycle(cycleId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/training/cycle/activate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: cycleId,
    ...(options || {}),
  });
}

export async function deleteTrainingCycle(cycleId: number, options?: { [key: string]: any }) {
  return request<boolean>('/api/training/cycle/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: cycleId,
    ...(options || {}),
  });
}

// ========== 拍照识别食物 ==========

export async function recognizeFoodImage(
  body: FormData,
  options?: { [key: string]: any },
) {
  const timeout = (options && options.timeout) || 60000;
  return request('/api/chat/recognize-food', {
    method: 'POST',
    data: body,
    timeout,
    requestType: 'form',
    ...(options || {}),
  });
}

// ========== 获取图片识别配置 ==========

export async function getRecognizeConfig() {
  return request('/api/chat/recognize-config', { method: 'GET' });
}

export async function saveRecognizedFood(
  data: {
    imageUrl: string;
    foodName: string;
    unit: string;
    actualAmount: number;
    perUnitAmount: number;
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
    fiber: number;
    mealType: string;
    source: string;
  },
  options?: { [key: string]: any },
) {
  return request<boolean>('/api/chat/recognize-food/save', {
    method: 'POST',
    data,
    ...(options || {}),
  });
}

/** 语音转录 (Groq Whisper) POST /api/chat/transcribe-audio */
export async function transcribeAudio(
  body: FormData,
  options?: { [key: string]: any },
) {
  return request<string>('/api/chat/transcribe-audio', {
    method: 'POST',
    data: body,
    ...(options || {}),
  });
}
