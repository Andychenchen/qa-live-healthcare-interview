/**
 * 后端 API 请求封装（基于原生 fetch）
 *
 * base url 来自环境变量 VITE_API_BASE_URL，默认 http://localhost:8080
 */

/** 后端服务地址 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/** API 请求超时时间（毫秒） */
const DEFAULT_TIMEOUT = 10000;

/** 后端统一响应结构（PRD 5.3 节） */
export interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T | null;
}

/** 后端统一错误对象 */
export class ApiError extends Error {
  /** HTTP 状态码，0 表示网络错误或超时 */
  readonly status: number;
  /** 业务错误码，如 PATIENT_LOGIN_FAILED */
  readonly code: string;

  constructor(message: string, status: number, code = 'UNKNOWN_ERROR') {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }

  /** 是否为未认证（401）错误，用于触发重新登录 */
  get isUnauthorized(): boolean {
    return this.status === 401;
  }
}

/** 请求选项 */
export interface RequestOptions {
  /** 外部取消信号 */
  signal?: AbortSignal;
  /** 超时时间（毫秒） */
  timeout?: number;
  /** 认证令牌，存在时以 Bearer 方式发送 */
  token?: string | null;
}

/**
 * 发起请求并解析统一响应结构。
 *
 * 后端返回 `{code, message, data}`；code !== 'OK' 时抛出带错误码的 ApiError。
 */
async function request<T>(
  method: 'GET' | 'POST',
  path: string,
  body?: unknown,
  options: RequestOptions = {}
): Promise<T> {
  const { signal, timeout = DEFAULT_TIMEOUT, token } = options;

  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeout);

  // 允许外部信号与超时信号同时生效
  const onExternalAbort = () => controller.abort();
  signal?.addEventListener('abort', onExternalAbort);

  const url = `${API_BASE_URL}${path}`;
  const headers: Record<string, string> = { Accept: 'application/json' };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=UTF-8';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });

    // 204 无响应体（登出）
    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    if (!text) {
      if (!response.ok) {
        throw new ApiError(`请求失败: ${response.status} ${response.statusText}`, response.status);
      }
      return undefined as T;
    }

    let parsed: unknown;
    try {
      parsed = JSON.parse(text);
    } catch (e) {
      throw new ApiError('响应数据不是合法的 JSON', response.status);
    }

    // 后端存在两种响应风格：
    // 1. 统一响应结构 {code, message, data}（患者认证接口）
    // 2. 裸数据（既有医生接口直接返回数组）
    // 此处按是否含 code 字段自动区分，保持既有接口向后兼容。
    if (parsed && typeof parsed === 'object' && 'code' in (parsed as Record<string, unknown>)) {
      const envelope = parsed as ApiEnvelope<T>;
      if (envelope.code !== 'OK') {
        throw new ApiError(envelope.message || '请求失败', response.status, envelope.code);
      }
      if (!response.ok) {
        throw new ApiError(
          envelope.message || `请求失败: ${response.status} ${response.statusText}`,
          response.status,
          envelope.code
        );
      }
      return envelope.data as T;
    }

    if (!response.ok) {
      throw new ApiError(`请求失败: ${response.status} ${response.statusText}`, response.status);
    }

    return parsed as T;
  } catch (e) {
    if (e instanceof ApiError) {
      throw e;
    }
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw new ApiError('请求超时，请检查后端服务是否已启动', 0, 'REQUEST_TIMEOUT');
    }
    throw new ApiError(`网络异常，无法连接后端服务（${API_BASE_URL}）`, 0, 'NETWORK_ERROR');
  } finally {
    window.clearTimeout(timer);
    signal?.removeEventListener('abort', onExternalAbort);
  }
}

/**
 * 发起 GET 请求并解析 JSON 响应
 *
 * @param path 接口路径，如 /api/doctors
 * @param options 额外请求参数
 * @returns 解析后的响应数据
 * @throws {ApiError} 请求失败、超时或响应体不是合法 JSON 时抛出
 */
export async function get<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return request<T>('GET', path, undefined, options);
}

/**
 * 发起 POST 请求并解析 JSON 响应
 *
 * @param path 接口路径，如 /api/patients/login
 * @param body 请求体对象
 * @param options 额外请求参数
 * @returns 解析后的响应数据
 * @throws {ApiError} 请求失败或业务错误时抛出（错误对象带 code 字段）
 */
export async function post<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
  return request<T>('POST', path, body, options);
}
