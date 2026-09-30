# Frontend — Design System & Figma Integration Rules

Rules for turning Figma designs (via the Figma MCP server) into code in `frontend/`.
Read this before implementing any design. The app is small and has **no formal design
system**: the "system" is a set of repeated Tailwind class recipes. Stay consistent with them.

---

## 1. Stack at a glance

| Concern | Choice | Where |
|---|---|---|
| UI framework | React 19 + TypeScript (strict-ish, `verbatimModuleSyntax`) | `package.json`, `tsconfig.app.json` |
| Routing | `react-router-dom` v7 (`BrowserRouter`, nested routes, `<Outlet />`) | `src/App.tsx` |
| Styling | **Tailwind CSS v4** via `@tailwindcss/vite`, utility classes only | `vite.config.ts`, `src/index.css` |
| HTTP | axios instance, same-origin `/api` + session cookie (HttpOnly) + XSRF header | `src/services/api.ts` |
| Build | Vite 8 (`tsc -b && vite build`), `vite-plugin-pwa` | `vite.config.ts` |
| Tests | Vitest + Testing Library (jsdom), Playwright e2e | `src/**/*.test.tsx`, `e2e/` |
| Deploy | Vercel (`vercel.json`) or Docker → nginx (`Dockerfile`, `nginx.conf`) | |

There is **no** UI kit (MUI, shadcn, Headless UI), **no** CSS-in-JS, **no** CSS Modules,
**no** Storybook, **no** icon library, and **no** `tailwind.config.*` file.
Don't add any of these unless the user asks.

---

## 2. Design tokens

### Where they live
Nowhere explicitly. `src/index.css` is one line:

```css
@import "tailwindcss";
```

There is no `@theme` block, no CSS variables, no token JSON, and no token pipeline
(Style Dictionary etc.). The tokens are **Tailwind v4's default palette and scale**,
used by convention. The only hard-coded hex values are the PWA/theme color `#2563eb`
(= `blue-600`) in `vite.config.ts` and `index.html`.

### The convention palette (by usage frequency in `src/`)

| Role | Classes |
|---|---|
| Brand / primary | `bg-blue-600`, hover `bg-blue-700`, focus `ring-blue-500`, links `text-blue-600` |
| App background | `bg-gray-50` |
| Surface (cards, tables, forms) | `bg-white` + `border border-gray-100` + `shadow-sm` |
| Input border | `border-gray-200` |
| Heading text | `text-gray-800` (h1/h2), `text-gray-700` (section titles) |
| Body / secondary text | `text-gray-600`, `text-gray-500` |
| Muted / meta text, table headers | `text-gray-400` |
| Sidebar (dark) | `bg-slate-900`, `bg-slate-800` (hover/active chip), `border-slate-700`, `text-slate-400` |
| Error | `bg-red-50 border-red-100 text-red-600`, action `text-red-700` |
| Success | `text-green-600`, badge `bg-green-100 text-green-700` |
| Warning / pending | badge `bg-yellow-100 text-yellow-700`; action `text-amber-600` |
| Neutral / secondary button | `bg-gray-100 text-gray-600 hover:bg-gray-200` |

**Typography:** system font stack (Tailwind default; no web fonts). Page title
`text-xl font-bold text-gray-800`; section title `font-semibold text-gray-700`; body
`text-sm`; labels `text-xs font-medium text-gray-500`; meta `text-xs text-gray-400`;
IDs/codes `font-mono`; stat numbers `text-3xl font-bold`.

**Radius:** `rounded-lg` for inputs/buttons/alerts, `rounded-xl` for cards and tables,
`rounded-2xl` for standalone auth/modal panels, `rounded-full` for badges and avatars.

**Spacing:** page padding `p-4 sm:p-6`; card padding `p-5` (modal `p-6`, auth card `p-8`);
table cells `px-5 py-3`; section gap `mb-6`; grid gap `gap-4`.

### Figma → token mapping rules
- Map every Figma color to the **nearest Tailwind default class** from the table above.
  Don't write hex values, `bg-[#...]` arbitrary values, or new CSS variables unless the
  design introduces a color that is clearly intentional and has no Tailwind equivalent.
  If that happens, ask first. If approved, add it as a token in `src/index.css`:
  ```css
  @import "tailwindcss";
  @theme {
    --color-brand-600: #2563eb;   /* becomes bg-brand-600, text-brand-600, … */
  }
  ```
- Snap Figma spacing/sizes to the Tailwind 4px scale (`14px → 3.5`, `20px → 5`).
- If `get_variable_defs` returns Figma variables, map them to the roles above. Don't mirror
  them 1:1 as new tokens.

---

## 3. Component library

