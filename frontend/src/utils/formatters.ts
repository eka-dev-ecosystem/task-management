/**
 * Example Utility Functions
 * Keep generic helper functions here (e.g. date formatting, currency, etc).
 */

export const formatCurrency = (value: number): string =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value)

export const formatDate = (date: string | Date): string =>
  new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  }).format(new Date(date))

export interface SmartDueDate {
  text: string
  isToday: boolean
  isTomorrow: boolean
  isOverdue: boolean
}

export const getSmartDueDate = (
  dateInput: string | Date,
  locale: string = 'en',
  t?: (key: string) => string
): SmartDueDate => {
  const d = new Date(dateInput)
  if (isNaN(d.getTime())) {
    return { text: '', isToday: false, isTomorrow: false, isOverdue: false }
  }

  const now = new Date()
  const dDate = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  const diffDays = Math.round((dDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  const isToday = diffDays === 0
  const isTomorrow = diffDays === 1
  const isOverdue = diffDays < 0

  const formattedDayMonth = d.toLocaleDateString(locale === 'id' ? 'id-ID' : 'en-US', {
    day: 'numeric',
    month: 'short'
  })

  let text: string
  if (isToday) {
    const todayLabel = t ? t('common.today') : locale === 'id' ? 'Hari Ini' : 'Today'
    text = `${todayLabel}, ${formattedDayMonth}`
  } else if (isTomorrow) {
    const tomorrowLabel = t ? t('common.tomorrow') : locale === 'id' ? 'Besok' : 'Tomorrow'
    text = `${tomorrowLabel}, ${formattedDayMonth}`
  } else {
    text = formattedDayMonth
  }

  return {
    text,
    isToday,
    isTomorrow,
    isOverdue
  }
}

export const cn = (...classes: (string | boolean | undefined | null)[]): string =>
  classes.filter(Boolean).join(' ')
