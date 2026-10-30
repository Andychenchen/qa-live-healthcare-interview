import { reactive, ref } from 'vue';
import patientData from '../data/patient-user.json';
import questionData from '../data/question-list.json';
import { DOCTOR_CREDENTIALS } from '../data/doctor-credentials';
import { fetchDoctors, type DoctorDto } from '../api/doctor';
import {
  fetchCurrentPatient,
  loginPatient as apiLoginPatient,
  logoutPatient as apiLogoutPatient,
  registerPatient as apiRegisterPatient,
  type AuthDto,
  type PatientDto,
  type RegisterPayload,
} from '../api/patient';
import { ApiError } from '../api/client';

export interface Doctor {
  id: string;
  username: string;
  password: string;
  name: string;
  title: string;
  department: string;
  avatar: string;
  experience: string;
  specialties: string[];
  isActive: boolean;
}

export interface Patient {
  id: string;
  username: string;
  name: string;
  birthday: string;
  phone: string;
  gender: string;
}

export interface Question {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  question: string;
  submitTime: string;
  status: 'pending' | 'answered';
  answer: string | null;
  answerTime: string | null;
}

interface State {
  doctors: Doctor[];
  patients: Patient[];
  questions: Question[];
  currentDoctor: Doctor | null;
  currentPatient: Patient | null;
}

const state = reactive<State>({
  doctors: [],
  patients: patientData as Patient[],
  questions: questionData as Question[],
  currentDoctor: null,
  currentPatient: null,
});

/** 医生数据加载状态 */
export const doctorsLoading = ref(false);
/** 医生数据加载错误信息 */
export const doctorsError = ref<string | null>(null);

/** 患者认证相关状态 */
export const patientAuthLoading = ref(false);
/** 患者认证错误信息（登录/注册失败时的文案） */
export const patientAuthError = ref<string | null>(null);
/** 患者认证错误码，用于前端按码映射多语言文案 */
export const patientAuthErrorCode = ref<string | null>(null);

/** 患者令牌在 localStorage 中的存储键 */
const PATIENT_TOKEN_KEY = 'qa-live-patient-token';

/**
 * 从 localStorage 读取令牌
 */
function readToken(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch (e) {
    return null;
  }
}

/**
 * 持久化令牌
 */
function writeToken(key: string, token: string | null) {
  try {
    if (token) {
      window.localStorage.setItem(key, token);
    } else {
      window.localStorage.removeItem(key);
    }
  } catch (e) {
    // 忽略存储异常（如隐私模式），不影响登录功能本身
  }
}

/**
 * 将后端返回的患者 DTO 转换为 store 内部的 Patient 结构
 */
function toPatient(dto: PatientDto): Patient {
  return {
    id: dto.id,
    username: dto.username,
    name: dto.name,
    birthday: dto.birthday ?? '',
    phone: dto.phone ?? '',
    gender: dto.gender ?? '',
  };
}

/**
 * 应用登录态：保存令牌与患者信息
 */
function applyAuth(auth: AuthDto) {
  writeToken(PATIENT_TOKEN_KEY, auth.token);
  state.currentPatient = toPatient(auth.patient);

  // 同步到 patients 列表，便于按患者查询历史问题
  const index = state.patients.findIndex(p => p.id === state.currentPatient!.id);
  if (index >= 0) {
    state.patients.splice(index, 1, state.currentPatient);
  } else {
    state.patients.push(state.currentPatient);
  }
}

/**
 * 记录认证错误信息（文案 + 错误码）
 */
function setAuthError(e: unknown) {
  if (e instanceof ApiError) {
    patientAuthError.value = e.message;
    patientAuthErrorCode.value = e.code;
  } else {
    patientAuthError.value = e instanceof Error ? e.message : '操作失败，请稍后重试';
    patientAuthErrorCode.value = 'UNKNOWN_ERROR';
  }
}

/**
 * 将后端返回的 DTO 转换为 store 内部的 Doctor 结构。
 *
 * 后端列表接口出于安全考虑不返回 password，这里从临时凭据表补齐，
 * 保证 Doctor 类型的字段完整性（待登录接口落地后可移除）。
 */
function toDoctor(dto: DoctorDto): Doctor {
  return {
    id: dto.id,
    username: dto.username,
    password: DOCTOR_CREDENTIALS[dto.username] ?? '',
    name: dto.name,
    title: dto.title,
    department: dto.department,
    avatar: dto.avatar,
    experience: dto.experience,
    specialties: dto.specialties ?? [],
    isActive: dto.isActive,
  };
}

