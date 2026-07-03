import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./index.css";
import App from "./App.tsx";
import { StoreProvider } from "./store/StoreContext";
import { CasesProvider } from "./store/CasesContext";
import { HomePage } from "./pages/HomePage";
import { CaseOpenPage } from "./pages/CaseOpenPage";
import { UpgradePage } from "./pages/UpgradePage";
import { ProfilePage } from "./pages/ProfilePage";
import { AdminPage } from "./pages/AdminPage";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <CasesProvider>
      <StoreProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<App />}>
              <Route index element={<HomePage />} />
              <Route path="case/:caseId" element={<CaseOpenPage />} />
              <Route path="upgrade" element={<UpgradePage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="admin" element={<AdminPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </StoreProvider>
    </CasesProvider>
  </StrictMode>
);
