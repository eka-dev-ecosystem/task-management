import { confirmDialog } from 'primereact/confirmdialog'
import { InputSwitch } from 'primereact/inputswitch'
import React, { useMemo, useState } from 'react'

import { getLocalizedErrorMessage } from '@/api'
import {
  useCreateRecurringTask,
  useDeleteRecurringTask,
  useProjects,
  useRecurringTasks,
  useToggleActiveRecurringTask,
  useUpdateRecurringTask,
  useUrlQueryState
} from '@/hooks'
import { useTranslation } from '@/i18n'
import type {
  CreateRecurringTaskPayload,
  RecurrencePattern,
  RecurringTask,
  TaskPriority,
  UpdateRecurringTaskPayload
} from '@/types'

const PRIORITY_META: Record<TaskPriority, { icon: string; class: string; badge: string }> = {
  urgent: {
    icon: 'pi-exclamation-triangle',
    class: 'text-rose-500 dark:text-rose-400',
    badge: 'bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border-rose-200 dark:border-rose-800/40'
  },
  high: {
    icon: 'pi-angle-double-up',
    class: 'text-amber-600 dark:text-amber-400',
    badge: 'bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-800/40'
  },
  medium: {
    icon: 'pi-minus',
    class: 'text-blue-600 dark:text-blue-400',
    badge: 'bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-800/40'
  },
  low: {
    icon: 'pi-angle-down',
    class: 'text-slate-500',
    badge: 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700'
  }
}

/**
 * Safely parse and format due_time for display (HH:mm)
 * Prevents corrupted '0000-' or ISO timestamp strings from displaying on cards
 */
const formatDueTimeDisplay = (dueTime?: string | null): string => {
  if (!dueTime) return ''
  const trimmed = dueTime.trim()
  if (trimmed.startsWith('0000-')) {
    if (trimmed.includes('T')) {
      const timePart = trimmed.split('T')[1]?.replace('Z', '').trim()
      if (timePart && timePart.length >= 5) {
        return timePart.substring(0, 5)
      }
    }
    if (trimmed.includes(' ')) {
      const timePart = trimmed.split(' ')[1]?.trim()
      if (timePart && timePart.length >= 5) {
        return timePart.substring(0, 5)
      }
    }
    return ''
  }
  if (trimmed.includes('T')) {
    const timePart = trimmed.split('T')[1]?.replace('Z', '').trim()
    if (timePart && timePart.length >= 5) {
      return timePart.substring(0, 5)
    }
  }
  if (trimmed.includes(' ')) {
    const timePart = trimmed.split(' ')[1]?.trim()
    if (timePart && timePart.length >= 5) {
      return timePart.substring(0, 5)
    }
  }
  if (trimmed.length >= 5 && trimmed.includes(':')) {
    return trimmed.substring(0, 5)
  }
  return ''
}

/**
 * Sanitize due_time before sending to backend in PUT/POST requests
 * Returns clean 'HH:mm:ss' or null
 */
const cleanPayloadDueTime = (dt: string): string | null => {
  const trimmed = dt.trim()
  if (!trimmed || trimmed === 'null' || trimmed.startsWith('0000-')) {
    return null
  }
  if (trimmed.length === 5 && trimmed.includes(':')) {
    return `${trimmed}:00`
  }
  if (trimmed.length >= 8 && trimmed.includes(':')) {
    return trimmed.substring(0, 8)
  }
  return null
}

