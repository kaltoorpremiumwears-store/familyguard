import { Test, TestingModule } from '@nestjs/testing';
import { PairingService } from './pairing.service';
import { PrismaService } from '../prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';
import { NotFoundException } from '@nestjs/common';

describe('PairingService', () => {
  let service: PairingService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    devicePairing: { updateMany: jest.fn(), create: jest.fn(), findFirst: jest.fn(), update: jest.fn() },
    device: { create: jest.fn() },
  };

  const mockJwt = {
    sign: jest.fn().mockReturnValue('mocked.device.token'),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PairingService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: JwtService, useValue: mockJwt },
      ],
    }).compile();

    service = module.get<PairingService>(PairingService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should throw NotFoundException if child profile is missing during pairing generation', async () => {
    mockPrisma.child.findUnique.mockResolvedValue(null);

    await expect(
      service.generatePairing('parent-1', { childId: 'non-existent-child' }),
    ).rejects.toThrow(NotFoundException);
  });
});
