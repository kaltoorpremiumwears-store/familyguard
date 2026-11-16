import { Controller, Get, Put, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { PoliciesService } from './policies.service';
import { UpdateScreenTimeDto } from './dto/update-screentime.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Parental Policies')
@Controller('api/v1/policies')
export class PoliciesController {
  constructor(private readonly policiesService: PoliciesService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('child/:childId')
  @ApiOperation({ summary: 'Get all active policies for a child' })
  async getChildPolicies(@Request() req, @Param('childId') childId: string) {
    return this.policiesService.getChildPolicies(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Put('child/:childId/screentime')
  @ApiOperation({ summary: 'Update screen time limits, schedules, and instant pause' })
  async updateScreenTime(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: UpdateScreenTimeDto,
  ) {
    return this.policiesService.updateScreenTime(req.user.parentId, childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('device/sync')
  @ApiOperation({ summary: 'Child device syncs its active restrictions bundle' })
  async syncDevicePolicy(@Request() req) {
    // req.user.sub contains deviceId when authenticated as CHILD_DEVICE
    return this.policiesService.getDeviceActivePolicy(req.user.sub || req.user.id);
  }
}
