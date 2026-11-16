import { Controller, Get, Post, Body, Query, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { EnrollmentService } from './enrollment.service';
import { DeviceIntegrityReportDto } from './enrollment.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Device Owner Enrollment & Provisioning')
@Controller('api/v1/enrollment')
export class EnrollmentController {
  constructor(private readonly enrollmentService: EnrollmentService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('enterprise-qr')
  @ApiOperation({ summary: 'Generate Android Enterprise 6-tap QR provisioning payload' })
  async getEnterpriseQr(@Request() req, @Query('serverUrl') serverUrl?: string) {
    return this.enrollmentService.generateAndroidEnterpriseQrPayload(req.user.familyCode, serverUrl);
  }

  @Get('adb-instructions')
  @ApiOperation({ summary: 'Get developer ADB shell commands for instant test provisioning' })
  getAdbInstructions() {
    return this.enrollmentService.getAdbProvisioningInstructions();
  }

  @Post('integrity-report')
  @ApiOperation({ summary: 'Child device reports Device Owner integrity and hardware health' })
  @ApiResponse({ status: 200, description: 'Integrity status acknowledged' })
  async reportIntegrity(@Body() dto: DeviceIntegrityReportDto) {
    return this.enrollmentService.verifyIntegrity(dto);
  }
}
