/**
 * 医生登录凭据（临时）
 *
 * 医生列表数据已迁移到 MySQL，通过 GET /api/doctors 获取。
 * 但该列表接口出于安全考虑不返回 password，而前端的医生登录
 * （DoctorLogin.vue -> store.loginDoctor）仍需要校验密码。
 *
 * 因此这里临时保留一份用户名 -> 密码 的映射，仅用于登录校验；
 * 待「问诊用户用户名密码登录」特性的登录接口（POST /api/auth/login）落地后，
 * 本文件即可删除，登录改为走后端接口。
 *
 * 数据来源与后端种子数据同源：web/qa-web/src/data/doctor-user-list.json
 */
export const DOCTOR_CREDENTIALS: Record<string, string> = {
  'dr-zhang-wei': '123456',
  'dr-li-na': '123456',
  'dr-wang-qiang': '123456',
  'dr-liu-min': '123456',
  'dr-chen-jie': '123456',
};
