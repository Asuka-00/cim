const local: App.I18n.Schema['page']['layout'] = {
  forceLogout: {
    title: 'Signed Out',
    defaultReason: 'Your account has signed in from another location'
  },
  permissionRefreshed: 'Permissions refreshed',
  permissionRefreshFailed: 'Failed to refresh permissions',
  oauth2: {
    failed: 'OAuth2 login failed: {error}',
    missingTicket: 'Missing OAuth2 ticket',
    exchangeFailed: 'OAuth2 ticket exchange failed',
    backToLogin: 'Back to login'
  },
  platformAdmin: 'Platform Admin',
  switchTenant: 'Switch Tenant'
};

export default local;
