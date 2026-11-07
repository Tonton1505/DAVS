import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#2E74B5',
        navy: '#1E3A5F',
        surface: '#F8FAFC',
        border: '#CBD5E1',
      },
    },
  },
  plugins: [],
} satisfies Config;
