import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear session storage before each test
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should call register endpoint', () => {
      const registerRequest = {
        email: 'test@example.com',
        username: 'testuser',
        password: 'password123'
      };

      service.register(registerRequest).subscribe();

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      req.flush(null);
    });
  });

  describe('login', () => {
    it('should call login endpoint and store token', () => {
      const loginRequest = {
        username: 'testuser',
        password: 'password123'
      };
      const loginResponse = {
        accessToken: 'test-token-123'
      };

      service.login(loginRequest).subscribe();

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginRequest);
      req.flush(loginResponse);

      // Verify token is stored
      expect(service.getToken()).toBe('test-token-123');
      expect(sessionStorage.getItem('access_token')).toBe('test-token-123');
    });

    it('should load current user after login', () => {
      const loginResponse = { accessToken: 'test-token' };
      const userResponse = {
        id: '123',
        email: 'test@example.com',
        username: 'testuser',
        role: 'USER'
      };

      service.login({ username: 'test', password: 'pass' }).subscribe();

      const loginReq = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
      loginReq.flush(loginResponse);

      const meReq = httpMock.expectOne(`${environment.apiBaseUrl}/auth/me`);
      meReq.flush(userResponse);

      service.currentUser$.subscribe(user => {
        expect(user).toEqual(userResponse);
      });
    });
  });

  describe('logout', () => {
    it('should clear token and current user', () => {
      // Set up token first
      sessionStorage.setItem('access_token', 'test-token');

      service.logout();

      expect(service.getToken()).toBeNull();
      expect(sessionStorage.getItem('access_token')).toBeNull();

      service.currentUser$.subscribe(user => {
        expect(user).toBeNull();
      });
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when token exists', () => {
      sessionStorage.setItem('access_token', 'test-token');
      expect(service.isAuthenticated()).toBe(true);
    });

    it('should return false when no token exists', () => {
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('getCurrentUser', () => {
    it('should call /me endpoint', () => {
      const userResponse = {
        id: '123',
        email: 'test@example.com',
        username: 'testuser',
        role: 'USER'
      };

      service.getCurrentUser().subscribe(user => {
        expect(user).toEqual(userResponse);
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/me`);
      expect(req.request.method).toBe('GET');
      req.flush(userResponse);
    });
  });

  describe('token storage', () => {
    it('should use in-memory storage first', () => {
      const token = 'test-token';
      sessionStorage.setItem('access_token', token);

      // Token should be retrieved from sessionStorage initially
      expect(service.getToken()).toBe(token);
    });

    it('should persist token in sessionStorage', () => {
      const loginResponse = { accessToken: 'test-token' };

      service.login({ username: 'test', password: 'pass' }).subscribe();

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
      req.flush(loginResponse);

      // Skip the /me request
      httpMock.expectOne(`${environment.apiBaseUrl}/auth/me`).flush({
        id: '1', email: 'test@test.com', username: 'test', role: 'USER'
      });

      expect(sessionStorage.getItem('access_token')).toBe('test-token');
    });
  });
});
