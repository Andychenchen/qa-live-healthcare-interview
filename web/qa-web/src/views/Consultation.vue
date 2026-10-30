<template>
  <div class="consultation">
    <div class="consultation-container">
      <div v-if="!currentPatient" class="auth-section">
        <div class="auth-card">
          <h1>{{ t('patient.auth.title') }}</h1>
          <p>{{ t('patient.auth.subtitle') }}</p>

          <div class="auth-tabs">
            <a-button
              type="primary"
              :class="{ 'auth-tab-active': authMode === 'login' }"
              @click="switchAuthMode('login')"
            >
              {{ t('patient.auth.loginTab') }}
            </a-button>
            <a-button
              :class="{ 'auth-tab-active': authMode === 'register' }"
              @click="switchAuthMode('register')"
            >
              {{ t('patient.auth.registerTab') }}
            </a-button>
          </div>

          <!-- 登录表单 -->
          <a-form
            v-if="authMode === 'login'"
            :model="loginForm"
            :rules="loginRules"
            @finish="onLogin"
            layout="vertical"
            class="login-form"
          >
            <a-form-item label="用户名" name="username">
              <a-input
                v-model:value="loginForm.username"
                size="large"
                placeholder="请输入用户名"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="密码" name="password">
              <a-input-password
                v-model:value="loginForm.password"
                size="large"
                placeholder="请输入密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="patientAuthLoading"
              >
                {{ t('patient.auth.loginButton') }}
              </a-button>
            </a-form-item>
          </a-form>

          <!-- 注册表单 -->
          <a-form
            v-else
            :model="registerForm"
            :rules="registerRules"
            @finish="onRegister"
            layout="vertical"
            class="login-form"
          >
            <a-form-item label="用户名" name="username">
              <a-input
                v-model:value="registerForm.username"
                size="large"
                placeholder="4-32 位，字母、数字、下划线或连字符"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="密码" name="password">
              <a-input-password
                v-model:value="registerForm.password"
                size="large"
                placeholder="8-32 位，需同时包含字母和数字"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item label="姓名" name="name">
              <a-input
                v-model:value="registerForm.name"
                size="large"
                placeholder="请输入您的姓名"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="手机号" name="phone">
              <a-input
                v-model:value="registerForm.phone"
                size="large"
                placeholder="选填"
              >
                <template #prefix>
                  <PhoneOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="性别" name="gender">
              <a-select
                v-model:value="registerForm.gender"
                size="large"
                placeholder="选填"
                allow-clear
              >
                <a-select-option value="男">男</a-select-option>
                <a-select-option value="女">女</a-select-option>
                <a-select-option value="其他">其他</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="patientAuthLoading"
              >
                {{ t('patient.auth.registerButton') }}
              </a-button>
            </a-form-item>
          </a-form>

          <a-alert
            v-if="patientAuthError"
            :message="patientAuthError"
            type="error"
            show-icon
            class="auth-error"
          />

          <a-alert
            :message="t('patient.auth.hintTitle')"
            :description="t('patient.auth.hintDescription')"
            type="info"
            show-icon
          />
        </div>
      </div>

      <div v-else class="patient-portal">
        <div class="portal-header">
          <div class="patient-info">
            <UserOutlined class="patient-icon-large" />
            <div>
              <h1>{{ currentPatient.name }} 的问诊</h1>
              <p>{{ t('patient.portal.welcome') }}</p>
            </div>
          </div>
          <div class="portal-actions">
            <a-button @click="logoutPatient">
              <LogoutOutlined />
              {{ t('patient.auth.logout') }}
            </a-button>
          </div>
        </div>

        <div class="selected-doctor" v-if="selectedDoctor">
          <a-alert
            :message="`当前诊室: ${selectedDoctor.name} - ${selectedDoctor.department}`"
            type="success"
            show-icon
            closable
            @close="clearSelectedDoctor"
          />
        </div>

        <div class="questions-section">
          <div class="section-header">
            <h2>我的问题</h2>
            <a-button type="primary" @click="showSubmitModal">
              <PlusOutlined />
              提交问题
            </a-button>
          </div>

          <a-empty v-if="myQuestions.length === 0" description="您还没有提交过问题" />

          <div v-else class="my-questions-list">
            <a-card
              v-for="question in myQuestions"
              :key="question.id"
              class="question-item"
            >
              <template #title>
                <div class="question-title">
                  <span>{{ question.doctorName }}</span>
                  <a-tag :color="question.status === 'answered' ? 'green' : 'orange'">
                    {{ question.status === 'answered' ? '已解答' : '待解答' }}
                  </a-tag>
                </div>
              </template>
              <div class="question-detail">
                <p class="question-text"><strong>问题:</strong> {{ question.question }}</p>
                <p class="submit-time">提交时间: {{ formatTime(question.submitTime) }}</p>
                <div v-if="question.status === 'answered'" class="answer-section">
                  <a-divider />
                  <p class="answer-text"><strong>医生回复:</strong> {{ question.answer }}</p>
                  <p class="answer-time">回复时间: {{ formatTime(question.answerTime!) }}</p>
                </div>
              </div>
            </a-card>
          </div>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="submitModalVisible"
      title="提交问题"
      @ok="submitQuestion"
      @cancel="closeSubmitModal"
      :confirmLoading="submitting"
      width="600px"
    >
      <a-form layout="vertical">
        <a-form-item label="选择医生" required>
          <a-select
            v-model:value="questionForm.doctorId"
            size="large"
            placeholder="请选择您要咨询的医生"
            :disabled="!!selectedDoctor"
          >
            <a-select-option
              v-for="doctor in availableDoctors"
              :key="doctor.id"
              :value="doctor.id"
            >
              <div class="doctor-option">
                <img :src="doctor.avatar" :alt="doctor.name" class="doctor-option-avatar" />
                <div>
                  <div>{{ doctor.name }}</div>
                  <div style="font-size: 12px; color: #999;">
                    {{ doctor.title }} · {{ doctor.department }}
                  </div>
                </div>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="您的问题" required>
          <a-textarea
            v-model:value="questionForm.question"
            :rows="6"
            placeholder="请详细描述您的症状或问题..."
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  UserOutlined,
  LockOutlined,
  PhoneOutlined,
  LogoutOutlined,
  PlusOutlined
} from '@ant-design/icons-vue';
import { store, patientAuthLoading, patientAuthError, type Doctor } from '../store';

