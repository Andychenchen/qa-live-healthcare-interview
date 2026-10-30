/**
 * 患者认证相关接口（PRD 第 5 节）
 */
import { get, post } from './client';

/** 患者信息结构，与后端 PatientResponse 一致 */
export interface PatientDto {
  id: string;
  username: string;
  name: string;
  birthday: string | null;
  phone: string | null;
  gender: string | null;
  createdAt?: string;
}

/** 登录/注册成功后的返回结构 */
export interface AuthDto {
  token: string;
  patient: PatientDto;
}

/** 注册请求体 */
export interface RegisterPayload {
  username: string;
  password: string;
  name: string;
  birthday?: string;
  phone?: string;
  gender?: string;
}

/** 登录请求体 */
export interface LoginPayload {
  username: string;
  password: string;
}

/**
 * 患者注册，成功后直接返回登录态
 */
export function registerPatient(payload: RegisterPayload): Promise<AuthDto> {
  return post<AuthDto>('/api/patients/register', payload);
}

/**
 * 患者登录
 */
export function loginPatient(payload: LoginPayload): Promise<AuthDto> {
  return post<AuthDto>('/api/patients/login', payload);
}

/**
 * 患者登出，使当前会话立即失效
 */
export function logoutPatient(token: string): Promise<void> {
  return post<void>('/api/patients/logout', undefined, { token });
}

/**
 * 查询当前登录患者信息，用于刷新页面后恢复登录态
 */
export function fetchCurrentPatient(token: string): Promise<PatientDto> {
  return get<PatientDto>('/api/patients/me', { token });
}