```
src/
├── components/          # shared, reusable UI (one default-exported component per file)
│   ├── Layout.tsx         # app shell: Sidebar + mobile top bar + <Outlet/>
│   ├── Sidebar.tsx        # dark nav, role-dependent items, user chip, sign-out
│   ├── StatCard.tsx       # KPI tile with a constrained `color` prop
│   ├── (no modals currently)
│   ├── ProtectedRoute.tsx # auth/role guard (no visual except spinner)
│   └── ErrorBoundary.tsx  # class component, fallback card
├── pages/               # one file per route; each owns its data fetching + local state
├── context/             # AuthContext.tsx (provider), auth-context.ts, useAuth.ts
├── services/            # api.ts (axios) + one `xxxService` object per resource
├── types/index.ts       # all shared TS types (domain models, request DTOs, unions)
└── test/setup.ts        # jest-dom + cleanup
```

**Architecture:** function components, typed props via a local `interface XxxProps`,
`export default function Xxx(...)`. No compound components, no `forwardRef`, no
`className` passthrough props, and no variants library (`cva`, `clsx`). Variants are
plain `Record<Union, string>` maps:

```tsx
// src/components/StatCard.tsx — the variant pattern to copy
interface StatCardProps {
  label: string; value: number; sub: string;
  color: 'blue' | 'green' | 'orange' | 'red';
}
const colorMap: Record<StatCardProps['color'], string> = {
  blue: 'text-blue-600', green: 'text-green-600',
  orange: 'text-orange-500', red: 'text-red-500',
};
```

Class names are composed with template literals, not a helper:

```tsx
className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
  isActive ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white hover:bg-slate-800'
}`}
```

**Documentation:** there is no Storybook. Colocated `*.test.tsx` files (e.g.
`StatCard.test.tsx`) are the de facto usage docs.

### Recurring UI recipes (inlined in pages, not yet extracted)
Reuse these exact strings so new screens match. Extract a component only when the user
asks or the same recipe would appear in a third new place.

```tsx
// Primary button
"bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors"
// + disabled: "disabled:opacity-50 disabled:cursor-not-allowed"
// Secondary button
"bg-gray-100 text-gray-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-200"
// Pagination / outline button
"px-3 py-1.5 text-xs font-medium rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
// Row text actions
"text-blue-600 hover:text-blue-800 text-xs font-medium"   // Edit
"text-red-500 hover:text-red-700 text-xs font-medium"     // Delete
"text-green-600 hover:text-green-800 text-xs font-medium" // Positive action

// Label + input
<label className="block text-xs font-medium text-gray-500 mb-1">First Name</label>
<input className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />

// Card / table container
"bg-white rounded-xl shadow-sm border border-gray-100"            // + p-5 for cards
"bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto" // tables

// Table
<table className="w-full text-sm min-w-[560px]">
  <thead className="bg-gray-50 text-gray-400 text-xs uppercase"> … <th className="px-5 py-3 text-left">
  <tbody className="divide-y divide-gray-50"> … <tr className="hover:bg-gray-50"> … <td className="px-5 py-3">
// Empty row: <td colSpan={n} className="px-5 py-8 text-center text-gray-400 text-sm">

// Status badge (map lives in the page as Record<Status, string>)
<span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor[e.status]}`}>
// CONFIRMED/ACTIVE: bg-green-100 text-green-700 · PENDING: bg-yellow-100 text-yellow-700
// CANCELLED/ARCHIVED: bg-red-100 text-red-600 · INACTIVE: bg-gray-100 text-gray-600

// Error alert (with optional Retry/Dismiss action on the right)
"bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg flex items-center justify-between gap-4"

// Loading spinner
<div className="flex items-center justify-center h-64">
  <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
</div>

// Modal
<div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 w-full max-w-md">

// Page header
<div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-6">
  <div><h1 className="text-xl font-bold text-gray-800">Students</h1>
       <p className="text-sm text-gray-400">{n} of {total}</p></div>
  …actions
</div>
```

**Z-index layers:** mobile top bar `z-20`, sidebar backdrop `z-30`, sidebar `z-40`,
modal `z-50`.

### Page structure contract
Each page component follows the same shape. Keep it when you implement a new Figma screen:
1. `useState` for data, `loading`, `loadError`, form state, and the form `error`.
2. `useCallback` loader + `useEffect(() => { void load(); }, [load])` with the existing
   `// eslint-disable-next-line react-hooks/set-state-in-effect` comment.
3. Early-return the spinner, then early-return the error alert with a Retry button.
4. Render inside `<div className="p-4 sm:p-6">`.
5. Call the backend only through `src/services/*Service.ts` and show backend errors with
   `getErrorMessage(err, 'fallback')` from `src/services/errorMessage.ts`.
6. Add new domain types to `src/types/index.ts`. Wire routes in `src/App.tsx` (wrap them in
   `<ProtectedRoute roles={[…]}>` when the backend restricts the endpoint) and add nav items
   to the role-specific arrays in `Sidebar.tsx`.

---

## 4. Assets

- `public/` holds files served verbatim at the root: `favicon.svg`, the PWA PNGs, and
  `apple-touch-icon.png`. Reference them by absolute path (`/favicon.svg`).
- `src/assets/` is for bundled assets imported from code (`import img from '../assets/x.png'`).
  Vite hashes and fingerprints them.
- PWA icons are **generated** from `public/favicon.svg` by `node scripts/generate-pwa-icons.mjs`
  (sharp). Re-run the script when the logo changes; don't hand-edit the PNGs.
