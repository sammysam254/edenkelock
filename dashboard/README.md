# Eden M-Kopa Dashboard

Next.js admin dashboard for managing devices, customers, and payments.

## Features

- Multi-tenant role-based access control
- Device enrollment and management
- Payment processing (M-Pesa, Cash, Crypto)
- Real-time analytics and reporting
- QR code generation for device provisioning

## Tech Stack

- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Supabase (Database + Auth)
- Recharts (Analytics)

## Getting Started

1. Install dependencies:
```bash
npm install
```

2. Set up environment variables:
```bash
cp .env.local.example .env.local
```

Edit `.env.local` with your Supabase credentials.

3. Run development server:
```bash
npm run dev
```

4. Open http://localhost:3000

## Project Structure

```
dashboard/
├── app/                    # Next.js App Router
│   ├── api/               # API routes
│   ├── devices/           # Devices page
│   ├── page.tsx           # Home page
│   ├── layout.tsx         # Root layout
│   └── globals.css        # Global styles
├── components/            # React components
├── lib/                   # Utilities
│   └── supabase.ts       # Supabase client
├── package.json
├── tsconfig.json
└── tailwind.config.ts
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm start` - Start production server
- `npm run lint` - Run ESLint

## Deployment

Deploy to Vercel:

```bash
vercel deploy --prod
```

Or connect your GitHub repo to Vercel for automatic deployments.

## Environment Variables

Required:
- `NEXT_PUBLIC_SUPABASE_URL` - Your Supabase project URL
- `NEXT_PUBLIC_SUPABASE_ANON_KEY` - Your Supabase anon key

## Authentication

Uses Supabase Auth with email/password. Row Level Security ensures users can only access their own data.

## License

MIT
