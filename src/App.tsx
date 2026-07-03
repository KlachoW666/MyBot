import { Outlet } from "react-router-dom";
import { Navbar } from "./components/Navbar";
import { StatsBar } from "./components/StatsBar";
import { TopCasesStrip } from "./components/TopCasesStrip";
import { Footer } from "./components/Footer";

function App() {
  return (
    <div className="flex min-h-full flex-col bg-noise">
      <Navbar />
      <StatsBar />
      <TopCasesStrip />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 lg:px-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

export default App;
