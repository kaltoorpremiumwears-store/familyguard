import { Test, TestingModule } from '@nestjs/testing';
import { TelemetryService } from './telemetry.service';
import { PrismaService } from '../prisma/prisma.service';

describe('TelemetryService', () => {
  let service: TelemetryService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    device: { findUnique: jest.fn() },
    activityRecord: { create: jest.fn(), findMany: jest.fn() },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TelemetryService,
        { provide: PrismaService, useValue: mockPrisma },
      ],
    }).compile();

    service = module.get<TelemetryService>(TelemetryService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should compute daily category breakdown correctly', async () => {
    mockPrisma.child.findUnique.mockResolvedValue({
      id: 'child-1',
      parentId: 'parent-1',
      name: 'Alex',
      screenTimeSchedule: { dailyLimitMinutes: 120 },
    });

    mockPrisma.activityRecord.findMany.mockResolvedValue([
      { packageName: 'com.roblox.client', appName: 'Roblox', category: 'GAMES', durationSeconds: 3600 },
      { packageName: 'com.duolingo', appName: 'Duolingo', category: 'EDUCATION', durationSeconds: 1800 },
    ]);

    const report = await service.getDailyReport('parent-1', 'child-1', '2026-11-14');

    expect(report.totalScreenTimeMinutes).toBe(90);
    expect(report.categories.gamesMinutes).toBe(60);
    expect(report.categories.educationMinutes).toBe(30);
    expect(report.topApps.length).toBe(2);
  });
});