const route = useRoute();
const { t } = useI18n();

const currentPatient = computed(() => store.state.currentPatient);
const myQuestions = computed(() =>
  currentPatient.value
    ? store.getQuestionsByPatient(currentPatient.value.id)
    : []
);

const selectedDoctor = ref<Doctor | null>(null);

/** 认证模式：登录 / 注册 */
const authMode = ref<'login' | 'register'>('login');

const loginForm = reactive({
  username: '',
  password: '',
});

const registerForm = reactive({
  username: '',
  password: '',
  name: '',
  phone: '',
  gender: undefined as string | undefined,
});

/** 与后端 PatientValidator 保持一致的校验规则（PRD V-01 ~ V-06） */
const usernameRules = [
  { required: true, message: t('patient.validation.usernameRequired') },
  {
    pattern: /^[A-Za-z0-9_-]{4,32}$/,
    message: t('patient.validation.usernamePattern'),
  },
];

const passwordRules = [
  { required: true, message: t('patient.validation.passwordRequired') },
  {
    pattern: /^(?=.*[A-Za-z])(?=.*\d)[\S]{8,32}$/,
    message: t('patient.validation.passwordPattern'),
  },
];

const loginRules = {
  username: usernameRules,
  password: passwordRules,
};

const registerRules = {
  username: usernameRules,
  password: passwordRules,
  name: [
    { required: true, message: t('patient.validation.nameRequired') },
    { max: 32, message: t('patient.validation.nameMax') },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: t('patient.validation.phonePattern') }],
};

const submitModalVisible = ref(false);
const submitting = ref(false);

const questionForm = reactive({
  doctorId: '',
  question: '',
});

const availableDoctors = computed(() => {
  return selectedDoctor.value
    ? [selectedDoctor.value]
    : store.getActiveDoctors();
});

