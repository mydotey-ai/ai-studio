/**
 * Simple in-memory cache for API responses
 */
class ApiCache {
  private cache: Map<string, { data: any; timestamp: number; ttl: number }>
  private defaultTTL: number

  constructor(defaultTTL: number = 5 * 60 * 1000) {
    // 5 minutes default
    this.cache = new Map()
    this.defaultTTL = defaultTTL
  }

  /**
   * Get cached data
   */
  get(key: string): any | null {
    const item = this.cache.get(key)
    if (!item) return null

    const now = Date.now()
    if (now - item.timestamp > item.ttl) {
      // Expired
      this.cache.delete(key)
      return null
    }

    return item.data
  }

  /**
   * Set cached data
   */
  set(key: string, data: any, ttl?: number): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttl || this.defaultTTL
    })
  }

  /**
   * Clear cache by key pattern
   */
  clear(pattern?: string): void {
    if (!pattern) {
      this.cache.clear()
      return
    }

    for (const key of this.cache.keys()) {
      if (key.match(pattern)) {
        this.cache.delete(key)
      }
    }
  }

  /**
   * Clear expired entries
   */
  clearExpired(): void {
    const now = Date.now()
    for (const [key, item] of this.cache.entries()) {
      if (now - item.timestamp > item.ttl) {
        this.cache.delete(key)
      }
    }
  }
}

export const apiCache = new ApiCache()

// Auto clear expired cache every 10 minutes
setInterval(
  () => {
    apiCache.clearExpired()
  },
  10 * 60 * 1000
)
