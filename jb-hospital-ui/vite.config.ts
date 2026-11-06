import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite'; // 1. Import the official v4 Tailwind compiler plugin

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(), 
    tailwindcss() // 2. Add it straight into the assembly array right next to react()
  ],
  server: {
    port: 3000, 
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
});
