/**
 * 后端 API 请求封装（基于原生 fetch）
 *
 * base url 来自环境变量 VITE_API_BASE_URL，默认 http://localhost:8080
 */

/** 后端服务地址 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/** API 请求超时时间（毫秒） */
const DEFAULT_TIMEOUT = 10000;

/** 后端统一错误对象 */
export class ApiError extends Error {
  /** HTTP 状态码，0 表示网络错误或超时 */
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
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
export async function get<T>(path: string, options: { signal?: AbortSignal; timeout?: number } = {}): Promise<T> {
  const { signal, timeout = DEFAULT_TIMEOUT } = options;

  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeout);

  // 允许外部信号与超时信号同时生效
  const onExternalAbort = () => controller.abort();
  signal?.addEventListener('abort', onExternalAbort);

  const url = `${API_BASE_URL}${path}`;

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new ApiError(`请求失败: ${response.status} ${response.statusText}`, response.status);
    }

    const text = await response.text();
    if (!text) {
      return undefined as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch (e) {
      throw new ApiError('响应数据不是合法的 JSON', response.status);
    }
  } catch (e) {
    if (e instanceof ApiError) {
      throw e;
    }
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw new ApiError('请求超时，请检查后端服务是否已启动', 0);
    }
    throw new ApiError(`网络异常，无法连接后端服务（${API_BASE_URL}）`, 0);
  } finally {
    window.clearTimeout(timer);
    signal?.removeEventListener('abort', onExternalAbort);
  }
}