export const store = {
  state,

  /**
   * 从后端加载医生列表。
   *
   * @param force 为 true 时忽略已有的成功加载结果，强制重新请求
   * @returns 加载成功返回 true，失败返回 false（错误信息见 doctorsError）
   */
  async loadDoctors(force = false): Promise<boolean> {
    if (!force && state.doctors.length > 0 && !doctorsError.value) {
      return true;
    }

    doctorsLoading.value = true;
    doctorsError.value = null;

    try {
      const dtos = await fetchDoctors();
      state.doctors = dtos.map(toDoctor);
      return true;
    } catch (e) {
      doctorsError.value = e instanceof Error ? e.message : '加载医生数据失败';
      state.doctors = [];
      return false;
    } finally {
      doctorsLoading.value = false;
    }
  },

  /**
   * 医生登录校验。
   *
   * 若医生列表已从后端加载，则以列表数据为准做用户名+密码校验；
   * 若尚未加载（用户直接访问登录页），退回到凭据表校验用户名是否存在。
   */
  loginDoctor(username: string, password: string): Doctor | null {
    if (state.doctors.length === 0) {
      const expected = DOCTOR_CREDENTIALS[username];
      if (!expected || expected !== password) {
        return null;
      }
      // 列表未加载时无法构造完整医生信息，交由调用方触发加载后重试
      return null;
    }

    const doctor = state.doctors.find(
      d => d.username === username && d.password === password
    );
    if (doctor) {
      state.currentDoctor = doctor;
      return doctor;
    }
    return null;
  },

  logoutDoctor() {
    state.currentDoctor = null;
  },

  /**
   * 患者注册（PRD REQ-001）。
   *
   * 注册成功后自动写入登录态。
   *
   * @returns 成功返回患者信息，失败返回 null（错误信息见 patientAuthError）
   */
  async registerPatient(payload: RegisterPayload): Promise<Patient | null> {
    patientAuthLoading.value = true;
    patientAuthError.value = null;
    patientAuthErrorCode.value = null;

    try {
      const auth: AuthDto = await apiRegisterPatient(payload);
      applyAuth(auth);
      return state.currentPatient;
    } catch (e) {
      setAuthError(e);
      return null;
    } finally {
      patientAuthLoading.value = false;
    }
  },

  /**
   * 患者登录（PRD REQ-004）。
   *
   * @returns 成功返回患者信息，失败返回 null（错误信息见 patientAuthError）
   */
  async loginPatient(username: string, password: string): Promise<Patient | null> {
    patientAuthLoading.value = true;
    patientAuthError.value = null;
    patientAuthErrorCode.value = null;

    try {
      const auth: AuthDto = await apiLoginPatient({ username, password });
      applyAuth(auth);
      return state.currentPatient;
    } catch (e) {
      setAuthError(e);
      return null;
    } finally {
      patientAuthLoading.value = false;
    }
  },

  /**
   * 患者登出（PRD REQ-007）。
   *
   * 先通知服务端使会话失效，再清除本地状态与令牌。
   */
  async logoutPatient(): Promise<void> {
    const token = readToken(PATIENT_TOKEN_KEY);
    try {
      if (token) {
        await apiLogoutPatient(token);
      }
    } catch (e) {
      // 服务端不可用时也要保证本地登出
    } finally {
      state.currentPatient = null;
      writeToken(PATIENT_TOKEN_KEY, null);
    }
  },

  /**
   * 恢复登录态（PRD REQ-006）。
   *
   * 应用启动时调用，校验本地令牌是否仍然有效。
   *
   * @returns 恢复成功返回患者信息，否则返回 null
   */
  async restoreSession(): Promise<Patient | null> {
    const token = readToken(PATIENT_TOKEN_KEY);
    if (!token) {
      return null;
    }

    try {
      const dto = await fetchCurrentPatient(token);
      state.currentPatient = toPatient(dto);
      return state.currentPatient;
    } catch (e) {
      // 令牌过期或非法，清除本地状态
      state.currentPatient = null;
      writeToken(PATIENT_TOKEN_KEY, null);
      return null;
    }
  },

  getQuestionsByDoctor(doctorId: string): Question[] {
    return state.questions.filter(q => q.doctorId === doctorId);
  },

  getQuestionsByPatient(patientId: string): Question[] {
    return state.questions.filter(q => q.patientId === patientId);
  },

  addQuestion(question: Omit<Question, 'id' | 'submitTime' | 'status' | 'answer' | 'answerTime'>): Question {
    const newQuestion: Question = {
      ...question,
      id: `q${Date.now()}`,
      submitTime: new Date().toISOString(),
      status: 'pending',
      answer: null,
      answerTime: null,
    };
    state.questions.push(newQuestion);
    return newQuestion;
  },

  answerQuestion(questionId: string, answer: string) {
    const question = state.questions.find(q => q.id === questionId);
    if (question) {
      question.status = 'answered';
      question.answer = answer;
      question.answerTime = new Date().toISOString();
    }
  },

  markQuestionAsAnswered(questionId: string) {
    const question = state.questions.find(q => q.id === questionId);
    if (question) {
      question.status = 'answered';
      question.answer = '已口述解答';
      question.answerTime = new Date().toISOString();
    }
  },

  getDoctorByUsername(username: string): Doctor | undefined {
    return state.doctors.find(d => d.username === username);
  },

  getActiveDoctors(): Doctor[] {
    return state.doctors.filter(d => d.isActive);
  },

  getStatistics() {
    const totalDoctors = state.doctors.length;
    const totalQuestions = state.questions.length;
    const activeSessions = state.questions.filter(q => q.status === 'pending').length;
    const totalSessions = state.doctors.filter(d => d.isActive).length;

    return {
      totalDoctors,
      totalQuestions,
      activeSessions,
      totalSessions,
    };
  },
};
