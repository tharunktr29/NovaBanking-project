import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { RouterProvider } from 'react-router-dom';
import { store } from './store';
import { router } from './router';
import './styles.css';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#2457d6'
    },
    secondary: {
      main: '#19a974'
    },
    background: {
      default: '#f4f7fb'
    }
  },
  typography: {
    fontFamily: ['Inter', 'Segoe UI', 'Roboto', 'Arial', 'sans-serif'].join(',')
  },
  shape: {
    borderRadius: 14
  }
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <RouterProvider router={router} />
      </ThemeProvider>
    </Provider>
  </React.StrictMode>
);
