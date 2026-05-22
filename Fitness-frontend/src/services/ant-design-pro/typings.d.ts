// @ts-ignore
/* eslint-disable */

declare namespace API {

  /**
   * 用于对接后端的通用返回类
   *
   */
  type BaseResponse<T> = {
    code: number;
    data: T;
    message: string;
    description?: string;
  };

  type Exercise = {
    id: number;
    name: string;
    muscleGroup: string;
    equipment: string;
    difficulty: string;
    steps?: string;
    tips?: string;
    recommendedSets?: number;
    recommendedReps?: string;
    restSeconds?: number;
    videoUrl?: string;
    sortOrder?: number;
  };


  type CurrentUser = {
    id: number;
    username: string;
    userAccount: string;
    avatarUrl?: string;
    gender?: number;
    height?: number;
    weight?: number;
    age?: number;
    activityLevel?: string;
    activityFactor?: number;
    dailyCalorieBurn?: number;
    customDailyCalories?: number;
    targetWeight?: number;
    fitnessGoal?: string;
    favoritesExercises?: string;
    userRole: number;
    createTime?: Date;
    experienceLevel?: string;
    preferredEquipment?: string;
    userProfile?: string;
  };

  type SummaryNotificationCard = {
    id: string;
    type: 'daily' | 'weekly';
    title: string;
    subtitle: string;
    preview: string;
    content: string;
    read?: boolean;
    date?: string;
  };

  type SummaryNotification = {
    id: string;
    type: 'daily' | 'weekly' | 'mixed';
    title: string;
    cards: SummaryNotificationCard[];
    pushDate: string;
    pushTime: string;
    unreadCount: number;
    hasUnread: boolean;
  };

  type UpdatePasswordParams = {
    newPassword: string;
    checkPassword: string;
    captcha: string;
  };

  type LoginResult = {
    status?: string;
    type?: string;
    currentAuthority?: string;
  };

  type RegisterResult = number;

  type CaptchaResult = {
    captchaId?: string;
    captchaImage: string;
    imageBase64?: string;
    captcha?: string;
  };

  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  type RuleListItem = {
    key?: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };

