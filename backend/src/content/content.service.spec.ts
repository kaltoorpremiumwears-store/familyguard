import { Test, TestingModule } from '@nestjs/testing';
import { ContentService } from './content.service';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';

describe('ContentService', () => {
  let service: ContentService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    webSafetyConfig: { upsert: jest.fn(), findUnique: jest.fn() },
    websiteRestriction: { findMany: jest.fn(), upsert: jest.fn(), delete: jest.fn() },
  };

  const mockEventsGateway = {
    broadcastPolicyUpdate: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ContentService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: EventsGateway, useValue: mockEventsGateway },
      ],
    }).compile();

    service = module.get<ContentService>(ContentService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should build valid Managed Chrome policy with SafeSearch and strict YouTube', () => {
    const config = {
      safeSearchEnabled: true,
      blockAdultContent: true,
      blockGambling: true,
    };
    const rules = [
      { domain: 'gambling.com', isBlocked: true },
      { domain: 'khanacademy.org', isBlocked: false },
    ];

    const policy = service.buildManagedChromePolicy(config, rules);

    expect(policy.packageName).toBe('com.android.chrome');
    expect(policy.restrictions.ForceGoogleSafeSearch).toBe(true);
    expect(policy.restrictions.ForceYouTubeRestrict).toBe(2);
    expect(policy.restrictions.URLBlocklist).toContain('gambling.com');
    expect(policy.restrictions.URLAllowlist).toContain('khanacademy.org');
  });
});
