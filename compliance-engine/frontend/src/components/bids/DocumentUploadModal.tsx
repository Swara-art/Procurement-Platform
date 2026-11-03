import React, { useState } from 'react';
import { X, UploadCloud, FileText, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { bidsApi } from '../../api/bidsApi';
import { BidDocument } from '../../types';

interface DocumentUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  bidId: string;
  onUploadSuccess: (newDoc: BidDocument) => void;
}

export const DocumentUploadModal: React.FC<DocumentUploadModalProps> = ({
  isOpen,
  onClose,
  bidId,
  onUploadSuccess,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      if (!selected.name.toLowerCase().endsWith('.pdf')) {
        setError('Only PDF documents are supported for AI evidence extraction.');
        setFile(null);
        return;
      }
      if (selected.size > 10 * 1024 * 1024) {
        setError('File size exceeds the 10 MB limit specified in the procurement guidelines.');
        setFile(null);
        return;
      }
      setFile(selected);
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select a PDF document to upload.');
      return;
    }

    setIsUploading(true);
    setError('');
    try {
      const doc = await bidsApi.uploadDocument(bidId, file);
      // Auto trigger indexing for evidence retrieval
      await bidsApi.indexDocument(doc.id);
      setSuccessMsg(`Document "${file.name}" uploaded and indexed successfully into vector store.`);
      setTimeout(() => {
        onUploadSuccess(doc);
        setIsUploading(false);
        setSuccessMsg('');
        setFile(null);
        onClose();
      }, 1200);
    } catch (err: any) {
      setError(err?.message || 'Failed to upload document. Please ensure PDF is valid.');
      setIsUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-navy-900 text-white flex items-center justify-center">
              <UploadCloud className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Upload Bidder Verification PDF</h3>
              <p className="text-[11px] text-slate-500">
                Bid ID: {bidId}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-slate-400 hover:text-slate-600 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Upload Body */}
        <div className="p-6 space-y-4 text-xs">
          {/* Dropzone */}
          <label className="border-2 border-dashed border-slate-200 hover:border-navy-900/40 rounded-xl p-8 flex flex-col items-center justify-center text-center cursor-pointer bg-slate-50/50 hover:bg-slate-50 transition-all block">
            <input
              type="file"
              accept=".pdf"
              onChange={handleFileChange}
              className="hidden"
            />
            <div className="w-12 h-12 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-400 mb-3 shadow-2xs">
              <FileText className="w-6 h-6 stroke-[1.5]" />
            </div>
            {file ? (
              <div>
                <p className="font-semibold text-slate-900 text-sm truncate max-w-xs">{file.name}</p>
                <p className="text-slate-400 text-xs mt-0.5">
                  {(file.size / (1024 * 1024)).toFixed(2)} MB • PDF Document
                </p>
              </div>
            ) : (
              <div>
                <p className="font-semibold text-slate-800">
                  Click to select or drag and drop PDF file
                </p>
                <p className="text-slate-400 text-[11px] mt-1">
                  Audited Balance Sheets, Tax Registrations, ISO Certificates (Max 10 MB)
                </p>
              </div>
            )}
          </label>

          {/* Feedback messages */}
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg flex items-center gap-2 text-emerald-700">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Help notice */}
          <div className="p-3 bg-slate-50 rounded-lg text-slate-500 leading-relaxed text-[11px]">
            Once uploaded, the document will be automatically chunked and indexed into the pgvector vector store for hybrid RAG semantic query verification.
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isUploading}
              className="px-3.5 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleUpload}
              disabled={!file || isUploading}
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors disabled:opacity-50 cursor-pointer"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  Uploading & Indexing...
                </>
              ) : (
                'Upload and Index PDF'
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
