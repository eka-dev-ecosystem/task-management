import { useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'

import { useTaskStore } from '@/stores'

import { useDebounce } from './useDebounce'

/**
 * Custom hook to synchronize the Zustand `useTaskStore` filter states
 * with the browser's URL query parameters bi-directionally.
 *
 * Persists:
 * - `view`: 'list' | 'board'
 * - `status`: 'todo' | 'in_progress' | 'review' | 'done' | 'backlog'
 * - `project_id`: numeric project ID
 * - `focus`: boolean
 * - `search`: search query keyword (debounced by 300ms)
 * - `priority`: 'low' | 'medium' | 'high' | 'urgent'
 * - `tag`: string tag name
 */
export const useTaskFiltersUrlSync = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const isSyncingFromUrl = useRef(false)
  const isInitialMount = useRef(true)

  const {
    activeView,
    setActiveView,
    filterStatus,
    setFilterStatus,
    filterPriority,
    setFilterPriority,
    filterTag,
    setFilterTag,
    filterProject,
    setFilterProject,
    filterFocusOnly,
    setFilterFocusOnly,
    searchQuery,
    setSearchQuery
  } = useTaskStore()

  const debouncedSearch = useDebounce(searchQuery, 300)

  // 1. Sync from URL search params -> useTaskStore on Mount & on popstate (browser back/forward)
  useEffect(() => {
    isSyncingFromUrl.current = true

    const viewParam = searchParams.get('view')
    if (viewParam === 'board' || viewParam === 'list') {
      if (activeView !== viewParam) setActiveView(viewParam)
    }

    const statusParam = searchParams.get('status') || ''
    if (filterStatus !== statusParam) setFilterStatus(statusParam)

    const priorityParam = searchParams.get('priority') || ''
    if (filterPriority !== priorityParam) setFilterPriority(priorityParam)

    const tagParam = searchParams.get('tag') || ''
    if (filterTag !== tagParam) setFilterTag(tagParam)

    const projectParam = searchParams.get('project_id')
    const parsedProject = projectParam ? parseInt(projectParam, 10) : null
    const validProject =
      parsedProject !== null && !isNaN(parsedProject) ? parsedProject : null
    if (filterProject !== validProject) {
      setFilterProject(validProject)
    }

    const focusParam = searchParams.get('focus') === 'true'
    if (filterFocusOnly !== focusParam) setFilterFocusOnly(focusParam)

    const searchParam = searchParams.get('search') || ''
    if (searchQuery !== searchParam) setSearchQuery(searchParam)

    // Allow state change listener to proceed after initial mount sync
    setTimeout(() => {
      isSyncingFromUrl.current = false
      isInitialMount.current = false
    }, 50)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  // 2. Sync from useTaskStore -> URL search params when store changes
  useEffect(() => {
    if (isInitialMount.current || isSyncingFromUrl.current) return

    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)

        // If currently viewing recurring templates tab, skip updating task list filters
        if (next.get('tab') === 'recurring') return next

        // View mode
        if (activeView === 'board') next.set('view', 'board')
        else next.delete('view')

        // Status filter
        if (filterStatus) next.set('status', filterStatus)
        else next.delete('status')

        // Priority filter
        if (filterPriority) next.set('priority', filterPriority)
        else next.delete('priority')

        // Tag filter
        if (filterTag) next.set('tag', filterTag)
        else next.delete('tag')

        // Project filter
        if (filterProject !== null) next.set('project_id', String(filterProject))
        else next.delete('project_id')

        // Focus filter
        if (filterFocusOnly) next.set('focus', 'true')
        else next.delete('focus')

        // Search query (debounced)
        if (debouncedSearch.trim()) next.set('search', debouncedSearch.trim())
        else next.delete('search')

        return next
      },
      { replace: true }
    )
  }, [
    activeView,
    filterStatus,
    filterPriority,
    filterTag,
    filterProject,
    filterFocusOnly,
    debouncedSearch,
    setSearchParams
  ])
}
