import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';
import { PrismaService } from '../prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';
import { BadRequestException } from '@nestjs/common';

describe('AuthService', () => {
  let service: AuthService;
  let prisma: PrismaService;

  const mockPrisma = {
    user: {
      findUnique: jest.fn(),
      create: jest.fn(),
      delete: jest.fn(),
    },
    auditLog: {
      create: jest.fn(),
    },
  };

  const mockJwt = {
    sign: jest.fn().mockReturnValue('mocked.jwt.token'),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: JwtService, useValue: mockJwt },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    prisma = module.get<PrismaService>(PrismaService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should reject registration if Verifiable Parental Consent is false', async () => {
    await expect(
      service.register({
        email: 'parent@example.com',
        password: 'Password123!',
        fullName: 'Jane Doe',
        hasConsentedToParentalTerms: false,
      }),
    ).rejects.toThrow(BadRequestException);
  });

  it('should export all user data in compliance with GDPR Article 20', async () => {
    mockPrisma.user.findUnique.mockResolvedValue({
      id: 'usr_1',
      email: 'parent@example.com',
      fullName: 'Jane Doe',
      parentProfile: { children: [] },
    });
    mockPrisma.auditLog.create.mockResolvedValue({});

    const result = await service.exportData('usr_1');
    expect(result.userData.id).toBe('usr_1');
    expect(result.complianceStatement).toBeDefined();
    expect(mockPrisma.auditLog.create).toHaveBeenCalled();
  });

  it('should delete user and all family data in compliance with GDPR Article 17', async () => {
    mockPrisma.user.findUnique.mockResolvedValue({
      id: 'usr_1',
      email: 'parent@example.com',
    });
    mockPrisma.user.delete.mockResolvedValue({ id: 'usr_1' });

    const result = await service.deleteAccount('usr_1');
    expect(result.success).toBe(true);
    expect(mockPrisma.user.delete).toHaveBeenCalledWith({ where: { id: 'usr_1' } });
  });
});
