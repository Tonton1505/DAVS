export function App() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-sm text-slate-800">
      <header className="h-14 bg-navy text-white px-6 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-lg tracking-wide">DAVS</span>
          <span className="text-xs text-slate-300">
            Hệ thống Phân tích và Trực quan hóa dữ liệu
          </span>
        </div>
        <span className="text-xs bg-primary px-2.5 py-1 rounded">v0.1.0</span>
      </header>
      <div className="flex flex-1">
        <aside
          aria-label="Điều hướng chính"
          className="w-40 bg-white border-r border-border p-3 flex flex-col gap-1"
        >
          <div className="px-3 py-2 rounded bg-slate-100 font-medium text-navy">
            Trang chủ
          </div>
        </aside>
        <main className="flex-1 p-6">
          <h1 className="text-xl font-semibold text-navy mb-2">
            Khung hệ thống DAVS
          </h1>
          <p className="text-slate-600">
            Môi trường Docker Compose (PostgreSQL 16, MinIO, Spring Boot, React +
            Canvas) đã sẵn sàng.
          </p>
        </main>
      </div>
    </div>
  );
}
