const axios = require('axios');
const fs = require('fs');
const path = require('path');

class ApiClient {
  constructor(config) {
    this.config = config;
    this.token = null;
    this.refreshToken = null;
    this.baseURL = config.baseUrl;
    this.timeout = config.timeout || 30000;
    this.retryAttempts = config.retryAttempts || 3;
    this.retryDelay = config.retryDelay || 1000;

    // 加载端点配置
    const endpointsPath = path.join(__dirname, '../config/endpoints.json');
    this.endpoints = JSON.parse(fs.readFileSync(endpointsPath, 'utf8')).endpoints;

    // 创建axios实例
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: this.timeout,
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // 请求拦截器
    this.client.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // 响应拦截器
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // Token过期，尝试刷新
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          if (this.refreshToken) {
            try {
              await this.refreshAccessToken();
              return this.client(originalRequest);
            } catch (refreshError) {
              return Promise.reject(refreshError);
            }
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * 登录获取Token
   */
  async login(username, password) {
    try {
      const response = await this.client.post(this.endpoints.iam.login, {
        username,
        password
      });

      if (response.data.code === 200 && response.data.data) {
        this.token = response.data.data.token;
        this.refreshToken = response.data.data.refreshToken;
        return response.data.data;
      }

      throw new Error(`Login failed: ${response.data.message}`);
    } catch (error) {
      throw new Error(`Login error: ${error.message}`);
    }
  }

  /**
   * 刷新Token
   */
  async refreshAccessToken() {
    try {
      const response = await this.client.post(this.endpoints.iam.refreshToken, {
        refreshToken: this.refreshToken
      });

      if (response.data.code === 200 && response.data.data) {
        this.token = response.data.data.token;
        this.refreshToken = response.data.data.refreshToken;
        return response.data.data;
      }

      throw new Error('Token refresh failed');
    } catch (error) {
      this.token = null;
      this.refreshToken = null;
      throw error;
    }
  }

  /**
   * 登出
   */
  async logout() {
    try {
      await this.client.post(this.endpoints.iam.logout);
      this.token = null;
      this.refreshToken = null;
      return true;
    } catch (error) {
      console.warn('Logout error:', error.message);
      return false;
    }
  }

  /**
   * 通用GET请求
   */
  async get(endpoint, params = null) {
    return this._makeRequest('GET', endpoint, null, params);
  }

  /**
   * 通用POST请求
   */
  async post(endpoint, data = null) {
    return this._makeRequest('POST', endpoint, data);
  }

  /**
   * 通用PUT请求
   */
  async put(endpoint, data = null) {
    return this._makeRequest('PUT', endpoint, data);
  }

  /**
   * 通用DELETE请求
   */
  async delete(endpoint) {
    return this._makeRequest('DELETE', endpoint);
  }

  /**
   * 带重试的请求方法
   */
  async _makeRequest(method, endpoint, data = null, params = null, attempt = 1) {
    try {
      const config = {
        method,
        url: endpoint,
        data,
        params
      };

      const response = await this.client.request(config);

      // 检查业务状态码
      if (response.data && response.data.code !== 200) {
        throw new ApiError(
          response.data.message || 'Request failed',
          response.data.code,
          response.status
        );
      }

      return response.data;
    } catch (error) {
      // 网络错误或服务器错误，尝试重试
      if (attempt < this.retryAttempts && this._shouldRetry(error)) {
        console.log(`Retry attempt ${attempt + 1}/${this.retryAttempts} for ${method} ${endpoint}`);
        await this._sleep(this.retryDelay);
        return this._makeRequest(method, endpoint, data, params, attempt + 1);
      }

      throw error;
    }
  }

  /**
   * 判断是否应该重试
   */
  _shouldRetry(error) {
    // 网络错误
    if (!error.response) return true;

    // 5xx服务器错误
    if (error.response.status >= 500) return true;

    // 429 Too Many Requests
    if (error.response.status === 429) return true;

    return false;
  }

  /**
   * 延迟函数
   */
  _sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * 指标相关API
   */
  indicators = {
    create: (data) => this.post(this.endpoints.indicators.create, data),

    getById: (id) => this.get(this.endpoints.indicators.getById.replace('{id}', id)),

    list: (params) => this.get(this.endpoints.indicators.list, params),

    update: (id, data) => this.put(this.endpoints.indicators.update.replace('{id}', id), data),

    delete: (id) => this.delete(this.endpoints.indicators.delete.replace('{id}', id)),

    distribute: (id) => this.post(this.endpoints.indicators.distribute.replace('{id}', id)),

    getChildren: (id) => this.get(this.endpoints.indicators.getChildren.replace('{id}', id)),

    getParent: (id) => this.get(this.endpoints.indicators.getParent.replace('{id}', id))
  };

  /**
   * 填报相关API
   */
  reports = {
    create: (data) => this.post(this.endpoints.reports.create, data),

    getById: (id) => this.get(this.endpoints.reports.getById.replace('{id}', id)),

    list: (params) => this.get(this.endpoints.reports.list, params),

    update: (id, data) => this.put(this.endpoints.reports.update.replace('{id}', id), data),

    delete: (id) => this.delete(this.endpoints.reports.delete.replace('{id}', id)),

    submit: (id) => this.post(this.endpoints.reports.submit.replace('{id}', id)),

    withdraw: (id) => this.post(this.endpoints.reports.withdraw.replace('{id}', id)),

    getByIndicator: (indicatorId) =>
      this.get(this.endpoints.reports.getByIndicator.replace('{indicatorId}', indicatorId)),

    getByPeriod: (period) =>
      this.get(this.endpoints.reports.getByPeriod.replace('{period}', period))
  };

  /**
   * 工作流相关API
   */
  workflow = {
    getDefinitions: () => this.get(this.endpoints.workflow.getDefinitions),

    getDefinitionById: (id) =>
      this.get(this.endpoints.workflow.getDefinitionById.replace('{id}', id)),

    getInstances: (params) => this.get(this.endpoints.workflow.getInstances, params),

    getInstanceById: (id) =>
      this.get(this.endpoints.workflow.getInstanceById.replace('{id}', id)),

    getTimeline: (id) =>
      this.get(this.endpoints.workflow.getTimeline.replace('{id}', id)),

    getHistory: (id) =>
      this.get(this.endpoints.workflow.getHistory.replace('{id}', id)),

    approve: (id, comment) =>
      this.post(this.endpoints.workflow.approve.replace('{id}', id), { comment }),

    reject: (id, comment) =>
      this.post(this.endpoints.workflow.reject.replace('{id}', id), { comment }),

    withdraw: (id) =>
      this.post(this.endpoints.workflow.withdraw.replace('{id}', id))
  };

  /**
   * 周期相关API
   */
  cycles = {
    create: (data) => this.post(this.endpoints.cycles.create, data),

    list: () => this.get(this.endpoints.cycles.list),

    getById: (id) => this.get(this.endpoints.cycles.getById.replace('{id}', id)),

    activate: (id) => this.post(this.endpoints.cycles.activate.replace('{id}', id)),

    close: (id) => this.post(this.endpoints.cycles.close.replace('{id}', id))
  };

  /**
   * 分析相关API
   */
  analytics = {
    getDashboard: (params) => this.get(this.endpoints.analytics.getDashboard, params),

    getIndicatorProgress: (id) =>
      this.get(this.endpoints.analytics.getIndicatorProgress.replace('{id}', id)),

    getOrgProgress: (id) =>
      this.get(this.endpoints.analytics.getOrgProgress.replace('{id}', id)),

    exportData: (params) => this.get(this.endpoints.analytics.exportData, params)
  };

  /**
   * 告警相关API
   */
  alerts = {
    list: (params) => this.get(this.endpoints.alerts.list, params),

    getById: (id) => this.get(this.endpoints.alerts.getById.replace('{id}', id)),

    markAsRead: (id) => this.post(this.endpoints.alerts.markAsRead.replace('{id}', id)),

    getMyAlerts: () => this.get(this.endpoints.alerts.getMyAlerts)
  };
}

/**
 * 自定义API错误类
 */
class ApiError extends Error {
  constructor(message, code, httpStatus) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.httpStatus = httpStatus;
  }
}

module.exports = { ApiClient, ApiError };
