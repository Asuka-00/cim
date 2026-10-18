declare namespace Api {
  /**
   * namespace Auth
   *
   * backend api module: "auth"
   */
  namespace Auth {
    /** Backend `LoginVO` returned by login / refresh / OAuth2 exchange */
    interface LoginVO {
      accessToken: string;
      refreshToken: string;
      accessExpireSeconds?: number;
      userName?: string;
      displayName?: string;
      tenantId?: string;
      platformAdmin?: boolean;
      tenants?: TenantSelect[];
    }

    interface LoginToken {
      token: string;
      refreshToken: string;
      accessExpireSeconds?: number;
      userName?: string;
      displayName?: string;
      tenantId?: string;
      platformAdmin?: boolean;
      tenants?: TenantSelect[];
    }

    interface LoginRequest {
      userName: string;
      password: string;
      tenantId: string;
    }

    interface UserInfo {
      userId: string;
      userName: string;
      displayName?: string;
      roles: string[];
      buttons: string[];
    }

    interface BrandPublic {
      title?: string | null;
      poweredBy?: string | null;
      logoSvg?: string | null;
      faviconSvg?: string | null;
      loginTitleSvg?: string | null;
      loginBgSvg?: string | null;
      showLoginTitleImage?: boolean | null;
    }

    interface TenantSelect {
      id: string;
      tenantId: string;
      tenantName: string;
    }

    interface OAuth2Provider {
      registrationId: string;
      displayName: string;
    }
  }
}
