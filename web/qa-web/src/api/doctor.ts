/**
 * 医生相关接口
 */
import { get } from './client';

/** 医生数据结构，与后端 GET /api/doctors 返回一致 */
export interface DoctorDto {
  id: string;
  username: string;
  name: string;
  title: string;
  department: string;
  avatar: string;
  experience: string;
  specialties: string[];
  isActive: boolean;
}

/**
 * 获取全部医生
 */
export function fetchDoctors(): Promise<DoctorDto[]> {
  return get<DoctorDto[]>('/api/doctors');
}

/**
 * 获取在线（isActive=true）的医生
 */
export function fetchActiveDoctors(): Promise<DoctorDto[]> {
  return get<DoctorDto[]>('/api/doctors/active');
}
