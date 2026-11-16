import { Test, TestingModule } from '@nestjs/testing';
import { AppsService } from './apps.service';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';

describe('AppsService', () => {
  let service: AppsService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    appRestriction: { upsert: jest.fn(), findMany: jest.fn() },
    screenTimeSchedule: { update: jest.fn() },
  };

  const mockEventsGateway = {
    broadcastPolicyUpdate: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AppsService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: EventsGateway, useValue: mockEventsGateway },
      ],
    }).compile();

    service = module.get<AppsService>(AppsService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should classify known game packages as GAMES', async () => {
    mockPrisma.child.findUnique.mockResolvedValue({ id: 'child-1' });
    mockPrisma.appRestriction.upsert.mockResolvedValue({
      id: 'res-1',
      category: 'GAMES',
    });

    const res = await service.syncInstalledApps('child-1', {
      apps: [{ packageName: 'com.roblox.client', appName: 'Roblox' }],
    });

    expect(res.count).toBe(1);
    expect(mockPrisma.appRestriction.upsert).toHaveBeenCalledWith(
      expect.objectContaining({
        create: expect.objectContaining({ category: 'GAMES' }),
      }),
    );
  });
});
