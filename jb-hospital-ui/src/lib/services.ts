import api from './api';
import type {
  LabOrder, LabOrderStatus, LabResultItem, LabTest, NewPatientRequest, NewVisitRequest, PatientRegistered,
  PatientSummary, QueueItem, ResultEntry, Role, StaffSummary, TestParameter, VisitEvent, VisitLabResults,
  VisitStatus, VitalsRecord, VitalsRequest,
} from './types';

export interface LoginResult {
  accessToken: string;
  username: string;
  fullName: string;
  role: Role;
}

export const AuthApi = {
  login: (username: string, password: string) =>
    api.post<LoginResult>('/api/v1/auth/login', { username, password }).then((r) => r.data),
  doctors: () => api.get<StaffSummary[]>('/api/v1/auth/doctors').then((r) => r.data),
  register: (body: Record<string, unknown>) => api.post('/api/v1/auth/register', body).then((r) => r.data),
};

export const OpdApi = {
  registerPatient: (body: NewPatientRequest) =>
    api.post<PatientRegistered>('/api/v1/opd/patients', body).then((r) => r.data),
  searchPatients: (q: string) =>
    api.get<PatientSummary[]>('/api/v1/opd/patients', { params: { q } }).then((r) => r.data),
  openVisit: (body: NewVisitRequest) => api.post<QueueItem>('/api/v1/opd/visits', body).then((r) => r.data),

  visit: (id: number) => api.get<QueueItem>(`/api/v1/opd/visits/${id}`).then((r) => r.data),
  events: (id: number) => api.get<VisitEvent[]>(`/api/v1/opd/visits/${id}/events`).then((r) => r.data),
  today: () => api.get<QueueItem[]>('/api/v1/opd/visits/today').then((r) => r.data),
  triageQueue: () => api.get<QueueItem[]>('/api/v1/opd/visits/triage-queue').then((r) => r.data),

  saveVitals: (body: VitalsRequest) => api.post<VitalsRecord>('/api/v1/opd/triage/vitals', body).then((r) => r.data),
  vitalsHistory: (visitId: number) =>
    api.get<VitalsRecord[]>(`/api/v1/opd/triage/vitals/visit/${visitId}`).then((r) => r.data),

  myQueue: (statuses: VisitStatus[]) =>
    api
      .get<QueueItem[]>('/api/v1/opd/visits/doctor/queue', {
        params: { status: statuses },
        paramsSerializer: { indexes: null }, // status=A&status=B
      })
      .then((r) => r.data),
  refreshLabStatuses: () => api.post('/api/v1/opd/visits/lab-status/refresh'),
  startConsultation: (id: number) => api.post(`/api/v1/opd/visits/${id}/start-consultation`),
  sendToLab: (id: number, testCodes: string[], clinicalNotes?: string) =>
    api.post(`/api/v1/opd/visits/${id}/send-to-lab`, { testCodes, clinicalNotes }),
  conclude: (id: number, diagnosis: string, consultationNotes: string) =>
    api.post(`/api/v1/opd/visits/${id}/conclude`, { diagnosis, consultationNotes }),
  labResults: async (id: number): Promise<VisitLabResults | null> => {
    const r = await api.get<VisitLabResults>(`/api/v1/opd/visits/${id}/lab-results`);
    return r.status === 204 ? null : r.data;
  },
  /** Opens the PDF in a new tab (download fallback when pop-ups are blocked). */
  openLabReport: async (id: number) => {
    const win = window.open('', '_blank');
    try {
      const r = await api.get(`/api/v1/opd/visits/${id}/lab-report`, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([r.data], { type: 'application/pdf' }));
      if (win) win.location.href = url;
      else Object.assign(document.createElement('a'), { href: url, download: `lab-report-${id}.pdf` }).click();
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      win?.close();
      throw e;
    }
  },
};

export const LabApi = {
  catalogue: () => api.get<LabTest[]>('/api/v1/lab/tests').then((r) => r.data),
  queue: (statuses?: LabOrderStatus[]) =>
    api
      .get<LabOrder[]>('/api/v1/lab/orders/queue', { params: { status: statuses }, paramsSerializer: { indexes: null } })
      .then((r) => r.data),
  collectSample: (id: number, testCode: string) =>
    api.post<LabOrder>(`/api/v1/lab/orders/${id}/tests/${encodeURIComponent(testCode)}/collect`).then((r) => r.data),
  setStatus: (id: number, status: LabOrderStatus) =>
    api.patch<LabOrder>(`/api/v1/lab/orders/${id}/status`, { status }).then((r) => r.data),
  parameters: (codes: string[]) =>
    api.get<TestParameter[]>('/api/v1/lab/tests/parameters', { params: { codes: codes.join(',') } }).then((r) => r.data),
  submitResults: (orderId: number, results: ResultEntry[]) =>
    api.post<LabResultItem[]>(`/api/v1/lab/orders/${orderId}/results`, { results }).then((r) => r.data),
};
