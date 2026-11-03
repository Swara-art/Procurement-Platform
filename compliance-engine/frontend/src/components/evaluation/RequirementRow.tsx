import React from 'react';
import { Eye, ShieldCheck } from 'lucide-react';
import { RequirementEvaluationItem } from '../../types';
import { StatusBadge } from '../common/StatusBadge';

interface RequirementRowProps {
  item: RequirementEvaluationItem;
  onViewEvidence: (item: RequirementEvaluationItem) => void;
}

export const RequirementRow: React.FC<RequirementRowProps> = ({ item, onViewEvidence }) => {
  return (
    <tr className="hover:bg-slate-50/80 transition-colors border-b border-slate-100 last:border-b-0">
      {/* Requirement Details */}
      <td className="py-3.5 px-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-slate-900 text-xs">{item.title}</span>
            {item.mandatory && (
              <span className="px-1.5 py-0.2 bg-rose-50 text-rose-600 border border-rose-200 text-[10px] font-bold rounded">
                Mandatory
              </span>
            )}
            {item.manualOverride && (
              <span className="inline-flex items-center gap-1 px-1.5 py-0.2 bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-semibold rounded" title={`Overridden by ${item.manualOverride.overriddenBy}`}>
                <ShieldCheck className="w-3 h-3 text-emerald-600" />
                Overridden
              </span>
            )}
          </div>
          <span className="text-[11px] text-slate-400 font-mono block mt-0.5">
            {item.requirementCode}
          </span>
        </div>
      </td>

      {/* Required Value */}
      <td className="py-3.5 px-4 text-xs font-medium text-slate-700">
        {item.requiredValue}
      </td>

      {/* Extracted Value */}
      <td className="py-3.5 px-4 text-xs font-mono font-medium text-slate-900">
        {item.extractedValue}
      </td>

      {/* Status Badge */}
      <td className="py-3.5 px-4">
        <StatusBadge status={item.status} />
      </td>

      {/* Action */}
      <td className="py-3.5 px-4 text-right">
        <button
          onClick={() => onViewEvidence(item)}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 hover:text-navy-900 transition-all shadow-2xs cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5 text-slate-500" />
          View Evidence
        </button>
      </td>
    </tr>
  );
};
