import React from 'react';
import { Search, X } from 'lucide-react';

interface FilterOption {
  label: string;
  value: string;
  count?: number;
}

interface SearchAndFiltersProps {
  searchPlaceholder?: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  filters?: {
    current: string;
    options: FilterOption[];
    onChange: (value: string) => void;
  };
  extraControls?: React.ReactNode;
}

export const SearchAndFilters: React.FC<SearchAndFiltersProps> = ({
  searchPlaceholder = 'Search...',
  searchValue,
  onSearchChange,
  filters,
  extraControls,
}) => {
  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
      {/* Filter Tabs / Pills */}
      {filters && (
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0 scrollbar-none">
          {filters.options.map((opt) => {
            const active = filters.current === opt.value;
            return (
              <button
                key={opt.value}
                onClick={() => filters.onChange(opt.value)}
                className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all whitespace-nowrap cursor-pointer ${
                  active
                    ? 'bg-slate-900 text-white shadow-sm font-semibold'
                    : 'bg-white text-slate-600 hover:bg-slate-100 hover:text-slate-900 border border-slate-200'
                }`}
              >
                {opt.label}
                {typeof opt.count === 'number' && (
                  <span
                    className={`ml-1.5 px-1.5 py-0.2 text-[10px] rounded-full ${
                      active ? 'bg-slate-700 text-slate-200' : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {opt.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}

      {/* Search Input & Controls */}
      <div className="flex items-center gap-2.5">
        <div className="relative flex-1 sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={searchPlaceholder}
            className="w-full pl-9 pr-8 py-1.5 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900 transition-all shadow-subtle"
          />
          {searchValue && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
        {extraControls}
      </div>
    </div>
  );
};
