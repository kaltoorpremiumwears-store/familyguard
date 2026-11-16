import { Test, TestingModule } from '@nestjs/testing';
import { LocationService } from './location.service';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';
import { NotificationsService } from '../notifications/notifications.service';

describe('LocationService', () => {
  let service: LocationService;

  const mockPrisma = {
    child: { findUnique: jest.fn() },
    locationRecord: { create: jest.fn(), findFirst: jest.fn(), findMany: jest.fn() },
    device: { update: jest.fn() },
    geofenceZone: { create: jest.fn(), findMany: jest.fn(), delete: jest.fn() },
  };

  const mockEventsGateway = {
    server: { to: jest.fn().mockReturnValue({ emit: jest.fn() }) },
  };

  const mockNotificationsService = {
    createNotification: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        LocationService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: EventsGateway, useValue: mockEventsGateway },
        { provide: NotificationsService, useValue: mockNotificationsService },
      ],
    }).compile();

    service = module.get<LocationService>(LocationService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should record location and emit real-time event', async () => {
    mockPrisma.child.findUnique.mockResolvedValue({
      id: 'child-1',
      parentId: 'parent-1',
      geofences: [],
    });
    mockPrisma.locationRecord.create.mockResolvedValue({
      id: 'rec-1',
      latitude: 37.7749,
      longitude: -122.4194,
      recordedAt: new Date(),
    });

    const res = await service.reportLocation('child-1', {
      deviceId: 'dev-1',
      latitude: 37.7749,
      longitude: -122.4194,
      accuracyMeters: 10,
      batteryPercentage: 85,
    });

    expect(res.status).toBe('location_recorded');
    expect(mockEventsGateway.server.to).toHaveBeenCalledWith('parent_parent-1');
  });

  it('should trigger notification when child enters a geofence', async () => {
    mockPrisma.child.findUnique.mockResolvedValue({
      id: 'child-1',
      name: 'Alex',
      parentId: 'parent-1',
      geofences: [
        {
          id: 'zone-1',
          name: 'Home',
          latitude: 37.7749,
          longitude: -122.4194,
          radiusMeters: 500,
          notifyOnEnter: true,
        },
      ],
    });
    mockPrisma.locationRecord.create.mockResolvedValue({ recordedAt: new Date() });

    await service.reportLocation('child-1', {
      deviceId: 'dev-1',
      latitude: 37.7749,
      longitude: -122.4194,
    });

    expect(mockNotificationsService.createNotification).toHaveBeenCalledWith(
      'parent-1',
      'child-1',
      'LOCATION_GEOFENCE',
      expect.stringContaining('entered Home'),
      expect.any(String),
      expect.any(Object),
    );
  });
});
