import { Test, TestingModule } from '@nestjs/testing';
import { EnrollmentService } from './enrollment.service';
import { PrismaService } from '../prisma/prisma.service';

describe('EnrollmentService', () => {
  let service: EnrollmentService;

  const mockPrisma = {
    device: { findUnique: jest.fn(), update: jest.fn() },
    auditLog: { create: jest.fn() },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        EnrollmentService,
        { provide: PrismaService, useValue: mockPrisma },
      ],
    }).compile();

    service = module.get<EnrollmentService>(EnrollmentService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should generate valid Android Enterprise QR payload with correct DPC component', async () => {
    const result = await service.generateAndroidEnterpriseQrPayload('FAM-TEST12');
    expect(result.rawPayload).toContain('org.familyguard.child/.dpc.ParentalDpcReceiver');
    expect(result.config['android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED']).toBe(true);
  });

  it('should provide ADB shell provisioning commands', () => {
    const adb = service.getAdbProvisioningInstructions();
    expect(adb.powershellCommand).toContain('dpm set-device-owner');
  });
});
