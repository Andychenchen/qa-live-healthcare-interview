import { reactive, ref } from 'vue';
import patientData from '../data/patient-user.json';
import questionData from '../data/question-list.json';
import { DOCTOR_CREDENTIALS } from '../data/doctor-credentials';
import { fetchDoctors, type DoctorDto } from '../api/doctor';

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

  verifyPatient(name: string, birthday: string): Patient {
    let patient = state.patients.find(
      p => p.name === name && p.birthday === birthday
    );

    if (!patient) {
      patient = {
        id: `patient${Date.now()}`,
        name,
        birthday,
        phone: '',
        gender: '',
      };
      state.patients.push(patient);
    }

    state.currentPatient = patient;
    return patient;
  },

  logoutPatient() {
    state.currentPatient = null;
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
