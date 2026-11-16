import { Test, TestingModule } from '@nestjs/testing';
import { YouTubeService } from './youtube.service';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';

describe('YouTubeService', () => {
  let service: YouTubeService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    youTubeRestriction: { findMany: jest.fn(), findUnique: jest.fn(), upsert: jest.fn(), deleteMany: jest.fn() },
    webSafetyConfig: { findUnique: jest.fn() },
  };

  const mockEventsGateway = {
    broadcastPolicyUpdate: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        YouTubeService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: EventsGateway, useValue: mockEventsGateway },
      ],
    }).compile();

    service = module.get<YouTubeService>(YouTubeService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should return curated educational channels list', () => {
    const channels = service.getCuratedEducationalChannels();
    expect(channels.length).toBeGreaterThanOrEqual(4);
    expect(channels.some((c) => c.title.includes('NASA'))).toBe(true);
  });

  it('should add item to approved YouTube library', async () => {
    mockPrisma.child.findUnique.mockResolvedValue({ id: 'child-1', parentId: 'parent-1' });
    mockPrisma.youTubeRestriction.findUnique.mockResolvedValue(null);
    mockPrisma.youTubeRestriction.upsert.mockResolvedValue({
      id: 'item-1',
      title: 'Crash Course Kids',
      isWhitelisted: true,
    });

    const res = await service.addApprovedItem('parent-1', 'child-1', {
      type: 'CHANNEL',
      externalId: 'UCXVCgDuD_XCkHYn-v95i75Q',
      title: 'Crash Course Kids',
    });

    expect(res.item.isWhitelisted).toBe(true);
    expect(mockEventsGateway.broadcastPolicyUpdate).toHaveBeenCalled();
  });
});
