export interface PaginationParams {
  page?: number
  pageSize?: number
}

export interface PaginationResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  INTERNAL_ERROR = 500
}
