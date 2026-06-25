import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { studentsApi } from '../api/students';
import { certificatesApi } from '../api/certificates';
import { useThemeClasses } from '../lib/theme';
import { Award, Download, FileText } from 'lucide-react';
import toast from 'react-hot-toast';

export const Certificates: React.FC = () => {
  const t = useThemeClasses();
  const [selectedStudentId, setSelectedStudentId] = useState(0);
  const [certName, setCertName] = useState('');
  const [issueDate, setIssueDate] = useState('');
  const [loading, setLoading] = useState(false);

  const { data: students = [] } = useQuery({
    queryKey: ['students-active'],
    queryFn: studentsApi.getActive,
  });

  const selectedStudent = students.find((s) => s.id === selectedStudentId);

  const today = new Date().toISOString().split('T')[0];

  const handleGenerate = async () => {
    if (!selectedStudent || !certName.trim() || !issueDate) {
      toast.error('Bütün sahələri doldurun');
      return;
    }

    const selectedDate = new Date(issueDate);
    if (selectedDate > new Date()) {
      toast.error('Tarix gələcəkdə ola bilməz');
      return;
    }

    setLoading(true);
    try {
      const blob = await certificatesApi.generate({
        studentId: selectedStudent.id,
        certName: certName.trim(),
        issueDate,
      });

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `sertifikat-${selectedStudent.firstName}-${selectedStudent.lastName}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      toast.success('Sertifikat yükləndi və emailə göndərildi!');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Sertifikat yaratmaq mümkün olmadı');
    } finally {
      setLoading(false);
    }
  };

  const inputCls = `w-full px-3 py-2.5 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;

  return (
    <div className="space-y-6">
      <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Sertifikatlar</h1>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className={`${t.cardBg} border rounded-xl p-6 transition-colors duration-300 max-w-xl`}
      >
        <div className="flex items-center gap-3 mb-6">
          <div className="p-3 bg-indigo-500/10 rounded-xl">
            <Award size={24} className="text-indigo-400" />
          </div>
          <div>
            <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>Yeni Sertifikat Yarat</h2>
            <p className={`text-sm transition-colors duration-300 ${t.muted}`}>PDF formatında sertifikat generasiya edin</p>
          </div>
        </div>

        <div className="space-y-4">
          {/* Student selector */}
          <div>
            <label className={`block text-sm font-medium mb-1.5 transition-colors duration-300 ${t.label}`}>Tələbə seçin</label>
            <select
              value={selectedStudentId}
              onChange={(e) => setSelectedStudentId(+e.target.value)}
              className={inputCls}
            >
              <option value={0}>Tələbə seçin...</option>
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName} ({s.fin})
                </option>
              ))}
            </select>
          </div>

          {/* Selected student info */}
          {selectedStudent && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className={`p-3 rounded-lg transition-colors duration-300 ${t.itemBg}`}
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-indigo-500/10 rounded-full flex items-center justify-center">
                  <span className="text-indigo-400 font-semibold">
                    {selectedStudent.firstName.charAt(0)}{selectedStudent.lastName.charAt(0)}
                  </span>
                </div>
                <div>
                  <p className={`font-medium transition-colors duration-300 ${t.title}`}>
                    {selectedStudent.firstName} {selectedStudent.lastName}
                  </p>
                  <p className={`text-xs transition-colors duration-300 ${t.muted}`}>
                    FİN: {selectedStudent.fin} | {selectedStudent.email}
                  </p>
                </div>
              </div>
            </motion.div>
          )}

          {/* Certificate name */}
          <div>
            <label className={`block text-sm font-medium mb-1.5 transition-colors duration-300 ${t.label}`}>Sertifikat adı</label>
            <input
              type="text"
              value={certName}
              onChange={(e) => setCertName(e.target.value)}
              placeholder="Məs: Python Proqramlaşdırma Kursu"
              className={inputCls}
            />
          </div>

          {/* Issue date */}
          <div>
            <label className={`block text-sm font-medium mb-1.5 transition-colors duration-300 ${t.label}`}>Verilmə tarixi</label>
            <input
              type="date"
              value={issueDate}
              onChange={(e) => setIssueDate(e.target.value)}
              max={today}
              className={inputCls}
            />
          </div>

          {/* Generate button */}
          <button
            onClick={handleGenerate}
            disabled={!selectedStudentId || !certName.trim() || !issueDate || loading}
            className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-medium rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-indigo-700/25"
          >
            {loading ? (
              <>
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                Yüklənir...
              </>
            ) : (
              <>
                <Download size={18} />
                Sertifikat Yüklə
              </>
            )}
          </button>
        </div>
      </motion.div>

      {/* Info card */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300 max-w-xl`}
      >
        <div className="flex items-start gap-3">
          <FileText size={18} className="text-slate-400 mt-0.5" />
          <div>
            <p className={`text-sm transition-colors duration-300 ${t.muted}`}>
              Sertifikat PDF formatında yaradılacaq. Daxilində tələbənin adı, sertifikatın adı və tarix yer alacaq.
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
};
