import { getCategoryColor, getCategoryBg, getCategoryIcon } from '../../utils/categories'

export function CategoryBadge({ category, size = 'sm' }) {
  const color = getCategoryColor(category)
  const bg    = getCategoryBg(category)
  const icon  = getCategoryIcon(category)

  const sizeClass = size === 'xs'
    ? 'px-2 py-0.5 text-xs gap-1'
    : 'px-2.5 py-1 text-xs gap-1.5'

  return (
    <span
      className={`inline-flex items-center rounded-full font-medium ${sizeClass}`}
      style={{ color, background: bg }}
    >
      <span className="text-sm leading-none">{icon}</span>
      {category || 'Other'}
    </span>
  )
}
