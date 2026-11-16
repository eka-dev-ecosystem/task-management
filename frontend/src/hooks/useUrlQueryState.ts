import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { useDebounce } from './useDebounce'

export interface UrlQueryOptions<T> {
  debounceMs?: number
  serialize?: (value: T) => string | null | undefined
  deserialize?: (raw: string | null) => T
}

/**
 * Reusable hook to synchronize a state variable with a URL search/query parameter.
 * Provides a [state, setState] tuple identical to useState, with automatic URL persistence.
 *
 * @param key The query parameter name in the URL (e.g. 'status', 'search')
 * @param defaultValue The fallback value when parameter is missing or empty
 * @param options Optional debounce and custom serializer/deserializer
 */
export function useUrlQueryState<T>(
  key: string,
  defaultValue: T,
  options: UrlQueryOptions<T> = {}
): [T, (valOrFn: T | ((prev: T) => T)) => void] {
  const [searchParams, setSearchParams] = useSearchParams()
  const { debounceMs = 0, serialize, deserialize } = options

  // Helper to parse value from current search params
  const parseFromUrl = useCallback(
    (params: URLSearchParams): T => {
      const raw = params.get(key)
      if (raw === null) return defaultValue
      if (deserialize) return deserialize(raw)
      if (typeof defaultValue === 'number') {
        const num = Number(raw)
        return (isNaN(num) ? defaultValue : num) as T
      }
      if (typeof defaultValue === 'boolean') {
        return (raw === 'true') as T
      }
      return raw as unknown as T
    },
    [key, defaultValue, deserialize]
  )

  const [state, setState] = useState<T>(() => parseFromUrl(searchParams))
  const debouncedState = useDebounce(state, debounceMs)
  const isInternalUpdate = useRef(false)

  // 1. Sync from URL to State when URL changes externally (e.g. browser Back / Forward buttons)
  useEffect(() => {
    if (isInternalUpdate.current) {
      isInternalUpdate.current = false
      return
    }
    const currentFromUrl = parseFromUrl(searchParams)
    setState((prev) => (prev === currentFromUrl ? prev : currentFromUrl))
  }, [searchParams, parseFromUrl])

  // 2. Sync from State to URL (debounced if debounceMs > 0)
  useEffect(() => {
    const isDefault =
      debouncedState === defaultValue ||
      debouncedState === '' ||
      debouncedState === null ||
      debouncedState === undefined

    const serializedVal = serialize
      ? serialize(debouncedState)
      : isDefault
        ? null
        : String(debouncedState)

    const currentUrlVal = searchParams.get(key)

    if (serializedVal === null || serializedVal === undefined) {
      if (currentUrlVal !== null) {
        setSearchParams(
          (prev) => {
            const next = new URLSearchParams(prev)
            next.delete(key)
            return next
          },
          { replace: true }
        )
      }
    } else if (currentUrlVal !== serializedVal) {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          next.set(key, serializedVal)
          return next
        },
        { replace: true }
      )
    }
  }, [key, debouncedState, defaultValue, searchParams, setSearchParams, serialize])

  const setUrlState = useCallback((valOrFn: T | ((prev: T) => T)) => {
    isInternalUpdate.current = true
    setState(valOrFn)
  }, [])

  return [state, setUrlState]
}
