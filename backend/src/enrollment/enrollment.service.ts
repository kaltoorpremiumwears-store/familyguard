import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { DeviceIntegrityReportDto } from './enrollment.dto';

@Injectable()
export class EnrollmentService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Generates Google Android Enterprise QR Code Provisioning Payload.
   * This is scanned by tapping 6 times on the Android Setup Wizard welcome screen on a factory-reset device.
   */
  async generateAndroidEnterpriseQrPayload(familyCode: string, serverUrl?: string) {
    const backendEndpoint = serverUrl || process.env.BACKEND_PUBLIC_URL || 'http://10.0.2.2:3000';

    const qrConfig = {
      'android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME':
        'org.familyguard.child/.dpc.ParentalDpcReceiver',
      'android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME': 'org.familyguard.child',
      'android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED': true,
      'android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE': {
        server_url: backendEndpoint,
        family_code: familyCode,
        enrollment_mode: 'ENTERPRISE_DEVICE_OWNER',
      },
    };

    return {
      title: 'Android Enterprise 6-Tap QR Provisioning',
      description:
        'Factory reset target Android device. On the Welcome/Hello screen, tap 6 times in the same empty space to launch the camera, then scan this QR payload.',
      format: 'application/json',
      rawPayload: JSON.stringify(qrConfig),
      config: qrConfig,
    };
  }

  /**
   * Generates the developer/ADB provisioning script for fast testing without factory reset.
   */
  getAdbProvisioningInstructions() {
    return {
      method: 'ADB Shell DPC Provisioning (Zero-Cost / Test Mode)',
      prerequisites: [
        'Device connected via USB with USB Debugging enabled.',
        'No Google Accounts or user accounts registered on device (Settings -> Accounts -> Remove).',
        'Child App APK installed via: adb install child_app-debug.apk',
      ],
      powershellCommand:
        'adb shell dpm set-device-owner org.familyguard.child/.dpc.ParentalDpcReceiver',
      bashCommand:
        'adb shell dpm set-device-owner org.familyguard.child/.dpc.ParentalDpcReceiver',
      expectedOutput: 'Success: Device owner set to package ComponentInfo{org.familyguard.child/org.familyguard.child.dpc.ParentalDpcReceiver}',
      troubleshooting: {
        accountsError:
          'If you see "Not allowed to set the device owner because there are already some accounts on the device", remove all Google/Samsung accounts from Settings -> Accounts, or run on an Android Emulator.',
        alreadyUserError:
          'If you see "Not allowed to set the device owner because there are already some users", run: adb shell pm list users and ensure only User 0 exists.',
      },
    };
  }

  /**
   * Verifies and records device management integrity.
   */
  async verifyIntegrity(dto: DeviceIntegrityReportDto) {
    const device = await this.prisma.device.findUnique({
      where: { id: dto.deviceId },
      include: { child: true },
    });

    if (!device) {
      throw new NotFoundException('Device not found.');
    }

    const updated = await this.prisma.device.update({
      where: { id: dto.deviceId },
      data: {
        deviceOwnerEnrolled: dto.isDeviceOwner,
        batteryLevel: dto.batteryLevel,
        isOnline: true,
        lastSeenAt: new Date(),
      },
    });

    // Record audit log if tamper detected or DO revoked
    if (!dto.isDeviceOwner || dto.isRootedOrTampered) {
      await this.prisma.auditLog.create({
        data: {
          action: 'DEVICE_INTEGRITY_ALERT',
          details: JSON.stringify({
            deviceId: dto.deviceId,
            childId: device.childId,
            isDeviceOwner: dto.isDeviceOwner,
            isRooted: dto.isRootedOrTampered,
            securityPatch: dto.securityPatchLevel,
          }),
        },
      });
    }

    return {
      status: 'verified',
      isCompliant: dto.isDeviceOwner && !dto.isRootedOrTampered,
      device: {
        id: updated.id,
        deviceOwnerEnrolled: updated.deviceOwnerEnrolled,
        lastSeenAt: updated.lastSeenAt,
      },
    };
  }
}