onMounted(() => {
  // 医生数据来自后端，需先加载
  store.loadDoctors();

  // 恢复登录态（刷新页面后保持登录，PRD REQ-006）
  store.restoreSession();

  const doctorUsername = route.params.doctorUsername as string;
  if (doctorUsername) {
    const doctor = store.getDoctorByUsername(doctorUsername);
    if (doctor && doctor.isActive) {
      selectedDoctor.value = doctor;
      questionForm.doctorId = doctor.id;
    }
  }
});

const switchAuthMode = (mode: 'login' | 'register') => {
  authMode.value = mode;
};

const onLogin = async () => {
  const patient = await store.loginPatient(loginForm.username, loginForm.password);
  if (patient) {
    message.success(t('patient.auth.loginSuccess'));
    loginForm.password = '';
  }
};

const onRegister = async () => {
  const patient = await store.registerPatient({
    username: registerForm.username,
    password: registerForm.password,
    name: registerForm.name,
    phone: registerForm.phone || undefined,
    gender: registerForm.gender,
  });
  if (patient) {
    message.success(t('patient.auth.registerSuccess'));
    registerForm.password = '';
  }
};

const logoutPatient = async () => {
  await store.logoutPatient();
  selectedDoctor.value = null;
  message.success(t('patient.auth.logoutSuccess'));
};

const clearSelectedDoctor = () => {
  selectedDoctor.value = null;
  questionForm.doctorId = '';
};

const showSubmitModal = () => {
  if (selectedDoctor.value) {
    questionForm.doctorId = selectedDoctor.value.id;
  }
  submitModalVisible.value = true;
};

const closeSubmitModal = () => {
  submitModalVisible.value = false;
  if (!selectedDoctor.value) {
    questionForm.doctorId = '';
  }
  questionForm.question = '';
};

const submitQuestion = () => {
  if (!questionForm.doctorId) {
    message.error('请选择医生');
    return;
  }

  if (!questionForm.question.trim()) {
    message.error('请输入问题');
    return;
  }

  submitting.value = true;

  setTimeout(() => {
    const doctor = store.state.doctors.find(d => d.id === questionForm.doctorId);
    if (doctor && currentPatient.value) {
      store.addQuestion({
        patientId: currentPatient.value.id,
        patientName: currentPatient.value.name,
        doctorId: doctor.id,
        doctorName: doctor.name,
        question: questionForm.question,
      });

      message.success('问题提交成功');
      closeSubmitModal();
    }

    submitting.value = false;
  }, 500);
};

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};
</script>

<style scoped>
.consultation {
  min-height: calc(100vh - 64px);
  padding-top: 64px;
  background: #f0f2f5;
}

.consultation-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.auth-section {
  min-height: calc(100vh - 112px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px;
  width: 100%;
  max-width: 450px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.auth-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.auth-tabs .ant-btn {
  flex: 1;
  height: 40px;
  border-radius: 8px;
  font-weight: 500;
  color: #666;
  background: #f5f5f5;
  border-color: #f5f5f5;
}

.auth-tabs .ant-btn.auth-tab-active {
  color: #fff;
  background: #667eea;
  border-color: #667eea;
}

.auth-error {
  margin-bottom: 16px;
}

.auth-card h1 {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  text-align: center;
  margin-bottom: 8px;
}

.auth-card > p {
  font-size: 16px;
  color: #666;
  text-align: center;
  margin-bottom: 32px;
}

.patient-portal {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.portal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.patient-icon-large {
  font-size: 48px;
  color: #fff;
}

.patient-info h1 {
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 4px;
}

.patient-info p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.selected-doctor {
  padding: 16px 24px;
  background: #f6ffed;
  border-bottom: 1px solid #e8e8e8;
}

.questions-section {
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.my-questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.question-detail {
  line-height: 1.6;
}

.question-text,
.answer-text {
  margin-bottom: 12px;
  color: #333;
}

.submit-time,
.answer-time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.answer-section {
  margin-top: 16px;
}

.doctor-option {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doctor-option-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .auth-card {
    margin: 24px;
    padding: 32px 24px;
  }

  .portal-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .portal-actions {
    width: 100%;
  }
}
</style>
