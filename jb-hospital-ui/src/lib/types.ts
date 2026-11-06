export type Role = 'ADMIN' | 'NURSE' | 'DOCTOR' | 'LABTECH';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type Priority = 'NORMAL' | 'URGENT' | 'EMERGENCY';
export type VisitStatus =
  | 'REGISTERED' | 'TRIAGED' | 'IN_CONSULTATION' | 'AWAITING_LAB'
  | 'LAB_RESULTS_READY' | 'COMPLETED' | 'CANCELLED';
export type LabOrderStatus = 'PLACED' | 'SAMPLE_COLLECTED' | 'COMPLETED' | 'CANCELLED';

export interface SessionUser {
  username: string;
  fullName: string;
  role: Role;
}

export interface StaffSummary {
  id: string;
  username: string;
  fullName: string;
  role: Role;
  department?: string | null;
  specialization?: string | null;
}

/** One row of any OPD worklist (triage, doctor queue, today's register). */
export interface QueueItem {
  visitId: number;
  patientMrn: string;
  patientFullName: string;
  tokenNumber: number;
  chiefComplaint?: string | null;
  priority: Priority;
  status: VisitStatus;
  patientAge?: number | null;
  patientGender?: Gender | null;
  patientBloodGroup?: string | null;
  patientAllergies?: string | null;
  assignedDoctorUsername?: string | null;
  assignedDoctorName?: string | null;
  registeredTime?: string | null;
  diagnosis?: string | null;
  temperatureCelsius?: number | null;
  pulseRate?: number | null;
  bloodPressure?: string | null;
  oxygenSaturation?: number | null;
  symptomsBrief?: string | null;
}

export interface PatientSummary {
  id: number;
  mrn: string;
  fullName: string;
  dateOfBirth: string;
  dobEstimated: boolean;
  gender: Gender;
  phone: string;
  bloodGroup?: string | null;
  allergies?: string | null;
  hasActiveVisit: boolean;
}

export interface NewPatientRequest {
  fullName: string;
  dateOfBirth: string;
  dobEstimated: boolean;
  gender: Gender;
  phone: string;
  address?: string;
  bloodGroup?: string;
  allergies?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  chiefComplaint: string;
  priority: Priority;
  assignedDoctorUsername: string;
}

export interface NewVisitRequest {
  patientMrn: string;
  chiefComplaint: string;
  priority: Priority;
  assignedDoctorUsername: string;
}

export interface PatientRegistered {
  id: number;
  visitId: number;
  tokenNumber: number;
  mrn: string;
  fullName: string;
}

export interface VitalsRequest {
  visitId: number;
  temperatureCelsius: number;
  pulseRate: number;
  bloodPressure: string;
  oxygenSaturation: number;
  symptomsBrief?: string;
}

export interface VitalsRecord extends VitalsRequest {
  id: number;
  patientName: string;
  recordedBy: string;
  recordedAt: string;
}

export interface VisitEvent {
  id: number;
  visitId: number;
  fromStatus?: VisitStatus | null;
  toStatus: VisitStatus;
  actor: string;
  note?: string | null;
  occurredAt: string;
}

export interface LabTest {
  testCode: string;
  testName: string;
  department: string;
  cost: number;
}

export interface LabSample {
  testCode: string;
  collected: boolean;
  collectedBy?: string | null;
  collectedAt?: string | null;
}

export interface LabOrder {
  id: number;
  visitId: number;
  patientMrn: string;
  patientName?: string | null;
  orderedByDoctor: string;
  orderStatus: LabOrderStatus;
  clinicalHistory?: string | null;
  orderedAt: string;
  testCodes: string[];
  /** One per ordered test; results can be entered only when all are collected. */
  samples: LabSample[];
}

export interface TestParameter {
  testCode: string;
  testName: string;
  parameterName: string;
  unit?: string | null;
  refLow?: number | null;
  refHigh?: number | null;
}

export interface ResultEntry {
  testCode: string;
  parameterName: string;
  observedValue: string;
  abnormal?: boolean;
  technicianNotes?: string;
}

export interface LabResultItem {
  id: number;
  orderId: number;
  testCode: string;
  testName: string;
  parameterName: string;
  observedValue: string;
  referenceRange?: string | null;
  abnormal: boolean;
  technicianNotes?: string | null;
  resultEnteredBy: string;
  resultEnteredAt: string;
}

export interface VisitLabResults {
  orderId: number;
  orderStatus: LabOrderStatus;
  orderedByDoctor: string;
  orderedAt: string;
  testCodes: string[];
  results: LabResultItem[];
}