- Optimization: only what Vite does by default (hashing, inlining small assets). There is no
  image pipeline and no CDN. Everything is served same-origin.
- `src/assets/hero.png`, `public/icons.svg` (social sprites), and the purple Vite-style
  `public/favicon.svg` are **leftovers from the Vite template** and aren't used in `src/`.
  Don't treat them as brand assets. The in-app logo is the text mark (see §5).

### ⚠️ CSP constraints (they affect Figma exports)
`vercel.json` and `nginx.conf` set a strict CSP:
`default-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'`.

- **Never** reference Figma MCP asset URLs (`http://localhost:3845/...`) or any remote image
  in shipped code. Download the asset into `src/assets/` (or `public/`) and import it.
- **No** Google Fonts or other remote fonts or stylesheets. If a design needs a custom font,
  self-host it under `src/assets/fonts/` and declare it in `index.css` with `@font-face`.
- If you add an external origin, update **both** `vercel.json` and `nginx.conf`.

---

## 5. Icons

There is **no icon system**. The existing icons are **emoji and Unicode glyphs** inside a `<span>`:

```tsx
// src/components/Sidebar.tsx
{ path: '/students', label: 'Students', icon: '👤' },
{ path: '/courses',  label: 'Courses',  icon: '📚' },
// Layout.tsx mobile menu button: ☰  (with aria-label="Open menu")
```

The logo is a text mark, not an image:

```tsx
<div className="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-sm">SM</div>
```

Avatars are the user's first initial in a `rounded-full bg-blue-600` circle.

**When a Figma design contains vector icons:**
- For one or two icons, export them as SVG and inline them as small components in
  `src/components/icons/<PascalName>Icon.tsx`. Use `fill="currentColor"` or
  `stroke="currentColor"`, size them with `w-* h-*` classes, and add
  `aria-hidden="true"`. Icon-only buttons get an `aria-label`.
- For many icons, don't pull in `lucide-react` or `heroicons` on your own; ask the user first.
- Naming: `PascalCase` + `Icon` suffix (`CourseIcon`, `MenuIcon`).

---

## 6. Styling approach

- **Tailwind utility classes inline in JSX.** No CSS Modules, no styled-components, no `@apply`.
- **Global styles:** only `src/index.css` (`@import "tailwindcss";`), which provides Preflight.
  Add global rules there only when a utility can't express them.
- **No `style={{…}}`** in the codebase. Keep it that way.
- **No dark mode.** The only dark surface is the sidebar. Ignore dark variants in Figma
  unless asked.
- **Responsive design is mobile-first, with `sm:` and `md:` as the breakpoints in use:**
  - `md:` (768px) is the shell breakpoint. Below it the sidebar becomes an off-canvas drawer
    (`-translate-x-full` → `md:translate-x-0 md:static`) with a backdrop and a sticky top bar.
  - `sm:` (640px) is the content breakpoint: `p-4 sm:p-6`, `flex-col sm:flex-row`,
    `grid-cols-1 sm:grid-cols-2`, `grid-cols-2 sm:grid-cols-4` (stat cards), `sm:w-72` (search).
  - Tables scroll horizontally on mobile (`overflow-x-auto` + `min-w-[560px|600px]`).
  - Figma usually provides desktop frames only. Derive the mobile layout with these same rules.
- Transitions: `transition-colors` on interactive elements, `transition-transform duration-200
  ease-in-out` for the drawer. No other animation except `animate-spin`.

---

## 7. Figma MCP workflow

1. `get_design_context` for the node (and `get_screenshot` for visual reference). If the
   response is truncated, use `get_metadata` first and fetch child nodes one at a time.
2. Treat the returned React + Tailwind code as a **description of the design, not final
   code**. Rewrite it into this project's conventions:
   - Replace arbitrary values (`text-[#1E293B]`, `p-[18px]`, `rounded-[10px]`) with the
     palette, scale, and radius conventions in §2 and §3.
   - Replace absolute positioning with flex or grid layouts.
   - Reuse `StatCard`, `Layout`/`Sidebar`, and the recipes in §3
     instead of re-creating them.
   - Use semantic elements (`button`, `label`, `table`, `nav`, `main`, `h1`/`h2`).
3. Download image and SVG assets locally (§4 CSP). Never ship localhost asset URLs.
4. Put new screens in `src/pages/`, new shared UI in `src/components/`, and types in
   `src/types/index.ts`, following the page contract in §3.
5. Check visual parity against the screenshot at desktop width and at a phone width below `sm`.
6. Verify with `npm run lint`, `npm run build` (runs `tsc -b`), and `npm test`. Add a
   colocated `*.test.tsx` for new shared components, in the style of `StatCard.test.tsx`.

### Known inconsistencies (don't copy them)
- The Dashboard's CANCELLED badge uses `text-red-700`, but the other pages use `text-red-600`.
  Prefer `text-red-600`.
- `statusColor` maps are duplicated across `CoursesPage`, `EnrollmentsPage`, and
  `MyCoursesPage`. If you touch several of them, consider extracting a `StatusBadge` component.