  type RuleList = {
    data?: RuleListItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type FakeCaptcha = {
    code?: number;
    status?: string;
  };

  type LoginParams = {
    userAccount?: string;
    userPassword?: string;
    username?: string;
    gender?: number;
    autoLogin?: boolean;
    type?: string;
  };

  type RegisterParams = {
    userAccount?: string;
    userPassword?: string;
    checkPassword?: string;
    captcha?: string;
  };

  type ErrorResponse = {
    /** 业务约定的错误码 */
    errorCode: string;
    /** 业务上的错误信息 */
    errorMessage?: string;
    /** 业务上的请求是否成功 */
    success?: boolean;
  };

  type NoticeIconList = {
    data?: NoticeIconItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type ChatHistory = {
    id?: number;
    userId?: number;
    summary?: string;
    emotionalState?: string;
    createTime?: string;
    updateTime?: string;
  };

  type UserTrainingTemplate = {
    id: number;
    name: string;
    items?: {
      id?: number;
      sectionType: string;
      sortOrder?: number;
      exerciseId: number;
      exerciseName?: string;
      muscleGroup?: string;
      equipment?: string;
      difficulty?: string;
      recommendedSets?: number;
      recommendedReps?: string;
      restSeconds?: number;
      videoUrl?: string;
      note?: string;
    }[];
  };

  type SaveUserTrainingTemplateRequest = {
    id?: number;
    name: string;
    items: {
      id?: number;
      sectionType: string;
      sortOrder?: number;
      exerciseId: number;
      note?: string;
    }[];
  };

  type UserTrainingCycle = {
    id: number;
    name: string;
    dayCount: number;
    startDate?: string;
    isActive: number;
    todayIndex?: number;
    days?: {
      dayIndex: number;
      templateId?: number;
      templateName?: string;
    }[];
  };

  type AgentTodayTrainingPlan = {
    date?: string;
    weekday?: string;
    cycleName?: string;
    dayCount?: number;
    todayIndex?: number;
    templateId?: number;
    templateName?: string;
    items?: UserTrainingTemplate['items'];
  };

  type AgentTodayDietPlan = {
    date?: string;
    weekday?: string;
    cycleName?: string;
    dayCount?: number;
    todayIndex?: number;
    currentMealType?: string;
    dayTemplateId?: number;
    dayTemplateName?: string;
    meals?: {
      mealType?: string;
      currentMeal?: boolean;
      templateId?: number;
      templateName?: string;
      items?: UserDietTemplate['items'];
    }[];
  };

  type SaveUserTrainingCycleRequest = {
    id?: number;
    name: string;
    dayCount: number;
    startDate?: string;
    activate?: boolean;
    days: {
      dayIndex: number;
      templateId?: number;
    }[];
  };

  type UserDietTemplate = {
    id: number;
    name: string;
    mealType?: string;
    items?: {
      id?: number;
      sortOrder?: number;
      foodItemId: number;
      foodName?: string;
      imageUrl?: string;
      baseAmount?: number;
      amount?: number;
      unit?: string;
      calories?: number;
      protein?: number;
      carbs?: number;
      fat?: number;
      fiber?: number;
      note?: string;
    }[];
  };

  type SaveUserDietTemplateRequest = {
    id?: number;
    name: string;
    mealType?: string;
    items: {
      id?: number;
      sortOrder?: number;
      foodItemId: number;
      amount: number;
      unit: string;
      note?: string;
    }[];
  };

  type UserDietDayTemplate = {
    id: number;
    name: string;
    mealSlots?: {
      mealType: string;
      templateId: number;
      templateName?: string;
    }[];
  };

  type SaveUserDietDayTemplateRequest = {
    id?: number;
    name: string;
    mealConfig: Record<string, number>;
  };

  type UserDietCycle = {
    id: number;
    name: string;
    dayCount: number;
    startDate?: string;
    isActive: number;
    todayIndex?: number;
    days?: {
      dayIndex: number;
      dayTemplateId?: number;
      dayTemplateName?: string;
    }[];
  };

  type SaveUserDietCycleRequest = {
    id?: number;
    name: string;
    dayCount: number;
    startDate?: string;
    activate?: boolean;
    days: {
      dayIndex: number;
      dayTemplateId?: number;
    }[];
  };

  type NoticeIconItemType = 'notification' | 'message' | 'event';

  type NoticeIconItem = {
    id?: string;
    extra?: string;
    key?: string;
    read?: boolean;
    avatar?: string;
    title?: string;
    status?: string;
    datetime?: string;
    description?: string;
    type?: NoticeIconItemType;
  };

  type AddExerciseRecordRequest = {
    exerciseId?: number;
    time?: string;
    exerciseName: string;
    muscleGroup?: string;
    completedSets?: number;
    totalSets?: number;
    durationSeconds?: number;
    caloriesBurned?: number;
    note?: string;
    source?: string;
  };

  type AddDietRecordRequest = {
    time?: string;
    mealType?: string;
    note?: string;
    source?: string;
    items: {
      foodItemId: number;
      amount: number;
    }[];
  };

  type FoodItem = {
    id: number;
    name: string;
    imageUrl?: string;
    category?: string;
    unit?: string;
    baseAmount?: number;
    calories?: number;
    protein?: number;
    carbs?: number;
    fat?: number;
    fiber?: number;
    createdBy?: number;
    isSystem?: number;
    createTime?: string;
    updateTime?: string;
  };

  type StructuredExerciseRecord = {
    exerciseId?: number;
    time?: string;
    name?: string;
    muscleGroup?: string;
    completedSets?: number;
    totalSets?: number;
    durationSeconds?: number;
    note?: string;
    source?: string;
  };

  type StructuredExerciseSession = {
    time?: string;
    name?: string;
    durationSeconds?: number;
    caloriesBurned?: number;
    note?: string;
    source?: string;
    items?: StructuredExerciseRecord[];
  };

  type StructuredDietRecord = {
    time?: string;
    name?: string;
    mealType?: string;
    calories?: number;
    note?: string;
    source?: string;
  };

  type UserRecord = {
    userId?: number;
    yesterdaySummary?: string;
    weeklyReviews?: string;
    weeklySummary?: string;
  };

  type ProgressTrendPoint = {
    date: string;
    label: string;
    weight?: number;
    intakeCalories?: number;
    targetCalories?: number;
    calorieBalance?: number;
    exerciseCalories?: number;
  };

  type UserProgressTrend = {
    points: ProgressTrendPoint[];
    latestWeight?: number;
    weeklyWeightChange?: number;
    weeklyCalorieBalance?: number;
    dailyCalorieBurn?: number;
    customDailyCalories?: number;
  };

}
