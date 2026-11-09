import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { ProfilesList } from './pages/profiles/ProfilesList'
import { ProfileView } from './pages/profiles/ProfileView'
import { ProfileForm } from './pages/profiles/ProfileForm'
import { CompaniesList } from './pages/companies/CompaniesList'
import { CompanyView } from './pages/companies/CompanyView'
import { CompanyForm } from './pages/companies/CompanyForm'
import { OutreachList } from './pages/outreach/OutreachList'
import { OutreachForm } from './pages/outreach/OutreachForm'
import { DsaTrackerPage } from './pages/dsa/DsaTrackerPage'
import { SystemDesignTrackerPage } from './pages/systemdesign/SystemDesignTrackerPage'
import { ResourcesPage } from './pages/resources/ResourcesPage'
import { PlannerPage } from './pages/PlannerPage'
import { CalendarPage } from './pages/calendar/CalendarPage'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />

        <Route path="profiles" element={<ProfilesList />} />
        <Route path="profiles/new" element={<ProfileForm />} />
        <Route path="profiles/:id" element={<ProfileView />} />
        <Route path="profiles/:id/edit" element={<ProfileForm />} />

        <Route path="companies" element={<CompaniesList />} />
        <Route path="companies/new" element={<CompanyForm />} />
        <Route path="companies/:id" element={<CompanyView />} />
        <Route path="companies/:id/edit" element={<CompanyForm />} />

        <Route path="outreach" element={<OutreachList />} />
        <Route path="outreach/new" element={<OutreachForm />} />
        <Route path="outreach/:id/prepare" element={<OutreachForm />} />

        <Route path="dsa-tracker" element={<DsaTrackerPage />} />
        <Route path="system-design" element={<SystemDesignTrackerPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="planner" element={<PlannerPage />} />
        <Route path="calendar" element={<CalendarPage />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App
