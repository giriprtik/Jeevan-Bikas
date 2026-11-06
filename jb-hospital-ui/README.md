# jb-hospital-ui

React 19 + TypeScript + Vite + Tailwind CSS v4 front end for the Jeevan Bikas OPD system.

```bash
npm ci
npm run dev      # http://localhost:3000, proxies /api to the gateway on :8080
npm run build    # type-check + production bundle in dist/
npm run lint     # oxlint
```

## Layout

| Path | What lives there |
|---|---|
| `src/lib/api.ts` | Axios instance (same-origin `/api`), JWT header, 401 handling, `apiError()` for problem+json |
| `src/lib/services.ts` | One typed function per backend endpoint (`AuthApi`, `OpdApi`, `LabApi`) |
| `src/lib/types.ts` | DTO types mirroring the Spring responses |
| `src/lib/format.ts` | Labels and clinical rules (vital-sign flags) shared across screens |
| `src/components/ui` | Design-system primitives: Button, Card, Field, Badge, Alert… |
| `src/components/AppShell.tsx` | Sidebar + top bar; menu is filtered by role |
| `src/hooks/usePolling.ts` | Load-then-refresh hook used by every live worklist |
| `src/pages/*` | One folder per workspace: Registration, Triage, Doctor, Lab, Staff |

Design tokens (colours, fonts, shadows) are defined once in `src/index.css` under `@theme`.