export const RecurringTasksManagementView: React.FC = () => {
  const { t } = useTranslation()
  const { data: rawTemplates, isLoading, isError, refetch } = useRecurringTasks()
  const templates = Array.isArray(rawTemplates) ? rawTemplates : []
  const { data: projects = [] } = useProjects()

  const toggleActiveMutation = useToggleActiveRecurringTask()
  const deleteMutation = useDeleteRecurringTask()
  const createMutation = useCreateRecurringTask()
  const updateMutation = useUpdateRecurringTask()

  const [searchQuery, setSearchQuery] = useUrlQueryState('search', '', { debounceMs: 300 })
  const [filterActiveOnly, setFilterActiveOnly] = useUrlQueryState<'all' | 'active' | 'paused'>(
    'recurring_status',
    'all'
  )

  // Modal State (Create / Edit)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<RecurringTask | null>(null)

  // Form State
  const [formTitle, setFormTitle] = useState('')
  const [formDesc, setFormDesc] = useState('')
  const [formPriority, setFormPriority] = useState<TaskPriority>('medium')
  const [formProjectId, setFormProjectId] = useState<number | null>(null)
  const [formPattern, setFormPattern] = useState<RecurrencePattern>('daily')
  const [formInterval, setFormInterval] = useState<number>(1)
  const [formDueTime, setFormDueTime] = useState<string>('')
  const [formIsActive, setFormIsActive] = useState<boolean>(true)
  const [formSubtasks, setFormSubtasks] = useState<string[]>([])
  const [newSubtaskInput, setNewSubtaskInput] = useState('')
  const [formTouched, setFormTouched] = useState(false)
  const [formErrorMsg, setFormErrorMsg] = useState<string | null>(null)

  const openCreateModal = () => {
    setEditingTemplate(null)
    setFormTitle('')
    setFormDesc('')
    setFormPriority('medium')
    setFormProjectId(null)
    setFormPattern('daily')
    setFormInterval(1)
    setFormDueTime('')
    setFormIsActive(true)
    setFormSubtasks([])
    setNewSubtaskInput('')
    setFormTouched(false)
    setFormErrorMsg(null)
    setIsModalOpen(true)
  }

  const openEditModal = (template: RecurringTask) => {
    setEditingTemplate(template)
    setFormTitle(template.title || '')
    setFormDesc(template.description || '')
    setFormPriority(template.priority || 'medium')
    setFormProjectId(template.project_id || null)
    setFormPattern(template.recurrence_pattern || 'daily')
    setFormInterval(template.recurrence_interval || 1)
    setFormDueTime(formatDueTimeDisplay(template.due_time))
    setFormIsActive(template.is_active)
    setFormSubtasks(template.subtasks?.map((s) => s.title) || [])
    setNewSubtaskInput('')
    setFormTouched(false)
    setFormErrorMsg(null)
    setIsModalOpen(true)
  }

  const handleAddSubtask = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!newSubtaskInput.trim()) return
    setFormSubtasks([...formSubtasks, newSubtaskInput.trim()])
    setNewSubtaskInput('')
  }

  const handleRemoveSubtask = (index: number) => {
    setFormSubtasks(formSubtasks.filter((_, i) => i !== index)) // eslint-disable-line @typescript-eslint/naming-convention
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormTouched(true)
    setFormErrorMsg(null)
    if (!formTitle.trim()) return

    const subtaskPayload = formSubtasks.map((st, idx) => ({
      title: st,
      order_index: idx
    }))

    const sanitizedDueTime = cleanPayloadDueTime(formDueTime)

    try {
      if (editingTemplate) {
        const payload: UpdateRecurringTaskPayload = {
          title: formTitle.trim(),
          description: formDesc.trim(),
          priority: formPriority,
          project_id: formProjectId,
          recurrence_pattern: formPattern,
          recurrence_interval: formInterval,
          due_time: sanitizedDueTime,
          is_active: formIsActive,
          subtasks: subtaskPayload
        }
        await updateMutation.mutateAsync({ id: editingTemplate.id, payload })
      } else {
        const payload: CreateRecurringTaskPayload = {
          title: formTitle.trim(),
          description: formDesc.trim(),
          priority: formPriority,
          project_id: formProjectId,
          recurrence_pattern: formPattern,
          recurrence_interval: formInterval,
          due_time: sanitizedDueTime || undefined,
          is_active: formIsActive,
          subtasks: subtaskPayload
        }
        await createMutation.mutateAsync(payload)
      }

      setIsModalOpen(false)
    } catch (err) {
      setFormErrorMsg(getLocalizedErrorMessage(err, t))
    }
  }

  const handleDeleteTemplate = (template: RecurringTask) => {
    confirmDialog({
      header: t('recurring.deleteConfirmHeader'),
      message: t('recurring.deleteConfirmMessage', { title: template.title }),
      icon: 'pi pi-exclamation-triangle text-rose-500',
      defaultFocus: 'reject',
      acceptClassName: 'p-button-danger',
      acceptLabel: t('recurring.deleteBtn'),
      rejectLabel: t('recurring.cancel'),
      accept: async () => {
        await deleteMutation.mutateAsync(template.id)
      }
    })
  }

  const filteredTemplates = useMemo(() => templates.filter((tpl) => {
      if (filterActiveOnly === 'active' && !tpl.is_active) return false
      if (filterActiveOnly === 'paused' && tpl.is_active) return false
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase()
        const matchesTitle = tpl.title.toLowerCase().includes(query)
        const matchesDesc = (tpl.description || '').toLowerCase().includes(query)
        const matchesProject = (tpl.project_name || '').toLowerCase().includes(query)
        if (!matchesTitle && !matchesDesc && !matchesProject) return false
      }
      return true
    }), [templates, filterActiveOnly, searchQuery])

  const activeCount = useMemo(() => templates.filter((t) => t.is_active).length, [templates])
  const pausedCount = useMemo(() => templates.filter((t) => !t.is_active).length, [templates])

  const getPatternLabel = (pattern: RecurrencePattern, interval: number) => {
    const unitMap: Record<RecurrencePattern, string> = {
      daily: interval > 1 ? `${interval} ${t('recurring.days')}` : t('recurring.pattern.daily'),
      weekdays: t('recurring.pattern.weekdays'),
      weekly: interval > 1 ? `${interval} ${t('recurring.weeks')}` : t('recurring.pattern.weekly'),
      monthly: interval > 1 ? `${interval} ${t('recurring.months')}` : t('recurring.pattern.monthly')
    }
    return unitMap[pattern] || pattern
  }

  return (
    <div className="flex flex-col h-full space-y-4 animate-fade-in min-h-0">
      {/* Header and Controls Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 shrink-0 bg-white/70 dark:bg-slate-900/70 p-3.5 rounded-xl border border-slate-200/80 dark:border-slate-800 backdrop-blur-md shadow-xs">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200/60 dark:border-indigo-800/60 flex items-center justify-center text-indigo-600 dark:text-indigo-400 shrink-0">
            <i className="pi pi-sync text-lg animate-spin-reverse" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              {t('recurring.title')}
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 font-semibold border border-slate-200 dark:border-slate-700">
                {templates.length}
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {t('recurring.subtitle')}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={openCreateModal}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
          >
            <i className="pi pi-plus text-xs" />
            <span>{t('recurring.createTemplateBtn')}</span>
          </button>
        </div>
      </div>

      {/* Filter and Stats Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2.5 shrink-0">
        <div className="flex items-center gap-2">
          {/* Status filter buttons */}
          <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 p-0.5 text-xs">
            <button
              type="button"
              onClick={() => setFilterActiveOnly('all')}
              className={`px-2.5 py-1 rounded-md font-medium transition-colors cursor-pointer ${
                filterActiveOnly === 'all'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              {t('recurring.filterAll')} ({templates.length})
            </button>
            <button
              type="button"
              onClick={() => setFilterActiveOnly('active')}
              className={`px-2.5 py-1 rounded-md font-medium transition-colors cursor-pointer flex items-center gap-1.5 ${
                filterActiveOnly === 'active'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              {t('recurring.active')} ({activeCount})
            </button>
            <button
              type="button"
              onClick={() => setFilterActiveOnly('paused')}
              className={`px-2.5 py-1 rounded-md font-medium transition-colors cursor-pointer flex items-center gap-1.5 ${
                filterActiveOnly === 'paused'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
              {t('recurring.paused')} ({pausedCount})
            </button>
          </div>
        </div>

        {/* Search input */}
        <div className="relative min-w-[200px] max-w-xs">
          <i className="pi pi-search absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 text-xs pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t('recurring.searchPlaceholder')}
            className="w-full pl-8 pr-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/90 dark:bg-slate-900/90 text-xs text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:border-indigo-500 transition-colors"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 cursor-pointer"
            >
              <i className="pi pi-times text-[10px]" />
            </button>
          )}
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto min-h-0 pr-1">
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div
                key={i}
                className="h-44 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-white/60 dark:bg-slate-900/60 animate-pulse p-4"
              />
            ))}
          </div>
        ) : isError ? (
          <div className="p-8 text-center bg-white dark:bg-slate-900 rounded-xl border border-rose-200 dark:border-rose-900/40">
            <i className="pi pi-exclamation-circle text-rose-500 text-2xl mb-2" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              {t('recurring.loadFailed')}
            </h3>
            <button
              type="button"
              onClick={() => refetch()}
              className="mt-3 px-3 py-1.5 rounded-lg bg-indigo-600 text-white text-xs font-medium cursor-pointer"
            >
              {t('recurring.retry')}
            </button>
          </div>
        ) : filteredTemplates.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-12 text-center bg-white/60 dark:bg-slate-900/60 rounded-2xl border border-slate-200/80 dark:border-slate-800">
            <div className="w-12 h-12 rounded-full bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 flex items-center justify-center text-indigo-500 dark:text-indigo-400 mb-3">
              <i className="pi pi-calendar-plus text-xl" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
              {t('recurring.noTemplatesFound')}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm">
              {t('recurring.noTemplatesSubtitle')}
            </p>
            <button
              type="button"
              onClick={openCreateModal}
              className="mt-4 inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
            >
              <i className="pi pi-plus text-xs" />
              <span>{t('recurring.createTemplateBtn')}</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
            {filteredTemplates.map((template) => {
              const priorityInfo = PRIORITY_META[template.priority] || PRIORITY_META.medium
              const patternText = getPatternLabel(template.recurrence_pattern, template.recurrence_interval)

              return (
                <div
                  key={template.id}
                  className={`flex flex-col justify-between p-4 rounded-xl border transition-all duration-200 ${
                    template.is_active
                      ? 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 shadow-xs hover:border-indigo-400/60 dark:hover:border-indigo-600/60'
                      : 'bg-slate-50/70 dark:bg-slate-950/50 border-slate-200/60 dark:border-slate-800/60 opacity-80'
                  }`}
                >
                  <div className="space-y-2.5">
                    {/* Top Row: Project badge & Active Toggle */}
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5 min-w-0">
                        {template.project_name ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60 truncate">
                            <span
                              className="w-1.5 h-1.5 rounded-full shrink-0"
                              style={{ backgroundColor: template.project_color || '#6366f1' }}
                            />
                            <span className="truncate">{template.project_name}</span>
                          </span>
                        ) : (
                          <span className="text-[10px] text-slate-400">{t('recurring.noProject')}</span>
                        )}
                        <span
                          className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold border ${priorityInfo.badge}`}
                        >
                          <i className={`pi ${priorityInfo.icon} text-[9px]`} />
                          <span className="capitalize">{template.priority}</span>
                        </span>
                      </div>

                      {/* Active Toggle Switch */}
                      <div className="flex items-center gap-1.5 shrink-0">
                        <span
                          className={`text-[10px] font-semibold ${
                            template.is_active
                              ? 'text-emerald-600 dark:text-emerald-400'
                              : 'text-amber-600 dark:text-amber-400'
                          }`}
                        >
                          {template.is_active ? t('recurring.active') : t('recurring.paused')}
                        </span>
                        <InputSwitch
                          checked={template.is_active}
                          disabled={toggleActiveMutation.isPending}
                          onChange={() => toggleActiveMutation.mutate(template.id)}
                          className="scale-75"
                        />
                      </div>
                    </div>

                    {/* Title & Description */}
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 leading-snug line-clamp-1">
                        {template.title}
                      </h3>
                      {template.description && (
                        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                          {template.description}
                        </p>
                      )}
                    </div>

                    {/* Recurrence & Due Meta Chips */}
                    <div className="flex flex-wrap items-center gap-1.5 pt-1">
                      <span className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-medium bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/40">
                        <i className="pi pi-sync text-[10px]" />
                        <span>{patternText}</span>
                      </span>

                      {Boolean(formatDueTimeDisplay(template.due_time)) && (
                        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                          <i className="pi pi-clock text-[10px]" />
                          <span>{formatDueTimeDisplay(template.due_time)}</span>
                        </span>
                      )}

                      {template.subtasks && template.subtasks.length > 0 && (
                        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200/60 dark:border-slate-700/60">
                          <i className="pi pi-check-square text-[10px]" />
                          <span>{t('recurring.subtasksBadge', { count: template.subtasks.length })}</span>
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Bottom Row: Actions */}
                  <div className="flex items-center justify-between pt-3 mt-3 border-t border-slate-100 dark:border-slate-800/80">
                    <span className="text-[10px] text-slate-400">
                      ID #{template.id}
                    </span>

                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => openEditModal(template)}
                        title={t('recurring.editTooltip')}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                      >
                        <i className="pi pi-pencil text-xs" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteTemplate(template)}
                        title={t('recurring.deleteTooltip')}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
                      >
                        <i className="pi pi-trash text-xs" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Modal: Create / Edit Template */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/45 dark:bg-slate-950/75 backdrop-blur-sm animate-fade-in">
          <div className="relative w-full max-w-xl max-h-[90vh] flex flex-col rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-2xl text-slate-900 dark:text-slate-100 overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800 shrink-0">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200/60 dark:border-indigo-800/60 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
                  <i className="pi pi-sync text-sm" />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    {editingTemplate ? t('recurring.editTemplateTitle') : t('recurring.createTemplateTitle')}
                  </h2>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    {t('recurring.subtitle')}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                title={t('common.close') || 'Tutup'}
              >
                <i className="pi pi-times text-xs" />
              </button>
            </div>

            {/* Error Message Banner */}
            {formErrorMsg && (
              <div className="mx-6 mt-4 flex items-start gap-2 rounded-lg border border-rose-500/20 bg-rose-500/10 p-3 text-xs text-rose-600 dark:text-rose-400 shrink-0">
                <i className="pi pi-exclamation-circle text-sm mt-0.5 shrink-0" />
                <span className="flex-1 font-medium">{formErrorMsg}</span>
                <button
                  type="button"
                  onClick={() => setFormErrorMsg(null)}
                  className="text-rose-500/70 hover:text-rose-500 cursor-pointer"
                >
                  <i className="pi pi-times text-xs" />
                </button>
              </div>
            )}

            {/* Scrollable Form Body */}
            <form onSubmit={handleSave} className="flex-1 overflow-y-auto p-6 space-y-4">
              {/* Title */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  {t('recurring.form.titleLabel')} <span className="text-rose-500 font-bold">*</span>
                </label>
                <input
                  type="text"
                  required
                  autoFocus
                  value={formTitle}
                  onBlur={() => setFormTouched(true)}
                  onChange={(e) => {
                    setFormTitle(e.target.value)
                    if (formTouched) setFormTouched(true)
                  }}
                  placeholder={t('recurring.form.titlePlaceholder')}
                  className={`w-full rounded-lg border px-3 py-2 text-xs text-slate-900 dark:text-slate-100 bg-slate-50 dark:bg-slate-950 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none transition-colors ${
                    formTouched && !formTitle.trim()
                      ? 'border-rose-500 focus:border-rose-500 ring-1 ring-rose-500/30'
                      : 'border-slate-200 dark:border-slate-800 focus:border-indigo-500'
                  }`}
                />
                {formTouched && !formTitle.trim() && (
                  <p className="text-[11px] text-rose-500 mt-1 flex items-center gap-1 font-medium">
                    <i className="pi pi-exclamation-circle text-[10px]" />
                    {t('recurring.form.titleRequired')}
                  </p>
                )}
              </div>

              {/* Description */}
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  {t('recurring.form.descriptionLabel')}
                </label>
                <textarea
                  rows={2}
                  value={formDesc}
                  onChange={(e) => setFormDesc(e.target.value)}
                  placeholder={t('recurring.form.descriptionPlaceholder')}
                  className="w-full rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 px-3 py-2 text-xs text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:border-indigo-500 focus:outline-none resize-none transition-colors"
                />
              </div>

              {/* Project & Priority */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('recurring.form.projectLabel')}
                  </label>
                  <select
                    value={formProjectId === null ? '' : String(formProjectId)}
                    onChange={(e) => setFormProjectId(e.target.value ? parseInt(e.target.value, 10) : null)}
                    className="w-full rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 px-2.5 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:border-indigo-500 focus:outline-none transition-colors"
                  >
                    <option value="" className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
                      {t('recurring.form.noProject')}
                    </option>
                    {projects.map((p) => (
                      <option
                        key={p.id}
                        value={String(p.id)}
                        className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100"
                      >
                        📁 {p.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('recurring.form.priorityLabel')} <span className="text-rose-500 font-bold">*</span>
                  </label>
                  <select
                    value={formPriority}
                    onChange={(e) => setFormPriority(e.target.value as TaskPriority)}
                    className="w-full rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 px-2.5 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:border-indigo-500 focus:outline-none transition-colors"
                  >
                    <option value="low" className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
                      {t('recurring.form.priorityLow')}
                    </option>
                    <option value="medium" className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
                      {t('recurring.form.priorityMedium')}
                    </option>
                    <option value="high" className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
                      {t('recurring.form.priorityHigh')}
                    </option>
                    <option value="urgent" className="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
                      {t('recurring.form.priorityUrgent')}
                    </option>
                  </select>
                </div>
              </div>

              {/* Recurrence Pattern & Interval Container */}
              <div className="p-3.5 rounded-xl border border-indigo-100 dark:border-indigo-900/40 bg-indigo-50/40 dark:bg-indigo-950/20 space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                      <i className="pi pi-sync text-indigo-500 text-[10px]" />
                      {t('recurring.form.patternLabel')}
                    </label>
                    <select
                      value={formPattern}
                      onChange={(e) => setFormPattern(e.target.value as RecurrencePattern)}
                      className="w-full rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-2.5 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:border-indigo-500 focus:outline-none transition-colors"
                    >
                      <option value="daily">{t('recurring.form.patternDaily')}</option>
                      <option value="weekdays">{t('recurring.form.patternWeekdays')}</option>
                      <option value="weekly">{t('recurring.form.patternWeekly')}</option>
                      <option value="monthly">{t('recurring.form.patternMonthly')}</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center justify-between">
                      <span>{t('recurring.form.intervalLabel')}</span>
                      <span className="text-[10px] text-slate-400 font-normal">
                        {formPattern === 'daily'
                          ? t('recurring.days')
                          : formPattern === 'weekly'
                          ? t('recurring.weeks')
                          : formPattern === 'monthly'
                          ? t('recurring.months')
                          : ''}
                      </span>
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={365}
                      disabled={formPattern === 'weekdays'}
                      value={formInterval}
                      onChange={(e) => setFormInterval(Math.max(1, Number(e.target.value)))}
                      className="w-full rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-2.5 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:border-indigo-500 focus:outline-none disabled:opacity-50 transition-colors"
                    />
                  </div>
                </div>

                {/* Due Time & Quick Presets */}
                <div className="pt-2.5 border-t border-indigo-100/70 dark:border-indigo-900/30">
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                      <i className="pi pi-clock text-indigo-500 text-[10px]" />
                      {t('recurring.form.dueTimeLabel')}
                    </label>
                    {formDueTime && (
                      <button
                        type="button"
                        onClick={() => setFormDueTime('')}
                        className="text-[10px] text-slate-400 hover:text-rose-500 flex items-center gap-1 transition-colors cursor-pointer"
                      >
                        <i className="pi pi-times text-[9px]" />
                        <span>{t('common.clear') || 'Reset Jam'}</span>
                      </button>
                    )}
                  </div>

                  <div className="flex flex-wrap sm:flex-nowrap items-center gap-2">
                    <input
                      type="time"
                      value={formDueTime}
                      onChange={(e) => setFormDueTime(e.target.value)}
                      className="w-full sm:w-36 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-2.5 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:border-indigo-500 focus:outline-none transition-colors"
                    />

                    {/* Quick Preset Buttons */}
                    <div className="flex items-center gap-1 overflow-x-auto py-0.5">
                      {['09:00', '12:00', '17:00', '21:00', '23:59'].map((preset) => (
                        <button
                          key={preset}
                          type="button"
                          onClick={() => setFormDueTime(preset)}
                          className={`px-2 py-1 rounded-md text-[11px] font-medium transition-colors cursor-pointer border ${
                            formDueTime === preset
                              ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800'
                          }`}
                        >
                          {preset}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Status Aktif Toggle */}
                <div className="pt-2.5 border-t border-indigo-100/70 dark:border-indigo-900/30 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <InputSwitch
                      checked={formIsActive}
                      onChange={(e) => setFormIsActive(Boolean(e.value))}
                    />
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
                      {t('recurring.form.activeStatusLabel')}
                    </span>
                  </div>
                  <span
                    className={`text-[11px] font-semibold ${
                      formIsActive
                        ? 'text-emerald-600 dark:text-emerald-400'
                        : 'text-amber-600 dark:text-amber-400'
                    }`}
                  >
                    {formIsActive ? t('recurring.active') : t('recurring.paused')}
                  </span>
                </div>
              </div>

              {/* Checklist Subtasks Template */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-medium text-slate-600 dark:text-slate-400">
                    {t('recurring.form.subtasksLabel')}
                    {formSubtasks.length > 0 && (
                      <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800/60">
                        {formSubtasks.length}
                      </span>
                    )}
                  </label>
                </div>

                {/* Subtask Items */}
                {formSubtasks.length > 0 && (
                  <div className="space-y-1.5 mb-2 max-h-36 overflow-y-auto pr-0.5">
                    {formSubtasks.map((st, idx) => (
                      <div
                        key={idx}
                        className="group flex items-center justify-between p-2 rounded-lg bg-slate-50 dark:bg-slate-950/60 hover:bg-slate-100/80 dark:hover:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs transition-colors"
                      >
                        <div className="flex items-center gap-2 flex-1 min-w-0">
                          <i className="pi pi-check-square text-xs text-indigo-500 dark:text-indigo-400 shrink-0" />
                          <span className="truncate text-slate-800 dark:text-slate-200">
                            {st}
                          </span>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleRemoveSubtask(idx)}
                          className="p-1 text-slate-400 hover:text-rose-500 dark:hover:text-rose-400 opacity-70 group-hover:opacity-100 transition-opacity cursor-pointer"
                          title="Hapus subtask"
                        >
                          <i className="pi pi-times text-[10px]" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Add Subtask Input */}
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newSubtaskInput}
                    onChange={(e) => setNewSubtaskInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        handleAddSubtask(e)
                      }
                    }}
                    placeholder={t('recurring.form.subtaskPlaceholder')}
                    className="flex-1 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 px-3 py-1.5 text-xs text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:border-indigo-500 focus:outline-none transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => handleAddSubtask()}
                    disabled={!newSubtaskInput.trim()}
                    className="px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 disabled:opacity-50 text-xs font-medium text-slate-700 dark:text-slate-200 flex items-center gap-1 transition-colors cursor-pointer"
                  >
                    <i className="pi pi-plus text-[10px]" />
                    {t('recurring.form.addSubtaskBtn')}
                  </button>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800 shrink-0">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-3.5 py-1.5 text-xs text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                >
                  {t('common.cancel') || 'Batal'}
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending || updateMutation.isPending || !formTitle.trim()}
                  className="px-4 py-1.5 text-xs font-medium text-white bg-indigo-600 hover:bg-indigo-500 rounded-lg disabled:opacity-50 flex items-center gap-1.5 shadow-xs transition-colors cursor-pointer"
                >
                  {(createMutation.isPending || updateMutation.isPending) && (
                    <i className="pi pi-spin pi-spinner text-xs" />
                  )}
                  <span>{editingTemplate ? t('recurring.save') : t('recurring.createTemplateBtn')}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
