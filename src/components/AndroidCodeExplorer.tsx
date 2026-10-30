import React, { useState } from 'react';
import {
  FileCode,
  Download,
  Copy,
  Check,
  Folder,
  Layers,
  Code2,
  FileText,
  Settings,
  Sparkles
} from 'lucide-react';
import JSZip from 'jszip';
import { ANDROID_PROJECT_FILES, AndroidFile } from '../androidProjectFiles';

export const AndroidCodeExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<AndroidFile>(ANDROID_PROJECT_FILES[3]); // AndroidManifest.xml by default
  const [copied, setCopied] = useState(false);
  const [isZipping, setIsZipping] = useState(false);
  const [filterCategory, setFilterCategory] = useState<string>('all');

  const copyToClipboard = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadAndroidZip = async () => {
    setIsZipping(true);
    try {
      const zip = new JSZip();

      // Add all project files into zip
      ANDROID_PROJECT_FILES.forEach((f) => {
        zip.file(f.path, f.content);
      });

      // Add a root gradlew stub and settings
      zip.file(
        'android/gradlew',
        `#!/usr/bin/env sh
exec gradle "$@"
`
      );

      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'floating-pro-scanner-android-source.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Failed to generate zip:', e);
    } finally {
      setIsZipping(false);
    }
  };

  const filteredFiles =
    filterCategory === 'all'
      ? ANDROID_PROJECT_FILES
      : ANDROID_PROJECT_FILES.filter((f) => f.category === filterCategory);

  return (
    <div className="space-y-4">
      {/* Top action header */}
      <div className="bg-[#0F151A] border border-[#1E2830] p-4 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Code2 className="w-5 h-5 text-[#00F59B]" />
            Native Android Project Source Explorer
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Complete production-ready Kotlin 2.0 & Jetpack Compose source code, AndroidManifest, Foreground Services, MediaProjection, and Gradle build files.
          </p>
        </div>

        {/* Download Zip Button */}
        <button
          onClick={downloadAndroidZip}
          disabled={isZipping}
          className="px-4 py-2.5 rounded-xl bg-[#00F59B] hover:bg-[#00c97f] active:scale-95 text-black font-extrabold text-xs flex items-center gap-2 shadow-[0_0_20px_#00F59B55] cursor-pointer transition disabled:opacity-50"
        >
          <Download className="w-4 h-4 stroke-[2.5]" />
          {isZipping ? 'Packaging ZIP...' : 'Download Android Project (.zip)'}
        </button>
      </div>

      {/* Main File Explorer Window */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 bg-[#070B0E] border border-[#1E2830] rounded-2xl overflow-hidden min-h-[560px]">
        
        {/* Left Sidebar: File Tree */}
        <div className="lg:col-span-4 bg-[#090D11] border-r border-[#1E2830] p-3 flex flex-col space-y-3">
          {/* Category Filter Tabs */}
          <div className="flex flex-wrap gap-1 text-[11px]">
            {['all', 'kotlin', 'manifest', 'config', 'res', 'docs'].map((cat) => (
              <button
                key={cat}
                onClick={() => setFilterCategory(cat)}
                className={`px-2 py-1 rounded-md capitalize font-semibold transition ${
                  filterCategory === cat
                    ? 'bg-[#00F59B] text-black'
                    : 'bg-[#141C24] text-slate-400 hover:text-white'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider px-1">
            Android Files ({filteredFiles.length})
          </div>

          {/* Files List */}
          <div className="flex-1 overflow-y-auto space-y-1 pr-1 max-h-[460px]">
            {filteredFiles.map((file) => {
              const isSelected = selectedFile.path === file.path;
              return (
                <div
                  key={file.path}
                  onClick={() => setSelectedFile(file)}
                  className={`p-2 rounded-xl text-xs cursor-pointer flex items-center justify-between transition ${
                    isSelected
                      ? 'bg-[#00F59B]/15 text-[#00F59B] border border-[#00F59B]/40 font-bold'
                      : 'text-slate-300 hover:bg-[#121921] border border-transparent'
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <FileCode
                      className={`w-3.5 h-3.5 shrink-0 ${
                        isSelected ? 'text-[#00F59B]' : 'text-slate-500'
                      }`}
                    />
                    <span className="truncate">{file.name}</span>
                  </div>
                  <span className="text-[9px] uppercase px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                    {file.category}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Code Display Area */}
        <div className="lg:col-span-8 p-4 flex flex-col justify-between overflow-hidden">
          {/* File Header Bar */}
          <div className="flex items-center justify-between pb-3 border-b border-[#1E2830] mb-3">
            <div>
              <div className="text-xs font-mono font-bold text-[#00F59B] flex items-center gap-2">
                <span>{selectedFile.path}</span>
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                {selectedFile.description}
              </div>
            </div>

            <button
              onClick={copyToClipboard}
              className="px-3 py-1.5 rounded-lg bg-[#141C24] hover:bg-[#1B2631] text-xs text-slate-200 flex items-center gap-1.5 border border-[#1E2830] transition"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-[#00F59B]" />
                  <span className="text-[#00F59B]">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Copy Code</span>
                </>
              )}
            </button>
          </div>

          {/* Syntax Code Block */}
          <div className="flex-1 overflow-auto bg-[#040608] border border-[#172026] rounded-xl p-4 font-mono text-xs text-slate-300 leading-relaxed max-h-[460px]">
            <pre>
              <code>{selectedFile.content}</code>
            </pre>
          </div>
        </div>

      </div>
    </div>
  );
};
